package com.zhi.common.core.domain.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 博客用户注册对象
 *
 * @author nevell
 * @date 2025-01-26
 */
public class SiteRegisterBody
{
    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须介于2和20之间")
    private String username;

    /** 昵称（选填） */
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /** 用户密码 */
    @NotBlank(message = "用户密码不能为空")
    @Size(min = 5, max = 20, message = "用户密码长度必须介于5和20之间")
    private String password;

    /** 确认密码 */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** 邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 邮箱验证码 */
    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "邮箱验证码必须为6位")
    private String emailCode;

    /** 图形验证码 */
    private String code;

    /** 唯一标识 */
    private String uuid;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword)
    {
        this.confirmPassword = confirmPassword;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmailCode()
    {
        return emailCode;
    }

    public void setEmailCode(String emailCode)
    {
        this.emailCode = emailCode;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
}
