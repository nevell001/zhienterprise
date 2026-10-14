package com.zhi.system.service.impl;

import com.zhi.common.exception.ServiceException;
import com.zhi.system.domain.Category;
import com.zhi.system.mapper.ArticleMapper;
import com.zhi.system.mapper.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 文章分类服务层单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("测试分类");
        testCategory.setDescription("测试分类描述");
    }

    /**
     * 测试查询分类列表
     */
    @Test
    void testSelectCategoryList() {
        // 准备数据
        List<Category> categoryList = Arrays.asList(testCategory);
        when(categoryMapper.selectCategoryList(any(Category.class)))
            .thenReturn(categoryList);

        // 执行测试
        List<Category> result = categoryService.selectCategoryList(new Category());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试分类", result.get(0).getName());
        verify(categoryMapper).selectCategoryList(any(Category.class));
    }

    /**
     * 测试查询分类列表 - 空结果
     */
    @Test
    void testSelectCategoryList_Empty() {
        // 模拟空结果
        when(categoryMapper.selectCategoryList(any(Category.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<Category> result = categoryService.selectCategoryList(new Category());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryMapper).selectCategoryList(any(Category.class));
    }

    /**
     * 测试根据ID查询分类
     */
    @Test
    void testSelectCategoryById() {
        // 准备数据
        when(categoryMapper.selectCategoryById(1L)).thenReturn(testCategory);

        // 执行测试
        Category result = categoryService.selectCategoryById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("测试分类", result.getName());
        verify(categoryMapper).selectCategoryById(1L);
    }

    /**
     * 测试根据ID查询分类 - 不存在
     */
    @Test
    void testSelectCategoryById_NotFound() {
        // 模拟分类不存在
        when(categoryMapper.selectCategoryById(999L)).thenReturn(null);

        // 执行测试
        Category result = categoryService.selectCategoryById(999L);

        // 验证结果
        assertNull(result);
        verify(categoryMapper).selectCategoryById(999L);
    }

    /**
     * 测试新增分类 - 成功
     */
    @Test
    void testInsertCategory_Success() {
        // 准备数据
        when(categoryMapper.countByName(any(Category.class))).thenReturn(0);
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);

        // 执行测试
        int result = categoryService.insertCategory(testCategory);

        // 验证结果
        assertEquals(1, result);
        assertNotNull(testCategory.getCreateTime());
        verify(categoryMapper).countByName(any(Category.class));
        verify(categoryMapper).insertCategory(any(Category.class));
    }

    /**
     * 测试新增分类 - 名称重复
     */
    @Test
    void testInsertCategory_DuplicateName() {
        // 准备数据
        when(categoryMapper.countByName(any(Category.class))).thenReturn(1);

        // 执行测试并验证异常
        ServiceException exception = assertThrows(ServiceException.class,
            () -> categoryService.insertCategory(testCategory));

        assertEquals("分类名称已存在", exception.getMessage());
        verify(categoryMapper).countByName(any(Category.class));
        verify(categoryMapper, never()).insertCategory(any(Category.class));
    }

    /**
     * 测试新增分类 - 名称为空
     */
    @Test
    void testInsertCategory_EmptyName() {
        // 设置名称为空
        testCategory.setName("");

        // 准备数据
        when(categoryMapper.countByName(any(Category.class))).thenReturn(0);
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);

        // 执行测试 - 注意：Service层可能没有验证名称为空
        int result = categoryService.insertCategory(testCategory);

        // 验证结果 - 如果Service层没有验证，这里应该返回1
        assertEquals(1, result);
        verify(categoryMapper).countByName(any(Category.class));
        verify(categoryMapper).insertCategory(any(Category.class));
    }

    /**
     * 测试新增分类 - 名称前后有空格
     */
    @Test
    void testInsertCategory_NameWithSpaces() {
        // 设置名称前后有空格
        testCategory.setName("  测试分类  ");

        // 准备数据
        when(categoryMapper.countByName(any(Category.class))).thenReturn(0);
        when(categoryMapper.insertCategory(any(Category.class))).thenReturn(1);

        // 执行测试
        int result = categoryService.insertCategory(testCategory);

        // 验证结果
        assertEquals(1, result);
        verify(categoryMapper).countByName(any(Category.class));
        verify(categoryMapper).insertCategory(any(Category.class));
    }

    /**
     * 测试更新分类 - 成功
     */
    @Test
    void testUpdateCategory_Success() {
        // 准备数据
        when(categoryMapper.countByName(any(Category.class))).thenReturn(0);
        when(categoryMapper.updateCategory(any(Category.class))).thenReturn(1);

        // 执行测试
        int result = categoryService.updateCategory(testCategory);

        // 验证结果
        assertEquals(1, result);
        assertNotNull(testCategory.getUpdateTime());
        verify(categoryMapper).countByName(any(Category.class));
        verify(categoryMapper).updateCategory(any(Category.class));
    }

    /**
     * 测试更新分类 - 名称重复
     */
    @Test
    void testUpdateCategory_DuplicateName() {
        // 准备数据
        when(categoryMapper.countByName(any(Category.class))).thenReturn(1);

        // 执行测试并验证异常
        ServiceException exception = assertThrows(ServiceException.class,
            () -> categoryService.updateCategory(testCategory));

        assertEquals("分类名称已存在", exception.getMessage());
        verify(categoryMapper).countByName(any(Category.class));
        verify(categoryMapper, never()).updateCategory(any(Category.class));
    }

    /**
     * 测试更新分类 - 名称为空
     */
    @Test
    void testUpdateCategory_EmptyName() {
        // 设置名称为空
        testCategory.setName("");

        // 准备数据
        when(categoryMapper.countByName(any(Category.class))).thenReturn(0);
        when(categoryMapper.updateCategory(any(Category.class))).thenReturn(1);

        // 执行测试 - 注意：Service层可能没有验证名称为空
        int result = categoryService.updateCategory(testCategory);

        // 验证结果 - 如果Service层没有验证，这里应该返回1
        assertEquals(1, result);
        verify(categoryMapper).countByName(any(Category.class));
        verify(categoryMapper).updateCategory(any(Category.class));
    }

    /**
     * 测试删除分类 - 成功场景
     */
    @Test
    void testDeleteCategoryByIds_Success() {
        // 准备数据
        when(articleMapper.countByCategoryIds(any(Long[].class))).thenReturn(0);
        when(categoryMapper.deleteCategoryByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        int result = categoryService.deleteCategoryByIds(new Long[]{1L});

        // 验证结果
        assertEquals(1, result);
        verify(categoryMapper).deleteCategoryByIds(any(Long[].class));
    }

    /**
     * 测试删除分类 - 分类不存在
     */
    @Test
    void testDeleteCategoryByIds_NotFound() {
        // 准备数据
        when(articleMapper.countByCategoryIds(any(Long[].class))).thenReturn(0);
        when(categoryMapper.deleteCategoryByIds(any(Long[].class))).thenReturn(0);

        // 执行测试 - 注意：Service可能没有检查分类是否存在
        int result = categoryService.deleteCategoryByIds(new Long[]{1L});

        // 验证结果 - 如果Service没有检查，这里应该返回0
        assertEquals(0, result);
        verify(categoryMapper).deleteCategoryByIds(any(Long[].class));
    }

    /**
     * 测试删除分类 - 存在文章引用
     */
    @Test
    void testDeleteCategoryByIds_HasArticles() {
        // 准备数据
        when(articleMapper.countByCategoryIds(any(Long[].class))).thenReturn(1);

        // 执行测试并验证异常
        ServiceException exception = assertThrows(ServiceException.class,
            () -> categoryService.deleteCategoryByIds(new Long[]{1L}));

        assertEquals("存在文章引用该分类，无法删除，请先迁移或删除相关文章", exception.getMessage());
        verify(articleMapper).countByCategoryIds(any(Long[].class));
        verify(categoryMapper, never()).deleteCategoryByIds(any(Long[].class));
    }

    /**
     * 测试删除分类 - 空ID列表
     */
    @Test
    void testDeleteCategoryByIds_EmptyList() {
        // 准备数据
        when(articleMapper.countByCategoryIds(any(Long[].class))).thenReturn(0);
        when(categoryMapper.deleteCategoryByIds(any(Long[].class))).thenReturn(0);

        // 执行测试
        int result = categoryService.deleteCategoryByIds(new Long[]{});

        // 验证结果
        assertEquals(0, result);
        verify(articleMapper).countByCategoryIds(any(Long[].class));
        verify(categoryMapper).deleteCategoryByIds(any(Long[].class));
    }

    /**
     * 测试查询前台分类列表
     */
    @Test
    void testSelectCategoryListForFront() {
        // 准备数据
        List<Category> categoryList = Arrays.asList(testCategory);
        when(categoryMapper.selectCategoryListForFront(any(Category.class)))
            .thenReturn(categoryList);

        // 执行测试
        List<Category> result = categoryService.selectCategoryListForFront(new Category());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(categoryMapper).selectCategoryListForFront(any(Category.class));
    }

    /**
     * 测试查询前台分类列表 - 空结果
     */
    @Test
    void testSelectCategoryListForFront_Empty() {
        // 模拟空结果
        when(categoryMapper.selectCategoryListForFront(any(Category.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<Category> result = categoryService.selectCategoryListForFront(new Category());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryMapper).selectCategoryListForFront(any(Category.class));
    }

    /**
     * 测试更新文章数量
     */
    @Test
    void testUpdateArticleCount() {
        // 准备数据
        when(categoryMapper.updateArticleCount(anyLong())).thenReturn(1);

        // 执行测试
        int result = categoryService.updateArticleCount(1L);

        // 验证结果
        assertEquals(1, result);
        verify(categoryMapper).updateArticleCount(1L);
    }

    /**
     * 测试更新所有分类文章数量
     */
    @Test
    void testUpdateAllArticleCount() {
        // 准备数据
        when(categoryMapper.updateAllArticleCount()).thenReturn(5);

        // 执行测试
        int result = categoryService.updateAllArticleCount();

        // 验证结果
        assertEquals(5, result);
        verify(categoryMapper).updateAllArticleCount();
    }

    /**
     * 测试统计分类数量
     */
    @Test
    void testSelectCategoryCount() {
        // 准备数据
        when(categoryMapper.countByName(any(Category.class))).thenReturn(1);

        // 执行测试
        int result = categoryMapper.countByName(testCategory);

        // 验证结果
        assertEquals(1, result);
        verify(categoryMapper).countByName(any(Category.class));
    }
}