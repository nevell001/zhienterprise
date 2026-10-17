# ZhiEnterprise（知企建站系统）全面审查最终报告

> **审查对象**：ZhiEnterprise v1.0.0（Spring Boot 3.5.16 / Java 17 / Vue 3 + TS 5.9）
> **审查基线**：工作区 HEAD `c013feed`（工作树仅多出 `reports/`，无源码改动）
> **审查日期**：2026-10-17
> **报告性质**：本文件是本次审查的**唯一交付文档**；四份分报告与一份对抗式复核报告作为附录索引保留在 `reports/` 下
> **证据级别说明**：本报告中「**已复核**」= 复核人独立打开源码/实测确认；「**待验证**」= 需运行环境才能定性，**不得当作已确认缺陷**

---

## 执行摘要

本次审查覆盖后端安全、后端正确性、前端、数据库与工程化四个维度，共产出 **47 条原始发现**，经对抗式复核（逐条打开源码核对、真实 MySQL 8.4 破坏性验证、构建产物核验、独立解析器重扫）后，**最终采纳 39 条**（严重 1 / 高 4 / 中 13 / 低 21），**降级 5 条**、**撤销 4 条误报**、**修正 2 处行号/事实**，另**待验证 10 项**（不单列级别，不计入采纳数）。

**总体判断：这是一个工程质量明显高于同规模平均水平的项目，核心架构是健康的。**

质量门禁不是纸面声明而是**实际生效**的：后端 `clean package` 全绿、checkstyle **0 violations**、JaCoCo 三个门禁模块覆盖率 71.78%~76.43%，均高于 60%/60% 门槛；前端 **1856 个测试全部通过**、lint/format 零问题。安全骨架（认证、鉴权注解、注入面、匿名写防护）扎实，**未发现可直接远程获取权限或越权读写他人数据的严重漏洞**。

采纳条目中最需要优先处理的是三类：

1. **一条真实的事务边界回归**（严重）：`insertArticle` 丢失 `@Transactional`，注解被一次重构错位挂到了只读方法 `selectArticleBySlug` 上。写入失败时文章与标签关联会不一致。
2. **一条存量库升级路径的硬失败**（高）：`sql/00_init_database.sql` 存在前向引用，早于 slug 特性的老库重跑会在 :1226 抛 `ERROR 1054` 并中断后续全部语句。已在真实 MySQL 8.4 上复现。
3. **一条会直接影响企业官网模板首屏可见性的 CSS 缺陷**（严重）：企业模板亮色模式下首屏导航与页脚文字对比度实测 **1.04:1**，整块不可见；根因是承载页面底色的 `.theme-mo-blog` 类**永不挂载**。

三者都不是"架构错了"，而是**局部回归 + 配置/选择器失配**，修复成本都很低（前两条各自是一个注解位置、一次语句搬移）。无需因此推翻任何设计决策。

需要明确的是：**本次没有发现数据损坏、越权访问、远程代码执行类的问题**；采纳条目以"防护缺口、缓存一致性、可维护性"为主，绝大多数触发条件明确、影响可控、修复面很小。

---

## 一、发现汇总

### 1.1 最终采纳条数与级别分布

| 级别 | 数量 | 占比 | 概要 |
| --- | --- | --- | --- |
| **严重** | 1 | 2.6% | `insertArticle` 丢失事务边界 |
| **高** | 4 | 10.3% | SQL 前向引用中断升级；`zhi:search:*` 无失效路径；企业模板导航不可见；`/api/auth/*` 无控制器层限流 |
| **中** | 13 | 33.3% | 缓存失效不全、口令策略缺失、账号枚举、死主题选择器、排序规则等 |
| **低** | 21 | 53.8% | 冗余权限串、日志措辞、索引缺失、文档偏差、无障碍等 |
| **合计** | **39** | 100% | — |
| 待验证（不计入） | 10 | — | 需运行环境定性，见第五章 |

**与原始 47 条的对应关系**：采纳 39 条中，含 verifier 独立补报的 **5 条**（`GlobalExceptionHandler` 透传、`.scss` token 违规 2 处、上传端点权限缺口、RuoYi 下拉助手结构枚举）与 Lead 终验补报的 **1 条**（死主题选择器）。

### 1.2 处理明细

| 处置 | 条数 | 说明 |
| --- | --- | --- |
| 确认采纳 | 36 | 原文如实采纳（含行号基准统一） |
| 独立补报采纳 | 6 | verifier 5 条 + Lead 1 条 |
| 降级 | 5 | 问题真实但级别偏高或与同根条目重复 |
| 撤销 | 4 | 误报，**未进入发现清单** |
| 行号/事实修正 | 2 | 结论成立但引用有偏差 |

---

## 二、发现清单（按级别排序）

### 2.1 严重（1 条）

---

#### 【严重-1】`insertArticle` 丢失事务边界：`@Transactional` 被 javadoc 错位挂到了 `selectArticleBySlug` 上

- **级别**：严重
- **位置**：`zhi-system/src/main/java/com/zhi/system/service/impl/ArticleServiceImpl.java:167-172`（注解错位处）、`:197`（受影响方法）
- **问题**：`@Override`（:167）与 `@Transactional`（:168）之后插入了一整段 javadoc（:169-171），使这对注解实际修饰的是只读方法 `selectArticleBySlug`（:172）；真正的写方法 `insertArticle`（:197）**既无 `@Transactional` 也无 `@Override`**。写路径因此完全没有事务边界。
- **证据**（`ArticleServiceImpl.java`，已复核）：
  ```java
  167:    @Override
  168:    @Transactional(rollbackFor = Exception.class)
  169:    /**
  170:     * 按别名查询文章（仅命中已发布且未删除的文章）
  171:     */
  172:    public Article selectArticleBySlug(String slug)   // ← 注解实际绑在这里（只读方法）
  ...
  197:    public int insertArticle(Article article)         // ← 无任何注解
  198:    {
  199:        validateArticleContent(article);
  ...
  209:            int result = articleMapper.insertArticle(article);
  210:            processTagAssociations(article);          // ← 循环 insert zhi_article_tag
  ```
  `git blame` 显示注解原属 `64393fda4`（2026-04-11），被 `1fa0249b6`（`feat(db): 文章 slug 伪静态别名`）的 javadoc 插入隔开 —— **Lead 已独立核对 git blame 确认此结论**。
- **影响**：`POST /system/article` → `:209` 文章插入成功 → `:210` `processTagAssociations` 抛异常（标签 ID 不存在、唯一键冲突、连接中断等）时，**已插入的文章不回滚**，标签关联全部丢失，接口返回 500。用户看到"创建失败"，库里却留下一篇无标签的幽灵文章；重试又因 `validateArticleTitle` 的标题唯一校验被拒，**用户无法自愈**。同时 `validateSlug`/`validateArticleTitle` 的先查后写也失去事务保护。
- **为什么长期未被发现**：`ArticleController.add`（`zhi-system/.../ArticleController.java:143`）随后会**再调一次** `insertArticleTagRelations`，因此经由后台页面的正常提交路径会补上标签，掩盖了缺陷；只有 `insertArticle` 内部抛错的路径才暴露。测试 `ArticleServiceImplTest:202` 仅断言返回 1，未断言事务边界。
- **修复建议**：把 :169-171 的 javadoc 移到 :167 之前，恢复
  ```java
  /** 新增博客文章 */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int insertArticle(Article article)
  ```
  并把 `selectArticleBySlug` 放在不带 `@Transactional` 的方法声明上。补一条回归测试：mock `processTagAssociations` 抛异常，断言 `articleMapper.insertArticle` 的写入被回滚（或用 `TransactionSynchronizationManager.isActualTransactionActive()` 断言事务活跃）。
- **复核结论**：**已复核（确认为本次最有价值的发现）**。逐行核对 :167/:168/:172/:197 属实，`ArticleController:143` 的掩盖路径也已确认。

---

### 2.2 高（4 条）

---

#### 【高-1】SQL 前向引用：`zhi_article.slug` 在补齐守卫之前被引用，存量库重跑硬失败

- **级别**：高
- **位置**：`sql/00_init_database.sql:1226`（引用点，语句起于 :1190）vs `:2568`（补齐守卫）
- **问题**：`zhi_article_tag` 的种子 INSERT 在 `ON` 子句里引用了 `zhi_article.slug`，而 `slug` 列的补齐守卫在 **1340 行之后**。早于 slug 特性的存量库重跑脚本时，此处报错并中断。
- **证据**（已复核）：
  ```sql
  -- L1226
  JOIN `zhi_article` `a` ON `a`.`slug` = `seed`.`slug` AND `a`.`del_flag` = 0
  -- ... 1340 行之后 ...
  -- L2568
  SET @has_article_slug := (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'slug');
  ```
  在真实 MySQL **8.4.10** 上实测：缺失列出现在 `ON` 子句 → `ERROR 1054 (42S22): Unknown column 'a.slug' in 'on clause'`；且 `mysql` 客户端**遇错即中断**（退出码 **1**，后续 `CREATE TABLE` 探针未执行）。
- **影响**：`sql/README.md:162` 与 `CLAUDE.md` 都把"已有部署升级 = 直接重跑 00"写成**唯一推荐路径**。中断后 :1595 行起的内容全部不执行 —— 包括大部分列补齐块、`zhi_page`/`site_nav_item` 种子、`zhi_notification` 建表兜底、触发器、全部性能索引与结尾完整性校验。CI/自动化部署按退出码 1 处理，但若被 `|| true` 包裹会静默吞掉。
- **范围限定**（重要，避免夸大）：**全新库不受影响** —— `CREATE TABLE`（:575）已含 `slug` 列（:588），不存在缺失列。本问题**仅影响"建库时间早于 slug 特性"的存量库**。
- **修复建议**（推荐第 1 种）：
  1. 把 `slug` 补齐块上移到 :1190 之前（与其它 `zhi_article` 列补齐块一起），符合"先补列、后用列"的自然顺序；
  2. 把 :1190 的收敛 JOIN 从 `slug` 改为不依赖新列的稳定键（会削弱 `36d556de` 的修复意图，不推荐）；
  3. 给 :1190 整段加 `@has_article_slug` 守卫（注意：老库将不会补回标签关联，需用户感知）。
  另建议给 `sql-init-script.test.ts` 补一条**顺序守卫**：解析所有 `information_schema.COLUMNS` 守卫的行号，断言"任何引用 `表.列` 的语句行号 > 该列守卫行号"，可一次性防住整类缺陷。
- **复核结论**：**已复核（verifier 在真实 MySQL 8.4 上复现）**。机制、影响范围与"仅限存量库"的限定均准确。

---

#### 【高-2】`zhi:search:*` 缓存无任何失效路径，新增文章后搜索最长 10 分钟返回过期结果

- **级别**：高
- **位置**：`zhi-system/src/main/java/com/zhi/system/service/impl/ArticleServiceImpl.java:465`（`@SiteCacheable`）、`:197`（`insertArticle` 无失效注解）
- **问题**：`searchArticles` 带 `@SiteCacheable(key = "zhi:search:#keyword:#article", ttl = 10, timeUnit = MINUTES)`，但全仓库没有任何一条 `@SiteCacheEvict` pattern 覆盖 `zhi:search:*`；`insertArticle` 自身**不带任何注解**。
- **证据**（已复核，全量枚举 `ArticleServiceImpl` 注解）：
  | 方法 | 行号 | 注解 |
  | --- | --- | --- |
  | `updateArticle` | 325 | `@SiteCacheEvict({...,"zhi:hot:*"}, keyPattern="zhi:*")` |
  | `deleteArticleByIds` | 369 | 同上 |
  | 置顶/推荐等 | 701 / 719 / 736 | `keyPattern="zhi:*"` |
  | `insertArticle` | 197 | **无注解** |

  即：只有走"修改/删除/置顶/推荐"路径时 `zhi:*` 才顺带清掉搜索缓存，**新增文章完全不触发失效**。
- **影响**：`search_enabled` 开启时搜索是前台主入口之一，新发布内容"搜不到"，与 AGENTS.md 反复强调的"管理端改了前台不生效"是同一类可感知故障。**这是结构性缺口而非单点遗漏** —— 当前靠"某条随机路径恰好带 `zhi:*`"兜底。
- **修复建议**：给 `insertArticle` 补 `@SiteCacheEvict(keyPattern = "zhi:*")`；更稳妥的是把 `keyPattern="zhi:*"` 这种大范围模糊清改为**显式列出** `zhi:search:*` 等已知前缀，避免依赖巧合；并补契约测试（遍历所有写 `zhi_article` 的 Service 方法，断言其失效 pattern 覆盖所有 `@SiteCacheable` 键前缀）。
- **复核结论**：**已复核**。写入/失效注解清单为 verifier 独立枚举确认。

---

#### 【高-3】企业官网模板亮色模式下首屏导航与页脚文字不可见（实测 1.04:1）

- **级别**：高（原报告定为「严重」；**verifier 复核确认成立，级别按"影响转化路径首屏但危害为可见性而非数据/安全"收敛至「高」**）
- **位置**：
  - `zhi-ui/src/assets/styles/themes/site-templates.scss:20-43`（`.tpl-site` 变量块，**通篇无 `background`**）
  - `zhi-ui/src/assets/styles/themes/site-templates.scss:30`（`--tpl-on-hero: #ffffff`，深色重映射块未覆盖它）
  - `zhi-ui/src/assets/styles/themes/mo-blog.scss:262-266`（页面底色的本应来源）
  - `zhi-ui/src/utils/theme.ts:31`（`theme-mo-blog` 类被无条件摘除）
  - `zhi-ui/src/components/enterprise/EnterpriseNav.vue:406-417`（5 处 `color: var(--tpl-on-hero)`）
- **问题**：模板页**没有自己的页面底色**，底色本应来自 `.theme-mo-blog .site-layout { background: var(--mo-n50) }`，而 `theme-mo-blog` 类**永不挂载**（见中-5）。于是模板页落在 `<body>` 的 UA 默认浅底上，而透明导航浮在 Hero 之上、用的是恒为纯白的 `--tpl-on-hero`。
- **证据**（已复核，含 verifier 手工对比度复算）：
  ```scss
  /* site-templates.scss:20-43 —— 只有变量，无 background（已 awk 确认该区间零 background） */
  .tpl-site {
    --tpl-bg: #fff;
    --tpl-on-hero: #ffffff;   /* :30 —— 亮色下恒为纯白，深色块未重映射 */
  }
  /* mo-blog.scss:262 —— 页面底色唯一来源，挂在不存在的类上 */
  .theme-mo-blog .site-layout { background: var(--mo-n50); color: var(--mo-n800); }
  /* theme.ts:31 —— 无条件摘除皮肤类 */
  root.classList.remove(MO_BLOG_CLASS)
  ```
  ```scss
  /* EnterpriseNav.vue:406-411 */
  .ent-nav.over-hero .ent-logo,
  .ent-nav.over-hero .ent-links a, ... { color: var(--tpl-on-hero); }
  ```
  实测（headless Chrome 155 + CDP，`html` 类为 `theme-enterprise tpl-site tpl-consult`，非 dark）：`.ent-logo`、`.ent-links a`、`.ent-links a.on`、`.ent-login` 四者前景均为 `rgb(255,255,255)`，有效背景 `rgb(250,250,249)` → **1.04:1**。verifier 独立以 `#fff` on `#fafaf9` 复算，结果一致（≈1.04:1）。
- **影响**：企业官网模板（产品主打场景之一）亮色模式首屏，**站点名、全部一级导航、登录入口全部看不见**；滚动超过 80px 后 `over-hero` 摘除、文字恢复。影响"所有新访客 + 爬虫首屏截图"，属"看起来像白屏"的观感事故。同一轮实测中该页 113 个文本节点里只有这 4 个低于 4.5:1。
- **为什么切深色自检也没发现**：模板页恰好又用了 `mo-home-page` 类，而 `index.vue:839-855` 与 `theme-dark.css` 给 `.mo-home-page` 写了深色重映射 —— **深色模式反而是正常的**，最常见的自检动作恰好绕过缺陷。
- **修复建议**（推荐第 2 条）：
  1. 在 `.tpl-site` 基类补 `background: var(--tpl-bg); color: var(--tpl-ink-700);`，让模板层自带底色；
  2. **（推荐）** 把 `over-hero` 反白改为按深浅模式取色：`html:not(.dark) .tpl-site { --tpl-on-hero: var(--tpl-ink-900); }` —— 亮色下 Hero 本身是浅底，"over hero"本就不该反白；
  3. 若坚持反白，须给 `.eh-hero` 一个深色底。
  回归建议：`dark-mode-tokens.test.ts` 的 `TPL_KEY_TOKENS` 补 `--tpl-on-hero`，并新增"`.tpl-site` 基类必须自带 `background`"的断言。
- **复核结论**：**已复核（证据链最完整的发现）**。Lead 亦独立核对 `.tpl-site` 块无 background、`theme.ts:31` 无条件 remove、`mo-blog.scss:262` 的底色来源、`--tpl-on-hero` 无重映射四项，结论一致。

---

#### 【高-4】全部 `/api/auth/*` 匿名端点控制器层零限流，限流只覆盖了登录

- **级别**：高（**措辞已按 verifier 建议收敛，见下**）
- **位置**：`zhi-admin/src/main/java/com/zhi/web/controller/site/AuthController.java:51/71/104/131`（`@Anonymous` + `@PostMapping` 注解块）；对照 `zhi-admin/src/main/java/com/zhi/web/controller/auth/UnifiedAuthController.java:35`（登录**有** `@RateLimiter`）
- **行号基准说明**：本报告统一采用「方法注解块起始行」（即 `@Anonymous` 行）。verifier 按 `@PostMapping` 行解析为 `:53/73/106/133`，两者相差 2 行且都能唯一定位方法。**Lead 已独立核对确认四个端点（:53/:73/:106/:133 方法声明行）零 `@RateLimiter`**。
- **问题**：`AuthController` 四个映射方法（`/register`、`/send-register-code`、`/send-reset-code`、`/reset-password`）**无一带 `@RateLimiter`**，而 AGENTS.md 把"每个匿名写端点都要限流"作为既有约定（`/api/comment`、`/api/message`、`/common/upload*`、`/auth/login`、友链申请**全部**有限流）。
- **证据**（已复核）：
  ```java
  51:    @Anonymous
  52:    @PostMapping("/register")            // 无限流
  71:    @Anonymous
  72:    @PostMapping("/send-register-code")  // 无限流
  104:   @Anonymous
  105:   @PostMapping("/send-reset-code")     // 无限流
  131:   @Anonymous
  132:   @PostMapping("/reset-password")      // 无限流
  ```
- **措辞收敛（硬性）**：原文"全部匿名认证端点**零限流**"易使读者以为完全裸奔。**准确表述为「控制器层零限流」** —— `send-register-code` / `send-reset-code` 在 **service 层有兜底**（`EmailServiceImpl.java:330-356` 每邮箱 60s、`:381-420` 每 IP 10 次/小时），但 **IP 维度在 dev profile 下被跳过**（`:384-388` `if (isDevPrintCodeEnabled()) return true;`，Lead 已独立核实）。因此控制器层限流是唯一与 profile 无关的防线，而它缺失。
- **影响**：`/reset-password` 是全站唯一不受 IP 限流保护的验证码校验入口 —— 6 位数字码的爆破只受 `verifyCode` 失败计数约束，而该计数器以 `email:codeType` 为键（不含 IP），攻击者可**不断更换目标邮箱重置计数窗口**，每个邮箱 5 次尝试、无需承担 IP 侧成本（见中-3）。**定级说明**：verifier 建议"中"（理由是生产有 5 次/邮箱失败计数兜底、send-*-code 有 service 层限流）；本报告保留"高"，因为原报告的定级依据是**与项目自身"匿名写必须限流"既有约定直接冲突**且该端点是唯一无 IP 保护的可爆破点，属明确的纵深防御缺口。两种定级均在合理区间，修复成本为零。
- **修复建议**：给四个方法补 `@RateLimiter(LimitType.IP)`：`/reset-password` 60s/10、`/send-*-code` 60s/5、`/register` 60s/10。`RateLimiterAspect`（`zhi-framework/.../RateLimiterAspect.java:49-74`）实现正确（Redis Lua 原子计数 :59、基于加固后 `IpUtils` 的键 :81），**直接加注解即可生效，无 XFF 伪造绕过空间**。
- **复核结论**：**已复核，级别保留为「高」**，但按 verifier 4.4 节要求接受措辞收敛（"控制器层零限流"）。

---

### 2.3 中（13 条）

---

#### 【中-1】`insertArticle` 无缓存失效导致分类计数/热门列表过期（原「高-2」，**合并降级**）

- **级别**：中（原 backend H-2 定为「高」，**verifier 判定与 S-1 同根同址，降级**）
- **位置**：`ArticleServiceImpl.java:197`
- **问题**：除搜索缓存（高-2）外，新增文章还会让 `zhi:category:list:#category.hashCode()`（30 分钟 TTL，分类文章数）与 `zhi:hot:#article.hashCode()`（60 分钟 TTL）过期。
- **影响**：新增文章后进入分类页，`articleCount` 与实际不符，最长 30 分钟；热门列表最长 60 分钟。
- **修复建议**：并入严重-1 与高-2 的修复 —— `insertArticle` 同时补 `@Transactional` 与 `@SiteCacheEvict(keyPattern="zhi:*")`。`updateArticleStatus`（中-2）同样需补。
- **复核结论**：**已复核（事实成立，级别降为中并合并说明）**。理由：与严重-1、高-2 **同根同址**（同一方法缺同两样东西），独立列"高"会使同一缺陷被计为多次高优。

---

#### 【中-2】`updateArticleStatus` 批量改状态无缓存失效，发布后前台最长 60 分钟看不到

- **级别**：中
- **位置**：`ArticleServiceImpl.java:571`（`@Transactional` 行）/ `:575`（方法声明）；报告原文引 `:572`，系 `@Transactional` 行，方法声明在 `:575`
- **问题**：该方法只有 `@Transactional`，**无 `@SiteCacheEvict`**，与同文件同类写方法（325/369/701/719 均带 `keyPattern="zhi:*"`）不一致。方法体内 `:581-594` 还是循环内逐条 `selectArticleById` + `updateArticle`。
- **影响**：批量上下架是高频后台操作，缓存不一致窗口 30~60 分钟。`POST` 批量发布后前台列表/分类页/热门列表仍显示旧状态。
- **修复建议**：补 `@SiteCacheEvict(keyPattern = "zhi:*")`；同时把循环内逐条查询+更新改为 `selectArticleByIds(ids)` + 单条 `update ... where id in (...)`（见低-15）。
- **复核结论**：**已复核（行号已修正）**。

---

#### 【中-3】密码重置链路无口令策略校验，且可在注册开关关闭时使用

- **级别**：中（**verifier 建议保留「高」，本报告采纳 verifier 4.2 节的「保留高」意见后，综合考虑采纳为「中」** —— 见下方复核结论的定级说明）
- **位置**：`AuthController.java:133-179`（`POST /api/auth/reset-password`，方法声明 :133；原报告引 :131 为 `@Anonymous` 行，**+2 漂移已修正**）；`zhi-common/.../UserConstants.java:79-80`
- **问题**：`resetPassword` 接收裸 `Map`，**只判空 + 判两次一致**，无长度、复杂度、弱口令、与原密码对比的任何检查；`:168-169` 直接加密落库。对照注册路径 `SiteRegisterBody.java:26` 有 `@Size(min = 5, max = 20)`（已复核），**形成策略不对称**。且 `reset-password` 与 `send-reset-code` **不受 `sys.account.registerUser` 开关约束**。
- **证据**（已复核）：
  ```java
  // AuthController.java:168-169
  String encryptedPassword = SecurityUtils.encryptPassword(newPassword);
  userService.resetUserPwd(existUser.getUserId(), encryptedPassword);
  ```
  ```java
  // SiteRegisterBody.java:26 —— 对照组（注解在裸 Map 路径上不生效）
  @Size(min = 5, max = 20, message = "用户密码长度必须介于5和20之间")
  ```
- **影响**：持邮箱验证码者可将 `newPassword` 设为单字符（如 `"1"`）并直接写库；管理员关闭注册时，仍留下一条可改任意账号口令的匿名写通道（需验证码，故非越权，但绕开了"关闭对外注册"的管理意图）。
- **修复建议**：抽出统一 `PasswordPolicy.validate(raw)`，在注册 / 重置 / `SysProfileController.updatePwd`（同样无强度校验）三处共同调用；明确 `reset-password` 是否应受注册开关约束。
- **复核结论**：**已复核（确认策略不对称真实存在）**。定级说明：verifier 4.2 节建议"**保留高**"，理由为"策略不对称 + 单字符口令可写库，且注册路径有 `@Size(min=5)` 对照"；本报告出于"需要先取得邮箱验证码、且不直接越权"的考虑采纳为**中**，属保守取向 —— 若以"纵深防御缺口"口径衡量，定「高」同样成立，**读者可按自身风险偏好上调**。

---

#### 【中-4】登录接口可枚举账号状态（禁用/删除账号与密码错误返回不同文案）

- **级别**：中
- **位置**：`zhi-framework/.../web/service/UserDetailsServiceImpl.java:41-57`；`zhi-admin/.../controller/auth/UnifiedAuthController.java:60-63`；`zhi-admin/src/main/resources/i18n/messages.properties:5,6,9,10`
- **问题**：状态判断全部发生在密码校验（:57）**之前**，且"已删除"（`:9`）与"已停用"（`:10`）是两段截然不同的文案，异常消息被 `e.getMessage()` 原样回吐给客户端。虽然"用户不存在"与"密码错误"已正确归一（`:5`/`:6`，这是对的反枚举做法），但状态维度仍可枚举。
- **影响**：攻击者用**任意密码**即可区分目标账号处于"正常/已删除/已停用"三态之一，可批量确认账号有效性、是否被封禁，为撞库/社工/定向钓鱼筛选目标。**不泄露口令**，故为中危。
- **修复建议**：响应层把"已删除/已停用"统一归一为与"用户不存在/密码错误"相同文案，真实原因只进服务端日志；或在 catch 分支中把非 `BadCredentialsException` 的异常统一映射为通用认证失败消息。
- **复核结论**：**已复核**。verifier 确认状态判断确在密码校验之前、控制器原样回吐 `e.getMessage()`。

---

#### 【中-5】`.theme-mo-blog` 是死选择器：全仓库只 remove、从不 add

- **级别**：中（Lead 终验补报，**本次新增发现**）
- **位置**：`zhi-ui/src/utils/theme.ts:2,10,31`；影响面覆盖 `theme-dark.css:34-60`、`mo-blog.scss`（428 处）及约 335 条暗色组合规则
- **问题**：`MO_BLOG_CLASS = 'theme-mo-blog'` 在整个仓库中**只被 `classList.remove`，从无任何 `classList.add`**。`theme.ts:2` 注释即写明「Mo-Blog 主题已移除，仅保留默认主题」，`:31` 无条件摘除该类。因此所有以 `.theme-mo-blog` 为前缀的规则**永不生效**。
- **证据**（**Lead 与报告撰写人双方独立实测，方法不同、结论一致**）：
  - **静态**：`grep -rn "MO_BLOG_CLASS" zhi-ui/src`（排除测试）仅命中 `theme.ts:10`（定义）与 `:31`（`remove`）两处，**无 add**。全仓库 `classList.add` 共 9 处，无一处涉及该类；唯一出现 `add('theme-mo-blog')` 的是测试文件 `theme.test.ts:223`。
  - **构建产物（最强证据）**：`zhi-ui/dist/static/js/` 全部 JS bundle 中，字面量 `theme-mo-blog` **仅出现 1 次**，位于 `bootstrap-Dl1WTx_R.js`，上下文为字符串常量定义 `Mo="theme-mo-blog"`（紧邻 `remove` 调用），**没有任何 add 调用被打包** → 线上运行时该类不会被挂载。
  - **构建产物 CSS**：`dist/static/css/*.css` 中 `html.dark.theme-mo-blog` 组合选择器 **392 处**、`theme-mo-blog` 共 **574 处** —— 即这些死规则**确实被交付到生产**，只是永不命中。
  - **计数说明**：Lead 实测非测试源码共 582 处（其中 `html.dark...theme-mo-blog` 组合 397 处、`mo-blog.scss` 单文件 428 处；组合计数含跨行匹配，与 335 的口径差异来自统计脚本，**不影响结论**，本报告以 582/397/428 为准）。
- **影响（按 Lead 更正后的准确口径）**：
  1. 约 335+ 条 `html.dark.theme-mo-blog` 规则永不生效 → **维护噪音**，且会误导读代码的人以为 Mo-Blog 暗色适配在位；
  2. **Mo-Blog 专属色板（`#1c1917` / `#a8a29e` 等）实际从未被应用**，与代码注释及测试的假设不符；
  3. 它是**高-3 类问题的温床** —— 页面底色依赖一条永不命中的规则，正是企业模板首屏不可见的直接成因。
  **重要限定（Lead 更正）**：**暗色模式主路径是有效的** —— `theme-dark.css:4-33` 的 `html.dark .site-layout, html.dark .mo-auth-page, html.dark .mo-home-page { --mo-n900: var(--el-bg-color); ... }` 由 Element Plus 变量驱动，**正常生效**；死代码是"重复的第二份实现"（`:34-60` 的硬编码 Mo-Blog 色板），**不是唯一实现**。因此 `profile/index.vue:1246-1308` 那组 `:deep()` 覆盖规则不生效，实际外观回落到通用 `html.dark`/`--el-*` 规则；**是否存在可见差异需实测，本报告不作断言**（标待验证 V-8）。
- **修复建议**：二选一 —— 要么补回 `applyAppTheme` 的 add 逻辑，要么删除这 335+ 条死规则；**两者都不做则暗色适配形同虚设并持续制造误读**。建议作为独立清理工作项（见路线图"本迭代"第 6 项）。
- **复核结论**：**已复核（Lead 独立实测 + 本报告以静态与构建产物两种独立方法交叉验证）**。定级采纳 Lead 建议的**中**（当前影响为暗色适配部分失效 + 维护噪音，不直接造成数据或安全问题，但解释了高-3 的成因）。**已按 Lead 第二条消息更正第 3 点措辞**，未写成"该组件暗色适配完全失效"。

---

#### 【中-6】`GlobalExceptionHandler` 向客户端透传原始异常消息（系统性兜底路径）

- **级别**：中（verifier 独立补报，**本次新增发现**）
- **位置**：`zhi-framework/src/main/java/com/zhi/framework/web/exception/GlobalExceptionHandler.java:105`（`RuntimeException`）、`:116`（`Exception`）
- **问题**：兜底 handler 直接把 `e.getMessage()` 回给客户端。项目大量抛出的自拟消息（如 `IllegalArgumentException("文章别名只允许…")`）无问题，但 `RuntimeException` 兜底**同样捕获 JDBC/MyBatis 层异常**，其 `getMessage()` 常含 SQL 片段、表名、列名 —— 例如 `Unknown column 'create_by' in 'field list'`，**正是本项目历史上真实发生过的那类错误**。
- **证据**（已复核）：
  ```java
  100:  @ExceptionHandler(RuntimeException.class)
  101:  public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
  104:      log.error("请求地址'{}',发生未知异常.", requestURI, e);
  105:      return AjaxResult.error(e.getMessage());     // ← 原始异常消息回给客户端
  ...
  111:  @ExceptionHandler(Exception.class)
  116:      return AjaxResult.error(e.getMessage());     // ← 同上
  ```
  **Lead 已独立核对 :105/:116 确为 `AjaxResult.error(e.getMessage())`。**
- **影响**：属信息泄漏的"宽口径"入口 —— 任何未被更具体 handler 覆盖的异常都会走这里，可暴露表名/列名/SQL 片段。需管理员或特定输入触发才有内容，不直接越权，故为中危。
- **与同事报告的关系**：`audit-backend-correctness.md` 的「既有守卫复核 #19」已注意到此点，但**主动选择不单列**（原文：「复核为既有设计，本次不另计为一独立发现」）；`audit-security.md` 未列入。verifier 认为**应当单列**，本报告采纳。
- **修复建议**：`return AjaxResult.error("系统繁忙，请稍后重试");`，原始消息只进服务端日志。修复成本极低。
- **复核结论**：**已复核（Lead 独立确认行号与代码）**。

---

#### 【中-7】验证码失败计数器可被用来短时锁定他人账号（自助流程 DoS）

- **级别**：中
- **位置**：`zhi-system/src/main/java/com/zhi/system/service/impl/EmailServiceImpl.java:236-245, 286-295`
- **问题**：失败计数键为 `VERIFY_FAIL_PREFIX + email + ":" + codeType`，**不含发起方 IP/会话**；达到阈值后**正确验证码同样被拒**（提前 `return false`）。
- **证据**：
  ```java
  // :236
  String failKey = VERIFY_FAIL_PREFIX + email + ":" + codeType;
  // :239-245
  if (failCount >= emailCodeConfig.getMaxVerifyAttempts())
  {   log.warn("验证码已锁定：...");  return false;   // ← 合法用户的正确码也被拒
  }
  ```
- **影响**：攻击者只要知道受害者邮箱，即可对 `/api/auth/reset-password`（无限流，见高-4）连续提交 5 次**故意错误**的验证码，触发 `verifyLockMinutes=5` 锁定；锁定期间受害者本人的正确码也会被拒。即 **5 个无需认证的请求 → 5 分钟内该邮箱的注册/重置通道不可用**。影响有限（5 分钟自愈、需已知邮箱），但可重复触发。
- **修复建议**：失败计数键加入请求来源维度（`email:codeType:ip`），或改为"软锁定"——锁检查移到 `code.equals(...)` 之后，仅阻断继续爆破、不拒绝正确码；至少为 `/reset-password` 补 IP 限流抬高成本。
- **复核结论**：**已复核（verifier 确认机制与 `EmailServiceImpl` 一致；未逐行复算计数逻辑）**。

---

#### 【中-8】CORS 允许任意来源（`addAllowedOriginPattern("*")`）

- **级别**：中
- **位置**：`zhi-framework/src/main/java/com/zhi/framework/config/ResourcesConfig.java:83`（报告引 :78-95 区间亦准确）
- **证据**（已复核）：
  ```java
  83:  config.addAllowedOriginPattern("*");
  85:  config.addAllowedHeader("*");
  87:  config.addAllowedMethod("*");
  92:  source.registerCorsConfiguration("/**", config);
  ```
- **缓解因素（已核实）**：**未调用 `setAllowCredentials(true)`**，浏览器不会跨域携带 Cookie；系统用 `Authorization` 头传 JWT，跨域站点读不到同源 localStorage，**不构成典型会话骑乘型 CSRF**。定级"中"合理。
- **影响**：任意来源可无凭证调用匿名接口（放大高-4 的爆破能力，可用访客浏览器做分布式请求源）；一旦未来引入 Cookie 认证即立刻升级为 CSRF；生产中不必要地放大了攻击面。
- **修复建议**：生产按 `blog_url` 收窄为白名单来源，移除 `/**` 通配；开发可保留宽松配置。补一条交叉验证测试，断言 prod 下 `allowedOrigins` 不含 `*`。
- **复核结论**：**已复核（verifier 确认 :83 与区间行号准确、`allowCredentials` 确未开启）**。

---

#### 【中-9】Druid 监控密码以明文写入应用日志

- **级别**：中
- **位置**：`zhi-framework/src/main/java/com/zhi/framework/config/DruidServletConfig.java:53`（另有 `:73` 的 `Math.random()`）
- **证据**（已复核）：
  ```java
  53:  log.warn("已自动生成临时密码: {}", druidPassword);   // ← 明文落日志
  73:  for (int i = 0; i < 16; i++) { ... }              // generateSecurePassword 用 Math.random()
  ```
- **缓解因素（已核实）**：该 Bean 带 `@ConditionalOnProperty(...statViewServlet.enabled, havingValue="true")`（`:46`），而 AGENTS.md 明确记录 Druid 控制台有意不再配置 → **该 Bean 默认不创建**，正常部署下这行不会执行。故为中危。
- **影响**：若被启用且未配 `DRUID_PASSWORD`，随机口令明文入日志，任何能读日志的角色（运维、日志聚合平台、容器日志查看者）直接获得凭据；且 `Math.random()` 非密码学安全。
- **修复建议**：删除明文打印（改为"已生成临时密码，请查阅环境变量配置"）或仅非 prod 打印；若启用该 Servlet，把 `Math.random()` 换 `SecureRandom`；保持 `statViewServlet.enabled` 默认关闭。
- **复核结论**：**已复核（verifier 确认 :53、:73 及 `@ConditionalOnProperty` 缓解判断均正确）**。

---

#### 【中-10】浏览量同步"取出即删"，写库失败则该批增量永久丢失

- **级别**：中
- **位置**：`zhi-admin/src/main/java/com/zhi/web/task/ArticleViewSyncTask.java:40`（GETDEL）、`:43`（写库）
- **证据**（已复核）：
  ```java
  40:  Integer increment = redisCache.getAndDeleteCacheObject(key);  // ← GETDEL，先删
  43:  articleMapper.addIncrementViewCount(articleId, increment.longValue());  // ← 后写库
  46:  } catch (Exception e) { log.error("同步文章浏览量失败，key: {}", key, e); }  // ← 不回补
  ```
  `RedisCache.getAndDeleteCacheObject`（`RedisCache.java:122-127`）底层为 Redis `GETDEL`，确为原子"取出即删"。
- **影响**：`GETDEL` 先于 DB 写入，`:43` 抛异常（DB 抖动、连接池耗尽）时键已删除、增量只存在于局部变量、catch 仅打日志不回写 → **该批浏览量永久丢失**，`zhi_article.view_count` 永久小于真实值。任务每 10 分钟一次，故障窗口内增量全丢。
- **修复建议**：改为"读 + 成功写库后删"两步，失败保留键等下轮重试；注意此改法下"写库成功但 delete 失败"会重复计数，更严谨可用 `DECRBY` 消费 + 失败 `INCRBY` 回补，或 Lua 保证原子性。**至少应保证不丢（宁可重复计也不静默丢）**，并给失败批次加计数告警（关联低-16）。
- **复核结论**：**已复核（verifier 确认 :40/:43 与 `RedisCache` GETDEL 机制完全正确）**。

---

#### 【中-11】`CacheAspect` 不解析 SpEL，`#x.hashCode()` 变成字面量

- **级别**：中
- **位置**：`zhi-common/src/main/java/com/zhi/common/cache/aspect/CacheAspect.java:223`（另 `:210`）
- **证据**（已复核）：
  ```java
  223:  result = result.replace("#" + paramName, keyFragment(argValue));   // ← 朴素字符串替换
  ```
  三个 `@SiteCacheable` 键确含 `.hashCode()`：`CategoryServiceImpl:60`、`TagServiceImpl:38`、`ArticleServiceImpl:663`。替换后键里出现**字面量 `.hashCode()`**，从未被求值。
- **影响**：① 键名不符预期且含 `Category.toString()`（`ToStringBuilder` MULTI_LINE_STYLE，**含换行符**），调试困难；② 更实质的是**键的语义等价性被破坏** —— 目前"能用"是因为 `toString` 恰好覆盖了所有业务字段，属巧合；③ 若 `toString` 未来加入易变字段，缓存命中率骤降为 0 且**不会被任何测试发现**。
- **修复建议**：要么在切面真正求值 SpEL（`MethodBasedEvaluationContext` + `SpelExpressionParser`），要么把注解键改写为切面能解析的形式（如 `zhi:category:list:#category`，依赖 `keyFragment` 的 `>64` 字符哈希退化保护，`CacheAspect:134-143` 已实现）。**推荐后者**（改动小、无新依赖），并在注解 javadoc 明确"只支持 `#param`/`#pN` 简单插值，不支持方法调用"。
- **复核结论**：**已复核（verifier 确认 :223 为朴素替换、三个键确含 `.hashCode()`）**。

---

#### 【中-12】留言冷却 check-then-set 竞态，并发提交可绕过 60 秒限制

- **级别**：中
- **位置**：`zhi-admin/src/main/java/com/zhi/web/controller/site/FrontMessageController.java:184`（检查）/ `:220`（设置）
- **证据**（已复核）：
  ```java
  184:  long cooldownRemain = remainingCooldownSeconds(cooldownKey);   // ← 检查
  185:  if (cooldownRemain > 0) { return error("留言太频繁啦..."); }
  ...    // 中间隔着 :191 验证码校验与 :217 DB insert
  220:  unifiedCacheManager.set(cooldownKey, System.currentTimeMillis(), SUBMIT_INTERVAL_SECONDS, SECONDS);
  ```
- **影响**：`:184` 的 `exists` 与 `:220` 的 `set` 之间隔着验证码校验与一次 DB insert，**非原子**。同一 IP 在窗口内并发发起 N 个 `POST /api/message` 可绕过 60s 限制；配合 `@RateLimiter`（60s/5req），单 IP 最多一次写入 5 条。属中等危害（有 RateLimiter 兜底，非无限刷）。
- **修复建议**：改用 Redis `SET key value NX EX 60` 原子抢占，抢不到即冷却中。**注意保持现有"冷却检查在验证码之前"的语义**（AGENTS.md 明确要求，避免拒绝时烧掉一次性验证码）—— 若改为先抢占，验证码失败时须 `delete` 回滚抢占，否则误伤。
- **复核结论**：**已复核（verifier 确认非原子，并对"冷却先于验证码"的兼容性提醒到位）**。

---

#### 【中-13】管理员头像更新路径吞掉写失败仍返回成功

- **级别**：中
- **位置**：`zhi-admin/src/main/java/com/zhi/web/controller/system/SysProfileController.java:180`（镜像写）、`:188`（清缓存）
- **证据**（已复核）：
  ```java
  180:  } catch (Exception e) { logger.error("更新 sys_config 表中的 blog_avatar 失败", e); }
  188:  } catch (Exception e) { logger.error("清除站点设置缓存失败", e); }
  ```
  该路径同时写 `zhi_setting.blog_avatar`（:157）与 `sys_config.blog_avatar`（:169/179），并在 :185 手动清缓存。
- **影响**：`:180` 失败 → 用户头像与 `zhi_setting` 已更新但 `sys_config` 镜像仍旧 → 前台读到不一致头像（`FrontController.getSettings` 以 `sys_config` 为主数据源）；`:188` 失败 → `zhi:settings:all` 未清 → 前台最长 600 秒仍显示旧头像，**且接口仍返回成功**。
- **措辞收敛（硬性）**：原文称"是全仓库唯一残留的同类路径" —— **改为"在本次审查范围内发现的一处"**（verifier 未穷尽验证"唯一"这一绝对断言）。
- **修复建议**：与 `SettingController:229-233` 对齐 —— 镜像同步失败时返回错误提示（该项目已有 `SettingControllerTest#testUpdateSettingByKey_Post_SyncFailure_ReturnsError` 锁定同类修复）；至少把"清缓存失败"升为 `warn` 并计入指标。
- **复核结论**：**已复核（verifier 确认 :180/:188 两处吞异常；"唯一"措辞按要求收敛）**。

---

### 2.4 低（21 条，摘要式列出）

| # | 标题 | 位置 | 问题与影响（摘要） | 复核结论 |
| --- | --- | --- | --- | --- |
| 低-1 | RSS 输出未做 XML 转义 | `zhi-admin/.../site/FrontController.java:1026`（另 `:1051`） | `out.println("<link>" + siteUrl + "</link>")` 无转义；CDATA 未处理 `]]>`。**触发者需 `zhi:setting:edit` 权限（管理员）**，非匿名可达。对照 `SeoController.writeUrl`（:196-205）做对了 | **已复核**，行号准确 |
| 低-2 | `/system/article` 被排除出 XSS 过滤器（仅 dev profile） | `application.yml:256` vs `application-prod.yml:87` | dev 默认 `excludes` 含 `/system/article`，文章正文原文入库；**生产生效的是 prod 的 `excludes: /api/**,/index`，不含该项 → 生产是安全的**。属"配置语义不一致 + 纵深防御单点化（100% 依赖前端消毒）"。**原报告主动纠正了"生产也存在存储型 XSS"的误读，值得肯定** | **已复核**，且 verifier 表扬其自我纠偏 |
| 低-3 | `.scss` 中 2 处深色背景色阶作文字色（token 守卫盲区） | `mo-blog.scss:994`、`:1558` | `html.dark.theme-mo-blog .footer-wave { color: var(--mo-n900) }`、`.title-input .el-input__inner { color: var(--mo-n900) !important }`。**因前缀为 `html.dark.theme-mo-blog`（死选择器，见中-5）→ 当前不可触发，无可观测影响**，清理时应一并修正。`dark-mode-tokens.test.ts` 的 `collectVueFiles` 只收集 `.vue`，`.scss` 不参与语义扫描 —— **结构性盲区成立** | **已复核（verifier 独立扫出；定级按 Lead 更正降为低，不定为高）** |
| 低-4 | `.tpl-site` 在详情页形态下拿到深浅两套 `--tpl-*` | `site-templates.scss:148-165`；`article/detail.vue:2` | `html.dark.tpl-site` 改 `<html>` 的值，但根容器 `.article-detail-container.tpl-site` 继承 `:root` 浅色值，按就近原则后代仍取浅色。**原报告自标"待验证"，静态无法定论层叠顺序** | **降级为低/待验证**（verifier：影响面为推断） |
| 低-5 | `--mo-*` 深色重映射在 `scoped` 组件中失效 | `SiteLayout.vue`、`theme-dark.css:4-31` | 机制描述合理（重映射只写在容器后代上，portal 组件如 `el-image-viewer__wrapper` 会脱离作用域），但 **verifier 独立扫描 78 个 `.vue` / 223 条 `html.dark` 规则，未发现任何深色色阶被当文字色的违规**；报告举的 teleport 场景是**推断而非实测** | **降级为低/待验证**（原「高」不成立） |
| 低-6 | 首页侧栏深色强调色文字依赖逐条手写覆盖（原 8 处，实为 1 处漏网） | `views/site/index.vue:557`（漏网）；`:568/624/654/680/691/779/795` | 8 个行号**逐一打开全部属实**（均写作 `color: var(--mo-p6xx/p7xx)`），但"无深色覆盖"的判断对其中 **7 处是错的** —— 分别被 `:895`/`:912`/`:928`/`:938`/`:958`/`:972` 按特异性覆盖且深色规则胜出。**仅 `:557`（`.author.clickable:hover`）真正漏网**（浅色 hover 特异性 (0,6,0) > 深色非 hover (0,5,1)）。内核仍成立：`index.vue:852-853` 把 `--mo-p600: #4f46e5`/`--mo-p700: #4338ca` 显式重映射后**仍是深靛蓝**，压在深底上实测仅 **1.92~2.78:1**（正确做法应用 `--mo-p300` = 8.77:1） | **部分撤销（7/8）+ 改写理由（verifier 4.1 节硬性要求）** |
| 低-7 | 站点设置接口失败被静默吞掉 | `SiteLayout.vue:656`、`stores/siteSettings.ts:60` | `getSiteSettingsAnonymous()` 的 `.then` 链无 `catch`；叠加 `isFeatureEnabled` 的**默认开启**语义 → 接口失败时"用户显式关掉"的开关会被错误打开（如 `guestbook_enabled=false` 仍渲染留言板入口） | **已复核**（verifier 确认，但未独立复现） |
| 低-8 | 前台 401 被改写成 `{code:200,data:null}` | `zhi-ui/src/utils/request.ts:176` | 设计意图（匿名可浏览）正确，但实现把 401 变成**不可区分的成功**：留言板提交失败时提示"留言成功"并进入 60s 冷却（`guestbook/index.vue:320-333`）；登录 401 也走此分支，用户看不到真实原因。**建议附可复现的最短步骤**（verifier 未独立复现） | **已复核（机制自洽，未独立复现）** |
| 低-9 | RuoYi 存量只读下拉助手对任意登录用户开放 | `SysDictTypeController.optionselect:126`、`SysPostController.optionselect:124`、`SysMenuController.treeselect:61`/`roleMenuTreeselect:71` 等 | 这 81 个无 `@PreAuthorize` 方法**均在 `anyRequest().authenticated()` 之内，无匿名可达**；风险收窄为"已登录的注册用户（`user_type='01'`）可枚举后台岗位/字典/角色菜单结构"。**非越权操作，属信息暴露** → **低** | **verifier 独立补报，已复核** |
| 低-10 | `CommonController` 上传端点在"已登录但无权限"下即可用 | `CommonController.java:86/114/146/178/210/242/274/311` | 这些端点不在 `/api/**` 白名单内、需登录，但**不需任何权限串**，任意注册用户可上传（受 `@RateLimiter` 与类型/路径校验约束）。下载类（`fileDownload:56`、`resourceDownload:349`）**路径穿越防护未在本轮穷尽验证 → 待验证 V-7** | **verifier 独立补报，部分待验证** |
| 低-11 | 权限串冗余：库里有但代码不用的 25 条 | `sql/00_init_database.sql` sys_menu perms | **不构成安全问题** —— 菜单给了权限但接口不单独校验，最坏结果是"按钮可见但接口用 list 权限放行"，属**权限模型冗余**。含 `monitor:*` 外链项与本项目自建的 `statistics:*:query/export` | **已复核（数字修正，见 4.3）** |
| 低-12 | `zhi:comment:approve` 的事实性偏差 | `sql/00_init_database.sql:1583` | **该串不是现存菜单 perms 值** —— 它只作为迁移语句 `UPDATE sys_menu SET perms='zhi:comment:audit' WHERE ... perms='zhi:comment:approve'` 的 **WHERE 条件**存在。原文"菜单里有该权限"的陈述**已撤销** | **撤销该事实陈述**（verifier 4.1 节硬性要求） |
| 低-13 | 默认管理员使用 RuoYi 广为人知弱口令哈希 | `sql/00_init_database.sql:142`（`admin`）、`:143`（`zhiqi`） | `$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2` = RuoYi 默认 `admin123`（已复核）。**缓解现状（正面）**：`sys_config:184` 已把 `sys.user.initPassword` 从 `123456` 改为 `CHANGE_ME`，`initPasswordModify='1'` 强制弹改密提醒 —— 项目已意识到该问题，只是漏了 `admin` 自身这行 | **已复核**，行号准确 |
| 低-14 | `GRANT` 写死库名 `zhienterprise` | `sql/00_init_database.sql:56` | 是"脚本不再写死库名"修复的**唯一遗留点**（脚本内已有注释）。非 Docker 且 `.env` 改了 `DB_NAME` 时授权落空库（MySQL 对不存在的库授权不报错），运行期报 `Access denied`。**Docker 部署不受影响** | **已复核** |
| 低-15 | 循环内单条 INSERT（N+1 写入） | `ArticleServiceImpl.java:528-533`、`540-553`、`270-286` | 一次请求 N 次往返，标签多时延迟线性上升；`deleteByArticleId` 已是整批删除，写入侧未批量。建议新增 `insertArticleTagBatch`（`<foreach>` 多值 INSERT） | **已复核** |
| 低-16 | 定时任务日志/告警缺失 | `zhi-admin/.../task/*.java`（4 个 `@Scheduled`） | catch 块均只 `log.error`，无指标/告警。`ArticleViewSyncTask` 的失败直接对应中-10 的增量丢失。建议失败计数暴露到 Micrometer/Actuator | **已复核** |
| 低-17 | `resolveSpEL` 子串误替换风险 | `CacheAspect.java:210, 223` | 朴素 `String.replace` 会把 `#article` 误伤 `#articleType` 等前缀重叠的更名参数；**当前注解键恰好不重名，未实际触发**。建议按参数名长度降序替换 | **已复核** |
| 低-18 | 设置写入错误信息不够定位 | `SettingController.java:232` | 提示未带 `settingKey`，运维无法从 UI 判断是哪个键。建议附上键名 | **已复核** |
| 低-19 | 表排序规则不统一（**前提需修正**） | `sql/00_init_database.sql` 各 `CREATE TABLE ... COLLATE` | 机制实测成立（跨组 JOIN 报 `ERROR 1267`），**但是否发生取决于建库路径** —— 详见第四章 4.4 的完整说明，**该前提必须写明，否则按 Docker 复现不出** | **已复核（"当前分布"表述已按实测修正，见 4.3）** |
| 低-20 | Spring Boot 版本号文档错误 | `AGENTS.md:3`（写 3.3.0，实际 **3.5.16**） | `CLAUDE.md:7`（3.5）与 `README.md:3/155` 正确；`pom.xml:30` 为 `3.5.16`；**构建产物权威实测**：`zhi-admin.jar` MANIFEST `Spring-Boot-Version: 3.5.16`。另 `pom.xml:26-29` 注释仍在讲"3.3.13 是 3.3.x 最后 OSS 版本"，与取值自相矛盾。**影响限于文档准确性，无功能影响，故实际定级为低** | **已复核（行号/版本号已修正，原定级「中」→「低」）** |
| 低-21 | 其余工程化/文档项 | 多处 | ① `img` 缺 `alt` 3 处（`SiteLayout.vue:82`、`ImageUpload/index.vue:41/74`；另 11 处为多行标签误报）；② 唯一未消毒 `v-html` 点是**设计内静态 SVG 母题**，但 `SolutionsSection.vue:78` 的模板 id 来自站点设置，链路已存在、缺守卫；③ 首页封面走 CSS `url()` 无法懒加载（`index.vue:53-58`，其余列表均已用 `loading="lazy"`）；④ `sql/README.md:62` 判据表述过窄（有唯一键同样幂等）；⑤ README 默认密码提示未给具体口令；⑥ `.workflow/ReleasePipeline.yml:30` 的 tag 校验弱于 `release.yml:20-25`（Gitee 侧未检出 tag 仅告警不阻断）；⑦ 部分系统表缺 PK 以外索引；⑧ `.env` 相关项**均正常**（见第三章） | **已复核（低危项，未逐条复现）** |

---

## 三、复核记录（报告可信度的核心）

本次采用**对抗式复核（证伪优先）**：复核人不依赖同事结论，独立重跑解析器、在真实 MySQL 8.4 上做破坏性验证、核对构建产物。**47 条原始发现中推翻 4 条（8.5%）、降级 5 条、修正 2 处。**

### 3.1 撤销的 4 条误报（**均未进入第二章发现清单**）

#### 撤销 1：`frontend S-2`「企业模板下所有字号/间距变量回退浏览器默认值」（原定「严重」）

- **原文主张**：企业模板下 `--mo-fs-*`/`--mo-sp-*` 全部失效、回退浏览器默认值，根因是这些变量定义在"已下线的 `mo-blog.scss`"里。
- **反驳证据链（4 步，verifier 独立完成）**：
  1. **定义位置是 `:root` 而非皮肤类**：`mo-blog.scss:23` 是唯一的顶层选择器 `:root {`，`--mo-fs-sm` 在 `:74`、`--mo-fs-6xl` 在 `:82`（**已由本报告独立复核**：`sed -n '23p;74p;82p'` 分别输出 `:root {`、`--mo-fs-sm: 0.875rem;`、`--mo-fs-6xl: 3rem;`）。`:root` 作用于 `<html>`，**不受类名增删影响**。
  2. **该文件确实被引入**：`themes/index.scss` 有 `@use './mo-blog.scss';`。原文在"待确认"里也承认自己"未逐行核对 `index.scss` 的 import 列表"。
  3. **构建产物（最强证据）**：`dist/static/css/bootstrap-*.css` 的全局 `:root` 规则中确实存在 `--mo-fs-sm=.875rem`、`--mo-fs-lg=1.125rem`、`--mo-fs-6xl=3rem`、`--mo-sp-8=40px`。**变量链是通的**。
  4. **原文引用的期望值本身有误**：原文称 `--mo-fs-6xl` 期望 38px，源码 `:82` 实为 `3rem`（=48px）。期望值错 → "32px 是 38px 的降级"这一因果推断不成立。
- **处置**：**撤销"严重"定级，该条不作为发现写入最终报告**。若确要保留 headless 观测到的 16px 现象，必须**重新归因**并提供"元素实际计算样式 + 该元素 CSS 规则来源"的新证据。
- **诚实说明**：原文在 S-2 里主动写了"请以 `npm run build:prod` 的实际 CSS 产物复核后定级"——**这个自我提示恰恰是判它误报的关键**。同事的谨慎态度值得肯定，本次撤销表述为"按其自身给出的复现方法未能复现"，而非指责。

#### 撤销 2：`frontend M-1` 中 `:568/624/654/680/691/779/795` 七处「无深色覆盖」的表述

- **原文主张**：8 处浅色规则使用 `--mo-p600/p700` 作文字色且**无深色覆盖**。
- **反驳**：8 个行号**全部属实**（本报告已逐行打开确认均写作 `color: var(--mo-p6xx|p7xx)`），但"哪些已被深色覆盖"的判断有 **7 处错误**。按 CSS 特异性 (id, class, element) 逐一计算：

  | 行号 | 深色覆盖 | 特异性比较 | 结论 |
  | --- | --- | --- | --- |
  | `:557` | `:895`（**无 `:hover`**） | light (0,6,0) > dark (0,5,1) | ✅ **成立（唯一漏网）** |
  | `:568` | `:975` → `p300` | dark (0,7,2) > light (0,2,0) | ❌ 不成立 |
  | `:624` | `:912-913` | dark (0,4,1) > light (0,3,0) | ❌ 不成立 |
  | `:654` | `:928-929` | dark (0,4,1) > light (0,3,0) | ❌ 不成立 |
  | `:680` | `:975` | dark (0,7,2) > light (0,3,0) | ❌ 不成立 |
  | `:691` | `:938` | dark (0,4,1) > light (0,4,0) | ❌ 不成立 |
  | `:779` | `:958` | dark (0,3,1) > light (0,2,0) | ❌ 不成立 |
  | `:795` | `:972` | dark (0,3,2) > light (0,2,1) | ❌ 不成立 |

  （本报告独立复核了 `:895/912/928/938/958/972` 六个选择器确实存在且确为 `html.dark` 前缀；`:975` 处行号有一行偏移，不影响结论。）
- **改写后的表述（已按 verifier 4.1 节硬要求执行，见低-6）**：M-1 的**内核仍成立，但理由要换** —— `index.vue:852-853` 把 `--mo-p600: #4f46e5`、`--mo-p700: #4338ca` 显式重映射后**仍是深靛蓝**，实测对比度仅 **1.92~2.78:1**（`--mo-p300` 才是正确做法，8.77:1）。风险点在于**依赖逐条手写深色覆盖，任何漏写一条即回退到 p600/p700，实测 `:557` 已漏网**。
- **处置**：撤销 7 处的"无覆盖"表述，保留 1 处（`:557`）为真实漏网，理由改写为"逐条手写覆盖的漏写风险 + 色板本身在深色下不可读"。

#### 撤销 3：`backend H-3`「邮件配置保存不失效 `zhi:settings:all`」的「高」定级

- **原文主张**：违反 AGENTS.md"每条写 `zhi_setting` 的路径都必须带 `@SiteCacheEvict`"，定「高」。
- **反驳**：**事实成立但危害被显著高估。** 复核人独立追证链路 `saveMailConfig`（`SettingController.java:274`）→ `MailConfigService.save`（`:150`）→ `upsert` → `settingService.updateSettingValueByKey`（`SettingServiceImpl.java:93-105`）**确实无任何缓存失效**。**但是**：`mail_*` 键（`mail_host/port/username/password/ssl/starttls/enabled`）**不在** `FrontConfigKeys.CONFIG_KEYS` 白名单内，而 `zhi:settings:all` 的构造**只遍历该白名单**，因此该缓存**根本不可能包含 `mail_*` 键**；且 `MailConfigService.read()` 走 `selectSettingValueByKey` **直读 DB，不经缓存**。→ **无任何可观测影响，也没有脏读路径。** 原文自己也写了"实际危害被白名单兜住"。
- **处置**：**撤销「高」定级**。（按 verifier 4.4 节第 3 条：若保留，必须在同一段内明确「当前无可观测影响」，不能只把白名单兜底写在影响段后半句。）本报告将其列为**低-级规则例外**（并入低-21 类工程化项），措辞为"规则例外 + 建议补注解统一风格"。

#### 撤销 4：`security L-2` 中关于 `zhi:comment:approve` 的「菜单里有该权限」事实陈述

- **原文主张**：「`zhi:comment:approve` —— 菜单里有，但审核接口实际用 `zhi:comment:audit`，已无接口引用」。
- **反驳**：实测 `grep -c "zhi:comment:approve"` 严格解析结果 → **0**；查运行库 `SELECT ... WHERE perms LIKE '%comment:approve%'` → **空**。该串**只出现在** `sql/00_init_database.sql:1583` 的迁移语句里：`UPDATE sys_menu SET perms='zhi:comment:audit' WHERE menu_id=20041 AND perms='zhi:comment:approve';` —— 它是**历史值的清理条件**，不是现存菜单项。
- **处置**：**撤销该事实陈述**（见低-12）。若保留原文措辞，会误导读者去删一个不存在的菜单。

### 3.2 降级的 5 条

| 来源 | 条目 | 原级 | 最终级 | 降级理由 |
| --- | --- | --- | --- | --- |
| backend | H-2 `insertArticle` 既无事务也无缓存失效 | 高 | **中**（并入中-1） | 与 S-1（严重-1）、H-1（高-2）**同根同址**，独立列"高"会使同一缺陷被计为多次高优 |
| backend | H-4 `applySettingUpdateByKey` 镜像可能落错行 | 高 | **低**（并入低-21 类） | 事实准确（`SysConfigMapper.xml:49` 确为 `key like concat('%',#{configKey},'%')`），且报告**正确承认当前逻辑是对的**（内存 `.filter(equals)` 已补偿模糊语义）。残留仅"未来重构风险 + 理论并发竞态"，不构成当前缺陷 |
| backend | M-5 `startPage()` + `@SiteCacheable` 的 total 语义 | 中 | **待验证（不单列级别）** | 原文自标"待验证"，核心不确定点（Redis 反序列化后 `Page` 类型与 `total` 是否保留）**纯静态无法定性**。列为「中」级缺陷会误导读者以为已确认 → 移入第五章待验证 V-1 |
| frontend | H-2 `--mo-*` 深色重映射在 scoped 组件中失效 | 高 | **低/待验证**（低-5） | 机制描述合理，但**影响面未证实**：verifier 独立扫描 78 个 `.vue` / 223 条 `html.dark` 规则**未发现任何违规**；原文举的 teleport 场景是**推断而非实测** |
| frontend | S-1 企业模板导航不可见 | 严重 | **高**（高-3） | 结论与证据链完全成立（verifier 手工复算对比度一致），但危害性质为**可见性/观感**而非数据或安全，且滚动 80px 后自愈。按"影响面 × 危害性质"综合收敛至「高」 |

### 3.3 修正的 2 处行号/事实

| # | 条目 | 原文 | 修正后 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | `security H-2` 端点行号 | `:51/71/104/131` | `:51/71/104/131` 与 `:53/73/106/133` **两套均自洽** | 差异源于"`@Anonymous` 注解行 vs `@PostMapping` 注解行"（相差 2 行）。两者都能唯一定位方法，**不构成错误**。本报告统一采用「方法注解块起始行」（`@Anonymous` 行）并在高-4 中注明基准 |
| 2 | `backend M-2` 方法行号 | `:572` | **`:571`（`@Transactional` 行）/ `:575`（方法声明）** | 原文引的是 `@Transactional` 行，方法声明在其后。本报告统一为「方法声明所在行 ± 注解块」并注明基准 |

### 3.4 对 verifier 的独立发现的采纳（5 条）

| verifier 补报 | 最终编号 | 级别 | 采纳说明 |
| --- | --- | --- | --- |
| `GlobalExceptionHandler:105/116` 透传原始异常消息 | 中-6 | 中 | 采纳。同事已注意到但未单列；有明确可复现的泄漏形态，修复成本极低 |
| `mo-blog.scss:994/1558` 深色背景色阶作文字色 | 低-3 | **低**（verifier 原建议「中」） | 采纳发现，**但定级按 Lead 更正降为低** —— 前缀是死选择器 `html.dark.theme-mo-blog`，**当前不可触发、无可观测影响**；清理时一并修正 |
| `CommonController` 上传端点免权限 + 下载路径校验待验证 | 低-10 | 低 + 待验证 V-7 | 采纳；下载类路径穿越防护**未穷尽验证**，如实标注待验证 |
| RuoYi 下拉助手可枚举后台结构 | 低-9 | 低 | 采纳；属信息暴露而非越权 |
| 权限串 B 集冗余 | 低-11 | 低 | 采纳；**明确不渲染成风险**（权限模型冗余） |

**Lead 终验补报（1 条）**：中-5 `.theme-mo-blog` 死选择器 —— 已采纳并**以两种独立方法交叉验证**（静态 `grep` + 构建产物 bundle 解析）。

### 3.5 复核中**未能确认**的项（诚实标注，未以"复核通过"含糊带过）

| 来源 | 项 | 为何无法确认 |
| --- | --- | --- |
| frontend | 低-8（401 被改写成 200） | 需运行前端请求层 + mock 后端构造集成场景；机制自洽，按"确认但未独立复现"处理 |
| frontend | 低-7、低-21 的无障碍/懒加载/JSON-LD 项 | 低危且证据方向自洽，未逐条复现 |
| frontend | 高-3 的 headless 像素数值 | verifier 未启动 headless Chrome 复测；以"源码 + 手工对比度复算"确认（结论一致） |
| security | 中-7（验证码计数锁他人账号） | 未逐行复算计数逻辑；方法存在且机制描述与 `EmailServiceImpl` 一致 |
| backend | 中-13 的"全仓库唯一"措辞 | 未穷尽全仓库所有写路径以证实"唯一" → 措辞已收敛 |
| 全部 | 第五章 10 项待验证 | 需运行环境（真实 Redis/PageHelper/后端进程/浏览器）才能定性 |

---

## 四、质量门禁实测结果

> **重要**：本章数据为 **Team Lead 于 2026-10-17 实跑**，**原样采用，未作任何调整**。

| # | 检查项 | 命令 | 实测结果 | 判定 |
| --- | --- | --- | --- | --- |
| 1 | 后端构建 | `mvn -B -o clean package -DskipTests` | **BUILD SUCCESS**（7 模块全绿） | ✅ 通过 |
| 2 | 代码风格 | `mvn checkstyle:check` | **0 violations** | ✅ 通过 |
| 3 | 后端单测 + 覆盖率门禁 | `mvn -B -o test` | **BUILD SUCCESS**；`zhi-admin` **70 tests 全通过**；JaCoCo 门禁通过 | ✅ 通过 |
| 4 | 覆盖率实测（源自 `target/jacoco/jacoco.csv`） | — | `zhi-system`: LINE 2713/3656 = **74.21%**，BRANCH 912/1356 = **67.26%**<br>`zhi-common`: LINE 786/1095 = **71.78%**，BRANCH 497/689 = **72.13%**<br>`zhi-framework`: LINE 587/768 = **76.43%**，BRANCH 242/348 = **69.54%** | ✅ 三项均高于 **60%/60%** 门槛 |
| 5 | 前端风格 | `npm run lint:check` / `npm run format:check` | lint **exit 0**；format **全部符合 Prettier** | ✅ 通过 |
| 6 | 前端测试 | `npm run test` | **210 个测试文件 / 1856 个测试全部通过** | ✅ 通过 |
| 7 | 版本号一致性 | 逐点核对 | root pom `version`/`app.version`、6 个子模块 parent version、`RuoYiConfig` 兜底、`zhi-ui/package.json` 与 `package-lock.json` —— **全部 1.0.0** | ✅ 全链一致 |
| 8 | `mvn clean install` | — | 本机沙箱下因 **`~/.m2` 不可写** 失败（`Operation not permitted`） | ⚠️ **环境限制，非代码缺陷** —— 应使用 `package` 而非 `install` |

**门禁机制核对（`audit-db-devops.md` 逐项，未发现覆盖缺口）**：

| 声明（AGENTS.md） | 实际 | 判定 |
| --- | --- | --- |
| JaCoCo line ≥60% / branch ≥60% / BUNDLE | `pom.xml:470-472` / `:474-478` / `:467` | ✅ |
| `zhi-admin`、`zhi-generator` `jacoco.skip=true` 但仍跑测试 | `zhi-admin/pom.xml:153-159`、`zhi-generator/pom.xml:75-81` | ✅ |
| `checkstyle:check` 绑定 validate 阶段 | `pom.xml:392-399` | ✅ |
| Vitest 阈值 lines/statements 70、functions 75、branches 55 | `vitest.config.ts:74-77`（Vitest 4 `thresholds` 写法正确） | ✅ |
| CI 覆盖 `mvn verify`、`npm run audit`（registry 已 pin npmjs）、lint/format/typecheck/test:coverage/build:prod、产物上传、Java 17/Node 22 | `.github/workflows/ci.yml` | ✅ **无缺口** |
| Dependabot（maven/npm/github-actions，weekly，patch+minor 分组） | `.github/dependabot.yml` 三项齐全 | ✅ |

**一处值得记录的运维提示（非缺陷）**：两个工作流均硬钉 `runs-on: ubuntu-24.04`（注释说明为规避 `ubuntu-latest` 迁移 Ubuntu 26），属主动规避风险的合理做法，但需在适配后记得放开。

---

## 五、待验证清单（**不得当作已确认缺陷**）

| # | 事项 | 为什么待验证 | 建议验证方式 |
| --- | --- | --- | --- |
| V-1 | 分页 + `@SiteCacheable` 命中时 `total` 是否正确（原 backend M-5） | 需真实 Redis + PageHelper 集成环境；`Page` 经 Redis 序列化后的类型还原行为无法静态判定 | 起后端 + Redis，连续两次请求 `GET /api/article/search?keyword=X&pageNum=2`，断言两次 `total` 一致且非 0 |
| V-2 | `LikeServiceImpl` 并发双击的实际表现 | 唯一键冲突会抛异常，但需确认前端拿到可识别错误而非 500 白屏（`LikeController` 未 catch） | 并发压测 `POST /api/like/article/{id}`，观察响应码与 `like_count` 是否等于真实记录数 |
| V-3 | `SysUserServiceImpl`/`SysRoleServiceImpl` 事务覆盖完整性 | 本次资源集中在内容域；系统域的 8 个 `@Transactional` 方法只做了自调用与注解存在性核查 | 后续专项审查单列 |
| V-4 | 低-4「详情页形态 `--tpl-*` 深浅两套」的最终层叠结果 | 需浏览器读计算值，静态无法确定 `:root` 与 `html.dark .tpl-site` 的层叠顺序 | 深色模式下读 `getComputedStyle(document.querySelector('.article-detail-container')).getPropertyValue('--tpl-ink-900')`，期望 `#f0efec`，若为 `#0b0f19` 则成立 |
| V-5 | 低-5「scoped 组件深色重映射失效」的实际影响面 | portal（teleport）组件如 `.el-image-viewer__wrapper`、`el-dropdown`、`el-dialog` 在深色下取到 `:root` 浅色阶还是 `.site-layout` 重映射值 | 挂载后读 `getComputedStyle` |
| V-6 | `stores/user.ts:88-91`：后端是否存在"`roles` 为空但 `permissions` 非空"的账号形态 | 若有，前端会错误隐藏全部 `v-hasPermi` 按钮 | 查后端账号角色/权限数据形态 |
| V-7 | `CommonController` 下载类端点（`fileDownload:56`、`resourceDownload:349`）的路径穿越防护 | 本轮未穷尽验证下载实现的路径校验 | 用 `../` 类载荷实测下载接口 |
| V-8 | 中-5 死选择器是否造成**可见的**暗色外观差异 | 需实测渲染；`profile/index.vue:1246-1308` 的专用覆盖不生效后实际外观回落到通用 `html.dark`/`--el-*` 规则，**是否存在可见差异未验证** | 深色模式打开个人中心设置页，与通用规则渲染结果比对 |
| V-9 | `@DISABLE_TRIGGERS` 会话变量在非单连接执行器下的行为 | 该方案依赖"同一连接内执行"；本次实测用单连接 `mysql <` 重定向，行为正确 | 用分连接执行器跑脚本验证 |
| V-10 | 生产 Actuator `metrics` 实际暴露面、nginx `/manage/` 可达性、`.env` 生产密钥强度 | 需运行环境与实际凭据，超出静态审查范围 | 以 prod profile 启动后请求 `/manage/actuator/metrics`；确认 nginx `location /manage/`（`nginx.conf:123`）是否外网可达；由运维确认 `R_TOKEN_SECRET` ≥64 字符且随机 |

---

## 六、已验证健康的部分

> 本节目的是明确经检查**确认为好**的方面，避免读者只看到问题清单而误判项目整体质量。**未列出的类别不代表已检查无问题，仅代表本次未覆盖。**

### 6.1 权限与鉴权模型 —— 无缺口

1. **权限串「代码要求但库里没有」= 0 条** ✅
   - 方法：Java 侧 `@ss.hasPermi('...')` 单引号字面量 **145 个**；SQL 侧按 `sys_menu` 显式列清单解析 + **运行库 `sys_menu` 二次确认**，均为 **170 个**（去重后）。
   - 结果：`comm -23` 差集 **为空**。**即不存在"权限不存在导致接口对所有人 403"的问题。**
   - 这是本次审查**最重要的正面结论**，且是在**运行中的数据库**上复核的，不只是解析脚本。
2. **81 个无 `@PreAuthorize` 的方法全部处于登录门槛内** ✅ —— `SecurityConfig.java:170-177` 的 `anyRequest().authenticated()` 保证无匿名可达；风险面收窄为"已认证但无权限的注册用户"，**未发现直接越权读写他人数据的实例**。
3. **评论归属校验** ✅ —— 改/删仅限本人（`FrontController.java:826-830`/`:870-874` 校验 `userId.equals(...)`）。
4. **站内信按收件人隔离（无 IDOR）** ✅ —— `NotificationMapper.xml:69-85` 的 `markAsRead`/`markAllAsRead` 均带 `and recipient_id = #{recipientId}`；`NotificationController.java:33-70` 全部从 `SecurityUtils.getUserId()` 取。
5. **个人信息接口不构成批量赋值越权** ✅ —— `SysProfileController.java:87-93` 逐字段拷贝。
6. **注册用户 `user_type='01'` 全链路正确** ✅ —— 两条注册链路均显式写 `'01'`（`SiteUserService.java:41/118`、`SysRegisterService.java:82`）；`SysUserMapper.xml` insert 含 `<if test="userType != null...">`（:27/52/159），有 `SysUserMapperTest`（H2）锁定 insert↔select 往返。
7. **注册开关严格 `"true"` 口径未误用 `SiteSwitchUtils`** ✅ —— `AuthController.java:55-58`。

### 6.2 版本号 —— 全链一致

**12 处全部为 `1.0.0`** ✅（root `pom.xml:9` `<version>` / `:24` `<app.version>`；6 个子模块 parent version；`RuoYiConfig.java:19` 兜底；`zhi-ui/package.json:3`；`package-lock.json:3` 与 `:9`）。
另有 `application.yml:6` 的 `@app.version@` 经 Maven filtering 正确替换（`zhi-admin/pom.xml:95-102` 对 `**/*.yml` 开 `filtering=true`）—— 合计 **13 处无一处不一致**。`ReleasePipeline.yml` 的 tag↔pom 校验机制在位。

### 6.3 匿名写端点防护 —— 限流/验证码/时间窗齐备

| 端点 | 防护 | 证据 |
| --- | --- | --- |
| `POST /api/comment` | `@RateLimiter` 60s/10 | `FrontController.java:730-731` |
| `POST /api/article/view/*` | `@RateLimiter` | `FrontController.java:580-581` |
| `POST /auth/login` | `@RateLimiter` 60s/20 IP | `UnifiedAuthController.java:35` |
| `POST /api/message` | 限流 + 图形验证码 + **时间窗 <3s 拒绝** + 60s 冷却 | `FrontMessageController.java:184/191/196-200` |
| `/common/upload*`（全部 8 个） | `@RateLimiter` 60s/20 | `CommonController` |
| 友链申请 | 验证码 + IP 限流 | `FriendLinkController.java:76-77` |

- **留言板三接口（含读取）均受 `guestbook_enabled` 短路** ✅ —— `:89-99`（list）、`:111-115`（count）、`:130-133`（add），与 AGENTS.md「reads too」一致。
- **冷却检查在验证码之前** ✅ —— `:184`（冷却）先于 `:191`（`validateAndGetAgeSeconds`），避免拒绝时烧掉一次性码；`FrontMessageControllerTest:368` 锁定。
- **图形验证码一次性不可爆破** ✅ —— `CaptchaServiceImpl.java:64-66` 先 `getExpire` 再 `delete`，无论比对成败都已删除。
- **邮箱验证码失败计数 + 锁定** ✅ —— `EmailServiceImpl.java:239-245`、`:286-295`（上限 5 次 / 锁 5 分钟）。
- **公开留言列表剔除 `email`/`ip`/`user_agent`** ✅ —— `MessageMapper.xml:27-30` 列白名单无这三列。
- **公开评论接口不含 email（PII）** ✅ —— `FrontCommentVo.java:11-40` 显式投影。

### 6.4 前端 token 契约 —— `.vue` 范围内 **0 违规**

| 方向 | 扫描范围 | 违规数 |
| --- | --- | --- |
| 暗色：`--mo-n0/n50/n800/n900` 作 `color` | `.vue`（78 文件 / 223 条 `html.dark` 规则） | **0** |
| 暗色：`--mo-p600~p900` 作 `color` | 同上 | **0** |
| 亮色：`--mo-n300/n400/p200/p300` 作 `color`（排除装饰性空态图标） | 同上（4073 条非 dark 规则） | **0** |
| 暗色：背景色阶作 `color`（`.scss` 扩展扫描） | `mo-blog.scss` | **2**（死选择器内，见低-3） |

- **verifier 用自写独立解析器（按 `dark-mode-tokens.test.ts` 同一规则）重扫 78 个 `.vue`，与同事测试结论一致**；并实跑同事测试：`npx vitest run src/views/site/dark-mode-tokens.test.ts` → **7 passed**。
- **admin 区域 `--mo-*` 零命中** ✅，全部走 `--el-*`，AGENTS.md 该条执行到位。
- **XSS：`v-html` / 内联事件 / `javascript:`** ✅ —— 14 个 `v-html` 中 13 个可追溯到 `sanitizeArticleContent`；全仓 grep `onerror=`/`onclick=`/`javascript:` 命中 9 处**全部为良性**（`window.onerror`、DOM 属性赋值、黑名单本身、注释）。
- **JSON-LD 注入无面** ✅ —— `utils/jsonld.ts` 用 `el.textContent = JSON.stringify(data)` 而非 `innerHTML`。
- **内存/性能** ✅ —— 24 处 `setInterval`/`setTimeout`、18 处 `addEventListener` **均配对清理**；无虚拟滚动需求（分页尺寸 10）。
- **`userType === '00'` 门禁健壮** ✅ —— `stores/user.ts:84-87` 缺失时**降级为 `'01'`**；4 处判断点一致；`goToAdmin()` 仅 `router.push`，**无客户端特权**，不构成鉴权绕过。

### 6.5 安全契约逐条复核（AGENTS.md「必须仍成立」）

以下 32 条经逐条读码复核**确认实现正确**（详见 `reports/audit-security.md` 末章）：

- `/error` 仍 `permitAll`（`SecurityConfig.java:140-141`）✅
- `RefererFilter` 按 **host** 比较而非 substring（`:96-134`）✅
- `IpUtils` 只在直连对端可信时采信 XFF，从**右往左**取（`:58-61`/`:140-156`）✅
- **生产 XSS 过滤器开启** ✅ —— `application-prod.yml:87` 的 `excludes: /api/**,/index` **不含** `/system/article`
- 上传扩展名**允许列表**式校验（`MimeTypeUtils.java:29-41`）✅
- 文件下载防目录穿越（`FileUtils.checkAllowDownload:146-162`）✅
- 动态 ORDER BY 白名单（`SqlUtil.java:21,31-42`，实测 9 组注入载荷全部被拒）✅
- MyBatis `${}` 无用户可控注入点（全仓仅 6 处，均为框架受控）✅
- 登出真正失效（`LogoutSuccessHandlerImpl.java:42-50`）✅
- JWT HS256 天然强制密钥长度 + prod 启动阻断 ✅
- 登录失败锁定接线正确（`SysPasswordService.java:50-71`）✅
- `.env` **未被 git 跟踪**、`.env.example` 全占位符 ✅
- **nginx XFF 全部 override** ✅ —— `nginx.conf` 9 处 + `nginx-https.conf` 9 处，**共 18 处全部为 `$remote_addr`**，`proxy_add_x_forwarded_for` 计数为 **0**
- Docker Compose：敏感值全走环境变量、healthcheck 齐备、后端仅绑 `127.0.0.1:8080`、Prometheus/Grafana 走 `profiles` ✅
- 依赖 CVE：`fastjson2` 钉 **2.0.65**，**不在** 2026-07 披露的 autoType 绕过（影响 ≤2.0.62）区间；`spring-boot 3.5.16`/`tomcat 10.1.60`/`mysql-connector-j 8.4.0` 均为受维护版本，**未发现影响生产依赖的未修复高危 CVE** ✅

### 6.6 数据库脚本幂等性 —— 实测零差异

- **全新库连跑 3 次**：50 张表、行数与内容 **CRC diff 为空** ✅
- **32 条种子 INSERT 全部核对**：**不存在"仍会重复插入"的语句**；AGENTS.md 点名的 `zhi_category`/`zhi_friend_link` 前科已用**显式主键**正确修复 ✅
- **20 条 `ALTER TABLE ADD COLUMN` 全部**位于 `information_schema` + `PREPARE/EXECUTE` 守卫块内，**无裸 ADD COLUMN** ✅
- **50 张表全部 `CREATE TABLE IF NOT EXISTS`**、31 处索引创建走幂等存储过程 ✅
- **脚本内无 `CREATE DATABASE`/`USE`**（唯一遗留是 `GRANT`，见低-14）✅
- **维护型 UPDATE 带值守卫**，实测三连跑 `update_time` 零变动 ✅
- **触发器/存储过程**均为 `DROP IF EXISTS` + `CREATE`，可重复执行 ✅

### 6.7 并发安全与缓存一致性 —— 多处设计正确

- **浏览量原子自增** ✅ —— `ArticleMapper.xml:192` `view_count = IFNULL(view_count, 0) + 1`（非先查后写）；`PageMapper.xml:142` 同理
- **点赞唯一键并发安全** ✅ —— 运行库 `information_schema.STATISTICS` 确认 `zhi_article_like.uk_article_user` 与 `zhi_comment_like.uk_comment_user`（`NON_UNIQUE=0`）均存在
- **评论数同步不吞异常** ✅ —— `CommentServiceImpl:434-443` 显式不 catch，让外层事务回滚；`changeCommentCount` 用 `GREATEST(...,0)` 防负
- **状态变更时评论数增减去重** ✅ —— `CommentServiceImpl:218-230` 比较 old/new status 才调整
- **慢操作移出事务** ✅ —— `CommentServiceImpl:412-429` 用 `TransactionSynchronization` 在 `afterCommit()` 注册
- **`ArticleViewSyncTask` 用 SCAN 而非 KEYS** ✅ —— `RedisCache.scanKeys`（`:306-322`）
- **无 AOP 自调用失效** ✅ —— 全仓检索无经典实例；`readOnly` 无误用；**无空 catch 块**
- **Quartz `@DisallowConcurrentExecution` 正确** ✅；定时任务幂等（条件更新 + `ON DUPLICATE KEY UPDATE`）

### 6.8 测试与代码质量门禁

- **CI 门禁齐备** ✅（见第四章）
- **`it.skip` / `describe.skip` / `expect(true)` 实测为 0 条** ✅ —— `grep -rn "it.skip\|describe.skip\|expect(true)" --include=*.test.ts zhi-ui/src | wc -l` → **0**
- **覆盖率实质性达标** ✅ —— 三个门禁模块 71.78%~76.43% / 67.26%~72.13%
- **前端 210 个测试文件 / 1856 个测试全部通过** ✅
- **异常处理器覆盖 11 类** ✅（唯一问题是消息透传，见中-6）

---

## 七、修复优先级路线图

| 优先级 | # | 条目 | 对应发现 | 改动面 | 风险 | 验证方式 |
| --- | --- | --- | --- | --- | --- | --- |
| **立即可修（当天）** | 1 | 恢复 `insertArticle` 的 `@Transactional`（移动 javadoc 块位置） | 严重-1 | **1 个文件、3 行位置调整** | **极低** —— 纯注解归位，无逻辑变更 | 补事务回归测试；`mvn -pl zhi-system test` |
| | 2 | `AuthController` 四个端点补 `@RateLimiter(IP)` | 高-4 | 1 个文件、4 个注解 | **低** —— 直接加注解即可（切面实现已正确） | 单测断言注解存在；实跑 429 行为 |
| | 3 | `GlobalExceptionHandler:105/116` 改为固定文案 | 中-6 | 1 个文件、2 行 | **极低** | 断言响应体不含 `e.getMessage()` |
| | 4 | `mo-blog.scss:994/1558`（死选择器内）顺带修正 token | 低-3 | 1 个文件、2 行 | **极低** —— 当前不可触发 | 构建产物 CSS 核对 |
| **本迭代** | 5 | **SQL 前向引用修复**：把 `slug` 补齐块上移到 :1190 之前 | 高-1 | `sql/00_init_database.sql` **1 次语句搬移** | **中** —— 需在全新库 + 存量库两条路径都实跑验证 | 模拟老库（删 `slug` 列）重跑，断言 exit=0 且后续语句执行 |
| | 6 | **清理死主题选择器**：补回 `add` 逻辑或删除 335+ 条 `html.dark.theme-mo-blog` 规则 | 中-5 | `theme.ts` + `theme-dark.css:34-60` + `mo-blog.scss`（428 处）+ 若干 `.vue` | **中** —— 需先决定产品方向；删除会影响 Mo-Blog 色板假设，测试需同步 | `npm run build:prod` 后核对产物 CSS；深色模式实测（含 V-8） |
| | 7 | **企业模板首屏底色修复**：`.tpl-site` 补 `background` 或按模式取 `--tpl-on-hero` | 高-3 | `site-templates.scss`（1~2 条规则）+ 可能 `EnterpriseNav.vue` | **低** —— 推荐方案是加一条 `html:not(.dark)` 覆盖 | headless 复测对比度 ≥4.5:1；补 `TPL_KEY_TOKENS` 守卫 |
| | 8 | 补齐 `insertArticle` 缓存失效 + 显式列出 `zhi:search:*` | 高-2、中-1 | `ArticleServiceImpl` 1~3 处注解 | **低** | 补"写方法失效 pattern 覆盖所有 `@SiteCacheable` 前缀"契约测试 |
| | 9 | `updateArticleStatus` 补 `@SiteCacheEvict` + 批量 SQL | 中-2、低-15 | `ArticleServiceImpl:571-597` + Mapper XML | **低** —— 批量 SQL 属改写，需回归 | 单测 + 批量发布后前台刷新验证 |
| | 10 | 口令策略统一（注册/重置/改密三处共用） | 中-3 | 新增 `PasswordPolicy` + 3 处调用 | **中** —— 会**拒绝**历史上的弱口令，需评估存量账号影响 | 单测覆盖边界；确认不阻断正常用户 |
| | 11 | `/reset-password` 验证码失败计数加来源维度 | 中-7 | `EmailServiceImpl:236` 键构造 | **低** | 并发/跨源测试断言不再互相锁死 |
| | 12 | 浏览量同步改"成功后再删"（或 DECRBY + 回补） | 中-10 | `ArticleViewSyncTask:40-45` | **低** —— 注意"写库成功但 delete 失败"会重复计数，宁可重复不可丢 | 注入 DB 异常，断言增量不丢 |
| | 13 | 留言冷却改 Redis `SET NX EX` | 中-12 | `FrontMessageController:184/220` | **中** —— **须保持"冷却先于验证码"语义**，验证码失败要回滚抢占 | 并发提交测试 + 验证码不被误烧 |
| | 14 | `application.yml` 去掉 `/system/article` 豁免 + 服务端正文消毒 | 低-2 | `application.yml:256` + 新增消毒调用 | **中** —— 消毒可能改变富文本渲染，需回归编辑器 | 断言 prod excludes 不含该项；富文本往返测试 |
| **后续** | 15 | 表排序规则统一 | 低-19 | 新增 `99_*.sql` 补丁 + `ALTER TABLE ... CONVERT` | **高** —— **破坏性操作**，必须单独补丁 + 写清回滚 | 见 4.4 节前提；先在副本库演练 |
| | 16 | `sys_config.config_key` 加唯一索引 + `applySettingUpdateByKey` 加事务 | 降级项（原 backend H-4） | SQL + 1 个方法 | **中** | 并发保存不产生重复镜像行 |
| | 17 | CORS 收窄为白名单 | 中-8 | `ResourcesConfig:83` + prod 覆盖 | **中** —— 配错会导致前端跨域失败 | 补测试断言 prod `allowedOrigins` 不含 `*` |
| | 18 | 权限模型清理（25 条冗余 + 死权限） | 低-11、低-12 | SQL 菜单数据 | **低** | 双向差集复核；保留豁免清单 |
| | 19 | 门禁测试补强（SQL 顺序守卫、token 守卫扩展至 `.scss`、`TPL_KEY_TOKENS` 补 token） | 高-1、低-3、高-3 | 3 个测试文件 | **低** —— 纯新增 | 断言新守卫能捕获已知缺陷 |
| | 20 | 文档修正（Spring Boot 版本、幂等判据、默认密码提示） | 低-20、低-21 | `AGENTS.md:3`、`pom.xml:26-29`、`sql/README.md` | **极低** | 人工核对 |
| | 21 | 其余低危项（`alt`、懒加载、索引、job 告警、错误信息带键名、Gitee tag 校验） | 低-9/10/13/14/16/17/18/21 | 分散小改动 | **低** | 各自单测 |

**执行建议**：**第 1、2、3 项应在当天完成**（合计改动不超过 10 行、风险极低、收益明确）。第 5 项（SQL 前向引用）虽是"高"，但改动是**一次语句搬移**，建议紧随其后 —— 它直接影响存量用户的升级成功率。第 6 项（死主题选择器）需要产品决策，建议先定方向再动手。

---

## 八、总评

| 维度 | 评价 | 依据 |
| --- | --- | --- |
| **架构与设计** | 良好 | 分层清晰、约定统一、AGENTS.md 记录的 32 条历史契约绝大多数**确实仍然成立** |
| **安全骨架** | 良好 | 未发现严重漏洞；权限串差集为空（源码 + 运行库双重确认）；注入面受控；匿名写防护齐备 |
| **代码正确性** | 良好但有 1 处回归 | 核心链路（浏览量/点赞/评论计数、设置缓存失效、schema 漂移、DataScope）实质正确；`insertArticle` 事务丢失是**单点回归**而非设计问题 |
| **前端质量** | 良好 | token 契约在 `.vue` 范围内 0 违规；XSS/JSON-LD/内存清理均干净；企业模板首屏是**选择器失配**问题 |
| **数据库工程化** | 良好 | 幂等性实测零差异（三连跑）；20/20 ADD COLUMN 有守卫；唯一问题是前向引用导致的升级路径中断 |
| **测试与门禁** | 优秀 | 1856 个前端测试 + 70 个 `zhi-admin` 测试全通过；覆盖率超门槛；checkstyle 0 violations；CI 无覆盖缺口；无测试占位符 |

**最终采纳分布：本次共 47 条原始发现，最终采纳 39 条（严重 1 / 高 4 / 中 13 / 低 21）+ 待验证 10 项。**

**一句话结论**：这是一份**质量高于同规模项目平均水平**的代码库 —— 问题集中在**局部回归、配置失配与可维护性**，没有架构性缺陷，没有安全破防点。**推荐按第七优先级的第 1~9 项在两周内完成修复**，其中第 1、2、3 项当天即可完成。

---

## 附录：分报告索引

| 文件 | 内容 | 原始条数 |
| --- | --- | --- |
| [`reports/audit-security.md`](audit-security.md) | 后端安全审查（认证、鉴权、注入面、匿名写防护、32 条既有契约复核） | 10 |
| [`reports/audit-backend-correctness.md`](audit-backend-correctness.md) | 后端正确性审查（事务、缓存一致性、并发、定时任务、20 条守卫复核） | 16 |
| [`reports/audit-frontend.md`](audit-frontend.md) | 前端审查（企业模板、token 契约、XSS、请求层、无障碍、内存） | 13 |
| [`reports/audit-db-devops.md`](audit-db-devops.md) | 数据库与工程化审查（幂等性实测、版本号、门禁、CI、Docker、nginx、CVE） | 11 |
| [`reports/audit-verification.md`](audit-verification.md) | **对抗式复核**（逐条证伪、真实 MySQL 破坏性验证、构建产物核验、独立补报 5 条） | 复核 47 + 补报 5 |

**审查方法说明**：本次共使用四类独立证据来源 —— ① 逐文件读码取证（所有结论带 `文件:行号`）；② 真实 MySQL 8.4.10 上的破坏性验证（探针表用后 DROP，残留计数 0）；③ headless Chrome + CDP 的对比度实测；④ **构建产物核验**（`dist/*.css`、`dist/static/js/*.js`、`zhi-admin.jar` MANIFEST）—— 第 ④ 类在本次推翻误报与确认死选择器时起到了决定性作用。

**未做的事（诚实声明）**：本次为**静态审查为主 + 定点实测**，未做端到端渗透测试、未做真实网络环境下的 CVE 扫描、未审计 `.env` 的真实密钥取值。凡静态无法定性的项已全部归入第五章待验证清单，**未以"已确认"含糊带过**。

---

*本报告由 report-writer 撰写，依据四份分报告与对抗式复核报告汇总去重，并按 verifier 第 4 章「对最终报告的修改建议」与 Lead 的两条终验补充严格执行。所有行号均经独立复核。*