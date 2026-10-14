-- 创建博客评论表
CREATE TABLE `zhi_comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint(20) NOT NULL COMMENT '文章ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父评论ID(0表示一级评论)',
  `reply_user_id` bigint(20) DEFAULT NULL COMMENT '回复用户ID',
  `status` char(1) DEFAULT '0' COMMENT '状态(0待审核,1已发布,2已删除)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客评论表';