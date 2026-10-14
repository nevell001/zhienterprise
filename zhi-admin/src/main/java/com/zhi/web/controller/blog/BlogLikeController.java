package com.zhi.web.controller.blog;

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
import com.zhi.common.utils.BlogSwitchUtils;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.service.IBlogLikeService;
import com.zhi.system.service.IBlogSettingService;

/**
 * 站点前台点赞控制器（文章 + 评论，需登录）
 *
 * @author nevell
 * @date 2026-09-05
 */
@RestController
@RequestMapping("/blog/like")
public class BlogLikeController extends BaseController
{
    @Autowired
    private IBlogLikeService blogLikeService;

    @Autowired
    private IBlogSettingService blogSettingService;

    /**
     * 切换文章点赞
     */
    @RateLimiter(key = "zhi:like:", time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/article/{articleId}")
    public AjaxResult toggleArticleLike(@PathVariable("articleId") Long articleId)
    {
        if (BlogSwitchUtils.isOff(blogSettingService.selectSettingValueByKey("like_enabled")))
        {
            return error("点赞功能已关闭");
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            return error("请先登录");
        }
        return success(blogLikeService.toggleArticleLike(userId, articleId));
    }

    /**
     * 查询文章点赞状态
     */
    @GetMapping("/article/{articleId}/status")
    public AjaxResult getArticleLikeStatus(@PathVariable("articleId") Long articleId)
    {
        Long userId = SecurityUtils.getUserId();
        return success(blogLikeService.getArticleLikeStatus(userId, articleId));
    }

    /**
     * 切换评论点赞
     */
    @RateLimiter(key = "zhi:like:", time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/comment/{commentId}")
    public AjaxResult toggleCommentLike(@PathVariable("commentId") Long commentId)
    {
        if (BlogSwitchUtils.isOff(blogSettingService.selectSettingValueByKey("like_enabled")))
        {
            return error("点赞功能已关闭");
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            return error("请先登录");
        }
        return success(blogLikeService.toggleCommentLike(userId, commentId));
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
        return success(blogLikeService.getLikedCommentIds(userId, ids));
    }
}
