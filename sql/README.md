# SQL 脚本目录

## 文件说明

### `00_init_database.sql` （幂等版）
数据库初始化/迁移的**唯一入口**（日常升级只需重跑它），包含：
- 所有表结构（系统表 + 站点内容表 + Quartz 表）
- 所有索引（含 FULLTEXT 全文索引）
- 站内信通知表 `blog_notification`
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
给存量库用（照 `blog_message.create_by` / `blog_friend_link.email` 的写法抄）。
这条规则由 `zhi-system` 的 `MapperColumnSchemaContractTest` 兜底：它比对 mapper 引用的列与脚本里的表结构。

### 案例：新增列遗漏会怎样（留言管理 500）
`blog_message` 初建时漏了 `create_by` / `update_by`，而 `BlogMessageMapper` 从生成那天起就 select 这两列，
于是后台「留言管理」列表、审核、回复三处都抛
`Unknown column 'create_by' in 'field list'`；前台提交留言不写这两列，所以留言板一直是好的 —— 缺陷只在后台第一次打开时暴露。

- **怎么修**：重跑 00 即可（新增的补齐块对缺列的库执行 `ALTER TABLE … ADD COLUMN`，已有列的库跳过），
  **只改库、不需要重建镜像**。
- 只想动这一张表的话，把脚本里 `-- 老库升级：补齐 blog_message` 到
  `DEALLOCATE PREPARE add_message_update_by_stmt;` 那一段单独执行也可以。
- 验证：`SHOW COLUMNS FROM blog_message LIKE '%_by';` 应看到 `reply_by` / `create_by` / `update_by` 三行。

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

这会自动补齐缺失的表/索引/触发器（如 `blog_notification`、FULLTEXT 索引），**不会**删除或覆盖任何已有数据。

### 在 Docker 中执行

`docker-compose.*.yml` 默认挂载 `./sql:/docker-entrypoint-initdb.d:ro`，**注意**：
- MySQL 容器只在**首次初始化**（数据卷为空）时执行 `docker-entrypoint-initdb.d` 中的脚本
- 已有数据的容器需要手动执行上述命令
