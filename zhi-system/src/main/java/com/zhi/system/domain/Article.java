package com.zhi.system.domain;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 博客文章对象 zhi_article
 * 
 * @author nevell
 * @date 2025-07-18
 */

public class Article extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 文章标题 */
    @Excel(name = "文章标题")
    private String title;

    /** 摘要 */
    @Excel(name = "摘要")
    private String summary;

    /** 文章内容（HTML，展示用） */
    @Excel(name = "文章内容")
    private String content;

    /** Markdown 源码（format=markdown 时保存） */
    private String contentMd;

    /** 内容格式 html|markdown */
    private String format;

    /** 封面图片 */
    @Excel(name = "封面图片")
    private String coverUrl;

    /** 分类ID */
    @Excel(name = "分类ID")
    private Long categoryId;

    /** 分类名称（非数据库字段，用于显示） */
    private String categoryName;

    /** 作者ID */
    @Excel(name = "作者ID")
    private Long authorId;

    /** 作者姓名 */
    @Excel(name = "作者姓名")
    private String author;

    /** 作者姓名（非数据库字段，用于显示） */
    private String authorName;

    /** 是否置顶 0否 1是 */
    @Excel(name = "是否置顶 0否 1是")
    private Long isTop;

    /** 是否推荐 0否 1是 */
    @Excel(name = "是否推荐 0否 1是")
    private Long isRecommend;

    /** 状态 0草稿 1发布 2定时发布 */
    @Excel(name = "状态 0草稿 1发布 2定时发布")
    private Long status;

    /** 定时发布时间（status=2 时生效） */
    private java.util.Date publishTime;

    /** 浏览量 */
    @Excel(name = "浏览量")
    private Long viewCount;

    /** 点赞数 */
    @Excel(name = "点赞数")
    private Long likeCount;

    /** 评论数 */
    @Excel(name = "评论数")
    private Long commentCount;

    /** 标签ID列表（用于前端显示和编辑） */
    private List<Long> tagIds;

    /** 标签列表（用于返回给前端） */
    private List<Tag> tags;

    /** 删除标志（0代表存在 2代表删除） */
    private Long delFlag;

    /** 归档日期（非数据库字段，用于查询） */
    private String archiveDate;

    /** 内容类型：news 资讯（默认）、business 核心业务、case 客户案例 */
    private String articleType;

    /** SEO 标题（留空回退文章标题） */
    private String seoTitle;

    /** SEO 关键词（英文逗号分隔） */
    private String seoKeywords;

    /** SEO 描述（留空回退文章摘要） */
    private String seoDescription;

    /** URL 别名（伪静态 /article/{slug}，留空用数字 ID） */
    private String slug;

    /** 卖点宫格（business/case）：JSON 数组 [{title,desc}] */
    private String sellingPoints;

    /** 规格参数表（business/case）：JSON 数组 [{name,value}] */
    private String specTable;

    /** 图集（business/case）：JSON 数组 [url] */
    private String gallery;

    /** 服务/实施流程步骤条（business/case）：JSON 数组 [{title,desc}] */
    private String processSteps;

    public void setArticleType(String articleType)
    {
        this.articleType = articleType;
    }

    public String getArticleType()
    {
        return articleType;
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

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSellingPoints(String sellingPoints)
    {
        this.sellingPoints = sellingPoints;
    }

    public String getSellingPoints()
    {
        return sellingPoints;
    }

    public void setSpecTable(String specTable)
    {
        this.specTable = specTable;
    }

    public String getSpecTable()
    {
        return specTable;
    }

    public void setGallery(String gallery)
    {
        this.gallery = gallery;
    }

    public String getGallery()
    {
        return gallery;
    }

    public void setProcessSteps(String processSteps)
    {
        this.processSteps = processSteps;
    }

    public String getProcessSteps()
    {
        return processSteps;
    }

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

    public void setCoverUrl(String coverUrl) 
    {
        this.coverUrl = coverUrl;
    }

    public String getCoverUrl()
    {
        return coverUrl;
    }

    // 兼容性方法，支持 coverImage 字段名
    public void setCoverImage(String coverImage)
    {
        this.coverUrl = coverImage;
    }

    public String getCoverImage()
    {
        return coverUrl;
    }

    public void setCategoryId(Long categoryId) 
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setAuthorId(Long authorId) 
    {
        this.authorId = authorId;
    }

    public Long getAuthorId() 
    {
        return authorId;
    }

    public void setAuthor(String author) 
    {
        this.author = author;
    }

    public String getAuthor() 
    {
        return author;
    }

    public void setAuthorName(String authorName) 
    {
        this.authorName = authorName;
    }

    public String getAuthorName() 
    {
        return authorName;
    }

    public void setIsTop(Long isTop) 
    {
        this.isTop = isTop;
    }

    public Long getIsTop() 
    {
        return isTop;
    }

    public void setIsRecommend(Long isRecommend) 
    {
        this.isRecommend = isRecommend;
    }

    public Long getIsRecommend() 
    {
        return isRecommend;
    }

    public void setStatus(Long status) 
    {
        this.status = status;
    }

    public Long getStatus() 
    {
        return status;
    }

    public java.util.Date getPublishTime() 
    {
        return publishTime;
    }

    public void setPublishTime(java.util.Date publishTime) 
    {
        this.publishTime = publishTime;
    }

    public void setViewCount(Long viewCount) 
    {
        this.viewCount = viewCount;
    }

    public Long getViewCount() 
    {
        return viewCount;
    }

    public void setLikeCount(Long likeCount) 
    {
        this.likeCount = likeCount;
    }

    public Long getLikeCount() 
    {
        return likeCount;
    }

    public void setCommentCount(Long commentCount) 
    {
        this.commentCount = commentCount;
    }

    public Long getCommentCount() 
    {
        return commentCount;
    }

    public void setTagIds(List<Long> tagIds) 
    {
        this.tagIds = tagIds;
    }

    public List<Long> getTagIds() 
    {
        return tagIds;
    }

    public void setTags(List<Tag> tags) 
    {
        this.tags = tags;
    }

    public List<Tag> getTags() 
    {
        return tags;
    }

    public void setDelFlag(Long delFlag)
    {
        this.delFlag = delFlag;
    }

    public Long getDelFlag()
    {
        if (delFlag == null) {
            // 如果字段为null，设置默认值并返回
            this.delFlag = 0L;
        }
        return delFlag;
    }

    public void setArchiveDate(String archiveDate)
    {
        this.archiveDate = archiveDate;
    }

    public String getArchiveDate()
    {
        return archiveDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("summary", getSummary())
            .append("content", getContent())
            .append("coverUrl", getCoverUrl())
            .append("categoryId", getCategoryId())
            .append("categoryName", getCategoryName())
            .append("authorId", getAuthorId())
            .append("author", getAuthor())
            .append("authorName", getAuthorName())
            .append("isTop", getIsTop())
            .append("isRecommend", getIsRecommend())
            .append("status", getStatus())
            .append("viewCount", getViewCount())
            .append("likeCount", getLikeCount())
            .append("commentCount", getCommentCount())
            .append("tagIds", getTagIds())
            .append("tags", getTags())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
