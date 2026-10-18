package com.zhi.web.controller.site;

import com.zhi.system.domain.Article;
import com.zhi.system.domain.Category;
import com.zhi.system.service.IArticleService;
import com.zhi.system.service.ICategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后台预览接口：草稿/定时稿可预览，删除稿拒绝，且不触发前台统计。
 */
class ArticlePreviewControllerTest
{
    private IArticleService articleService;
    private ICategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        articleService = mock(IArticleService.class);
        categoryService = mock(ICategoryService.class);

        ArticlePreviewController controller = new ArticlePreviewController();
        ReflectionTestUtils.setField(controller, "articleService", articleService);
        ReflectionTestUtils.setField(controller, "categoryService", categoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("草稿可预览：返回完整文章结构，不受 status=1 限制")
    void previewDraftReturnsArticle() throws Exception
    {
        Article draft = article(42L, 0L, 0L);
        draft.setTitle("未发布的草稿");
        draft.setContent("<p>草稿正文</p>");
        draft.setSummary("草稿摘要");
        when(articleService.selectArticleById(42L)).thenReturn(draft);

        mockMvc.perform(get("/system/article/preview/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.article.id").value(42))
            .andExpect(jsonPath("$.data.article.title").value("未发布的草稿"))
            .andExpect(jsonPath("$.data.article.status").value(0))
            .andExpect(jsonPath("$.data.article.content").value("<p>草稿正文</p>"))
            .andExpect(jsonPath("$.data.extraInfo").exists());

        verify(articleService, never()).updateArticle(org.mockito.ArgumentMatchers.any(Article.class));
        verify(articleService, never()).addViewCount(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("定时稿可预览，并带出计划发布时间")
    void previewScheduledArticleReturnsPublishTime() throws Exception
    {
        Article scheduled = article(43L, 2L, 0L);
        scheduled.setPublishTime(java.sql.Timestamp.valueOf("2030-01-02 03:04:05"));
        when(articleService.selectArticleById(43L)).thenReturn(scheduled);

        mockMvc.perform(get("/system/article/preview/43"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.article.status").value(2))
            .andExpect(jsonPath("$.data.article.publishTime").exists());
    }

    @Test
    @DisplayName("已删除文章拒绝预览")
    void previewDeletedArticleIsRejected() throws Exception
    {
        when(articleService.selectArticleById(44L)).thenReturn(article(44L, 0L, 2L));

        mockMvc.perform(get("/system/article/preview/44"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("文章已删除"));
    }

    @Test
    @DisplayName("不存在的文章返回明确错误")
    void previewMissingArticleReturnsError() throws Exception
    {
        when(articleService.selectArticleById(45L)).thenReturn(null);

        mockMvc.perform(get("/system/article/preview/45"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("文章不存在"));
    }

    @Test
    @DisplayName("分类信息随预览返回，已删除分类不下发")
    void previewCarriesCategoryOnlyWhenNotDeleted() throws Exception
    {
        Article draft = article(46L, 0L, 0L);
        draft.setCategoryId(7L);
        Category category = new Category();
        category.setId(7L);
        category.setName("行业资讯");
        category.setDelFlag("0");
        when(articleService.selectArticleById(46L)).thenReturn(draft);
        when(categoryService.selectCategoryById(7L)).thenReturn(category);

        mockMvc.perform(get("/system/article/preview/46"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.extraInfo.category.name").value("行业资讯"));

        Category deleted = new Category();
        deleted.setId(7L);
        deleted.setName("已删除分类");
        deleted.setDelFlag("2");
        when(categoryService.selectCategoryById(7L)).thenReturn(deleted);

        mockMvc.perform(get("/system/article/preview/46"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.extraInfo.category").doesNotExist());
    }

    @Test
    @DisplayName("历史脏数据在预览时补齐默认值，页面不出现空白字段")
    void previewFillsNullDefaults() throws Exception
    {
        Article draft = article(47L, 0L, 0L);
        draft.setTitle(null);
        draft.setContent(null);
        draft.setSummary(null);
        draft.setViewCount(null);
        draft.setLikeCount(null);
        draft.setCommentCount(null);
        draft.setIsTop(null);
        draft.setIsRecommend(null);
        draft.setAuthor(null);
        draft.setAuthorName(null);
        when(articleService.selectArticleById(47L)).thenReturn(draft);

        mockMvc.perform(get("/system/article/preview/47"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.article.title").value("无标题文章"))
            .andExpect(jsonPath("$.data.article.content").value("暂无内容"))
            .andExpect(jsonPath("$.data.article.summary").value(""))
            .andExpect(jsonPath("$.data.article.viewCount").value(0))
            .andExpect(jsonPath("$.data.article.likeCount").value(0))
            .andExpect(jsonPath("$.data.article.commentCount").value(0))
            .andExpect(jsonPath("$.data.article.isTop").value(0))
            .andExpect(jsonPath("$.data.article.isRecommend").value(0))
            .andExpect(jsonPath("$.data.article.author").value("未知作者"));
    }

    private Article article(Long id, Long status, Long delFlag)
    {
        Article article = new Article();
        article.setId(id);
        article.setStatus(status);
        article.setDelFlag(delFlag);
        article.setArticleType("news");
        return article;
    }
}
