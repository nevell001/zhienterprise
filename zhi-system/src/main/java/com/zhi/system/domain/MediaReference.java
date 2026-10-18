package com.zhi.system.domain;

/**
 * 媒体文件引用位置（删除上传文件前的影响检查）
 *
 * @author nevell
 * @date 2026-10-18
 */
public class MediaReference
{
    /** 引用类型：article/page/setting/friend_link/user */
    private String referenceType;

    /** 引用记录主键 */
    private Long referenceId;

    /** 引用记录标题（文章标题/页面标题/设置键/友链名称/用户名） */
    private String referenceTitle;

    public String getReferenceType()
    {
        return referenceType;
    }

    public void setReferenceType(String referenceType)
    {
        this.referenceType = referenceType;
    }

    public Long getReferenceId()
    {
        return referenceId;
    }

    public void setReferenceId(Long referenceId)
    {
        this.referenceId = referenceId;
    }

    public String getReferenceTitle()
    {
        return referenceTitle;
    }

    public void setReferenceTitle(String referenceTitle)
    {
        this.referenceTitle = referenceTitle;
    }
}
