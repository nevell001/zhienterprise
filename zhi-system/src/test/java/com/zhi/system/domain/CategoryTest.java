package com.zhi.system.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章分类对象单元测试
 *
 * @author test
 * @date 2026-01-11
 */
class CategoryTest {

    /**
     * 测试 getDelFlag 方法 - delFlag 为 null
     */
    @Test
    void testGetDelFlag_Null() {
        Category category = new Category();
        category.setDelFlag(null);

        String result = category.getDelFlag();

        // 验证返回默认值 "0"
        assertEquals("0", result);
    }

    /**
     * 测试 getDelFlag 方法 - delFlag 为 "0"
     */
    @Test
    void testGetDelFlag_Zero() {
        Category category = new Category();
        category.setDelFlag("0");

        String result = category.getDelFlag();

        // 验证返回 "0"
        assertEquals("0", result);
    }

    /**
     * 测试 getDelFlag 方法 - delFlag 为 "1"
     */
    @Test
    void testGetDelFlag_One() {
        Category category = new Category();
        category.setDelFlag("1");

        String result = category.getDelFlag();

        // 验证返回 "1"
        assertEquals("1", result);
    }

    /**
     * 测试 setSort 和 getSort 兼容性方法 - 设置 sort
     */
    @Test
    void testSortCompatibility_SetSort() {
        Category category = new Category();
        category.setSort(10);

        // 验证通过 setSort 设置的值可以通过 getSort 和 getSortOrder 获取
        assertEquals(10, category.getSort());
        assertEquals(10, category.getSortOrder());
    }

    /**
     * 测试 setSort 和 getSort 兼容性方法 - 设置 sortOrder
     */
    @Test
    void testSortCompatibility_SetSortOrder() {
        Category category = new Category();
        category.setSortOrder(20);

        // 验证通过 setSortOrder 设置的值可以通过 getSort 和 getSortOrder 获取
        assertEquals(20, category.getSortOrder());
        assertEquals(20, category.getSort());
    }

    /**
     * 测试所有基本属性
     */
    @Test
    void testAllProperties() {
        Category category = new Category();
        category.setId(1L);
        category.setName("技术");
        category.setAlias("tech");
        category.setDescription("技术类文章");
        category.setParentId(0L);
        category.setSortOrder(1);
        category.setArticleCount(10);
        category.setStatus(0);

        assertEquals(1L, category.getId());
        assertEquals("技术", category.getName());
        assertEquals("tech", category.getAlias());
        assertEquals("技术类文章", category.getDescription());
        assertEquals(0L, category.getParentId());
        assertEquals(1, category.getSortOrder());
        assertEquals(10, category.getArticleCount());
        assertEquals(0, category.getStatus());
    }

    /**
     * 测试 toString 方法
     */
    @Test
    void testToString() {
        Category category = new Category();
        category.setId(1L);
        category.setName("技术");

        String result = category.toString();

        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("技术"));
    }
}