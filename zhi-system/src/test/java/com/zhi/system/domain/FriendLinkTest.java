package com.zhi.system.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 友情链接对象单元测试
 *
 * @author test
 * @date 2026-01-11
 */
class FriendLinkTest {

    /**
     * 测试所有基本属性
     */
    @Test
    void testAllProperties() {
        FriendLink friendLink = new FriendLink();
        friendLink.setId(1L);
        friendLink.setName("示例网站");
        friendLink.setUrl("https://example.com");
        friendLink.setLogo("/uploads/logo.png");
        friendLink.setDescription("这是一个示例网站");
        friendLink.setSort(1);
        friendLink.setStatus("0");
        friendLink.setDelFlag("0");

        assertEquals(1L, friendLink.getId());
        assertEquals("示例网站", friendLink.getName());
        assertEquals("https://example.com", friendLink.getUrl());
        assertEquals("/uploads/logo.png", friendLink.getLogo());
        assertEquals("这是一个示例网站", friendLink.getDescription());
        assertEquals(1, friendLink.getSort());
        assertEquals("0", friendLink.getStatus());
        assertEquals("0", friendLink.getDelFlag());
    }

    /**
     * 测试不同状态
     */
    @Test
    void testDifferentStatus() {
        FriendLink friendLink = new FriendLink();

        // 正常状态
        friendLink.setStatus("0");
        assertEquals("0", friendLink.getStatus());

        // 停用状态
        friendLink.setStatus("1");
        assertEquals("1", friendLink.getStatus());
    }

    /**
     * 测试没有 logo 的友链
     */
    @Test
    void testFriendLinkWithoutLogo() {
        FriendLink friendLink = new FriendLink();
        friendLink.setId(1L);
        friendLink.setName("示例网站");
        friendLink.setUrl("https://example.com");
        friendLink.setLogo(null);

        assertNull(friendLink.getLogo());
        assertEquals("示例网站", friendLink.getName());
    }

    /**
     * 测试排序
     */
    @Test
    void testSort() {
        FriendLink friendLink1 = new FriendLink();
        friendLink1.setSort(1);

        FriendLink friendLink2 = new FriendLink();
        friendLink2.setSort(2);

        assertEquals(1, friendLink1.getSort());
        assertEquals(2, friendLink2.getSort());
    }

    /**
     * 测试 toString 方法
     */
    @Test
    void testToString() {
        FriendLink friendLink = new FriendLink();
        friendLink.setId(1L);
        friendLink.setName("示例网站");

        String result = friendLink.toString();

        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("示例网站"));
    }
}