package com.zhi.system.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 博客评论对象单元测试
 *
 * @author test
 * @date 2026-01-11
 */
class CommentTest {

    /**
     * 测试所有基本属性
     */
    @Test
    void testAllProperties() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setArticleId(100L);
        comment.setUserId(1L);
        comment.setNickname("访客");
        comment.setEmail("visitor@example.com");
        comment.setAvatar("/uploads/avatar.jpg");
        comment.setContent("这是一条评论");
        comment.setParentId(0L);
        comment.setReplyUserId(2L);
        comment.setStatus("1");

        assertEquals(1L, comment.getId());
        assertEquals(100L, comment.getArticleId());
        assertEquals(1L, comment.getUserId());
        assertEquals("访客", comment.getNickname());
        assertEquals("visitor@example.com", comment.getEmail());
        assertEquals("/uploads/avatar.jpg", comment.getAvatar());
        assertEquals("这是一条评论", comment.getContent());
        assertEquals(0L, comment.getParentId());
        assertEquals(2L, comment.getReplyUserId());
        assertEquals("1", comment.getStatus());
    }

    /**
     * 测试一级评论（parentId 为 0）
     */
    @Test
    void testTopLevelComment() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setArticleId(100L);
        comment.setContent("一级评论");
        comment.setParentId(0L);

        assertEquals(0L, comment.getParentId());
    }

    /**
     * 测试回复评论（parentId 不为 0）
     */
    @Test
    void testReplyComment() {
        Comment comment = new Comment();
        comment.setId(2L);
        comment.setArticleId(100L);
        comment.setContent("回复评论");
        comment.setParentId(1L);
        comment.setReplyUserId(1L);

        assertEquals(1L, comment.getParentId());
        assertEquals(1L, comment.getReplyUserId());
    }

    /**
     * 测试不同状态
     */
    @Test
    void testDifferentStatus() {
        Comment comment = new Comment();

        // 待审核
        comment.setStatus("0");
        assertEquals("0", comment.getStatus());

        // 已发布
        comment.setStatus("1");
        assertEquals("1", comment.getStatus());

        // 已删除
        comment.setStatus("2");
        assertEquals("2", comment.getStatus());
    }

    /**
     * 测试匿名评论
     */
    @Test
    void testAnonymousComment() {
        Comment comment = new Comment();
        comment.setUserId(null);
        comment.setNickname("匿名用户");
        comment.setEmail("anonymous@example.com");
        comment.setContent("匿名评论");

        assertNull(comment.getUserId());
        assertEquals("匿名用户", comment.getNickname());
        assertEquals("anonymous@example.com", comment.getEmail());
        assertEquals("匿名评论", comment.getContent());
    }

    /**
     * 测试 toString 方法
     */
    @Test
    void testToString() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("测试评论");

        String result = comment.toString();

        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("测试评论"));
    }
}