package com.zhi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 博客设置对象 zhi_setting
 * 
 * @author nevell
 * @date 2025-09-08
 */
public class BlogSetting extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 设置键 */
    @Excel(name = "设置键")
    private String settingKey;

    /** 设置值 */
    @Excel(name = "设置值")
    private String settingValue;

    // 兼容前端字段名
    public String getConfigKey() {
        return settingKey;
    }

    public void setConfigKey(String configKey) {
        this.settingKey = configKey;
    }

    public String getConfigValue() {
        return settingValue;
    }

    public void setConfigValue(String configValue) {
        this.settingValue = configValue;
    }

    public String getConfigName() {
        return description;
    }

    public void setConfigName(String configName) {
        this.description = configName;
    }

    /** 设置描述 */
    @Excel(name = "设置描述")
    private String description;

    /** 删除标志（0代表存在 1代表删除） */
    private String delFlag;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setSettingKey(String settingKey) 
    {
        this.settingKey = settingKey;
    }

    public String getSettingKey() 
    {
        return settingKey;
    }

    public void setSettingValue(String settingValue) 
    {
        this.settingValue = settingValue;
    }

    public String getSettingValue() 
    {
        return settingValue;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("settingKey", getSettingKey())
            .append("settingValue", getSettingValue())
            .append("description", getDescription())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}