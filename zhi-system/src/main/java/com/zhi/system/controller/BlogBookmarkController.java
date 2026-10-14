package com.zhi.system.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.zhi.common.annotation.RateLimiter;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.enums.LimitType;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.service.IBlogBookmarkService;

/**
 * 文章收藏Controller
 *
 * @author nevell
 * @date 2026-01-18
 */
@RestController
@RequestMapping("/api/bookmark")
public class BlogBookmarkController extends BaseController {

    @Autowired
    private IBlogBookmarkService blogBookmarkService;

    /**
     * 切换收藏状态
     */
    @RateLimiter(key = "zhi:bookmark:", time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/toggle/{articleId}")
    public AjaxResult toggleBookmark(@PathVariable("articleId") Long articleId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return error("请先登录");
        }

        int result = blogBookmarkService.toggleBookmark(userId, articleId);
        Map<String, Object> data = new HashMap<>();
        data.put("bookmarked", result == 1);
        return success(data);
    }

    /**
     * 检查收藏状态
     */
    @GetMapping("/check/{articleId}")
    public AjaxResult checkBookmark(@PathVariable("articleId") Long articleId) {
        Long userId = SecurityUtils.getUserId();
        boolean isBookmarked = false;
        if (userId != null) {
            isBookmarked = blogBookmarkService.isBookmarked(userId, articleId);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("bookmarked", isBookmarked);
        return success(data);
    }

    /**
     * 获取用户收藏列表（联查文章信息）
     */
    @GetMapping("/list")
    public AjaxResult listBookmarks() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return error("请先登录");
        }

        List<Map<String, Object>> list = blogBookmarkService.selectBookmarkArticlesByUserId(userId);
        return success(list);
    }
}
