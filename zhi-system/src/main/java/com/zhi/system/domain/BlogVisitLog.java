package com.zhi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;

/**
 * 访问明细对象 zhi_visit_log
 * 
 * @author nevell
 * @date 2026-09-10
 */
public class BlogVisitLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 明细ID */
    private Long id;

    /** 目标类型（article文章 page自定义页面） */
    @Excel(name = "目标类型", readConverterExp = "article=文章,page=自定义页面")
    private String targetType;

    /** 目标ID */
    @Excel(name = "目标ID")
    private Long targetId;

    /** 目标标题（查询时 JOIN 文章/页面表取得，非本表字段） */
    @Excel(name = "目标标题")
    private String targetTitle;

    /** 访问路径 */
    @Excel(name = "访问路径")
    private String path;

    /** 登录用户ID（匿名为空） */
    private Long userId;

    /** 访客标识（u<id> 或 ip:<ip>） */
    private String visitorKey;

    /** 访客IP */
    @Excel(name = "访客IP")
    private String ip;

    /** 浏览器UA */
    private String userAgent;

    /** 来源页 */
    @Excel(name = "来源页")
    private String referer;

    /** 是否当日该访客首次访问（1是 0否） */
    @Excel(name = "是否独立访客", readConverterExp = "1=是,0=否")
    private String isUnique;

    /** 访问日期 */
    @Excel(name = "访问日期")
    private String visitDate;

    /** 是否只看独立访客（查询条件，非本表字段） */
    private Boolean uniqueOnly;

    /** 标题/路径关键字（查询条件） */
    private String keyword;

    /** 开始日期（查询条件，yyyy-MM-dd） */
    private String beginDate;

    /** 结束日期（查询条件，yyyy-MM-dd） */
    private String endDate;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setTargetType(String targetType) 
    {
        this.targetType = targetType;
    }

    public String getTargetType() 
    {
        return targetType;
    }

    public void setTargetId(Long targetId) 
    {
        this.targetId = targetId;
    }

    public Long getTargetId() 
    {
        return targetId;
    }

    public void setTargetTitle(String targetTitle) 
    {
        this.targetTitle = targetTitle;
    }

    public String getTargetTitle() 
    {
        return targetTitle;
    }

    public void setPath(String path) 
    {
        this.path = path;
    }

    public String getPath() 
    {
        return path;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setVisitorKey(String visitorKey) 
    {
        this.visitorKey = visitorKey;
    }

    public String getVisitorKey() 
    {
        return visitorKey;
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

    public void setReferer(String referer) 
    {
        this.referer = referer;
    }

    public String getReferer() 
    {
        return referer;
    }

    public void setIsUnique(String isUnique) 
    {
        this.isUnique = isUnique;
    }

    public String getIsUnique() 
    {
        return isUnique;
    }

    public void setVisitDate(String visitDate) 
    {
        this.visitDate = visitDate;
    }

    public String getVisitDate() 
    {
        return visitDate;
    }

    public void setUniqueOnly(Boolean uniqueOnly) 
    {
        this.uniqueOnly = uniqueOnly;
    }

    public Boolean getUniqueOnly() 
    {
        return uniqueOnly;
    }

    public void setKeyword(String keyword) 
    {
        this.keyword = keyword;
    }

    public String getKeyword() 
    {
        return keyword;
    }

    public void setBeginDate(String beginDate) 
    {
        this.beginDate = beginDate;
    }

    public String getBeginDate() 
    {
        return beginDate;
    }

    public void setEndDate(String endDate) 
    {
        this.endDate = endDate;
    }

    public String getEndDate() 
    {
        return endDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("targetType", getTargetType())
            .append("targetId", getTargetId())
            .append("path", getPath())
            .append("userId", getUserId())
            .append("visitorKey", getVisitorKey())
            .append("ip", getIp())
            .append("isUnique", getIsUnique())
            .append("visitDate", getVisitDate())
            .append("createTime", getCreateTime())
            .toString();
    }
}
