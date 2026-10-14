package com.zhi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 博客标签对象 zhi_tag
 * 
 * @author nevell
 * @date 2025-09-08
 */
public class BlogTag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 标签ID */
    private Long id;

    /** 标签名称 */
    @Excel(name = "标签名称")
    private String name;

    /** 删除标志（0代表存在 1代表删除） */
    private Integer delFlag;

    /** 关联文章数 */
    @Excel(name = "关联文章数")
    private Integer articleCount;

    /** 标签描述 */
    @Excel(name = "标签描述")
    private String description;

    /** 标签颜色 */
    @Excel(name = "标签颜色")
    private String color;

    /** 标签图标 */
    @Excel(name = "标签图标")
    private String icon;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setDelFlag(Integer delFlag) 
    {
        this.delFlag = delFlag;
    }

    public Integer getDelFlag() 
    {
        if (delFlag == null) {
            // 如果字段为null，设置默认值并返回
            this.delFlag = 0;
        }
        return delFlag;
    }

    public void setArticleCount(Integer articleCount) 
    {
        this.articleCount = articleCount;
    }

    public Integer getArticleCount() 
    {
        return articleCount;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setColor(String color) 
    {
        this.color = color;
    }

    public String getColor() 
    {
        return color;
    }

    public void setIcon(String icon) 
    {
        this.icon = icon;
    }

    public String getIcon() 
    {
        return icon;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("articleCount", getArticleCount())
            .append("description", getDescription())
            .append("color", getColor())
            .append("icon", getIcon())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}