package com.zhi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.zhi.system.domain.Setting;

/**
 * 博客设置Mapper接口
 * 
 * @author nevell
 * @date 2025-09-08
 */
public interface SettingMapper 
{
    /**
     * 查询博客设置列表
     * 
     * @param setting 博客设置
     * @return 博客设置集合
     */
    public List<Setting> selectSettingList(Setting setting);

    /**
     * 通过设置键查询设置值
     * 
     * @param settingKey 设置键
     * @return 设置值
     */
    public String selectSettingValueByKey(String settingKey);

    /**
     * 通过ID查询单条数据
     * 
     * @param id 主键ID
     * @return 实例对象
     */
    public Setting selectSettingById(Long id);

    /**
     * 新增博客设置
     * 
     * @param setting 博客设置
     * @return 影响行数
     */
    public int insertSetting(Setting setting);

    /**
     * 修改博客设置
     * 
     * @param setting 博客设置
     * @return 影响行数
     */
    public int updateSetting(Setting setting);

    /**
     * 通过设置键修改设置值
     *
     * @param settingKey 设置键
     * @param settingValue 设置值
     * @return 影响行数
     */
    public int updateSettingValueByKey(@Param("settingKey") String settingKey, @Param("settingValue") String settingValue);

    /**
     * 通过主键删除数据
     * 
     * @param id 主键ID
     * @return 影响行数
     */
    public int deleteSettingById(Long id);

    /**
     * 批量删除博客设置
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    public int deleteSettingByIds(Long[] ids);
}