package com.zhi.system.service;

import java.util.List;
import java.util.Map;

/**
 * 点赞Service接口（文章点赞 + 评论点赞）
 *
 * @author nevell
 * @date 2026-09-05
 */
public interface ILikeService {

    /**
     * 切换文章点赞状态
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return {liked, likeCount}
     */
    public Map<String, Object> toggleArticleLike(Long userId, Long articleId);

    /**
     * 查询当前用户对文章的点赞状态
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return {liked, likeCount}
     */
    public Map<String, Object> getArticleLikeStatus(Long userId, Long articleId);

    /**
     * 切换评论点赞状态
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return {liked, likeCount}
     */
    public Map<String, Object> toggleCommentLike(Long userId, Long commentId);

    /**
     * 查询用户已点赞的评论ID集合
     *
     * @param userId 用户ID
     * @param commentIds 评论ID列表
     * @return 已点赞的评论ID集合
     */
    public List<Long> getLikedCommentIds(Long userId, List<Long> commentIds);
}
