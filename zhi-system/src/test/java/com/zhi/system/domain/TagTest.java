package com.zhi.system.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 博客标签对象单元测试
 *
 * @author test
 * @date 2026-01-11
 */
class TagTest {

    /**
     * 测试 getDelFlag 方法 - delFlag 为 null
     */
    @Test
    void testGetDelFlag_Null() {
        Tag tag = new Tag();
        tag.setDelFlag(null);
        
        Integer result = tag.getDelFlag();
        
        // 验证返回默认值 0
        assertEquals(0, result);
    }

    /**
     * 测试 getDelFlag 方法 - delFlag 为 0
     */
    @Test
    void testGetDelFlag_Zero() {
        Tag tag = new Tag();
        tag.setDelFlag(0);
        
        Integer result = tag.getDelFlag();
        
        // 验证返回 0
        assertEquals(0, result);
    }

    /**
     * 测试 getDelFlag 方法 - delFlag 为 1
     */
    @Test
    void testGetDelFlag_One() {
        Tag tag = new Tag();
        tag.setDelFlag(1);
        
        Integer result = tag.getDelFlag();
        
        // 验证返回 1
        assertEquals(1, result);
    }

    /**
     * 测试标签名称设置和获取
     */
    @Test
    void testTagName() {
        Tag tag = new Tag();
        tag.setName("测试标签");

        // 验证通过 setName 设置的值可以通过 getName 获取
        assertEquals("测试标签", tag.getName());
    }
}