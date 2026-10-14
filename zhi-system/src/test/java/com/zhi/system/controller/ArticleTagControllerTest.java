package com.zhi.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.page.TableDataInfo;
import com.zhi.system.domain.ArticleTag;
import com.zhi.system.service.IArticleTagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文章标签关联控制器单元测试
 *
 * @author test
 * @date 2025-01-11
 */
@WebMvcTest(ArticleTagController.class)
class ArticleTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IArticleTagService articleTagService;

    private ArticleTag testArticleTag;

    @BeforeEach
    void setUp() {
        testArticleTag = new ArticleTag();
        testArticleTag.setId(1L);
        testArticleTag.setArticleId(1L);
        testArticleTag.setTagId(1L);
    }

    /**
     * 测试查询文章标签关联列表
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:list"})
    void testList() throws Exception {
        // 准备数据
        List<ArticleTag> articleTagList = Arrays.asList(testArticleTag);
        when(articleTagService.selectArticleTagList(any(ArticleTag.class)))
            .thenReturn(articleTagList);

        // 执行测试
        mockMvc.perform(get("/system/articleTag/list")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows").isArray())
            .andExpect(jsonPath("$.rows[0].articleId").value(1L));

        // 验证结果
        when(articleTagService.selectArticleTagList(any(ArticleTag.class)))
            .thenReturn(articleTagList);
    }

    /**
     * 测试查询文章标签关联列表 - 空结果
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:list"})
    void testList_Empty() throws Exception {
        // 模拟空结果
        when(articleTagService.selectArticleTagList(any(ArticleTag.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        mockMvc.perform(get("/system/articleTag/list")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows").isArray())
            .andExpect(jsonPath("$.rows").isEmpty());
    }

    /**
     * 测试通过文章ID查询标签ID列表
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:list"})
    void testGetTagIdsByArticleId() throws Exception {
        // 准备数据
        List<Long> tagIds = Arrays.asList(1L, 2L, 3L);
        when(articleTagService.selectTagIdsByArticleId(1L)).thenReturn(tagIds);

        // 执行测试
        mockMvc.perform(get("/system/articleTag/tags/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0]").value(1L))
            .andExpect(jsonPath("$.data[1]").value(2L))
            .andExpect(jsonPath("$.data[2]").value(3L));
    }

    /**
     * 测试通过文章ID查询标签ID列表 - 空结果
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:list"})
    void testGetTagIdsByArticleId_Empty() throws Exception {
        // 模拟空结果
        when(articleTagService.selectTagIdsByArticleId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        mockMvc.perform(get("/system/articleTag/tags/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 测试通过标签ID查询文章ID列表
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:list"})
    void testGetArticleIdsByTagId() throws Exception {
        // 准备数据
        List<Long> articleIds = Arrays.asList(1L, 2L, 3L);
        when(articleTagService.selectArticleIdsByTagId(1L)).thenReturn(articleIds);

        // 执行测试
        mockMvc.perform(get("/system/articleTag/articles/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0]").value(1L))
            .andExpect(jsonPath("$.data[1]").value(2L))
            .andExpect(jsonPath("$.data[2]").value(3L));
    }

    /**
     * 测试通过标签ID查询文章ID列表 - 空结果
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:list"})
    void testGetArticleIdsByTagId_Empty() throws Exception {
        // 模拟空结果
        when(articleTagService.selectArticleIdsByTagId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        mockMvc.perform(get("/system/articleTag/articles/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 测试获取文章标签关联详细信息
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:query"})
    void testGetInfo() throws Exception {
        // 准备数据
        when(articleTagService.selectArticleTagById(1L)).thenReturn(testArticleTag);

        // 执行测试
        mockMvc.perform(get("/system/articleTag/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.articleId").value(1L))
            .andExpect(jsonPath("$.data.tagId").value(1L));
    }

    /**
     * 测试获取文章标签关联详细信息 - 不存在
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:query"})
    void testGetInfo_NotFound() throws Exception {
        // 模拟数据不存在
        when(articleTagService.selectArticleTagById(999L)).thenReturn(null);

        // 执行测试
        mockMvc.perform(get("/system/articleTag/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试新增文章标签关联
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:add"})
    void testAdd() throws Exception {
        // 准备数据
        when(articleTagService.insertArticleTag(any(ArticleTag.class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(post("/system/articleTag")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testArticleTag)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试新增文章标签关联 - 失败
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:add"})
    void testAdd_Failure() throws Exception {
        // 准备数据
        when(articleTagService.insertArticleTag(any(ArticleTag.class))).thenReturn(0);

        // 执行测试
        mockMvc.perform(post("/system/articleTag")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testArticleTag)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 测试批量新增文章标签关联
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:add"})
    void testBatchAdd() throws Exception {
        // 准备数据
        ArticleTag articleTag1 = new ArticleTag();
        articleTag1.setArticleId(1L);
        articleTag1.setTagId(1L);

        ArticleTag articleTag2 = new ArticleTag();
        articleTag2.setArticleId(1L);
        articleTag2.setTagId(2L);

        List<ArticleTag> articleTagList = Arrays.asList(articleTag1, articleTag2);
        when(articleTagService.batchInsertArticleTag(anyList())).thenReturn(2);

        // 执行测试
        mockMvc.perform(post("/system/articleTag/batch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(articleTagList)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试批量新增文章标签关联 - 空列表
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:add"})
    void testBatchAdd_Empty() throws Exception {
        // 准备数据
        when(articleTagService.batchInsertArticleTag(anyList())).thenReturn(0);

        // 执行测试
        mockMvc.perform(post("/system/articleTag/batch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.emptyList())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 测试修改文章标签关联
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:edit"})
    void testEdit() throws Exception {
        // 准备数据
        when(articleTagService.updateArticleTag(any(ArticleTag.class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(put("/system/articleTag")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testArticleTag)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试修改文章标签关联 - 失败
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:edit"})
    void testEdit_Failure() throws Exception {
        // 准备数据
        when(articleTagService.updateArticleTag(any(ArticleTag.class))).thenReturn(0);

        // 执行测试
        mockMvc.perform(put("/system/articleTag")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testArticleTag)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 测试删除文章标签关联
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:remove"})
    void testRemove() throws Exception {
        // 准备数据
        when(articleTagService.deleteArticleTagByIds(any(Long[].class))).thenReturn(2);

        // 执行测试
        mockMvc.perform(delete("/system/articleTag/1,2")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试通过文章ID删除文章和标签关联
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:remove"})
    void testRemoveByArticleId() throws Exception {
        // 准备数据
        when(articleTagService.deleteByArticleId(1L)).thenReturn(2);

        // 执行测试
        mockMvc.perform(delete("/system/articleTag/article/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试通过文章ID删除文章和标签关联 - 不存在
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:remove"})
    void testRemoveByArticleId_NotFound() throws Exception {
        // 准备数据
        when(articleTagService.deleteByArticleId(999L)).thenReturn(0);

        // 执行测试
        mockMvc.perform(delete("/system/articleTag/article/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 测试查询文章标签关联列表 - 带分页
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"zhi:articleTag:list"})
    void testList_WithPagination() throws Exception {
        // 准备数据
        List<ArticleTag> articleTagList = Arrays.asList(testArticleTag);
        when(articleTagService.selectArticleTagList(any(ArticleTag.class)))
            .thenReturn(articleTagList);

        // 执行测试 - 带分页参数
        mockMvc.perform(get("/system/articleTag/list")
                .param("pageNum", "1")
                .param("pageSize", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows").isArray());
    }
}