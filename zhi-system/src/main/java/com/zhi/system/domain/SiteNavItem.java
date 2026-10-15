package com.zhi.system.domain;

import java.util.List;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 站点导航项对象 site_nav_item
 *
 * @author zhi
 */
public class SiteNavItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 位置：header 头部导航 footer 页脚快速导航 */
    private String location;

    /** 父项ID，0=顶级 */
    private Long parentId;

    /** 导航标签 */
    private String label;

    /** 链接类型：path 站内路径 url 外链 cta 咨询按钮 */
    private String linkType;

    /** 链接值 */
    private String linkValue;

    /** 排序 */
    private Integer sort;

    /** 是否显示（0否 1是） */
    private Integer visible;

    /** 二级子项（仅树形返回用，非数据库字段） */
    private List<SiteNavItem> children;

    public List<SiteNavItem> getChildren()
    {
        return children;
    }

    public void setChildren(List<SiteNavItem> children)
    {
        this.children = children;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getLocation()
    {
        return location;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLinkType(String linkType)
    {
        this.linkType = linkType;
    }

    public String getLinkType()
    {
        return linkType;
    }

    public void setLinkValue(String linkValue)
    {
        this.linkValue = linkValue;
    }

    public String getLinkValue()
    {
        return linkValue;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setVisible(Integer visible)
    {
        this.visible = visible;
    }

    public Integer getVisible()
    {
        return visible;
    }
}
