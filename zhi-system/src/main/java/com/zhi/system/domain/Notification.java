package com.zhi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 站内信通知对象 zhi_notification
 *
 * @author nevell
 * @date 2026-07-26
 */
public class Notification extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 接收用户ID */
    private Long recipientId;

    /** 发送者昵称 */
    private String senderName;

    /** 通知类型: comment=评论, reply=回复 */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容(评论摘要) */
    private String content;

    /** 关联文章ID */
    private Long articleId;

    /** 关联文章标题 */
    private String articleTitle;

    /** 关联评论ID */
    private Long commentId;

    /** 是否已读: 0=未读, 1=已读 */
    private Integer isRead;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getRecipientId()
    {
        return recipientId;
    }

    public void setRecipientId(Long recipientId)
    {
        this.recipientId = recipientId;
    }

    public String getSenderName()
    {
        return senderName;
    }

    public void setSenderName(String senderName)
    {
        this.senderName = senderName;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Long getArticleId()
    {
        return articleId;
    }

    public void setArticleId(Long articleId)
    {
        this.articleId = articleId;
    }

    public String getArticleTitle()
    {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle)
    {
        this.articleTitle = articleTitle;
    }

    public Long getCommentId()
    {
        return commentId;
    }

    public void setCommentId(Long commentId)
    {
        this.commentId = commentId;
    }

    public Integer getIsRead()
    {
        return isRead;
    }

    public void setIsRead(Integer isRead)
    {
        this.isRead = isRead;
    }

    @Override
    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("recipientId", getRecipientId())
            .append("senderName", getSenderName())
            .append("type", getType())
            .append("title", getTitle())
            .append("content", getContent())
            .append("articleId", getArticleId())
            .append("commentId", getCommentId())
            .append("isRead", getIsRead())
            .append("createTime", getCreateTime())
            .toString();
    }
}
