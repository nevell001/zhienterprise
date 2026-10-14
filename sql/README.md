# SQL 脚本目录

## 文件说明

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

### 特性

✅ **完全幂等**：可重复执行，不会破坏已有数据
- `CREATE TABLE IF NOT EXISTS` - 表已存在时跳过
- `INSERT IGNORE INTO` - 数据已存在时跳过（依赖主键/唯一键）
- 索引通过 `sp_create_index_if_not_exists` 存储过程检查后创建
- 缺失的列通过 `information_schema.COLUMNS` 检查后 `ALTER TABLE … ADD COLUMN` 补齐

✅ **安全的反向兼容**：已初始化的数据库可直接重跑此脚本以补全缺失的表和索引

⚠️ **但"幂等"不等于"会修复已有行"**：`INSERT IGNORE` + 按标题判存在只会**跳过**已存在的数据。
若需改动既有数据行，应编写一次性数据补丁脚本（`99_fix_*.sql` 命名约定）按需执行。

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

## 使用方法

### 新部署（首次安装）

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE zhienterprise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 导入初始化脚本
mysql -u root -p zhienterprise < sql/00_init_database.sql
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

### 在 Docker 中执行

`docker-compose.*.yml` 默认挂载 `./sql:/docker-entrypoint-initdb.d:ro`，**注意**：
- MySQL 容器只在**首次初始化**（数据卷为空）时执行 `docker-entrypoint-initdb.d` 中的脚本
- 已有数据的容器需要手动执行上述命令
