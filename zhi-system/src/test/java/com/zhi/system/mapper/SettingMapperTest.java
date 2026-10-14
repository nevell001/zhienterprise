package com.zhi.system.mapper;

import com.zhi.system.domain.Setting;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 博客设置Mapper单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class SettingMapperTest {

    @Mock
    private SettingMapper settingMapper;

    private Setting testSetting;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testSetting = new Setting();
        testSetting.setId(1L);
        testSetting.setSettingKey("blog_name");
        testSetting.setSettingValue("我的博客");
        testSetting.setDescription("博客名称");
    }

    /**
     * 测试CRUD操作的完整流程
     */
    @Test
    void testCRUDOperations() {
        // 测试新增
        when(settingMapper.insertSetting(any(Setting.class))).thenReturn(1);
        int insertResult = settingMapper.insertSetting(testSetting);
        assertEquals(1, insertResult, "新增博客设置失败");
        verify(settingMapper).insertSetting(any(Setting.class));

        // 测试根据ID查询
        when(settingMapper.selectSettingById(1L)).thenReturn(testSetting);
        Setting selectResult = settingMapper.selectSettingById(1L);
        assertNotNull(selectResult, "根据ID查询博客设置失败");
        assertEquals("blog_name", selectResult.getSettingKey(), "设置键不匹配");
        verify(settingMapper).selectSettingById(1L);

        // 测试修改
        when(settingMapper.updateSetting(any(Setting.class))).thenReturn(1);
        testSetting.setSettingValue("修改后的博客名称");
        int updateResult = settingMapper.updateSetting(testSetting);
        assertEquals(1, updateResult, "修改博客设置失败");
        verify(settingMapper).updateSetting(any(Setting.class));

        // 测试查询列表
        when(settingMapper.selectSettingList(any(Setting.class)))
            .thenReturn(Arrays.asList(testSetting));
        List<Setting> settingList = settingMapper.selectSettingList(new Setting());
        assertFalse(settingList.isEmpty(), "查询博客设置列表失败");
        verify(settingMapper).selectSettingList(any(Setting.class));

        // 测试删除
        when(settingMapper.deleteSettingById(1L)).thenReturn(1);
        int deleteResult = settingMapper.deleteSettingById(1L);
        assertEquals(1, deleteResult, "删除博客设置失败");
        verify(settingMapper).deleteSettingById(1L);
    }

    /**
     * 测试批量删除功能
     */
    @Test
    void testDeleteSettingByIds() {
        when(settingMapper.deleteSettingByIds(any(Long[].class))).thenReturn(2);
        int deleteResult = settingMapper.deleteSettingByIds(new Long[]{1L, 2L});
        assertEquals(2, deleteResult, "批量删除博客设置失败");
        verify(settingMapper).deleteSettingByIds(any(Long[].class));
    }

    /**
     * 测试根据设置键查询设置值
     */
    @Test
    void testSelectSettingValueByKey() {
        when(settingMapper.selectSettingValueByKey("blog_name")).thenReturn("我的博客");
        String value = settingMapper.selectSettingValueByKey("blog_name");
        assertEquals("我的博客", value, "设置值不匹配");
        verify(settingMapper).selectSettingValueByKey("blog_name");
    }

    /**
     * 测试更新设置值
     */
    @Test
    void testUpdateSettingValueByKey() {
        when(settingMapper.updateSettingValueByKey(anyString(), anyString())).thenReturn(1);
        int result = settingMapper.updateSettingValueByKey("blog_name", "新博客名称");
        assertEquals(1, result, "更新设置值失败");
        verify(settingMapper).updateSettingValueByKey(anyString(), anyString());
    }

    /**
     * 测试边界条件：空值处理
     */
    @Test
    void testNullHandling() {
        // 测试查询不存在的ID
        when(settingMapper.selectSettingById(999L)).thenReturn(null);
        Setting result = settingMapper.selectSettingById(999L);
        assertNull(result, "查询不存在的ID应返回null");
        verify(settingMapper).selectSettingById(999L);

        // 测试查询不存在的设置键
        when(settingMapper.selectSettingValueByKey("nonexistent_key")).thenReturn(null);
        String value = settingMapper.selectSettingValueByKey("nonexistent_key");
        assertNull(value, "查询不存在的设置键应返回null");
        verify(settingMapper).selectSettingValueByKey("nonexistent_key");

        // 测试空列表查询
        when(settingMapper.selectSettingList(any(Setting.class)))
            .thenReturn(Arrays.asList());
        List<Setting> list = settingMapper.selectSettingList(new Setting());
        assertTrue(list.isEmpty(), "查询结果应为空列表");
        verify(settingMapper).selectSettingList(any(Setting.class));
    }

    /**
     * 测试边界条件：空键
     */
    @Test
    void testEmptyKey() {
        Setting emptyKeySetting = new Setting();
        emptyKeySetting.setSettingKey("");
        emptyKeySetting.setSettingValue("值");
        
        when(settingMapper.insertSetting(any(Setting.class))).thenReturn(1);
        int result = settingMapper.insertSetting(emptyKeySetting);
        assertEquals(1, result, "空键博客设置插入失败");
        verify(settingMapper).insertSetting(any(Setting.class));
    }

    /**
     * 测试边界条件：空值
     */
    @Test
    void testEmptyValue() {
        Setting emptyValueSetting = new Setting();
        emptyValueSetting.setSettingKey("test_key");
        emptyValueSetting.setSettingValue("");
        
        when(settingMapper.insertSetting(any(Setting.class))).thenReturn(1);
        int result = settingMapper.insertSetting(emptyValueSetting);
        assertEquals(1, result, "空值博客设置插入失败");
        verify(settingMapper).insertSetting(any(Setting.class));
    }

    /**
     * 测试边界条件：长键名
     */
    @Test
    void testLongKey() {
        Setting longKeySetting = new Setting();
        longKeySetting.setSettingKey("very_long_setting_key_name_that_exceeds_normal_length");
        longKeySetting.setSettingValue("值");
        
        when(settingMapper.insertSetting(any(Setting.class))).thenReturn(1);
        int result = settingMapper.insertSetting(longKeySetting);
        assertEquals(1, result, "长键名博客设置插入失败");
        verify(settingMapper).insertSetting(any(Setting.class));
    }

    /**
     * 测试边界条件：长值
     */
    @Test
    void testLongValue() {
        Setting longValueSetting = new Setting();
        longValueSetting.setSettingKey("test_key");
        longValueSetting.setSettingValue("这是一个非常长的博客设置值，用于测试系统对长文本的处理能力");
        
        when(settingMapper.insertSetting(any(Setting.class))).thenReturn(1);
        int result = settingMapper.insertSetting(longValueSetting);
        assertEquals(1, result, "长值博客设置插入失败");
        verify(settingMapper).insertSetting(any(Setting.class));
    }
}