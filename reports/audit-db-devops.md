# 数据库与工程化审查报告

审查对象：ZhiEnterprise（知企建站系统）`sql/*.sql`、`pom.xml`、`zhi-ui/vitest.config.ts`、
`docker-compose*.yml`、`.github/workflows/*`、`.workflow/*`、nginx 配置。
工作目录 `/Users/nevell/code/zhienterprise`；审查时 HEAD = `c013feed`，工作区干净。

**审查方法（重要）**：本报告的幂等性结论**不是**读代码推断的，而是在本机
`MySQL 8.4.11`（临时 datadir `/tmp/zhi-audit-mysql`，独立 socket，不触碰任何现有库）
上**端到端实跑**得出的：全新库连续执行 3 次 + 行数/内容 CRC 快照比对 + 12 组破坏性
场景注入测试。凡标「实测」的结论都有可复现命令。

---

## 结论摘要

**总评：合格偏上，可以直接交付；但有一个必须先修的高危缺陷。**

幂等性的核心机制（显式主键种子、`COUNT(*)` 存储过程判存、`information_schema` 列守卫、
按值守卫的维护型 UPDATE）**全部到位且实测有效**：全新库连跑 3 次，50 张表、行数与内容
CRC **零差异**；README 里记载的三次历史事故（`zhi_category`/`zhi_friend_link` 重复、
`site_nav_item` 双写、库名写死、`update_time` 被顶动）**均已真实修复且可验证**，
没有被修一半的情况。

但发现 **1 条高危**：脚本里存在**前向引用**——第 1190 行的 `zhi_article_tag` 种子引用了
`zhi_article.slug`，而 `slug` 列的补齐守卫在第 2568 行。任何**早于 slug 特性**的存量库重跑
00（即文档唯一推荐的升级路径）会在 1190 行**硬失败并退出码 1，且此后所有升级动作全部不执行**。
这不是理论风险，本报告已稳定复现。

版本号单点一致性、JaCoCo/Vitest 门禁阈值、nginx XFF override、CI 覆盖、`.env` 泄漏
这些「AGENTS.md 声明 vs 实际」的逐项核对**全部通过**，未发现不一致（唯一例外是文档里
Spring Boot 版本号写错，属文档缺陷）。

| 级别 | 条数 | 说明 |
|---|---|---|
| 严重 | 0 | — |
| 高 | 1 | SQL 前向引用，存量库升级路径硬失败 |
| 中 | 4 | 历史重复行清理可能误删用户数据 / 种子 id 占位冲突 / 文档 Spring Boot 版本错误 / 表排序规则不统一 |
| 低 | 6 | 默认管理员弱口令、`GRANT` 写死库名、季度表无索引、README 措辞与实测偏差等 |
| **合计** | **11** | |

---

## 幂等性专项

### 判据说明

`INSERT IGNORE` 只在**主键或唯一键冲突**时才忽略。因此一条种子 INSERT 是否会在重跑时
重复插入，取决于该表是否有可用的唯一键（或列清单里是否显式给了主键 id）：

- 有唯一键 → `INSERT IGNORE` 生效，天然幂等（**不管有没有显式 id**）；
- 无唯一键但**显式指定主键 id**（或位置化 `VALUES` 首列即主键）→ 靠 PK 冲突去重，幂等；
- 无唯一键、又**没给主键** → `INSERT IGNORE` **不忽略任何行**，每次重跑都追加 → 必须靠
  `COUNT(*)` 判存的存储过程或 `WHERE NOT EXISTS` 兜住。

AGENTS.md 原文说「种子数据必须显式指定主键 `id`（或用带 `COUNT(*)` 判断的存储过程）」
——这句话**不完整**：漏了「有唯一键也可以」这一类。实测证明 `zhi_setting`
（唯一键 `uk_config_key`）与 `zhi_page`（`uk_page_slug`）都没给显式 id，但**完全幂等**。

### 逐表核对结果（32 条种子 INSERT 全部核对）

| 表 | 判据 | 实测幂等 | 依据 |
|---|---|---|---|
| `sys_dept` / `sys_post` / `sys_user` / `sys_role` / `sys_config` / `sys_dict_type` / `sys_dict_data` | 位置化 `VALUES`，首列即主键 | ✅ | 位置化插入主键，PK 冲突去重 |
| `sys_user_role` / `sys_role_menu` | 复合主键 `(user_id,role_id)` / `(role_id,menu_id)` | ✅ | 复合 PK 冲突去重（`sys_role_menu` 无唯一键但 PK 即 `(role_id,menu_id)`） |
| `sys_menu`（36 条） | 列清单显式 `menu_id` | ✅ | PK 冲突去重 |
| `zhi_category`（1030） | 列清单显式 `id` | ✅ | PK 冲突去重 |
| `zhi_tag`（1049） | 列清单显式 `id` | ✅ | PK 冲突去重 |
| `zhi_friend_link`（1278） | 列清单显式 `id` | ✅ | PK 冲突去重 |
| `zhi_setting`（6 条） | 唯一键 `uk_config_key` | ✅ | 唯一键冲突去重 |
| `zhi_page`（2686 / 2768） | 唯一键 `uk_page_slug` | ✅ | 唯一键冲突去重 |
| `zhi_article_tag`（1190） | `uk_article_tag(article_id,tag_id)` | ✅ | 唯一键冲突去重 |
| `zhi_article`（1083，存储过程内） | `sp_insert_sample_articles` 带 `COUNT(*)` 判存 **＋** 唯一索引 `uk_article_title_alive` 双保险 | ✅ | 见下方专项测试 H |
| `site_nav_item`（1421 / 1444） | `WHERE NOT EXISTS` 按「位置+父项+链接类型+链接值」 | ✅ | 无唯一键，靠 NOT EXISTS 收敛 |

**结论：`sql/00_init_database.sql` 中不存在「仍会重复插入」的 INSERT 语句。**
AGENTS.md 点名的 `zhi_category` / `zhi_friend_link` 前科已修复，且修复方式是**显式主键**
（正确做法），不是靠 `WHERE NOT EXISTS` 打补丁。

### 前置结论的实测证据

```
实测（MySQL 8.4.11，临时 datadir，全新库 zhi_final）：
  第 1 次执行 exit=0，50 张表，art=9 cat=11 nav=15 menu=178
  第 2 次执行 exit=0，50 张表，art=9 cat=11 nav=15 menu=178
  第 3 次执行 exit=0，50 张表，art=9 cat=11 nav=15 menu=178
  行数 + zhi_article/zhi_category/zhi_setting/sys_menu/site_nav_item 内容 CRC → diff 为空
```

### 幂等性专项破坏性测试（12 组）

| # | 场景 | 结果 | 判定 |
|---|---|---|---|
| A | 用户删掉种子分类 `id=11` 后重跑 | 分类回到 11 条（补回） | ✅ 符合 README「补缺失」语义 |
| B | 用户自建分类占用自增 id 12 后重跑 | `expo` 补回 id 11，用户项保留 | ✅ 正确 |
| C | 用户自建分类**占用种子保留 id 11** 后重跑 | `expo` **永远不出现**，静默缺 1 条 | ⚠️ **中等**（见 M-2） |
| D | 用户手工加一条与种子同 `name+url` 的友链后重跑 | 用户那条被 `DELETE f1` **删除** | ⚠️ **中等**（见 M-1） |
| E | 用户自建同名同 alias 分类（id 更大）后重跑 | 用户那条被删除，保留更早的行 | ⚠️ **中等**（M-1 同源） |
| F | 站长改导航标签（核心业务→咨询业务）后重跑 | **不重复**，`/business` 仅 1 条 | ✅ 7e3d2ade 修复有效 |
| G | 删掉被 `COUNT(*)` 判存的那篇文章后重跑 | 补回 9 篇，无重复标题 | ✅ 唯一索引兜底有效 |
| H | 仅删判存文章、保留其余 8 篇后重跑 | 无重复 | ✅ |
| I | 模拟老库缺 `slug` 列后重跑（升级路径） | **exit=1，脚本中断** | 🔴 **高危**（见 H-1） |
| J | 模拟老库缺 11 个后加列后重跑 | 因 I 同一原因中断 | 🔴 H-1 |
| K | 表排序规则交叉 JOIN | `ERROR 1267` | ⚠️ 潜在（见 M-4） |
| L | 位置化主键表连跑 3 次 | 零变化 | ✅ |

---

## 版本号一致性核对表

| 位置 | 期望 | 实际 | 结论 |
|---|---|---|---|
| root `pom.xml:9` `<version>` | 1.0.0 | `1.0.0` | ✅ |
| root `pom.xml:24` `<app.version>` | 1.0.0 | `1.0.0` | ✅ |
| `zhi-common/pom.xml` parent `<version>` | 1.0.0 | `1.0.0` | ✅ |
| `zhi-system/pom.xml` parent `<version>` | 1.0.0 | `1.0.0` | ✅ |
| `zhi-framework/pom.xml` parent `<version>` | 1.0.0 | `1.0.0` | ✅ |
| `zhi-quartz/pom.xml` parent `<version>` | 1.0.0 | `1.0.0` | ✅ |
| `zhi-generator/pom.xml` parent `<version>` | 1.0.0 | `1.0.0` | ✅ |
| `zhi-admin/pom.xml` parent `<version>` | 1.0.0 | `1.0.0` | ✅ |
| `RuoYiConfig.java:19` `${ruoyi.version:1.0.0}` 兜底 | 1.0.0 | `1.0.0` | ✅ |
| `zhi-ui/package.json:3` `"version"` | 1.0.0 | `1.0.0` | ✅ |
| `zhi-ui/package-lock.json:3` 根 `"version"` | 1.0.0 | `1.0.0` | ✅ |
| `zhi-ui/package-lock.json:9` `packages[""].version` | 1.0.0 | `1.0.0` | ✅ |
| `application.yml:6` `ruoyi.version` | 跟随 Maven | `@app.version@`（filtering 开启） | ✅ |
| tag `vX.Y.Z` vs pom | 无 tag | 仓库无 tag，`release.yml`/`.workflow/ReleasePipeline.yml` 均有校验 | ✅ 机制在位 |

**结论：13 处版本号全部一致，均为 1.0.0，未发现不一致。**
`zhi-admin/pom.xml:95-102` 对 `**/*.yml` 开了 `filtering=true`，因此 `@app.version@`
确实会被替换为 `1.0.0`（该资源过滤配置正确，不是空谈）。

---

## 严重

未发现。

---

## 高

### H-1 SQL 前向引用：`zhi_article.slug` 在补齐守卫之前被引用，存量库重跑硬失败

**位置**：`sql/00_init_database.sql:1226`（语句起于 :1190），引用点早于补齐守卫
`sql/00_init_database.sql:2568`。

**片段**：

```sql
-- L1190  ukk: 插入文章标签关联数据
INSERT IGNORE INTO `zhi_article_tag` (`article_id`, `tag_id`)
SELECT `a`.`id`, `t`.`id`
FROM ( ... ) `seed`
JOIN `zhi_article` `a` ON `a`.`slug` = `seed`.`slug` AND `a`.`del_flag` = 0   -- ← L1226，前向引用
JOIN `zhi_tag` `t` ON `t`.`name` = `seed`.`tag_name`;

-- ... 1300 多行之后 ...
-- L2568  老库升级：补齐 zhi_article.slug 列（幂等）
SET @has_article_slug := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'slug');
```

**影响**：`sql/README.md:162` 与 `CLAUDE.md` 都把「已有部署升级 = 直接重跑 00」写成唯一
推荐路径。任何建库时间早于 slug 特性（`36d556de fix(db): 标签关联种子改按 slug+标签名收敛`）
的存量库，重跑时会在此处抛：

```
ERROR 1054 (42S22) at line 1190: Unknown column 'a.slug' in 'on clause'
```

`mysql` 客户端**默认遇到错误即中断**，实测：

- 退出码 **1**（非 0）——CI/自动化部署会当失败处理，但若用 `|| true` 之类包一层就会静默吞掉；
- **第 1190 行之后的全部内容都不执行**：包括 20 个列补齐块中的大部分、`zhi_page`/`site_nav_item`
  种子、`zhi_notification` 建表兜底、触发器、全部性能索引、以及结尾的完整性校验。
  实测中断后 `zhi_article.slug` 仍不存在、脚本摘要（「🎉 数据库初始化完成」）未输出。

**复现（本机实测）**：

```bash
mysql -u root zhi_old < sql/00_init_database.sql          # 建好基线库
mysql -u root zhi_old -e "ALTER TABLE zhi_article DROP INDEX idx_slug; \
                          ALTER TABLE zhi_article DROP COLUMN slug;"
mysql -u root zhi_old < sql/00_init_database.sql          # 升级路径
# => ERROR 1054 (42S22) at line 1190: Unknown column 'a.slug' in 'on clause'
# => 退出码 1
```

**为什么现有门禁没拦住**：`zhi-ui/src/sql-init-script.test.ts` 只断言了「不得有
`CREATE DATABASE`/`USE`」与「维护型 UPDATE 必须带值守卫」两条，**完全没有涉及语句执行顺序**；
`MapperColumnSchemaContractTest` 比对的是「mapper 引用的列 vs 脚本里的列」，也不检查脚本
自身的先后依赖。实测 `grep -n "slug\|forward\|order" zhi-ui/src/sql-init-script.test.ts` 无匹配。

**修复建议**（三选一，推荐第 1 种）：

1. **把 `slug` 的补齐块上移到 :1190 之前**（与 `article_type` 等其它 `zhi_article` 列补齐块一起
   提到首次引用之前）。这是最小改动，且符合「先补列、后用列」的自然顺序。
2. 把 :1190 的收敛 JOIN 从 `slug` 改为不依赖新列的稳定键（例如按标题），但会削弱
   `36d556de` 当初「id 漂移时别把标签挂错文章」的修复意图，不推荐。
3. 给 :1190 整段加 `@has_article_slug` 存在性守卫（列不存在则整段跳过），让它在新库/老库都安全。
   注意：这样老库重跑将**不会**补回标签关联，需要用户感知。

**顺带建议**：给 `sql-init-script.test.ts` 补一条顺序守卫——解析所有
`information_schema.COLUMNS ... COLUMN_NAME='x'` 守卫的行号，断言「任何引用
`表.列` 的语句行号 > 该列守卫行号」。这条测试能一次性防住整类缺陷。

---

## 中

### M-1 历史重复行清理可能删除用户自建数据（`zhi_friend_link` / `zhi_category`）

**位置**：`sql/00_init_database.sql:1274-1276`（友链）、`:1023-1027`（分类）。

**片段**：

```sql
-- L1274 幂等保护 1：清理历史重跑产生的重复友链（同 url+name 保留最小 id）
DELETE f1 FROM `zhi_friend_link` f1
JOIN `zhi_friend_link` f2
  ON f1.`url` = f2.`url` AND f1.`name` = f2.`name` AND f1.`id` > f2.`id`;
```

**影响**：这两条清理的判据是「业务字段相同」，**不区分「脚本重复插入的」与「用户手工建的」**。
只要用户通过后台添加了一条与种子同 `name+url` 的友链，重跑 00 就会把它静默删掉。
分类同理（`:1023` 还额外要求「未被文章引用」才删，风险略低，但仍会删用户行）。

**实测证据**：

- 场景 D：手工插入一条 `Spring 官网 / https://spring.io/`（id=7）后重跑 → 重跑后该行**消失**，
  友链回到 6 条。
- 场景 E：插入一条同名同 alias `expo` 的分类（id=14）后重跑 → 被删除，只保留 id=13 的行。

**修复建议**：给清理加「仅当被删行仍是未修改的种子形态」的守卫，例如友链补
`AND f1.`logo` = f2.`logo` AND f1.`description` = f2.`description``，或更稳妥地
**限制为 `f2.id <= 6`（种子保留 id 段）**，即只清理「与种子行重复的历史副本」。
改完请把 README 的「重跑不删数据」表述与实际行为对齐。

### M-2 用户数据占用种子保留 id 时，该种子静默缺失

**位置**：`sql/00_init_database.sql:1030`（分类）、`:1049`（标签）、`:1278`（友链）。

**影响**：种子用显式主键去重，代价是「保留 id 一旦被别的东西占用，`INSERT IGNORE` 会
把**种子**当成重复行丢弃」。实测场景 C：用户自建分类恰好拿到 id=11，重跑 00 后
`expo`（展会活动）**永久不会出现**，且**没有任何报错或提示**，站长只会觉得「少了个分类」。

**说明**：这个风险很窄（需要用户自建行的自增值正好撞上种子保留段 `1..11`），但后果是静默的，
排查成本高。属于「显式主键方案」的固有代价，值得记录。

**修复建议**：在种子的 `INSERT IGNORE` 之后补一段校验型 SELECT，把「种子期望存在但实际
不存在的行」打出来（脚本结尾已有大量这类校验 SELECT，沿用同一风格即可），让缺失可见。

### M-3 文档 Spring Boot 版本号错误（AGENTS.md 写 3.3.0，实际 3.5.16）

**位置**：`AGENTS.md:3`、`CLAUDE.md:7`、`pom.xml:30`。

**片段**：

```
AGENTS.md:3   ... Spring Boot 3.3.0 backend (Java 17, `jakarta.*` namespace ...
CLAUDE.md:7   ... built on Spring Boot 3.5 + Vue 3 ...
pom.xml:30    <spring-boot.version>3.5.16</spring-boot.version>
```

**实际值**：`pom.xml:30` 为 `3.5.16`，`pom.xml:89-92` 以 `spring-boot-dependencies`
BOM `import` 方式接管全部 Spring 依赖版本，**实际生效的就是 3.5.16**。
另注 `pom.xml:26-29` 的注释仍在讲「3.3.13 是 3.3.x 最后一个 OSS 版本」，
与 `3.5.16` 的实际取值**自相矛盾**，属遗留注释。

**结论**：**CLAUDE.md 正确（3.5），AGENTS.md 错误（3.3.0）**，即文档与实现不一致这一事实成立。

**修复建议**：把 `AGENTS.md:3` 的 `3.3.0` 改为 `3.5.x`（或直接写 `3.5.16`），
并更新 `pom.xml:26-29` 的过时注释。建议在 `docs/VERSION_MANAGEMENT.md` 同一处
补一句「Spring Boot 版本以 `pom.xml` 的 `<spring-boot.version>` 为准」。

### M-4 表排序规则不统一（`utf8mb4_0900_ai_ci` vs `utf8mb4_unicode_ci`）

**位置**：`sql/00_init_database.sql` 各 `CREATE TABLE ... COLLATE` 子句。

**实测分布（50 张表）**：`utf8mb4_0900_ai_ci` **13 张**（全部 `zhi_*` 业务表：
`zhi_article`/`zhi_category`/`zhi_comment`/`zhi_page`/`zhi_setting`/`site_nav_item` …），
`utf8mb4_unicode_ci` **35 张**（`sys_*` 系统表、`QRTZ_*`、`gen_*`、以及较晚新增的
`zhi_notification`/`zhi_bookmark`/`zhi_article_like`/`zhi_comment_like`/`zhi_email_code`），
另 2 张为系统视图。

注意：任务书中「排序规则统一性（utf8mb4_unicode_ci）」的**前提不成立**——实际是两套并存。
建库语句用的是 `utf8mb4_unicode_ci`（README/CLAUDE.md 的 `CREATE DATABASE` 示例），
但多数 `zhi_*` 表在 DDL 里显式指定了 `0900_ai_ci`，**表级设置覆盖库级默认**。

**影响**：跨组字符串 JOIN/比较会抛错，实测：

```sql
SELECT COUNT(*) FROM zhi_setting z JOIN sys_config s ON z.config_key = s.config_key;
-- ERROR 1267 (HY000): Illegal mix of collations (utf8mb4_0900_ai_ci,IMPLICIT)
--                     and (utf8mb4_unicode_ci,IMPLICIT) for operation '='
```

**当前未实际触发**（已逐一核对）：现有 mapper 的跨组 JOIN 都走数值列
（如 `CommentMapper.xml:27` `left join sys_user u on c.user_id = u.user_id`），
`SysConfigMapper.xml` 也只查单表，`updateByKey` 的 `sys_config` 镜像读写不涉及跨表字符串比较。
因此定为**潜在**风险而非现行缺陷：一旦有人写出 `zhi_setting.config_key = sys_config.config_key`
这类 JOIN，就会立刻报 1267。

**修复建议**：长期统一到一套（推荐 `utf8mb4_unicode_ci`，与建库语句和 README 一致；
或反过来把 `sys_*` 也改 `0900`，但 RuoYi 上游是 `unicode_ci`，改动面更大）。
统一需要 `ALTER TABLE ... CONVERT TO CHARACTER SET`，属破坏性操作，建议单独开一个
`99_*.sql` 补丁并写清回滚方式，**不要**塞进 00。

---

## 低

### L-1 默认管理员账号使用 RuoYi 广为人知的弱口令哈希

**位置**：`sql/00_init_database.sql:142`（`admin`）、`:143`（`zhiqi`）。

**片段**：

```sql
INSERT IGNORE INTO sys_user VALUES(1, 103, 'admin', '知企', '00', 'zhiqi@163.com', ...,
  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', ...);
```

该哈希是**若依（RuoYi）框架默认口令 `admin123`** 的固定 bcrypt 值（RuoYi 官方 SQL 原样
保留、网络上大量公开资料给出其明文），且 `sys_user` 在 `sql/00_init_database.sql:117` 建表后
**未**对该行追加更强的初始化策略。

**影响**：任何按 README 默认流程部署并直接暴露公网、未及时改密的实例，等于公开后台口令。
`README.md:24` 与脚本结尾都有「首次登录请修改默认密码」的提示，属**有警示但默认不安全**。

**缓解现状（正面）**：`sys_config:184` 已把 `sys.user.initPassword` 从 RuoYi 原值
`123456` 改为 `CHANGE_ME`，`sys.account.initPasswordModify='1'` 会强制弹改密提醒——
**说明项目已经意识到这个问题，但漏掉了 `admin` 自身这一行**。

**修复建议**：把 :142/:143 的哈希换成随机口令的 bcrypt（脚本注释里写明「部署前必须修改」），
或在脚本结尾加一段守卫：检测 admin 口令哈希仍等于该已知值时，打印醒目警告（不阻断，
因为脚本需保持可重跑）。

### L-2 `GRANT` 写死库名 `zhienterprise`

**位置**：`sql/00_init_database.sql:55-56`。

```sql
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES,
      CREATE ROUTINE, ALTER ROUTINE, EXECUTE, TRIGGER
ON zhienterprise.* TO 'zhienterprise_app'@'%';
```

**影响**：这是 `d13060ed`「脚本不再写死库名」修复的**唯一遗留点**，脚本内已有注释说明。
非 Docker 且把 `.env` 的 `DB_NAME` 改成别的名字时，授权会落在不存在的 `zhienterprise` 库上
（MySQL 对不存在的库授权不报错），应用账号对新库**无权限**，表现为运行期
`Access denied for user ... to database`。
Docker 部署不受影响（容器按 `MYSQL_DATABASE` 自动建号授权）。

**修复建议**：脚本无法知道库名（这是不可写死库名的必然代价），因此**保留现状 + 加强提示**
是合理选择；建议在 README「新部署」一节显式列出该手工步骤，而不是只在脚本注释里提。

### L-3 部分表缺少主键以外索引

**位置**：`sys_menu` / `sys_user` / `sys_role` / `sys_dept` / `sys_dict_data` / `sys_post` /
`sys_notice` / `sys_job` / `sys_job_log` / `sys_role_menu` / `sys_user_role` / `sys_user_post` /
`sys_role_dept` / `gen_table` / `gen_table_column` 及全部 `QRTZ_*`。

**影响**：`sql/00_init_database.sql:1901-1929` 的「性能优化索引」章节仅为 `zhi_*` 业务表建索引，
系统表依赖 RuoYi 原生 DDL（多数只带 PK）。在数据量增长后，`sys_menu` 按 `parent_id` 取菜单树、
`sys_oper_log`/`sys_logininfor` 按时间清理等会成为全表扫。
属**可优化项**，当前规模（默认种子几十行）无实际影响。

**修复建议**：低频维护场景可不动；若要补，用脚本已有的
`sp_create_index_if_not_exists` 存储过程追加，天然幂等。

### L-4 `sql/README.md` 的「INSERT IGNORE 依赖主键/唯一键」表述与实际判据略有出入

**位置**：`sql/README.md:62`、`AGENTS.md`「Testing」段落。

README 与 AGENTS.md 都强调「种子必须显式指定主键 id」，但实测表明**有唯一键同样幂等**
（`zhi_setting` 的 `uk_config_key`、`zhi_page` 的 `uk_page_slug`、`zhi_article_tag` 的
`uk_article_tag` 都没给显式 id，行为完全正确）。表述过窄会让后来者以为必须补 id。

**修复建议**：把判据改写为「必须存在可被 `INSERT IGNORE` 命中的唯一键（主键或唯一索引），
否则必须显式给主键 id，或用带 `COUNT(*)` 判存的存储过程 / `WHERE NOT EXISTS`」。

### L-5 `README.md` 与脚本结尾的「默认密码」提示未给具体初始口令

**位置**：`README.md:24`、`sql/00_init_database.sql` 结尾 `step4`。

仅写「首次登录请修改默认密码」，未说明初始口令是什么，用户需自行翻查哈希或搜索若依文档。
与 L-1 联动，建议一并处理（要么给强随机初始口令并写明，要么保留默认但显著标注风险）。

### L-6 `.workflow/ReleasePipeline.yml` 的 tag 校验比 `release.yml` 弱

**位置**：`.workflow/ReleasePipeline.yml:30` vs `.github/workflows/release.yml:20-25`。

Gitee 侧在「未检出 tag」时**仅告警不阻断**（注释说明是 Gitee Go 容器限制），
GitHub 侧则**硬失败**。两套流水线的门禁强度不一致；若发布主要走 Gitee，
AGENTS.md 宣称的「tag ≠ pom 版本则构建失败」在 Gitee 侧并不成立。

**修复建议**：文档里区分说明两套流水线的差异，或在 Gitee 侧增加手工确认步骤。

---

## 门禁与 CI 覆盖核对表

### JaCoCo / Checkstyle（`pom.xml`）

| 声明（AGENTS.md） | 实际 | 结论 |
|---|---|---|
| line ≥ 60% | `pom.xml:470-472` `COVEREDRATIO` `LINE` `minimum=0.60` | ✅ |
| branch ≥ 60% | `pom.xml:474-478` `COVEREDRATIO` `BRANCH` `minimum=0.60` | ✅ |
| BUNDLE 规则 | `pom.xml:467` `<element>BUNDLE</element>` | ✅ |
| `zhi-admin` `jacoco.skip=true` | `zhi-admin/pom.xml:153-159` `<skip>true</skip>` | ✅ |
| `zhi-generator` `jacoco.skip=true` | `zhi-generator/pom.xml:75-81` `<skip>true</skip>` | ✅ |
| 仍跑测试但无覆盖率门槛 | `skip` 只作用于 jacoco 插件，surefire 不受影响 | ✅ |
| `checkstyle:check` 在 validate 阶段 | `pom.xml:392-399` execution `validate` → `check` | ✅ |
| 报告位于 `target/jacoco/index.html` | `pom.xml:419` `outputDirectory=target/jacoco` | ✅ |

**JaCoCo 排除项**：`pom.xml:420-465` 有较大 `excludes` 列表（`com/zhi/common/**` 基础设施、
`com/zhi/framework/config/**` 装配类等），注释说明了理由（「无法在不启动容器/JVM 的前提下断言」），
且显式保留了 `SecurityConfigValidator`、`RefererPolicy` 等含判定逻辑的类在门槛内。
**属有意设计，非门禁注水**，但需注意：覆盖面随排除项增长而实际下降，建议定期复核。

### Vitest 覆盖率（`zhi-ui/vitest.config.ts`）

| 声明 | 实际 | 结论 |
|---|---|---|
| lines ≥ 70 | `thresholds.lines: 70`（`vitest.config.ts:74`） | ✅ |
| statements ≥ 70 | `thresholds.statements: 70`（`:77`） | ✅ |
| functions ≥ 75 | `thresholds.functions: 75`（`:75`） | ✅ |
| branches ≥ 55 | `thresholds.branches: 55`（`:76`） | ✅ |
| 仅统计 `stores/utils/api/components` 的 `.ts` | `coverage.include` 四项完全对应 | ✅ |

**补充核对**：配置文件第 71 行注释写明「Vitest 4 使用 `thresholds` 生效（旧式顶层字段会被忽略）」，
写法正确。`provider: 'v8'`、`reportOnFailure: true` 已设。
**结论：前端门禁阈值与 AGENTS.md 声明逐项一致。**

### CI 工作流覆盖（`.github/workflows/ci.yml`）

| AGENTS.md 声明的检查 | ci.yml 是否覆盖 | 结论 |
|---|---|---|
| 后端编译 + 单测 + checkstyle + JaCoCo | `mvn -B clean verify`（verify 含全部） | ✅ |
| `npm run audit` 且 registry pin 到 npmjs | `npm run audit`，脚本为 `npm audit --omit=dev --audit-level=high --registry=https://registry.npmjs.org` | ✅ **registry 已 pin** |
| `npm run lint:check` | 有 | ✅ |
| `npm run format:check` | 有 | ✅ |
| `npm run typecheck` | 有 | ✅ |
| `npm run test:coverage` | 有 | ✅ |
| `npm run build:prod` | 有 | ✅ |
| 产物上传 | `backend-jar` / `frontend-dist`，均 `if-no-files-found: error` | ✅ |
| Java 17 / Node 22 | `java-version: '17'` / `node-version: '22'` | ✅ |
| release.yml 同样覆盖 | `release.yml` 有 tag↔pom 校验 + 同样全套检查 | ✅ |
| dependabot（maven/npm/github-actions，weekly，patch+minor 分组） | `.github/dependabot.yml` 三项齐全 | ✅ |

**未发现覆盖缺口。**

一处**运维提示**（非缺陷）：两个工作流都硬钉 `runs-on: ubuntu-24.04`，注释说明
「ubuntu-latest 自 2026-10-19 迁移 Ubuntu 26，适配后再放开」。这是主动规避风险的合理做法，
但需有人在适配后记得放开，否则会一直停在旧镜像。

### 前端「源码文本断言」门禁现状

AGENTS.md 声称「跨产物契约检查」是被明确允许的例外。已核对相关文件确实存在且通过：

```
npx vitest run src/sql-init-script.test.ts        → 2 passed
npx vitest run src/views/admin/setting/settings-seeds.test.ts \
               src/views/admin/article/sample-articles-seeds.test.ts  → 8 passed
```

`sql-init-script.test.ts` 的两条断言（不得有 `CREATE DATABASE`/`USE`；维护型 UPDATE 必须带值守卫）
**确实有效**：前者对应 `d13060ed` 的真实事故，后者对应「重跑顶动 update_time」的真实事故。
但其**未覆盖语句顺序**，这正是 H-1 能漏过的原因（见 H-1 修复建议）。

---

## 其他核对结论（未发现问题的项）

以下各项逐项核对后**未发现问题**，按任务要求明确记录：

- **`ALTER TABLE ADD COLUMN` 的 `information_schema` 守卫**：脚本内共 **20 条**
  `ALTER TABLE … ADD COLUMN`，经脚本逐条配对校验，**20/20 全部**位于
  `SET @has_x := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE
  TABLE_SCHEMA=DATABASE() AND TABLE_NAME='…' AND COLUMN_NAME='…')` + `PREPARE/EXECUTE`
  块内，**没有裸 `ADD COLUMN`**；22 个 `PREPARE` 与 20 个列补齐 + 2 个其它用途对应。
  并已实测「删列 → 重跑 → 列恢复」升级路径有效（除 H-1 阻断点之外的分支）。
  这正是 MySQL 8 不支持 `ADD COLUMN IF NOT EXISTS` 所要求的正确写法。
- **`CREATE TABLE IF NOT EXISTS`**：50 张表全部使用，无裸 `CREATE TABLE`。
- **索引创建幂等**：31 处 `CALL sp_create_index_if_not_exists(...)`，存储过程以
  `information_schema.STATISTICS` 判存，三连跑零报错。
- **库名写死**：脚本内**无** `CREATE DATABASE` / `USE`（守卫测试断言 + 人工确认），
  仅 `GRANT` 一处遗留（见 L-2）。
- **维护型 UPDATE 顶动 `update_time`**：`sql/00_init_database.sql:1238-1247`（分类计数）与
  `:685-695`（文章评论数）均带 `<>` / `IS NULL` 值守卫；`sp_fix_category_article_count_by_id`
  的单分类与全部分类两个分支（`:2148-2184`）也都有守卫。实测三连跑无 `update_time` 变动
  （CRC 比对零差异），**`d13060ed` 修复有效**。
  注：`sp_verify_category_article_count`（`:2227`）是纯 SELECT 校验过程，不写数据，`CALL` 它安全。
- **`@DISABLE_TRIGGERS` 会话变量**：`:1232/:1250`、`:2144/:2190` 成对设置与复位，
  无泄漏。**待验证**：该方案依赖「同一连接内执行」，若用某些分连接执行器跑脚本理论上会失效；
  本次实测用单连接 `mysql <` 重定向，行为正确。
- **触发器**：3 个（`tr_article_insert` / `tr_article_update_time` / `tr_article_delete_physical`）
  均为 `DROP TRIGGER IF EXISTS` + `CREATE`，可重复执行。
- **存储过程**：4 个，全部 `DROP PROCEDURE IF EXISTS` + `CREATE`，可重复执行，
  临时过程 `sp_insert_sample_articles` 用后即 `DROP`（`:1185`），不留垃圾。
- **`nginx` 的 X-Forwarded-For**：`zhi-ui/nginx.conf`（9 处）与 `zhi-ui/nginx-https.conf`
  （9 处）中**全部 18 处** `proxy_set_header X-Forwarded-For` 均为 `$remote_addr`（**override**），
  `grep -c proxy_add_x_forwarded_for` 两个文件均为 **0**（**无一处** append）；多处还带注释
  「覆盖而非追加，避免客户端伪造值被后端采信」。`X-Real-IP` 同样为 `$remote_addr`。
  **完全符合 AGENTS.md 要求**，未发现伪造风险。
- **`.env` 泄漏**：`.gitignore:78` 忽略 `.env`，`git check-ignore -v .env` 确认命中；
  仓库内**只**跟踪 `.env.example`（`git ls-files` 无 `.env`），且示例文件全为
  `your_strong_password_here` / 空值占位，**无真实密钥**。`.dockerignore` 排除
  `.env` / `.env.*` / `*.pem` / `*.key`。`uploadPath/`、`target/` 均已忽略且未被跟踪。
- **Docker Compose**：`prod` 全部敏感值走 `${…}` 环境变量（无硬编码密钥）；
  关键服务均有 `healthcheck`；`zhi-admin` / `mysql` / `redis` 有 `deploy.resources.limits`；
  后端仅绑 `127.0.0.1:8080`（避免绕过反代，与限流/审计设计一致）；
  `depends_on` 用 `condition: service_healthy`；Prometheus/Grafana 走
  `profiles: ["monitoring"]` 且绑 `127.0.0.1`，符合 CLAUDE.md 说明。
- **依赖版本与 CVE（仅列影响生产依赖的高危项）**：
  - `spring-boot.version = 3.5.16`、`tomcat.version = 10.1.60`、`mysql-connector-j 8.4.0`
    —— 均为当前受维护的较新版本，**未发现影响生产依赖的未修复高危 CVE**。
  - **`fastjson2` 已核对并排除**：2026-07 披露的 Fastjson 2.x autoType 绕过 RCE
    （CVSS 9.8）影响范围为 **≤ 2.0.62**，项目在 `pom.xml:44` 钉的是 **2.0.65**，
    **不在受影响区间**。且 `zhi-common/pom.xml:87` 声明的是 `fastjson2`（非 1.x）。
    参考：[绿盟通告](https://blog.nsfocus.net/fastjson-2-x%E8%BF%9C%E7%A8%8B%E4%BB%A3%E7%A0%81%E6%89%A7%E8%A1%8C%E6%BC%8F%E6%B4%9E%E9%80%9A%E5%91%8A/)、
    [fastjson2 issue #7702](https://github.com/alibaba/fastjson2/issues/7702)。
  - **`repositories` 无认证仓库**：`pom.xml:549-559` 仅一个公开的
    `maven.aliyun.com/repository/public`，无 `<username>`/`<password>`。符合 AGENTS.md
    「保留无认证仓库，否则 Dependabot 的 maven 生态会静默停止上报」的要求。
  - **Maven CVE 覆盖策略**：CI 无 OWASP dependency-check（AGENTS.md 说明是刻意为之，
    理由为需要 NVD key 且首次同步 30–60 分钟），由 Dependabot 承担——**与声明一致**；
    但需知晓：**Dependabot 只在 GitHub 远端生效，Gitee/origin 侧无 CVE 提醒**。

---

## 备注

- 本报告仅写入 `reports/audit-db-devops.md`，**未修改任何源码或 SQL**。
- 所有数据库实验均在临时 datadir（`/tmp/zhi-audit-mysql`，socket `/tmp/zhi-audit.sock`）
  的独立实例上进行，未触碰机器上任何既有 MySQL 数据；实验库为 `zhi_audit` / `zhi_old` / `zhi_final`。
- 标「待验证」的仅 1 处：`@DISABLE_TRIGGERS` 在非单连接执行器下的行为（见「其他核对结论」）。
- 未覆盖项（超出本次范围）：运行时行为测试、前端页面实际渲染、真实网络环境下的 CVE 扫描。