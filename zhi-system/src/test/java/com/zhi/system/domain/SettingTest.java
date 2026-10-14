package com.zhi.system.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 博客设置对象单元测试
 *
 * @author test
 * @date 2026-01-11
 */
class SettingTest {

    /**
     * 测试所有基本属性
     */
    @Test
    void testAllProperties() {
        Setting setting = new Setting();
        setting.setId(1L);
        setting.setSettingKey("blog_name");
        setting.setSettingValue("我的博客");
        setting.setDescription("博客名称");
        setting.setDelFlag("0");

        assertEquals(1L, setting.getId());
        assertEquals("blog_name", setting.getSettingKey());
        assertEquals("我的博客", setting.getSettingValue());
        assertEquals("博客名称", setting.getDescription());
        assertEquals("0", setting.getDelFlag());
    }

    /**
     * 测试前端兼容性方法 - configKey
     */
    @Test
    void testConfigKeyCompatibility() {
        Setting setting = new Setting();
        setting.setConfigKey("blog_name");

        // 验证通过 setConfigKey 设置的值可以通过 getConfigKey 和 getSettingKey 获取
        assertEquals("blog_name", setting.getConfigKey());
        assertEquals("blog_name", setting.getSettingKey());
    }

    /**
     * 测试前端兼容性方法 - configValue
     */
    @Test
    void testConfigValueCompatibility() {
        Setting setting = new Setting();
        setting.setConfigValue("我的博客");

        // 验证通过 setConfigValue 设置的值可以通过 getConfigValue 和 getSettingValue 获取
        assertEquals("我的博客", setting.getConfigValue());
        assertEquals("我的博客", setting.getSettingValue());
    }

    /**
     * 测试前端兼容性方法 - configName
     */
    @Test
    void testConfigNameCompatibility() {
        Setting setting = new Setting();
        setting.setConfigName("博客名称");

        // 验证通过 setConfigName 设置的值可以通过 getConfigName 和 getDescription 获取
        assertEquals("博客名称", setting.getConfigName());
        assertEquals("博客名称", setting.getDescription());
    }

    /**
     * 测试不同类型的设置值
     */
    @Test
    void testDifferentSettingValues() {
        Setting setting = new Setting();

        // 字符串类型
        setting.setSettingValue("我的博客");
        assertEquals("我的博客", setting.getSettingValue());

        // 数字类型（存储为字符串）
        setting.setSettingValue("100");
        assertEquals("100", setting.getSettingValue());

        // 布尔类型（存储为字符串）
        setting.setSettingValue("true");
        assertEquals("true", setting.getSettingValue());

        // JSON 类型
        setting.setSettingValue("{\"key\":\"value\"}");
        assertEquals("{\"key\":\"value\"}", setting.getSettingValue());
    }

    /**
     * 测试 toString 方法
     */
    @Test
    void testToString() {
        Setting setting = new Setting();
        setting.setId(1L);
        setting.setSettingKey("blog_name");

        String result = setting.toString();

        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("blog_name"));
    }
}