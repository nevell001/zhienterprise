-- zhi_comment 的最小 H2 结构（列取自 sql/00_init_database.sql 的 CREATE TABLE zhi_comment）
-- 关键：parent_id 建表默认 0（不是 NULL）——生产插入顶级评论时会省略该列，落库即 0，
-- 所以「顶级评论」判定必须覆盖 is null or = 0，前台评论列表才不会永远为空。
-- sys_user 只保留前台评论查询 left join 需要的 user_id / avatar 两列。

CREATE TABLE IF NOT EXISTS zhi_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT,
    nickname VARCHAR(64),
    email VARCHAR(128),
    content TEXT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    like_count INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avatar VARCHAR(100)
);
