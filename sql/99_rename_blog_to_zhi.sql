-- =============================================================================
-- 99_rename_blog_to_zhi.sql —— 存量安装升级补丁：表名/角色键去 blog
-- =============================================================================
-- 修什么：
--   1) 17 张业务表 blog_* 重命名为 zhi_*（含索引/自增/注释随表迁移）
--   2) sys_role.role_key: 'blog_user' → 'site_user'（自助注册用户角色）
--
-- ★ 必须配合第二步：本补丁跑完后，**再重跑一次 00_init_database.sql**。
--   原因：MySQL 的 RENAME TABLE 不会改写数据库侧例程体——
--   tr_article_* 触发器、update_category_article_count 函数、
--   sp_fix_category_article_count* / sp_verify_category_article_count 存储过程
--   里都写死了 `FROM blog_category` 之类的语句，
--   表改名后它们会在运行时抛 `Table 'xxx.blog_category' doesn't exist`
--   （定时任务「同步文章浏览量」最先炸出来）。
--   00 里这些例程都是 `DROP ... IF EXISTS` + `CREATE`，重跑即按新表名重建，且幂等。
--
-- 完整升级（顺序不能反）：
--   mysql -u root -p <库名> < sql/99_rename_blog_to_zhi.sql
--   mysql -u root -p <库名> < sql/00_init_database.sql
--
-- 为什么表名必须单独跑：
--   sql/00_init_database.sql 用 CREATE TABLE IF NOT EXISTS / INSERT IGNORE 初始化，
--   老库里同名对象一律被「跳过」而非重建——重跑初始化脚本不会改掉已有的表名。
--
-- 安全性：
--   * 幂等：每张表都先查 information_schema，只有「旧名在、新名不在」才 RENAME；
--     重复执行第二次为 0 行变化。role_key 同样带 WHERE 守卫。
--   * 范围：只动上面列出的 17 张表和 1 行 sys_role；不碰任何数据行内容，
--     也不改 blog_setting/sys_config 里的设置项键（如 blog_name，属另一批）。
--   * 不带 CREATE DATABASE / USE：库名只在命令行给，避免误写生产库。
--
-- 执行前备份（够用即可，不必整库）：
--   mysqldump --single-transaction --routines <库名> blog_article blog_setting \
--     > rename_blog_to_zhi.before.sql
--
-- 用法：
--   mysql -u root -p <库名> < sql/99_rename_blog_to_zhi.sql
-- =============================================================================

SET @db := DATABASE();

-- ---------- 执行前自检：将要改名的表 ----------
SELECT TABLE_NAME AS old_name,
       CONCAT('zhi_', SUBSTRING(TABLE_NAME, 6)) AS new_name,
       TABLE_ROWS AS approx_rows
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = @db AND TABLE_NAME LIKE 'blog\_%'
 ORDER BY TABLE_NAME;

-- blog_article → zhi_article
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_article') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_article') = 0,
  'RENAME TABLE `blog_article` TO `zhi_article`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_article_daily_stats → zhi_article_daily_stats
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_article_daily_stats') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_article_daily_stats') = 0,
  'RENAME TABLE `blog_article_daily_stats` TO `zhi_article_daily_stats`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_article_like → zhi_article_like
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_article_like') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_article_like') = 0,
  'RENAME TABLE `blog_article_like` TO `zhi_article_like`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_article_tag → zhi_article_tag
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_article_tag') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_article_tag') = 0,
  'RENAME TABLE `blog_article_tag` TO `zhi_article_tag`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_bookmark → zhi_bookmark
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_bookmark') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_bookmark') = 0,
  'RENAME TABLE `blog_bookmark` TO `zhi_bookmark`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_category → zhi_category
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_category') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_category') = 0,
  'RENAME TABLE `blog_category` TO `zhi_category`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_comment → zhi_comment
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_comment') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_comment') = 0,
  'RENAME TABLE `blog_comment` TO `zhi_comment`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_comment_like → zhi_comment_like
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_comment_like') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_comment_like') = 0,
  'RENAME TABLE `blog_comment_like` TO `zhi_comment_like`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_email_code → zhi_email_code
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_email_code') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_email_code') = 0,
  'RENAME TABLE `blog_email_code` TO `zhi_email_code`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_friend_link → zhi_friend_link
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_friend_link') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_friend_link') = 0,
  'RENAME TABLE `blog_friend_link` TO `zhi_friend_link`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_message → zhi_message
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_message') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_message') = 0,
  'RENAME TABLE `blog_message` TO `zhi_message`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_notification → zhi_notification
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_notification') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_notification') = 0,
  'RENAME TABLE `blog_notification` TO `zhi_notification`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_page → zhi_page
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_page') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_page') = 0,
  'RENAME TABLE `blog_page` TO `zhi_page`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_setting → zhi_setting
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_setting') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_setting') = 0,
  'RENAME TABLE `blog_setting` TO `zhi_setting`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_tag → zhi_tag
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_tag') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_tag') = 0,
  'RENAME TABLE `blog_tag` TO `zhi_tag`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_upload → zhi_upload
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_upload') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_upload') = 0,
  'RENAME TABLE `blog_upload` TO `zhi_upload`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- blog_visit_log → zhi_visit_log
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'blog_visit_log') = 1
  AND (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'zhi_visit_log') = 0,
  'RENAME TABLE `blog_visit_log` TO `zhi_visit_log`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 丢弃数据库侧例程（第二步重跑 00 会按新表名重建） ----------
-- RENAME TABLE 不改写例程体：不先丢掉这些陈旧对象，第二步的种子脚本会在数据段
-- 被陈旧触发器/函数绊住（报 Table 'xxx.blog_category' doesn't exist）而中途失败。
-- 00 里每个例程都是 DROP ... IF EXISTS + CREATE，所以这里丢掉是安全的。
DROP TRIGGER IF EXISTS `tr_article_update_time`;
DROP TRIGGER IF EXISTS `tr_article_insert`;
DROP TRIGGER IF EXISTS `tr_article_delete_physical`;
DROP FUNCTION IF EXISTS `update_category_article_count`;
DROP PROCEDURE IF EXISTS `sp_fix_category_article_count_by_id`;
DROP PROCEDURE IF EXISTS `sp_fix_category_article_count`;
DROP PROCEDURE IF EXISTS `sp_verify_category_article_count`;

-- ---------- 角色键：blog_user → site_user（幂等） ----------
SELECT CONCAT('role blog_user before: ', COUNT(*)) AS note FROM sys_role WHERE role_key = 'blog_user';
UPDATE sys_role SET role_key = 'site_user'
 WHERE role_key = 'blog_user'
   AND NOT EXISTS (SELECT 1 FROM (SELECT role_id FROM sys_role WHERE role_key = 'site_user') AS t);

-- ---------- 执行后复核 ----------
SELECT COUNT(*) AS remaining_blog_tables FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = @db AND TABLE_NAME LIKE 'blog\_%';
SELECT TABLE_NAME AS renamed_table FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = @db AND TABLE_NAME LIKE 'zhi\_%' ORDER BY TABLE_NAME;
SELECT role_id, role_key FROM sys_role WHERE role_key IN ('blog_user', 'site_user');
SELECT CONCAT('例程已丢弃，当前触发器数=', COUNT(*),
              '；下一步必须重跑 sql/00_init_database.sql 按新表名重建') AS must_run_step2
  FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = @db;
