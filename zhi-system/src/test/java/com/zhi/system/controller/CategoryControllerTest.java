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
 * 文章分类控制器单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@WebMvcTest(controllers = CategoryController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.zhi.system.service.ICategoryService categoryService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试获取分类列表接口
     */
    @Test
    void testGetCategoryList() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Category> categoryList = new ArrayList<>();
        com.zhi.system.domain.Category category = new com.zhi.system.domain.Category();
        category.setId(1L);
        category.setName("测试分类");
        categoryList.add(category);

        when(categoryService.selectCategoryListForFront(any(com.zhi.system.domain.Category.class)))
            .thenReturn(categoryList);

        // 执行测试 - delFlag 为 null，触发自动设置为 "0" 的分支
        mockMvc.perform(get("/system/category/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].name").value("测试分类"));

        verify(categoryService).selectCategoryListForFront(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试获取分类列表接口 - 带参数
     */
    @Test
    void testGetCategoryList_WithParams() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Category> categoryList = new ArrayList<>();
        com.zhi.system.domain.Category category = new com.zhi.system.domain.Category();
        category.setId(1L);
        category.setName("测试分类");
        categoryList.add(category);

        when(categoryService.selectCategoryListForFront(any(com.zhi.system.domain.Category.class)))
            .thenReturn(categoryList);

        // 执行测试 - delFlag 已设置，不触发自动设置分支
        mockMvc.perform(get("/system/category/list")
                .param("delFlag", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(categoryService).selectCategoryListForFront(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试获取分类列表接口 - 空结果
     */
    @Test
    void testGetCategoryList_Empty() throws Exception {
        // 模拟空结果
        when(categoryService.selectCategoryListForFront(any(com.zhi.system.domain.Category.class)))
            .thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(get("/system/category/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isEmpty());

        verify(categoryService).selectCategoryListForFront(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试获取分类详情接口
     */
    @Test
    void testGetCategoryDetail() throws Exception {
        // 模拟数据
        com.zhi.system.domain.Category category = new com.zhi.system.domain.Category();
        category.setId(1L);
        category.setName("测试分类");

        when(categoryService.selectCategoryById(1L)).thenReturn(category);

        // 执行测试
        mockMvc.perform(get("/system/category/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试分类"));

        verify(categoryService).selectCategoryById(1L);
    }

    /**
     * 测试获取分类详情接口 - 分类不存在
     */
    @Test
    void testGetCategoryDetail_NotFound() throws Exception {
        // 模拟分类不存在
        when(categoryService.selectCategoryById(999L)).thenReturn(null);

        // 执行测试 - 当分类不存在时，返回的 data 字段为 null
        mockMvc.perform(get("/system/category/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(categoryService).selectCategoryById(999L);
    }

    /**
     * 测试新增分类接口
     */
    @Test
    void testAddCategory() throws Exception {
        // 模拟成功添加
        when(categoryService.insertCategory(any(com.zhi.system.domain.Category.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("name", "测试分类");
        params.put("description", "分类描述");

        // 执行测试
        mockMvc.perform(post("/system/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(categoryService).insertCategory(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试更新分类接口
     */
    @Test
    void testEditCategory() throws Exception {
        // 模拟成功更新
        when(categoryService.updateCategory(any(com.zhi.system.domain.Category.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("name", "修改后的分类");
        params.put("description", "修改后的描述");

        // 执行测试
        mockMvc.perform(put("/system/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(categoryService).updateCategory(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试删除分类接口
     */
    @Test
    void testRemoveCategory() throws Exception {
        // 模拟成功删除
        when(categoryService.deleteCategoryByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(delete("/system/category/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(categoryService).deleteCategoryByIds(any(Long[].class));
    }

    /**
     * 测试删除分类接口 - 失败
     */
    @Test
    void testRemoveCategory_Failure() throws Exception {
        // 模拟删除失败
        when(categoryService.deleteCategoryByIds(any(Long[].class))).thenReturn(0);

        // 执行测试
        mockMvc.perform(delete("/system/category/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(categoryService).deleteCategoryByIds(any(Long[].class));
    }

    /**
     * 测试新增分类接口 - 失败
     */
    @Test
    void testAddCategory_Failure() throws Exception {
        // 模拟添加失败
        when(categoryService.insertCategory(any(com.zhi.system.domain.Category.class)))
            .thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("name", "测试分类");
        params.put("description", "分类描述");

        // 执行测试
        mockMvc.perform(post("/system/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(categoryService).insertCategory(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试更新分类接口 - 失败
     */
    @Test
    void testEditCategory_Failure() throws Exception {
        // 模拟更新失败
        when(categoryService.updateCategory(any(com.zhi.system.domain.Category.class)))
            .thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("name", "修改后的分类");
        params.put("description", "修改后的描述");

        // 执行测试
        mockMvc.perform(put("/system/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(categoryService).updateCategory(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试导出分类接口
     */
    @Test
    void testExportCategory() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Category> categoryList = new ArrayList<>();
        com.zhi.system.domain.Category category = new com.zhi.system.domain.Category();
        category.setId(1L);
        category.setName("测试分类");
        categoryList.add(category);

        when(categoryService.selectCategoryListForFront(any(com.zhi.system.domain.Category.class)))
            .thenReturn(categoryList);

        // 执行测试
        mockMvc.perform(post("/system/category/export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService).selectCategoryListForFront(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试导出分类接口 - 带参数
     */
    @Test
    void testExportCategory_WithParams() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Category> categoryList = new ArrayList<>();
        com.zhi.system.domain.Category category = new com.zhi.system.domain.Category();
        category.setId(1L);
        category.setName("测试分类");
        categoryList.add(category);

        when(categoryService.selectCategoryListForFront(any(com.zhi.system.domain.Category.class)))
            .thenReturn(categoryList);

        // 执行测试 - delFlag 已设置，不触发自动设置分支
        mockMvc.perform(post("/system/category/export")
                .param("delFlag", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService).selectCategoryListForFront(any(com.zhi.system.domain.Category.class));
    }

    /**
     * 测试导出分类接口 - 空结果
     */
    @Test
    void testExportCategory_Empty() throws Exception {
        // 模拟空结果
        when(categoryService.selectCategoryListForFront(any(com.zhi.system.domain.Category.class)))
            .thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(post("/system/category/export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService).selectCategoryListForFront(any(com.zhi.system.domain.Category.class));
    }
}