package com.zhi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 留言板对象 zhi_message
 * 
 * @author nevell
 * @date 2026-09-10
 */
public class BlogMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 留言ID */
    private Long id;

    /** 留言用户ID（匿名为空） */
    private Long userId;

    /** 昵称 */
    @Excel(name = "昵称")
    private String nickname;

    /** 邮箱（不公开） */
    @Excel(name = "邮箱")
    private String email;

    /** 个人网站 */
    @Excel(name = "个人网站")
    private String website;

    /** 留言内容 */
    @Excel(name = "留言内容")
    private String content;

    /** 管理员回复内容 */
    @Excel(name = "管理员回复")
    private String replyContent;

    /** 回复时间 */
    private java.util.Date replyTime;

    /** 回复人 */
    private String replyBy;

    /** 状态（0待审核 1已发布 2已拒绝） */
    @Excel(name = "状态", readConverterExp = "0=待审核,1=已发布,2=已拒绝")
    private String status;

    /** 留言IP */
    private String ip;

    /** 浏览器UA */
    private String userAgent;

    /** 删除标志（0代表存在 1代表删除） */
    private String delFlag;

    /** 图形验证码（仅接收前端参数，不落库） */
    private String code;

    /** 验证码唯一标识（仅接收前端参数，不落库） */
    private String uuid;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setNickname(String nickname) 
    {
        this.nickname = nickname;
    }

    public String getNickname() 
    {
        return nickname;
    }

    public void setEmail(String email) 
    {
        this.email = email;
    }

    public String getEmail() 
    {
        return email;
    }

    public void setWebsite(String website) 
    {
        this.website = website;
    }

    public String getWebsite() 
    {
        return website;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setReplyContent(String replyContent) 
    {
        this.replyContent = replyContent;
    }

    public String getReplyContent() 
    {
        return replyContent;
    }

    public void setReplyTime(java.util.Date replyTime) 
    {
        this.replyTime = replyTime;
    }

    public java.util.Date getReplyTime() 
    {
        return replyTime;
    }

    public void setReplyBy(String replyBy) 
    {
        this.replyBy = replyBy;
    }

    public String getReplyBy() 
    {
        return replyBy;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setIp(String ip) 
    {
        this.ip = ip;
    }

    public String getIp() 
    {
        return ip;
    }

    public void setUserAgent(String userAgent) 
    {
        this.userAgent = userAgent;
    }

    public String getUserAgent() 
    {
        return userAgent;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    public void setCode(String code) 
    {
        this.code = code;
    }

    public String getCode() 
    {
        return code;
    }

    public void setUuid(String uuid) 
    {
        this.uuid = uuid;
    }

    public String getUuid() 
    {
        return uuid;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("nickname", getNickname())
            .append("email", getEmail())
            .append("website", getWebsite())
            .append("content", getContent())
            .append("replyContent", getReplyContent())
            .append("replyTime", getReplyTime())
            .append("replyBy", getReplyBy())
            .append("status", getStatus())
            .append("ip", getIp())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
