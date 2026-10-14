package com.zhi.web.controller.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.system.service.IArticleService;
import com.zhi.system.service.ICategoryService;
import com.zhi.system.service.ITagService;
import com.zhi.system.service.ICommentService;
import com.zhi.system.service.ISysUserService;
import com.zhi.system.domain.Article;
import com.zhi.system.domain.Category;
import com.zhi.system.domain.Tag;
import com.zhi.system.domain.Comment;
import com.zhi.common.core.domain.entity.SysUser;
import java.util.HashMap;
import java.util.Map;

/**
 * 统计分析控制器
 */
@RestController
@RequestMapping("/system-stats")
public class SystemStatsController {

    @Autowired
    private IArticleService articleService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private ITagService tagService;

    @Autowired
    private ICommentService commentService;

    @Autowired
    private ISysUserService userService;

    /**
     * 获取系统概览统计信息
     */
    @PreAuthorize("@ss.hasPermi('statistics:overview:list')")
    @GetMapping("/overview")
    public AjaxResult getOverview() {
        Map<String, Object> data = new HashMap<>();

        // 获取文章总数
        long articleCount = articleService.selectArticleCount(new Article());

        // 获取分类总数（使用列表大小）
        long categoryCount = categoryService.selectCategoryList(new Category()).size();

        // 获取标签总数（使用列表大小）
        long tagCount = tagService.selectTagList(new Tag()).size();

        // 获取评论总数
        long commentCount = commentService.selectCommentCount(new Comment());

        // 获取总浏览量
        long totalViews = articleService.selectTotalViewCount();

        // 获取注册用户总数（仅统计正常状态、未删除的用户）
        SysUser userQuery = new SysUser();
        userQuery.setStatus("0");
        long userCount = userService.selectUserCount(userQuery);

        data.put("articleCount", articleCount);
        data.put("categoryCount", categoryCount);
        data.put("tagCount", tagCount);
        data.put("commentCount", commentCount);
        data.put("viewCount", totalViews);
        data.put("totalViews", totalViews); // 兼容旧字段名
        data.put("userCount", userCount);

        return AjaxResult.success(data);
    }
}