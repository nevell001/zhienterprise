package com.zhi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.domain.Setting;
import com.zhi.system.mapper.SettingMapper;
import com.zhi.system.service.ISettingService;

/**
 * 博客设置Service业务层处理
 * 
 * @author nevell
 * @date 2025-09-08
 */
@Service
public class SettingServiceImpl implements ISettingService
{
    @Autowired
    private SettingMapper settingMapper;

    /**
     * 查询博客设置列表
     * 
     * @param setting 博客设置
     * @return 博客设置集合
     */
    @Override
    public List<Setting> selectSettingList(Setting setting)
    {
        return settingMapper.selectSettingList(setting);
    }

    /**
     * 通过设置键查询设置值
     * 
     * @param settingKey 设置键
     * @return 设置值
     */
    @Override
    public String selectSettingValueByKey(String settingKey)
    {
        return settingMapper.selectSettingValueByKey(settingKey);
    }

    /**
     * 通过ID查询单条数据
     * 
     * @param id 主键ID
     * @return 实例对象
     */
    @Override
    public Setting selectSettingById(Long id)
    {
        return settingMapper.selectSettingById(id);
    }

    /**
     * 新增博客设置
     * 
     * @param setting 博客设置
     * @return 结果
     */
    @Override
    public int insertSetting(Setting setting)
    {
        setting.setCreateBy(SecurityUtils.getUsername());
        return settingMapper.insertSetting(setting);
    }

    /**
     * 修改博客设置
     * 
     * @param setting 博客设置
     * @return 结果
     */
    @Override
    public int updateSetting(Setting setting)
    {
        setting.setUpdateBy(SecurityUtils.getUsername());
        return settingMapper.updateSetting(setting);
    }

    /**
     * 通过设置键修改设置值（如果不存在则创建）
     * 
     * @param settingKey 设置键
     * @param settingValue 设置值
     * @return 结果
     */
    @Override
    public int updateSettingValueByKey(String settingKey, String settingValue)
    {
        int result = settingMapper.updateSettingValueByKey(settingKey, settingValue);
        // 如果更新失败（记录不存在），则创建新记录
        if (result == 0) {
            Setting newSetting = new Setting();
            newSetting.setSettingKey(settingKey);
            newSetting.setSettingValue(settingValue);
            newSetting.setDescription("系统自动创建的设置项");
            result = settingMapper.insertSetting(newSetting);
        }
        return result;
    }

    /**
     * 通过主键删除数据
     * 
     * @param id 主键ID
     * @return 影响行数
     */
    @Override
    public int deleteSettingById(Long id)
    {
        return settingMapper.deleteSettingById(id);
    }

    /**
     * 批量删除博客设置
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    @Override
    public int deleteSettingByIds(Long[] ids)
    {
        return settingMapper.deleteSettingByIds(ids);
    }
}