-- ===============================================================
-- 📌 99_dedupe_nav_defaults.sql —— 导航默认项重复行清理（存量库一次性补丁）
-- ===============================================================
-- 修什么：
--   后台「菜单管理」表 site_nav_item 没有自然唯一键。00_init_database.sql 早先版本的默认
--   导航种子按「位置+父项+标签」判存在，而站长在后台改过标签的库（真实例子：把 /business
--   的标签从「核心业务」改成「咨询业务」）里再也找不到「核心业务」这一行，于是重跑初始化
--   脚本会**再插一条**默认标签的行 —— 头部/页脚各多出一个指向同一路径的重复菜单。
--   这两行标签不同，00 里那段按「同位置+父项+标签+链接」清理重复项的历史逻辑（①②③）抓不到。
--
-- 为什么得单独跑：
--   种子判存键已改为「位置+父项+链接类型+链接值」，全新安装不需要本脚本；对已有库重跑 00
--   也只能「不再新增重复行」，不会删除已经存在的重复行（幂等 ≠ 会修复已有行）。
--
-- 安全性：
--   1) 只删「与种子默认项逐字段相同（位置+标签+链接类型+链接值）且 create_by='admin'」的行，
--      即只清理种子自己写进去的那一行；用户改名后的行、自建行都不满足条件，一律保留。
--   2) 删除还要求同键（位置+父项+链接类型+链接值）存在 id 更小的另一行 —— 保留最早那行。
--   3) cta 类型的 /contact（咨询合作）与 path 类型的 /contact（联系我们）是两条合法菜单，
--      判重键含 link_type，不会被互相误删；且逐字段比对种子元组，标签与链接不配对的行不动。
--   4) 只处理顶级项（parent_id=0）；二级项重复与孤儿项由 00 的 ①③ 段负责，不在本脚本范围。
--   5) 幂等：删完后「同键存在更早一行」不再成立，第二次执行 0 行变化。
--   6) 本脚本只有 DELETE，不写任何列，因此不存在 ON UPDATE CURRENT_TIMESTAMP 顶动 update_time
--      的问题，也不会把示例数据冲到「最近更新」列表。
--
-- 执行前备份：
--   mysqldump --single-transaction <库名> site_nav_item > nav.before.sql
--
-- 用法（库名只出现在命令行一处，脚本内不含 USE / CREATE DATABASE）：
--   mysql -u root -p <库名> < sql/99_dedupe_nav_defaults.sql
--   Docker：docker exec -i -e MYSQL_PWD="$DB_PASSWORD" mysql mysql -uroot <库名> < sql/99_dedupe_nav_defaults.sql
-- ===============================================================

-- ① 执行前自检：列出本补丁会删除的行（空结果 = 本库不需要本补丁）
SELECT dup.`id`, dup.`location`, dup.`label`, dup.`link_type`, dup.`link_value`,
       MIN(keep.`id`) AS kept_id
  FROM `site_nav_item` dup
  JOIN (
        SELECT 'header' AS `location`, '首页' AS `label`, 'path' AS `link_type`, '/' AS `link_value`
  UNION ALL SELECT 'header', '核心业务', 'path', '/business'
  UNION ALL SELECT 'header', '解决方案', 'path', '/solutions'
  UNION ALL SELECT 'header', '新闻动态', 'path', '/news'
  UNION ALL SELECT 'header', '关于我们', 'path', '/about'
  UNION ALL SELECT 'header', '联系我们', 'path', '/contact'
  UNION ALL SELECT 'header', '咨询合作', 'cta',    '/contact'
  UNION ALL SELECT 'footer', '首页',     'path', '/'
  UNION ALL SELECT 'footer', '核心业务', 'path', '/business'
  UNION ALL SELECT 'footer', '解决方案', 'path', '/solutions'
  UNION ALL SELECT 'footer', '新闻动态', 'path', '/news'
  UNION ALL SELECT 'footer', '关于我们', 'path', '/about'
  ) s
    ON s.`location` = dup.`location` AND s.`label` = dup.`label`
   AND s.`link_type` = dup.`link_type` AND s.`link_value` = dup.`link_value`
  JOIN `site_nav_item` keep
    ON keep.`location` = dup.`location` AND keep.`parent_id` = dup.`parent_id`
   AND keep.`link_type` = dup.`link_type` AND keep.`link_value` = dup.`link_value`
   AND keep.`id` < dup.`id`
   AND keep.`label` <> dup.`label`
 WHERE dup.`parent_id` = 0 AND dup.`create_by` = 'admin'
 GROUP BY dup.`id`, dup.`location`, dup.`label`, dup.`link_type`, dup.`link_value`;

-- ② 清理：条件与 ① 完全一致
DELETE dup FROM `site_nav_item` dup
  JOIN (
        SELECT 'header' AS `location`, '首页' AS `label`, 'path' AS `link_type`, '/' AS `link_value`
  UNION ALL SELECT 'header', '核心业务', 'path', '/business'
  UNION ALL SELECT 'header', '解决方案', 'path', '/solutions'
  UNION ALL SELECT 'header', '新闻动态', 'path', '/news'
  UNION ALL SELECT 'header', '关于我们', 'path', '/about'
  UNION ALL SELECT 'header', '联系我们', 'path', '/contact'
  UNION ALL SELECT 'header', '咨询合作', 'cta',    '/contact'
  UNION ALL SELECT 'footer', '首页',     'path', '/'
  UNION ALL SELECT 'footer', '核心业务', 'path', '/business'
  UNION ALL SELECT 'footer', '解决方案', 'path', '/solutions'
  UNION ALL SELECT 'footer', '新闻动态', 'path', '/news'
  UNION ALL SELECT 'footer', '关于我们', 'path', '/about'
  ) s
    ON s.`location` = dup.`location` AND s.`label` = dup.`label`
   AND s.`link_type` = dup.`link_type` AND s.`link_value` = dup.`link_value`
  JOIN `site_nav_item` keep
    ON keep.`location` = dup.`location` AND keep.`parent_id` = dup.`parent_id`
   AND keep.`link_type` = dup.`link_type` AND keep.`link_value` = dup.`link_value`
   AND keep.`id` < dup.`id`
   AND keep.`label` <> dup.`label`
 WHERE dup.`parent_id` = 0 AND dup.`create_by` = 'admin';

-- ③ 执行后复核：仍「同一位置同父同链接有多行」的分组（应只剩站长的自建项）
SELECT n.`location`, n.`parent_id`, n.`link_type`, n.`link_value`,
       GROUP_CONCAT(CONCAT(n.`id`, ':', n.`label`, ':', n.`create_by`) ORDER BY n.`id`) AS rows_in_group,
       COUNT(*) AS remaining
  FROM `site_nav_item` n
 WHERE n.`parent_id` = 0
 GROUP BY n.`location`, n.`parent_id`, n.`link_type`, n.`link_value`
HAVING COUNT(*) > 1;

-- ④ 执行后复核：导航全貌（应与后台「菜单管理」看到的一致）
SELECT `location`, `parent_id`, `label`, `link_type`, `link_value`, `sort`, `visible`, `create_by`
  FROM `site_nav_item`
 ORDER BY `location`, `parent_id`, `sort`, `id`;
