package com.zhi.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhi.system.domain.Article;
import com.zhi.system.service.IArticleService;
import com.zhi.system.service.ICategoryService;
import com.zhi.system.service.ITagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 博客文章控制器单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@WebMvcTest(controllers = ArticleController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)  // 禁用安全过滤器，跳过权限检查
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IArticleService articleService;

    @MockBean
    private ICategoryService categoryService;

    @MockBean
    private ITagService tagService;

    private ObjectMapper objectMapper;
    private Article testArticle;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // 初始化测试数据
        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("测试文章标题");
        testArticle.setContent("测试文章内容");
        testArticle.setStatus(0L);
    }

    /**
     * 测试获取文章列表接口
     */
    @Test
    void testGetArticleList() throws Exception {
        // 模拟数据
        List<Article> articleList = new ArrayList<>();
        articleList.add(testArticle);
        when(articleService.selectArticleList(any(Article.class))).thenReturn(articleList);

        // 执行测试
        mockMvc.perform(get("/system/article/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].title").value("测试文章标题"));

        verify(articleService).selectArticleList(any(Article.class));
    }

    /**
     * 测试获取文章详情接口
     */
    @Test
    void testGetArticleDetail() throws Exception {
        // 模拟数据
        when(articleService.selectArticleById(1L)).thenReturn(testArticle);

        // 执行测试 - 使用正确的端点路径 /{id}
        mockMvc.perform(get("/system/article/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("测试文章标题"));

        verify(articleService).selectArticleById(1L);
    }

    /**
     * 测试新增文章接口
     */
    @Test
    void testAddArticle() throws Exception {
        // 模拟成功添加
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        // 准备请求体 - 使用Map因为实际接口接收Map参数
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);

        // 执行测试 - 使用正确的端点路径 POST /
        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).insertArticle(any(Article.class));
    }

    /**
     * 测试更新文章接口
     */
    @Test
    void testEditArticle() throws Exception {
        // 模拟成功更新
        when(articleService.updateArticle(any(Article.class))).thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);

        // 执行测试 - 使用正确的端点路径 PUT /
        mockMvc.perform(put("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).updateArticle(any(Article.class));
    }

    /**
     * 测试删除文章接口
     */
    @Test
    void testRemoveArticle() throws Exception {
        // 模拟成功删除
        when(articleService.deleteArticleByIds(any(Long[].class))).thenReturn(1);

        // 执行测试 - 使用正确的端点路径 DELETE /{ids}
        mockMvc.perform(delete("/system/article/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).deleteArticleByIds(any(Long[].class));
    }

    /**
     * 回收站：批量恢复
     */
    @Test
    void testRestoreArticles() throws Exception {
        when(articleService.restoreArticleByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(put("/system/article/restore/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).restoreArticleByIds(any(Long[].class));
    }

    /**
     * 回收站：彻底删除（物理删除）
     */
    @Test
    void testPurgeArticles() throws Exception {
        when(articleService.purgeArticleByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/system/article/purge/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).purgeArticleByIds(any(Long[].class));
    }

    /**
     * 测试更新文章状态接口
     */
    @Test
    void testChangeStatus() throws Exception {
        // 模拟成功更新状态
        when(articleService.updateArticleStatus(anyList(), anyInt())).thenReturn(1);

        // 准备请求体
        Map<String, Object> articleStatus = new HashMap<>();
        // 模拟真实 JSON 反序列化：数字是 Integer，不是 Long
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        articleStatus.put("ids", ids);
        articleStatus.put("status", 1);

        // 控制器必须转成 List<Long>，否则 service 迭代会 ClassCastException
        when(articleService.updateArticleStatus(anyList(), anyInt())).thenAnswer(invocation -> {
            List<Long> passed = invocation.getArgument(0);
            assertThat(passed).containsExactly(1L);
            return 1;
        });

        // 执行测试 - 使用正确的端点路径 PUT /status
        mockMvc.perform(put("/system/article/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(articleStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).updateArticleStatus(anyList(), eq(1));
    }

    /**
     * 回归：局部更新（如列表「发布/转草稿」）不带 format 时，不得把文章格式改成 html。
     * 后端 mapper 仅在 format != null 时更新；控制器若默认写 html，Markdown 文章会被静默转换。
     */
    @Test
    void testEditWithoutFormatLeavesFormatUnset() throws Exception {
        when(articleService.updateArticle(any(Article.class))).thenReturn(1);

        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("title", "测试文章标题");
        params.put("status", 1L);

        mockMvc.perform(put("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleService).updateArticle(captor.capture());
        assertThat(captor.getValue().getFormat()).isNull();
    }

    /**
     * 回归：状态接口缺 status 时应返回明确错误，而不是静默更新 0 行。
     */
    @Test
    void testChangeStatusRejectsMissingStatus() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", List.of(1));

        mockMvc.perform(put("/system/article/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("状态不能为空"));

        verify(articleService, never()).updateArticleStatus(anyList(), anyInt());
    }

    /**
     * 测试搜索文章接口
     */    /**
     * 测试搜索文章接口
     */
    @Test
    void testSearch() throws Exception {
        // 模拟数据
        List<Article> articleList = new ArrayList<>();
        articleList.add(testArticle);
        when(articleService.searchArticles(anyString(), any(Article.class))).thenReturn(articleList);

        // 执行测试
        mockMvc.perform(get("/system/article/search")
                .param("keyword", "测试")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].title").value("测试文章标题"));

        verify(articleService).searchArticles(anyString(), any(Article.class));
    }

    /**
     * 测试获取分类和标签选项接口
     */
    @Test
    void testGetOptions() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Category> categories = new ArrayList<>();
        com.zhi.system.domain.Category category = new com.zhi.system.domain.Category();
        category.setId(1L);
        category.setName("测试分类");
        categories.add(category);

        List<com.zhi.system.domain.Tag> tags = new ArrayList<>();
        com.zhi.system.domain.Tag tag = new com.zhi.system.domain.Tag();
        tag.setId(1L);
        tag.setName("测试标签");
        tags.add(tag);

        when(categoryService.selectCategoryList(any(com.zhi.system.domain.Category.class))).thenReturn(categories);
        when(tagService.selectTagList(any(com.zhi.system.domain.Tag.class))).thenReturn(tags);

        // 执行测试
        mockMvc.perform(get("/system/article/options")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.tags").isArray());

        verify(categoryService).selectCategoryList(any(com.zhi.system.domain.Category.class));
        verify(tagService).selectTagList(any(com.zhi.system.domain.Tag.class));
    }

    /**
     * 测试根据分类获取文章接口
     */
    @Test
    void testGetByCategory() throws Exception {
        // 模拟数据
        List<Article> articleList = new ArrayList<>();
        articleList.add(testArticle);
        when(articleService.selectArticleList(any(Article.class))).thenReturn(articleList);

        // 执行测试
        mockMvc.perform(get("/system/article/category/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        verify(articleService).selectArticleList(any(Article.class));
    }

    /**
     * 测试根据分类获取文章接口 - 空结果
     */
    @Test
    void testGetByCategory_Empty() throws Exception {
        // 模拟空结果
        when(articleService.selectArticleList(any(Article.class))).thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(get("/system/article/category/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isEmpty());

        verify(articleService).selectArticleList(any(Article.class));
    }

    /**
     * 测试根据标签获取文章接口
     */
    @Test
    void testGetByTag() throws Exception {
        // 模拟数据
        List<Article> articleList = new ArrayList<>();
        articleList.add(testArticle);
        when(articleService.selectArticlesByTagId(1L)).thenReturn(articleList);

        // 执行测试
        mockMvc.perform(get("/system/article/tag/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.rows[0].title").value("测试文章标题"));

        verify(articleService).selectArticlesByTagId(1L);
    }

    /**
     * 测试根据标签获取文章接口 - 空结果
     */
    @Test
    void testGetByTag_Empty() throws Exception {
        // 模拟空结果
        when(articleService.selectArticlesByTagId(1L)).thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(get("/system/article/tag/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isEmpty());

        verify(articleService).selectArticlesByTagId(1L);
    }

    /**
     * 测试获取文章详情接口 - 文章不存在
     */
    @Test
    void testGetArticleDetail_NotFound() throws Exception {
        // 模拟文章不存在
        when(articleService.selectArticleById(999L)).thenReturn(null);

        // 执行测试
        mockMvc.perform(get("/system/article/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(articleService).selectArticleById(999L);
    }

    /**
     * 测试新增文章接口 - 失败
     */
    @Test
    void testAddArticle_Failure() throws Exception {
        // 模拟添加失败
        when(articleService.insertArticle(any(Article.class))).thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);

        // 执行测试
        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(articleService).insertArticle(any(Article.class));
    }

    /**
     * 测试更新文章接口 - 失败
     */
    @Test
    void testEditArticle_Failure() throws Exception {
        // 模拟更新失败
        when(articleService.updateArticle(any(Article.class))).thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);

        // 执行测试
        mockMvc.perform(put("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(articleService).updateArticle(any(Article.class));
    }

    /**
     * 测试更新文章状态接口 - 空ID列表
     */
    @Test
    void testChangeStatus_EmptyIds() throws Exception {
        // 准备请求体 - 空ID列表
        Map<String, Object> articleStatus = new HashMap<>();
        articleStatus.put("ids", new ArrayList<>());
        articleStatus.put("status", 1);

        // 执行测试
        mockMvc.perform(put("/system/article/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(articleStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 测试搜索文章接口 - 空结果
     */
    @Test
    void testSearch_Empty() throws Exception {
        // 模拟空结果
        when(articleService.searchArticles(anyString(), any(Article.class))).thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(get("/system/article/search")
                .param("keyword", "不存在的关键词")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isEmpty());

        verify(articleService).searchArticles(anyString(), any(Article.class));
    }

    /**
     * 测试新增文章接口 - 带标签
     */
    @Test
    void testAddArticle_WithTags() throws Exception {
        // 模拟成功添加
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        // 准备请求体 - 包含标签
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);
        List<Long> tagIds = new ArrayList<>();
        tagIds.add(1L);
        tagIds.add(2L);
        params.put("tagIds", tagIds);

        // 执行测试
        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).insertArticle(any(Article.class));
    }

    /**
     * 测试新增文章接口 - 带标签（Integer类型）
     */
    @Test
    void testAddArticle_WithTagsInteger() throws Exception {
        // 模拟成功添加
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        // 准备请求体 - 包含标签（Integer类型）
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);
        List<Integer> tagIds = new ArrayList<>();
        tagIds.add(1);
        tagIds.add(2);
        params.put("tagIds", tagIds);

        // 执行测试
        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).insertArticle(any(Article.class));
    }

    /**
     * 测试新增文章接口 - 带标签（String类型）
     */
    @Test
    void testAddArticle_WithTagsString() throws Exception {
        // 模拟成功添加
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        // 准备请求体 - 包含标签（String类型）
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);
        List<String> tagIds = new ArrayList<>();
        tagIds.add("1");
        tagIds.add("2");
        params.put("tagIds", tagIds);

        // 执行测试
        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).insertArticle(any(Article.class));
    }

    /**
     * 测试新增文章接口 - 带无效标签
     */
    @Test
    void testAddArticle_WithInvalidTags() throws Exception {
        // 模拟成功添加
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        // 准备请求体 - 包含无效标签
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);
        List<String> tagIds = new ArrayList<>();
        tagIds.add("invalid");
        tagIds.add("2");
        params.put("tagIds", tagIds);

        // 执行测试
        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).insertArticle(any(Article.class));
    }

    /**
     * 测试更新文章接口 - 带标签
     */
    @Test
    void testEditArticle_WithTags() throws Exception {
        // 模拟成功更新
        when(articleService.updateArticle(any(Article.class))).thenReturn(1);

        // 准备请求体 - 包含标签
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);
        List<Long> tagIds = new ArrayList<>();
        tagIds.add(1L);
        tagIds.add(2L);
        params.put("tagIds", tagIds);

        // 执行测试
        mockMvc.perform(put("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).updateArticle(any(Article.class));
    }

    /**
     * 测试新增文章接口 - 带所有可选字段
     */
    @Test
    void testAddArticle_WithAllFields() throws Exception {
        // 模拟成功添加
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        // 准备请求体 - 包含所有字段
        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("summary", "测试摘要");
        params.put("content", "测试文章内容");
        params.put("coverUrl", "http://example.com/cover.jpg");
        params.put("categoryId", 1L);
        params.put("status", 0L);
        params.put("isTop", 1L);
        params.put("isRecommend", 0L);
        params.put("authorId", 1L);
        params.put("authorName", "测试作者");

        // 执行测试
        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(articleService).insertArticle(any(Article.class));
    }

    /**
     * 服务层异常必须原样冒泡出去，交给 GlobalExceptionHandler 统一转成错误响应。
     * <p>
     * 控制器早前的 try/catch + {@code code=1} 形态已被移除，这三个用例断言的正是
     * 「不在控制器内吞异常」这条契约；转换后的响应形态由 zhi-framework 的
     * {@code GlobalExceptionHandlerTest} 断言（跨模块，此处无法引用该处理器）。
     */
    @Test
    void updateStatus_propagatesServiceFailureToGlobalHandler() throws Exception {
        when(articleService.updateArticleStatus(anyList(), anyInt()))
                .thenThrow(new RuntimeException("数据库错误"));

        Map<String, Object> articleStatus = new HashMap<>();
        articleStatus.put("ids", List.of(1L));
        articleStatus.put("status", 1);

        Throwable thrown = catchThrowable(() -> mockMvc.perform(put("/system/article/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(articleStatus))));

        assertThat(rootCause(thrown)).isInstanceOf(RuntimeException.class).hasMessage("数据库错误");
        verify(articleService).updateArticleStatus(anyList(), anyInt());
    }

    @Test
    void add_propagatesServiceFailureToGlobalHandler() throws Exception {
        when(articleService.insertArticle(any(Article.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);

        Throwable thrown = catchThrowable(() -> mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params))));

        assertThat(rootCause(thrown)).isInstanceOf(RuntimeException.class).hasMessage("数据库错误");
        verify(articleService).insertArticle(any(Article.class));
    }

    @Test
    void edit_propagatesServiceFailureToGlobalHandler() throws Exception {
        when(articleService.updateArticle(any(Article.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容");
        params.put("status", 0L);

        Throwable thrown = catchThrowable(() -> mockMvc.perform(put("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params))));

        assertThat(rootCause(thrown)).isInstanceOf(RuntimeException.class).hasMessage("数据库错误");
        verify(articleService).updateArticle(any(Article.class));
    }

    /**
     * 后台保存必须携带内容类型与别名/SEO：解析层早前漏掉这几个字段，
     * 核心业务管理发布的内容于是落库成 news（article_type 走 DDL 默认值），
     * 别名与 SEO 三字段静默丢弃。
     */
    @Test
    void add_mapsArticleTypeSlugAndSeoFields() throws Exception {
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        Map<String, Object> params = new HashMap<>();
        params.put("title", "核心业务发布测试");
        params.put("content", "测试文章内容足够长");
        params.put("status", 1L);
        params.put("articleType", "business");
        params.put("slug", "ops-hosting");
        params.put("seoTitle", "SEO 标题");
        params.put("seoKeywords", "关键词一,关键词二");
        params.put("seoDescription", "SEO 描述");

        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleService).insertArticle(captor.capture());
        Article saved = captor.getValue();
        assertThat(saved.getArticleType()).isEqualTo("business");
        assertThat(saved.getSlug()).isEqualTo("ops-hosting");
        assertThat(saved.getSeoTitle()).isEqualTo("SEO 标题");
        assertThat(saved.getSeoKeywords()).isEqualTo("关键词一,关键词二");
        assertThat(saved.getSeoDescription()).isEqualTo("SEO 描述");
    }

    /**
     * 内容类型只接受白名单值，非法值不落库（插入时由 mapper 回退为默认 news）。
     */
    @Test
    void add_ignoresArticleTypeOutsideWhitelist() throws Exception {
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        Map<String, Object> params = new HashMap<>();
        params.put("title", "测试文章标题");
        params.put("content", "测试文章内容足够长");
        params.put("articleType", "video");

        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleService).insertArticle(captor.capture());
        assertThat(captor.getValue().getArticleType()).isNull();
    }

    private static Throwable rootCause(Throwable thrown) {
        Throwable current = thrown;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * 解决方案筛选维度与业务详情结构化字段必须随保存落库：
     * parseArticleFromParams 是逐字段白名单，漏映射的字段会被静默丢弃。
     */
    @Test
    void add_mapsSolutionDimensionsAndStructuredFields() throws Exception {
        when(articleService.insertArticle(any(Article.class))).thenReturn(1);

        String sellingPoints = "[{\"title\":\"开箱即用\",\"desc\":\"快速部署\"}]";
        String specTable = "[{\"name\":\"并发\",\"value\":\"1000\"}]";
        String gallery = "[\"/profile/a.jpg\"]";
        String processSteps = "[{\"title\":\"诊断\",\"desc\":\"现状诊断\"}]";

        Map<String, Object> params = new HashMap<>();
        params.put("title", "解决方案发布测试");
        params.put("content", "测试文章内容足够长");
        params.put("status", 1L);
        params.put("articleType", "solution");
        params.put("industry", "制造业");
        params.put("scenario", "数字化转型,降本增效");
        params.put("sellingPoints", sellingPoints);
        params.put("specTable", specTable);
        params.put("gallery", gallery);
        params.put("processSteps", processSteps);

        mockMvc.perform(post("/system/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleService).insertArticle(captor.capture());
        Article saved = captor.getValue();
        assertThat(saved.getArticleType()).isEqualTo("solution");
        assertThat(saved.getIndustry()).isEqualTo("制造业");
        assertThat(saved.getScenario()).isEqualTo("数字化转型,降本增效");
        assertThat(saved.getSellingPoints()).isEqualTo(sellingPoints);
        assertThat(saved.getSpecTable()).isEqualTo(specTable);
        assertThat(saved.getGallery()).isEqualTo(gallery);
        assertThat(saved.getProcessSteps()).isEqualTo(processSteps);
    }
}