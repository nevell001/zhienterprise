-- 媒体库引用检查所需的最小 H2 结构（列取自 sql/00_init_database.sql）
CREATE TABLE IF NOT EXISTS zhi_page (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200),
    content LONGTEXT,
    sections LONGTEXT,
    del_flag CHAR(1) DEFAULT '0'
);

CREATE TABLE IF NOT EXISTS zhi_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100),
    config_value LONGTEXT
);

CREATE TABLE IF NOT EXISTS zhi_friend_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    logo VARCHAR(500),
    del_flag CHAR(1) DEFAULT '0'
);
