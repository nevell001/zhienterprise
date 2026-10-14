package com.zhi.system.domain;

import com.zhi.common.annotation.Excel;
import com.zhi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 博客邮箱验证码对象 zhi_email_code
 *
 * @author nevell
 * @date 2025-01-26
 */
public class EmailCode extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 邮箱地址 */
    @Excel(name = "邮箱地址")
    private String email;

    /** 验证码 */
    @Excel(name = "验证码")
    private String code;

    /** 验证码类型：register=注册, reset=重置密码, bind=绑定邮箱 */
    @Excel(name = "验证码类型")
    private String codeType;

    /** 过期时间 */
    @Excel(name = "过期时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 是否已使用：0=未使用, 1=已使用 */
    @Excel(name = "是否已使用")
    private Integer used;

    /** 请求IP地址 */
    @Excel(name = "请求IP地址")
    private String ipAddress;

    /** 使用时间 */
    @Excel(name = "使用时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date useTime;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmail()
    {
        return email;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }

    public void setCodeType(String codeType)
    {
        this.codeType = codeType;
    }

    public String getCodeType()
    {
        return codeType;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }

    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setUsed(Integer used)
    {
        this.used = used;
    }

    public Integer getUsed()
    {
        return used;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public void setUseTime(Date useTime)
    {
        this.useTime = useTime;
    }

    public Date getUseTime()
    {
        return useTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("email", getEmail())
            .append("code", getCode())
            .append("codeType", getCodeType())
            .append("expireTime", getExpireTime())
            .append("used", getUsed())
            .append("ipAddress", getIpAddress())
            .append("createTime", getCreateTime())
            .append("useTime", getUseTime())
            .toString();
    }
}
