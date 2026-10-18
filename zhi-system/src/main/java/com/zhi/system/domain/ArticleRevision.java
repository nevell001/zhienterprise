package com.zhi.system.domain;

import java.util.Date;

/**
 * 文章修订历史
 *
 * 每次保存文章时记录一份内容快照，用于查看与回滚历史版本。
 *
 * @author nevell
 */
public class ArticleRevision
{
    private Long id;

    private Long articleId;

    private Integer revisionNo;

    private String title;

    private String summary;

    private String content;

    private String contentMd;

    private String format;

    private String coverUrl;

    private Long categoryId;

    private String articleType;

    private String industry;

    private String scenario;

    private String seoTitle;

    private String seoKeywords;

    private String seoDescription;

    private String slug;

    private String sellingPoints;

    private String specTable;

    private String gallery;

    private String processSteps;

    private Long status;

    private Long isTop;

    private Long isRecommend;

    private Long editorId;

    private String editorName;

    private Date createTime;

    /**
     * 用文章当前状态构造快照（仅内容相关字段，状态/置顶/推荐不参与回滚）。
     */
    public static ArticleRevision fromArticle(Article article)
    {
        ArticleRevision revision = new ArticleRevision();
        revision.setArticleId(article.getId());
        revision.setTitle(article.getTitle());
        revision.setSummary(article.getSummary());
        revision.setContent(article.getContent());
        revision.setContentMd(article.getContentMd());
        revision.setFormat(article.getFormat());
        revision.setCoverUrl(article.getCoverUrl());
        revision.setCategoryId(article.getCategoryId());
        revision.setArticleType(article.getArticleType());
        revision.setIndustry(article.getIndustry());
        revision.setScenario(article.getScenario());
        revision.setSeoTitle(article.getSeoTitle());
        revision.setSeoKeywords(article.getSeoKeywords());
        revision.setSeoDescription(article.getSeoDescription());
        revision.setSlug(article.getSlug());
        revision.setSellingPoints(article.getSellingPoints());
        revision.setSpecTable(article.getSpecTable());
        revision.setGallery(article.getGallery());
        revision.setProcessSteps(article.getProcessSteps());
        revision.setStatus(article.getStatus());
        revision.setIsTop(article.getIsTop());
        revision.setIsRecommend(article.getIsRecommend());
        return revision;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getArticleId()
    {
        return articleId;
    }

    public void setArticleId(Long articleId)
    {
        this.articleId = articleId;
    }

    public Integer getRevisionNo()
    {
        return revisionNo;
    }

    public void setRevisionNo(Integer revisionNo)
    {
        this.revisionNo = revisionNo;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContentMd()
    {
        return contentMd;
    }

    public void setContentMd(String contentMd)
    {
        this.contentMd = contentMd;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    public String getCoverUrl()
    {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl)
    {
        this.coverUrl = coverUrl;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getArticleType()
    {
        return articleType;
    }

    public void setArticleType(String articleType)
    {
        this.articleType = articleType;
    }

    public String getIndustry()
    {
        return industry;
    }

    public void setIndustry(String industry)
    {
        this.industry = industry;
    }

    public String getScenario()
    {
        return scenario;
    }

    public void setScenario(String scenario)
    {
        this.scenario = scenario;
    }

    public String getSeoTitle()
    {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle)
    {
        this.seoTitle = seoTitle;
    }

    public String getSeoKeywords()
    {
        return seoKeywords;
    }

    public void setSeoKeywords(String seoKeywords)
    {
        this.seoKeywords = seoKeywords;
    }

    public String getSeoDescription()
    {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription)
    {
        this.seoDescription = seoDescription;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public String getSellingPoints()
    {
        return sellingPoints;
    }

    public void setSellingPoints(String sellingPoints)
    {
        this.sellingPoints = sellingPoints;
    }

    public String getSpecTable()
    {
        return specTable;
    }

    public void setSpecTable(String specTable)
    {
        this.specTable = specTable;
    }

    public String getGallery()
    {
        return gallery;
    }

    public void setGallery(String gallery)
    {
        this.gallery = gallery;
    }

    public String getProcessSteps()
    {
        return processSteps;
    }

    public void setProcessSteps(String processSteps)
    {
        this.processSteps = processSteps;
    }

    public Long getStatus()
    {
        return status;
    }

    public void setStatus(Long status)
    {
        this.status = status;
    }

    public Long getIsTop()
    {
        return isTop;
    }

    public void setIsTop(Long isTop)
    {
        this.isTop = isTop;
    }

    public Long getIsRecommend()
    {
        return isRecommend;
    }

    public void setIsRecommend(Long isRecommend)
    {
        this.isRecommend = isRecommend;
    }

    public Long getEditorId()
    {
        return editorId;
    }

    public void setEditorId(Long editorId)
    {
        this.editorId = editorId;
    }

    public String getEditorName()
    {
        return editorName;
    }

    public void setEditorName(String editorName)
    {
        this.editorName = editorName;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
