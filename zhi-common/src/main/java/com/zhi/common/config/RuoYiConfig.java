package com.zhi.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 读取项目相关配置
 * 
 * @author ruoyi
 */
@Component
public class RuoYiConfig
{
    /** 项目名称 */
    @Value("${ruoyi.name:RuoYi}")
    private String name;

    /** 版本 */
    @Value("${ruoyi.version:1.1.0}")
    private String version;

    /** 版权年份 */
    @Value("${ruoyi.copyrightYear:2026}")
    private String copyrightYear;

    /** 上传路径 */
    private static String profile;

    /** 获取地址开关 */
    private static boolean addressEnabled;

    /** 验证码类型 */
    @Value("${ruoyi.captchaType:math}")
    private String captchaType;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getCopyrightYear()
    {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear)
    {
        this.copyrightYear = copyrightYear;
    }

    public static String getProfile()
    {
        return profile;
    }

    @Value("${ruoyi.profile}")
    public void setProfile(String profile)
    {
        RuoYiConfig.profile = profile;
    }

    public static boolean isAddressEnabled()
    {
        return addressEnabled;
    }

    @Value("${ruoyi.addressEnabled}")
    public void setAddressEnabled(boolean addressEnabled)
    {
        RuoYiConfig.addressEnabled = addressEnabled;
    }

    public String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }

    /**
     * 获取导入上传路径
     */
    public static String getImportPath()
    {
        return getProfile() + "/import";
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath()
    {
        return getProfile() + "/upload";
    }
}
