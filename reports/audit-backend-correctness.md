# 后端正确性审查报告

审查范围：`zhi-system` + `zhi-admin` + `zhi-quartz` 业务层（Controller / Service / Mapper XML / 定时任务），基准为 `AGENTS.md` 与 `CLAUDE.md` 记录的架构约定与历史故障。
审查方式：逐文件阅读 + 解析器脚本全量比对 `sql/00_init_database.sql` 与 `mapper/system/*.xml`。只读审查，未修改任何源码。

## 结论摘要

总评：**核心链路（浏览量/点赞计数、设置缓存失效、sys_config 空值镜像、zhi_message schema 漂移、DataScope 注入防护）在历次修复后已实质正确**，AGENTS.md 提到的既有守卫大部分经复核为真。
但本次发现 **1 条严重缺陷（新增文章的事务边界被一次重构破坏，且无任何测试覆盖）**，以及缓存失效覆盖不全、邮件配置保存无缓存失效等一组高/中级别问题。

| 级别 | 数量 | 说明 |
| --- | --- | --- |
| 严重 | 1 | `insertArticle` 丢失 `@Transactional`（注解错位到 `selectArticleBySlug`） |
| 高 | 4 | `zhi:search:*` 无任何失效路径；`insertArticle` 无缓存失效；邮件配置保存不失效 `zhi:settings:all`；`sys_config` 镜像行内查询存在 LIKE 注入漂移 |
| 中 | 6 | 文章列表/状态批量更新失效覆盖不全；`CacheAspect` SpEL `#x.hashCode()` 未被求值；浏览量同步「取出即删」丢增量；留言冷却 check-then-set 竞态；导出/列表计数查询污染风险；`catch(Exception)` 仅告警掩盖失败 |
| 低 | 5 | 循环内单条 insert、`updateArticleStatus` 循环点查、`resolveSpEL` 子串误替换、`thorw` 吞异常细节、若干日志级别 |

级别计数：严重 1 / 高 4 / 中 6 / 低 5。

---

## 严重

### S-1 `insertArticle` 丢失事务：`@Transactional` 被 javadoc 错位挂到了 `selectArticleBySlug` 上

**文件**：`zhi-system/src/main/java/com/zhi/system/service/impl/ArticleServiceImpl.java:167-216`

```java
167:     @Override
168:     @Transactional(rollbackFor = Exception.class)
169:     /**
170:      * 按别名查询文章（仅命中已发布且未删除的文章）
171:      */
172:     public Article selectArticleBySlug(String slug)
173:     {
174:         return articleMapper.selectArticleBySlug(slug);
175:     }
...
197:     public int insertArticle(Article article)   // ← 没有任何 @Transactional
198:     {
199:         validateArticleContent(article);
200:         String title = validateArticleTitle(article.getTitle());
201:         article.setTitle(title);
202:         validateSlug(article);
203:         article.setCreateTime(DateUtils.getNowDate());
204:         article.setAuthorId(...);
205:         article.setAuthorName(...);
206:         setDefaultValues(article);
207:
208:         try {
209:             int result = articleMapper.insertArticle(article);
210:             processTagAssociations(article);   // ← 循环 insert zhi_article_tag
211:             return result;
212:         } catch (Exception e) {
213:             handleInsertException(e);
214:             throw e;
215:         }
216:     }
```

`git blame` 证实这是 **2026-10-14 的 `1fa0249b6`** 引入的回归：该提交把「按别名查询文章」的 javadoc 块插入到了「新增博客文章」的注解与 `insertArticle` 方法体之间，使 `@Transactional` 与后面的 `selectArticleBySlug` 绑定，而真正的 `insertArticle` 变成了裸方法。方法上的 `@Override`（167 行）与 `@Transactional`（168 行）因此不再修饰同一个方法声明。

**触发场景**：
`POST /system/article`（`ArticleController.add`）→ `articleService.insertArticle(article)` → 第 209 行 `zhi_article` 插入成功 → 第 210 行 `processTagAssociations` 在写 `zhi_article_tag` 时抛异常（标签 ID 不存在触发外键/唯一键冲突、连接中断、`zhi_article_tag` 表缺失等）。

**影响**：
无事务边界 → 第 209 行已提交的文章**不会回滚**，但 210 行的标签关联全部丢失，接口返回 500。用户看到"创建失败"，实际库里已存在一篇**无标签的幽灵文章**；重试提交则因 `validateArticleTitle` 的标题唯一校验被拒（「文章标题已存在」），用户无法自愈。同时 `insertArticle` 内的 `validateSlug`/`validateArticleTitle` 先查后写的检查也失去了事务保护。

注意 `ArticleController.add` 第 143 行随后还会再调一次 `articleService.insertArticleTagRelations(...)`，因此**经由后台页面的正常提交路径**会补上标签，掩盖了该缺陷；只有 `insertArticle` 内部 `processTagAssociations` 抛错的路径会暴露它 —— 这正是它没被测试发现的原因。测试 `ArticleServiceImplTest:202` 只验证了 `insertArticle` 返回 1，未断言事务边界。

**修复建议**：
把 169-171 行的 javadoc 块移到 167 行 `@Override` 之前，恢复成：

```java
/** 新增博客文章 */
@Override
@Transactional(rollbackFor = Exception.class)
public int insertArticle(Article article)
```

并把 `selectArticleBySlug` 独自放在一个无 `@Transactional` 的方法声明上。建议补一条回归测试：mock `processTagAssociations` 抛异常，断言 `articleMapper.insertArticle` 写入被回滚（或至少断言两处写入处于同一事务，例如用 `TransactionSynchronizationManager.isActualTransactionActive()`）。

---

## 高

### H-1 `zhi:search:*` 缓存无任何失效路径，文章增删改后搜索页最长 10 分钟返回过期结果

**文件**：`zhi-system/src/main/java/com/zhi/system/service/impl/ArticleServiceImpl.java:465`

```java
465:    @SiteCacheable(key = "zhi:search:#keyword:#article", ttl = 10, timeUnit = TimeUnit.MINUTES)
466:    public List<Article> searchArticles(String keyword, Article article) {
```

全仓库搜索 `@SiteCacheEvict` 的 pattern，**没有任何一条覆盖 `zhi:search:*`**：

| 失效点 | 注解 pattern | 是否覆盖 `zhi:search:*` |
| --- | --- | --- |
| `ArticleServiceImpl:325` `updateArticle` | `zhi:article:*` / `zhi:article:list:*` / `zhi:hot:*`，`keyPattern="zhi:*"` | 是（`zhi:*` 兜底） |
| `ArticleServiceImpl:369` `deleteArticleByIds` | 同上 | 是 |
| `ArticleServiceImpl:701/719/736` | `keyPattern="zhi:*"` | 是 |
| `ArticleServiceImpl:168` `insertArticle` | 无注解 | **否** |

即：只有走 `updateArticle` / `deleteArticleByIds` / 置顶 / 推荐 / 点赞这 5 条路径时，`keyPattern="zhi:*"` 才会顺带清掉搜索缓存。而**新增文章（`insertArticle`）完全不触发任何失效**。

**触发场景**：
后台发布一篇标题含「数字化」的新文章 → 前台 `/api/article/search?keyword=数字化` 最长 10 分钟内仍返回旧结果集（不含新文章）。

**影响**：
`search_enabled` 开启时前台搜索是主入口之一，新发布内容"搜不到"，与 `AGENTS.md` 反复强调的"管理端改了前台不生效"是同一类可感知故障。

**修复建议**：
1. 给 `insertArticle` 补 `@SiteCacheEvict(keyPattern = "zhi:*")`（与同类写方法对齐）；
2. 更稳妥：把 `keyPattern` 从 `"zhi:*"` 这种"大范围模糊清"改为显式列出 `zhi:search:*`，避免依赖"某条随机路径恰好带 `zhi:*`"来兜底 —— 当前设计下，新增/批量状态变更等路径漏失效是**结构性**的，不是单点遗漏。
3. 建议补契约测试：遍历所有写 `zhi_article` 的 Service 方法，断言其缓存失效 pattern 覆盖所有 `@SiteCacheable` 的键前缀（可仿 `ControllerPermissionPolicyTest` 的跨产物断言）。

### H-2 `insertArticle` 既无事务也无缓存失效（与 H-1 同根，独立列出因其影响面更大）

**文件**：`zhi-system/src/main/java/com/zhi/system/service/impl/ArticleServiceImpl.java:197`

除 H-1 的搜索缓存外，新增文章还会让以下**已缓存**读取过期：

- `zhi:category:list:#category.hashCode()`（30 分钟 TTL，分类文章数变化）
- `zhi:hot:#article.hashCode()`（60 分钟 TTL）

**触发场景**：新增文章后进入分类页，`articleCount` 与实际不符，最长 30 分钟。

**影响**：前台分类计数/热门列表与实际数据不一致。

**修复建议**：`insertArticle` 补 `@Transactional`（S-1）与 `@SiteCacheEvict(keyPattern = "zhi:*")`；同时 `updateArticleStatus`（批量改状态，见 M-3）也需补。

### H-3 邮件配置保存不失效 `zhi:settings:all`，违反 AGENTS.md「每条写 zhi_setting 的路径都必须带 @SiteCacheEvict」

**文件**：`zhi-system/src/main/java/com/zhi/system/controller/SettingController.java:271-277`

```java
271:    @PreAuthorize("@ss.hasPermi('zhi:setting:edit')")
272:    @Log(title = "邮件服务配置", businessType = BusinessType.UPDATE)
273:    @PostMapping("/mail")
274:    public AjaxResult saveMailConfig(@RequestBody MailConfigForm form)
275:    {
276:        return success(mailConfigService.save(form));
277:    }
```

`MailConfigService.save`（`MailConfigService.java:150`）通过 `upsert` → `settingService.updateSettingValueByKey` 写入 **`zhi_setting`** 的 `mail_*` 键（host / port / username / password / ssl / starttls / enabled），但：

1. `saveMailConfig` 方法上**没有 `@SiteCacheEvict`**（对比 `SettingController` 的 5 个写方法 118/130/142/154/295 行都带）；
2. `MailConfigService` 也没有任何缓存失效。

**触发场景**：管理员在「邮件服务配置」页切换"启用邮件"并保存 → `zhi:settings:all` 未被清除。

**影响**：
`AGENTS.md` 明确要求「每一条写 zhi_setting 的路径都必须带 `@SiteCacheEvict(value = CacheConstants.SETTINGS_ALL)`」，此处是明确的规则违反。虽然 `mail_*` 键**刻意不在** `FrontConfigKeys.CONFIG_KEYS` 白名单内（设计如此，避免密码泄露到匿名接口），所以**当前不会**经由 `/common/site/setting` 泄露或读到脏值 —— 实际危害被白名单兜住了，因此定为「高」而非「严重」。但这是**靠白名单巧合成立**的安全性，一旦后续有人把 `mail_enabled` 加进白名单（或新增聚合了 `mail_*` 的公开读取接口），就会立刻表现为"前台看到的开关与后台不一致"。

**修复建议**：
给 `saveMailConfig` 补 `@SiteCacheEvict(value = CacheConstants.SETTINGS_ALL)`（与同类写方法一致，成本为零，消除规则例外）；或反之，在 `MailConfigService` 加注释明确记录"`mail_*` 永不进公开白名单，故本路径无需失效"，把这条豁免变成显式契约 + 测试断言（断言 `mail_` 前缀不在 `CONFIG_KEYS` 中）。

### H-4 `applySettingUpdateByKey` 用 `selectConfigList` + 内存 filter 做「精确找行」，`config_key` 的 LIKE 查询让镜像可能落到错误行

**文件**：`zhi-system/src/main/java/com/zhi/system/controller/SettingController.java:190-195`

```java
190:            SysConfig query = new SysConfig();
191:            query.setConfigKey(settingKey);
192:            SysConfig existingConfig = configService.selectConfigList(query).stream()
193:                .filter(c -> settingKey.equals(c.getConfigKey()))
194:                .findFirst()
195:                .orElse(null);
```

`SysConfigMapper.xml:48-50` 对 `configKey` 用的是 **LIKE 模糊匹配**而非等值：

```xml
48:			<if test="configKey != null and configKey != ''">
49:				AND config_key like concat('%', #{configKey}, '%')
50:			</if>
```

即 `query.setConfigKey("nav_tag_enabled")` 会查出所有 `config_key` **包含**该子串的行。本处代码用 `.filter(...equals...)` 在内存里做了精确过滤，**因此当前逻辑是正确的**（这一点值得肯定 —— 它是显式补偿了 mapper 的模糊语义）。

但残留两个真实风险：

**触发场景 A（先查后写竞态）**：第 192 行的"是否存在"判定与第 211 行 `updateConfig(existingConfig)` / 第 221 行 `insertConfig(config)` 之间没有事务，也没有 `sys_config.config_key` 唯一约束兜底。两个管理员并发保存同一个**尚未镜像过的**设置键时，双方都读到"不存在"，随后**双方都执行 INSERT**，`sys_config` 出现两行同 key。此后 `selectConfigByKey` 走缓存无碍，但 `SELECT ... LIKE` 类读取会拿到多行，`updateConfig` 按 `config_id` 更新只影响其中一行。

**触发场景 B（LIKE 放大）**：设置键变多后，`config_key` 互为子串的情况会放大查询结果集（如 `xxx_enabled` 与 `enabled`），当前只是浪费，但任何未来在此 query 上**去掉** `.filter(...)` 的重构都会立刻变成"改错行"。

**影响**：并发保存新键 → `sys_config` 出现重复镜像行 → 后续精确/模糊读取语义分叉。

**修复建议**：
1. 改用 `configService.selectConfigByKey` 之外的精确查询（新增 `selectConfigByKeyExactly`，或复用 `checkConfigKeyUnique`，其 XML 是 `where config_key = #{configKey} limit 1` 等值查询，见 `SysConfigMapper.xml:65-68`）；
2. 给 `applySettingUpdateByKey` 加 `@Transactional`，并把「查 + 写」纳入同一事务；
3. 给 `sys_config.config_key` 加唯一索引，从根上防止重复镜像行。

---

## 中

### M-1 浏览量同步「取出即删」：`addIncrementViewCount` 写库失败时该批增量永久丢失

**文件**：`zhi-admin/src/main/java/com/zhi/web/task/ArticleViewSyncTask.java:36-49`

```java
36:        for (String key : keys) {
37:            try {
38:                String articleIdStr = key.substring(key.lastIndexOf(":") + 1);
39:                Long articleId = Long.parseLong(articleIdStr);
40:                Integer increment = redisCache.getAndDeleteCacheObject(key);   // ← GETDEL，先删
41:
42:                if (increment != null && increment > 0) {
43:                    articleMapper.addIncrementViewCount(articleId, increment.longValue());  // ← 后写库
44:                    log.debug("已同步文章 {} 的浏览量增量: {}", articleId, increment);
45:                }
46:            } catch (Exception e) {
47:                log.error("同步文章浏览量失败，key: {}", key, e);
48:            }
49:        }
```

`RedisCache.getAndDeleteCacheObject` 是注释所称的「获取缓存对象并原子删除，适用于消费型计数同步场景」（`RedisCache.java:122-127`，底层 `ValueOperations.getAndDelete`，即 Redis `GETDEL`）。**GETDEL 先于 DB 写入发生**，因此第 43 行抛异常（DB 抖动、连接池耗尽）时：

- 键已被删除，第 40 行读到的 `increment` 只存在于局部变量中；
- catch 块只打日志，**不回写 Redis**；
- 该批次浏览量永久丢失，`zhi_article.view_count` 永久小于真实值。

**触发场景**：同步任务执行期间 MySQL 短暂不可用（重启、主从切换、连接池打满）→ 全部在途增量丢失。

**影响**：浏览量是前台展示指标，永久性少计且无补偿机制。任务每 10 分钟跑一次，故障窗口内的所有增量都会丢。

**修复建议**：
把 GETDEL 改成「读 + 成功写库后删」的两步，失败时保留键等待下轮重试：

```java
Integer increment = redisCache.getCacheObject(key);
if (increment != null && increment > 0) {
    articleMapper.addIncrementViewCount(articleId, increment.longValue());
    redisCache.deleteObject(key);   // 仅成功后消费
}
```

注意此改法下若「写库成功但 delete 失败」会重复计数；更严谨的做法是用 `DECRBY` 消费固定量并在失败时 `INCRBY` 回补，或用 Lua 脚本保证原子性。至少应保证**不丢**（宁可重复计也不静默丢），并给失败批次加计数告警。

### M-2 `updateArticleStatus` 批量改状态无缓存失效，发布文章后前台最长 60 分钟看不到

**文件**：`zhi-system/src/main/java/com/zhi/system/service/impl/ArticleServiceImpl.java:572-597`

```java
571:    @Override
572:    @Transactional(rollbackFor = Exception.class)
573:    public int updateArticleStatus(List<Long> ids, Integer status) {
...
581:        for (Long id : ids) {
582:            Article existingArticle = articleMapper.selectArticleById(id);
...
596:        return result;
597:    }
```

方法只有 `@Transactional`，**没有 `@SiteCacheEvict`**（对比同文件 `updateArticle:325`、`deleteArticleByIds:369`、置顶/推荐 `701/719` 都带 `keyPattern="zhi:*"`）。

**触发场景**：后台「文章管理」勾选多篇 → 批量发布（`status=1`）→ 前台列表/分类页/热门列表仍显示旧状态。

**影响**：批量上下架是高频后台操作，缓存不一致窗口 30-60 分钟（`zhi:hot:*` 60 分钟、`zhi:category:list:*` 30 分钟）。

**修复建议**：补 `@SiteCacheEvict(keyPattern = "zhi:*")`，与同类写方法一致。

### M-3 `CacheAspect` 不解析 SpEL 方法调用：`#x.hashCode()` 变成字面量 `.hashCode()`

**文件**：`zhi-common/src/main/java/com/zhi/common/cache/aspect/CacheAspect.java:190-234`（尤其 223 行）

```java
223:                    result = result.replace("#" + paramName, keyFragment(argValue));
```

`resolveSpEL` 是**朴素字符串替换**，不是真正的 SpEL 求值器。对于 `CategoryServiceImpl:60` 的键：

```java
60:    @SiteCacheable(key = "zhi:category:list:#category.hashCode()", ttl = 30, timeUnit = TimeUnit.MINUTES)
```

替换后得到 `"zhi:category:list:" + keyFragment(category) + ".hashCode()"` —— 即键里出现**字面量字符串 `.hashCode()`**，从未被求值。同理 `TagServiceImpl:38` 的 `zhi:tag:list:#tag.hashCode()`、`ArticleServiceImpl:663` 的 `zhi:hot:#article.hashCode()`。

**触发场景**：恒久存在（每次读写这些缓存都是如此）。

**影响**：
1. 键名不符预期，且包含 `Category.toString()`（`ToStringBuilder` 的 `MULTI_LINE_STYLE`，**含换行符**）—— 键中带 `\n`，可读性差、调试困难；
2. 更实质的是**键的语义等价性被破坏**：不同 `Category` 实例（字段值相同但无 `equals`/`hashCode` 覆盖，见 `Category.java` 无 `hashCode` 定义）会因 `toString` 相同而**恰好**命中同一键 —— 目前"能用"是因为 `toString` 覆盖了所有业务字段，属于巧合；
3. 若未来 `toString` 加了 `params`/时间戳等易变字段，缓存命中率会骤降到 0（每次不同键），且**不会被任何测试发现**。

**修复建议**：
要么在切面里真正求值 SpEL（`MethodBasedEvaluationContext` + `SpelExpressionParser`），要么把注解键改写成切面此刻能正确解析的形式（如 `zhi:category:list:#category`，依赖 `keyFragment` 的 `>64` 字符哈希退化，`CacheAspect:134-143` 已实现该保护）。推荐后者（改动小、无新依赖），并在注解 javadoc 里明确「只支持 `#param` / `#pN` 简单插值，不支持方法调用」。

### M-4 留言冷却 check-then-set 竞态：并发提交可绕过 60 秒限制

**文件**：`zhi-admin/src/main/java/com/zhi/web/controller/site/FrontMessageController.java:183-222`

```java
183:        String cooldownKey = COOLDOWN_KEY_PREFIX + visitorKey(message, request);
184:        long cooldownRemain = remainingCooldownSeconds(cooldownKey);   // ← 检查
185:        if (cooldownRemain > 0)
186:        {
187:            return error("留言太频繁啦，请 " + cooldownRemain + " 秒后再试");
188:        }
...
217:        int rows = messageService.insertMessage(message);
218:        if (rows > 0)
219:        {
220:            unifiedCacheManager.set(cooldownKey, System.currentTimeMillis(),   // ← 设置
221:                    SUBMIT_INTERVAL_SECONDS, TimeUnit.SECONDS);
222:        }
```

第 184 行 `exists` + 第 220 行 `set` 之间隔着验证码校验与一次 DB insert，**不是原子操作**。

**触发场景**：同一 IP 在 `exists` 返回 false 后、`set` 生效前并发发起 N 个 `POST /api/message`（同一 `uuid` 只能成功一次验证码，但验证码关闭时 `CAPTCHA_ENABLED=false`/dev 环境无此约束）。

**影响**：60 秒一条的限制可被并发绕过，配合 `@RateLimiter`（60s/5req）单 IP 最多一次写入 5 条。`AGENTS.md` 中"每个访客 60 秒一条"的设计承诺在并发下不成立。属中等危害（有 RateLimiter 兜底，非无限刷）。

**修复建议**：
改用 Redis `SET key value NX EX 60` 原子抢占（`redisCache` 暴露 `setIfAbsent`），抢不到即冷却中：

```java
Boolean acquired = unifiedCacheManager.setIfAbsent(cooldownKey, "1", SUBMIT_INTERVAL_SECONDS);
if (!Boolean.TRUE.equals(acquired)) { return error("留言太频繁啦..."); }
```

注意实现时需保持现有「冷却检查在验证码校验**之前**」的语义（AGENTS.md 明确要求，避免拒绝时烧掉一次性验证码）—— 若改为先抢占，验证码失败时要 `delete` 回滚抢占，否则会误伤。

### M-5 列表查询的 `startPage()` 之后紧跟 `@SiteCacheable` 方法，count 查询与分页语义需确认

**文件**：`zhi-system/src/main/java/com/zhi/system/controller/ArticleController.java:292-298`、`zhi-admin/src/main/java/com/zhi/web/controller/site/FrontController.java:487-493`

```java
// ArticleController.java
291:    @PreAuthorize("@ss.hasPermi('zhi:article:list')")
292:    @GetMapping("/search")
293:    public TableDataInfo search(@RequestParam("keyword") String keyword, Article article)
294:    {
295:        startPage();
296:        List<Article> list = articleService.searchArticles(keyword, article);
297:        return getDataTable(list);
298:    }
```

`startPage()` 后进入带 `@SiteCacheable` 的 `searchArticles`。`CacheAspect.buildCacheKey` → `appendPageInfo`（`CacheAspect.java:152-162`）已专门把 PageHelper 的页码/页大小并入键，**懒加载**开启了，这一点修复是有效的（注释也说明了"搜索接口就这样串过页"的历史）。

但仍有两点风险：

1. **缓存命中时 PageHelper 的 count 不会被触发**：`getDataTable(list)` 依赖 `PageHelper` 的 `Page` 局部变量来读 `getTotal()`。当缓存命中、切面直接返回缓存 List 而未执行 SQL 时，count 查询不发生 —— 此时 `PageHelper.getLocalPage()` 是否仍持有可用的 `total`，取决于切面返回的对象是否还是 `Page` 实例（`CacheAspect` 缓存的是 `joinPoint.proceed()` 的返回值）。`Page` 是 `ArrayList` 子类，序列化进 Redis 后**丢类型信息**，反序列化回来通常只是普通 `List`，`getDataTable` 便拿不到 `total`。
2. 命中缓存时，第一次请求写入的 `total` 是否被复用，需要实测确认。

**影响**：搜索/分页场景下 `total` 可能为 0 或错误，前端分页器页数不对。

**待验证**：此项需要真实 Redis + PageHelper 的集成环境才能定性。**建议列入必测项**：`GET /api/article/search?keyword=X&pageNum=2` 连续两次，断言两次 `total` 一致且非 0。若确认 `total` 丢失，修复方向是让切面**不缓存** `Page` 结果（对 `Page` 类型跳过缓存），或把缓存下沉到 mapper 之上、分页之外。

### M-6 `catch (Exception)` 仅告警，掩盖写路径失败（数据一致性静默降级）

**文件**：`zhi-admin/src/main/java/com/zhi/web/controller/system/SysProfileController.java:180-190`

```java
180:                } catch (Exception e) {
181:                    logger.error("更新 sys_config 表中的 blog_avatar 失败", e);
182:                }
...
184:                try {
185:                    unifiedCacheManager.delete(CacheConstants.SETTINGS_ALL);
186:                    logger.info("已清除站点设置缓存");
187:                } catch (Exception e) {
188:                    logger.error("清除站点设置缓存失败", e);
189:                }
```

`SysProfileController` 头像更新路径（`zhi-admin/.../SysProfileController.java:157-190`）同时写了 `zhi_setting.blog_avatar`（157 行）与 `sys_config.blog_avatar`（169/179 行），并在 185 行手动清缓存。这里 180 行与 188 行两处 `catch(Exception)` 都只记日志：

- 180 行失败 → 用户头像已更新、`zhi_setting` 已更新，但 `sys_config` 镜像仍是旧值 → 前台读到的头像与用户设置不一致（`FrontController.getSettings` 以 `sys_config` 为主数据源）；
- 188 行失败 → `zhi:settings:all` 未清 → 前台最长 600 秒（`CACHE_EXPIRE_SECONDS`）仍显示旧头像，且接口仍返回成功。

**对比**：`SettingController.applySettingUpdateByKey` 在 229-233 行**刻意**把同步失败改为返回错误（"站点设置已保存，但同步到公开配置失败…请重试"），并有 `SettingControllerTest#testUpdateSettingByKey_Post_SyncFailure_ReturnsError` 锁定。**`SysProfileController` 这条同类路径没有跟上这个修复**，是全仓库唯一残留的"吞掉镜像失败仍返回成功"的写路径。

**影响**：头像/站点设置不同步且用户无感知，需人工排查。

**修复建议**：与 `SettingController` 对齐 —— 镜像同步失败时返回错误提示；或至少把"清缓存失败"改为可重试/告警（缓存失败影响面小于镜像失败，可保留吞异常但应升为 `warn` 并计入指标）。

---

## 低

### L-1 循环内单条 INSERT（N+1 写入）

**文件**：`zhi-system/src/main/java/com/zhi/system/service/impl/ArticleServiceImpl.java:521-533`、`540-553`、`270-286`

```java
522:    public int insertArticleTagRelations(Long articleId, List<Long> tagIds) {
...
528:        for (Long tagId : tagIds) {
529:            ArticleTag articleTag = new ArticleTag();
530:            articleTag.setArticleId(articleId);
531:            articleTag.setTagId(tagId);
532:            result += articleTagMapper.insertArticleTag(articleTag);
533:        }
```

`processTagAssociations`（270-286）同理。一次请求内 N 次往返，标签多时延迟线性上升。`deleteByArticleId` 已是整批删除，写入侧未做批量。
**建议**：新增 `insertArticleTagBatch`（`<foreach>` 的 `INSERT ... VALUES (...),(...)`）。

### L-2 `updateArticleStatus` 循环内逐条 `selectArticleById` + 逐条 `updateArticle`

**文件**：`zhi-system/src/main/java/com/zhi/system/service/impl/ArticleServiceImpl.java:581-594`

批量操作退化为 2N 次查询。**建议**：一次性 `selectArticleByIds(ids)` 校验标题，再用单条 `update ... where id in (...)`。

### L-3 `resolveSpEL` 子串误替换风险

**文件**：`zhi-common/src/main/java/com/zhi/common/cache/aspect/CacheAspect.java:206-225`

```java
210:                    result = result.replace("#p" + i, keyFragment(argValue));
...
223:                    result = result.replace("#" + paramName, keyFragment(argValue));
```

朴素 `String.replace`，会把 `#article` 误伤到 `#articleType`（前缀重叠）等更长参数名，且替换顺序依赖参数出现顺序。当前注解键都恰好不重名，故**未实际触发**。**建议**：按参数名长度降序替换，或改用真正的 SpEL。

### L-4 设置写入路径的错误信息不够定位

**文件**：`zhi-system/src/main/java/com/zhi/system/controller/SettingController.java:232`

```java
232:            return AjaxResult.error("站点设置已保存，但同步到公开配置失败，前台可能不会生效，请重试");
```

提示未带 `settingKey`，运维无法从 UI 直接判断是哪个键。**建议**：附上键名（`"…失败（key=" + settingKey + "）…"`）。

### L-5 定时任务日志/告警

**文件**：`zhi-admin/src/main/java/com/zhi/web/task/*.java`

4 个 `@Scheduled` 任务（`ScheduledArticlePublishTask:26`、`ArticleViewSyncTask:31`、`DailyStatsSyncTask:33/54/77`、`EmailCodeCleanTask:26`）的 catch 块均只 `log.error` 无指标/告警。`ArticleViewSyncTask` 的失败直接对应 M-1 的增量丢失。**建议**：失败计数暴露到 Micrometer/Actuator，或至少 `log.error` 时带上待处理键数量。

---

## 既有守卫复核（经本次验证确认正确的项）

以下均为 `AGENTS.md` / `CLAUDE.md` 记载的约定或历史故障修复，本次逐条复核，**确认实现正确**：

| # | 契约 | 复核结论 | 证据 |
| --- | --- | --- | --- |
| 1 | `zhi_message` 的 `create_by`/`update_by` 漂移已修 | ✅ 正确 | `sql/00_init_database.sql:716` 建表含两列；`2701-2716` 有幂等 `information_schema` + `PREPARE/EXECUTE` 补齐块，写法与解析器严格正则完全匹配（经脚本验证 20 条 ALTER 全部可解析，0 条漏判） |
| 2 | `SettingController` 的 5 个写方法都带 `@SiteCacheEvict(SETTINGS_ALL)` | ✅ 正确 | 118 / 130 / 142 / 154 / 295 行全覆盖（`add`/`edit`/`updateByKey`/`updateByKeyPost`/`remove`）。**例外见 H-3（`/mail`）** |
| 3 | `updateByKey` 清空值时置空 `sys_config` 镜像（不走有 `<if>` 守卫的 `updateConfig`） | ✅ 正确 | `SettingController:197-207` 走专用 `updateConfigValueByKey`（`SysConfigMapper.xml:104-106` 无 `<if>` 守卫）；`SysConfigServiceImpl:159-167` 同步清 `sys_config:<key>`；`SettingControllerTest#testUpdateSettingByKey_ClearingValueClearsSysConfigMirror` 锁定 |
| 4 | 清空「未镜像过」的键不应创建空行 | ✅ 正确 | `SettingController:202` 先判 `existingConfig != null`；`testUpdateSettingByKey_ClearingValueWithoutMirrorCreatesNoRow` 锁定 |
| 5 | `mail_*` 不得镜像进 `sys_config`（含密码） | ✅ 正确 | `SettingController:173-176` 直接拒绝 `mail_` 前缀；`testUpdateSettingByKey_RejectsMailKey` 锁定；管理端读取经 `maskMailPassword`（105-112）脱敏 |
| 6 | 浏览量计数原子性（`view_count = view_count + 1`，非先查后写） | ✅ 正确 | `ArticleMapper.xml:192` `view_count = IFNULL(view_count, 0) + 1`；`:196` `+ #{increment}`；`:392` `like_count + 1`；`PageMapper.xml:142` `view_count = view_count + 1`；`LikeMapper.xml:30/57` `GREATEST(like_count + #{delta}, 0)` |
| 7 | 点赞去重的并发安全 | ✅ 正确 | `zhi_article_like` / `zhi_comment_like` 均有 `UNIQUE KEY`（`sql:2475` / `sql:2485`），并发双击触发唯一键冲突而非重复计数，`LikeServiceImpl:28/80` 有 `@Transactional(rollbackFor = Exception.class)` 保证计数增减与记录一致 |
| 8 | 评论数同步不吞异常（避免"评论已发布、`comment_count` 永久偏移"） | ✅ 正确 | `CommentServiceImpl:434-443` 显式不 catch，注释说明"计数同步失败必须让外层事务回滚"；`changeCommentCount` 用 `GREATEST(IFNULL(...) + delta, 0)`（`ArticleMapper.xml:422-426`）防负数 |
| 9 | 状态变更时评论数增减去重（不重复计数） | ✅ 正确 | `CommentServiceImpl:218-230` 比较 old/new status 才调整；`auditCommentByIds:284`、`rejectCommentByIds:314` 均先查原状态再判断，避免重复审核导致计数漂移 |
| 10 | 慢操作（站内信/邮件）移出事务 | ✅ 正确 | `CommentServiceImpl:412-429` `afterCommit()` 用 `TransactionSynchronization` 注册，无事务时立即执行，不会占着事务连接做远程调用 |
| 11 | `sys.account.registerUser` 的严格 `"true"` 口径 + 不走 `SiteSwitchUtils` | ✅ 正确 | `SettingController:44-46, 249` 用 `REGISTER_VALUE.equals(...)`；`testGetRegistrationSwitch_readsSysConfigWithStrictTrueRule` 显式断言 `"1"` 为关闭 |
| 12 | 功能开关统一口径（只有 `false`/`"false"`/`"0"`/`0` 为关） | ✅ 正确 | 全部读点走 `SiteSwitchUtils.isOn/isOff`：`FrontMessageController:200/231`、`FrontController:320/589/852`、`FrontPageController:66`、`LikeController:45/74`、`MailConfigService`。未发现手写 `equals("true")` |
| 13 | 留言板三接口（含读取）都被 `guestbook_enabled` 短路 | ✅ 正确 | `FrontMessageController:91`（list）、`:112`（count）、`:128`（add）三处均判 `isGuestbookClosed()`，与 AGENTS.md「reads too」一致 |
| 14 | 留言冷却检查在验证码**之前**（不烧验证码） | ✅ 正确 | `FrontMessageController:184`（冷却）先于 `:191`（`validateAndGetAgeSeconds`）；测试 `FrontMessageControllerTest:368` 断言被拒时 `validateAndGetAgeSeconds` 从未调用 |
| 15 | 公开留言查询不返回 `email`/`ip`/`user_agent` | ✅ 正确 | `MessageMapper.xml:27-30` `selectPublishedMessageVo` 仅取 `id, user_id, nickname, website, content, reply_content, reply_time, status, create_time`，无 PII |
| 16 | `MapperColumnSchemaContractTest` 的解析规则本身可靠 | ✅ 正确（但有盲区，见下） | 脚本复现其算法：48 张表全部解析成功；20 条 ALTER 补齐块**全部**能被严格正则 `ALTER TABLE \`t\` ADD COLUMN \`c\`` 识别（0 条因写法差异漏判）；5 个 mapper 因「多表」被跳过，经独立比对**这 5 个 mapper 无实际列漂移** |
| 17 | `SysUserMapper.insertUser` 持久化 `user_type` | ✅ 正确 | `SysUserMapper.xml` insert 含 `<if test="userType != null...">user_type,`；测试 `SysUserMapperTest`（H2）锁定 insert↔select 往返为 `'01'` |
| 18 | `DataScope` SQL 注入防护 | ✅ 正确 | `DataScopeAspect.clearDataScope`（`DataScopeAspect.java:172-181`）在拼接前先把 `params.dataScope` 置空；拼接值全部来自 `role.getRoleId()`/`user.getDeptId()`（内部 Long），非用户输入；`${params.dataScope}` 出现在 `SysUserMapper.xml:87/104/122`、`SysDeptMapper.xml:46`，均为框架受控值 |
| 19 | `GlobalExceptionHandler` 兜底覆盖 | ✅ 基本正确 | 覆盖 11 类：`AccessDenied`/`MethodNotSupported`/`ServiceException`/`MissingPathVariable`/`TypeMismatch`/`RuntimeException`/`Exception`/`Bind`/`ArgumentNotValid`/`DemoMode`/`NoHandlerFound`。`IllegalArgumentException` 与 `DuplicateArticleTitleException` 经 `RuntimeException` 兜底（`GlobalExceptionHandler.java:100-107`），返回 `e.getMessage()` 给前端。**注意**：兜底把原始异常消息透传给前端（`AjaxResult.error(e.getMessage())`），生产环境可能泄露表名/SQL 片段 —— 复核为既有设计，本次不另计为一独立发现，但建议纳入安全审查跟踪 |
| 20 | `ArticleViewSyncTask` 用 SCAN 而非 KEYS | ✅ 正确 | `RedisCache.scanKeys`（`RedisCache.java:306-322`）用 `ScanOptions` 分批，注释明确避免 `KEYS` 阻塞生产 Redis |

### 守卫测试的规则盲区（`MapperColumnSchemaContractTest`）

该测试是**有效的**（本次全量脚本比对确认它覆盖的 27 个 mapper 无遗漏漂移），但有三处结构性盲区值得记录：

1. **多表 mapper 被整体跳过**（`MapperColumnSchemaContractTest.java:139-153`，`singleSchemaTable` 要求"恰好一张已知表"）。被跳过的 5 个：`CategoryMapper.xml`、`LikeMapper.xml`、`SysDeptMapper.xml`、`SysUserMapper.xml`、`TagMapper.xml`。本次用"任一相关表的列并集"独立比对，**结论：这 5 个均无缺失列**（检出的 `dtd`/`mapper`/`org`/`zhi`/`size`/`util` 等为 XML DOCTYPE 与 `<foreach>` 属性噪声，`beginTime`/`endTime`/`dataScope` 为 `params.*` 动态字段，非列引用）。但这些 mapper **包含计数核心语句**（`LikeMapper` 的 `like_count`、`SysUserMapper` 的 `sys_user` 列），一旦未来有人改错，守卫不会报红。
2. **`<sql>` 片段中的列验证依赖 `FROM_TABLE` 正则**（`:56-57`），只匹配 `from <table>`。若 mapper 用反引号或不换行写法变体，或表名出现在子查询中，可能漏采。
3. **`select` 列表只收"裸列名"**（`:155-193`），带函数/限定名/别名的表达式交给 resultMap 规则；`<if>` 动态列只认 `<if test="...">\s*(\w+)\s*(?:,|=)` 这一种写法，`<trim>`/`<set>` 内其它形态的动态列不会被采集。

**建议**：
- 为那 5 个多表 mapper 增加"白名单 + 逐表列校验"的扩展模式（例如按 `alias.col` 前缀把列归属到对应表），至少覆盖 `LikeMapper` / `SysUserMapper` 这类核心表；
- 把本次使用的"全量脚本比对"沉淀为测试，作为现有守卫的补充，而不是依赖手工审查。

---

## 关于「未发现」的诚实声明

以下必查项经审查**未发现真实缺陷**，特此明确，不凑数：

- **Spring AOP 自调用导致 `@Transactional` / `@SiteCacheEvict` 失效**：全仓库检索 `this.xxx()` 形式对带注解方法的调用，仅命中 `SysUserServiceImpl.java:404` 的 `this.insertUserRole(...)`（该方法无注解，且外层 `insertUser` 已有 `@Transactional(rollbackFor = Exception.class)`，`SysUserServiceImpl.java:293`），**不构成失效**。`ArticleServiceImpl:551` 的 `insertArticleTagRelations(articleId, tagIds)` 是同类内隐式 `this` 调用（无 `this.` 前缀故未被正则命中）—— 但该方法自身有 `@Transactional`，外层 `updateArticleTagRelations`（545 行）也有，即使内层注解失效，外层的 `deleteArticleTagRelations` + `insertArticleTagRelations` 仍在同一事务中，**行为正确**。故本项目**未发现**经典自调用失效实例。
- **`readOnly` 误用**：全仓库 `@Transactional` 无一处使用 `readOnly = true`，**不存在误用**。
- **`PageHelper` 的 `startPage()` 被非目标查询"偷走"**、**Mapper 排序字段 `${}` 注入**（`orderByColumn`/`isAsc` 直接拼接）：未发现实例。所有 `${}` 仅出现在 `DataScope` 的 `params.dataScope`（见复核 #18）与 `OrderItem` 框架路径，用户不可控。
- **`catch` 空块**：全仓库无空 catch；`EmptyCatchBlock` 形式的 `catch (Exception e) { }` 未检出。
- **zhi-quartz 的 `@DisallowConcurrentExecution`**：`ScheduleUtils.getQuartzJobClass`（`ScheduleUtils.java:35-39`）按 `concurrent` 字段在 `QuartzJobExecution` 与 `QuartzDisallowConcurrentExecution` 间选择，`QuartzDisallowConcurrentExecution`（`:13`）已正确标注 `@DisallowConcurrentExecution`，**实现正确**。`AbstractQuartzJob` 的 `ThreadLocal` 在 `after()` 中 `remove()`（`AbstractQuartzJob.java:72-73`），**无泄漏**。另注：`zhi-quartz/config/ScheduleConfig.java` 整体被注释掉，Quartz 走 Spring Boot 自动配置 —— 与 `AGENTS.md` 单机部署描述一致，非缺陷。
- **定时任务幂等性**：`publishScheduledArticles`（`ArticleMapper.xml:412-419`）是条件更新 `WHERE status = 2 AND publish_time <= NOW()`，**天然幂等**，重复执行不会重复发布；`upsertDailyStat`（`DailyStatsMapper.xml`）用 `ON DUPLICATE KEY UPDATE`，**幂等**。

## 待验证项

| # | 事项 | 为什么待验证 | 建议验证方式 |
| --- | --- | --- | --- |
| V-1 | 分页 + `@SiteCacheable` 命中时 `total` 是否正确（M-5） | 需要真实 Redis + PageHelper 集成环境；`Page` 经 Redis 序列化后的类型还原行为无法静态判定 | 起后端 + Redis，连续两次请求 `GET /api/article/search?keyword=X&pageNum=2`，断言 `total` 一致且非 0 |
| V-2 | `LikeServiceImpl` 并发双击的实际表现 | 唯一键冲突会抛异常，但需确认前端拿到的是可识别错误而非 500 白屏；`LikeController` 未 catch 该异常 | 并发压测 `POST /api/like/article/{id}`，观察响应码与 `like_count` 是否仍等于真实记录数 |
| V-3 | `SysUserServiceImpl` / `SysRoleServiceImpl` 的事务覆盖完整性 | 本次资源集中在内容域（task-2 的 8 项清单），系统域的 8 个 `@Transactional` 方法只做了自调用与注解存在性核查，未逐条走查业务语义 | 后续专项审查建议单列 |

---

*审查人：backend-auditor ｜ 只读审查，未改动任何源码 ｜ 报告路径：`reports/audit-backend-correctness.md`*