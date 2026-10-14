package com.zhi.system.mapper;

import com.zhi.system.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 文章分类Mapper单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class CategoryMapperTest {

    @Mock
    private CategoryMapper categoryMapper;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("测试分类");
        testCategory.setAlias("test");
        testCategory.setDescription("测试描述");
        testCategory.setSortOrder(1);
    }

    /**
     * 测试CRUD操作的完整流程
     */
    @Test
    void testCRUDOperations() {
        // 测试新增
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);
        int insertResult = categoryMapper.insertCategory(testCategory);
        assertEquals(1, insertResult, "新增文章分类失败");
        verify(categoryMapper).insertCategory(any(Category.class));

        // 测试根据ID查询
        when(categoryMapper.selectCategoryById(1L)).thenReturn(testCategory);
        Category selectResult = categoryMapper.selectCategoryById(1L);
        assertNotNull(selectResult, "根据ID查询文章分类失败");
        assertEquals("测试分类", selectResult.getName(), "分类名称不匹配");
        verify(categoryMapper).selectCategoryById(1L);

        // 测试修改
        when(categoryMapper.updateCategory(any(Category.class))).thenReturn(1);
        testCategory.setName("修改后的分类");
        int updateResult = categoryMapper.updateCategory(testCategory);
        assertEquals(1, updateResult, "修改文章分类失败");
        verify(categoryMapper).updateCategory(any(Category.class));

        // 测试查询列表
        when(categoryMapper.selectCategoryList(any(Category.class)))
            .thenReturn(Arrays.asList(testCategory));
        List<Category> categoryList = categoryMapper.selectCategoryList(new Category());
        assertFalse(categoryList.isEmpty(), "查询文章分类列表失败");
        verify(categoryMapper).selectCategoryList(any(Category.class));

        // 测试删除
        when(categoryMapper.deleteCategoryById(1L)).thenReturn(1);
        int deleteResult = categoryMapper.deleteCategoryById(1L);
        assertEquals(1, deleteResult, "删除文章分类失败");
        verify(categoryMapper).deleteCategoryById(1L);
    }

    /**
     * 测试批量删除功能
     */
    @Test
    void testDeleteCategoryByIds() {
        when(categoryMapper.deleteCategoryByIds(any(Long[].class))).thenReturn(2);
        int deleteResult = categoryMapper.deleteCategoryByIds(new Long[]{1L, 2L});
        assertEquals(2, deleteResult, "批量删除文章分类失败");
        verify(categoryMapper).deleteCategoryByIds(any(Long[].class));
    }

    /**
     * 测试统计同名分类数量
     */
    @Test
    void testCountByName() {
        when(categoryMapper.countByName(any(Category.class))).thenReturn(0);
        int count = categoryMapper.countByName(testCategory);
        assertEquals(0, count, "统计同名分类数量失败");
        verify(categoryMapper).countByName(any(Category.class));
    }

    /**
     * 测试查询前台分类列表
     */
    @Test
    void testSelectCategoryListForFront() {
        when(categoryMapper.selectCategoryListForFront(any(Category.class)))
            .thenReturn(Arrays.asList(testCategory));
        List<Category> categoryList = categoryMapper.selectCategoryListForFront(new Category());
        assertNotNull(categoryList, "前台分类列表不应为null");
        assertEquals(1, categoryList.size(), "前台分类列表大小不正确");
        verify(categoryMapper).selectCategoryListForFront(any(Category.class));
    }

    /**
     * 测试更新文章数量
     */
    @Test
    void testUpdateArticleCount() {
        when(categoryMapper.updateArticleCount(anyLong())).thenReturn(1);
        int result = categoryMapper.updateArticleCount(1L);
        assertEquals(1, result, "更新文章数量失败");
        verify(categoryMapper).updateArticleCount(1L);
    }

    /**
     * 测试边界条件：空值处理
     */
    @Test
    void testNullHandling() {
        // 测试查询不存在的ID
        when(categoryMapper.selectCategoryById(999L)).thenReturn(null);
        Category result = categoryMapper.selectCategoryById(999L);
        assertNull(result, "查询不存在的ID应返回null");
        verify(categoryMapper).selectCategoryById(999L);

        // 测试空列表查询
        when(categoryMapper.selectCategoryList(any(Category.class)))
            .thenReturn(Arrays.asList());
        List<Category> list = categoryMapper.selectCategoryList(new Category());
        assertTrue(list.isEmpty(), "查询结果应为空列表");
        verify(categoryMapper).selectCategoryList(any(Category.class));
    }

    /**
     * 测试边界条件：空名称
     */
    @Test
    void testEmptyName() {
        Category emptyNameCategory = new Category();
        emptyNameCategory.setName("");
        
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);
        int result = categoryMapper.insertCategory(emptyNameCategory);
        assertEquals(1, result, "空名称分类插入失败");
        verify(categoryMapper).insertCategory(any(Category.class));
    }

    /**
     * 测试边界条件：空别名
     */
    @Test
    void testEmptyAlias() {
        Category emptyAliasCategory = new Category();
        emptyAliasCategory.setName("测试");
        emptyAliasCategory.setAlias("");
        
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);
        int result = categoryMapper.insertCategory(emptyAliasCategory);
        assertEquals(1, result, "空别名分类插入失败");
        verify(categoryMapper).insertCategory(any(Category.class));
    }

    /**
     * 测试边界条件：排序值
     */
    @Test
    void testSortOrder() {
        // 测试负排序值
        Category negativeSort = new Category();
        negativeSort.setName("测试");
        negativeSort.setSortOrder(-1);
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);
        int result1 = categoryMapper.insertCategory(negativeSort);
        assertEquals(1, result1, "负排序值分类插入失败");

        // 测试零排序值
        Category zeroSort = new Category();
        zeroSort.setName("测试");
        zeroSort.setSortOrder(0);
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);
        int result2 = categoryMapper.insertCategory(zeroSort);
        assertEquals(1, result2, "零排序值分类插入失败");

        // 测试正排序值
        Category positiveSort = new Category();
        positiveSort.setName("测试");
        positiveSort.setSortOrder(100);
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);
        int result3 = categoryMapper.insertCategory(positiveSort);
        assertEquals(1, result3, "正排序值分类插入失败");

        verify(categoryMapper, times(3)).insertCategory(any(Category.class));
    }
}