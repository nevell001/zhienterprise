package com.zhi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 点赞Mapper接口（文章点赞 + 评论点赞）
 *
 * @author nevell
 * @date 2026-09-05
 */
public interface LikeMapper {

    // ---------- 文章点赞 ----------

    public int insertArticleLike(@Param("articleId") Long articleId, @Param("userId") Long userId);

    public int deleteArticleLike(@Param("articleId") Long articleId, @Param("userId") Long userId);

    public int existsArticleLike(@Param("articleId") Long articleId, @Param("userId") Long userId);

    /** 读取文章当前点赞数字段（含历史基数） */
    public Long selectArticleLikeCount(@Param("articleId") Long articleId);

    public int changeArticleLikeCount(@Param("articleId") Long articleId, @Param("delta") int delta);

    // ---------- 评论点赞 ----------

    public int insertCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    public int deleteCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    public int existsCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /** 读取评论当前点赞数字段 */
    public Long selectCommentLikeCount(@Param("commentId") Long commentId);

    public int changeCommentLikeCount(@Param("commentId") Long commentId, @Param("delta") int delta);

    /** 查询用户已点赞的评论ID集合 */
    public List<Long> selectLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);
}
