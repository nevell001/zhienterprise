-- H2 没有 MySQL 的 DATE_FORMAT，归档等 SQL 依赖它，用别名指到测试内的兼容实现
CREATE ALIAS IF NOT EXISTS DATE_FORMAT FOR "com.zhi.system.testsupport.H2MysqlCompat.dateFormat";

-- 博客文章表
CREATE TABLE IF NOT EXISTS zhi_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content LONGTEXT,
    content_md LONGTEXT,
    format VARCHAR(10) DEFAULT 'html',
    article_type VARCHAR(20) NOT NULL DEFAULT 'news',
    seo_title VARCHAR(200),
    seo_keywords VARCHAR(255),
    seo_description VARCHAR(500),
    slug VARCHAR(100),
    cover_url VARCHAR(500),
    category_id BIGINT,
    author_id BIGINT,
    author VARCHAR(100),
    author_name VARCHAR(100),
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    comment_count BIGINT DEFAULT 0,
    status BIGINT DEFAULT 0,
    publish_time TIMESTAMP NULL,
    is_top BIGINT DEFAULT 0,
    is_recommend BIGINT DEFAULT 0,
    is_hot BIGINT DEFAULT 0,
    is_original BIGINT DEFAULT 1,
    source_url VARCHAR(500),
    del_flag CHAR(1) DEFAULT '0',
    create_by VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500)
);

-- 博客分类表
CREATE TABLE IF NOT EXISTS zhi_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    del_flag CHAR(1) DEFAULT '0',
    create_by VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500)
);

-- 博客标签表
CREATE TABLE IF NOT EXISTS zhi_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    del_flag CHAR(1) DEFAULT '0',
    create_by VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500)
);

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS zhi_article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id)
);
