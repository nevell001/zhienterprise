package com.zhi.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zhi.system.mapper.LikeMapper;
import com.zhi.system.service.ILikeService;

/**
 * 点赞Service实现（文章点赞 + 评论点赞）
 *
 * <p>以 zhi_article_like / zhi_comment_like 记录表为准做幂等去重与取消，
 * zhi_article.like_count / zhi_comment.like_count 作为展示计数同步增减。</p>
 *
 * @author nevell
 * @date 2026-09-05
 */
@Service
public class LikeServiceImpl implements ILikeService
{
    @Autowired
    private LikeMapper likeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleArticleLike(Long userId, Long articleId)
    {
        Map<String, Object> result = new HashMap<>();
        if (userId == null || articleId == null)
        {
            result.put("liked", false);
            result.put("likeCount", 0L);
            return result;
        }

        // 文章不存在时不落点赞记录
        Long current = likeMapper.selectArticleLikeCount(articleId);
        if (current == null)
        {
            result.put("liked", false);
            result.put("likeCount", 0L);
            return result;
        }

        boolean exists = likeMapper.existsArticleLike(articleId, userId) > 0;
        boolean liked;
        if (exists)
        {
            likeMapper.deleteArticleLike(articleId, userId);
            likeMapper.changeArticleLikeCount(articleId, -1);
            liked = false;
        }
        else
        {
            likeMapper.insertArticleLike(articleId, userId);
            likeMapper.changeArticleLikeCount(articleId, 1);
            liked = true;
        }

        result.put("liked", liked);
        result.put("likeCount", safeCount(likeMapper.selectArticleLikeCount(articleId)));
        return result;
    }

    @Override
    public Map<String, Object> getArticleLikeStatus(Long userId, Long articleId)
    {
        Map<String, Object> result = new HashMap<>();
        boolean liked = userId != null && articleId != null
                && likeMapper.existsArticleLike(articleId, userId) > 0;
        result.put("liked", liked);
        result.put("likeCount", safeCount(likeMapper.selectArticleLikeCount(articleId)));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleCommentLike(Long userId, Long commentId)
    {
        Map<String, Object> result = new HashMap<>();
        if (userId == null || commentId == null)
        {
            result.put("liked", false);
            result.put("likeCount", 0L);
            return result;
        }

        Long current = likeMapper.selectCommentLikeCount(commentId);
        if (current == null)
        {
            result.put("liked", false);
            result.put("likeCount", 0L);
            return result;
        }

        boolean exists = likeMapper.existsCommentLike(commentId, userId) > 0;
        boolean liked;
        if (exists)
        {
            likeMapper.deleteCommentLike(commentId, userId);
            likeMapper.changeCommentLikeCount(commentId, -1);
            liked = false;
        }
        else
        {
            likeMapper.insertCommentLike(commentId, userId);
            likeMapper.changeCommentLikeCount(commentId, 1);
            liked = true;
        }

        result.put("liked", liked);
        result.put("likeCount", safeCount(likeMapper.selectCommentLikeCount(commentId)));
        return result;
    }

    @Override
    public List<Long> getLikedCommentIds(Long userId, List<Long> commentIds)
    {
        if (userId == null || commentIds == null || commentIds.isEmpty())
        {
            return java.util.Collections.emptyList();
        }
        return likeMapper.selectLikedCommentIds(userId, commentIds);
    }

    private long safeCount(Long count)
    {
        return count == null ? 0L : count.longValue();
    }
}
