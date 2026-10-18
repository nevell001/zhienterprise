package com.zhi.web.controller.site;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.system.domain.Article;
import com.zhi.system.domain.Category;
import com.zhi.system.service.IArticleService;
import com.zhi.system.service.ICategoryService;

/**
 * 未发布内容预览：草稿与定时稿按前台真实布局渲染。
 *
 * <p>公开详情接口只放行 status=1 且会累计浏览量；预览必须跳过状态门槛和统计，
 * 同时只对拥有文章查询权限的后台用户开放。删除的文章仍然拒绝预览。</p>
 *
 * @author nevell
 */
@RestController
@RequestMapping("/system/article/preview")
public class ArticlePreviewController extends BaseController
{
    @Autowired
    private IArticleService articleService;

    @Autowired
    private ICategoryService categoryService;

    /**
     * 预览文章（草稿 / 定时 / 已发布均可），返回结构与前台详情一致
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:query')")
    @GetMapping("/{id}")
    public AjaxResult preview(@PathVariable("id") Long id)
    {
        Article article = articleService.selectArticleById(id);
        if (article == null)
        {
            return error("文章不存在");
        }
        Long delFlag = article.getDelFlag();
        if (delFlag != null && delFlag.longValue() != 0L)
        {
            return error("文章已删除");
        }

        // 与前台详情保持同样的兜底，避免预览时因历史脏数据出现空白字段
        if (article.getViewCount() == null) article.setViewCount(0L);
        if (article.getLikeCount() == null) article.setLikeCount(0L);
        if (article.getCommentCount() == null) article.setCommentCount(0L);
        if (article.getIsTop() == null) article.setIsTop(0L);
        if (article.getIsRecommend() == null) article.setIsRecommend(0L);
        if (article.getTitle() == null) article.setTitle("无标题文章");
        if (article.getContent() == null) article.setContent("暂无内容");
        if (article.getSummary() == null) article.setSummary("");
        if (article.getAuthor() == null) article.setAuthor("未知作者");
        if (article.getAuthorName() == null) article.setAuthorName(article.getAuthor());

        Map<String, Object> extraInfo = new HashMap<>();
        try
        {
            if (article.getCategoryId() != null)
            {
                Category category = categoryService.selectCategoryById(article.getCategoryId());
                if (category != null && "0".equals(category.getDelFlag()))
                {
                    extraInfo.put("category", category);
                }
            }
        }
        catch (Exception e)
        {
            logger.warn("预览文章时获取分类信息出错: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("article", article);
        result.put("extraInfo", extraInfo);
        return success(result);
    }
}
