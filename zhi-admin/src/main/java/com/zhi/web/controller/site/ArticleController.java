package com.zhi.web.controller.site;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zhi.common.annotation.Anonymous;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.page.TableDataInfo;
import com.zhi.system.domain.Article;
import com.zhi.system.service.IArticleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文章前台访问控制器
 * 提供前台文章匿名访问接口
 *
 * @author nevell
 * @date 2025-12-18
 */
@Tag(name = "文章管理（前台）")
@Anonymous
@RestController("frontArticleController")
@RequestMapping("/common/site/article")
public class ArticleController extends BaseController {

    @Autowired
    private IArticleService articleService;

    /**
     * 获取置顶文章列表
     */
    @Operation(summary = "获取置顶文章列表")
    @GetMapping("/top")
    public TableDataInfo getTopArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "5") Integer pageSize) {
        try {
            startPage();
            
            // 构建查询条件：只查询置顶且已发布的文章
            Article article = new Article();
            article.setIsTop(1L);
            article.setStatus(1L); // 只显示已发布的文章

            List<Article> list = articleService.selectArticleList(article);
            return getDataTable(list);
        } catch (Exception e) {
            logger.error("获取置顶文章列表失败", e);
            return getDataTable(new ArrayList<>());
        }
    }

    /**
     * 获取推荐文章列表
     */
    @Operation(summary = "获取推荐文章列表")
    @GetMapping("/recommend")
    public TableDataInfo getRecommendArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "5") Integer pageSize) {
        try {
            startPage();
            
            // 构建查询条件：只查询推荐且已发布的文章
            Article article = new Article();
            article.setIsRecommend(1L);
            article.setStatus(1L); // 只显示已发布的文章

            List<Article> list = articleService.selectArticleList(article);
            return getDataTable(list);
        } catch (Exception e) {
            logger.error("获取推荐文章列表失败", e);
            return getDataTable(new ArrayList<>());
        }
    }
}