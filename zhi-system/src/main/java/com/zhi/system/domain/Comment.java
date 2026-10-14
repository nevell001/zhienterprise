package com.zhi.system.domain;

import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 博客评论
 * 
 * @author nevell
 */
public class Comment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 评论ID */
    private Long id;

    /** 文章ID */
    @Excel(name = "文章ID")
    private Long articleId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 昵称（匿名评论用） */
    @Excel(name = "昵称")
    private String nickname;

    /** 邮箱（匿名评论用） */
    @Excel(name = "邮箱")
    private String email;

    /** 头像（关联查询用户头像） */
    private String avatar;

    /** 评论内容 */
    @Excel(name = "评论内容")
    private String content;

    /** 父评论ID(0表示一级评论) */
    @Excel(name = "父评论ID")
    private Long parentId;

    /** 回复用户ID */
    @Excel(name = "回复用户ID")
    private Long replyUserId;

    /** 状态(0待审核,1已发布,2已删除) */
    @Excel(name = "状态")
    private String status;

    /** 点赞数 */
    @Excel(name = "点赞数")
    private Long likeCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getReplyUserId() {
        return replyUserId;
    }

    public void setReplyUserId(Long replyUserId) {
        this.replyUserId = replyUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("articleId", getArticleId())
            .append("userId", getUserId())
            .append("content", getContent())
            .append("parentId", getParentId())
            .append("replyUserId", getReplyUserId())
            .append("status", getStatus())
            .append("likeCount", getLikeCount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}