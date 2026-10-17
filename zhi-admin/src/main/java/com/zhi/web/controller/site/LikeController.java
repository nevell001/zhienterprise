package com.zhi.web.controller.site;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.RateLimiter;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.enums.LimitType;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.service.ILikeService;

/**
 * 站点前台点赞控制器（文章 + 评论，需登录）
 *
 * @author nevell
 * @date 2026-09-05
 */
@RestController
@RequestMapping("/api/like")
public class LikeController extends BaseController
{
    @Autowired
    private ILikeService likeService;

    /**
     * 切换文章点赞
     */
    @RateLimiter(key = "zhi:like:", time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/article/{articleId}")
    public AjaxResult toggleArticleLike(@PathVariable("articleId") Long articleId)
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            return error("请先登录");
        }
        return success(likeService.toggleArticleLike(userId, articleId));
    }

    /**
     * 查询文章点赞状态
     */
    @GetMapping("/article/{articleId}/status")
    public AjaxResult getArticleLikeStatus(@PathVariable("articleId") Long articleId)
    {
        Long userId = SecurityUtils.getUserId();
        return success(likeService.getArticleLikeStatus(userId, articleId));
    }

    /**
     * 切换评论点赞
     */
    @RateLimiter(key = "zhi:like:", time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/comment/{commentId}")
    public AjaxResult toggleCommentLike(@PathVariable("commentId") Long commentId)
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            return error("请先登录");
        }
        return success(likeService.toggleCommentLike(userId, commentId));
    }

    /**
     * 批量查询用户已点赞的评论（用于回显点赞态）
     */
    @GetMapping("/comments/status")
    public AjaxResult getLikedCommentIds(@RequestParam("commentIds") String commentIds)
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null || commentIds == null || commentIds.trim().isEmpty())
        {
            return success(java.util.Collections.emptyList());
        }
        List<String> raw = Arrays.asList(commentIds.split(","));
        List<Long> ids = raw.stream()
                .filter(s -> s != null && s.matches("\\d+"))
                .map(Long::valueOf)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        return success(likeService.getLikedCommentIds(userId, ids));
    }
}
