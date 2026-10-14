package com.zhi.system.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章标签关联对象单元测试
 *
 * @author test
 * @date 2026-01-11
 */
class ArticleTagTest {

    /**
     * 测试所有基本属性
     */
    @Test
    void testAllProperties() {
        ArticleTag articleTag = new ArticleTag();
        articleTag.setId(1L);
        articleTag.setArticleId(100L);
        articleTag.setTagId(10L);

        assertEquals(1L, articleTag.getId());
        assertEquals(100L, articleTag.getArticleId());
        assertEquals(10L, articleTag.getTagId());
    }

    /**
     * 测试文章与标签的关联
     */
    @Test
    void testArticleTagAssociation() {
        ArticleTag articleTag = new ArticleTag();

        // 设置文章ID
        articleTag.setArticleId(100L);
        assertEquals(100L, articleTag.getArticleId());

        // 设置标签ID
        articleTag.setTagId(10L);
        assertEquals(10L, articleTag.getTagId());
    }

    /**
     * 测试多对多关系 - 一篇文章关联多个标签
     */
    @Test
    void testMultipleTagsForArticle() {
        ArticleTag articleTag1 = new ArticleTag();
        articleTag1.setArticleId(100L);
        articleTag1.setTagId(10L);

        ArticleTag articleTag2 = new ArticleTag();
        articleTag2.setArticleId(100L);
        articleTag2.setTagId(11L);

        ArticleTag articleTag3 = new ArticleTag();
        articleTag3.setArticleId(100L);
        articleTag3.setTagId(12L);

        // 验证同一篇文章关联了3个不同的标签
        assertEquals(100L, articleTag1.getArticleId());
        assertEquals(100L, articleTag2.getArticleId());
        assertEquals(100L, articleTag3.getArticleId());

        assertEquals(10L, articleTag1.getTagId());
        assertEquals(11L, articleTag2.getTagId());
        assertEquals(12L, articleTag3.getTagId());
    }

    /**
     * 测试多对多关系 - 一个标签关联多篇文章
     */
    @Test
    void testMultipleArticlesForTag() {
        ArticleTag articleTag1 = new ArticleTag();
        articleTag1.setArticleId(100L);
        articleTag1.setTagId(10L);

        ArticleTag articleTag2 = new ArticleTag();
        articleTag2.setArticleId(101L);
        articleTag2.setTagId(10L);

        ArticleTag articleTag3 = new ArticleTag();
        articleTag3.setArticleId(102L);
        articleTag3.setTagId(10L);

        // 验证同一个标签关联了3篇不同的文章
        assertEquals(10L, articleTag1.getTagId());
        assertEquals(10L, articleTag2.getTagId());
        assertEquals(10L, articleTag3.getTagId());

        assertEquals(100L, articleTag1.getArticleId());
        assertEquals(101L, articleTag2.getArticleId());
        assertEquals(102L, articleTag3.getArticleId());
    }

    /**
     * 测试 toString 方法
     */
    @Test
    void testToString() {
        ArticleTag articleTag = new ArticleTag();
        articleTag.setId(1L);
        articleTag.setArticleId(100L);
        articleTag.setTagId(10L);

        String result = articleTag.toString();

        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("100"));
        assertTrue(result.contains("10"));
    }
}