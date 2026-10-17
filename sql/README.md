# SQL 脚本目录

## 文件说明

### `99_dedupe_nav_defaults.sql`（存量升级补丁，一次性）
**只有被旧版导航种子写坏过的库需要跑**（全新安装不需要）：清理「站长改过导航标签后重跑
`00_init_database.sql`」造成的重复菜单 —— 同一路径下有两条菜单，一条是用户改名的、一条是种子
按默认标签新插的，00 里那段按标签判重的历史清理抓不到标签不同的这一类。

- 幂等：删除条件要求「同键（位置+父项+链接类型+链接值）存在 id 更小的另一行」，清完即不成立，
  **第二次执行 0 行变化**。
- 范围：只删 `parent_id=0` 且标签与链接**逐字段等于种子默认值**、`create_by='admin'` 的行；
  用户改名的行、自建行、`cta` 与 `path` 同指 `/contact` 的两条合法菜单、二级项都不在范围内。
- 脚本自带执行前自检（列出会被删的行）与执行后复核（重复分组应为空），**跑之前先看第一段输出**。
- 执行前备份：`mysqldump --single-transaction <库名> site_nav_item > nav.before.sql`。
- 用法：`mysql -u root -p <库名> < sql/99_dedupe_nav_defaults.sql`（脚本内没有 `USE`，库名只在命令行出现）。

### `99_rename_blog_to_zhi.sql`（存量升级补丁，一次性）
**只有存量库需要跑**（全新安装不需要）：把 17 张 `blog_*` 业务表重命名为 `zhi_*`，
并把 `sys_role.role_key` 的 `blog_user` 改为 `site_user`。

为什么不能靠重跑 `00_init_database.sql`：该脚本用 `CREATE TABLE IF NOT EXISTS` / `INSERT IGNORE`，
老库里同名对象一律被**跳过**，表名不会自己变。任何涉及「改名/改既有行」的升级都必须写成
这样的显式补丁（`99_*.sql` 命名约定），并按需执行一次。

- 幂等：每张表都先查 `information_schema`，只有「旧名在、新名不在」才 `RENAME TABLE`；
  `sys_role` 的更新带 `WHERE` 守卫。**重复执行第二次为 0 行变化**。
- 范围：只改 17 张表名 + 1 行角色键；不动任何数据行，也不动 `blog_name` 等设置项键。
- **必须两步、顺序不能反**：
  ```bash
  mysql -u root -p <库名> < sql/99_rename_blog_to_zhi.sql   # 1. 改表名/角色键
  mysql -u root -p <库名> < sql/00_init_database.sql        # 2. 刷新数据库侧例程
  ```
  第二步不可省：`RENAME TABLE` **不会改写触发器/函数/存储过程体**，
  `tr_article_*`、`update_category_article_count`、`sp_fix_category_article_count*`、
  `sp_verify_category_article_count` 里都写死了 `blog_category` 之类的表名，
  不重建就会在运行时抛 `Table 'xxx.blog_category' doesn't exist`。
  00 里这些例程都是 `DROP … IF EXISTS` + `CREATE`，重跑即按新表名重建，且幂等。
- 执行前建议备份：`mysqldump --single-transaction <库名> zhi_article zhi_setting > rename.before.sql`。
- 演练注意：用 `CREATE TABLE … LIKE` 复制的 scratch 副本**不含触发器/存储过程**，
  因此表改名演练必须额外把例程也复制过去（或按上面的两步流程在副本上端到端跑一遍），
  否则会漏掉「例程体里的旧表名」这类只在运行时暴露的缺陷。
- 权限串（`blog:*` → `zhi:*`）已由 `00_init_database.sql` 里的幂等 UPDATE 覆盖，无需单独跑；
  但**已登录用户的权限缓存在 Redis**，改完要重新登录一次才生效。

### `00_init_database.sql` （幂等版）
数据库初始化/迁移的**唯一入口**（日常升级只需重跑它），包含：
- 所有表结构（系统表 + 站点内容表 + Quartz 表）
- 所有索引（含 FULLTEXT 全文索引）
- 站内信通知表 `zhi_notification`
- 触发器和存储过程
- 示例数据（管理员账号、菜单、字典等）

⚠️ **脚本不建库、不切换库**：目标库只由调用方给出（`mysql … <库名> < sql/00_init_database.sql`，
或 Docker 的 `MYSQL_DATABASE`，其默认值取自 `.env` 的 `DB_NAME`）。**不要**在里面加 `CREATE DATABASE` / `USE`
—— 会把调用方给的库名覆盖掉（演练写进主库、`DB_NAME` 失效，见下文案例）。

### 特性

✅ **完全幂等**：可重复执行，不会破坏已有数据
- `CREATE TABLE IF NOT EXISTS` - 表已存在时跳过
- `INSERT IGNORE INTO` - 数据已存在时跳过（**依赖主键或唯一键**：两者任一存在即可幂等，
  并非"必须显式给主键 id"。例如 `zhi_setting`/`zhi_page`/`zhi_article_tag` 靠唯一键去重，
  没有显式 id 也完全幂等；真正会重复插入的是**既无显式主键、又无唯一键**的表，见下方案例）
- 索引通过 `sp_create_index_if_not_exists` 存储过程检查后创建
- 缺失的列通过 `information_schema.COLUMNS` 检查后 `ALTER TABLE … ADD COLUMN` 补齐

✅ **安全的反向兼容**：已初始化的数据库可直接重跑此脚本以补全缺失的表和索引

⚠️ **但"幂等"不等于"会修复已有行"**：`INSERT IGNORE` + 按标题判存在只会**跳过**已存在的数据。
若需改动既有数据行，应编写一次性数据补丁脚本（`99_fix_*.sql` 命名约定）按需执行。

⚠️ **脚本里删掉某个种子键 ≠ 已有库会清掉它**：`zhi_setting` 的旧行仍留在表中（脚本只做
`INSERT IGNORE`，从不对账删除）。这在**功能层面无害** —— 后台设置列表会把这些行一并返回，
但前端 `SettingController` 的 `managedKeys` 白名单已不含该键，不会渲染成字段、也不会被保存；
前台公开设置同样只取白名单内的键。因此项目惯例是**只删种子、不写 DELETE**
（`wechat_qr`、`blog_author`、`blog_signature` 均如此），避免在生产库上执行非必要删除。
若确需彻底清理，按 `99_fix_*.sql` 约定写一次性补丁并自行评估影响：

```sql
DELETE FROM zhi_setting WHERE config_key IN ('blog_author', 'blog_signature');
DELETE FROM sys_config  WHERE config_key IN ('blog_author', 'blog_signature');
```

⚠️ **`CREATE TABLE IF NOT EXISTS` 也不等于"会补列"**：表已存在时整段建表语句被跳过，后来新增的列
不会凭空出现。因此**新增列必须同时写两处** —— ①`CREATE TABLE` 里给新装用；②文件后半段的
`SET @has_x := (SELECT COUNT(*) FROM information_schema.COLUMNS …)` + `PREPARE/EXECUTE` 补齐块里
给存量库用（照 `zhi_message.create_by` / `zhi_friend_link.email` 的写法抄）。
这条规则由 `zhi-system` 的 `MapperColumnSchemaContractTest` 兜底：它比对 mapper 引用的列与脚本里的表结构。

### 案例：新增列遗漏会怎样（留言管理 500）
`zhi_message` 初建时漏了 `create_by` / `update_by`，而 `MessageMapper` 从生成那天起就 select 这两列，
于是后台「留言管理」列表、审核、回复三处都抛
`Unknown column 'create_by' in 'field list'`；前台提交留言不写这两列，所以留言板一直是好的 —— 缺陷只在后台第一次打开时暴露。

- **怎么修**：重跑 00 即可（新增的补齐块对缺列的库执行 `ALTER TABLE … ADD COLUMN`，已有列的库跳过），
  **只改库、不需要重建镜像**。
- 只想动这一张表的话，把脚本里 `-- 老库升级：补齐 zhi_message` 到
  `DEALLOCATE PREPARE add_message_update_by_stmt;` 那一段单独执行也可以。
- 验证：`SHOW COLUMNS FROM zhi_message LIKE '%_by';` 应看到 `reply_by` / `create_by` / `update_by` 三行。

### 案例：重复种子会让菜单翻倍（菜单管理）
初版的 `site_nav_item` 默认导航种子在「初始化区」和文件末尾的「升级区」各写了一份，
而该表**没有自然唯一键**（`INSERT IGNORE` 只对主键/唯一键去重），于是**单次执行就插入两份**：
全新安装 / 重建数据卷后，后台「菜单管理」里每条菜单都出现两次。

- **怎么修**：已重跑过旧版脚本的库，**重跑 00 即可**（脚本会先清理同位置+父项+标签+链接+显隐
  完全相同的重复项，保留最小 id，并清理孤儿二级项；种子改为显式主键 id，重跑整体幂等）。
  验证：`SELECT location, parent_id, label, COUNT(*) FROM site_nav_item GROUP BY 1,2,3 HAVING COUNT(*) > 1;`
  应为空。
- **长期规则**：给无自然唯一键的表写默认种子，要么显式指定主键 id，要么加「仅当表为空才插入」守卫，
  且**种子只能有一处**（能重跑的脚本里重复一份 = 单次执行插入两份）。

### 案例：改过导航标签后重跑，同一路径又冒出一条（菜单管理）
上一段的清理只认「**标签也相同**」的重复行，挡不住这一类：站长在后台把 `/business` 的标签
从「核心业务」改成「咨询业务」之后重跑 00 —— 种子的判存条件写的是标签，库里搜不到「核心业务」，
于是再插一条默认标签的行，头部/页脚各多出一个指向同一路径的菜单，**且标签不同、清理逻辑看不见它**。

- **怎么防**：00 里的种子判存键已改为「位置 + 父项 + 链接类型 + 链接值」（二级项同理：父项按
  `link_value='/about'` 定位、子项按 `link_value` 判存），标签只作为「缺失时写什么」的默认值，
  不再作为存在性判据。链接类型必须进键，否则 `path /contact`（联系我们）与 `cta /contact`（咨询合作）
  两条合法菜单会互相顶掉。
- **存量库怎么清**：跑一次性补丁 `sql/99_dedupe_nav_defaults.sql`（见上文文件说明）。
  只删「与种子默认项逐字段相同 + `create_by='admin'` + 同键存在更早且标签不同的一行」，
  用户改名后的行、自建行、二级项一律不动。
- **重跑 00 修不了它**：新判存键只负责「不再新增重复行」，不会删除既有重复行 —— 又是「幂等 ≠ 会修复已有行」。
- 已知行为：种子是「补缺失」而非「保持删除」，站主**主动删掉**的默认项在重跑 00 时会被补回来。
  要永久去掉某条默认菜单，请在「菜单管理」里改标签/链接或改 `visible`，并接受重跑升级脚本可能补回。
- 验证（三种场景已在 scratch 副本上演练通过，2026-10-16）：全新库 15 行且重跑 0 变化；
  改过标签的存量库重跑新种子只补 2 条「解决方案」、`/business` 不再翻倍；
  二级项改名后重跑新种子不再插入重复子项（旧种子会 +1 行）。

### 案例：脚本里写死库名，演练写进了生产库（库名只应由调用方给出）
`00_init_database.sql` 里曾有 `CREATE DATABASE IF NOT EXISTS zhienterprise; USE zhienterprise;`，两层后果：

- **演练不可信**：按修复演练流程把脚本重定向给副本库（`mysql <副本库> < 00_init_database.sql`），
  `USE` 会把连接切回 `zhienterprise`，**写入实际落到主库** —— 真实发生过：本地库多出 2 条种子导航项、
  9 篇示例文章的 `update_time` 被刷新，而副本库里一张表都没建。
- **自定义库名失效**：`docker-compose*.yml` 的库名来自 `MYSQL_DATABASE: ${DB_NAME:-zhienterprise}`，
  但脚本内的 `USE` 覆盖了容器给的目标库，把 `DB_NAME` 改成别的名字时，表和授权仍会落到 `zhienterprise`。
- **怎么修**：删掉这两行，库名只由调用方给出（命令行位置参数，或 Docker 的 `MYSQL_DATABASE`）。
  未指定库时 mysql 直接报 `ERROR 1046 No database selected` —— 响亮失败，不再静默写错库。
- **约束**：给副本库演练任何脚本前，先确认脚本里没有 `USE`（有就先剥掉再用）；
  `zhi-ui/src/sql-init-script.test.ts` 也断言了 00 里不存在 `CREATE DATABASE`/`USE` 语句。
- 唯一还写死库名的地方是 `GRANT … ON zhienterprise.*`：不带 Docker 且改过 `DB_NAME` 需手工改（脚本内有注释）。

### 案例：重跑升级把文章/分类刷到「最近更新」（update_time 被顶动）
维护型语句「文章评论数校正」和「分类文章数校正」（含 `sp_fix_category_article_count_by_id` 的
「修复所有分类」分支）曾无条件命中全表，而这些表带 `ON UPDATE CURRENT_TIMESTAMP`，
`zhi_article` 上还有 BEFORE UPDATE 触发器 `tr_article_update_time` 无条件 `SET NEW.update_time = NOW()`
—— 于是**每次重跑升级脚本，全部文章与全部分类的更新时间都被刷成当前时刻**，示例文章顶到「最近更新」首位。

- **关键认知**：在有这种触发器的表上 `SET update_time = update_time` 是**无效的**，触发器在 SET 之后覆盖 `NEW`。
- **怎么修**：给维护型语句加「值确实不一致」守卫（`<>` / `IS NULL`），重跑命中 0 行即不写行、不触发触发器；
  只有值真的对不上时才写，并把该行顶到当前时间（数据确实变了，合理）。
- 验证（scratch 副本，2026-10-17）：全新执行 50 张表；再次执行后 21 张带 `update_time` 的表的
  行数 + 时间戳签名**零变化**；把 1 篇文章的 `comment_count`、1 个分类的 `article_count` 改成 99 再重跑，
  两处计数被纠正回真值，其余 8 篇文章 / 10 个分类完全未动。
- **约束**：这两类语句由 `zhi-ui/src/sql-init-script.test.ts` 断言必须带值守卫（或按主键单行更新）。


## 使用方法

### 新部署（首次安装）

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE zhienterprise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 导入初始化脚本
mysql -u root -p zhienterprise < sql/00_init_database.sql
```

#### ⚠️ 排序规则（collation）必须显式指定 `utf8mb4_unicode_ci`

脚本内各表的 `COLLATE` **不统一**：实测 24 处显式声明中，**14 处**为 `utf8mb4_0900_ai_ci`、
**9 处**为 `utf8mb4_unicode_ci`，其余表继承**建库时**的默认排序规则。因此最终是否混用，
取决于你用哪种方式建库：

| 建库方式 | 库默认 collation | 结果 |
| --- | --- | --- |
| 上面这条 `CREATE DATABASE … utf8mb4_unicode_ci` | `utf8mb4_unicode_ci` | 与脚本内 9 处一致；其余表随库默认 |
| Docker（`MYSQL_DATABASE`，MySQL 8 默认） | `utf8mb4_0900_ai_ci` | 与脚本内 14 处一致；其余表随库默认 |

**风险在哪**：跨 collation 比较**字符串列**时会报
`ERROR 1267 (HY000): Illegal mix of collations`。已在真实库复核：本项目关键跨表 JOIN
（如 `zhi_comment.user_id ⋈ sys_user.user_id`）的键**都是 `bigint`**，数值比较不受
collation 影响，因此**当前不会触发**。但若将来新增按**字符串列**做 JOIN / 比较的查询，
混用就会暴露 —— 建议：
1. 统一用上面这条命令建库（`utf8mb4_unicode_ci`），或全用 Docker 路径，二选一，不要交叉；
2. 新增表时显式写 `COLLATE`，与既有同类表保持一致；
3. 新增按字符串列跨表关联的查询前，先确认两边 collation 相同。

排查现有库的分布：

```sql
SELECT TABLE_COLLATION, COUNT(*) FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() GROUP BY TABLE_COLLATION;
```

### 已有部署（升级）

直接重跑 00 脚本即可（表/列/索引/菜单/权限自动补齐）：

```bash
# 在 Docker 环境
docker exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" zhienterprise < /docker-entrypoint-initdb.d/00_init_database.sql'

# 或本地环境
mysql -u root -p zhienterprise < sql/00_init_database.sql
```

这会自动补齐缺失的表/索引/触发器（如 `zhi_notification`、FULLTEXT 索引），**不会**删除或覆盖任何已有数据。

⚠️ 「不会覆盖」的另一面是**也不会清理**：如果这台站在后台改过导航标签，历史上又重跑过旧版种子，
导航里已经躺着重复菜单，重跑 00 之后它们还在 —— 需要另外跑一次
`sql/99_dedupe_nav_defaults.sql`（见上文，先跑它的自检再决定）。

### 在 Docker 中执行

`docker-compose.*.yml` 默认挂载 `./sql:/docker-entrypoint-initdb.d:ro`，**注意**：
- MySQL 容器只在**首次初始化**（数据卷为空）时执行 `docker-entrypoint-initdb.d` 中的脚本
- 首次初始化时容器把 `MYSQL_DATABASE`（默认取 `.env` 的 `DB_NAME`）作为目标库传给每个脚本，
  所以脚本内**不需要也不该**再 `USE` 一次库名 —— 改 `DB_NAME` 部署自定义库名才能生效
- 已有数据的容器需要手动执行上述命令
