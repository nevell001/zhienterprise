package com.zhi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhi.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 友情链接对象 zhi_friend_link
 * 
 * @author nevell
 * @date 2025-09-08
 */
public class FriendLink extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 友链ID */
    private Long id;

    /** 网站名称 */
    @Excel(name = "网站名称")
    private String name;

    /** 网站链接 */
    @Excel(name = "网站链接")
    private String url;

    /** 网站logo */
    @Excel(name = "网站logo")
    private String logo;

    /** 网站描述 */
    @Excel(name = "网站描述")
    private String description;

    /** 申请人邮箱（友链申请） */
    @Excel(name = "申请人邮箱")
    /** 申请者邮箱：仅后台可见，前台查询不取该列，因此这里让 null 值不出现在 JSON 中 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String email;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sort;

    /** 状态（0正常 1停用 2待审核） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用,2=待审核")
    private String status;

    /** 删除标志（0代表存在 1代表删除） */
    private String delFlag;

    /** 图形验证码（仅前台申请时使用，不落库） */
    private String code;

    /** 图形验证码唯一标识（仅前台申请时使用，不落库） */
    private String uuid;

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

    public void setUrl(String url) 
    {
        this.url = url;
    }

    public String getUrl() 
    {
        return url;
    }

    public void setLogo(String logo) 
    {
        this.logo = logo;
    }

    public String getLogo() 
    {
        return logo;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setSort(Integer sort) 
    {
        this.sort = sort;
    }

    public Integer getSort() 
    {
        return sort;
    }

    public void setEmail(String email) 
    {
        this.email = email;
    }

    public String getEmail() 
    {
        return email;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
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

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("url", getUrl())
            .append("logo", getLogo())
            .append("description", getDescription())
            .append("sort", getSort())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}