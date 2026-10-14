package com.zhi.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 博客功能开关判定工具测试
 * 
 * <p>口径必须与前端 store.isFeatureEnabled 一致，否则会出现“开关不生效”。</p>
 *
 * @author test
 * @date 2026-09-10
 */
class SwitchUtilsTest
{
    @Test
    void testBlankTreatsAsOn()
    {
        assertTrue(SiteSwitchUtils.isOn(null));
        assertTrue(SiteSwitchUtils.isOn(""));
        assertTrue(SiteSwitchUtils.isOn("   "));
        assertFalse(SiteSwitchUtils.isOff(null));
    }

    @Test
    void testTrueVariantsAreOn()
    {
        assertTrue(SiteSwitchUtils.isOn("true"));
        assertTrue(SiteSwitchUtils.isOn("TRUE"));
        assertTrue(SiteSwitchUtils.isOn("1"));
        assertTrue(SiteSwitchUtils.isOn(" true "));
    }

    @Test
    void testFalseVariantsAreOff()
    {
        assertTrue(SiteSwitchUtils.isOff("false"));
        assertTrue(SiteSwitchUtils.isOff("FALSE"));
        assertTrue(SiteSwitchUtils.isOff("0"));
        assertTrue(SiteSwitchUtils.isOff(" false "));
        assertFalse(SiteSwitchUtils.isOn("false"));
        assertFalse(SiteSwitchUtils.isOn("0"));
    }

    @Test
    void testUnknownValueTreatsAsOn()
    {
        // 非预期的值不应当导致功能被意外关闭
        assertTrue(SiteSwitchUtils.isOn("yes"));
        assertTrue(SiteSwitchUtils.isOn("2"));
    }
}
