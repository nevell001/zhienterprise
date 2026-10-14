package com.zhi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 文章分类对象 zhi_category
 *
 * @author nevell
 * @date 2025-07-18
 */
public class BlogCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 分类名称 */
    @Excel(name = "分类名称")
    private String name;

    /** 分类别名 */
    @Excel(name = "分类别名")
    private String alias;

    /** 分类描述 */
    @Excel(name = "分类描述")
    private String description;

    /** 父分类ID */
    @Excel(name = "父分类ID")
    private Long parentId;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 文章数量 */
    @Excel(name = "文章数量")
    private Integer articleCount;

    /** 状态 0正常 1停用 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private Integer status;

    /** 删除标志 0正常 1删除 */
    private String delFlag = "0";

    /** 排序（兼容旧字段） */
    @Excel(name = "排序")
    private Integer sort;

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

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public String getAlias()
    {
        return alias;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setArticleCount(Integer articleCount)
    {
        this.articleCount = articleCount;
    }

    public Integer getArticleCount()
    {
        return articleCount;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        if (delFlag == null) {
            this.delFlag = "0";
        }
        return delFlag;
    }

    // 兼容旧字段的方法
    public void setSort(Integer sort)
    {
        this.sort = sort;
        this.sortOrder = sort; // 同步到新字段
    }

    public Integer getSort()
    {
        if (sort == null && sortOrder != null) {
            return sortOrder;
        }
        return sort;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("alias", getAlias())
            .append("description", getDescription())
            .append("parentId", getParentId())
            .append("sortOrder", getSortOrder())
            .append("articleCount", getArticleCount())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
