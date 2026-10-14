package com.zhi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 文章标签关联对象 zhi_article_tag
 * 
 * @author nevell
 * @date 2025-09-08
 */
public class BlogArticleTag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 文章ID */
    @Excel(name = "文章ID")
    private Long articleId;

    /** 标签ID */
    @Excel(name = "标签ID")
    private Long tagId;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setArticleId(Long articleId) 
    {
        this.articleId = articleId;
    }

    public Long getArticleId() 
    {
        return articleId;
    }

    public void setTagId(Long tagId) 
    {
        this.tagId = tagId;
    }

    public Long getTagId() 
    {
        return tagId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("articleId", getArticleId())
            .append("tagId", getTagId())
            .toString();
    }
}