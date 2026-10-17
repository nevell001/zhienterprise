# 后端安全审查报告

审查范围：`zhi-admin` / `zhi-framework` / `zhi-system` / `zhi-common` / `zhi-quartz` / `zhi-generator`
审查基线：`AGENTS.md`（安全约定与历史契约）、`CLAUDE.md`、`sql/00_init_database.sql`
审查方式：逐文件读源码取证（非经验断言），每条结论均带 `文件:行号`
审查日期：2026-10-17
结论版本：工作区当前 HEAD `c013feed`（工作树无源码改动，本报告是唯一新增文件）

---

## 结论摘要

**总评：核心安全骨架（认证、鉴权注解、注入面、匿名写防护）整体扎实且与 AGENTS.md 历史契约一致，未发现"严重"级漏洞；主要问题集中在"匿名认证端点缺少限流/口令策略"与"登录态用户枚举"这类中等风险面。**

严格说，本次**没有发现可直接远程获取权限或读写他人数据的"严重"漏洞**。逐项核对的结果是：权限串与菜单种子数据 100% 对齐（差集为空）、无 MyBatis `${}` 注入可达点、ORDER BY 动态排序有白名单校验、上传扩展名黑名单已删除 html/svg、`/error` 仍是 permitAll、`RefererFilter` 仍按 host 比较、`IpUtils` 仍只在直连对端可信时才采信 XFF。这些都是"必须仍然成立"的约定，本次复核确认成立。

真正的发现是**防护缺口**而非破防点：`/api/auth/reset-password` 无任何限流且无口令策略校验、登录接口可枚举账号状态、验证码失败计数可被用于短时锁定他人账号。

| 级别 | 数量 | 概要 |
|------|------|------|
| 严重 | 0 | — |
| 高 | 2 | 密码重置全链路无口令策略；匿名认证端点（`/api/auth/*`）零限流 |
| 中 | 4 | 登录态用户枚举；验证码失败计数 DoS；CORS 通配；Druid 密码入日志 |
| 低 | 4 | `/api/*` 的 `@Anonymous` 覆盖面复核建议；monitor 权限串为死权限；RSS 未转义（需 admin）；`/system/article` XSS 过滤豁免依赖前端消毒 |
| 待验证 | 3 | Actuator `metrics` 在生产实际暴露面；nginx `/manage/` 反代可达性；`.env` 生产实际取值 |

---

## 严重

未发现。本次审查未找到可远程获取权限、越权读写他人数据或绕过认证的漏洞。

---

## 高

### H-1　密码重置链路完全没有口令策略校验，且可在注册开关关闭时使用

**位置**
- `zhi-admin/src/main/java/com/zhi/web/controller/site/AuthController.java:131-179`（`POST /api/auth/reset-password`）
- `zhi-common/src/main/java/com/zhi/common/constant/UserConstants.java:79-80`（仅有的长度约束常量，`PASSWORD_MIN_LENGTH = 5`）

**代码片段**
```java
// AuthController.java:131
@Anonymous
@PostMapping("/reset-password")
public AjaxResult resetPassword(@RequestBody Map<String, String> params)
{
    String email = params.get("email");
    String code = params.get("code");
    String newPassword = params.get("newPassword");
    String confirmPassword = params.get("confirmPassword");

    if (StringUtils.isEmpty(email) || StringUtils.isEmpty(code) ||
            StringUtils.isEmpty(newPassword) || StringUtils.isEmpty(confirmPassword))
    {   // AuthController.java:140 —— 只判空
        return AjaxResult.error("参数不能为空");
    }
    if (!newPassword.equals(confirmPassword)) { ... }   // :146 只判两次一致
    if (!emailService.verifyCode(email, code, IEmailService.CODE_TYPE_RESET)) { ... }  // :152
    ...
    String encryptedPassword = SecurityUtils.encryptPassword(newPassword);  // :168 直接加密落库
    userService.resetUserPwd(existUser.getUserId(), encryptedPassword);     // :169
```

**攻击场景 / 触发路径**
1. 攻击者只猜/撞库得到受害者**邮箱**（站点前台 `blog_email`、友链申请 `zhi_friend_link.email` 都是公开或半公开的），诱导或先行完成一次邮箱验证码获取即可。
2. 持码者把 `newPassword` 设为单字符（如 `"1"`）——`reset-password` 只做判空与两次一致校验，**没有长度、复杂度、弱口令、与原密码对比的任何检查**，单字符口令直接写库（`:168-169`）。
3. 对照组：注册路径 `SiteRegisterBody.java:25-26` 有 `@Size(min = 5, max = 20)`；登录路径 `SysLoginService.java:32-38` 也按 `PASSWORD_MIN_LENGTH/MAX` 限定。**唯独重置路径完全裸奔**，形成策略不对称。
4. 附带问题：`reset-password` 与 `send-reset-code` 都**不受 `sys.account.registerUser` 开关约束**（该开关只 gate 了 `register` 与 `SysRegisterController`）。管理员关闭注册时，仍留下一条可改任意账号口令的匿名写通道（这条通道本身需要邮箱验证码，因此不是越权，但它绕开了管理员"关闭对外注册"的意图）。

**修复建议**
- 在 `resetPassword` 中复用统一口令策略校验：长度 ≥ 8、至少两类字符、拒绝常见弱口令，并与 `SiteRegisterBody` 的约束保持一致（不要只靠 DTO 注解，该方法接收的是裸 `Map`，注解根本不生效）。
- 把口令策略抽成单一工具（如 `PasswordPolicy.validate(raw)`），在注册 / 重置 / `SysProfileController.updatePwd`（`zhi-admin/.../SysProfileController.java:111-137`，同样**无任何强度校验**）三处共同调用，避免再次漂移。
- 明确 `reset-password` 是否应受注册开关约束：若"关闭注册"意在关闭全部自助账号变更，应在该端点同样检查 `sys.account.registerUser`。

**严重级别：高**（不直接越权，但把"重置口令"变成可设置任意弱口令的通道，且与注册路径策略矛盾，属于明确的纵深防御缺口）

---

### H-2　全部 `/api/auth/*` 匿名认证端点零限流，限流只覆盖了登录

**位置** `zhi-admin/src/main/java/com/zhi/web/controller/site/AuthController.java:51,71,104,131`
对照：`zhi-admin/src/main/java/com/zhi/web/controller/auth/UnifiedAuthController.java:35`（登录**有** `@RateLimiter`）

**代码片段**
```java
// AuthController.java —— 4 个映射方法，无一带 @RateLimiter
51:    @Anonymous
52:    @PostMapping("/register")            public AjaxResult register(...)          // 无限流
71:    @Anonymous
72:    @PostMapping("/send-register-code")  public AjaxResult sendRegisterCode(...)  // 无限流
104:   @Anonymous
105:   @PostMapping("/send-reset-code")     public AjaxResult sendResetCode(...)     // 无限流
131:   @Anonymous
132:   @PostMapping("/reset-password")      public AjaxResult resetPassword(...)     // 无限流

// 对照：UnifiedAuthController.java:35 —— 只有登录做了限流
@RateLimiter(key = "auth:login:", time = 60, count = 20, limitType = LimitType.IP)
@PostMapping("/login")
```

**攻击场景 / 触发路径**
- `/reset-password` 是**验证码校验的爆破点**：6 位数字码的暴力尝试只受 `verifyCode` 的失败计数约束（`EmailServiceImpl.java:236-245`，上限 `maxVerifyAttempts=5`）。该计数器是**每 email+codeType 一个 key**，所以攻击者可以不断更换目标邮箱来"重置计数窗口"，每个邮箱 5 次尝试、无需承担任何 IP 侧限流成本 —— 5 次 × N 个邮箱的规模下，`/reset-password` 是全站唯一不受 IP 限流保护的验证码校验入口（`/api/comment`、`/api/message`、`/common/upload*`、`/auth/login`、友链申请**全部**有限流；AGENTS.md 也把"每个匿名写端点都要限流"作为既有约定）。
- `/send-reset-code`、`/send-register-code` 虽有 service 层兜底（`EmailServiceImpl.java:330-356` 每邮箱 60s、`:381-420` 每 IP 10 次/小时），但**IP 维度的限制依赖 `IpUtils.getIpAddr` 且仅在 `isDevPrintCodeEnabled()` 为 false 时生效**（`:384-388`）；在 dev profile 下 `checkIpRateLimit` 直接 `return true`，完全没有 IP 限流。控制器层限流是这套防护唯一的、与 profile 无关的防线，而它缺失。
- `/register` 无限流：配合 H-1，注册路径同样可被脚本化批量试探（虽然需要邮箱验证码）。

**修复建议**
- 给 `AuthController` 的四个映射方法补 `@RateLimiter`（`LimitType.IP`），与既有约定一致：`/reset-password` 建议 60s / 10 req，`/send-*-code` 建议 60s / 5 req，`/register` 建议 60s / 10 req。
- 说明：`RateLimiterAspect`（`zhi-framework/src/main/java/com/zhi/framework/aspectj/RateLimiterAspect.java:49-74`）实现是正确的 —— Redis Lua 脚本原子计数（`:59`）、基于加固后的 `IpUtils.getIpAddr()`（`:81`）作为键，因此直接加注解即可生效，不存在绕过 `X-Forwarded-For` 伪造的空间。

**严重级别：高**（把一个可爆破的验证码校验端点暴露在无 IP 限流下，且与项目自身"匿名写必须限流"的既定约定直接冲突）

---

## 中

### M-1　登录接口可枚举账号状态（禁用/删除账号与密码错误返回不同文案）

**位置**
- `zhi-framework/src/main/java/com/zhi/framework/web/service/UserDetailsServiceImpl.java:41-57`
- `zhi-admin/src/main/resources/i18n/messages.properties:5,6,9,10`
- `zhi-admin/src/main/java/com/zhi/web/controller/auth/UnifiedAuthController.java:60-63`

**代码片段**
```java
// UserDetailsServiceImpl.java:41 —— 状态判断全部发生在密码校验（:57）之前
if (StringUtils.isNull(user)) {
    throw new ServiceException(MessageUtils.message("user.not.exists"));   // "用户不存在/密码错误"
}
else if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
    throw new ServiceException(MessageUtils.message("user.password.delete"));  // "对不起，您的账号已被删除"
}
else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
    throw new ServiceException(MessageUtils.message("user.blocked"));      // "用户已封禁，请联系管理员"
}
passwordService.validate(user);   // :57 密码校验在最后
```
```java
// UnifiedAuthController.java:60 —— 异常文案原样回给客户端
catch (Exception e) {
    logger.error("登录失败：username={}, error={}", username, e.getMessage());
    return AjaxResult.error("登录失败：" + e.getMessage());
}
```
```
messages.properties:5   user.not.exists=用户不存在/密码错误
messages.properties:6   user.password.not.match=用户不存在/密码错误
messages.properties:9   user.password.delete=对不起，您的账号已被删除
messages.properties:10  user.blocked=用户已封禁，请联系管理员
```

**攻击场景 / 触发路径**
- "用户不存在"与"密码错误"被刻意归一为同一文案（`:5`/`:6`，这是正确的反枚举做法），但**被删除**（`:9`）与**被停用**（`:10`）是两段截然不同的文案，且判定发生在密码校验之前 —— 攻击者用**任意密码**请求登录，即可区分目标账号处于"正常 / 已删除 / 已停用"三种状态之一，无需知道口令。
- 现实影响：可批量确认某邮箱/用户名是否为有效账号、是否被管理员封禁，为后续撞库、社工、定向钓鱼筛选目标。这不泄露口令，因此定为中危而非高危。

**修复建议**
- 将"账号已被删除/已被停用"在响应层统一归一为与"用户不存在/密码错误"相同的文案（如统一 `user.not.exists`），真实原因只写服务端日志（日志里已有 `log.info("登录用户：{} 已被停用.")`）。
- 或在 `SysLoginService` 的 catch 分支中把非 `BadCredentialsException` 的异常统一映射为通用认证失败消息，避免 `e.getMessage()` 直通到响应体。

**严重级别：中**

---

### M-2　验证码失败计数器可被用来短时锁定他人账号（自助流程 DoS）

**位置** `zhi-system/src/main/java/com/zhi/system/service/impl/EmailServiceImpl.java:236-245, 286-295`

**代码片段**
```java
// EmailServiceImpl.java:236 —— 失败计数键 = 邮箱 + 类型，与请求来源无关
String failKey = VERIFY_FAIL_PREFIX + email + ":" + codeType;

// :239-245 —— 达到阈值后，正确验证码同样被拒（提前 return）
Object failCountObj = redisTemplate.opsForValue().get(failKey);
int failCount = failCountObj == null ? 0 : ((Number) failCountObj).intValue();
if (failCount >= emailCodeConfig.getMaxVerifyAttempts())
{
    log.warn("验证码已锁定：email={}, type={}, failCount={}", email, codeType, failCount);
    return false;   // ← 合法用户的正确验证码在这里也被拒绝
}
```
```java
// :286-295 —— 计数在每次失败时自增，与调用者身份无关
private void recordVerifyFail(String failKey) {
    Long count = redisTemplate.opsForValue().increment(failKey);
    boolean reached = count != null && count >= emailCodeConfig.getMaxVerifyAttempts();
    redisTemplate.expire(failKey, reached ? emailCodeConfig.getVerifyLockMinutes() : emailCodeConfig.getExpireMinutes(), TimeUnit.MINUTES);
```

**攻击场景 / 触发路径**
- 失败计数以 `email:codeType` 为键，**不含发起方 IP/会话**。攻击者只要知道受害者邮箱，就能对 `/api/auth/reset-password` 连续提交 5 次**故意错误**的验证码（该端点无限流，见 H-2），把 `failKey` 顶到 `maxVerifyAttempts=5`，触发 `verifyLockMinutes=5` 的锁定。
- 锁定期间**受害者本人拿着正确的 6 位码提交也会被 `return false`（`:244`）拒掉**，表现为"验证码错误或已过期"。即：5 个无需认证的请求 → 5 分钟内该邮箱的注册/重置验证码通道不可用。
- 影响有限（5 分钟自愈、需已知邮箱、说明中可提示"稍后重试"），但这是一个明确的、可重复触发的可用性攻击面。

**修复建议**
- 把失败计数键加上请求来源维度（如 `email + ":" + codeType + ":" + ip`），或对同一 `email` 的**跨来源**失败改为"软锁定"：锁定仅拒绝错误码，正确码仍放行（即把锁检查移到 `code.equals(emailCode.getCode())` 之后，仅用于阻断继续爆破）。
- 至少为 `/reset-password` 补 IP 限流（见 H-2），抬高触发成本。

**严重级别：中**

---

### M-3　CORS 允许任意来源（`addAllowedOriginPattern("*")`）

**位置** `zhi-framework/src/main/java/com/zhi/framework/config/ResourcesConfig.java:78-95`

**代码片段**
```java
// ResourcesConfig.java:79
@Bean
public CorsFilter corsFilter()
{
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOriginPattern("*");   // :83 任意来源
    config.addAllowedHeader("*");          // :85
    config.addAllowedMethod("*");          // :87
    config.setMaxAge(1800L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);   // :92 全路径
    return new CorsFilter(source);
}
```

**攻击场景 / 触发路径**
- 该配置对 `/**` 放开全部来源与全部方法，任意站点均可跨域调用本 API。
- **缓解因素（已核实）**：`config` 上**没有调用 `setAllowCredentials(true)`**，因此浏览器不会在跨域请求中携带 Cookie；本系统使用 `Authorization` 头传递 JWT（`SecurityConfig.java:197` 附近的 `token.header: Authorization`），攻击者站点无法读取受害者浏览器里的 JWT（该头由前端 JS 从 localStorage 读取并显式附加，跨域站点读不到同源 localStorage）。因此这**不是**典型的会话骑乘型 CSRF。
- 仍存在的风险面：任意来源可无凭证调用匿名接口（放大 H-2 的爆破/刷接口能力，攻击者可用访客浏览器做分布式请求源）；错误配置的隐身风险（一旦未来加入 Cookie 认证即立刻升级为 CSRF）；以及生产中不必要地放大了可被第三方网页调用的攻击面。

**修复建议**
- 生产环境按站点访问地址 `blog_url` 收窄为白名单来源（`setAllowedOrigins(...)`），移除 `/**` 通配；开发环境可保留宽松配置。
- 若确需通配，至少在 `application-prod.yml` 中覆盖为具体域，并显式保持 `allowCredentials=false`（不要在通配下开启凭证）。
- 建议补一条交叉验证测试，锁定 prod 下 `allowedOrigins` 不含 `*`。

**严重级别：中**（无凭证前提下不构成直接越权，故非高；但属于明确的生产配置过宽）

---

### M-4　Druid 监控密码以明文写入应用日志

**位置** `zhi-framework/src/main/java/com/zhi/framework/config/DruidServletConfig.java:49-56`

**代码片段**
```java
// DruidServletConfig.java:49
if (druidPassword == null || druidPassword.isEmpty()) {
    druidPassword = generateSecurePassword();
    log.warn("========================================");
    log.warn("警告: Druid监控密码未配置！");
    log.warn("已自动生成临时密码: {}", druidPassword);   // :53 明文落日志
    log.warn("请在环境变量中设置 DRUID_PASSWORD");
}
```

**攻击场景 / 触发路径**
- 未配置 `DRUID_PASSWORD` 时，随机生成的监控口令被明文写入日志。任何能读到应用日志的角色（运维、日志聚合平台、容器日志查看者、日志文件被误挂载/误上传）都直接获得该凭据。
- **缓解因素（已核实）**：该 Bean 带 `@ConditionalOnProperty(name = "spring.datasource.druid.statViewServlet.enabled", havingValue = "true")`（`:46`），而 AGENTS.md 明确记录 Druid 控制台有意不再配置，默认不满足条件 → 该 Bean **默认不会创建**，正常部署下这行日志不会执行。因此定为中危而非高危。
- 另注：`generateSecurePassword()`（`:70-78`）使用 `Math.random()` 生成口令 —— 非密码学安全随机源，一旦该路径被启用则应一并修正为 `SecureRandom`。

**修复建议**
- 删除明文打印（改为"已生成临时密码，请查阅环境变量配置"），或仅在非 prod profile 打印。
- 若该 Servlet 未来启用，把 `Math.random()` 换成 `SecureRandom`。
- 保持 `statViewServlet.enabled` 默认关闭，并在文档中明确禁止开启。

**严重级别：中**

---

## 低

### L-1　`/api/**` 匿名端点覆盖面建议逐项再确认（`@Anonymous` 清单复核）

**位置**：`PermitAllUrlProperties.java:40-52` 扫描出的匿名 URL 清单；本次共枚举 40+ 处 `@Anonymous`

复核结论（**逐个判断后，未发现不该公开的端点**，但列出需持续关注的几处）：

| 端点 | 判断 | 依据 |
|------|------|------|
| `GET /api/setting`、`GET /common/site/setting` | 应公开 | 键走 `FrontConfigKeys.CONFIG_KEYS` 白名单（35 个键），已核实**不含**任何密码/密钥（`FrontConfigKeys.java:16-36`） |
| `GET /common/site/setting/value/{configKey}` | 应公开 | 同一白名单校验（`FrontSettingController.java:243-247`），未知键返回"不支持的配置键" |
| `GET /api/stats/overview` | 应公开 | 仅返回已发布文章/分类/标签/已审核评论计数与总浏览量，无用户维度（`FrontController.java:174-207`） |
| `GET /api/message/list`、`/count`、`POST /api/message` | 应公开 | 读查询走 `selectPublishedMessageVo`（**列白名单，已核实剔除 email/ip/user_agent**，`MessageMapper.xml:27-30`） |
| `GET /api/comment/article/{articleId}` | 应公开 | 经 `FrontCommentVo` 显式投影，不含 email（`FrontCommentVo.java:11-40`，类注释即说明这是防 PII 泄漏的专用 VO） |
| `GET /api/article/{key}` | 应公开 | 仅返回已发布（`status=1`）且未删除内容 |
| `GET /api/page/list`、`/api/page/{slug}` | 应公开 | `selectPageBySlug` 强制 `status='1' and del_flag='0'`（`PageMapper.xml:66`），未发布页面不可读 |
| `GET /sitemap.xml`、`/robots.txt`、`/api/rss`、`/system/version` | 应公开 | SEO/探活用途 |
| `POST /api/auth/*`（4 个） | 应公开但需限流 | 见 **H-2** |
| `GET /system/friendLink/front/list`、`POST /system/friendLink/apply` | 应公开 | 申请带验证码 + IP 限流（`FriendLinkController.java:76-77`） |

**建议**：无需改动。建议为 `FrontConfigKeys.CONFIG_KEYS` 增加一条守卫测试，断言白名单中永远不出现 `*password*` / `*secret*` / `*key*` 形态的键，防止未来误加。

**严重级别：低**

---

### L-2　`sql/00_init_database.sql` 中存在无任何 `@PreAuthorize` 引用的权限串（死权限）

**位置** `sql/00_init_database.sql`（menu 表 perms 列） vs 全后端 `@PreAuthorize`

**核查方法**：脚本提取 Java 侧全部 `hasPermi('...')` 权限串（145 个）与 SQL 侧全部 menu `perms` 串（173 个）求差集。

**结果**
- **代码有、SQL 无：0 条** —— 即所有 `@PreAuthorize` 权限串都能在菜单种子数据里找到对应行，无"权限不存在导致接口对所有人 403"的问题。（这是本次交叉验证最关键的结论，属正面结果。）
- **SQL 有、代码无：28 条**，其中绝大多数是 RuoYi 遗留的监控/工具类菜单（`monitor:cache:*`、`monitor:job:*`、`monitor:online:*`、`monitor:druid:list`、`tool:gen:add`、`tool:swagger:list` 等）。这些监控页面在前端是**外链跳转**（Prometheus/Grafana/Actuator 由 `monitorUrl.ts` 解析），后端确实没有对应接口，因此"菜单给了权限但接口不校验"不构成漏洞 —— 只是菜单项存在而功能已改由外部系统承载。
- 值得单独一提的 2 条：
  - `zhi:comment:approve` —— 菜单里有，但审核接口实际用 `zhi:comment:audit`（`CommentController.java:107`）与 `zhi:comment:reject`（`:118`）。`approve` 疑为历史遗留命名，**已无接口引用**。
  - `statistics:*:query` / `statistics:*:export` 等若干串无代码引用。

**攻击场景**：无直接可利用性。属"权限模型与种子数据存在冗余"，会让后续维护者误以为某接口受某权限保护。若某个角色只被授予了 `zhi:comment:approve` 而以为能审核评论，实际会因接口要求 `zhi:comment:audit` 而被 403 —— 表现为功能异常而非安全问题。

**修复建议**：清理 SQL 中确认废弃的 perms（尤其 `zhi:comment:approve`），或在菜单注释里标注"仅用于页面入口/外链，无后端接口"。建议保留 `ControllerPermissionPolicyTest` 这类交叉校验测试，并考虑新增"SQL perms 必须被引用或有明确豁免清单"的守卫。

**严重级别：低**

---

### L-3　RSS 输出未做 XML 转义（`siteUrl` 直接拼接）

**位置** `zhi-admin/src/main/java/com/zhi/web/controller/site/FrontController.java:998-1070`

**代码片段**
```java
// FrontController.java:1008-1015
String siteUrl = settingService.selectSettingValueByKey("blog_url");
if (siteUrl == null || siteUrl.trim().isEmpty()) { siteUrl = "http://localhost:3000"; }
String siteName = settingService.selectSettingValueByKey("blog_name");
...
out.println("<link>" + siteUrl + "</link>");                       // ← 未转义
out.println("<title><![CDATA[" + siteName + "]]></title>");        // CDATA 未过滤 "]]>"
out.println("<description><![CDATA[" + siteDesc + "]]></description>");
```

**攻击场景 / 触发路径**
- `blog_url` / `blog_name` / `blog_desc` 由 `SettingController` 写入，需要 `zhi:setting:edit` 权限（`SettingController.java:141-156`）。**因此触发者是已认证的管理员**，不是匿名攻击者 —— 这是把它定为低危的原因。
- 若管理员账号被接管（或内部人员恶意），可在 `blog_url` 中注入 `</link><script>...` 之类内容破坏 RSS 结构；`blog_name`/`blog_desc` 的 CDATA 未处理 `]]>` 也同理。对订阅该 RSS 的第三方阅读器可能构成注入。
- 对照：同一作者的 `SeoController.writeUrl` 做得**正确** —— 对 `loc`/`lastmod` 调用 `escapeXml(...)`（`SeoController.java:196-205`），说明这是 RSS 一处遗漏而非全局缺陷。

**修复建议**：对 RSS 中所有插值（`siteUrl`、`siteName`、`siteDesc`）统一走 XML 转义工具；CDATA 内的值需替换 `]]>` 为 `]]]]><![CDATA[>`，或干脆改用转义而非 CDATA。

**严重级别：低**（需要 `zhi:setting:edit`，非匿名可达）

---

### L-4　`/system/article` 被排除出 XSS 过滤器，存储型 XSS 防护完全依赖前端消毒

**位置**
- `zhi-admin/src/main/resources/application.yml:256`：`excludes: /system/notice,/system/article,/manage/**`
- `zhi-admin/src/main/resources/application-prod.yml:87`：`excludes: /api/**,/index`（**不含** `/system/article`）
- `zhi-common/src/main/java/com/zhi/common/filter/XssFilter.java:59-69`（GET/DELETE 不过滤；仅 POST/PUT 走过滤）

**代码片段**
```yaml
# application.yml:252
xss:
  enabled: true
  excludes: /system/notice,/system/article,/manage/**   # ← dev/profile 默认排除文章接口
  urlPatterns: /system/*,/monitor/*,/tool/*
```
```yaml
# application-prod.yml:83 —— prod 覆盖为不含 /system/article
xss:
  enabled: true
  excludes: /api/**,/index
  urlPatterns: /system/*,/monitor/*,/tool/*,/admin/*
```

**分析（重要：生产实际是安全的）**
- 两个文件的 `excludes`/`urlPatterns` 都是**标量字符串**，Spring profile 覆盖时是整串替换而非合并。生产以 `SPRING_PROFILES_ACTIVE=prod` 启动（`docker-compose.prod.yml:46`、`docker-compose.release.yml:38`），因此**生产生效的是 `application-prod.yml` 的 `excludes: /api/**,/index`** —— `/system/article` **在生产是被过滤的**。这一点必须澄清，否则容易被误读为"生产存在存储型 XSS"。
- 实际缺口只存在于**开发/默认 profile**：此时 `/system/article` 被排除，文章正文（含量大 HTML）以原文入库。
- 服务端**没有**任何 HTML 消毒：全仓库 `sanitizeHtml`/`Jsoup`/`HTMLFilter` 调用点仅有 `EscapeUtil.clean`（供 XssFilter 与 `JsonSanitizer` 使用，`XssHttpServletRequestWrapper.java:42,66`），没有任何一处对 `article.content` 做服务端消毒。
- 兜底完全在前端：`sanitizeArticleContent`（`zhi-ui/src/utils/sanitize.ts:131-155`）在渲染前消毒，且**后台预览与前台详情用的是同一个消毒器**（`zhi-ui/src/views/admin/article/index.vue:1172-1176`、`zhi-ui/src/views/admin/page/index.vue:1271`）。
- 写入侧需要权限：`ArticleController.add/edit` 要求 `zhi:article:add` / `zhi:article:edit`（`zhi-system/.../ArticleController.java:127,154`），不是任意登录用户。

**攻击场景 / 触发路径**
- 生产环境下无法直接利用（XssFilter 生效）。
- 开发/默认 profile 下：任意具备 `zhi:article:add` 的账号写入 `<script>`/`onerror=` 正文并入库；此后**任何绕过前端消毒的消费方**（第三方 API 调用者、未来的新渲染入口、直接读 API 的移动端）都会收到未消毒 HTML。当前所有已知渲染入口都过了 `sanitizeArticleContent`，因此没有可直接利用的路径。
- 风险性质：**纵深防御单点化** —— 防护 100% 依赖前端，服务端零消毒，且 dev/prod 配置存在语义不一致（同一份 `xss.excludes` 在两个 profile 下含义不同），未来若有人把 prod 的 excludes 抄成默认值，就会立刻变成真实的存储型 XSS。

**修复建议**
- 在服务端对文章/页面正文做一次消毒（引入 `jsoup` 白名单或复用 `HTMLFilter`）后再入库，使安全不依赖前端。
- 统一 dev 与 prod 的 `xss.excludes` 语义，去掉 `/system/article` 豁免，避免"配置文件看起来一致实则行为不同"的陷阱；如确需豁免富文本，应在代码注释中写明理由并配测试锁定。
- 补一条测试断言 `application-prod.yml` 的 excludes 不含 `/system/article`。

**严重级别：低**（生产不可利用；属配置不一致 + 纵深防御单点）

---

## 待验证

以下三项**无法仅凭静态读码确认**，需要运行环境或额外信息，如实标注。

### V-1　Actuator `metrics` 在生产是否泄漏敏感信息
`SecurityConfig.java:144-150` 对所有环境 `permitAll` 了 `/manage/actuator`、`/health/**`、`/info`、`/metrics/**`、`/prometheus`；prod 的 `include: health,info,metrics,prometheus`（`application-prod.yml:100`）已移除 `env`/`configprops`（这两者只在非 prod 放行，`SecurityConfig.java:160-163`），且 `show-details: never`（`:109`）。
**需要什么才能确认**：以 prod profile 实际启动后请求 `/manage/actuator/metrics`，确认响应中不含连接串、主机名、用户名等；并确认 nginx `location /manage/`（`zhi-ui/nginx.conf:123`）是否对外网可达（若反代未限制来源，则 metrics 与 prometheus 是匿名可读的）。

### V-2　生产 nginx 对 `/manage/` 的暴露面
本次只确认了 `zhi-ui/nginx.conf:123` 存在 `location /manage/`，未核实其是否绑定到对外 vhost、是否加了来源限制。
**需要什么才能确认**：实际部署的 nginx 配置与访问测试。

### V-3　`.env` 中生产密钥的实际取值强度
`.env` 已被 `.gitignore:78` 忽略且未被 git 跟踪（已核实 `git check-ignore` 命中、`git ls-files` 无记录），`.env.example` 全部为占位符（已逐行核实，无真实密钥）。工作区内的 `.env`（8357 字节，存在）取值未审计——审计它需要读取真实凭据，超出本次静态审查范围。
**需要什么才能确认**：由运维确认 `R_TOKEN_SECRET` ≥64 字符且随机、`DB_PASSWORD`/`REDIS_PASSWORD`/`DRUID_PASSWORD` 非弱口令。`SecurityConfigValidator` 已实现 prod 启动阻断（`SecurityConfigValidator.java:102` throw、`:228` prod 判定），机制本身可信。

---

## 已验证但仍然成立的既有安全契约

本节列出 AGENTS.md 明确要求"必须仍然成立"的历史契约，逐条**读码复核确认实现正确**。目的是避免下一轮审查重复劳动 —— 这些**不需要再查**，除非有人改动对应文件。

| # | 契约（AGENTS.md） | 复核结论 | 证据 |
|---|------------------|---------|------|
| 1 | `/error` 必须 `permitAll` | ✅ 成立 | `SecurityConfig.java:140-141` 显式放行，且带事故注释说明 403 曾被包装成 200+401 |
| 2 | `RefererFilter` 必须比较 **host** 而非 substring | ✅ 成立 | `RefererFilter.java:96-134` 按 host 精确/父域比较；`normalizeHost`（`:164-218`）剥协议/端口/路径/大小写；文档注释明确禁止 `contains` |
| 3 | `IpUtils` 只在直连对端是可信代理时才信 XFF | ✅ 成立 | `IpUtils.java:58-61` 先判 `isTrustedProxy(peer)`，非可信直接返回 peer；`:140-156` XFF 从**右往左**取第一个合法 IP（忽略伪造前缀）；`:159-170` 仅接受 IP 字面量 |
| 4 | 限流端点清单（`/api/comment`、`/api/article/view/*`、`/auth/login`、`/common/upload*`） | ✅ 成立 | `FrontController.java:730-731`（comment 60s/10）、`:580-581`（view）、`UnifiedAuthController.java:35`（login 60s/20）、`CommonController.java` 全部 8 个上传端点各带 `@RateLimiter(60s/20)` |
| 5 | 留言板三项前端接口均受 `guestbook_enabled` gate（**含读**） | ✅ 成立 | `FrontMessageController.java:89-99`（list）、`:111-115`（count）、`:130-133`（add）均先判 `isGuestbookClosed()` |
| 6 | 留言 60s 冷却检查在验证码**之前**（避免烧掉一次性码） | ✅ 成立 | `FrontMessageController.java:188-193` 冷却判断，`:196-198` 才是 `captchaService.validateAndGetAgeSeconds` |
| 7 | 留言时间窗：验证码年龄 <3s 拒绝 | ✅ 成立 | `FrontMessageController.java:196-200`（`MIN_FILL_SECONDS`）；`CaptchaServiceImpl.java:56-82` 返回真实年龄（先取 TTL 再删，`:64-66`） |
| 8 | 图形验证码一次性、不可爆破 | ✅ 成立 | `CaptchaServiceImpl.java:64-66` 先 `getExpire` 再 `delete`，无论比对成败都已删除 |
| 9 | 邮箱验证码失败计数 + 锁定 | ✅ 成立 | `EmailServiceImpl.java:239-245`（锁检查）、`:286-295`（自增+TTL）；上限 `EmailCodeConfig.maxVerifyAttempts=5`、锁 5 分钟 |
| 10 | 公开留言列表剔除 `email`/`ip`/`user_agent` | ✅ 成立 | `MessageMapper.xml:27-30` `selectPublishedMessageVo` 列白名单中**无**这三列 |
| 11 | 公开评论接口不得含 email（PII） | ✅ 成立 | `FrontCommentVo.java:11-40` 显式投影；`FrontController.java:632-636` 前台查询 + 投影并用 |
| 12 | 自定义页面详情仅返回已发布 | ✅ 成立 | `PageMapper.xml:66` `where slug = #{slug} and status = '1' and del_flag = '0'` |
| 13 | 注册开关严格规则（仅字面量 `"true"` 开启） | ✅ 成立 | `AuthController.java:55-58`、`SysRegisterService` 同规则；未误用默认开启的 `SiteSwitchUtils` |
| 14 | 注册用户 `user_type='01'` 且 insert 持久化该列 | ✅ 成立 | `SiteUserService.java:41,118` 设 `USER_TYPE_REGISTERED="01"`；`SysUserMapper.insertUser` 写入该列（有 `SysUserMapperTest` H2 往返测试锁定） |
| 15 | 文章/页面浏览去重与 PV 明细（`zhi_visit_log`） | ✅ 成立 | `FrontController.java:325-330`、`FrontPageController.java:66-72` 均按 `view_count_enabled` 判定后 `recordVisit` |
| 16 | 前台设置接口键白名单 | ✅ 成立 | `FrontConfigKeys.java:16-36`，`/api/setting`（`FrontController.java:260-270`）与 `/common/site/setting*`（`FrontSettingController.java:43,245`）共用同一份 |
| 17 | 匿名公开统计不含用户/系统敏感维度 | ✅ 成立 | `FrontController.java:174-207` 仅 5 个内容计数 |
| 18 | 上传扩展名黑名单已删除 html/htm/svg | ✅ 成立 | `MimeTypeUtils.java:29-41` 白名单式允许，注释明确禁止放行 html/svg；`FileUploadUtils.assertAllowed`（`:191-229`）为**允许列表**（非黑名单）校验 |
| 19 | 文件下载防目录穿越 | ✅ 成立 | `FileUtils.checkAllowDownload`（`:146-162`）拒绝含 `..`，且扩展名须命中允许列表 |
| 20 | 上传文件名不构成路径穿越 | ✅ 成立 | 实测 `FilenameUtils.getBaseName()` 对 `../../x.png`、`..\..\x.png`、`a/../../x.png` 均归一为 `x`（无分隔符残留）；`extractFilename`（`FileUploadUtils.java:149-152`）用 baseName + 序列号 |
| 21 | 动态 ORDER BY 有白名单校验 | ✅ 成立 | `PageUtils.java:23` 对**拼接后的整串**调用 `escapeOrderBySql`；`SqlUtil.java:21,31-42` 正则 `[a-zA-Z0-9_\ \,\.]+` + 500 长度上限。实测注入载荷 `id asc, (select 1)`、`id asc--`、`id asc/*` **全部被拒** |
| 22 | MyBatis `${}` 无用户可控注入点 | ✅ 成立 | 全仓库 mapper XML 仅 6 处 `${}`：`params.dataScope`（SysUser/SysRole/SysDept，RuoYi 数据权限框架生成，非请求参数直通）、`GenTableMapper.xml:175` `${sql}`（代码生成建表语句，来自 `GenController` 且需 `tool:gen` 权限）。无请求参数直通 `${}` |
| 23 | 登出真正失效 | ✅ 成立 | `LogoutSuccessHandlerImpl.java:42-50` 调 `tokenService.delLoginUser` 删 Redis `login_tokens:<uuid>`；`TokenService.java:102-109` 确认删除逻辑 |
| 24 | JWT 密钥强度与 prod 启动阻断 | ✅ 成立 | `TokenService.java:211,225` 使用 `Keys.hmacShaKeyFor`（HS256，密钥 <256bit 会直接抛异常，天然强制长度）；`SecurityConfigValidator.java:102` prod 缺失即抛异常阻断启动 |
| 25 | 登录失败锁定（密码重试次数） | ✅ 成立 | `SysPasswordService.java:50-71` 按 username 计数、`maxRetryCount` 后抛 `UserPasswordRetryLimitExceededException`；`UserDetailsServiceImpl.java:57` 实际调用（已确认接线，非死代码） |
| 26 | `updateByKey` 清空值需清 `sys_config` 镜像 | ✅ 成立 | `SettingController.applySettingUpdateByKey` 走 `SysConfigMapper.updateConfigValueByKey`（无 `<if>` 空值守卫） |
| 27 | 站点设置缓存写入必失效 | ✅ 成立 | `SettingController` 写方法均带 `@SiteCacheEvict(CacheConstants.SETTINGS_ALL)` |
| 28 | 服务端开关强制（不依赖前端隐藏入口） | ✅ 成立 | `comment_enabled`（`FrontController.java:733-736`）、`like_enabled`、`search_enabled`、`guestbook_enabled`、`view_count_enabled`、`referer_enabled` 均在服务端短路 |
| 29 | 评论归属校验（改/删仅限本人） | ✅ 成立 | `FrontController.java:826-830`（update）、`:870-874`（delete）均校验 `userId.equals(existing.getUserId())`；回复校验 parent 同文章（`:783-791`） |
| 30 | 站内信按收件人隔离（防 IDOR） | ✅ 成立 | `NotificationMapper.xml:69-85` `markAsRead`/`markAllAsRead` 均带 `and recipient_id = #{recipientId}`；`NotificationController.java:33-70` 全部从 `SecurityUtils.getUserId()` 取 |
| 31 | 个人信息接口不构成越权/批量赋值 | ✅ 成立 | `SysProfileController.java:87-93` 逐字段拷贝（nickName/email/phonenumber/sex），不接受任意 `SysUser` 字段 |
| 32 | `.env.example` 无真实密钥、`.env` 未入库 | ✅ 成立 | `.env.example` 逐行核实全为占位符；`.env` 被 `.gitignore:78` 忽略且 `git ls-files` 无记录 |

---

## 附：本次使用的核查方法（便于复现）

1. **权限串交叉验证**（对应 L-2 与"代码有 SQL 无 = 0"的结论）：Python 脚本遍历 `zhi-{system,admin,quartz,generator,framework}/src/main/java` 提取全部 `hasPermi('...')`（正则 `hasPermi\(\s*['"]([^'"]+)['"]`），与 `sql/00_init_database.sql` 全部引号 token 中含 ≥2 个冒号的串求双向差集。
   *踩坑记录*：初版正则字符类 `[A-Za-z0-9_:]*` 漏掉了 hyphen，导致 `statistics:article:category-distribution` 等 4 条被误报为"代码有 SQL 无"。修正为允许 `-` 后差集归零。**后续复核此类问题务必覆盖带连字符的权限串。**
2. **ORDER BY 注入实测**：用 `javac` 直接编译复现 `PageDomain.getOrderBy()` + `SqlUtil.SQL_PATTERN` 的拼接与校验逻辑，对 9 组载荷逐一断言，确认全部被拒。
3. **文件名穿越实测**：用 `commons-io 2.21.0`（`~/.m2` 实际版本）调用 `FilenameUtils.getBaseName/getExtension`，对 9 组穿越与畸形文件名断言输出。
4. **无 `@PreAuthorize` 方法扫描**：Python 解析所有 `@RestController/@Controller` 类，按方法收集注解块，输出既无 `@PreAuthorize` 也无 `@Anonymous` 的映射方法；再与 `ControllerPermissionPolicyTest.AUTHENTICATED_ONLY_ALLOWLIST`（`zhi-admin/src/test/java/com/zhi/web/config/ControllerPermissionPolicyTest.java:56-92`）逐一对照，确认全部为有意放行的自助接口。

**未做的事（诚实声明）**：本次为**纯静态审查**，未启动应用、未发送真实请求、未运行测试套件。H-1/H-2/M-1/M-2 的利用链条是基于代码控制流的推理，均有行号支撑，但未在运行环境端到端复现；V-1~V-3 已标注为待验证。