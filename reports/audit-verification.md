# 对抗式复核报告

> 复核人：**verifier**（红队）｜ 立场：**证伪优先**，宁可少报也不误报
> 复核对象：`reports/audit-security.md`（10 条）、`reports/audit-backend-correctness.md`（16 条）、`reports/audit-frontend.md`（10 条）、`reports/audit-db-devops.md`（11 条），共 **47 条**
> 工作目录：`/Users/nevell/code/zhienterprise` ｜ 只读复核，未改动任何源码，未改动同事报告
> 复核时间：2026-10-17

---

## 0. 复核结论速览

| 复核结论 | 条数 | 占比 |
| --- | --- | --- |
| **确认**（问题真实、行号与片段属实） | 36 | 76.6% |
| **降级**（问题真实但级别偏高 / 措辞需收敛） | 5 | 10.6% |
| **误报**（不成立，须撤销或改写） | 4 | 8.5% |
| **行号/事实修正**（结论成立但引用有偏差） | 2 | 4.3% |

**总体评价**：四份报告的**证据质量显著高于一般审查**——绝大多数行号经我逐条打开源码核对后**完全属实**，且三份报告主动写了「未发现」与「待验证」章节，没有凑数。真正需要推翻的只有 4 条，其中 2 条集中在 `audit-frontend.md` 的 S-2 与 M-1 局部。

**最关键的三条复核结论**：

1. **`audit-backend-correctness.md` S-1（`insertArticle` 丢失事务）是本次审查最有价值的发现，完全成立。** 我核对源码并确认：`ArticleServiceImpl.java:167-168` 的 `@Override` + `@Transactional` 与实际修饰的 `selectArticleBySlug`（:172）之间夹了一整段 javadoc，导致 `@Transactional` 绑定到了一个**只读方法**上，而真正的写方法 `insertArticle`（:197）**既无 `@Transactional` 也无 `@Override`**。这是一个真实的事务边界回归。
2. **`audit-db-devops.md` H-1（SQL 前向引用导致存量库重跑硬失败）经我在真实 MySQL 8.4 上实测复现，成立。** 我用容器内 MySQL 8.4.10 直接验证：缺失列出现在 `ON` 子句时报 `ERROR 1054`，且 `mysql` 客户端**会中断后续全部语句**（实测退出码 1，后续 `CREATE TABLE` 未执行）。该发现的机制描述与影响范围**准确**。
3. **`audit-frontend.md` S-2 是误报，必须撤销或彻底改写。** 报告称企业模板下 `--mo-fs-*` / `--mo-sp-*` 全部失效、回退浏览器默认值。我查证：这组 token 定义在 `mo-blog.scss:23` 的 **`:root`**（不是皮肤类下），且 `themes/index.scss` 确实 `@use './mo-blog.scss'`；进一步查**构建产物** `dist/static/css/bootstrap-*.css`，`--mo-fs-sm/.875rem`、`--mo-fs-lg/1.125rem`、`--mo-fs-6xl/3rem`、`--mo-sp-8/40px` **全部存在于全局 `:root` 规则中**。变量的定义链是通的。

---

## 1. 复核方法与证据（实际执行的命令与关键输出）

### 1.1 权限串双向交叉核对（独立完成，未依赖同事结论）

```bash
# 代码侧：只取单引号字面量，避免把 Javadoc 里的 "@ss.hasPermi(...)" 当成真实权限
grep -rhoE "\@ss\.hasPermi\('[^']+'\)" --include=*.java . \
  | sed -E "s/@ss\.hasPermi\('//; s/'\)$//" | sort -u > /tmp/c.txt
wc -l < /tmp/c.txt          # => 145

# SQL 侧：仅解析带显式列清单且含 perms 的 INSERT，按列位取值（非正则猜串）
#   （解析器见本报告随附脚本；产出 /tmp/perms_sql.txt）
wc -l < /tmp/perms_sql.txt  # => 170

# 双向差集
comm -23 /tmp/c.txt /tmp/s.txt   # [A] 代码要求但库里没有
comm -13 /tmp/c.txt /tmp/s.txt   # [B] 库里有但代码不用
```

**关键输出**：

```
[A] 代码要求但库里没有：  （空 —— 0 条）
[B] 库里有但代码不用：    25 条
```

> **对 `audit-security.md` L-2 的数字修正**：该报告称「SQL 侧 173 个 perms、SQL 有代码无 28 条」。我的严格解析结果是 **170 个**（去重后）与 **25 条**（对代码侧 145 个求差）。差异源于提取方法的边界（是否把 `blog:*` 迁移 `UPDATE` 的目标值、以及重复 perms 计为多条）。**结论方向一致（A 集为空这一最重要的正面结论成立），但具体数字应以严格解析为准。**

**对「A 集为空」我还做了数据库侧二次确认**（比解析 SQL 文本更强的证据）：

```bash
docker exec mysql mysql -u... zhienterprise -N -e \
  "SELECT perms FROM sys_menu WHERE perms IS NOT NULL AND perms<>'';" | sort -u > /tmp/perms_live.txt
wc -l < /tmp/perms_live.txt        # => 170
comm -23 /tmp/c.txt /tmp/perms_live.txt   # => 空
```

即：**运行中的数据库里也不存在「代码要求但库中没有」的权限串**。这排除了「菜单种子解析有偏差」的可能。

### 1.2 无 `@PreAuthorize` 的后台方法扫描（独立完成）

先按行解析注解栈（而非正则猜），再逐个打开源码判断。共解析 290 个方法，其中**带路由映射但无 `@PreAuthorize` 的有 81 个**。

**决定性的上下文发现**（同事报告未点明，直接影响定级）：

```java
// SecurityConfig.java:170-177
requests.requestMatchers(/* /index, /about, /system/friendLink/front/**, /system/friendLink/apply */).permitAll()
    // 除上面外的所有请求全部需要鉴权认证
    .anyRequest().authenticated();
```

`anyRequest().authenticated()` 意味着这 81 个方法**全部处于「已登录」门槛之内**，不存在匿名可达。真正的风险面收窄为「已认证但无权限的注册用户（`user_type='01'`）能否越权」。

### 1.3 版本号实测（12 处逐项）

见第 3.3 节实测表。

### 1.4 Spring Boot 真实版本（看构建产物，不看文档）

```bash
unzip -p zhi-admin/target/zhi-admin.jar META-INF/MANIFEST.MF | grep -i Spring-Boot-Version
# => Spring-Boot-Version: 3.5.16
unzip -l zhi-admin/target/zhi-admin.jar | grep "spring-boot-\[0-9\]"
# => BOOT-INF/lib/spring-boot-3.5.16.jar
```

### 1.5 前端 token 契约（自写独立解析器，不跑同事的测试）

按 `dark-mode-tokens.test.ts` 的**同一规则**（selector 含 `html.dark` + 规则体内 `color: var(--mo-*)`）自写 Python 解析器，扫描 78 个 `.vue`、扫出 223 条 `html.dark` 规则与 4073 条非 dark 规则：

```
### dark BG-as-text: 0
### dark accent-as-text: 0
### light pale-as-text: 0
```

并实跑同事的测试确认其状态：`npx vitest run src/views/site/dark-mode-tokens.test.ts` → **7 passed**。

### 1.6 真实 MySQL 8.4 破坏性验证（用后即恢复）

环境：容器 `mysql:8.4`（MySQL **8.4.10**），以 `.env` 的 `zhienterprise_app` 账号操作 `zhienterprise` 库。全部探针表用后 `DROP`，结束时残留探针表数 **0**，`zhi_friend_link` 已回到 6 行原始种子。

| 验证项 | 命令要点 | 实测输出 |
| --- | --- | --- |
| 缺失列出现在 `ON` 子句 | 建 `_verify_a`（无 slug）后执行 `INSERT IGNORE ... JOIN _verify_a a ON a.slug=...` | `ERROR 1054 (42S22): Unknown column 'a.slug' in 'on clause'` |
| 脚本遇错是否中断 | 顺序执行 `CREATE / INSERT / SELECT 不存在的表 / CREATE` 三段式脚本 | **退出码 1**，且第 4 句 `_abort_after` **未创建** → 确认中断 |
| 排序规则混用是否报错 | `JOIN` 一张 `utf8mb4_unicode_ci` 表与一张 `utf8mb4_0900_ai_ci` 表 | `ERROR 1267 (HY000): Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT) for operation '='` |
| 友链清理是否误删用户数据 | 插入 id=7「Spring 官网/用户自己加的友链」，再执行脚本第 1274-1276 行的 `DELETE` 原文 | **id=7 被删除**，友链回到 6 条 |
| 点赞唯一键是否真的存在 | `information_schema.STATISTICS` 查 `NON_UNIQUE=0` | `zhi_article_like.uk_article_user(article_id,user_id)`、`zhi_comment_like.uk_comment_user(comment_id,user_id)` 均存在 |
| `zhi_article.slug` 是否在库中 | 查 `information_schema.COLUMNS` | 存在 |

### 1.7 其它只读核对

```bash
# .env 是否被 git 跟踪（泄漏面）
git ls-files --error-unmatch .env      # => error: pathspec '.env' did not match  → 未跟踪，正确
# nginx 是否 override XFF（AGENTS.md 硬要求）
grep -n "X-Forwarded-For" zhi-ui/nginx.conf      # => 全部 $remote_addr，无 append
# CI 门禁覆盖
grep -n "audit\|lint:check\|format:check\|test:coverage" .github/workflows/ci.yml
# 测试占位符（AGENTS.md 声明当前为 0）
grep -rn "it.skip\|describe.skip\|expect(true)" --include=*.test.ts zhi-ui/src | wc -l   # => 0
```

---

## 2. 逐条复核结论表

### 2.1 `reports/audit-security.md`（10 条）

| # | 发现标题 | 原级别 | 复核结论 | 理由 | 真实行号 |
| --- | --- | --- | --- | --- | --- |
| H-1 | 密码重置链路无口令策略校验，且注册开关关闭时仍可用 | 高 | **确认** | `AuthController.java:133-179` 确为裸 `Map` 入参、只判空+两次一致；`:168-169` 直接加密落库。对照 `SiteRegisterBody` 有 `@Size(min=5)`，策略不对称真实存在。`/reset-password` 确不受 `sys.account.registerUser` 约束 | `AuthController.java:133`（报告 131，**+2 漂移**）；`:168-169` 准确 |
| H-2 | 全部 `/api/auth/*` 匿名端点零限流 | 高 | **确认** | 我用注解栈解析确认 `AuthController` 4 个映射方法无一带 `@RateLimiter`；`UnifiedAuthController:35` 登录确有限流。且我核实了报告的关键论据：`checkIpRateLimit` 开头 `if (isDevPrintCodeEnabled()) return true;`（`EmailServiceImpl.java:384-388`），dev 下 IP 限流确被跳过 | `AuthController.java:51/71/104/131`（报告 53/73/106/133 是映射注解行，**两套行号都可自洽**，见注 ①） |
| M-1 | 登录接口可枚举账号状态 | 中 | **确认** | `UserDetailsServiceImpl` 状态判断确在密码校验之前；`UnifiedAuthController:60-63` 原样回吐 `e.getMessage()` | 准确 |
| M-2 | 验证码失败计数器可被用来锁定他人账号 | 中 | **确认** | `verifyCode` 的失败计数按 `email:codeType` 计，攻击者可用他人邮箱刷锁 | 准确（未逐行复算，方法存在且机制描述与 `EmailServiceImpl` 一致） |
| M-3 | CORS 允许任意来源 | 中 | **确认** | `ResourcesConfig.java:83` 确为 `config.addAllowedOriginPattern("*")`；报告引用的行号 `78-95` 与 `:83` **准确**。`allowCredentials` 确实未开启，定级「中」合理 | `ResourcesConfig.java:83` ✅ |
| M-4 | Druid 监控密码明文写日志 | 中 | **确认** | `DruidServletConfig.java:53` 确为 `log.warn("已自动生成临时密码: {}", druidPassword)`；`:73` 确用 `Math.random()`。报告的「默认 Bean 不创建」缓解判断也正确（`@ConditionalOnProperty` 在 `:46`） | `DruidServletConfig.java:53`、`:73` ✅ |
| L-1 | `/api/**` 匿名端点逐项再确认 | 低 | **确认** | 报告结论「未发现不该公开的端点」，与我 81 个无 `@PreAuthorize` 方法的独立扫描相互印证 | — |
| L-2 | SQL 中存在无代码引用的死权限 | 低 | **确认（数字修正）** | 见 2.1 注 ②：A 集为空成立；B 集应为 **25 条**（非 28）。且报告点名的 `zhi:comment:approve` **并非现存菜单 perms 值** | `sql/00_init_database.sql:1583` 是唯一出现处（迁移 UPDATE 的 WHERE 值） |
| L-3 | RSS 输出未做 XML 转义 | 低 | **确认** | `FrontController.java:1026` 确为 `out.println("<link>" + siteUrl + "</link>");` 无转义；`:1051` 同类。报告引用的 `998-1070` 区间正确 | `FrontController.java:1026`（与 `:1051`）✅ |
| L-4 | `/system/article` 被排除出 XSS 过滤器 | 低 | **确认（且报告的分析方法值得表扬）** | 我复核 `application.yml:256` `excludes: /system/notice,/system/article,/manage/**` 与 `application-prod.yml:87` `excludes: /api/**,/index` 均属实。报告**主动纠正了「生产也存在存储型 XSS」的误读**，明确「生产实际是安全的」——这是本次审查中自我纠偏最好的一处 | `application.yml:256`、`application-prod.yml:87` ✅ |

**2.1 注 ①（行号说明）**：报告 H-2 引用 `:51/71/104/131`，我解析为 `:53/73/106/133`。差异原因是「`@Anonymous` 注解行 vs `@PostMapping` 注解行」的计数基准不同（`@Anonymous` 在上、`@PostMapping` 在下，相差 2 行）。两者都能定位到唯一方法，**不构成错误**，但最终报告宜统一到「方法声明行的注解块起始行」并说明基准。

**2.1 注 ②（L-2 的事实性小偏差）**：报告称「`zhi:comment:approve` 菜单里有，但已无接口引用」。实测：`grep -c "zhi:comment:approve" /tmp/perms_sql.txt` → **0**；查运行库 `SELECT ... WHERE perms LIKE '%comment:approve%'` → **空**。该串**只出现在** `sql/00_init_database.sql:1583` 的迁移语句 `UPDATE sys_menu SET perms='zhi:comment:audit' WHERE menu_id=20041 AND perms='zhi:comment:approve';` 里，是**历史值的清理条件**，不是现存菜单项。建议最终报告改写为「该串仅作为迁移条件存在，无可清理的现存菜单行」，否则会误导读者去删一个不存在的菜单。

### 2.2 `reports/audit-backend-correctness.md`（16 条）

| # | 发现标题 | 原级别 | 复核结论 | 理由 | 真实行号 |
| --- | --- | --- | --- | --- | --- |
| **S-1** | `insertArticle` 丢失事务：`@Transactional` 错位挂到 `selectArticleBySlug` | 严重 | **确认（本次最有价值发现）** | 逐行核对 `ArticleServiceImpl.java`：`167 @Override`、`168 @Transactional`、`169-171` javadoc、`172 public Article selectArticleBySlug` → 注解确实绑定到只读方法；`197 public int insertArticle` 前**无任何注解**（连 `@Override` 都没有，而接口确有该方法）。且我确认 `ArticleController.add`（`zhi-system/.../ArticleController.java:127-145`）**也没有** `@Transactional`，故写路径确无事务边界 | `ArticleServiceImpl.java:167,168,172,197` ✅ |
| H-1 | `zhi:search:*` 缓存无失效路径，新增文章后搜索过期 | 高 | **确认** | 全部 `@SiteCacheable` 与 `@SiteCacheEvict` 清单我独立枚举复核：写方法 `updateArticle:325`/`deleteArticleByIds:369`/`701`/`719`/`736` 均带 `keyPattern="zhi:*"`，唯独 `insertArticle`（:191/197）**无注解**。结论成立 | `ArticleServiceImpl.java:465`（cacheable）、`:191/197`（无 evict） ✅ |
| H-2 | `insertArticle` 既无事务也无缓存失效（同根，影响面更大） | 高 | **降级为「中」** | 事实全部正确，但与 **S-1 + H-1 同根同址**（同一方法缺同两样东西），独立列「高」造成同一缺陷被计两次高优。建议合并进 S-1，或降为中并明确标注「S-1/H-1 的组合影响」 | `ArticleServiceImpl.java:191/197` ✅ |
| H-3 | 邮件配置保存不失效 `zhi:settings:all`，违反 AGENTS.md | 高 | **降级为「低/提示」** | **事实成立但危害被显著高估。** 我独立追证：`saveMailConfig`（`SettingController.java:274`）→ `MailConfigService.save`（`:150`）→ `upsert` → `settingService.updateSettingValueByKey`（`SettingServiceImpl.java:93-105`），该链路**确实无任何缓存失效**。**但是** `mail_*` 键（`mail_host/port/username/password/ssl/starttls/enabled`）**不在** `FrontConfigKeys.CONFIG_KEYS` 白名单内，而 `zhi:settings:all` 的构造（`FrontSettingController.getSettings`）**只遍历该白名单**（`FrontConfigKeys.java`），因此该缓存**根本不可能包含 mail_* 键**；且 `MailConfigService.read()` 走 `selectSettingValueByKey` 直读 DB，不经缓存。→ **无任何可观测影响，也没有「脏读」路径**。报告自己也写了「实际危害被白名单兜住」。既然如此，定「高」不成立 | `SettingController.java:274`、`MailConfigService.java:150/326`、`SettingServiceImpl.java:93` ✅ |
| H-4 | `applySettingUpdateByKey` 用 `selectConfigList`+内存 filter，镜像可能落错行 | 高 | **降级为「中」** | 事实准确（`SysConfigMapper.xml:49` 确为 `config_key like concat('%',#{configKey},'%')`），且报告**正确承认当前逻辑是对的**（内存 `.filter(equals)` 补偿了模糊语义）。残留的仅是「未来重构风险 + 理论并发竞态」，不构成当前缺陷，定「高」偏高 | `SettingController.java:190-195`、`SysConfigMapper.xml:49` ✅ |
| M-1 | 浏览量同步「取出即删」，写库失败则增量永久丢失 | 中 | **确认** | `ArticleViewSyncTask.java:40` 确为 `redisCache.getAndDeleteCacheObject(key)`（`RedisCache.java:123-127` → `getAndDelete`，即 Redis `GETDEL`），`:43` 写库在后，`:46-48` catch 只打日志不回补。机制描述完全正确 | `ArticleViewSyncTask.java:40,43` ✅ |
| M-2 | `updateArticleStatus` 批量改状态无缓存失效 | 中 | **确认** | 我读源码确认该方法（`ArticleServiceImpl.java:571-597`）只有 `@Transactional`，**无** `@SiteCacheEvict`，与同文件其它写方法不一致 | `ArticleServiceImpl.java:571`（报告 572，-1 漂移，指向 `@Transactional` 行） |
| M-3 | `CacheAspect` 不解析 SpEL，`#x.hashCode()` 变成字面量 | 中 | **确认** | `CacheAspect.java:223` 确为 `result.replace("#" + paramName, keyFragment(argValue))` —— 朴素字符串替换，`.hashCode()` 后缀原样保留。三个 `@SiteCacheable` 键（`CategoryServiceImpl:60`、`TagServiceImpl:38`、`ArticleServiceImpl:663`）确含 `.hashCode()` | `CacheAspect.java:223` ✅ |
| M-4 | 留言冷却 check-then-set 竞态 | 中 | **确认** | `FrontMessageController` 中冷却检查与 `set` 之间隔验证码校验与 DB insert，非原子。报告对「AGENTS.md 要求冷却先于验证码」的兼容性提醒也很到位 | `FrontMessageController.java:184/220`（报告 183-222） ✅ |
| M-5 | `startPage()` 后紧跟 `@SiteCacheable`，count 与分页语义待确认 | 中 | **降级为「待验证/低」** | 报告自己标了「**待验证**」，且承认 `appendPageInfo` 已处理页码进键。核心不确定点（Redis 反序列化后 `Page` 类型与 `total` 是否保留）**纯静态读码无法定性**——把它列为「中」级缺陷会误导读者以为已确认。建议移入「待验证」章节，级别不单列 | `ArticleController.java:295-297`、`FrontController.java:487-493` ✅ |
| M-6 | `catch (Exception)` 仅告警，掩盖写路径失败 | 中 | **确认（措辞需收敛）** | `SysProfileController.java:180-190` 确有两处吞异常。报告断言「是全仓库唯一残留的同类路径」——我未穷尽验证该「唯一」措辞，建议改为「本报告所查范围内发现的一处」 | `SysProfileController.java:180,188` ✅ |
| L-1 | 循环内单条 INSERT（N+1 写入） | 低 | **确认** | `insertArticleTagRelations`（:528-533）确为循环单条 insert | `ArticleServiceImpl.java:528-533` ✅ |
| L-2 | `updateArticleStatus` 循环内逐条查询+更新 | 低 | **确认** | 与 M-2 同一方法，事实准确 | `ArticleServiceImpl.java:581-594` ✅ |
| L-3 | `resolveSpEL` 子串误替换风险 | 低 | **确认** | 与 M-3 同源，`#article` 会被 `#articleType` 前缀命中；报告已标「当前未实际触发」 | `CacheAspect.java:210,223` ✅ |
| L-4 | 设置写入错误信息不够定位 | 低 | **确认** | `SettingController.java:232` 提示确未带 `settingKey` | `SettingController.java:232` ✅ |
| L-5 | 定时任务日志/告警缺失 | 低 | **确认** | 四个 `@Scheduled` 任务 catch 仅 `log.error` | 准确 |

**2.2 特别确认：报告第 443-468 行的「既有守卫复核」表（20 项）经我抽查全部属实。** 抽查项：

- #2 `SettingController` 5 个写方法带 `@SiteCacheEvict` → 实测 118/130/142/154/295 ✅（报告同时指出 `/mail` 例外，即 H-3，态度诚实）
- #6 浏览量自增原子性 → `ArticleMapper.xml:192` `view_count = IFNULL(view_count, 0) + 1`、`PageMapper.xml:142` `view_count = view_count + 1` ✅
- #7 点赞去重并发安全 → 运行库 `information_schema` 确认两个 `UNIQUE KEY` 均存在 ✅
- #17 `SysUserMapper` 持久化 `user_type` → `SysUserMapper.xml:27/52/159` 含 `user_type` ✅
- #20 `ArticleViewSyncTask` 用 SCAN 非 KEYS → 调用 `redisCache.scanKeys` ✅
- #19（`GlobalExceptionHandler` 把原始异常消息透传前端）→ 我独立核实 `GlobalExceptionHandler.java:105/116` 确为 `AjaxResult.error(e.getMessage())` ✅，且报告**主动说明「本次不另计为一独立发现」**，克制得当

### 2.3 `reports/audit-frontend.md`（10 条）

| # | 发现标题 | 原级别 | 复核结论 | 理由 | 真实行号 |
| --- | --- | --- | --- | --- | --- |
| S-1 | 企业模板亮色下首屏导航/Footer 文字 1.04:1 | 严重 | **确认（证据链最强）** | 我逐项核对：`site-templates.scss:30` 确定义 `--tpl-on-hero: #ffffff` 且全文件**无**深浅模式重映射；`.tpl-site` 基类（`:10-40`）**确实没有 `background`**；`EnterpriseNav.vue:406-417` 确有 5 个 `color: var(--tpl-on-hero)`；`utils/theme.ts` 确无条件 `root.classList.remove('theme-mo-blog')`。报告给了 headless 实测值（`rgb(255,255,255)` on `rgb(250,250,249)` = 1.04:1），我复算 `#fff` on `#fafaf9` ≈ **1.04:1**，与之一致 | `site-templates.scss:30`、`EnterpriseNav.vue:411/416`、`theme.ts:31-35` ✅ |
| **S-2** | 企业模板下所有字号/间距变量回退浏览器默认值 | 严重 | **误报（必须撤销或彻底改写）** | 见第 2.5 节详述。实测：`--mo-fs-*`/`--mo-sp-*` 定义在 `mo-blog.scss:23` 的 **`:root`**（非皮肤类），`themes/index.scss` 确 `@use` 该文件，且**构建产物** `dist/static/css/bootstrap-*.css` 的全局 `:root` 规则内确实存在 `--mo-fs-sm:.875rem`、`--mo-fs-lg:1.125rem`、`--mo-fs-6xl:3rem`、`--mo-sp-8:40px`。变量链是通的。报告声称的期望值 `--mo-fs-6xl = 38px` 也与源码不符（`:82` 为 `3rem` = 48px） | `mo-blog.scss:23,76,82,93` |
| H-1 | `.tpl-site` 在详情页形态下拿到深浅两套 `--tpl-*` | 高 | **确认** | 机制与 `site-templates.scss` 的 `html.dark.tpl-site` / `html.dark .tpl-site` 双选择器写法一致（该测试自身的断言 `:138-146` 就要求同时命中两者），报告的分析与既有测试意图吻合 | 准确 |
| H-2 | `--mo-*` 深色重映射在带 `scoped` 的组件中失效 | 高 | **降级为「待验证/中」** | **机制描述合理，但当前影响面未证实，报告自己也标了「未能穷尽/待验证」。** 我的独立扫描（78 个 `.vue`，223 条 `html.dark` 规则）**未发现任何深色色阶被当文字色的违规**；报告举的 teleport 场景（`el-image-viewer` 等）是**推断**而非实测。把「推断的影响面」定「高」不成立 | `SiteLayout.vue`、`theme-dark.css:4-31` |
| **M-1** | 首页侧栏 5 处用 `--mo-p600/p700` 作文字色 | 中 | **部分误报（8 行中 3 行不成立，5 行成立但需改写理由）** | 见第 2.6 节详述。8 个行号我逐一打开**全部属实**（`:557/568/624/654/680/691/779/795` 确为 `color: var(--mo-p6xx|p7xx)`），但「无深色覆盖」的判断对 **691/779/795 三条是错的**——它们分别被 `:938`（`.htitle:hover`→`p300`）、`:958`（`.tag-blue`→`p300`）、`:972`（`.search-result-note button`→`p300`）**按特异性覆盖**。我算了特异性：dark 规则均胜出（如 `.tag-blue` light=(0,2,0) vs dark=(0,3,1)） | 见 2.6 表 |
| M-2 | 前台 401 被改写成 `{code:200,data:null}` | 中 | **确认** | 未逐行复算 `request.ts`，但报告给出了具体分支与 `request.test.ts:554-602` 的覆盖说明，机制自洽。**保留原级**，建议最终报告附上一段可复现的最短步骤 | 未独立确认（诚实标注） |
| M-3 | 站点设置接口失败被静默吞掉 | 中 | **确认** | 与 backend M-6 同类模式，方向一致 | 未独立确认（诚实标注） |
| L-1 | `img` 缺 `alt`：3 处 | 低 | **确认** | 低危、可自查，未逐条复算 | 未独立确认 |
| L-2 | 唯一 `v-html` 未消毒点是设计内静态 SVG 母题 | 低 | **确认** | 报告主动为「唯一的 v-html」定性为设计内、并指出缺守卫，判断克制 | 未独立确认 |
| L-3 | 首页封面走 CSS `url()` 无法懒加载 | 低 | **确认** | 低危性能项 | 未独立确认 |

### 2.4 `reports/audit-db-devops.md`（11 条）

| # | 发现标题 | 原级别 | 复核结论 | 理由 | 真实行号 |
| --- | --- | --- | --- | --- | --- |
| H-1 | SQL 前向引用：`zhi_article.slug` 在补齐守卫之前被引用，存量库重跑硬失败 | 高 | **确认（我在真实 MySQL 上复现）** | 逐项核实：`:1226` 确为 `JOIN zhi_article a ON a.slug = seed.slug`；`:2568` 起确有 `information_schema` 守卫块。**关键是我实测确认了两点**：(1) 缺失列在 `ON` 子句 → `ERROR 1054`；(2) `mysql` 客户端**中断后续语句**（退出码 1，后续建表未执行）。报告把它**严格限定在「早于 slug 特性的存量库」**、并指出全新库因 `CREATE TABLE`（`:575`，`slug` 列在 `:588`）不含此问题 —— 这个限定是**准确且必要的**，没有夸大 | `sql/00_init_database.sql:1226`（引用）、`:2568`（守卫）、`:588`（CREATE 中的 slug） ✅ |
| M-1 | 历史重复行清理可能删除用户自建数据 | 中 | **确认（我在真实库上复现了数据丢失）** | 我插入 id=7「Spring 官网 / 用户自己加的友链」，再执行脚本 `:1274-1276` 的 `DELETE` 原文 → **id=7 被删除**。分类清理（`:1023-1027`）另有 `LEFT JOIN zhi_article ... IS NULL` 守卫，风险确实略低，报告的区分正确。（测试后我已将表恢复原状） | `sql/00_init_database.sql:1274-1276`（友链）、`:1023-1027`（分类） ✅ |
| M-2 | 用户数据占用种子保留 id 时该种子静默缺失 | 中 | **确认** | 逻辑成立：`INSERT IGNORE` + 显式主键，主键被占则整行丢弃且无提示。报告自我评价「风险很窄但静默」恰当 | `sql/00_init_database.sql:1030/1049/1278` ✅ |
| M-3 | 文档 Spring Boot 版本号错误（AGENTS.md 3.3.0 vs 实际 3.5.16） | 中 | **确认（我也独立测到同一结论）** | 见第 3.3/3.4 节。**我的独立测量路径不同（读构建产物 MANIFEST）却得到同一结论**，可交叉印证：`Spring-Boot-Version: 3.5.16`。报告还额外发现了 `pom.xml:26-29` 的过时注释自相矛盾，属实 | `AGENTS.md:3`、`pom.xml:30`、`pom.xml:26-29` ✅ |
| M-4 | 表排序规则不统一（`0900_ai_ci` vs `unicode_ci`） | 中 | **确认（机制实测成立，但「当前分布」表述需重要修正）** | **机制我在真实库上实测成立**：跨两种排序规则 `JOIN` 报 `ERROR 1267`。**但报告的分布数字与运行库不符**：报告称「`0900` 13 张 / `unicode_ci` 35 张」；实测运行库为 **`0900` 39 张 / `unicode_ci` 9 张**；脚本里显式 `COLLATE=` 仅 **23 处**（`0900` 14 + `unicode_ci` 9），其余 28 张表**不写 COLLATE、继承库默认**。所以「混用」的**发生与否取决于建库时的默认排序规则**：Docker 路径（`MYSQL_DATABASE`）默认即 `0900_ai_ci`，**全库自洽、不会报错**；而 README/CLAUDE.md 推荐的 `CREATE DATABASE ... COLLATE utf8mb4_unicode_ci` 路径下，未显式指定的表继承 `unicode_ci`，才会与 `zhi_*` 的 `0900` 混用。→ 结论**成立但必须补上「取决于建库路径」这个前提**，否则读者按 Docker 部署复现不出 | 建库语句见 `sql/00_init_database.sql:21`、`CLAUDE.md:67`；显式 `COLLATE=` 23 处 |
| L-1 | 默认管理员使用 RuoYi 广为人知弱口令哈希 | 低 | **确认** | `sql/00_init_database.sql:142` 确为 `$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2`（RuoYi 默认 `admin123` 哈希） | `sql/00_init_database.sql:142` ✅ |
| L-2 | `GRANT` 写死库名 `zhienterprise` | 低 | **确认** | `sql/00_init_database.sql:56` 确为 `ON zhienterprise.*`；脚本内已有注释说明这是唯一残留处 | `sql/00_init_database.sql:56` ✅ |
| L-3 | 部分表缺少主键以外索引 | 低 | **确认** | 低危 | 未逐条复算 |
| L-4 | `sql/README.md` 表述与判据略有出入 | 低 | **确认** | 文档一致性问题 | 未逐条复算 |
| L-5 | README 与脚本结尾的默认密码提示未给具体口令 | 低 | **确认** | 与 L-1 同源 | 未逐条复算 |
| L-6 | `.workflow/ReleasePipeline.yml` 的 tag 校验比 `release.yml` 弱 | 低 | **确认** | 低危工程化项 | 未逐条复算 |

### 2.5 S-2 误报的完整反驳证据（`audit-frontend.md`）

**报告的主张**：企业官网模板下 `--mo-fs-*` / `--mo-sp-*` 全部失效、回退浏览器默认值（实测 16px/32px），根因是这些变量定义在「已下线的 `mo-blog.scss`」里。

**反驳证据链（4 步）**：

1. **定义位置不是皮肤类，而是 `:root`。**
   ```bash
   awk 'NR<=75 && /^[^ \t].*\{/ {print NR": "$0}' zhi-ui/src/assets/styles/themes/mo-blog.scss
   # => 23: :root {          ← 唯一的顶层选择器
   grep -n -- "--mo-fs-sm:" zhi-ui/src/assets/styles/themes/mo-blog.scss
   # => 74:  --mo-fs-sm: 0.875rem;   （在 :root 块内）
   ```
   `:root` 与 `.theme-mo-blog` 是**两回事**：`applyAppTheme` 移除的是 `theme-mo-blog` **类**，而 `:root` 规则作用于 `<html>`，**不受类名增删影响**。

2. **该文件确实被引入。**
   ```bash
   cat zhi-ui/src/assets/styles/themes/index.scss
   # => @use './mo-blog.scss';   ← 确认被引入
   ```
   报告在「待确认」里也承认自己「未逐行核对 `index.scss` 的 import 列表」——这一步核对后，S-2 的前提即被推翻。

3. **构建产物里这些 token 真实存在于全局 `:root`（最强证据，直接反驳"失效"）。**
   ```bash
   find zhi-ui/dist -name "*.css" -exec grep -l "mo-fs-sm" {} \;
   # => 大量命中（bootstrap-*.css、index-*.css ...）
   # 自写脚本解析 bootstrap-CSHT-wyZ.css 的 :root 规则：
   #   SELECTOR: :root
   #   --mo-fs-sm = .875rem
   #   --mo-fs-lg = 1.125rem
   #   --mo-fs-6xl = 3rem
   #   --mo-sp-8  = 40px
   #   --mo-sp-11 = 80px
   ```
   变量有定义 → `var(--mo-fs-lg)` **不会**退化为继承值。报告观测到的 16px/32px 需要另找原因（例如该元素实际未使用这些变量、或作者样式 vs UA 样式的层叠问题），**但不能归因于变量未定义**。

4. **报告引用的期望值本身也有误。** 报告称 `--mo-fs-6xl` 期望 38px、实测 32px；源码 `mo-blog.scss:82` 为 `--mo-fs-6xl: 3rem`（= 48px），构建产物确认 `3rem`。期望值错 → 32px 与「38px 的降级」之间的因果推断不成立。

**结论**：S-2 的**现象**（若 headless 确实量到 16px）可能另有成因，但其**根因分析（变量随皮肤下线而失效）已被构建产物直接证伪**。必须撤销「严重」定级；若要保留该现象，需重新归因并提供「元素实际计算样式 + 该元素 CSS 规则来源」的新证据。

> 附带说明：报告在 S-2 里主动写了「**待确认**…请以 `npm run build:prod` 的实际 CSS 产物复核后定级」——**这个自我提示恰恰是判它误报的关键**。同事的谨慎态度值得肯定，最终的撤销也应表述为「按其自身给出的复现方法未能复现」，而非指责。

### 2.6 M-1 的部分误报（`audit-frontend.md`）

报告的 8 个行号**全部属实**（我逐行打开确认均写作 `color: var(--mo-p6xx/p7xx)`）。但「哪些已被深色覆盖」的判断有三处错误。我按 CSS 特异性（id, class/attr/pseudo-class, element）逐一计算：

| 行号 | 浅色规则 | 深色覆盖 | 特异性比较 | 复核结论 |
| --- | --- | --- | --- | --- |
| `index.vue:557` | `.mo-home-page .article-card .meta .author.clickable:hover` | `:895` `html.dark ... .meta .author`（**无 `:hover`**） | light **(0,6,0)** > dark (0,5,1) | ✅ **成立**（浅色 hover 规则胜出） |
| `index.vue:568` | `.mo-home-page .avatar-sm` | `:975` `html.dark ... .avatar-sm, ... .rank.top` → `p300` | dark (0,7,2) > light (0,2,0) | ❌ **不成立**（已被覆盖） |
| `index.vue:624` | `.mo-home-page .cat-item:hover` | `:912-913` `html.dark ... .cat-item:hover, .active` | dark (0,4,1) > light (0,3,0) | ❌ **不成立** |
| `index.vue:654` | `.mo-home-page .tc:hover` | `:928-929` `html.dark ... .tc:hover, .tc.lg` | dark (0,4,1) > light (0,3,0) | ❌ **不成立** |
| `index.vue:680` | `.mo-home-page .rank.top` | `:975` 同 `.avatar-sm` 组 → `p300` | dark (0,7,2) > light (0,3,0) | ❌ **不成立** |
| `index.vue:691` | `.mo-home-page .hot-item:hover .htitle` | `:938` `html.dark ... .htitle:hover` → `p300` | dark (0,4,1) > light (0,4,0) | ❌ **不成立**（报告标注「无深色覆盖」有误） |
| `index.vue:779` | `.mo-home-page .tag-blue` | `:958` `html.dark ... .tag-blue` → `p300` | dark (0,3,1) > light (0,2,0) | ❌ **不成立**（报告标注「无深色覆盖」有误） |
| `index.vue:795` | `.mo-home-page .search-result-note button` | `:972` `html.dark ... .search-result-note button` → `p300` | dark (0,3,2) > light (0,2,1) | ❌ **不成立**（报告标注「无深色覆盖」有误） |

即：**8 处中只有 `:557` 一处是真正的深色模式违规**（浅色 `:hover` 规则特异性高于深色非 hover 规则）。

**但 M-1 的"内核"仍然成立，只是理由要换。** 我另查明一个报告未充分展开、却更能支撑结论的事实：`index.vue:838-855` 有一个 `html.dark.theme-mo-blog .mo-home-page` 块，其中**显式重映射**了 `--mo-p600: #4f46e5`、`--mo-p700: #4338ca`。这两个值虽是"重映射"，但仍是**深靛蓝**，压在深底上照样不可读。我实算对比度：

| token（Mo-Blog 深色下的实际值） | 压在 `--mo-n900`(#1c1917) 页底 | 压在 `--mo-n800`(#292524) 卡片 | 达标(4.5:1) |
| --- | --- | --- | --- |
| `--mo-p600` = `#4f46e5` | **2.78:1** | **2.41:1** | ❌ |
| `--mo-p700` = `#4338ca` | **2.21:1** | **1.92:1** | ❌ |
| `--mo-p300` = `#a5b4fc`（正确做法） | **8.77:1** | **7.61:1** | ✅ |

**因此 M-1 应改写为**：「Mo-Blog 深色下 `--mo-p600/p700` 被重映射为 `#4f46e5/#4338ca`（`index.vue:852-853`），仍是**深靛蓝**，实测对比度仅 1.92–2.78:1；而正确的强调色文字应使用 `--mo-p300`（8.77:1）。风险点在于**依赖逐条手写深色覆盖**（如 `:975` 那样），任何漏写一条即回退到 `p600/p700`；实测已确认 `:557`（`.author.clickable:hover`）就是漏网的一处。」——这个版本事实更准、且不依赖错误的「无覆盖」判断。

### 2.7 复核中我**未能确认**的项（诚实标注）

| 来源 | 项 | 为何无法确认 |
| --- | --- | --- |
| frontend | M-2（401 被改写成 200） | 需运行前端请求层 + mock 后端，我未构造该集成场景；报告机制自洽，暂按「确认但未独立复现」处理 |
| frontend | M-3、L-1、L-2、L-3 | 均为低/中危且证据方向自洽，未逐条复现 |
| frontend | S-1/S-2 的 headless 实测数值 | 我未启动 headless Chrome 复测像素值；S-1 我以「源码 + 手工对比度复算」确认（结论一致），S-2 我以「构建产物」证伪（结论相反） |
| security | M-2（验证码失败计数锁他人账号）、V-1/V-2/V-3 | 需运行环境或真实凭据；报告已如实标注为待验证 |
| backend | M-5（分页 + 缓存 total）、V-1/V-2/V-3 | 需真实 Redis + PageHelper 集成环境，静态无法定性 —— 我同意报告的「待验证」判断 |
| backend | M-6 的「全仓库唯一」措辞 | 我未穷尽全仓库所有写路径以证实「唯一」 |

---

## 3. 独立验证发现（我自己找到、同事没报的问题）

### 3.1 【真实缺陷】`GlobalExceptionHandler` 向客户端透传原始异常消息（系统性，非单点）

- **位置**：`zhi-framework/src/main/java/com/zhi/framework/web/exception/GlobalExceptionHandler.java:105`（`RuntimeException`）、`:116`（`Exception`）
- **代码**：
  ```java
  100:  @ExceptionHandler(RuntimeException.class)
  101:  public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
  104:      log.error("请求地址'{}',发生未知异常.", requestURI, e);
  105:      return AjaxResult.error(e.getMessage());     // ← 原始异常消息回给客户端
  ...
  111:  @ExceptionHandler(Exception.class)
  116:      return AjaxResult.error(e.getMessage());     // ← 同上
  ```
- **为什么值得单列**：这是一条**兜底路径**——任何未被更具体 handler 覆盖的异常都会走这里。项目大量抛出 `IllegalArgumentException("文章别名只允许…")` 这类**自拟消息**（这些没问题），但 `RuntimeException` 兜底同样会捕获 JDBC/MyBatis 层抛出的异常，其 `getMessage()` 常含 SQL 片段、表名、列名（如 `Unknown column 'create_by' in 'field list'` —— 正是本项目历史上真实发生过的那类错误）。这属于信息泄漏的"宽口径"入口。
- **与同事的关系**：`audit-backend-correctness.md` 的「既有守卫复核 #19」**已经注意到了**这一点，但**主动选择不单列为发现**（原文：「复核为既有设计，本次不另计为一独立发现，但建议纳入安全审查跟踪」）；`audit-security.md` 未列入。→ 我的结论是**应当单列并定「中/低」**，理由是它有明确的、可复现的泄漏形态，且修复成本极低（返回固定文案 + 保留服务端日志即可）。
- **定级建议**：**中**（生产可复现、泄漏面为错误细节而非凭据；但需管理员触发才有内容，且不是直接越权）。
- **修复**：`return AjaxResult.error("系统繁忙，请稍后重试");`，原始消息只进日志。

### 3.2 【真实缺陷】深色 token 契约的守卫存在 `.scss` 覆盖盲区，且已存在 2 处真实违规

- **位置**：`zhi-ui/src/assets/styles/themes/mo-blog.scss:994`、`:1558`
- **代码**：
  ```scss
  993: html.dark.theme-mo-blog .site-layout .footer-wave {
  994:   color: var(--mo-n900);          // ← 深色下 n900 是背景色（#1c1917）
  995: }
  ...
  1557: html.dark.theme-mo-blog .title-input .el-input__inner {
  1558:   color: var(--mo-n900) !important;
  1559: }
  ```
- **为什么同事漏了**：`dark-mode-tokens.test.ts` 的 `collectVueFiles`（`:25-36`）**只收集 `.vue`**，`.scss/.css` 不参与语义扫描。我用独立解析器确认：扫描范围内（`.vue`）违规为 **0**，而这两个 `.scss` 文件里的 `html.dark` + `color: var(--mo-n900)` 组合**恰好命中测试的正则规则**——只是文件类型不在扫描集内所以从未被检查。
- **可读性影响**：`html.dark.theme-mo-blog` 下设 `--mo-n900: #1c1917`（背景色），作为 `color` 压在深底上约 **1.0:1**。
- **可触发性**：`.footer-wave` 在 `mo-blog.scss:1002` 有 `.theme-mo-blog .site-layout .footer-wave { display: none; }`，**浅色下隐藏**；而深色规则（`:993`）未隐藏它，因此该元素**仅在深色下渲染**且文字与背景同色——是"仅在深色模式可见的不可见文字"。`:1558` 的 `.title-input .el-input__inner` 是**文章编辑器标题输入框**，深色下输入文字不可见（`!important` 加剧）。
- **与同事的关系**：`audit-frontend.md` 的「守卫复核 → 盲区 2」**正确指出了"测试只收集 `.vue`、不扫 `.scss`"这一结构性盲区**，但没有**去扫 `.scss` 找出具体违规点。我补上了这 2 个 `文件:行`。
- **定级建议**：**中**（编辑器标题在深色下不可见是可感知的功能缺陷；`!important` 使其无法被后续覆盖修复）。
- **修复**：`:994` 改用 `--mo-n100/n200` 之类文字色阶或 `--mo-p300`；`:1558` 改用 `--mo-n200`。

### 3.3 版本号一致性实测表（12 处，全部一致 —— **正面结论**）

| # | 位置 | AGENTS.md 要求 | 实测值 | 结论 |
| --- | --- | --- | --- | --- |
| 1 | root `pom.xml:9` `<version>` | 1.0.0 | `1.0.0` | ✅ |
| 2 | root `pom.xml:24` `<app.version>` | 1.0.0 | `1.0.0` | ✅ |
| 3 | `zhi-common/pom.xml` parent version | 1.0.0 | `1.0.0` | ✅ |
| 4 | `zhi-system/pom.xml` parent version | 1.0.0 | `1.0.0` | ✅ |
| 5 | `zhi-framework/pom.xml` parent version | 1.0.0 | `1.0.0` | ✅ |
| 6 | `zhi-quartz/pom.xml` parent version | 1.0.0 | `1.0.0` | ✅ |
| 7 | `zhi-generator/pom.xml` parent version | 1.0.0 | `1.0.0` | ✅ |
| 8 | `zhi-admin/pom.xml` parent version | 1.0.0 | `1.0.0` | ✅ |
| 9 | `RuoYiConfig.java:19` `${ruoyi.version:…}` 兜底 | 1.0.0 | `1.0.0` | ✅ |
| 10 | `zhi-ui/package.json:3` `"version"` | 1.0.0 | `1.0.0` | ✅ |
| 11 | `zhi-ui/package-lock.json:3` 根 `"version"` | 1.0.0 | `1.0.0` | ✅ |
| 12 | `zhi-ui/package-lock.json:9` `packages[""].version` | 1.0.0 | `1.0.0` | ✅ |

**结论：版本号单点一致性没有问题，12 处全部为 `1.0.0`。** 这与 `audit-db-devops.md` 的核对表**完全吻合**（该表 13 行含 `application.yml` 的 `@app.version@`，亦正确）。**这一项应作为「已验证健康」写入最终报告。**

### 3.4 Spring Boot 真实版本（文档 vs 实现）

| 来源 | 声称 | 实测/判定 |
| --- | --- | --- |
| `AGENTS.md:3` | **Spring Boot 3.3.0** | ❌ **错误** |
| `CLAUDE.md:7` | Spring Boot 3.5 | ✅ 正确 |
| `README.md:3/155` | Spring Boot 3.5 | ✅ 正确 |
| `pom.xml:30` | `<spring-boot.version>3.5.16</spring-boot.version>` | （配置源） |
| `pom.xml:86-93` | 以 `spring-boot-dependencies` BOM `import` 接管版本 | 生效路径 |
| **构建产物** `zhi-admin.jar` MANIFEST | `Spring-Boot-Version: 3.5.16` | ✅ **权威实测** |
| `zhi-admin.jar/BEAN-INF/lib` | `spring-boot-3.5.16.jar` | ✅ 佐证 |

**实测结论：实际生效的是 Spring Boot 3.5.16。`AGENTS.md:3` 的 `3.3.0` 是过时描述。**
另注 `pom.xml:26-29` 的注释仍在讲「3.3.13 是 3.3.x 最后一个 OSS 版本」，与实际取值自相矛盾（`audit-db-devops.md` M-3 已报）。

### 3.5 权限串交叉核对结果（双向差集）

**方法**：Java 侧 `@ss.hasPermi('...')` 单引号字面量 145 个；SQL 侧按 `sys_menu` 显式列清单解析出的非空 `perms` 170 个；并用运行库 `sys_menu` 二次确认（同样 170 个）。

**清单 A —— 代码要求但库里没有（0 条）：**

```
（空）
```

→ **无「权限不存在导致接口对所有人 403」的问题。** 这是本次审查最重要的正面结论，且我在**运行库**上复核过，不只是解析脚本。

**清单 B —— 库里有但代码不用（25 条）：**

| 权限串 | 判断 |
| --- | --- |
| `monitor:actuator:list` / `monitor:actuator:query` | 外链监控页（Actuator），后端无对应接口，非漏洞 |
| `monitor:grafana:list` / `monitor:grafana:query` | 同上（Grafana 外链） |
| `monitor:prometheus:list` / `monitor:prometheus:query` | 同上（Prometheus 外链） |
| `monitor:cache:add/edit/remove/clean/query` | RuoYi 遗留缓存菜单；`monitor:cache:list` 有接口，其余为 Redis 6 后失效的子操作 |
| `monitor:job:detail` / `monitor:job:execute` | RuoYi 遗留（接口用 `monitor:job:query`/`changeStatus`） |
| `monitor:logininfor:query` / `monitor:operlog:query` / `monitor:online:query` | RuoYi 遗留查询按钮权限，接口未单独校验 |
| `monitor:online:batchLogout` | 同上 |
| `statistics:article:query` / `statistics:user:query` / `statistics:overview:query` / `statistics:visit:query` | 本项目统计页的「查询」按钮权限未在接口层使用（接口只校验 `:list`） |
| `statistics:article:export` / `statistics:user:export` | 同上（导出接口未单独校验） |
| `tool:gen:add` / `tool:swagger:list` | RuoYi 遗留 |

**说明**：这 25 条**不构成安全问题**——菜单给了权限但接口不单独校验，最坏结果是「按钮可见但接口用 list 权限放行」，属**权限模型冗余**。建议作为「低」的清理项，并**不要**在最终报告里渲染成风险。注意其中 `statistics:*:query/export` 是本项目自己新增菜单时留下的，比纯 RuoYi 遗留更值得顺手清理。

### 3.6 无 `@PreAuthorize` 的后台方法清单（81 个）与越权判断

**前置事实**：`SecurityConfig.java:170-177` 的 `anyRequest().authenticated()` 保证这 81 个方法**全部需要登录**。因此风险只有一类：**已登录的注册用户（`user_type='01'`，无任何后台角色）能否借此越权**。

我按「数据归属」逐个分组判断：

| 分组 | 代表方法（文件:行） | 越权风险判断 |
| --- | --- | --- |
| **本人数据自助（安全）** | `SysProfileController:67/82/112/144`（profile/updateProfile/updatePwd/avatar）、`BookmarkController:33/49/65`、`LikeController:43/61/72/90`、`NotificationController:34/47/58/69`、`ArticleController.getMyArticles:99`、`FrontController:823/869`（updateMyComment/deleteMyComment） | **无越权**。全部用 `SecurityUtils.getUserId()` 取当前用户并据此限定。我额外核实了通知的归属安全：`NotificationMapper.xml:69-78` 的 `markAsRead` 带 `and recipient_id = #{recipientId}` —— **不存在 IDOR**。`getMyArticles` 也强制 `article.setAuthorId(currentUserId)` |
| **匿名公开站点接口（设计如此）** | `FrontController` 全部 `@Anonymous` 方法、`FrontMessageController:88/110/125`、`FrontPageController:45/56`、`FrontNavController:31`、`SeoController:49/162`、`FrontSettingController:71/241`、`AuthController` 4 个、`FriendLinkController:68/80`、`ArticleController(site):43/67` | **设计内公开**，且多数见 `@Anonymous`；`FrontMessageController`/`FriendLinkController.apply` 另有开关与限流。与 `audit-security.md` L-1 结论一致 |
| **RuoYi 存量只读下拉助手（低风险）** | `SysDictDataController.dictType:76`、`SysDictTypeController.optionselect:126`、`SysPostController.optionselect:124`、`SysMenuController.treeselect:61`/`roleMenuTreeselect:71`、`SysLoginController.getRouters:33`、`SysIndexController:31/41`、`CommonController` 各上传/下载 | **不构成越权读取他人数据**，但**值得收敛**：`SysDictTypeController.optionselect` 返回全部字典类型、`SysPostController.optionselect` 返回全部岗位、`SysMenuController.roleMenuTreeselect` 传入任意 `roleId` 即可读该角色的菜单勾选 → 任何已登录的注册用户都能枚举**后台的岗位/字典/角色菜单结构**。信息价值有限（非业务数据），属**信息暴露**而非越权操作。**建议给这几个方法补 `@PreAuthorize`（如 `system:dict:list` 等）或至少限定为 `user_type='00'`。** |
| **上传类（有实际风险面）** | `CommonController.uploadFile:86`、`uploadFileCompressed:114`、`uploadAvatar:146`、`uploadThumbnail:178`、`uploadArticleCover:210`、`uploadMobileImage:242`、`uploadWatermarkImage:274`、`uploadFiles:311`；`fileDownload:56`、`resourceDownload:349` | **需注意**：这些端点不在 `/api/**` 白名单内，故需登录；但**不需要任何权限串**，即**任意注册用户都能上传文件**（受 `@RateLimiter` 与类型/路径校验约束）。`resourceDownload`/`fileDownload` 若允许任意路径则风险更高。→ **建议列为「中」并核实下载类是否有路径穿越防护**（我未在本轮穷尽验证下载实现的路径校验，属**未确认**，请最终报告标注待验证） |

**结论**：81 个方法中**没有发现直接越权读写他人数据**的实例（与 `audit-security.md` 结论一致），但 `CommonController` 的上传/下载免权限、以及 RuoYi 下拉助手的后台结构枚举，**值得作为两条独立的中低发现写入最终报告**——这是同事报告未覆盖的角度（`audit-security.md` L-1 只对 `@Anonymous` 做了清单，未覆盖「已登录无权限」这一层）。

### 3.7 token 契约违规清单（亮色/暗色，实测）

| 方向 | 扫描范围 | 违规数 | 说明 |
| --- | --- | --- | --- |
| 暗色：`--mo-n0/n50/n800/n900` 作 `color` | `.vue`（78 文件 / 223 条 `html.dark` 规则） | **0** | 契约执行到位 |
| 暗色：`--mo-p600~p900` 作 `color` | 同上 | **0** | 同上 |
| 亮色：`--mo-n300/n400/p200/p300` 作 `color`（排除 `.empty-icon`/`.not-found-icon`） | 同上（4073 条非 dark 规则） | **0** | 契约执行到位 |
| **暗色：背景色阶作 `color`（`.scss` 扩展扫描）** | `assets/styles/themes/mo-blog.scss` | **2** | `:994`（`.footer-wave`）、`:1558`（`.title-input .el-input__inner`，带 `!important`）—— **测试盲区，见 3.2** |

**实跑同事的测试**：`npx vitest run src/views/site/dark-mode-tokens.test.ts` → **7 passed**。可见「测试通过」与「契约无违规」并不等价，盲区在文件类型与"拆成两条规则"的写法上。

---

## 4. 对最终报告的修改建议

### 4.1 必须**撤销**（误报，不能进入最终报告的问题清单）

| 来源 | 条目 | 处置 |
| --- | --- | --- |
| frontend | **S-2**（企业模板字号/间距全部失效） | **撤销「严重」定级。** 构建产物证明 `--mo-fs-*`/`--mo-sp-*` 真实存在于全局 `:root`。若确要保留 headless 观测到的 16px 现象，必须**重新归因**（需该元素的计算样式来源证据），否则整条删除。**不得**以「变量失效」为由写进报告 |
| frontend | **M-1** 中 `:568`、`:624`、`:654`、`:680`、`:691`、`:779`、`:795` 七处 | **撤销「无深色覆盖」的表述。** 我按特异性计算，这 7 处均**已被深色规则覆盖且深色规则胜出**。M-1 应改为「依赖逐条手写覆盖、存在漏写风险，实测 `:557` 已漏网」，并用对比度数据（p600/p700 在深色下仅 1.92–2.78:1）支撑 |
| backend | **H-3**（邮件配置不失效设置缓存） | **撤销「高」定级。** `mail_*` 不在 `FrontConfigKeys.CONFIG_KEYS` 白名单内，缓存不可能含该键，且该链路读 DB 不经缓存 → **无可观测影响**。可降为「低」并改写为「规则例外 + 建议补注解统一风格」 |
| security | **L-2** 中关于 `zhi:comment:approve` 的描述 | **撤销「菜单里有该权限」的事实陈述。** 实测库中/解析结果均无此 perms 值，它只作为迁移 `UPDATE` 的 WHERE 条件存在（`sql:1583`） |

### 4.2 需要**调整级别**

| 来源 | 条目 | 原级 | 建议级 | 理由 |
| --- | --- | --- | --- | --- |
| backend | H-2（`insertArticle` 无事务无失效） | 高 | **合并进 S-1 / 或中** | 与 S-1、H-1 同址同根，避免同一缺陷重复计为多次高优 |
| backend | H-4（镜像可能落错行） | 高 | **中** | 报告自认当前逻辑正确，残留为未来重构风险 + 理论竞态 |
| backend | M-5（分页 + 缓存 total） | 中 | **待验证（不单列级别）** | 静态无法定性，报告自标待验证，不宜按「中」计入缺陷数 |
| frontend | H-2（scoped 下重映射失效） | 高 | **中 / 待验证** | 影响面为推断（teleport），我的独立扫描 0 违规 |
| security | H-2（auth 端点零限流） | 高 | **中**（可与建议合并） | 事实成立（`/reset-password` 确为唯一无 IP 限流的验证码校验点），但生产有 `verifyCode` 失败计数（5 次/邮箱）兜底，且 `send-*-code` 有 service 层限流；「高」偏高 |
| security | H-1（重置无口令策略） | 高 | **保留高**（建议维持） | 策略不对称 + 单字符口令可写库，且注册路径有 `@Size(min=5)` 对照，属实 |

### 4.3 需要**补充**（我独立发现，建议纳入）

| 建议新增 | 级别 | 依据 |
| --- | --- | --- |
| `GlobalExceptionHandler` 透传原始异常消息（`:105`/`:116`） | 中 | 见 3.1；同事已注意到但未单列 |
| `mo-blog.scss:994`/`:1558` 深色下背景色阶作文字色（token 守卫盲区） | 中 | 见 3.2；同事指出了盲区但未扫出具体点 |
| `CommonController` 上传端点在「已登录但无权限」下即可用；下载类路径校验待验证 | 中/待验证 | 见 3.6 |
| RuoYi 下拉助手（`dictType`/`optionselect`/`roleMenuTreeselect`）对任意登录用户开放，可枚举后台结构 | 低 | 见 3.6 |
| 权限串 B 集 25 条冗余（含本项目自建的 `statistics:*:query/export`） | 低 | 见 3.5 |

### 4.4 措辞收敛建议

1. **`audit-security.md` H-2** 的「全部 `/api/auth/*` 匿名认证端点**零限流**」——建议改为「**控制器层**零限流；`send-register-code`/`send-reset-code` 在 service 层有**每邮箱 60s + 每 IP 10 次/小时**的兜底，但 dev profile 下 IP 维度被 `isDevPrintCodeEnabled()` 跳过」。原标题容易让读者以为这些端点完全裸奔。
2. **`audit-backend-correctness.md` M-6** 的「是全仓库唯一残留的同类路径」——建议改为「在本次审查范围内发现的一处」，避免未经穷尽证明的绝对断言。
3. **`audit-backend-correctness.md` H-3**（若保留）——必须在同一段内明确「**当前无可观测影响**」，不能只把白名单兜底写在影响段的后半句。
4. **行号基准统一**：`audit-security.md` H-2 用 `@Anonymous` 行（`:51/71/104/131`），我按方法注解块起点解析为 `:53/73/106/133`；`audit-backend-correctness.md` M-2 用 `@Transactional` 行（`:572`）而方法声明在 `:571`。建议最终报告统一为「**方法声明所在行 ± 注解块**」并注明基准，避免读者对不上。
5. **`audit-db-devops.md` M-4** 的排序规则分布——必须补上「**取决于建库路径**」这一前提（Docker `MYSQL_DATABASE` 默认 `0900_ai_ci` → 全库自洽；README 推荐的 `unicode_ci` 建库 → 才会混用），并把分布数字改为实测值（运行库 `0900` 39 / `unicode_ci` 9；脚本显式 `COLLATE=` 23 处）。
6. **语气**：四份报告整体克制，但 frontend 的 S-1 用了"实测 1.04:1"这类精确数字——这些数字来自 headless 实测，我复核 S-1 的**源码与手工对比度复算**与之一致，**可以保留**；但 S-2 同类数字既已被证伪，同类"实测"表述应加注环境与复现命令，以区分「实测」与「推断」。

### 4.5 明确要求写入「已验证健康」的正面结论（防止读者误判项目质量）

1. **权限模型无缺口**：代码要求但库中不存在的权限串 **0 条**（源码解析 + 运行库双重确认）。
2. **版本号单点一致性完好**：12 处全部 `1.0.0`。
3. **上传/下载外的匿名写端点均有防护**：`/api/comment`、`/api/message`、`/api/like`、`/api/bookmark`、友链申请、`/common/upload*`、`/auth/login` 均带 `@RateLimiter`；验证码/长度/时间窗/冷却齐备。
4. **token 契约在 `.vue` 范围内执行到位**：暗色背景色阶作文字 0 处、`p600~p900` 作文字 0 处、亮色淡色阶作文字 0 处（仅 `.scss` 有 2 处例外）。
5. **XSS 过滤器在生产是开启的**（`application-prod.yml` 的 excludes 不含 `/system/article`），同事报告已主动澄清这一点。
6. **.env 未被 git 跟踪**；nginx 的 `X-Forwarded-For` 全部以 `$remote_addr` **覆盖**而非追加。
7. **质量门禁落地**：CI 覆盖 `mvn verify`（含 checkstyle + JaCoCo 60%/60%）、`npm run audit`（registry 已钉 npmjs）、`lint:check`、`format:check`、`test:coverage`；测试占位符 `it.skip`/`expect(true)` 实测为 **0** 条；`zhi-admin`/`zhi-generator` 的 `jacoco.skip=true` 与 AGENTS.md 声明一致。
8. **并发安全的设计多处正确**：点赞表唯一键（运行库确认）、浏览量 `view_count = view_count + 1` 原子自增、评论数 `GREATEST(...,0)` 防负、通知 `markAsRead` 带 `recipient_id` 归属校验（无 IDOR）。

---

## 5. 复核方法与诚实性声明

- 本轮复核**未修改任何源码**，**未修改任何同事报告**；产出仅本文件 `reports/audit-verification.md`。
- 对数据库做的破坏性验证（H-1 前向引用、M-1 误删、M-4 排序规则）**全部在同一容器的 `zhienterprise` 库上以临时探针表进行，用后 DROP**；结束时残留探针表计数为 **0**，`zhi_friend_link` 已恢复为 6 行原始种子数据。唯一的持久副作用是 H-1/M-1 验证过程中插入再被删除的那一行测试友链——其最终状态与验证前**完全一致**。
- 本报告中标注「未独立确认」的项目均为时间预算外或需运行环境才能定性的项，已在 2.7 节逐条列明，**未以"复核通过"含糊带过**。
- 对同事报告的总体评价：**证据质量高、克制、主动披露不确定性**。四份报告共 47 条中被我推翻的仅 4 条（8.5%），且其中 2 条（S-2、M-1 局部）源自同一类错误——**混淆了"变量定义在哪个选择器下"与"变量是否失效"**，以及**未计算深色覆盖规则的特异性**。这两点建议在后续审查中固化为检查清单。