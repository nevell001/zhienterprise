package com.zhi.system.service.impl;

import com.zhi.system.domain.Setting;
import com.zhi.system.mapper.SettingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zhi.common.utils.SecurityUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 博客设置服务层单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class SettingServiceImplTest {

    @Mock
    private SettingMapper settingMapper;

    @InjectMocks
    private SettingServiceImpl settingService;

    private Setting testSetting;

    @BeforeEach
    void setUp() {
        testSetting = new Setting();
        testSetting.setId(1L);
        testSetting.setSettingKey("blog_name");
        testSetting.setSettingValue("我的博客");
        testSetting.setDescription("博客名称");
    }

    /**
     * 测试查询设置列表
     */
    @Test
    void testSelectSettingList() {
        // 准备数据
        List<Setting> settingList = Arrays.asList(testSetting);
        when(settingMapper.selectSettingList(any(Setting.class)))
            .thenReturn(settingList);

        // 执行测试
        List<Setting> result = settingService.selectSettingList(new Setting());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("blog_name", result.get(0).getSettingKey());
        verify(settingMapper).selectSettingList(any(Setting.class));
    }

    /**
     * 测试查询设置列表 - 空结果
     */
    @Test
    void testSelectSettingList_Empty() {
        // 模拟空结果
        when(settingMapper.selectSettingList(any(Setting.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<Setting> result = settingService.selectSettingList(new Setting());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(settingMapper).selectSettingList(any(Setting.class));
    }

    /**
     * 测试根据ID查询设置
     */
    @Test
    void testSelectSettingById() {
        // 准备数据
        when(settingMapper.selectSettingById(1L)).thenReturn(testSetting);

        // 执行测试
        Setting result = settingService.selectSettingById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("blog_name", result.getSettingKey());
        verify(settingMapper).selectSettingById(1L);
    }

    /**
     * 测试根据ID查询设置 - 不存在
     */
    @Test
    void testSelectSettingById_NotFound() {
        // 模拟设置不存在
        when(settingMapper.selectSettingById(999L)).thenReturn(null);

        // 执行测试
        Setting result = settingService.selectSettingById(999L);

        // 验证结果
        assertNull(result);
        verify(settingMapper).selectSettingById(999L);
    }

    /**
     * 测试根据设置键查询设置值
     */
    @Test
    void testSelectSettingValueByKey() {
        // 准备数据
        when(settingMapper.selectSettingValueByKey("blog_name")).thenReturn("我的博客");

        // 执行测试
        String result = settingService.selectSettingValueByKey("blog_name");

        // 验证结果
        assertNotNull(result);
        assertEquals("我的博客", result);
        verify(settingMapper).selectSettingValueByKey("blog_name");
    }

    /**
     * 测试根据设置键查询设置值 - 不存在
     */
    @Test
    void testSelectSettingValueByKey_NotFound() {
        // 模拟设置不存在
        when(settingMapper.selectSettingValueByKey("non_existent_key")).thenReturn(null);

        // 执行测试
        String result = settingService.selectSettingValueByKey("non_existent_key");

        // 验证结果
        assertNull(result);
        verify(settingMapper).selectSettingValueByKey("non_existent_key");
    }

    /**
     * 测试新增设置 - 成功
     */
    @Test
    void testInsertSetting_Success() {
        // 准备数据
        when(settingMapper.insertSetting(any(Setting.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = settingService.insertSetting(testSetting);

            // 验证结果
            assertEquals(1, result);
            verify(settingMapper).insertSetting(any(Setting.class));
        }
    }

    /**
     * 测试更新设置 - 成功
     */
    @Test
    void testUpdateSetting_Success() {
        // 准备数据
        when(settingMapper.updateSetting(any(Setting.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = settingService.updateSetting(testSetting);

            // 验证结果
            assertEquals(1, result);
            verify(settingMapper).updateSetting(any(Setting.class));
        }
    }

    /**
     * 测试通过设置键更新设置值 - 成功
     */
    @Test
    void testUpdateSettingValueByKey_Success() {
        // 准备数据
        when(settingMapper.updateSettingValueByKey(anyString(), anyString())).thenReturn(1);

        // 执行测试
        int result = settingService.updateSettingValueByKey("blog_name", "新博客名称");

        // 验证结果
        assertEquals(1, result);
        verify(settingMapper).updateSettingValueByKey("blog_name", "新博客名称");
    }

    /**
     * 测试删除设置 - 成功
     */
    @Test
    void testDeleteSettingById_Success() {
        // 准备数据
        when(settingMapper.deleteSettingById(1L)).thenReturn(1);

        // 执行测试
        int result = settingService.deleteSettingById(1L);

        // 验证结果
        assertEquals(1, result);
        verify(settingMapper).deleteSettingById(1L);
    }

    /**
     * 测试删除设置 - 不存在
     */
    @Test
    void testDeleteSettingById_NotFound() {
        // 准备数据
        when(settingMapper.deleteSettingById(999L)).thenReturn(0);

        // 执行测试
        int result = settingService.deleteSettingById(999L);

        // 验证结果
        assertEquals(0, result);
        verify(settingMapper).deleteSettingById(999L);
    }

    /**
     * 测试批量删除设置 - 成功
     */
    @Test
    void testDeleteSettingByIds_Success() {
        // 准备数据
        when(settingMapper.deleteSettingByIds(any(Long[].class))).thenReturn(2);

        // 执行测试
        int result = settingService.deleteSettingByIds(new Long[]{1L, 2L});

        // 验证结果
        assertEquals(2, result);
        verify(settingMapper).deleteSettingByIds(any(Long[].class));
    }

    /**
     * 测试批量删除设置 - 空列表
     */
    @Test
    void testDeleteSettingByIds_EmptyList() {
        // 执行测试
        int result = settingService.deleteSettingByIds(new Long[]{});

        // 验证结果
        assertEquals(0, result);
        verify(settingMapper).deleteSettingByIds(any(Long[].class));
    }

    /**
     * 测试设置值长度限制
     */
    @Test
    void testInsertSetting_LongValue() {
        // 设置超长值
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("测试值");
        }
        testSetting.setSettingValue(longValue.toString());

        // 准备数据
        when(settingMapper.insertSetting(any(Setting.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = settingService.insertSetting(testSetting);

            // 验证结果
            assertEquals(1, result);
            verify(settingMapper).insertSetting(any(Setting.class));
        }
    }

    /**
     * 测试设置键包含特殊字符
     */
    @Test
    void testInsertSetting_SpecialCharacters() {
        // 设置包含特殊字符的键
        testSetting.setSettingKey("blog_name_test_123");

        // 准备数据
        when(settingMapper.insertSetting(any(Setting.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = settingService.insertSetting(testSetting);

            // 验证结果
            assertEquals(1, result);
            verify(settingMapper).insertSetting(any(Setting.class));
        }
    }

    /**
     * 测试查询多个设置值
     */
    @Test
    void testSelectMultipleSettingValues() {
        // 准备数据
        when(settingMapper.selectSettingValueByKey("blog_name")).thenReturn("我的博客");
        when(settingMapper.selectSettingValueByKey("blog_desc")).thenReturn("博客描述");
        when(settingMapper.selectSettingValueByKey("blog_author")).thenReturn("作者");

        // 执行测试
        String name = settingService.selectSettingValueByKey("blog_name");
        String desc = settingService.selectSettingValueByKey("blog_desc");
        String author = settingService.selectSettingValueByKey("blog_author");

        // 验证结果
        assertEquals("我的博客", name);
        assertEquals("博客描述", desc);
        assertEquals("作者", author);
    }
}