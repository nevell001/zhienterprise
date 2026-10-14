package com.zhi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 自定义页面对象 zhi_page
 * 
 * @author nevell
 * @date 2026-09-10
 */
public class BlogPage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 页面ID */
    private Long id;

    /** 页面标题 */
    @Excel(name = "页面标题")
    private String title;

    /** 页面别名（前台 URL 使用） */
    @Excel(name = "页面别名")
    private String slug;

    /** 页面摘要 */
    @Excel(name = "页面摘要")
    private String summary;

    /** 页面内容（Markdown） */
    private String content;

    /** 状态（0草稿 1已发布） */
    @Excel(name = "状态", readConverterExp = "0=草稿,1=已发布")
    private String status;

    /** 是否在导航显示（0否 1是） */
    @Excel(name = "导航显示", readConverterExp = "0=否,1=是")
    private String showInNav;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sort;

    /** 浏览次数 */
    @Excel(name = "浏览次数")
    private Long viewCount;

    /** SEO标题 */
    private String seoTitle;

    /** SEO关键词 */
    private String seoKeywords;

    /** SEO描述 */
    private String seoDescription;

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

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setSlug(String slug) 
    {
        this.slug = slug;
    }

    public String getSlug() 
    {
        return slug;
    }

    public void setSummary(String summary) 
    {
        this.summary = summary;
    }

    public String getSummary() 
    {
        return summary;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setShowInNav(String showInNav) 
    {
        this.showInNav = showInNav;
    }

    public String getShowInNav() 
    {
        return showInNav;
    }

    public void setSort(Integer sort) 
    {
        this.sort = sort;
    }

    public Integer getSort() 
    {
        return sort;
    }

    public void setViewCount(Long viewCount) 
    {
        this.viewCount = viewCount;
    }

    public Long getViewCount() 
    {
        return viewCount;
    }

    public void setSeoTitle(String seoTitle) 
    {
        this.seoTitle = seoTitle;
    }

    public String getSeoTitle() 
    {
        return seoTitle;
    }

    public void setSeoKeywords(String seoKeywords) 
    {
        this.seoKeywords = seoKeywords;
    }

    public String getSeoKeywords() 
    {
        return seoKeywords;
    }

    public void setSeoDescription(String seoDescription) 
    {
        this.seoDescription = seoDescription;
    }

    public String getSeoDescription() 
    {
        return seoDescription;
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
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("slug", getSlug())
            .append("summary", getSummary())
            .append("status", getStatus())
            .append("showInNav", getShowInNav())
            .append("sort", getSort())
            .append("viewCount", getViewCount())
            .append("seoTitle", getSeoTitle())
            .append("seoKeywords", getSeoKeywords())
            .append("seoDescription", getSeoDescription())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
