package com.zhi.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 博客标签控制器单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@WebMvcTest(controllers = TagController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.zhi.system.service.ITagService tagService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试获取标签列表接口
     */
    @Test
    void testGetTagList() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Tag> tagList = new ArrayList<>();
        com.zhi.system.domain.Tag tag = new com.zhi.system.domain.Tag();
        tag.setId(1L);
        tag.setName("测试标签");
        tagList.add(tag);

        when(tagService.selectTagList(any(com.zhi.system.domain.Tag.class)))
            .thenReturn(tagList);

        // 执行测试
        mockMvc.perform(get("/system/tag/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].name").value("测试标签"));

        verify(tagService).selectTagList(any(com.zhi.system.domain.Tag.class));
    }

    /**
     * 测试获取标签详情接口
     */
    @Test
    void testGetTagDetail() throws Exception {
        // 模拟数据
        com.zhi.system.domain.Tag tag = new com.zhi.system.domain.Tag();
        tag.setId(1L);
        tag.setName("测试标签");

        when(tagService.selectTagById(1L)).thenReturn(tag);

        // 执行测试
        mockMvc.perform(get("/system/tag/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试标签"));

        verify(tagService).selectTagById(1L);
    }

    /**
     * 测试新增标签接口 - 成功
     */
    @Test
    void testAddTag_Success() throws Exception {
        // 模拟标签名称唯一
        when(tagService.checkTagNameUnique(any(com.zhi.system.domain.Tag.class)))
            .thenReturn(true);
        when(tagService.insertTag(any(com.zhi.system.domain.Tag.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("name", "测试标签");
        params.put("color", "#ff0000");

        // 执行测试
        mockMvc.perform(post("/system/tag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tagService).insertTag(any(com.zhi.system.domain.Tag.class));
    }

    /**
     * 测试新增标签接口 - 名称重复
     */
    @Test
    void testAddTag_DuplicateName() throws Exception {
        // 模拟标签名称重复
        when(tagService.checkTagNameUnique(any(com.zhi.system.domain.Tag.class)))
            .thenReturn(false);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("name", "测试标签");

        // 执行测试
        mockMvc.perform(post("/system/tag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(tagService, never()).insertTag(any(com.zhi.system.domain.Tag.class));
    }

    /**
     * 测试更新标签接口 - 成功
     */
    @Test
    void testEditTag_Success() throws Exception {
        // 模拟标签名称唯一
        when(tagService.checkTagNameUnique(any(com.zhi.system.domain.Tag.class)))
            .thenReturn(true);
        when(tagService.updateTag(any(com.zhi.system.domain.Tag.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("name", "修改后的标签");

        // 执行测试
        mockMvc.perform(put("/system/tag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tagService).updateTag(any(com.zhi.system.domain.Tag.class));
    }

    /**
     * 测试删除标签接口 - 成功
     */
    @Test
    void testRemoveTag_Success() throws Exception {
        // 模拟标签未关联文章
        when(tagService.checkTagExistArticle(anyLong())).thenReturn(false);
        when(tagService.deleteTagByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(delete("/system/tag/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tagService).deleteTagByIds(any(Long[].class));
    }

    /**
     * 测试删除标签接口 - 标签已关联文章
     */
    @Test
    void testRemoveTag_HasArticles() throws Exception {
        // 模拟标签已关联文章
        when(tagService.checkTagExistArticle(anyLong())).thenReturn(true);

        // 执行测试
        mockMvc.perform(delete("/system/tag/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(tagService, never()).deleteTagByIds(any(Long[].class));
    }

    /**
     * 测试获取所有标签接口
     */
    @Test
    void testGetAllTags() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Tag> tagList = new ArrayList<>();
        com.zhi.system.domain.Tag tag = new com.zhi.system.domain.Tag();
        tag.setId(1L);
        tag.setName("测试标签");
        tagList.add(tag);

        when(tagService.selectAllTagList()).thenReturn(tagList);

        // 执行测试
        mockMvc.perform(get("/system/tag/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("测试标签"));

        verify(tagService).selectAllTagList();
    }
}