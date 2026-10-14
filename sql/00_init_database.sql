-- ===============================================================
-- 🌟 知企建站系统完整数据库初始化脚本（幂等版）
-- ===============================================================
-- 📅 创建时间：2025-11-07
-- 🔧 最后更新：2026-08-26
-- 📝 描述：整合所有SQL文件，创建完整的知企建站系统数据库
-- 🚀 版本：v1.0.0 (幂等版 - 可重复执行)
--
-- 📋 包含内容：
-- ✅ 若依系统基础表结构和数据
-- ✅ Quartz定时任务表结构
-- ✅ 站点系统表结构和数据（含 zhi_notification 站内信通知表）
-- ✅ 注册用户认证系统（注册用户角色、邮箱验证码表）
-- ✅ 代码生成器表结构
-- ✅ 性能优化索引（含全文索引 ft_article_title_content）
-- ✅ 数据完整性约束和触发器（含零日期修复）
-- ✅ 完整的示例数据
-- ✅ 站点管理、系统管理、系统监控、数据统计、系统工具菜单和权限配置
--
-- 🎯 使用方法：
-- 1. 创建数据库：CREATE DATABASE zhienterprise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 2. 导入本脚本：mysql -u root -p zhienterprise < 00_init_database.sql
-- 3. 完成！不再需要单独执行 index 或 fix 脚本
-- 2. 选择数据库：USE zhienterprise;
-- 3. 执行脚本：source /path/to/init_database.sql;
-- 4. 验证结果：查看最后的统计信息
--
-- ⚠️  注意事项：
-- - 请确保MySQL版本 >= 8.0
-- - 执行前请备份现有数据（虽然脚本幂等，备份永远是好习惯）
-- - ✅ 幂等设计：所有 CREATE TABLE 用 IF NOT EXISTS，INSERT 用 IGNORE，索引通过存储过程检查
-- - ✅ 可重复执行：在已有数据的数据库上再次运行也不会破坏数据
-- - ✅ 不再需要 01_fix_fulltext_index.sql 和 02_fix_notifications_and_search.sql（已合并）
-- - 包含触发器和索引，首次执行时间约2-3分钟
-- ===============================================================

-- 1. 创建数据库和设置应用账号
CREATE DATABASE IF NOT EXISTS zhienterprise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zhienterprise;

-- 创建专用应用账号，避免应用服务依赖root远程访问
-- ⚠️ 安全警告：部署前必须修改此密码为强密码
-- ⚠️ Docker Compose 环境无需修改这里：mysql 容器启动时会根据 .env 中的
--    MYSQL_USER / MYSQL_PASSWORD（即 DB_USERNAME / DB_PASSWORD）自动创建同名用户，
--    本语句因 IF NOT EXISTS 会被跳过，不会覆盖 .env 配置的密码。
--    仅当不使用 Docker、手动执行本脚本初始化数据库时，才需要把下面的
--    'CHANGE_ME_STRONG_PASSWORD_32_CHARS' 改成与 .env 的 DB_PASSWORD 一致；
--    若库中已存在 zhienterprise_app（旧密码），还需 ALTER USER 同步密码。
CREATE USER IF NOT EXISTS 'zhienterprise_app'@'%' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD_32_CHARS';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES, CREATE ROUTINE, ALTER ROUTINE, EXECUTE, TRIGGER
ON zhienterprise.* TO 'zhienterprise_app'@'%';
FLUSH PRIVILEGES;

-- ========== 幂等索引创建辅助存储过程（早期定义，全文使用） ==========
DELIMITER $$
DROP PROCEDURE IF EXISTS sp_create_index_if_not_exists$$
CREATE PROCEDURE sp_create_index_if_not_exists(
    IN table_name_param VARCHAR(64),
    IN index_name_param VARCHAR(64),
    IN create_sql_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND INDEX_NAME = index_name_param
    ) THEN
        SET @create_index_sql = create_sql_param;
        PREPARE create_index_stmt FROM @create_index_sql;
        EXECUTE create_index_stmt;
        DEALLOCATE PREPARE create_index_stmt;
    END IF;
END$$
DELIMITER ;

-- ========== 导入若依系统基础表结构 ==========

-- 1、部门表
CREATE TABLE IF NOT EXISTS sys_dept (
  dept_id           BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '部门id',
  parent_id         BIGINT(20)      DEFAULT 0                  COMMENT '父部门id',
  ancestors         VARCHAR(50)     DEFAULT ''                 COMMENT '祖级列表',
  dept_name         VARCHAR(30)     DEFAULT ''                 COMMENT '部门名称',
  order_num         INT(4)          DEFAULT 0                  COMMENT '显示顺序',
  leader            VARCHAR(20)     DEFAULT NULL               COMMENT '负责人',
  phone             VARCHAR(11)     DEFAULT NULL               COMMENT '联系电话',
  email             VARCHAR(50)     DEFAULT NULL               COMMENT '邮箱',
  status            CHAR(1)         DEFAULT '0'                COMMENT '部门状态（0正常 1停用）',
  del_flag          CHAR(1)         DEFAULT '0'                COMMENT '删除标志（0代表存在 2代表删除）',
  create_by         VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time       DATETIME                                   COMMENT '创建时间',
  update_by         VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time       DATETIME                                   COMMENT '更新时间',
  PRIMARY KEY (dept_id)
) ENGINE=INNODB AUTO_INCREMENT=200 COMMENT = '部门表';

-- 初始化-部门表数据
INSERT IGNORE INTO sys_dept VALUES(100,  0,   '0',          '知企技术',   0, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(101,  100, '0,100',      '西安总公司', 1, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(103,  101, '0,100,101',  '研发部门',   1, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(102,  100, '0,100',      '上海分公司', 2, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(104,  101, '0,100,101',  '市场部门',   2, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(105,  101, '0,100,101',  '测试部门',   3, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(106,  101, '0,100,101',  '财务部门',   4, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(107,  101, '0,100,101',  '运维部门',   5, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(108,  102, '0,100,102',  '市场部门',   1, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);
INSERT IGNORE INTO sys_dept VALUES(109,  102, '0,100,102',  '财务部门',   2, '知企', '15888888888', 'zhiqi@qq.com', '0', '0', 'admin', NOW(), '', NULL);

-- 2、用户信息表
CREATE TABLE IF NOT EXISTS sys_user (
  user_id           BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '用户ID',
  dept_id           BIGINT(20)      DEFAULT NULL               COMMENT '部门ID',
  user_name         VARCHAR(30)     NOT NULL                   COMMENT '用户账号',
  nick_name         VARCHAR(30)     NOT NULL                   COMMENT '用户昵称',
  user_type         VARCHAR(2)      DEFAULT '00'               COMMENT '用户类型（00系统用户）',
  email             VARCHAR(50)     DEFAULT ''                 COMMENT '用户邮箱',
  phonenumber       VARCHAR(11)     DEFAULT ''                 COMMENT '手机号码',
  sex               CHAR(1)         DEFAULT '0'                COMMENT '用户性别（0男 1女 2未知）',
  avatar            VARCHAR(100)    DEFAULT ''                 COMMENT '头像地址',
  password          VARCHAR(100)    DEFAULT ''                 COMMENT '密码',
  status            CHAR(1)         DEFAULT '0'                COMMENT '账号状态（0正常 1停用）',
  del_flag          CHAR(1)         DEFAULT '0'                COMMENT '删除标志（0代表存在 2代表删除）',
  login_ip          VARCHAR(128)    DEFAULT ''                 COMMENT '最后登录IP',
  login_date        DATETIME                                   COMMENT '最后登录时间',
  pwd_update_date   DATETIME                                   COMMENT '密码最后更新时间',
  create_by         VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time       DATETIME                                   COMMENT '创建时间',
  update_by         VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time       DATETIME                                   COMMENT '更新时间',
  remark            VARCHAR(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (user_id)
) ENGINE=INNODB AUTO_INCREMENT=100 COMMENT = '用户信息表';

-- 初始化-用户信息表数据
INSERT IGNORE INTO sys_user VALUES(1,  103, 'admin', '知企', '00', 'zhiqi@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), NOW(), 'admin', NOW(), '', NULL, '管理员');
INSERT IGNORE INTO sys_user VALUES(2,  105, 'zhiqi', '知企', '00', 'zhiqi@qq.com',  '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), NOW(), 'admin', NOW(), '', NULL, '测试员');

-- 3、岗位信息表
CREATE TABLE IF NOT EXISTS sys_post (
  post_id       BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '岗位ID',
  post_code     VARCHAR(64)     NOT NULL                   COMMENT '岗位编码',
  post_name     VARCHAR(50)     NOT NULL                   COMMENT '岗位名称',
  post_sort     INT(4)          NOT NULL                   COMMENT '显示顺序',
  status        CHAR(1)         NOT NULL                   COMMENT '状态（0正常 1停用）',
  create_by     VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time   DATETIME                                   COMMENT '创建时间',
  update_by     VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time   DATETIME                                   COMMENT '更新时间',
  remark        VARCHAR(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (post_id)
) ENGINE=INNODB COMMENT = '岗位信息表';

-- 初始化-岗位信息表数据
INSERT IGNORE INTO sys_post VALUES(1, 'ceo',  '董事长',    1, '0', 'admin', NOW(), '', NULL, '');
INSERT IGNORE INTO sys_post VALUES(2, 'se',   '项目经理',  2, '0', 'admin', NOW(), '', NULL, '');
INSERT IGNORE INTO sys_post VALUES(3, 'hr',   '人力资源',  3, '0', 'admin', NOW(), '', NULL, '');
INSERT IGNORE INTO sys_post VALUES(4, 'user', '普通员工',  4, '0', 'admin', NOW(), '', NULL, '');

-- 4、参数配置表（已修复config_value字段长度）
CREATE TABLE IF NOT EXISTS sys_config (
  config_id         BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '参数主键',
  config_name       VARCHAR(100)    DEFAULT ''                 COMMENT '参数名称',
  config_key        VARCHAR(100)    DEFAULT ''                 COMMENT '参数键名',
  config_value      VARCHAR(1000)   DEFAULT ''                 COMMENT '参数键值（已扩展到1000字符）',
  config_type       CHAR(1)         DEFAULT 'N'                COMMENT '系统内置（Y是 N否）',
  create_by         VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time       DATETIME                                   COMMENT '创建时间',
  update_by         VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time       DATETIME                                   COMMENT '更新时间',
  remark            VARCHAR(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (config_id),
  INDEX idx_config_key (config_key)
) ENGINE=INNODB AUTO_INCREMENT=100 COMMENT = '参数配置表';

-- 初始化-参数配置表数据
INSERT IGNORE INTO sys_config VALUES(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',               'skin-blue',     'Y', 'admin', NOW(), '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow' );
INSERT IGNORE INTO sys_config VALUES(2, '用户管理-账号初始密码',         'sys.user.initPassword',            'CHANGE_ME',     'Y', 'admin', NOW(), '', NULL, '⚠️ 初始化密码，部署后请立即修改为强密码' );
INSERT IGNORE INTO sys_config VALUES(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',              'theme-dark',    'Y', 'admin', NOW(), '', NULL, '深色主题theme-dark，浅色主题theme-light' );
INSERT IGNORE INTO sys_config VALUES(4, '账号自助-验证码开关',           'sys.account.captchaEnabled',       'true',          'Y', 'admin', NOW(), '', NULL, '是否开启验证码功能（true开启，false关闭）');
INSERT IGNORE INTO sys_config VALUES(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser',         'false',         'Y', 'admin', NOW(), '', NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT IGNORE INTO sys_config VALUES(6, '用户登录-黑名单列表',           'sys.login.blackIPList',            '',              'Y', 'admin', NOW(), '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
INSERT IGNORE INTO sys_config VALUES(7, '用户管理-初始密码修改策略',     'sys.account.initPasswordModify',   '1',             'Y', 'admin', NOW(), '', NULL, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
INSERT IGNORE INTO sys_config VALUES(8, '用户管理-账号密码更新周期',     'sys.account.passwordValidateDays', '0',             'Y', 'admin', NOW(), '', NULL, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');
INSERT IGNORE INTO sys_config VALUES(9, '站点头像',                       'blog_avatar',                       '',              'Y', 'admin', NOW(), '', NULL, '站点头像URL，用于站点前台页面显示。与zhi_setting表中的blog_avatar保持同步');

-- 5、角色信息表
CREATE TABLE IF NOT EXISTS sys_role (
  role_id              BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '角色ID',
  role_name            VARCHAR(30)     NOT NULL                   COMMENT '角色名称',
  role_key             VARCHAR(100)    NOT NULL                   COMMENT '角色权限字符串',
  role_sort            INT(4)          NOT NULL                   COMMENT '显示顺序',
  data_scope           CHAR(1)         DEFAULT '1'                COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  menu_check_strictly  TINYINT(1)      DEFAULT 1                  COMMENT '菜单树选择项是否关联显示',
  dept_check_strictly  TINYINT(1)      DEFAULT 1                  COMMENT '部门树选择项是否关联显示',
  status               CHAR(1)         NOT NULL                   COMMENT '角色状态（0正常 1停用）',
  del_flag             CHAR(1)         DEFAULT '0'                COMMENT '删除标志（0代表存在 2代表删除）',
  create_by            VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time          DATETIME                                   COMMENT '创建时间',
  update_by            VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time          DATETIME                                   COMMENT '更新时间',
  remark               VARCHAR(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (role_id)
) ENGINE=INNODB AUTO_INCREMENT=100 COMMENT = '角色信息表';

-- 初始化-角色信息表数据
INSERT IGNORE INTO sys_role VALUES('1', '超级管理员',  'admin',  1, 1, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '超级管理员');
INSERT IGNORE INTO sys_role VALUES('2', '普通角色',    'common', 2, 2, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '普通角色');
INSERT IGNORE INTO sys_role VALUES('3', '注册用户',    'site_user', 3, 1, 1, 1, '0', '0', 'admin', NOW(), '', NULL, '注册用户，用于站点前台注册登录');

-- 6、菜单权限表
CREATE TABLE IF NOT EXISTS sys_menu (
  menu_id           BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '菜单ID',
  menu_name         VARCHAR(50)     NOT NULL                   COMMENT '菜单名称',
  parent_id         BIGINT(20)      DEFAULT 0                  COMMENT '父菜单ID',
  order_num         INT(4)          DEFAULT 0                  COMMENT '显示顺序',
  path              VARCHAR(200)    DEFAULT ''                 COMMENT '路由地址',
  component         VARCHAR(255)    DEFAULT NULL               COMMENT '组件路径',
  query             VARCHAR(255)    DEFAULT NULL               COMMENT '路由参数',
  route_name        VARCHAR(50)     DEFAULT ''                 COMMENT '路由名称',
  is_frame          INT(1)          DEFAULT 1                  COMMENT '是否为外链（0是 1否）',
  is_cache          INT(1)          DEFAULT 0                  COMMENT '是否缓存（0缓存 1不缓存）',
  menu_type         CHAR(1)         DEFAULT ''                 COMMENT '菜单类型（M目录 C菜单 F按钮）',
  visible           CHAR(1)         DEFAULT 0                  COMMENT '菜单状态（0显示 1隐藏）',
  status            CHAR(1)         DEFAULT 0                  COMMENT '菜单状态（0正常 1停用）',
  perms             VARCHAR(100)    DEFAULT NULL               COMMENT '权限标识',
  icon              VARCHAR(100)    DEFAULT '#'                COMMENT '菜单图标',
  create_by         VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time       DATETIME                                   COMMENT '创建时间',
  update_by         VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time       DATETIME                                   COMMENT '更新时间',
  remark            VARCHAR(500)    DEFAULT ''                 COMMENT '备注',
  PRIMARY KEY (menu_id)
) ENGINE=INNODB AUTO_INCREMENT=2000 COMMENT = '菜单权限表';

-- 初始化-菜单信息表数据
INSERT IGNORE INTO sys_menu VALUES('1', '系统管理', '0', '5', 'admin/system',    NULL, '', '', 1, 0, 'M', '0', '0', '', 'system',   'admin', NOW(), '', NULL, '系统管理目录');
INSERT IGNORE INTO sys_menu VALUES('2', '系统监控', '0', '3', 'admin/monitor',   NULL, '', '', 1, 0, 'M', '0', '0', '', 'monitor',  'admin', NOW(), '', NULL, '系统监控目录');
INSERT IGNORE INTO sys_menu VALUES('3', '系统工具', '0', '4', 'admin/tool',      NULL, '', '', 1, 0, 'M', '0', '0', '', 'tool',     'admin', NOW(), '', NULL, '系统工具目录');

-- 7、用户和角色关联表（用户N-1角色）
CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id   BIGINT(20) NOT NULL COMMENT '用户ID',
  role_id   BIGINT(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id)
) ENGINE=INNODB COMMENT = '用户和角色关联表';

-- 初始化-用户和角色关联表数据（admin用户分配超级管理员角色，普通用户分配普通角色）
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2);

-- 8、角色和菜单关联表（角色1-N菜单）
CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id   BIGINT(20) NOT NULL COMMENT '角色ID',
  menu_id   BIGINT(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (role_id, menu_id)
) ENGINE=INNODB COMMENT = '角色和菜单关联表';

-- 9、角色和部门关联表（角色1-N部门）
CREATE TABLE IF NOT EXISTS sys_role_dept (
  role_id   BIGINT(20) NOT NULL COMMENT '角色ID',
  dept_id   BIGINT(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (role_id, dept_id)
) ENGINE=INNODB COMMENT = '角色和部门关联表';

-- 10、用户与岗位关联表（用户1-N岗位）
CREATE TABLE IF NOT EXISTS sys_user_post (
  user_id   BIGINT(20) NOT NULL COMMENT '用户ID',
  post_id   BIGINT(20) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (user_id, post_id)
) ENGINE=INNODB COMMENT = '用户与岗位关联表';

-- 11、字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
  dict_id          BIGINT(20)     NOT NULL AUTO_INCREMENT    COMMENT '字典主键',
  dict_name        VARCHAR(100)   DEFAULT ''                 COMMENT '字典名称',
  dict_type        VARCHAR(100)   DEFAULT ''                 COMMENT '字典类型',
  status           CHAR(1)        DEFAULT '0'                COMMENT '状态（0正常 1停用）',
  create_by        VARCHAR(64)    DEFAULT ''                 COMMENT '创建者',
  create_time      DATETIME                                  COMMENT '创建时间',
  update_by        VARCHAR(64)    DEFAULT ''                 COMMENT '更新者',
  update_time      DATETIME                                  COMMENT '更新时间',
  remark           VARCHAR(500)   DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (dict_id),
  UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=INNODB AUTO_INCREMENT=100 COMMENT = '字典类型表';

-- 12、字典数据表
CREATE TABLE IF NOT EXISTS sys_dict_data (
  dict_code        BIGINT(20)     NOT NULL AUTO_INCREMENT    COMMENT '字典编码',
  dict_sort        INT(4)         DEFAULT 0                  COMMENT '字典排序',
  dict_label       VARCHAR(100)   DEFAULT ''                 COMMENT '字典标签',
  dict_value       VARCHAR(100)   DEFAULT ''                 COMMENT '字典键值',
  dict_type        VARCHAR(100)   DEFAULT ''                 COMMENT '字典类型',
  css_class        VARCHAR(100)   DEFAULT NULL               COMMENT '样式属性（其他样式扩展）',
  list_class       VARCHAR(100)   DEFAULT NULL               COMMENT '表格回显样式',
  is_default       CHAR(1)        DEFAULT 'N'                COMMENT '是否默认（Y是 N否）',
  status           CHAR(1)        DEFAULT '0'                COMMENT '状态（0正常 1停用）',
  create_by        VARCHAR(64)    DEFAULT ''                 COMMENT '创建者',
  create_time      DATETIME                                  COMMENT '创建时间',
  update_by        VARCHAR(64)    DEFAULT ''                 COMMENT '更新者',
  update_time      DATETIME                                  COMMENT '更新时间',
  remark           VARCHAR(500)   DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (dict_code)
) ENGINE=INNODB AUTO_INCREMENT=100 COMMENT = '字典数据表';

-- 初始化字典数据
INSERT IGNORE INTO sys_dict_type VALUES(1,  '用户性别', 'sys_user_sex',        '0', 'admin', NOW(), '', NULL, '用户性别列表');
INSERT IGNORE INTO sys_dict_type VALUES(2,  '菜单状态', 'sys_show_hide',       '0', 'admin', NOW(), '', NULL, '菜单状态列表');
INSERT IGNORE INTO sys_dict_type VALUES(3,  '系统开关', 'sys_normal_disable',  '0', 'admin', NOW(), '', NULL, '系统开关列表');
INSERT IGNORE INTO sys_dict_type VALUES(4,  '评论状态', 'comment_status',     '0', 'admin', NOW(), '', NULL, '评论状态列表');
INSERT IGNORE INTO sys_dict_type VALUES(5,  '公告类型', 'sys_notice_type',    '0', 'admin', NOW(), '', NULL, '公告类型列表');
INSERT IGNORE INTO sys_dict_type VALUES(6,  '公告状态', 'sys_notice_status',  '0', 'admin', NOW(), '', NULL, '公告状态列表');

INSERT IGNORE INTO sys_dict_data VALUES(1,  1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', '0', 'admin', NOW(), '', NULL, '性别男');
INSERT IGNORE INTO sys_dict_data VALUES(2,  2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', '0', 'admin', NOW(), '', NULL, '性别女');
INSERT IGNORE INTO sys_dict_data VALUES(3,  3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', '0', 'admin', NOW(), '', NULL, '性别未知');

INSERT IGNORE INTO sys_dict_data VALUES(4,  1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '显示菜单');
INSERT IGNORE INTO sys_dict_data VALUES(5,  2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '隐藏菜单');

INSERT IGNORE INTO sys_dict_data VALUES(6,  1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态');
INSERT IGNORE INTO sys_dict_data VALUES(7,  2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '停用状态');

INSERT IGNORE INTO sys_dict_data VALUES(8,  1,  '待审核',   '0',       'comment_status',      '',   'warning',  'N', '0', 'admin', NOW(), '', NULL, '待审核状态');
INSERT IGNORE INTO sys_dict_data VALUES(9,  2,  '已审核',   '1',       'comment_status',      '',   'success', 'N', '0', 'admin', NOW(), '', NULL, '已审核状态');
INSERT IGNORE INTO sys_dict_data VALUES(10, 3,  '已删除',   '2',       'comment_status',      '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '已删除状态');

INSERT IGNORE INTO sys_dict_data VALUES(11, 1,  '通知',     '1',       'sys_notice_type',     '',   'primary', 'N', '0', 'admin', NOW(), '', NULL, '通知');
INSERT IGNORE INTO sys_dict_data VALUES(12, 2,  '公告',     '2',       'sys_notice_type',     '',   'success', 'N', '0', 'admin', NOW(), '', NULL, '公告');

INSERT IGNORE INTO sys_dict_data VALUES(13, 1,  '正常',     '0',       'sys_notice_status',   '',   'primary', 'Y', '0', 'admin', NOW(), '', NULL, '正常状态');
INSERT IGNORE INTO sys_dict_data VALUES(14, 2,  '关闭',     '1',       'sys_notice_status',   '',   'danger',  'N', '0', 'admin', NOW(), '', NULL, '关闭状态');

-- 13、通知公告表
CREATE TABLE IF NOT EXISTS sys_notice (
  notice_id         BIGINT(20)     NOT NULL AUTO_INCREMENT    COMMENT '公告ID',
  notice_title      VARCHAR(50)    NOT NULL                   COMMENT '公告标题',
  notice_type       CHAR(1)        DEFAULT ''                 COMMENT '公告类型（1通知 2公告）',
  notice_content    LONGTEXT       DEFAULT NULL               COMMENT '公告内容',
  status            CHAR(1)        DEFAULT '0'                COMMENT '公告状态（0正常 1关闭）',
  create_by         VARCHAR(64)    DEFAULT ''                 COMMENT '创建者',
  create_time       DATETIME                                   COMMENT '创建时间',
  update_by         VARCHAR(64)    DEFAULT ''                 COMMENT '更新者',
  update_time       DATETIME                                   COMMENT '更新时间',
  remark            VARCHAR(255)   DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (notice_id)
) ENGINE=INNODB AUTO_INCREMENT=10 COMMENT = '通知公告表';

-- 9、系统访问记录表
CREATE TABLE IF NOT EXISTS `sys_logininfor` (
  `info_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` VARCHAR(50) DEFAULT '' COMMENT '用户账号',
  `ipaddr` VARCHAR(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` VARCHAR(255) DEFAULT '' COMMENT '登录地点',
  `browser` VARCHAR(50) DEFAULT '' COMMENT '浏览器类型',
  `os` VARCHAR(50) DEFAULT '' COMMENT '操作系统',
  `status` CHAR(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` VARCHAR(255) DEFAULT '' COMMENT '提示消息',
  `login_time` DATETIME DEFAULT NULL COMMENT '登录时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_user_name` (`user_name`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=INNODB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统访问记录';

-- 10、操作日志记录表
CREATE TABLE IF NOT EXISTS `sys_oper_log` (
  `oper_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` VARCHAR(50) DEFAULT '' COMMENT '模块标题',
  `business_type` INT(2) DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` VARCHAR(100) DEFAULT '' COMMENT '方法名称',
  `request_method` VARCHAR(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` INT(1) DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` VARCHAR(50) DEFAULT '' COMMENT '操作人员',
  `dept_name` VARCHAR(50) DEFAULT '' COMMENT '部门名称',
  `oper_url` VARCHAR(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` VARCHAR(128) DEFAULT '' COMMENT '主机地址',
  `oper_location` VARCHAR(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` VARCHAR(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` VARCHAR(2000) DEFAULT '' COMMENT '返回参数',
  `status` INT(1) DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` VARCHAR(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` DATETIME DEFAULT NULL COMMENT '操作时间',
  `cost_time` BIGINT(20) DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_oper_name` (`oper_name`),
  KEY `idx_business_type` (`business_type`)
) ENGINE=INNODB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志记录';

-- ========== 导入Quartz定时任务表结构 ==========

-- 按照正确的顺序删除表（先删除有外键约束的表）

-- 1、存储每一个已配置的 jobDetail 的详细信息
CREATE TABLE IF NOT EXISTS QRTZ_JOB_DETAILS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    job_name             VARCHAR(200)    NOT NULL            COMMENT '任务名称',
    job_group            VARCHAR(200)    NOT NULL            COMMENT '任务组名',
    description          VARCHAR(250)    NULL                COMMENT '相关介绍',
    job_class_name       VARCHAR(250)    NOT NULL            COMMENT '执行任务类名称',
    is_durable           VARCHAR(1)      NOT NULL            COMMENT '是否持久化',
    is_nonconcurrent     VARCHAR(1)      NOT NULL            COMMENT '是否并发',
    is_update_data       VARCHAR(1)      NOT NULL            COMMENT '是否更新数据',
    requests_recovery    VARCHAR(1)      NOT NULL            COMMENT '是否接受恢复执行',
    job_data             BLOB            NULL                COMMENT '存放持久化job对象',
    PRIMARY KEY (sched_name, job_name, job_group)
) ENGINE=INNODB COMMENT = '任务详细信息表';

-- 2、存储已配置的 Trigger 的信息
CREATE TABLE IF NOT EXISTS QRTZ_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    trigger_name         VARCHAR(200)    NOT NULL            COMMENT '触发器的名字',
    trigger_group        VARCHAR(200)    NOT NULL            COMMENT '触发器所属组的名字',
    job_name             VARCHAR(200)    NOT NULL            COMMENT 'qrtz_job_details表job_name的外键',
    job_group            VARCHAR(200)    NOT NULL            COMMENT 'qrtz_job_details表job_group的外键',
    description          VARCHAR(250)    NULL                COMMENT '相关介绍',
    next_fire_time       BIGINT(13)      NULL                COMMENT '上一次触发时间（毫秒）',
    prev_fire_time       BIGINT(13)      NULL                COMMENT '下一次触发时间（默认为-1表示不触发）',
    priority             INTEGER         NULL                COMMENT '优先级',
    trigger_state        VARCHAR(16)     NOT NULL            COMMENT '触发器状态',
    trigger_type         VARCHAR(8)      NOT NULL            COMMENT '触发器的类型',
    start_time           BIGINT(13)      NOT NULL            COMMENT '开始时间',
    end_time             BIGINT(13)      NULL                COMMENT '结束时间',
    calendar_name        VARCHAR(200)    NULL                COMMENT '日程表名称',
    misfire_instr        SMALLINT(2)     NULL                COMMENT '补偿执行的策略',
    job_data             BLOB            NULL                COMMENT '存放持久化job对象',
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, job_name, job_group) REFERENCES QRTZ_JOB_DETAILS(sched_name, job_name, job_group)
) ENGINE=INNODB COMMENT = '触发器详细信息表';

-- 3、 存储简单的 Trigger，包括重复次数，间隔，以及已触发的次数
CREATE TABLE IF NOT EXISTS QRTZ_SIMPLE_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    trigger_name         VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_name的外键',
    trigger_group        VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_group的外键',
    repeat_count         BIGINT(7)       NOT NULL            COMMENT '重复的次数统计',
    repeat_interval      BIGINT(12)      NOT NULL            COMMENT '重复的间隔时间',
    times_triggered      BIGINT(10)      NOT NULL            COMMENT '已经触发的次数',
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
) ENGINE=INNODB COMMENT = '简单触发器的信息表';

-- 4、 存储 Cron Trigger，包括 Cron 表达式和时区信息
CREATE TABLE IF NOT EXISTS QRTZ_CRON_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    trigger_name         VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_name的外键',
    trigger_group        VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_group的外键',
    cron_expression      VARCHAR(200)    NOT NULL            COMMENT 'cron表达式',
    time_zone_id         VARCHAR(80)                         COMMENT '时区',
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
) ENGINE=INNODB COMMENT = 'Cron类型的触发器表';

-- 5、 Trigger 作为 Blob 类型存储
CREATE TABLE IF NOT EXISTS QRTZ_BLOB_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    trigger_name         VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_name的外键',
    trigger_group        VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_group的外键',
    blob_data            BLOB            NULL                COMMENT '存放持久化Trigger对象',
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
) ENGINE=INNODB COMMENT = 'Blob类型的触发器表';

-- 6、 以 Blob 类型存储存放日历信息
CREATE TABLE IF NOT EXISTS QRTZ_CALENDARS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    calendar_name        VARCHAR(200)    NOT NULL            COMMENT '日历名称',
    calendar             BLOB            NOT NULL            COMMENT '存放持久化calendar对象',
    PRIMARY KEY (sched_name, calendar_name)
) ENGINE=INNODB COMMENT = '日历信息表';

-- 7、 存储已暂停的 Trigger 组的信息
CREATE TABLE IF NOT EXISTS QRTZ_PAUSED_TRIGGER_GRPS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    trigger_group        VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_group的外键',
    PRIMARY KEY (sched_name, trigger_group)
) ENGINE=INNODB COMMENT = '暂停的触发器表';

-- 8、 存储与已触发的 Trigger 相关的状态信息
CREATE TABLE IF NOT EXISTS QRTZ_FIRED_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    entry_id             VARCHAR(95)     NOT NULL            COMMENT '调度器实例id',
    trigger_name         VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_name的外键',
    trigger_group        VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_group的外键',
    instance_name        VARCHAR(200)    NOT NULL            COMMENT '调度器实例名',
    fired_time           BIGINT(13)      NOT NULL            COMMENT '触发的时间',
    sched_time           BIGINT(13)      NOT NULL            COMMENT '定时器制定的时间',
    priority             INTEGER         NOT NULL            COMMENT '优先级',
    state                VARCHAR(16)     NOT NULL            COMMENT '状态',
    job_name             VARCHAR(200)    NULL                COMMENT '任务名称',
    job_group            VARCHAR(200)    NULL                COMMENT '任务组名',
    is_nonconcurrent     VARCHAR(1)      NULL                COMMENT '是否并发',
    requests_recovery    VARCHAR(1)      NULL                COMMENT '是否接受恢复执行',
    PRIMARY KEY (sched_name, entry_id)
) ENGINE=INNODB COMMENT = '已触发的触发器表';

-- 9、 存储调度器的状态信息
CREATE TABLE IF NOT EXISTS QRTZ_SCHEDULER_STATE (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    instance_name        VARCHAR(200)    NOT NULL            COMMENT '实例名称',
    last_checkin_time    BIGINT(13)      NOT NULL            COMMENT '上次检查时间',
    checkin_interval     BIGINT(13)      NOT NULL            COMMENT '检查间隔时间',
    PRIMARY KEY (sched_name, instance_name)
) ENGINE=INNODB COMMENT = '调度器状态表';

-- 10、 存储程序的悲观锁的信息
CREATE TABLE IF NOT EXISTS QRTZ_LOCKS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    lock_name            VARCHAR(40)     NOT NULL            COMMENT '悲观锁名称',
    PRIMARY KEY (sched_name, lock_name)
) ENGINE=INNODB COMMENT = '存储的悲观锁信息表';

-- 11、 Quartz集群实现同步机制的行锁表
CREATE TABLE IF NOT EXISTS QRTZ_SIMPROP_TRIGGERS (
    sched_name           VARCHAR(120)    NOT NULL            COMMENT '调度名称',
    trigger_name         VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_name的外键',
    trigger_group        VARCHAR(200)    NOT NULL            COMMENT 'qrtz_triggers表trigger_group的外键',
    str_prop_1           VARCHAR(512)    NULL                COMMENT 'String类型的trigger的第一个参数',
    str_prop_2           VARCHAR(512)    NULL                COMMENT 'String类型的trigger的第二个参数',
    str_prop_3           VARCHAR(512)    NULL                COMMENT 'String类型的trigger的第三个参数',
    int_prop_1           INT             NULL                COMMENT 'int类型的trigger的第一个参数',
    int_prop_2           INT             NULL                COMMENT 'int类型的trigger的第二个参数',
    long_prop_1          BIGINT          NULL                COMMENT 'long类型的trigger的第一个参数',
    long_prop_2          BIGINT          NULL                COMMENT 'long类型的trigger的第二个参数',
    dec_prop_1           NUMERIC(13,4)   NULL                COMMENT 'decimal类型的trigger的第一个参数',
    dec_prop_2           NUMERIC(13,4)   NULL                COMMENT 'decimal类型的trigger的第二个参数',
    bool_prop_1          VARCHAR(1)      NULL                COMMENT 'Boolean类型的trigger的第一个参数',
    bool_prop_2          VARCHAR(1)      NULL                COMMENT 'Boolean类型的trigger的第二个参数',
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES QRTZ_TRIGGERS(sched_name, trigger_name, trigger_group)
) ENGINE=INNODB COMMENT = '同步机制的行锁表';

-- ========== 导入站点系统完整表结构和数据 ==========

-- 定时任务调度表
CREATE TABLE IF NOT EXISTS sys_job (
    job_id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    job_name varchar(64) NOT NULL DEFAULT '' COMMENT '任务名称',
    job_group varchar(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
    invoke_target varchar(500) NOT NULL COMMENT '调用目标字符串',
    cron_expression varchar(255) DEFAULT '' COMMENT 'cron执行表达式',
    misfire_policy varchar(20) DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
    concurrent char(1) DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
    status char(1) DEFAULT '0' COMMENT '状态（0正常 1暂停）',
    create_by varchar(64) DEFAULT '' COMMENT '创建者',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by varchar(64) DEFAULT '' COMMENT '更新者',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    remark varchar(500) DEFAULT '' COMMENT '备注信息',
    PRIMARY KEY (job_id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度表';

-- 定时任务执行日志表
CREATE TABLE IF NOT EXISTS sys_job_log (
    job_log_id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
    job_name varchar(64) NOT NULL COMMENT '任务名称',
    job_group varchar(64) NOT NULL COMMENT '任务组名',
    invoke_target varchar(500) NOT NULL COMMENT '调用目标字符串',
    job_message varchar(500) DEFAULT NULL COMMENT '日志信息',
    status char(1) DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
    exception_info varchar(2000) DEFAULT '' COMMENT '异常信息',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (job_log_id)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务执行日志表';

-- 博客文章表
CREATE TABLE IF NOT EXISTS `zhi_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `content` longtext NOT NULL COMMENT '文章内容（HTML，展示用）',
  `content_md` longtext COMMENT 'Markdown 源码（format=markdown 时保存）',
  `format` varchar(10) NOT NULL DEFAULT 'html' COMMENT '内容格式 html|markdown',
  `article_type` varchar(20) NOT NULL DEFAULT 'news' COMMENT '内容类型：news 资讯 business 核心业务 case 客户案例',
  `seo_title` varchar(200) DEFAULT NULL COMMENT 'SEO 标题（留空回退文章标题）',
  `seo_keywords` varchar(255) DEFAULT NULL COMMENT 'SEO 关键词（英文逗号分隔）',
  `seo_description` varchar(500) DEFAULT NULL COMMENT 'SEO 描述（留空回退文章摘要）',
  `slug` varchar(100) DEFAULT NULL COMMENT 'URL 别名（伪静态 /blog/article/{slug}，留空用数字 ID）',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '封面图片URL',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `author_id` bigint DEFAULT NULL COMMENT '作者ID',
  `author_name` varchar(50) DEFAULT NULL COMMENT '作者名称',
  `is_top` tinyint DEFAULT '0' COMMENT '是否置顶：0否 1是',
  `is_recommend` tinyint DEFAULT '0' COMMENT '是否推荐：0否 1是',
  `status` tinyint DEFAULT '0' COMMENT '文章状态：0草稿 1已发布 2定时发布(待发布)',
  `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间（status=2 时生效）',
  `view_count` int DEFAULT '0' COMMENT '浏览量（已使用IFNULL处理NULL值，默认从0开始计数）',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `comment_count` int DEFAULT '0' COMMENT '评论数',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客文章表';

-- 博客分类表
CREATE TABLE IF NOT EXISTS `zhi_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `alias` varchar(100) DEFAULT NULL COMMENT '分类别名',
  `description` varchar(255) DEFAULT NULL COMMENT '分类描述',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID，0表示顶级分类',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `article_count` int DEFAULT '0' COMMENT '文章数量',
  `status` tinyint DEFAULT '1' COMMENT '状态：0禁用 1启用',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `sort` int DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客分类表';

-- 博客标签表
CREATE TABLE IF NOT EXISTS `zhi_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) NOT NULL COMMENT '标签名称',
  `description` varchar(255) DEFAULT NULL COMMENT '标签描述',
  `color` varchar(20) DEFAULT '#409EFF' COMMENT '标签颜色',
  `icon` varchar(100) DEFAULT NULL COMMENT '标签图标',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志 0正常 1删除',
  `article_count` int DEFAULT '0' COMMENT '文章数量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客标签表';

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS `zhi_article_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章标签关联表';

-- 博客评论表
CREATE TABLE IF NOT EXISTS `zhi_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（可为空，匿名评论）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称（匿名评论用）',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱（匿名评论用）',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
  `status` tinyint DEFAULT '1' COMMENT '状态 0待审核 1正常 2已删除',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `ip` varchar(64) DEFAULT NULL COMMENT '评论IP',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客评论表';

-- 一次性校正历史评论数（按已发布评论重算 zhi_article.comment_count，幂等可重复执行）
UPDATE zhi_article ba
SET ba.comment_count = (SELECT COUNT(*) FROM zhi_comment c
                        WHERE c.article_id = ba.id AND c.status = '1')
WHERE ba.del_flag = '0';

-- 博客友情链接表
CREATE TABLE IF NOT EXISTS `zhi_friend_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) NOT NULL COMMENT '友链名称',
  `url` varchar(255) NOT NULL COMMENT '友链地址',
  `logo` varchar(255) DEFAULT NULL COMMENT '友链Logo',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `email` varchar(100) DEFAULT NULL COMMENT '申请人邮箱（友链申请）',
  `status` tinyint DEFAULT '1' COMMENT '状态 0正常 1停用 2待审核',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客友链表';

-- 博客上传记录表（附件/媒体库）
-- 博客留言板表（独立于文章的留言，支持匿名提交与后台审核/回复）
CREATE TABLE IF NOT EXISTS `zhi_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（匿名为空）',
  `nickname` varchar(64) NOT NULL COMMENT '昵称',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱（不公开）',
  `website` varchar(255) DEFAULT NULL COMMENT '个人网站',
  `content` varchar(500) NOT NULL COMMENT '留言内容',
  `reply_content` varchar(500) DEFAULT NULL COMMENT '管理员回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '回复时间',
  `reply_by` varchar(64) DEFAULT NULL COMMENT '回复人',
  `status` tinyint DEFAULT '0' COMMENT '状态 0待审核 1已发布 2已拒绝',
  `ip` varchar(64) DEFAULT NULL COMMENT '留言IP',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '浏览器UA',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`id`),
  KEY `idx_message_status` (`status`),
  KEY `idx_message_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客留言板表';

-- 博客自定义页面表（单页文档，支持 Markdown 内容与 SEO）
CREATE TABLE IF NOT EXISTS `zhi_page` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(100) NOT NULL COMMENT '页面标题',
  `slug` varchar(100) NOT NULL COMMENT '页面别名（用于前台 URL）',
  `summary` varchar(255) DEFAULT NULL COMMENT '页面摘要',
  `content` longtext COMMENT '页面内容（Markdown）',
  `status` tinyint DEFAULT '0' COMMENT '状态 0草稿 1已发布',
  `show_in_nav` tinyint DEFAULT '1' COMMENT '是否在导航显示 0否 1是',
  `sort` int DEFAULT '0' COMMENT '排序',
  `view_count` bigint DEFAULT '0' COMMENT '浏览次数',
  `seo_title` varchar(255) DEFAULT NULL COMMENT 'SEO标题',
  `seo_keywords` varchar(255) DEFAULT NULL COMMENT 'SEO关键词',
  `seo_description` varchar(500) DEFAULT NULL COMMENT 'SEO描述',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_slug` (`slug`),
  KEY `idx_page_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客自定义页面表';

CREATE TABLE IF NOT EXISTS `zhi_upload` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_name` varchar(255) NOT NULL COMMENT '存储文件名',
  `original_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `url` varchar(500) DEFAULT NULL COMMENT '访问路径',
  `mime_type` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `upload_type` varchar(40) DEFAULT 'upload' COMMENT '上传类型：upload/compressed/avatar/thumbnail/article-cover/mobile/watermark',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_upload_type` (`upload_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客上传记录表';

-- 文章按日 PV/UV 统计表
CREATE TABLE IF NOT EXISTS `zhi_article_daily_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `pv` bigint NOT NULL DEFAULT '0' COMMENT '当日浏览量（去重计数）',
  `uv` bigint NOT NULL DEFAULT '0' COMMENT '当日独立访客数',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_date` (`article_id`, `stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章按日PV/UV统计表';

-- 博客系统设置表
-- 访问明细表（PV/UV 明细：每次内容访问一行，is_unique 表示当日该访客首次）
CREATE TABLE IF NOT EXISTS `zhi_visit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `target_type` varchar(20) NOT NULL DEFAULT 'article' COMMENT '目标类型（article文章 page自定义页面）',
  `target_id` bigint DEFAULT NULL COMMENT '目标ID（文章为文章ID，页面为页面ID）',
  `path` varchar(255) DEFAULT NULL COMMENT '访问路径',
  `user_id` bigint DEFAULT NULL COMMENT '登录用户ID（匿名为空）',
  `visitor_key` varchar(80) DEFAULT NULL COMMENT '访客标识（u<id> 或 ip:<ip>）',
  `ip` varchar(64) DEFAULT NULL COMMENT '访客IP',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '浏览器UA',
  `referer` varchar(500) DEFAULT NULL COMMENT '来源页',
  `is_unique` tinyint DEFAULT '0' COMMENT '是否当日该访客首次访问（1是 0否）',
  `visit_date` date DEFAULT NULL COMMENT '访问日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_visit_date` (`visit_date`),
  KEY `idx_visit_target` (`target_type`, `target_id`),
  KEY `idx_visit_visitor` (`visitor_key`, `visit_date`),
  KEY `idx_visit_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='访问明细表（PV/UV 明细）';

CREATE TABLE IF NOT EXISTS `zhi_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(64) NOT NULL COMMENT '配置项Key',
  `config_value` longtext COMMENT '配置项Value',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='博客系统设置表';

-- 博客邮箱验证码表
CREATE TABLE IF NOT EXISTS `zhi_email_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `email` varchar(50) NOT NULL COMMENT '邮箱地址',
  `code` varchar(6) NOT NULL COMMENT '验证码',
  `code_type` varchar(20) NOT NULL COMMENT '验证码类型：register=注册, reset=重置密码, bind=绑定邮箱',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `used` tinyint(1) DEFAULT '0' COMMENT '是否已使用：0=未使用, 1=已使用',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '请求IP地址',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  PRIMARY KEY (`id`),
  KEY `idx_email_code` (`email`, `code_type`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客邮箱验证码表';

-- ========== 代码生成器表 ==========

-- 代码生成业务表
CREATE TABLE IF NOT EXISTS gen_table (
  table_id          BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT 'id',
  table_name        VARCHAR(200)    DEFAULT ''                 COMMENT '表名称',
  table_comment     VARCHAR(500)    DEFAULT ''                 COMMENT '表描述',
  sub_table_name    VARCHAR(64)     DEFAULT NULL               COMMENT '关联子表的表名',
  sub_table_fk_name VARCHAR(64)     DEFAULT NULL               COMMENT '子表关联的外键名',
  class_name        VARCHAR(100)    DEFAULT ''                 COMMENT '实体类名称',
  tpl_category      VARCHAR(200)    DEFAULT 'crud'              COMMENT '使用的模板（crud单表 tree树表）',
  tpl_web_type      VARCHAR(30)     DEFAULT 'element-plus'      COMMENT '前端模板类型（element-plus element-ui）',
  package_name      VARCHAR(100)    DEFAULT NULL               COMMENT '生成包路径',
  module_name       VARCHAR(30)     DEFAULT NULL               COMMENT '生成模块名',
  business_name     VARCHAR(30)     DEFAULT NULL               COMMENT '生成业务名',
  function_name     VARCHAR(50)     DEFAULT NULL               COMMENT '生成功能名',
  function_author   VARCHAR(50)     DEFAULT NULL               COMMENT '生成功能作者',
  gen_type          CHAR(1)         DEFAULT '0'                COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  gen_path          VARCHAR(200)    DEFAULT '/'                 COMMENT '生成路径（不填默认项目路径）',
  options           VARCHAR(1000)   DEFAULT NULL               COMMENT '其它生成选项',
  create_by         VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time       DATETIME                                   COMMENT '创建时间',
  update_by         VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time       DATETIME                                   COMMENT '更新时间',
  remark            VARCHAR(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (table_id)
) ENGINE=INNODB AUTO_INCREMENT=1 COMMENT = '代码生成业务表';

-- 代码生成字段表
CREATE TABLE IF NOT EXISTS gen_table_column (
  column_id         BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '列id',
  table_id          BIGINT(20)      DEFAULT NULL               COMMENT '归属表id',
  column_name       VARCHAR(200)    DEFAULT NULL               COMMENT '列名称',
  column_comment    VARCHAR(500)    DEFAULT NULL               COMMENT '列描述',
  column_type       VARCHAR(100)    DEFAULT NULL               COMMENT '列类型',
  java_type         VARCHAR(500)    DEFAULT NULL               COMMENT 'Java类型',
  java_field        VARCHAR(200)    DEFAULT NULL               COMMENT 'Java字段名',
  is_pk             CHAR(1)         DEFAULT NULL               COMMENT '是否主键（1是）',
  is_increment      CHAR(1)         DEFAULT NULL               COMMENT '是否自增（1是）',
  is_required       CHAR(1)         DEFAULT NULL               COMMENT '是否必填（1是）',
  is_insert         CHAR(1)         DEFAULT NULL               COMMENT '是否为插入字段（1是）',
  is_edit           CHAR(1)         DEFAULT NULL               COMMENT '是否编辑字段（1是）',
  is_list           CHAR(1)         DEFAULT NULL               COMMENT '是否列表字段（1是）',
  is_query          CHAR(1)         DEFAULT NULL               COMMENT '是否查询字段（1是）',
  query_type        VARCHAR(200)    DEFAULT 'EQ'               COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  html_type         VARCHAR(200)    DEFAULT 'input'             COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  dict_type         VARCHAR(200)    DEFAULT ''                 COMMENT '字典类型',
  sort              INT(4)          DEFAULT NULL               COMMENT '排序',
  create_by         VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time       DATETIME                                   COMMENT '创建时间',
  update_by         VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time       DATETIME                                   COMMENT '更新时间',
  PRIMARY KEY (column_id)
) ENGINE=INNODB AUTO_INCREMENT=1 COMMENT = '代码生成字段表';

-- ========== 初始化站点系统数据 ==========

-- 初始化完整的站点设置数据（38项配置）
INSERT IGNORE INTO `zhi_setting` (`config_key`, `config_value`, `description`, `create_time`, `update_time`) VALUES
-- 基础信息设置
('blog_name', '知企建站系统', '站点名称', NOW(), NOW()),
('blog_desc', '企业官网 · 内容管理与品牌门户', '站点描述', NOW(), NOW()),
('blog_author', 'admin', '站长', NOW(), NOW()),
('blog_keywords', '企业官网,建站系统,企业网站建设,数字化转型,Vue,Spring Boot', '站点关键词', NOW(), NOW()),
('blog_copyright', 'Copyright © 2025-2026 知企建站系统. All rights reserved.', '版权信息', NOW(), NOW()),
('blog_beian', '陕ICP备12345678号', '备案信息', NOW(), NOW()),

-- 联系方式设置
('blog_email', 'admin@example.com', '站点联系邮箱', NOW(), NOW()),
('blog_url', 'http://localhost:3000', '站点访问地址', NOW(), NOW()),
-- 监控入口（留空表示按站点访问地址自动推导：Prometheus +:9090 / Grafana +:3001 / Actuator 同源）
('prometheus_url', '', 'Prometheus 监控地址', NOW(), NOW()),
('grafana_url', '', 'Grafana 监控地址', NOW(), NOW()),
('actuator_url', '', 'Actuator 监控地址', NOW(), NOW()),
('blog_avatar', '', '站点头像', NOW(), NOW()),
('blog_signature', '以数字化，服务企业成长', '站长签名', NOW(), NOW()),
('blog_start_time', '2025-01-01', '站点创建时间', NOW(), NOW()),

-- 社交媒体设置
('author_title', '网站管理员', '站长头衔', NOW(), NOW()),
('author_description', '这个人很懒，什么都没留下', '站长描述', NOW(), NOW()),
('github_url', 'https://github.com/nevell', 'GitHub链接', NOW(), NOW()),
('weibo_url', '', '微博链接', NOW(), NOW()),
('zhihu_url', '', '知乎链接', NOW(), NOW()),
-- 后台「个人信息」页签里的四项（此前只存在于设置页，未进种子，全新库保存后才会生成）
('author_bio', '', '个人简介', NOW(), NOW()),
('author_location', '', '位置信息', NOW(), NOW()),
('personal_website', '', '个人网站', NOW(), NOW()),
('wechat_qr', '', '微信二维码', NOW(), NOW()),


-- 功能开关设置
('footer_enabled', 'true', '是否显示底部', NOW(), NOW()),
('beian_enabled', 'false', '是否显示页脚ICP备案信息（内容取 blog_beian，非空才显示）', NOW(), NOW()),
('copyright_enabled', 'true', '是否显示版权信息', NOW(), NOW()),
('comment_enabled', 'true', '是否开启评论功能', NOW(), NOW()),
('comment_review', 'true', '评论是否需要审核', NOW(), NOW()),
('friend_link_enabled', 'true', '前台是否展示友情链接', NOW(), NOW()),
('friend_link_apply_enabled', 'false', '前台是否展示友链申请入口（关闭后申请页提示关闭且接口拒绝提交）', NOW(), NOW()),
('like_enabled', 'true', '是否开启点赞功能', NOW(), NOW()),
('guestbook_enabled', 'false', '前台是否展示留言板（关闭后导航隐藏入口、页面提示关闭、留言接口拒绝提交）', NOW(), NOW()),
('view_count_enabled', 'true', '是否开启浏览统计', NOW(), NOW()),
('share_enabled', 'true', '是否开启分享功能', NOW(), NOW()),
('search_enabled', 'true', '是否开启搜索功能', NOW(), NOW()),
('sidebar_enabled', 'true', '是否显示侧边栏', NOW(), NOW()),

-- 企业官网模式：首页显示指定自定义页面（填 slug），留空则首页显示文章列表
('home_page_slug', '', '首页显示的自定义页面 slug（留空则首页显示文章列表）', NOW(), NOW()),
-- 顶部导航裁剪（面向企业官网时不展示分类/标签/归档）
('nav_category_enabled', 'true', '导航是否显示分类入口', NOW(), NOW()),
('nav_tag_enabled', 'true', '导航是否显示标签入口', NOW(), NOW()),
('nav_archive_enabled', 'true', '导航是否显示归档入口', NOW(), NOW()),

-- 界面样式设置
('theme_color', '#4f46e5', '主题颜色', NOW(), NOW()),
('header_background', '#ffffff', '头部背景色', NOW(), NOW()),
('sidebar_style', 'dark', '侧边栏样式', NOW(), NOW()),

-- 显示数量设置
('page_size', '10', '每页显示文章数量', NOW(), NOW()),
('hot_article_count', '5', '热门文章显示数量', NOW(), NOW()),
('recent_comment_count', '5', '最新评论显示数量', NOW(), NOW()),

-- 内容设置
('greeting_message', '欢迎光临，很高兴为您服务！', '欢迎信息', NOW(), NOW()),

-- SEO优化设置
('seo_title', '知企建站系统 - 企业官网建站平台', 'SEO标题', NOW(), NOW()),
('seo_description', '知企建站系统是面向企业的一站式官网建站平台，支持内容管理、产品与服务展示、SEO 优化与安全部署。', 'SEO描述', NOW(), NOW()),
('seo_canonical_url', '', '规范URL（留空则用当前访问域名）', NOW(), NOW()),
('seo_robots', 'index,follow', 'Robots规则', NOW(), NOW()),
('seo_favicon', '/favicon.ico', '网站图标', NOW(), NOW());

-- 品牌化迁移：清理历史种子中遗留的旧品牌关键词（仅当值仍为未修改的旧种子时生效，不影响用户自定义值）
UPDATE `zhi_setting`
SET `config_value` = '企业官网,建站系统,企业网站建设,数字化转型,Vue,Spring Boot'
WHERE `config_key` = 'blog_keywords'
  AND `config_value` = '企业官网,建站系统,RuoYi,Vue,Spring Boot,MySQL,前端开发,后端开发';

-- 防盗链/域名管理设置（默认关闭，开关与白名单可在后台“站点设置-站点信息”管理）
INSERT IGNORE INTO `zhi_setting` (`config_key`, `config_value`, `description`, `create_time`, `update_time`) VALUES
('referer_enabled', 'false', '防盗链开关（true/false）', NOW(), NOW()),
('referer_allowed_domains', 'localhost,127.0.0.1', '防盗链允许域名（逗号、分号或换行分隔）', NOW(), NOW());

-- 邮件通知开关（评论/回复/审核结果，需已配置邮件服务）
INSERT IGNORE INTO `zhi_setting` (`config_key`, `config_value`, `description`, `create_time`, `update_time`) VALUES
('email_notify_enabled', 'true', '邮件通知开关（评论/回复/审核结果）', NOW(), NOW());

-- 企业建站：核心业务栏目、企业首页模板与联系信息（已有库重跑自动补齐）
INSERT IGNORE INTO `zhi_setting` (`config_key`, `config_value`, `description`, `create_time`, `update_time`) VALUES
('business_label', '核心业务', '核心业务栏目名称（可改为：产品中心/服务项目/解决方案）', NOW(), NOW()),
('home_layout', '', '首页模板：留空=文章列表（默认）；business=企业官网区块化模板（home_page_slug 已设置时自定义页面优先）', NOW(), NOW()),
('site_template', 'consult', '企业官网行业模板：consult 管理咨询 / tech 信息技术 / mfg 制造业 / trade 商贸 / digital 数码产品', NOW(), NOW()),
('contact_phone', '', '联系电话（官网联系信息展示）', NOW(), NOW()),
('contact_address', '', '联系地址（官网联系信息展示）', NOW(), NOW());

-- 用户协议与隐私政策（注册页勾选与前台全文页面展示，后台「协议页面」可编辑；HTML 内不得出现单引号）
INSERT IGNORE INTO `zhi_setting` (`config_key`, `config_value`, `description`, `create_time`, `update_time`) VALUES
('user_agreement_content', '<p>更新日期：2026-10-04 · 生效日期：2026-10-04</p><h3>一、协议的接受</h3><p>本站是基于知企建站系统搭建的站点，由本站运营者（以下简称「本站」）运营。本《用户协议》（以下简称「本协议」）是您与本站之间关于注册、登录、使用本站账号及使用本站各项服务所订立的契约。</p><p>您在注册页面勾选「我已阅读并同意《用户协议》和《隐私政策》」并完成注册，或以其他方式实际使用本站服务，均表示您已充分阅读、理解并接受本协议的全部内容。若您不同意本协议的任何条款，请勿注册或使用本站服务。</p><h3>二、定义</h3><ul><li>「本站」：指本站点及其配套服务；</li><li>「用户」：指通过邮箱验证方式注册并获得本站账号的自然人；</li><li>「用户内容」：指用户在本站发表的评论、留言等内容。</li></ul><h3>三、账号注册与安全</h3><ul><li>您应当具备中华人民共和国法律规定的完全民事行为能力；若您为未成年人，应在监护人的指导下使用本站服务；</li><li>注册时您须提供真实、有效的邮箱地址，并通过邮箱验证码完成验证；昵称为选填项；</li><li>您应妥善保管账号密码，不得出借、出租、转让或与他人共用账号；通过您的账号进行的操作均视为您本人的行为；</li><li>如发现账号被盗用或存在其他安全问题，应立即通过本页提供的联系方式与本站取得联系。</li></ul><h3>四、用户行为规范</h3><p>您承诺在使用本站过程中，遵守《中华人民共和国宪法》《中华人民共和国网络安全法》《中华人民共和国个人信息保护法》等法律法规及公序良俗，不得利用本站制作、复制、发布、传播含有下列内容的信息：</p><ul><li>反对宪法确定的基本原则，危害国家安全、泄露国家秘密、颠覆国家政权、破坏国家统一的；</li><li>宣扬淫秽、色情、赌博、暴力、凶杀、恐怖或者教唆犯罪的；</li><li>侮辱、诽谤他人，侵害他人名誉权、肖像权、隐私权、著作权等合法权益的；</li><li>散布谣言、虚假信息、垃圾广告，或其他与所在页面无关的骚扰信息的；</li><li>其他法律、行政法规禁止的内容。</li></ul><p>您不得以任何技术手段干扰本站的正常运行，或实施恶意注册、刷量、爬取数据等危害站点安全的行为。对违反本条规范的内容与账号，本站有权不经通知采取删除、屏蔽、限制功能、封禁账号等措施，并保留追究法律责任的权利。</p><h3>五、用户内容与知识产权</h3><ul><li>本站的界面设计、程序代码、Logo 等要素的知识产权归本站运营者所有；</li><li>用户内容的著作权归您或原权利人所有；您同意在免费、非独占的范围内授予本站存储、展示、传播用户内容的许可，以实现发表、展示等本站基本功能；</li><li>您承诺用户内容系您原创或已获得合法授权，因用户内容引发的一切纠纷由您自行承担相应责任；</li><li>未经权利人许可，任何一方不得将用户内容用于商业目的。</li></ul><h3>六、内容审核与通知</h3><p>本站开启评论审核功能时，您提交的评论将先经审核，审核通过后方公开展示，审核结果通过站内消息告知您；未通过审核的评论不会公开展示。</p><p>为及时告知评论回复、审核结果等信息，本站会通过站内消息中心向您推送通知；在邮件通知开启时，亦可能向您注册时填写的邮箱发送通知邮件。</p><h3>七、服务的变更、中断与终止</h3><ul><li>本站有权基于运营需要，变更、中断或终止部分或全部服务，并将尽可能提前公告；</li><li>因系统维护、升级、故障或不可抗力导致服务中断的，本站将尽力减少影响，但不承担由此产生的赔偿责任（法律另有规定除外）；</li><li>您可以随时停止使用本站服务；如需注销账号，可通过本页提供的联系方式向本站提出申请。</li></ul><h3>八、免责声明与责任限制</h3><p>本站仅提供信息存储与展示服务，除开启审核功能的内容外，不对用户内容进行事先审查，亦不对用户内容的真实性、完整性、合法性作出保证。对您因使用本站服务所产生的直接或间接损失，在法律允许的最大范围内，本站不承担责任。本站页面包含的指向第三方网站的外部链接，其内容与本站无关。</p><h3>九、法律适用与争议解决</h3><p>本协议的订立、履行与解释适用中华人民共和国法律。因本协议产生的争议，双方应友好协商解决；协商不成的，任何一方可向本站运营者所在地有管辖权的人民法院提起诉讼。</p><h3>十、协议的修订与联系我们</h3><p>本站可能适时修订本协议，修订后的协议将在本页面发布；若您在协议修订后继续使用本站服务，视为接受修订后的协议。</p><p>如您对本协议有任何疑问、意见或建议，可通过「关于」页面提供的联系方式与本站取得联系。</p>', '用户协议内容', NOW(), NOW()),
('privacy_policy_content', '<p>更新日期：2026-10-04 · 生效日期：2026-10-04</p><h3>一、引言</h3><p>本站（基于知企建站系统搭建的站点，以下简称「本站」）深知个人信息对您的重要性。我们将按照《中华人民共和国网络安全法》《中华人民共和国个人信息保护法》等法律法规的要求，以最小必要为原则处理您的个人信息，并采取相应的安全保护措施。本《隐私政策》帮助您了解本站收集哪些信息、如何使用与保护这些信息，以及您享有哪些权利。</p><p>本政策与《用户协议》共同构成您与本站之间的约定。您注册或使用本站服务，即表示您已阅读并同意本政策。</p><h3>二、我们收集的信息</h3><p>本站遵循合法、正当、必要的原则，收集以下信息：</p><ul><li>注册信息：您注册时提供的用户名、昵称（选填）与邮箱地址；密码经单向加密（BCrypt）后存储，本站任何人员均无法获知其明文；</li><li>验证信息：注册、找回密码过程中用于校验身份的邮箱验证码发送记录与图形验证码会话；</li><li>互动信息：您发表的评论、留言，以及点赞、收藏等互动行为数据；</li><li>使用信息：文章浏览量等统计数据，以及为保障站点安全与排查故障而记录的请求日志（如 IP 地址、浏览器类型 User-Agent、访问时间）。</li></ul><h3>三、信息的使用</h3><p>我们将上述信息用于以下目的：</p><ul><li>创建与管理您的账号，完成登录与身份验证；</li><li>展示您发表的评论及昵称、头像等公开互动信息；</li><li>向您推送评论回复、审核结果等站内通知或邮件通知；</li><li>统计文章浏览量等站点运营数据，用于改进内容与服务；</li><li>识别并防范恶意注册、垃圾评论、恶意攻击等危害站点安全的行为。</li></ul><p>本站不会将您的个人信息用于上述目的之外的用途；如确需超出上述范围使用，将再次征得您的同意。</p><h3>四、Cookie 与本地存储</h3><p>您登录后，本站会将登录凭证（Token）保存在您浏览器的本地存储（LocalStorage）中，用于维持登录状态；图形验证码的会话信息将临时保存在服务端。本站不接入任何第三方广告、统计或追踪服务，不会将上述技术用于广告追踪。</p><h3>五、信息的共享与披露</h3><p>本站不会向任何第三方出售您的个人信息，仅在以下情形下发生必要的信息流动：</p><ul><li>邮件服务：验证码与通知邮件需借助第三方邮件服务商发送，该过程仅涉及您的邮箱地址；</li><li>法律要求：根据法律法规规定，或司法机关、行政机关的强制性要求，在必要范围内提供；</li><li>您的同意：经您明确授权的其他情形。</li></ul><h3>六、信息的存储与保护</h3><p>您的信息存储于本站数据库中，存储期限为实现本政策所述目的所必需的最短时间，超出期限后我们将予以删除或匿名化处理。本站采取密码单向加密存储、后台接口登录鉴权、最小权限访问控制等安全措施；建议您通过 HTTPS 访问本站以加密传输过程。</p><p>如不幸发生个人信息安全事件，本站将按照法律法规的要求及时告知您，并采取合理的补救措施。</p><h3>七、您的权利</h3><ul><li>访问与更正：您可随时在「个人中心」查看、修改昵称、头像、邮箱等个人信息；</li><li>删除：您可自行删除自己发表的评论、收藏等内容；</li><li>账号注销：如需注销账号或清空全部个人信息，可通过「关于」页面提供的联系方式联系站长处理；</li><li>撤回同意：您可以选择不再使用本站服务，并要求本站删除相关个人信息。</li></ul><h3>八、未成年人保护</h3><p>本站面向一般用户提供服务。若您是未满 14 周岁的未成年人，请在监护人的陪同与同意下使用本站服务，并由监护人帮助您完成必要的个人信息提供与授权。</p><h3>九、政策的修订</h3><p>本站可能适时修订本政策，修订后的政策将在本页面发布并更新顶部「更新日期」。若变更会实质影响您的权益，本站将以显著方式提示您；若您在政策修订后继续使用本站服务，视为接受修订后的政策。</p><h3>十、联系我们</h3><p>如您对本政策有任何疑问、意见或建议，可通过「关于」页面提供的联系方式与本站取得联系，我们将在收到反馈后尽快答复。</p>', '隐私政策内容', NOW(), NOW());

-- 插入完整的站点分类数据（包含层级结构）
-- 幂等保护 1：清理历史重跑产生的重复分类（同 alias+name 保留最小 id，且只删没有被文章引用的重复行）
DELETE c1 FROM `zhi_category` c1
JOIN `zhi_category` c2
  ON c1.`alias` = c2.`alias` AND c1.`name` = c2.`name` AND c1.`id` > c2.`id`
LEFT JOIN `zhi_article` a ON a.`category_id` = c1.`id`
WHERE a.`id` IS NULL;
-- 幂等保护 2：种子数据显式指定主键 id（与 zhi_tag 一致），
-- 这样 INSERT IGNORE 在重跑时会因主键冲突而被忽略，不会重复插入
INSERT IGNORE INTO `zhi_category` (`id`, `name`, `alias`, `description`, `parent_id`, `sort_order`, `sort`, `status`) VALUES
-- 一级分类
(1, '公司动态', 'company', '公司新闻、活动与公告', 0, 1, 1, 1),
(2, '行业资讯', 'industry', '行业动态与趋势洞察', 0, 2, 2, 1),
(3, '产品知识', 'knowledge', '产品使用技巧与知识分享', 0, 4, 4, 1),
(4, '技术支持', 'support', '技术文章与常见问题解答', 0, 5, 5, 1),

-- 二级分类（公司动态下的子分类）
(5, '新闻发布', 'press', '公司新闻稿与媒体报道', 1, 1, 1, 1),
(6, '活动纪要', 'events', '公司活动与展会纪实', 1, 2, 2, 1),
(7, '产品发布', 'release', '新产品与服务发布信息', 1, 3, 3, 1),
(8, '团队风采', 'team', '团队建设与企业文化', 1, 4, 4, 1),

-- 二级分类（行业资讯下的子分类）
(9, '政策解读', 'policy', '行业政策法规解读', 2, 1, 1, 1),
(10, '市场洞察', 'market', '市场趋势与分析报告', 2, 2, 2, 1),
(11, '展会活动', 'expo', '行业展会与交流活动', 2, 3, 3, 1);

-- 插入完整的站点标签数据（18个常用标签）
INSERT IGNORE INTO `zhi_tag` (`id`, `name`, `description`, `color`, `icon`, `article_count`) VALUES
-- 核心标签
(1, '公司动态', '公司新闻与活动相关文章', '#4f46e5', 'el-icon-menu', 0),
(2, '产品发布', '新产品与服务发布相关文章', '#0ea5e9', 'el-icon-box', 0),
(3, '行业洞察', '行业趋势与市场分析相关文章', '#8b5cf6', 'el-icon-link', 0),
(4, '客户案例', '客户合作与成功案例相关文章', '#10b981', 'el-icon-folder-opened', 0),
(5, '数字化转型', '企业数字化转型相关文章', '#f59e0b', 'el-icon-connection', 0),
(6, '企业官网', '企业官网建设相关文章', '#6366f1', 'el-icon-monitor', 0),

-- 技术领域
(7, '云计算', '云计算技术与实践相关文章', '#3b82f6', 'el-icon-cloudy', 0),
(8, '人工智能', '人工智能技术应用相关文章', '#ec4899', 'el-icon-cpu', 0),
(9, '大数据', '大数据技术与应用相关文章', '#14b8a6', 'el-icon-database', 0),
(10, '网络安全', '网络与信息安全相关文章', '#ef4444', 'el-icon-document', 0),

-- 行业领域
(11, '智能制造', '智能制造与工业互联网相关文章', '#64748b', 'el-icon-setting', 0),
(12, '物联网', '物联网技术与应用相关文章', '#06b6d4', 'el-icon-connection', 0),
(13, '绿色低碳', '绿色低碳与可持续发展相关文章', '#22c55e', 'el-icon-coin', 0),

-- 企业经营
(14, '企业管理', '企业管理与运营相关文章', '#a855f7', 'el-icon-server', 0),
(15, '市场营销', '市场营销与品牌推广相关文章', '#f97316', 'el-icon-brush', 0),
(16, '技术服务', '技术服务与支持相关文章', '#0d9488', 'el-icon-node', 0),
(17, '团队文化', '团队建设与企业文化相关文章', '#d946ef', 'el-icon-menu', 0),
(18, '招聘信息', '招聘岗位与人才招募相关文章', '#eab308', 'el-icon-document', 0);

-- 插入完整的站点文章示例数据
-- 幂等保护：仅在尚无示例文章时插入，避免重跑脚本导致重复
DROP PROCEDURE IF EXISTS sp_insert_sample_articles;
DELIMITER $$
CREATE PROCEDURE sp_insert_sample_articles()
BEGIN
  IF (SELECT COUNT(*) FROM `zhi_article` WHERE `title` = '知企建站系统 v1.0 正式发布：一站式企业官网搭建平台') = 0 THEN
INSERT IGNORE INTO `zhi_article` (`title`, `summary`, `content`, `cover_url`, `category_id`, `author_id`, `author_name`, `is_top`, `is_recommend`, `status`, `view_count`, `like_count`, `article_type`, `slug`) VALUES
-- 置顶推荐文章
('知企建站系统 v1.0 正式发布：一站式企业官网搭建平台', '知企建站系统 v1.0 正式发布，提供内容管理、产品与服务展示、SEO 优化、功能开关与安全部署等一站式企业官网搭建能力。',
'<h1>知企建站系统 v1.0 正式发布</h1>
<h2>🎉 版本亮点</h2>
<ul>
<li><strong>内容管理</strong> - 富文本与 Markdown 双格式编辑，支持定时发布与草稿箱</li>
<li><strong>企业官网模式</strong> - 首页可由自定义页面接管，导航按需裁剪</li>
<li><strong>SEO 优化</strong> - 站点级与页面级 TDK、sitemap、robots 一应俱全</li>
<li><strong>安全可靠</strong> - JWT 认证、接口限流、防盗链与依赖漏洞扫描</li>
</ul>
<h2>🧭 适用场景</h2>
<p>无论是产品型企业展示产品中心，还是服务型企业介绍业务与客户案例，都可以通过自定义页面与内容管理快速搭建出专业的企业官网。</p>
<h2>🚀 快速开始</h2>
<p>使用 Docker Compose 即可一键部署，配合后台「站点设置」完成站点信息、导航与功能开关的配置，几分钟内即可上线您的企业官网。</p>
<p>感谢一路支持我们的用户，欢迎通过「关于」页面提出宝贵意见。</p>',
'', 7, 1, 'admin', 1, 1, 1, 156, 23, 'business', 'zhi-v1-release'),

('2026 企业数字化转型趋势观察', '从官网体验、内容运营到数据驱动，梳理 2026 年企业数字化转型中最值得关注的几个趋势。',
'<h1>2026 企业数字化转型趋势观察</h1>
<h2>📈 趋势一：官网重新成为品牌门户</h2>
<p>随着获客成本上升，越来越多企业重新重视自有官网的建设与运营，把官网作为品牌展示与内容沉淀的主阵地。</p>
<h2>🤖 趋势二：AI 辅助内容生产进入日常</h2>
<p>AI 工具正在深入内容创作流程，帮助企业以更低的成本保持官网内容的持续更新。</p>
<h2>🔐 趋势三：安全与合规成为底线</h2>
<p>个人信息保护与数据安全法规日益完善，企业在数字化转型中需要把安全合规作为基础设施来投入。</p>
<h2>💡 小结</h2>
<p>数字化转型不是一次性项目，而是持续的运营过程。选择一套可长期演进、自主可控的建站系统，是迈向数字化的第一步。</p>',
'', 10, 1, 'admin', 0, 1, 1, 89, 15, 'news', 'digital-2026-trends'),

-- 客户案例
('客户案例：某制造企业官网升级实践', '分享某制造企业基于知企建站系统完成官网升级的实践：从内容梳理、页面搭建到上线运营的全过程。',
'<h1>客户案例：某制造企业官网升级实践</h1>
<h2>🏭 项目背景</h2>
<p>该企业原有的官网多年未更新，信息架构混乱、移动端体验不佳，难以支撑品牌展示与市场活动的要求。</p>
<h2>🛠️ 实施过程</h2>
<ol>
<li><strong>内容梳理</strong> - 梳理产品线与服务体系，建立清晰的栏目结构</li>
<li><strong>页面搭建</strong> - 使用自定义页面搭建首页与核心栏目，按需裁剪导航</li>
<li><strong>内容运营</strong> - 通过文章管理定期发布企业动态与行业资讯</li>
<li><strong>SEO 优化</strong> - 配置站点 TDK 与 sitemap，上线后搜索收录明显改善</li>
</ol>
<h2>✅ 上线效果</h2>
<p>新官网上线后，页面平均停留时长与访问深度显著提升，市场部可以自主完成内容更新，不再依赖外部开发资源。</p>',
'', 1, 1, 'admin', 0, 0, 1, 67, 12, 'case', 'manufacturing-website-case'),

('企业官网 SEO 优化实用指南', '从站点标题、描述关键词到 sitemap 与 robots，介绍企业官网 SEO 优化的实用配置要点。',
'<h1>企业官网 SEO 优化实用指南</h1>
<h2>🔤 基础配置：TDK</h2>
<p>在后台「站点设置 - SEO优化」中配置站点标题、描述与关键词，这是搜索引擎理解网站的第一入口。</p>
<h2>📄 页面级优化</h2>
<ul>
<li>为每个自定义页面配置独立的 SEO 标题、关键词与描述</li>
<li>标题体现核心业务，避免全站使用同一套关键词</li>
<li>定期更新企业动态与行业资讯，保持站点活跃度</li>
</ul>
<h2>🗺️ 收录加速</h2>
<p>系统内置 sitemap.xml 与 robots.txt，会随内容发布自动更新；将站点地址提交到搜索引擎站长平台，可以进一步加快收录。</p>
<h2>📊 效果追踪</h2>
<p>结合后台的访问统计与文章统计功能，持续观察哪些内容带来更多访问，逐步优化内容策略。</p>',
'', 4, 1, 'admin', 0, 0, 1, 45, 8, 'news', 'enterprise-seo-guide'),

('站点运维托管服务正式上线', '面向没有专职技术团队的中小企业，提供站点备份、监控告警与版本升级的运维托管服务。',
'<h1>站点运维托管服务正式上线</h1>
<h2>🧰 服务内容</h2>
<ul>
<li><strong>数据备份</strong> - 数据库与上传文件每日自动备份，支持异地容灾</li>
<li><strong>监控告警</strong> - 服务可用性、磁盘与内存指标异常时及时告警</li>
<li><strong>版本升级</strong> - 系统版本升级与安全补丁统一安排，免维护上线</li>
</ul>
<h2>👥 适合谁</h2>
<p>没有专职运维人员的中小企业：把站点交给我们，团队专注于业务内容的运营。</p>
<h2>📮 申请方式</h2>
<p>通过官网「关于」页面的联系方式与我们取得联系，我们会安排专人对接评估与实施。</p>',
'', 7, 1, 'admin', 0, 1, 1, 78, 11, 'business', 'ops-hosting-service'),

('加入我们：与知企一起成长', '知企团队诚邀技术、设计与市场伙伴加入，一起把企业建站这件事做到极致。',
'<h1>加入我们：与知企一起成长</h1>
<h2>🌟 我们在做什么</h2>
<p>知企建站系统致力于让每一家企业都能轻松拥有专业、安全、可持续运营的官网。我们相信，好的工具应该让内容创作者专注内容本身。</p>
<h2>💼 热招岗位</h2>
<ul>
<li><strong>Java 开发工程师</strong> - 负责 Spring Boot 后端服务的设计与开发</li>
<li><strong>前端开发工程师</strong> - 负责 Vue 3 前端站点与管理后台的开发</li>
<li><strong>市场运营专员</strong> - 负责官网内容运营与市场活动策划</li>
</ul>
<h2>🤝 我们期待</h2>
<p>热爱技术、乐于分享、对产品体验有追求的你。有意向请通过「关于」页面的联系方式与我们取得联系。</p>',
'', 8, 1, 'admin', 0, 0, 1, 92, 16, 'news', 'join-zhiqi');
  END IF;
END$$
DELIMITER ;
CALL sp_insert_sample_articles();
DROP PROCEDURE IF EXISTS sp_insert_sample_articles;

-- 插入文章标签关联数据（建立文章与标签的多对多关系）
INSERT IGNORE INTO `zhi_article_tag` (`article_id`, `tag_id`) VALUES
-- 文章1：知企建站系统 v1.0 正式发布（公司动态 / 产品发布 / 企业官网）
(1, 1), (1, 2), (1, 6),

-- 文章2：2026 企业数字化转型趋势观察（行业洞察 / 数字化转型 / 人工智能）
(2, 3), (2, 5), (2, 8),

-- 文章3：客户案例：某制造企业官网升级实践（客户案例 / 数字化转型 / 智能制造）
(3, 4), (3, 5), (3, 11),

-- 文章4：企业官网 SEO 优化实用指南（企业官网 / 市场营销）
(4, 6), (4, 15),

-- 文章5：站点运维托管服务正式上线（产品发布 / 技术服务 / 云计算）
(5, 2), (5, 16), (5, 7),

-- 文章6：加入我们：与知企一起成长（团队文化 / 招聘信息）
(6, 17), (6, 18);

-- ========== 更新分类文章数量 ==========

-- 禁用触发器以避免冲突
SET @DISABLE_TRIGGERS = TRUE;

-- 批量更新所有分类的文章数量
UPDATE zhi_category c
SET c.article_count = (
    SELECT COUNT(*)
    FROM zhi_article a
    WHERE a.category_id = c.id
    AND a.del_flag = '0'
    AND a.status = 1
),
c.update_time = NOW()
WHERE c.del_flag = '0';

-- 重新启用触发器
SET @DISABLE_TRIGGERS = NULL;

-- ========== 验证分类文章数量统计 ==========

SELECT '📊 验证分类文章数量统计：' AS info;
SELECT
    c.id,
    c.name,
    c.status,
    c.del_flag,
    IFNULL(COUNT(a.id), 0) as actual_article_count,
    c.article_count as cached_article_count,
    CASE
        WHEN IFNULL(COUNT(a.id), 0) != c.article_count THEN '❌ 不匹配'
        ELSE '✅ 匹配'
    END as status
FROM zhi_category c
LEFT JOIN zhi_article a ON c.id = a.category_id AND a.del_flag = '0' AND a.status = 1
WHERE c.del_flag = '0'
GROUP BY c.id, c.name, c.status, c.del_flag, c.article_count
ORDER BY c.sort_order ASC;

-- 插入完整的友情链接数据
-- 幂等保护 1：清理历史重跑产生的重复友链（同 url+name 保留最小 id）
DELETE f1 FROM `zhi_friend_link` f1
JOIN `zhi_friend_link` f2
  ON f1.`url` = f2.`url` AND f1.`name` = f2.`name` AND f1.`id` > f2.`id`;
-- 幂等保护 2：种子数据显式指定主键 id，重跑时 INSERT IGNORE 因主键冲突被忽略
INSERT IGNORE INTO `zhi_friend_link` (`id`, `name`, `url`, `logo`, `description`, `status`, `sort`) VALUES
(1, 'Spring 官网', 'https://spring.io/', 'https://spring.io/images/spring-logo.png', '后端核心框架 Spring 官方网站', 0, 1),
(2, 'Vue.js 官网', 'https://vuejs.org/', 'https://vuejs.org/logo.svg', '前端框架 Vue.js 官方网站', 1, 2),
(3, 'Element Plus', 'https://element-plus.org/', 'https://element-plus.org/images/element-plus-logo.svg', 'Vue 3 桌面端 UI 组件库官方网站', 1, 3),
(4, 'MySQL', 'https://www.mysql.com/', 'https://www.mysql.com/common/logos/logo-mysql-170x115.png', '关系型数据库 MySQL 官方网站', 1, 4),
(5, 'Redis', 'https://redis.io/', 'https://redis.io/images/redis-white.svg', '内存数据库 Redis 官方网站', 0, 5),
(6, 'GitHub', 'https://github.com/', 'https://github.com/github.png', '全球最大的代码托管平台', 0, 6);

-- ========== 配置站点管理菜单 ==========

-- 站点管理主菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2000, '站点管理', 0, 1, 'admin', NULL, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', NOW(), '', NULL, '站点管理目录');

-- 存量升级：一级菜单路径去 blog（admin/blog → admin），幂等，可重复执行
UPDATE sys_menu SET path = 'admin' WHERE menu_id = 2000 AND path = 'admin/blog';

-- 存量升级：菜单组件路径去 blog 前缀（blog/article/index → article/index），
-- 与前端视图目录 views/admin/blog/* → views/admin/* 同步；幂等，可重复执行
UPDATE sys_menu SET component = SUBSTRING(component, 6) WHERE component LIKE 'blog/%';

-- 存量升级：菜单权限串去 blog 前缀（blog:* → zhi:*），幂等，可重复执行。
-- 注意：已登录用户的权限缓存在 Redis 里，升级后需重新登录才会生效。
UPDATE sys_menu SET perms = REPLACE(perms, 'blog:', 'zhi:') WHERE perms LIKE 'blog:%';

-- 文章管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2001, '文章管理', 2000, 1, 'article', 'article/index', '', '', 1, 0, 'C', '0', '0', 'zhi:article:list', 'edit', 'admin', NOW(), '', NULL, '文章管理菜单');

-- 分类管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2002, '分类管理', 2000, 2, 'category', 'category/index', '', '', 1, 0, 'C', '0', '0', 'zhi:category:list', 'list', 'admin', NOW(), '', NULL, '分类管理菜单');

-- 标签管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2003, '标签管理', 2000, 3, 'tag', 'tag/index', '', '', 1, 0, 'C', '0', '0', 'zhi:tag:list', 'tab', 'admin', NOW(), '', NULL, '标签管理菜单');

-- 评论管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2004, '评论管理', 2000, 4, 'comment', 'comment/index', '', '', 1, 0, 'C', '0', '0', 'zhi:comment:list', 'message', 'admin', NOW(), '', NULL, '评论管理菜单');

-- 友链管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2006, '友链管理', 2000, 5, 'friendLink', 'friendLink/index', '', '', 1, 0, 'C', '0', '0', 'zhi:friendLink:list', 'link', 'admin', NOW(), '', NULL, '友链管理菜单');

-- 媒体管理（上传记录）
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2007, '媒体管理', 2000, 6, 'media', 'media/index', '', '', 1, 0, 'C', '0', '0', 'zhi:media:list', 'component', 'admin', NOW(), '', NULL, '媒体管理菜单');
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20080, '媒体删除', 2007, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:media:remove', '#', 'admin', NOW(), '', NULL, '');
-- 留言管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(2008, '留言管理', 2000, 7, 'message', 'message/index', '', '', 1, 0, 'C', '0', '0', 'zhi:message:list', 'message', 'admin', NOW(), '', NULL, '留言管理菜单');

-- 留言管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20090, '留言查询', 2008, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:message:query', '#', 'admin', NOW(), '', NULL, ''),
(20091, '留言审核', 2008, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:message:edit', '#', 'admin', NOW(), '', NULL, ''),
(20092, '留言回复', 2008, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:message:reply', '#', 'admin', NOW(), '', NULL, ''),
(20093, '留言删除', 2008, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:message:remove', '#', 'admin', NOW(), '', NULL, ''),
(20094, '留言导出', 2008, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:message:export', '#', 'admin', NOW(), '', NULL, '');

-- 自定义页面管理
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(2009, '页面管理', 2000, 8, 'page', 'page/index', '', '', 1, 0, 'C', '0', '0', 'zhi:page:list', 'documentation', 'admin', NOW(), '', NULL, '自定义页面管理菜单');

-- 自定义页面按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20100, '页面查询', 2009, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:page:query', '#', 'admin', NOW(), '', NULL, ''),
(20101, '页面新增', 2009, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:page:add', '#', 'admin', NOW(), '', NULL, ''),
(20102, '页面修改', 2009, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:page:edit', '#', 'admin', NOW(), '', NULL, ''),
(20103, '页面删除', 2009, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:page:remove', '#', 'admin', NOW(), '', NULL, ''),
(20104, '页面导出', 2009, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:page:export', '#', 'admin', NOW(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 2007), (1, 20080);

-- 站点设置
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2005, '站点设置', 2000, 7, 'setting', 'setting/index', '', '', 1, 0, 'C', '0', '0', 'zhi:setting:list', 'swagger', 'admin', NOW(), '', NULL, '站点设置菜单');

-- 为管理员角色分配站点管理菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 2000), (1, 2001), (1, 2002), (1, 2003), (1, 2004), (1, 2006), (1, 2005);

-- 核心业务管理（同一文章实体按 article_type=business 过滤，复用文章管理组件与权限）
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2010, '核心业务管理', 2000, 2, 'business', 'article/index', '', '', 1, 0, 'C', '0', '0', 'zhi:article:list', 'shopping', 'admin', NOW(), '', NULL, '核心业务管理菜单（article_type=business 的文章）');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 2010), (2, 2010);


-- ========== 配置站点管理按钮权限 ==========

-- 文章管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20010, '文章查询', 2001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:article:query', '#', 'admin', NOW(), '', NULL, ''),
(20011, '文章新增', 2001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:article:add', '#', 'admin', NOW(), '', NULL, ''),
(20012, '文章修改', 2001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:article:edit', '#', 'admin', NOW(), '', NULL, ''),
(20013, '文章删除', 2001, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:article:remove', '#', 'admin', NOW(), '', NULL, ''),
(20014, '文章导出', 2001, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:article:export', '#', 'admin', NOW(), '', NULL, ''),
(20015, '文章标签查询', 2001, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:articleTag:list', '#', 'admin', NOW(), '', NULL, ''),
(20016, '文章标签详情', 2001, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:articleTag:query', '#', 'admin', NOW(), '', NULL, ''),
(20017, '文章标签新增', 2001, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:articleTag:add', '#', 'admin', NOW(), '', NULL, ''),
(20018, '文章标签修改', 2001, 9, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:articleTag:edit', '#', 'admin', NOW(), '', NULL, ''),
(20019, '文章标签删除', 2001, 10, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:articleTag:remove', '#', 'admin', NOW(), '', NULL, '');

-- 分类管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20020, '分类查询', 2002, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:category:query', '#', 'admin', NOW(), '', NULL, ''),
(20021, '分类新增', 2002, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:category:add', '#', 'admin', NOW(), '', NULL, ''),
(20022, '分类修改', 2002, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:category:edit', '#', 'admin', NOW(), '', NULL, ''),
(20023, '分类删除', 2002, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:category:remove', '#', 'admin', NOW(), '', NULL, ''),
(20024, '分类导出', 2002, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:category:export', '#', 'admin', NOW(), '', NULL, '');

-- 标签管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20030, '标签查询', 2003, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:tag:query', '#', 'admin', NOW(), '', NULL, ''),
(20031, '标签新增', 2003, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:tag:add', '#', 'admin', NOW(), '', NULL, ''),
(20032, '标签修改', 2003, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:tag:edit', '#', 'admin', NOW(), '', NULL, ''),
(20033, '标签删除', 2003, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:tag:remove', '#', 'admin', NOW(), '', NULL, ''),
(20034, '标签导出', 2003, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:tag:export', '#', 'admin', NOW(), '', NULL, '');

-- 评论管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20040, '评论查询', 2004, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:comment:query', '#', 'admin', NOW(), '', NULL, ''),
(20041, '评论审核', 2004, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:comment:audit', '#', 'admin', NOW(), '', NULL, ''),
(20042, '评论删除', 2004, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:comment:remove', '#', 'admin', NOW(), '', NULL, ''),
(20043, '评论驳回', 2004, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:comment:reject', '#', 'admin', NOW(), '', NULL, ''),
(20044, '评论导出', 2004, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:comment:export', '#', 'admin', NOW(), '', NULL, ''),
(20045, '评论新增', 2004, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:comment:add', '#', 'admin', NOW(), '', NULL, ''),
(20046, '评论修改', 2004, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:comment:edit', '#', 'admin', NOW(), '', NULL, '');

-- 友链管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20060, '友链查询', 2006, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:friendLink:query', '#', 'admin', NOW(), '', NULL, ''),
(20061, '友链新增', 2006, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:friendLink:add', '#', 'admin', NOW(), '', NULL, ''),
(20062, '友链修改', 2006, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:friendLink:edit', '#', 'admin', NOW(), '', NULL, ''),
(20063, '友链删除', 2006, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:friendLink:remove', '#', 'admin', NOW(), '', NULL, ''),
(20064, '友链导出', 2006, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:friendLink:export', '#', 'admin', NOW(), '', NULL, '');

-- 站点设置按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(20050, '设置查询', 2005, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:setting:query', '#', 'admin', NOW(), '', NULL, ''),
(20051, '设置修改', 2005, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:setting:edit', '#', 'admin', NOW(), '', NULL, ''),
(20052, '设置新增', 2005, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:setting:add', '#', 'admin', NOW(), '', NULL, ''),
(20053, '设置删除', 2005, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'zhi:setting:remove', '#', 'admin', NOW(), '', NULL, '');

-- 为管理员角色分配所有站点管理按钮权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 20010), (1, 20011), (1, 20012), (1, 20013), (1, 20014),
(1, 20015), (1, 20016), (1, 20017), (1, 20018), (1, 20019),
(1, 20020), (1, 20021), (1, 20022), (1, 20023),
(1, 20024),
(1, 20030), (1, 20031), (1, 20032), (1, 20033), (1, 20034),
(1, 20040), (1, 20041), (1, 20042), (1, 20043), (1, 20044), (1, 20045), (1, 20046),
(1, 20060), (1, 20061), (1, 20062), (1, 20063), (1, 20064),
(1, 20050), (1, 20051), (1, 20052), (1, 20053),
(1, 2008), (1, 20090), (1, 20091), (1, 20092), (1, 20093), (1, 20094),
(1, 2009), (1, 20100), (1, 20101), (1, 20102), (1, 20103), (1, 20104);

-- 为普通角色(role_id=2)分配站点管理菜单及按钮权限（内容管理员）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
-- 站点管理主菜单和文章管理
(2, 2000), (2, 2001),
-- 文章管理按钮权限（查询、新增、修改、删除、导出）
(2, 20010), (2, 20011), (2, 20012), (2, 20013), (2, 20014),
-- 文章标签关联按钮权限
(2, 20015), (2, 20016), (2, 20017), (2, 20018), (2, 20019),
-- 分类管理
(2, 2002),
-- 分类管理按钮权限（查询、新增、修改、删除、导出）
(2, 20020), (2, 20021), (2, 20022), (2, 20023), (2, 20024),
-- 标签管理
(2, 2003),
-- 标签管理按钮权限
(2, 20030), (2, 20031), (2, 20032), (2, 20033), (2, 20034),
-- 评论管理
(2, 2004),
-- 评论管理按钮权限（查询、审核、删除、驳回、导出）
(2, 20040), (2, 20041), (2, 20042), (2, 20043), (2, 20044),
-- 站点设置
(2, 2005),
-- 站点设置按钮权限（查询、修改）
(2, 20050), (2, 20051),
-- 友链管理
(2, 2006),
-- 友链管理按钮权限（查询、新增、修改、删除、导出）
(2, 20060), (2, 20061), (2, 20062), (2, 20063), (2, 20064),
-- 媒体管理
 (2, 2007),
-- 媒体管理按钮权限（删除）
 (2, 20080),
-- 留言管理（审核/回复/删除）
 (2, 2008), (2, 20090), (2, 20091), (2, 20092), (2, 20093),
-- 自定义页面（查询/新增/修改/删除）
 (2, 2009), (2, 20100), (2, 20101), (2, 20102), (2, 20103);

-- 注册用户角色(role_id=3)不分配任何后台菜单，仅通过个人中心（/user/profile）管理个人资料
-- 历史库迁移（幂等）：清理 role_id=3 已分配的后台菜单权限
DELETE FROM sys_role_menu WHERE role_id = 3 AND menu_id IN (2000, 2001, 2002, 2003, 2004, 2005, 2006,
  20010, 20011, 20012, 20013, 20014, 20015, 20016, 20017, 20018, 20019,
  20020, 20021, 20022, 20023, 20024, 20030, 20031, 20032, 20033, 20034,
  20040, 20041, 20042, 20043, 20044, 20045, 20046);

-- 历史库迁移（幂等）：评论审核权限串统一为 zhi:comment:audit（旧值为 approve）
UPDATE sys_menu SET perms = 'zhi:comment:audit' WHERE menu_id = 20041 AND perms = 'zhi:comment:approve';

-- ========== 配置系统管理菜单 ==========

-- 1、用户管理菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (100, '用户管理', 1, 1, 'user', 'system/user/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', NOW(), '', NULL, '用户管理菜单');

-- 用户管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1001, '用户查询', 100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', NOW(), '', NULL, ''),
(1002, '用户新增', 100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', NOW(), '', NULL, ''),
(1003, '用户修改', 100, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', NOW(), '', NULL, ''),
(1004, '用户删除', 100, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', NOW(), '', NULL, ''),
(1005, '用户导出', 100, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', NOW(), '', NULL, ''),
(1006, '用户导入', 100, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', NOW(), '', NULL, ''),
(1007, '重置密码', 100, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', NOW(), '', NULL, '');

-- 2、角色管理菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (101, '角色管理', 1, 2, 'role', 'system/role/role/index', '', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', NOW(), '', NULL, '角色管理菜单');

-- 角色管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1011, '角色查询', 101, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', NOW(), '', NULL, ''),
(1012, '角色新增', 101, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', NOW(), '', NULL, ''),
(1013, '角色修改', 101, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', NOW(), '', NULL, ''),
(1014, '角色删除', 101, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', NOW(), '', NULL, ''),
(1015, '角色导出', 101, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', NOW(), '', NULL, '');

-- 3、菜单管理菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/menu/index', '', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', NOW(), '', NULL, '菜单管理菜单');

-- 菜单管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1021, '菜单查询', 102, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', NOW(), '', NULL, ''),
(1022, '菜单新增', 102, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', NOW(), '', NULL, ''),
(1023, '菜单修改', 102, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', NOW(), '', NULL, ''),
(1024, '菜单删除', 102, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', NOW(), '', NULL, '');

-- 4、部门管理菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/dept/index', '', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', NOW(), '', NULL, '部门管理菜单');

-- 部门管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1031, '部门查询', 103, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', NOW(), '', NULL, ''),
(1032, '部门新增', 103, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', NOW(), '', NULL, ''),
(1033, '部门修改', 103, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', NOW(), '', NULL, ''),
(1034, '部门删除', 103, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', NOW(), '', NULL, '');

-- 5、岗位管理菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/post/index', '', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', NOW(), '', NULL, '岗位管理菜单');

-- 岗位管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1041, '岗位查询', 104, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', NOW(), '', NULL, ''),
(1042, '岗位新增', 104, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', NOW(), '', NULL, ''),
(1043, '岗位修改', 104, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', NOW(), '', NULL, ''),
(1044, '岗位删除', 104, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', NOW(), '', NULL, ''),
(1045, '岗位导出', 104, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', NOW(), '', NULL, '');

-- 6、字典管理菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/dict/index', '', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', NOW(), '', NULL, '字典管理菜单');

-- 字典管理按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1051, '字典查询', 105, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', NOW(), '', NULL, ''),
(1052, '字典新增', 105, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', NOW(), '', NULL, ''),
(1053, '字典修改', 105, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', NOW(), '', NULL, ''),
(1054, '字典删除', 105, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', NOW(), '', NULL, ''),
(1055, '字典导出', 105, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', NOW(), '', NULL, '');

-- 7、参数设置菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (106, '参数设置', 1, 7, 'config', 'system/config/config/index', '', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', NOW(), '', NULL, '参数设置菜单');

-- 参数设置按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1061, '参数查询', 106, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', NOW(), '', NULL, ''),
(1062, '参数新增', 106, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', NOW(), '', NULL, ''),
(1063, '参数修改', 106, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', NOW(), '', NULL, ''),
(1064, '参数删除', 106, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', NOW(), '', NULL, ''),
(1065, '参数导出', 106, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', NOW(), '', NULL, '');

-- 8、通知公告菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/notice/index', '', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 'admin', NOW(), '', NULL, '通知公告菜单');

-- 通知公告按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1071, '公告查询', 107, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 'admin', NOW(), '', NULL, ''),
(1072, '公告新增', 107, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 'admin', NOW(), '', NULL, ''),
(1073, '公告修改', 107, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 'admin', NOW(), '', NULL, ''),
(1074, '公告删除', 107, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', NOW(), '', NULL, '');

-- ========== 配置系统监控菜单 ==========

-- 1、在线用户菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', NOW(), '', NULL, '在线用户菜单');

-- 在线用户按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1091, '在线用户查询', 109, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 'admin', NOW(), '', NULL, ''),
(1092, '批量强退', 109, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', NOW(), '', NULL, ''),
(1093, '单条强退', 109, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', NOW(), '', NULL, '');

-- 2、定时任务菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job', 'admin', NOW(), '', NULL, '定时任务菜单');

-- 定时任务按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1101, '任务查询', 110, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query', '#', 'admin', NOW(), '', NULL, ''),
(1102, '任务新增', 110, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add', '#', 'admin', NOW(), '', NULL, ''),
(1103, '任务修改', 110, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit', '#', 'admin', NOW(), '', NULL, ''),
(1104, '任务删除', 110, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove', '#', 'admin', NOW(), '', NULL, ''),
(1105, '任务导出', 110, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export', '#', 'admin', NOW(), '', NULL, ''),
(1106, '状态修改', 110, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'admin', NOW(), '', NULL, ''),
(1107, '任务详细', 110, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:detail', '#', 'admin', NOW(), '', NULL, ''),
(1108, '任务执行', 110, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:execute', '#', 'admin', NOW(), '', NULL, '');

-- 3、数据监控菜单（已停用：Druid 监控页面前端已移除，如需启用请先恢复
--    zhi-ui/src/views/admin/monitor/druid/index.vue 再取消下面的注释）
-- INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
-- VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list', 'druid', 'admin', NOW(), '', NULL, '数据监控菜单');

-- 4、服务监控菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', '', 1, 0, 'C', '0', '0', 'monitor:server:list', 'server', 'admin', NOW(), '', NULL, '服务监控菜单');

-- 5、缓存监控菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 'admin', NOW(), '', NULL, '缓存监控菜单');

-- 6、缓存列表菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (114, '缓存列表', 2, 6, 'cacheList', 'monitor/cache/list', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis-list', 'admin', NOW(), '', NULL, '缓存列表菜单');

-- 缓存列表按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1141, '缓存查询', 114, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:cache:query', '#', 'admin', NOW(), '', NULL, ''),
(1142, '缓存新增', 114, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:cache:add', '#', 'admin', NOW(), '', NULL, ''),
(1143, '缓存修改', 114, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:cache:edit', '#', 'admin', NOW(), '', NULL, ''),
(1144, '缓存删除', 114, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:cache:remove', '#', 'admin', NOW(), '', NULL, ''),
(1145, '缓存清理', 114, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:cache:clean', '#', 'admin', NOW(), '', NULL, '');

-- ========== 配置系统工具菜单 ==========

-- 1、表单构建菜单（已停用：表单构建页面为占位实现且前端已移除，如需启用请先恢复
--    zhi-ui/src/views/admin/tool/build/index.vue 再取消下面的注释）
-- INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
-- VALUES (115, '表单构建', 3, 1, 'build', 'tool/build/index', '', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 'admin', NOW(), '', NULL, '表单构建菜单');

-- 2、代码生成菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (116, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin', NOW(), '', NULL, '代码生成菜单');

-- 代码生成按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(1161, '生成查询', 116, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 'admin', NOW(), '', NULL, ''),
(1162, '生成新增', 116, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:add', '#', 'admin', NOW(), '', NULL, ''),
(1163, '生成编辑', 116, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 'admin', NOW(), '', NULL, ''),
(1164, '生成删除', 116, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 'admin', NOW(), '', NULL, ''),
(1165, '导入代码', 116, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 'admin', NOW(), '', NULL, ''),
(1166, '预览代码', 116, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 'admin', NOW(), '', NULL, ''),
(1167, '生成代码', 116, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 'admin', NOW(), '', NULL, '');

-- 3、系统接口菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (117, '系统接口', 3, 3, 'swagger', 'tool/swagger/index', '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list', 'swagger', 'admin', NOW(), '', NULL, '系统接口菜单');

-- ========== 系统监控补充菜单 ==========

-- 1、Actuator监控菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5000, 'Actuator监控', 2, 10, 'actuator', 'monitor/actuator/index', '', '', 1, 0, 'C', '0', '0', 'monitor:actuator:list', 'monitor', 'admin', NOW(), '', NULL, 'Actuator监控菜单');

-- Actuator监控按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(5001, 'Actuator查询', 5000, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:actuator:query', '#', 'admin', NOW(), '', NULL, '');

-- 2、Prometheus监控菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5002, 'Prometheus监控', 2, 11, 'prometheus', 'monitor/prometheus/index', '', '', 1, 0, 'C', '0', '0', 'monitor:prometheus:list', 'chart', 'admin', NOW(), '', NULL, 'Prometheus监控菜单');

-- Prometheus监控按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(5003, 'Prometheus查询', 5002, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:prometheus:query', '#', 'admin', NOW(), '', NULL, '');

-- 3、Grafana监控菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5004, 'Grafana监控', 2, 12, 'grafana', 'monitor/grafana/index', '', '', 1, 0, 'C', '0', '0', 'monitor:grafana:list', 'dashboard', 'admin', NOW(), '', NULL, 'Grafana监控菜单');

-- Grafana监控按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(5005, 'Grafana查询', 5004, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:grafana:query', '#', 'admin', NOW(), '', NULL, '');

-- 4、登录日志菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5006, '登录日志', 2, 7, 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', NOW(), '', NULL, '登录日志菜单');

-- 登录日志按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(5007, '登录日志查询', 5006, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', NOW(), '', NULL, ''),
(5008, '登录日志删除', 5006, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', NOW(), '', NULL, ''),
(5009, '登录日志导出', 5006, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', NOW(), '', NULL, ''),
(5014, '登录日志解锁', 5006, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 'admin', NOW(), '', NULL, '');

-- 5、操作日志菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5010, '操作日志', 2, 8, 'operlog', 'monitor/operlog/index', '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', NOW(), '', NULL, '操作日志菜单');

-- 操作日志按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(5011, '操作日志查询', 5010, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', NOW(), '', NULL, ''),
(5012, '操作日志删除', 5010, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', NOW(), '', NULL, ''),
(5013, '操作日志导出', 5010, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', NOW(), '', NULL, '');

-- ========== 数据统计菜单配置 ==========

-- 1、数据统计一级菜单（注意：visible='0' 表示显示，'1' 表示隐藏）
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (5, '数据统计', 0, 2, 'admin/statistics', NULL, '', '', 1, 0, 'M', '0', '0', '', 'nested', 'admin', NOW(), '', NULL, '数据统计目录');

-- 2、数据概览菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (6001, '数据概览', 5, 1, 'overview', 'statistics/overview/index', '', '', 1, 0, 'C', '0', '0', 'statistics:overview:list', 'chart', 'admin', NOW(), '', NULL, '数据概览菜单');

-- 数据概览按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(6002, '数据概览查询', 6001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:overview:query', '#', 'admin', NOW(), '', NULL, '');

-- 3、文章统计菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (6003, '文章统计', 5, 2, 'article', 'statistics/article/index', '', '', 1, 0, 'C', '0', '0', 'statistics:article:list', 'documentation', 'admin', NOW(), '', NULL, '文章统计菜单');

-- 文章统计按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(6004, '文章统计查询', 6003, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:article:query', '#', 'admin', NOW(), '', NULL, ''),
(6005, '文章统计导出', 6003, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:article:export', '#', 'admin', NOW(), '', NULL, ''),
(6009, '文章趋势', 6003, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:article:trend', '#', 'admin', NOW(), '', NULL, ''),
(6010, '分类分布', 6003, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:article:category-distribution', '#', 'admin', NOW(), '', NULL, ''),
(6011, '热门标签', 6003, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:article:hot-tags', '#', 'admin', NOW(), '', NULL, '');

-- 4、用户统计菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (6006, '用户统计', 5, 3, 'user', 'statistics/user/index', '', '', 1, 0, 'C', '0', '0', 'statistics:user:list', 'user', 'admin', NOW(), '', NULL, '用户统计菜单');

-- 用户统计按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(6007, '用户统计查询', 6006, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:user:query', '#', 'admin', NOW(), '', NULL, ''),
(6008, '用户统计导出', 6006, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:user:export', '#', 'admin', NOW(), '', NULL, ''),
(6012, '用户活跃', 6006, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:user:activity', '#', 'admin', NOW(), '', NULL, ''),
(6013, '注册趋势', 6006, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:user:register-trend', '#', 'admin', NOW(), '', NULL, ''),
(6014, '角色分布', 6006, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:user:role-distribution', '#', 'admin', NOW(), '', NULL, '');

-- 访问明细菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (6015, '访问明细', 5, 4, 'visit', 'statistics/visit/index', '', '', 1, 0, 'C', '0', '0', 'statistics:visit:list', 'eye', 'admin', NOW(), '', NULL, '访问明细（PV/UV 明细）菜单');

-- 访问明细按钮权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(6016, '访问明细查询', 6015, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:visit:query', '#', 'admin', NOW(), '', NULL, ''),
(6017, '访问明细导出', 6015, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:visit:export', '#', 'admin', NOW(), '', NULL, ''),
(6018, '访问明细清理', 6015, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'statistics:visit:remove', '#', 'admin', NOW(), '', NULL, '');

-- ========== 为管理员角色分配系统管理、监控和工具菜单权限 ==========

-- 系统管理菜单权限（包括菜单和按钮权限）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
-- 系统管理主菜单
(1, 1),
-- 用户管理
(1, 100), (1, 1001), (1, 1002), (1, 1003), (1, 1004), (1, 1005), (1, 1006), (1, 1007),
-- 角色管理
(1, 101), (1, 1011), (1, 1012), (1, 1013), (1, 1014), (1, 1015),
-- 菜单管理
(1, 102), (1, 1021), (1, 1022), (1, 1023), (1, 1024),
-- 部门管理
(1, 103), (1, 1031), (1, 1032), (1, 1033), (1, 1034),
-- 岗位管理
(1, 104), (1, 1041), (1, 1042), (1, 1043), (1, 1044), (1, 1045),
-- 字典管理
(1, 105), (1, 1051), (1, 1052), (1, 1053), (1, 1054), (1, 1055),
-- 参数设置
(1, 106), (1, 1061), (1, 1062), (1, 1063), (1, 1064), (1, 1065),
-- 通知公告
(1, 107), (1, 1071), (1, 1072), (1, 1073), (1, 1074);

-- 系统监控菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 109), (1, 1091), (1, 1092), (1, 1093),
(1, 110), (1, 1101), (1, 1102), (1, 1103), (1, 1104), (1, 1105), (1, 1106), (1, 1107), (1, 1108),
(1, 112), (1, 113),
(1, 114), (1, 1141), (1, 1142), (1, 1143), (1, 1144), (1, 1145),
(1, 5000), (1, 5001),
(1, 5002), (1, 5003),
(1, 5004), (1, 5005),
(1, 5006), (1, 5007), (1, 5008), (1, 5009), (1, 5014),
(1, 5010), (1, 5011), (1, 5012), (1, 5013);

-- 系统工具菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 116), (1, 1161), (1, 1162), (1, 1163), (1, 1164), (1, 1165), (1, 1166), (1, 1167), (1, 117);

-- 数据统计菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 5),
(1, 6001), (1, 6002),
(1, 6003), (1, 6004), (1, 6005), (1, 6009), (1, 6010), (1, 6011),
(1, 6006), (1, 6007), (1, 6008), (1, 6012), (1, 6013), (1, 6014),
(1, 6015), (1, 6016), (1, 6017), (1, 6018);

-- ========== 创建性能优化索引（幂等，调用前面的存储过程） ==========

-- 博客评论表索引优化
CALL sp_create_index_if_not_exists('zhi_comment', 'idx_article_id', 'CREATE INDEX idx_article_id ON zhi_comment(article_id)');
CALL sp_create_index_if_not_exists('zhi_comment', 'idx_parent_id', 'CREATE INDEX idx_parent_id ON zhi_comment(parent_id)');
CALL sp_create_index_if_not_exists('zhi_comment', 'idx_status', 'CREATE INDEX idx_status ON zhi_comment(status)');
CALL sp_create_index_if_not_exists('zhi_comment', 'idx_create_time', 'CREATE INDEX idx_create_time ON zhi_comment(create_time)');
CALL sp_create_index_if_not_exists('zhi_comment', 'idx_article_status', 'CREATE INDEX idx_article_status ON zhi_comment(article_id, status)');

-- 博客标签表索引优化
CALL sp_create_index_if_not_exists('zhi_tag', 'idx_del_flag', 'CREATE INDEX idx_del_flag ON zhi_tag(del_flag)');
CALL sp_create_index_if_not_exists('zhi_tag', 'idx_name', 'CREATE INDEX idx_name ON zhi_tag(name)');
CALL sp_create_index_if_not_exists('zhi_tag', 'idx_article_count', 'CREATE INDEX idx_article_count ON zhi_tag(article_count)');

-- 博客文章表复合索引优化
CALL sp_create_index_if_not_exists('zhi_article', 'idx_status_del_time', 'CREATE INDEX idx_status_del_time ON zhi_article(status, del_flag, create_time)');
CALL sp_create_index_if_not_exists('zhi_article', 'idx_category_status', 'CREATE INDEX idx_category_status ON zhi_article(category_id, status)');
CALL sp_create_index_if_not_exists('zhi_article', 'idx_author_status', 'CREATE INDEX idx_author_status ON zhi_article(author_id, status)');
CALL sp_create_index_if_not_exists('zhi_article', 'idx_top_status_time', 'CREATE INDEX idx_top_status_time ON zhi_article(is_top, status, create_time)');
-- 标题唯一（仅约束未删除行）：应用层查重之外兜底并发写入；函数索引使已删除行不占用标题。
-- MySQL 8.0.13+ 函数索引；建前需保证存量未删除行无重复标题
CALL sp_create_index_if_not_exists('zhi_article', 'uk_article_title_alive', 'CREATE UNIQUE INDEX uk_article_title_alive ON zhi_article ((CASE WHEN del_flag = 0 THEN title END))');

-- 博客设置表索引优化
CALL sp_create_index_if_not_exists('zhi_setting', 'idx_config_key', 'CREATE INDEX idx_config_key ON zhi_setting(config_key)');

-- 文章标签关联表索引优化
CALL sp_create_index_if_not_exists('zhi_article_tag', 'idx_tag_id', 'CREATE INDEX idx_tag_id ON zhi_article_tag(tag_id)');

-- =====================================================
-- ========== 性能优化索引（幂等创建，可重复执行）==========
-- ========== 注：sp_create_index_if_not_exists 已在文件开头定义 ==========
-- =====================================================

-- 文章状态和删除标志复合索引（用于列表查询）
CALL sp_create_index_if_not_exists('zhi_article', 'idx_article_status_del_flag', 'CREATE INDEX idx_article_status_del_flag ON zhi_article(status, del_flag)');

-- 文章创建时间索引（用于排序和归档）
CALL sp_create_index_if_not_exists('zhi_article', 'idx_article_create_time', 'CREATE INDEX idx_article_create_time ON zhi_article(create_time)');

-- 文章浏览量索引（用于热门文章排序）
CALL sp_create_index_if_not_exists('zhi_article', 'idx_article_view_count', 'CREATE INDEX idx_article_view_count ON zhi_article(view_count DESC)');

-- 文章置顶和推荐状态复合索引（用于首页展示）
CALL sp_create_index_if_not_exists('zhi_article', 'idx_article_is_top_recommend', 'CREATE INDEX idx_article_is_top_recommend ON zhi_article(is_top DESC, is_recommend DESC, status DESC, del_flag)');

-- 文章标题全文索引（用于搜索功能）
CALL sp_create_index_if_not_exists('zhi_article', 'ft_article_title', 'CREATE FULLTEXT INDEX ft_article_title ON zhi_article(title)');
CALL sp_create_index_if_not_exists('zhi_article', 'ft_article_content', 'CREATE FULLTEXT INDEX ft_article_content ON zhi_article(content)');
CALL sp_create_index_if_not_exists('zhi_article', 'ft_article_title_content', 'CREATE FULLTEXT INDEX ft_article_title_content ON zhi_article(title, content)');

-- 评论复合索引（用于前台展示评论）
CALL sp_create_index_if_not_exists('zhi_comment', 'idx_comment_article_status_time', 'CREATE INDEX idx_comment_article_status_time ON zhi_comment(article_id, status, create_time DESC)');

-- 分类排序索引
CALL sp_create_index_if_not_exists('zhi_category', 'idx_category_sort_order', 'CREATE INDEX idx_category_sort_order ON zhi_category(sort_order)');

-- 标签使用次数索引（用于热门标签）
CALL sp_create_index_if_not_exists('zhi_tag', 'idx_tag_article_count', 'CREATE INDEX idx_tag_article_count ON zhi_tag(article_count DESC)');

-- 文章标签关联复合唯一索引（防止重复关联）
CALL sp_create_index_if_not_exists('zhi_article_tag', 'uk_article_tag_unique', 'CREATE UNIQUE INDEX uk_article_tag_unique ON zhi_article_tag(article_id, tag_id)');

-- 链接排序索引
CALL sp_create_index_if_not_exists('zhi_friend_link', 'idx_friend_link_sort', 'CREATE INDEX idx_friend_link_sort ON zhi_friend_link(sort DESC)');

-- 链接状态索引（用于查询有效链接）
CALL sp_create_index_if_not_exists('zhi_friend_link', 'idx_friend_link_status', 'CREATE INDEX idx_friend_link_status ON zhi_friend_link(status)');

-- 设置更新时间索引（用于缓存失效判断）
CALL sp_create_index_if_not_exists('zhi_setting', 'idx_setting_update_time', 'CREATE INDEX idx_setting_update_time ON zhi_setting(update_time)');

-- 性能监控视图
CREATE OR REPLACE VIEW v_slow_queries AS
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH,
    (DATA_LENGTH + INDEX_LENGTH) as TOTAL_SIZE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME LIKE 'blog_%'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

CREATE OR REPLACE VIEW v_index_usage AS
SELECT
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    NULLABLE,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME LIKE 'blog_%'
ORDER BY TABLE_NAME, INDEX_NAME;

-- 执行统计信息更新
ANALYZE TABLE zhi_article;
ANALYZE TABLE zhi_category;
ANALYZE TABLE zhi_tag;
ANALYZE TABLE zhi_article_tag;
ANALYZE TABLE zhi_comment;
ANALYZE TABLE zhi_setting;
ANALYZE TABLE zhi_friend_link;

-- ========== 添加数据完整性约束 ==========

-- 博客文章表约束检查和触发器
DELIMITER $$

-- 创建分类文章数量更新函数
DROP FUNCTION IF EXISTS `update_category_article_count`$$
CREATE FUNCTION `update_category_article_count`(
    category_id BIGINT,
    count_change INT
) RETURNS BOOLEAN
DETERMINISTIC
BEGIN
    DECLARE category_exists BOOLEAN;

    -- 检查分类是否存在且未删除
    SELECT COUNT(*) > 0 INTO category_exists
    FROM zhi_category
    WHERE id = category_id AND del_flag = '0';

    -- 如果分类存在，更新文章数量
    IF category_exists THEN
        UPDATE zhi_category
        SET article_count = GREATEST(0, article_count + count_change),
            update_time = NOW()
        WHERE id = category_id;
        RETURN TRUE;
    END IF;

    RETURN FALSE;
END$$

-- 文章更新前触发器
DROP TRIGGER IF EXISTS `tr_article_update_time`$$
CREATE TRIGGER `tr_article_update_time`
BEFORE UPDATE ON `zhi_article`
FOR EACH ROW
BEGIN
    DECLARE old_valid_articles INT DEFAULT 0;
    DECLARE new_valid_articles INT DEFAULT 0;
    DECLARE category_count_updated BOOLEAN DEFAULT FALSE;

    -- 更新时间总是设置
    SET NEW.update_time = NOW();

    -- 如果禁用了触发器（用于批量更新），直接返回（不执行后续逻辑）
    IF @DISABLE_TRIGGERS IS NULL THEN
        -- 只有在未禁用触发器时才执行分类统计更新

    -- 检查旧文章状态是否有效（未删除且已发布）
    IF OLD.del_flag = '0' AND OLD.status = 1 AND OLD.category_id IS NOT NULL THEN
        SET old_valid_articles = 1;
    END IF;

    -- 检查新文章状态是否有效
    IF NEW.del_flag = '0' AND NEW.status = 1 AND NEW.category_id IS NOT NULL THEN
        SET new_valid_articles = 1;
    END IF;

    -- 处理分类变更
    IF OLD.category_id != NEW.category_ID THEN
        -- 从旧分类中减去
        IF old_valid_articles = 1 THEN
            SET category_count_updated = update_category_article_count(OLD.category_id, -1);
        END IF;

        -- 向新分类中添加
        IF new_valid_articles = 1 THEN
            SET category_count_updated = update_category_article_count(NEW.category_id, 1);
        END IF;
    ELSE
        -- 同一分类内的状态变更
        IF OLD.category_id IS NOT NULL THEN
            SET category_count_updated = update_category_article_count(OLD.category_id, new_valid_articles - old_valid_articles);
        END IF;
    END IF;
    END IF;
END$$

-- 文章插入前触发器
DROP TRIGGER IF EXISTS `tr_article_insert`$$
CREATE TRIGGER `tr_article_insert`
BEFORE INSERT ON `zhi_article`
FOR EACH ROW
BEGIN
    DECLARE category_count_updated BOOLEAN DEFAULT FALSE;

    -- 设置创建时间
    IF NEW.create_time IS NULL THEN
        SET NEW.create_time = NOW();
    END IF;
    SET NEW.update_time = NOW();

    -- 如果禁用了触发器（用于批量更新），直接返回（不执行后续逻辑）
    IF @DISABLE_TRIGGERS IS NULL THEN
        -- 自动增加分类文章数（仅在文章有效时）
        IF NEW.category_id IS NOT NULL AND NEW.del_flag = '0' AND NEW.status = 1 THEN
            SET category_count_updated = update_category_article_count(NEW.category_id, 1);
        END IF;
    END IF;
END$$

-- 文章删除后触发器（物理删除）
DROP TRIGGER IF EXISTS `tr_article_delete_physical`$$
CREATE TRIGGER `tr_article_delete_physical`
AFTER DELETE ON `zhi_article`
FOR EACH ROW
BEGIN
    DECLARE category_count_updated BOOLEAN DEFAULT FALSE;

    -- 如果禁用了触发器，直接返回（不执行后续逻辑）
    IF @DISABLE_TRIGGERS IS NULL THEN
        -- 物理删除时减少分类文章数（仅在文章有效时）
        IF OLD.category_id IS NOT NULL AND OLD.del_flag = '0' AND OLD.status = 1 THEN
            SET category_count_updated = update_category_article_count(OLD.category_id, -1);
        END IF;
    END IF;
END$$

DELIMITER ;

-- ========== 创建分类数量修复存储过程 ==========

DELIMITER $$

-- 修复分类文章数量的存储过程（指定分类ID；传入NULL时修复所有分类）
DROP PROCEDURE IF EXISTS `sp_fix_category_article_count_by_id`$$
CREATE PROCEDURE `sp_fix_category_article_count_by_id`(
    IN category_id_param BIGINT
)
BEGIN
    DECLARE fix_count INT DEFAULT 0;
    DECLARE total_categories INT DEFAULT 0;
    DECLARE mismatch_count INT DEFAULT 0;

    -- 禁用触发器以避免冲突
    SET @DISABLE_TRIGGERS = TRUE;

    -- 如果指定了分类ID，只修复该分类
    IF category_id_param IS NOT NULL THEN
        UPDATE zhi_category c
        SET c.article_count = (
            SELECT COUNT(*)
            FROM zhi_article a
            WHERE a.category_id = c.id
            AND a.del_flag = '0'
            AND a.status = 1
        ),
        c.update_time = NOW()
        WHERE c.id = category_id_param AND c.del_flag = '0';

        SET fix_count = ROW_COUNT();
    ELSE
        -- 修复所有分类
        UPDATE zhi_category c
        SET c.article_count = (
            SELECT COUNT(*)
            FROM zhi_article a
            WHERE a.category_id = c.id
            AND a.del_flag = '0'
            AND a.status = 1
        ),
        c.update_time = NOW()
        WHERE c.del_flag = '0';

        SET fix_count = ROW_COUNT();
    END IF;

    -- 重新启用触发器
    SET @DISABLE_TRIGGERS = NULL;

    -- 统计信息
    SELECT COUNT(*) INTO total_categories
    FROM zhi_category
    WHERE del_flag = '0';

    SELECT COUNT(*) INTO mismatch_count
    FROM zhi_category c
    LEFT JOIN (
        SELECT category_id, COUNT(*) as real_count
        FROM zhi_article
        WHERE del_flag = '0' AND status = 1
        GROUP BY category_id
    ) a ON c.id = a.category_id
    WHERE c.del_flag = '0'
    AND IFNULL(a.real_count, 0) != c.article_count;

    -- 返回修复结果
    SELECT
        fix_count as fixed_categories,
        total_categories as total_categories,
        mismatch_count as remaining_mismatches,
        CASE
            WHEN mismatch_count = 0 THEN '✅ 所有分类数量已同步'
            ELSE '⚠️ 仍有分类数量不匹配'
        END as status;
END$$

-- 修复所有分类文章数量的便捷存储过程
DROP PROCEDURE IF EXISTS `sp_fix_category_article_count`$$
CREATE PROCEDURE `sp_fix_category_article_count`()
BEGIN
    CALL sp_fix_category_article_count_by_id(NULL);
END$$

-- 验证分类文章数量的存储过程
DROP PROCEDURE IF EXISTS `sp_verify_category_article_count`$$
CREATE PROCEDURE `sp_verify_category_article_count`()
BEGIN
    -- 显示详细的验证结果
    SELECT
        c.id,
        c.name,
        c.status as category_status,
        c.del_flag as category_del_flag,
        IFNULL(COUNT(a.id), 0) as actual_article_count,
        c.article_count as cached_article_count,
        CASE
            WHEN IFNULL(COUNT(a.id), 0) != c.article_count THEN '❌ 不匹配'
            ELSE '✅ 匹配'
        END as status,
        CASE
            WHEN c.status = 1 AND c.del_flag = '0' THEN '✅ 启用'
            ELSE '❌ 禁用/删除'
        END as visibility
    FROM zhi_category c
    LEFT JOIN zhi_article a ON c.id = a.category_id AND a.del_flag = '0' AND a.status = 1
    WHERE c.del_flag = '0'
    GROUP BY c.id, c.name, c.status, c.del_flag, c.article_count
    ORDER BY c.sort_order ASC;

    -- 统计前台可见的分类
    SELECT
        '🔍 前台可见分类统计（启用且未删除）：' AS type,
        COUNT(*) as total_categories,
        SUM(article_count) as total_articles
    FROM zhi_category
    WHERE del_flag = '0' AND status = 1;

    -- 查找不匹配的分类
    SELECT
        '⚠️ 不匹配的分类：' AS type,
        COUNT(*) as mismatch_count
    FROM zhi_category c
    LEFT JOIN (
        SELECT category_id, COUNT(*) as real_count
        FROM zhi_article
        WHERE del_flag = '0' AND status = 1
        GROUP BY category_id
    ) a ON c.id = a.category_id
    WHERE c.del_flag = '0'
    AND IFNULL(a.real_count, 0) != c.article_count;
END$$

DELIMITER ;

-- ========== 数据完整性验证 ==========

-- 修复可能的NULL值（确保view_count、like_count、comment_count字段不为NULL）
UPDATE zhi_article SET view_count = 0 WHERE view_count IS NULL;
UPDATE zhi_article SET like_count = 0 WHERE like_count IS NULL;
UPDATE zhi_article SET comment_count = 0 WHERE comment_count IS NULL;

-- 验证关键字段完整性
SELECT '验证站点文章数据完整性...' AS status;
SELECT COUNT(*) AS empty_title_articles FROM zhi_article WHERE title = '' OR title IS NULL;
SELECT COUNT(*) AS empty_content_articles FROM zhi_article WHERE content = '' OR content IS NULL;
SELECT COUNT(*) AS invalid_status_articles FROM zhi_article WHERE status NOT IN (0, 1);
SELECT COUNT(*) AS invalid_del_flag_articles FROM zhi_article WHERE del_flag NOT IN ('0', '1');

SELECT '验证站点分类数据完整性...' AS status;
SELECT COUNT(*) AS empty_name_categories FROM zhi_category WHERE name = '' OR name IS NULL;
SELECT COUNT(*) AS invalid_parent_categories FROM zhi_category WHERE parent_id NOT IN (SELECT id FROM zhi_category) AND parent_id != 0;

SELECT '验证站点标签数据完整性...' AS status;
SELECT COUNT(*) AS empty_name_tags FROM zhi_tag WHERE name = '' OR name IS NULL;
SELECT COUNT(*) AS duplicate_tags
FROM (
    SELECT name
    FROM zhi_tag
    GROUP BY name
    HAVING COUNT(*) > 1
) duplicate_tag_names;

SELECT '验证站点设置数据完整性...' AS status;
SELECT COUNT(*) AS total_settings FROM zhi_setting;
SELECT COUNT(*) AS duplicate_settings
FROM (
    SELECT config_key
    FROM zhi_setting
    GROUP BY config_key
    HAVING COUNT(*) > 1
) duplicate_setting_keys;

-- ========== 性能统计 ==========

-- 统计各表数据量
SELECT 'zhi_article' AS table_name, COUNT(*) AS record_count FROM zhi_article
UNION ALL
SELECT 'zhi_category' AS table_name, COUNT(*) AS record_count FROM zhi_category
UNION ALL
SELECT 'zhi_tag' AS table_name, COUNT(*) AS record_count FROM zhi_tag
UNION ALL
SELECT 'zhi_article_tag' AS table_name, COUNT(*) AS record_count FROM zhi_article_tag
UNION ALL
SELECT 'zhi_comment' AS table_name, COUNT(*) AS record_count FROM zhi_comment
UNION ALL
SELECT 'zhi_friend_link' AS table_name, COUNT(*) AS record_count FROM zhi_friend_link
UNION ALL
SELECT 'zhi_setting' AS table_name, COUNT(*) AS record_count FROM zhi_setting;

-- ========== 数据库初始化完成 ==========

SELECT '🎉 数据库初始化完成！' AS result;
SELECT '✅ 系统基础表数量：' AS info, COUNT(*) as count FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name LIKE 'sys_%';
SELECT '✅ 站点系统表数量：' AS info, COUNT(*) as count FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name LIKE 'blog_%';
SELECT '✅ Quartz定时任务表数量：' AS info, COUNT(*) as count FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name LIKE 'QRTZ_%';
SELECT '✅ 代码生成器表数量：' AS info, COUNT(*) as count FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name LIKE 'gen_%';
SELECT '✅ 站点管理菜单数量：' AS info, COUNT(*) as count FROM sys_menu WHERE menu_name = '站点管理' OR parent_id = 2000;
SELECT '✅ 系统管理菜单数量：' AS info, COUNT(*) as count FROM sys_menu WHERE menu_id BETWEEN 100 AND 107;
SELECT '✅ 系统监控菜单数量：' AS info, COUNT(*) as count FROM sys_menu WHERE menu_id BETWEEN 109 AND 114 OR menu_id BETWEEN 5000 AND 5013;
SELECT '✅ 数据统计菜单数量：' AS info, COUNT(*) as count FROM sys_menu WHERE menu_id = 5 OR parent_id = 5;
SELECT '✅ 系统工具菜单数量：' AS info, COUNT(*) as count FROM sys_menu WHERE menu_id BETWEEN 115 AND 117;

-- ========== 执行分类数量验证和修复 ==========

-- 调用修复存储过程确保分类数量正确
SELECT '🔧 正在执行分类数量修复...' AS status;
CALL sp_fix_category_article_count();

-- 调用验证存储过程显示修复结果
SELECT '✅ 分类数量修复完成，验证结果如下：' AS status;
CALL sp_verify_category_article_count();

-- 显示初始化的数据统计
SELECT '📊 数据初始化统计：' AS status;
SELECT CONCAT('📝 文章数量: ', COUNT(*)) AS info FROM zhi_article WHERE del_flag = '0'
UNION ALL
SELECT CONCAT('📁 分类数量: ', COUNT(*)) AS info FROM zhi_category WHERE del_flag = '0'
UNION ALL
SELECT CONCAT('🏷️  标签数量: ', COUNT(*)) AS info FROM zhi_tag WHERE del_flag = '0'
UNION ALL
SELECT CONCAT('💬 评论数量: ', COUNT(*)) AS info FROM zhi_comment
UNION ALL
SELECT CONCAT('🔗 友链数量: ', COUNT(*)) AS info FROM zhi_friend_link WHERE del_flag = '0'
UNION ALL
SELECT CONCAT('📮 留言数量: ', COUNT(*)) AS info FROM zhi_message WHERE del_flag = '0'
UNION ALL
SELECT CONCAT('📄 自定义页面数量: ', COUNT(*)) AS info FROM zhi_page WHERE del_flag = '0'
UNION ALL
SELECT CONCAT('👣 访问明细数量: ', COUNT(*)) AS info FROM zhi_visit_log
UNION ALL
SELECT CONCAT('⚙️  设置数量: ', COUNT(*)) AS info FROM zhi_setting;

-- 验证标签和文章关联数据
SELECT '📋 标签列表验证' as type, t.id, t.name, t.color, t.description
FROM zhi_tag t WHERE t.del_flag = 0
ORDER BY t.id;

-- 验证文章-标签关联
SELECT
    '📎 文章标签关联' as type,
    a.id as article_id,
    a.title as article_title,
    t.id as tag_id,
    t.name as tag_name,
    t.color as tag_color
FROM zhi_article a
LEFT JOIN zhi_article_tag bat ON a.id = bat.article_id
LEFT JOIN zhi_tag t ON bat.tag_id = t.id AND t.del_flag = 0
WHERE a.id IN (1, 2, 4) AND a.del_flag = 0
ORDER BY a.id, t.name;

-- 统计每篇文章的标签数量
SELECT
    '📊 标签数量统计' as type,
    a.id as article_id,
    a.title as article_title,
    COUNT(bat.tag_id) as tag_count
FROM zhi_article a
LEFT JOIN zhi_article_tag bat ON a.id = bat.article_id
LEFT JOIN zhi_tag t ON bat.tag_id = t.id AND t.del_flag = 0
WHERE a.id IN (1, 2, 4) AND a.del_flag = 0
GROUP BY a.id, a.title
ORDER BY a.id;

SELECT '🚀 知企建站系统已准备就绪，可以开始使用了！' AS final_message;

-- ===============================================================
-- 📖 使用指南和故障排除
-- ===============================================================

SELECT '📚 使用指南：' AS guide_title;
SELECT '1. 启动后端服务：mvn spring-boot:run' AS step1;
SELECT '2. 启动前端服务：cd zhi-ui && npm run dev' AS step2;
SELECT '3. 访问管理后台：http://localhost:8080' AS step3;
SELECT '4. 默认账号：admin（首次登录请修改默认密码）' AS step4;
SELECT '5. 访问前台首页：http://localhost:3000' AS step5;

-- 验证关键配置
SELECT '🔍 关键配置验证：' AS verify_title;
SELECT config_key, config_value, description
FROM zhi_setting
WHERE config_key IN ('blog_name', 'footer_enabled', 'copyright_enabled', 'comment_enabled')
ORDER BY config_key;

-- 常见问题解决方案
SELECT '🛠️  常见问题：' AS trouble_title;
SELECT 'Q: 忘记密码怎么办？' AS q1, 'A: 删除sys_user表中admin记录，重新运行脚本' AS a1;
SELECT 'Q: 前端页面空白？' AS q2, 'A: 检查API接口地址是否正确，端口是否被占用' AS a2;
SELECT 'Q: 评论功能不工作？' AS q3, 'A: 检查comment_enabled设置是否为true' AS a3;
SELECT 'Q: 图片上传失败？' AS q4, 'A: 检查文件路径权限和磁盘空间' AS a4;
SELECT 'Q: 分类文章数量不准确？' AS q5, 'A: 执行 CALL sp_fix_category_article_count(); 修复' AS a5;
SELECT 'Q: 文章阅读数不增加？' AS q6, 'A: 1. BlogArticleMapper.xml使用IFNULL处理NULL; 2. 确保view_count默认值为0; 3. 执行UPDATE修复现有数据; 4. 重启后端服务' AS a6;

-- 分类数量管理指南
SELECT '📊 分类数量管理指南：' AS category_guide;
SELECT '1. 查看验证结果：CALL sp_verify_category_article_count();' AS guide1;
SELECT '2. 修复单个分类：CALL sp_fix_category_article_count_by_id(分类ID);' AS guide2;
SELECT '3. 修复所有分类：CALL sp_fix_category_article_count();' AS guide3;
SELECT '4. 手动修复语句：UPDATE zhi_category SET article_count = (SELECT COUNT(*) FROM zhi_article WHERE category_id = 目标ID AND del_flag = ''0'' AND status = 1);' AS guide4;

-- 性能优化建议
SELECT '⚡ 性能优化建议：' AS performance_title;
SELECT '1. 定期清理软删除的数据：DELETE FROM zhi_article WHERE del_flag = 1 AND update_time < DATE_SUB(NOW(), INTERVAL 30 DAY)' AS tip1;
SELECT '2. 优化大表查询：使用分页和索引' AS tip2;
SELECT '3. 启用Redis缓存：减轻数据库压力' AS tip3;
SELECT '4. 定期备份数据：mysqldump -u root -p zhienterprise > backup.sql' AS tip4;

-- ===============================================================
-- 📌 文章收藏表
-- ===============================================================
CREATE TABLE IF NOT EXISTS zhi_bookmark (
    id          BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '主键ID',
    user_id     BIGINT(20)      NOT NULL                    COMMENT '用户ID',
    article_id  BIGINT(20)      NOT NULL                    COMMENT '文章ID',
    tenant_id   BIGINT(20)      DEFAULT 0                   COMMENT '租户ID',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_article (user_id, article_id),
    KEY idx_user_id (user_id),
    KEY idx_article_id (article_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏表';

-- ===============================================================
-- 📌 文章点赞记录表 / 评论点赞记录表（点赞体系）
-- ===============================================================
CREATE TABLE IF NOT EXISTS zhi_article_like (
    id          BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '主键ID',
    article_id  BIGINT(20)      NOT NULL                    COMMENT '文章ID',
    user_id     BIGINT(20)      NOT NULL                    COMMENT '用户ID',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_user (article_id, user_id),
    KEY idx_article_like_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞记录表';

CREATE TABLE IF NOT EXISTS zhi_comment_like (
    id          BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '主键ID',
    comment_id  BIGINT(20)      NOT NULL                    COMMENT '评论ID',
    user_id     BIGINT(20)      NOT NULL                    COMMENT '用户ID',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    KEY idx_comment_like_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞记录表';

-- 老库升级：补齐 zhi_comment.like_count 列（幂等）
SET @has_comment_like_count := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_comment' AND COLUMN_NAME = 'like_count');
SET @add_comment_like_count := IF(@has_comment_like_count = 0,
    'ALTER TABLE `zhi_comment` ADD COLUMN `like_count` int NOT NULL DEFAULT 0 COMMENT ''点赞数''',
    'SELECT 1');
PREPARE add_comment_like_count_stmt FROM @add_comment_like_count;
EXECUTE add_comment_like_count_stmt;
DEALLOCATE PREPARE add_comment_like_count_stmt;

-- 老库升级：补齐 zhi_article.publish_time 列（幂等，定时发布功能）
SET @has_article_publish_time := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'publish_time');
SET @add_article_publish_time := IF(@has_article_publish_time = 0,
    'ALTER TABLE `zhi_article` ADD COLUMN `publish_time` datetime DEFAULT NULL COMMENT ''定时发布时间''',
    'SELECT 1');
PREPARE add_article_publish_time_stmt FROM @add_article_publish_time;
EXECUTE add_article_publish_time_stmt;
DEALLOCATE PREPARE add_article_publish_time_stmt;

-- 老库升级：补齐 zhi_article.format / content_md 列（幂等，Markdown 文章）
SET @has_article_format := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'format');
SET @add_article_format := IF(@has_article_format = 0,
    'ALTER TABLE `zhi_article` ADD COLUMN `format` varchar(10) NOT NULL DEFAULT ''html'' COMMENT ''内容格式 html|markdown''',
    'SELECT 1');
PREPARE add_article_format_stmt FROM @add_article_format;
EXECUTE add_article_format_stmt;
DEALLOCATE PREPARE add_article_format_stmt;

SET @has_article_content_md := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'content_md');
SET @add_article_content_md := IF(@has_article_content_md = 0,
    'ALTER TABLE `zhi_article` ADD COLUMN `content_md` longtext COMMENT ''Markdown 源码（format=markdown 时保存）''',
    'SELECT 1');
PREPARE add_article_content_md_stmt FROM @add_article_content_md;
EXECUTE add_article_content_md_stmt;
DEALLOCATE PREPARE add_article_content_md_stmt;

-- 老库升级：补齐 zhi_article.article_type 列（幂等，核心业务/案例内容类型）
SET @has_article_type := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'article_type');
SET @add_article_type := IF(@has_article_type = 0,
    'ALTER TABLE `zhi_article` ADD COLUMN `article_type` varchar(20) NOT NULL DEFAULT ''news'' COMMENT ''内容类型：news 资讯 business 核心业务 case 客户案例''',
    'SELECT 1');
PREPARE add_article_type_stmt FROM @add_article_type;
EXECUTE add_article_type_stmt;
DEALLOCATE PREPARE add_article_type_stmt;

-- 老库升级：补齐 zhi_article 文章级 TDK 列（幂等，SEO 优化）
SET @has_article_seo_title := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'seo_title');
SET @add_article_seo_title := IF(@has_article_seo_title = 0,
    'ALTER TABLE `zhi_article` ADD COLUMN `seo_title` varchar(200) DEFAULT NULL COMMENT ''SEO 标题（留空回退文章标题）''',
    'SELECT 1');
PREPARE add_article_seo_title_stmt FROM @add_article_seo_title;
EXECUTE add_article_seo_title_stmt;
DEALLOCATE PREPARE add_article_seo_title_stmt;

SET @has_article_seo_keywords := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'seo_keywords');
SET @add_article_seo_keywords := IF(@has_article_seo_keywords = 0,
    'ALTER TABLE `zhi_article` ADD COLUMN `seo_keywords` varchar(255) DEFAULT NULL COMMENT ''SEO 关键词（英文逗号分隔）''',
    'SELECT 1');
PREPARE add_article_seo_keywords_stmt FROM @add_article_seo_keywords;
EXECUTE add_article_seo_keywords_stmt;
DEALLOCATE PREPARE add_article_seo_keywords_stmt;

SET @has_article_seo_description := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'seo_description');
SET @add_article_seo_description := IF(@has_article_seo_description = 0,
    'ALTER TABLE `zhi_article` ADD COLUMN `seo_description` varchar(500) DEFAULT NULL COMMENT ''SEO 描述（留空回退文章摘要）''',
    'SELECT 1');
PREPARE add_article_seo_description_stmt FROM @add_article_seo_description;
EXECUTE add_article_seo_description_stmt;
DEALLOCATE PREPARE add_article_seo_description_stmt;

-- 老库升级：补齐 zhi_article.slug 列（幂等，伪静态别名）
SET @has_article_slug := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_article' AND COLUMN_NAME = 'slug');
SET @add_article_slug := IF(@has_article_slug = 0,
    'ALTER TABLE `zhi_article` ADD COLUMN `slug` varchar(100) DEFAULT NULL COMMENT ''URL 别名（伪静态 /blog/article/{slug}，留空用数字 ID）''',
    'SELECT 1');
PREPARE add_article_slug_stmt FROM @add_article_slug;
EXECUTE add_article_slug_stmt;
DEALLOCATE PREPARE add_article_slug_stmt;

-- 幂等索引：伪静态查询列（slug/alias 的解析查询走点查，早期库重跑补齐索引）
CALL sp_create_index_if_not_exists('zhi_article', 'idx_slug', 'CREATE INDEX idx_slug ON zhi_article(slug)');
CALL sp_create_index_if_not_exists('zhi_category', 'idx_alias', 'CREATE INDEX idx_alias ON zhi_category(alias)');

-- 老库升级：补齐 zhi_friend_link.email 列（幂等，友链申请功能）
SET @has_friend_link_email := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_friend_link' AND COLUMN_NAME = 'email');
SET @add_friend_link_email := IF(@has_friend_link_email = 0,
    'ALTER TABLE `zhi_friend_link` ADD COLUMN `email` varchar(100) DEFAULT NULL COMMENT ''申请人邮箱（友链申请）''',
    'SELECT 1');
PREPARE add_friend_link_email_stmt FROM @add_friend_link_email;
EXECUTE add_friend_link_email_stmt;
DEALLOCATE PREPARE add_friend_link_email_stmt;

-- 老库升级：补齐 zhi_message 的 create_by / update_by 列（幂等）
-- 表初建时漏了这两列，而 BlogMessageMapper 的列表查询与审核/回复更新一直按 RuoYi 约定引用它们，
-- 于是「留言管理」列表、审核、回复三处都会抛 Unknown column 'create_by'/'update_by' in 'field list'。
SET @has_message_create_by := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_message' AND COLUMN_NAME = 'create_by');
SET @add_message_create_by := IF(@has_message_create_by = 0,
    'ALTER TABLE `zhi_message` ADD COLUMN `create_by` varchar(64) DEFAULT '''' COMMENT ''创建者'' AFTER `user_agent`',
    'SELECT 1');
PREPARE add_message_create_by_stmt FROM @add_message_create_by;
EXECUTE add_message_create_by_stmt;
DEALLOCATE PREPARE add_message_create_by_stmt;

SET @has_message_update_by := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zhi_message' AND COLUMN_NAME = 'update_by');
SET @add_message_update_by := IF(@has_message_update_by = 0,
    'ALTER TABLE `zhi_message` ADD COLUMN `update_by` varchar(64) DEFAULT '''' COMMENT ''更新者'' AFTER `create_time`',
    'SELECT 1');
PREPARE add_message_update_by_stmt FROM @add_message_update_by;
EXECUTE add_message_update_by_stmt;
DEALLOCATE PREPARE add_message_update_by_stmt;

-- ===============================================================
-- 📌 站内信通知表
-- ===============================================================
CREATE TABLE IF NOT EXISTS zhi_notification (
    id            BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '主键ID',
    recipient_id  BIGINT(20)      NOT NULL                    COMMENT '接收用户ID',
    sender_name   VARCHAR(64)     NOT NULL                    COMMENT '发送者昵称',
    type          VARCHAR(20)     NOT NULL                    COMMENT '通知类型: comment=评论, reply=回复',
    title         VARCHAR(255)    DEFAULT ''                  COMMENT '通知标题',
    content       VARCHAR(500)    DEFAULT ''                  COMMENT '通知内容(评论摘要)',
    article_id    BIGINT(20)      NOT NULL                    COMMENT '关联文章ID',
    article_title VARCHAR(255)    DEFAULT ''                  COMMENT '关联文章标题',
    comment_id    BIGINT(20)      DEFAULT NULL                COMMENT '关联评论ID',
    is_read       INT(1)          DEFAULT 0                   COMMENT '是否已读: 0=未读, 1=已读',
    create_time   DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_recipient_read (recipient_id, is_read),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内信通知表';

-- 「关于」内置页面（slug=about）：页面管理列表中直接管理，前台 /blog/about 仅渲染此页（无其他兜底来源）；
-- 幂等：INSERT IGNORE 依赖 slug 唯一键，重跑不覆盖站长在页面管理里的修改；被逻辑删除的行也会阻止重新插入（删除即删除）
INSERT IGNORE INTO `zhi_page` (`title`, `slug`, `summary`, `content`, `status`, `show_in_nav`, `sort`, `create_by`, `create_time`, `update_time`)
VALUES ('关于', 'about', '', '<p>这是一个基于Spring Boot + Vue.js构建的现代化企业建站系统。</p><p>主要功能包括：</p><ul><li>文章管理</li><li>分类标签</li><li>评论系统</li><li>搜索功能</li></ul>', '1', '0', 0, 'admin', NOW(), NOW());

-- 联系信息
SELECT '📞 技术支持：' AS support_title;
SELECT 'Gitee: https://gitee.com/nevell/zhienterprise' AS gitee;
SELECT 'Email: nevell@foxmail.com' AS email;
SELECT '✅ 初始化脚本执行完成！祝您使用愉快！' AS complete_message;
