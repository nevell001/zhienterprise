package com.zhi.system.service;

/**
 * 博客邮件服务接口
 *
 * @author nevell
 * @date 2025-01-26
 */
public interface IEmailService
{
    /** 验证码类型：注册 */
    String CODE_TYPE_REGISTER = "register";

    /** 验证码类型：重置密码 */
    String CODE_TYPE_RESET = "reset";

    /** 验证码类型：绑定邮箱 */
    String CODE_TYPE_BIND = "bind";

    /**
     * 发送注册验证码
     *
     * @param email 邮箱地址
     * @param ipAddress 请求IP地址
     * @return 结果
     */
    boolean sendRegisterCode(String email, String ipAddress);

    /**
     * 发送密码重置验证码
     *
     * @param email 邮箱地址
     * @param ipAddress 请求IP地址
     * @return 结果
     */
    boolean sendResetCode(String email, String ipAddress);

    /**
     * 验证验证码是否有效
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @param codeType 验证码类型
     * @return 结果
     */
    boolean verifyCode(String email, String code, String codeType);

    /**
     * 获取验证码
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @param codeType 验证码类型
     * @return 验证码对象
     */
    Object getCode(String email, String code, String codeType);

    /**
     * 删除过期的验证码
     *
     * @return 结果
     */
    int cleanExpiredCodes();

    /**
     * 发送站内通知邮件（评论/回复/审核结果等）
     *
     * @param email 收件邮箱
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本）
     * @return true=发送成功
     */
    boolean sendNotificationMail(String email, String subject, String content);

    /**
     * 检查邮箱频率限制
     *
     * @param email 邮箱地址
     * @param codeType 验证码类型
     * @return true=允许发送, false=频率限制
     */
    boolean checkEmailRateLimit(String email, String codeType);

    /**
     * 检查IP频率限制
     *
     * @param ipAddress IP地址
     * @return true=允许发送, false=频率限制
     */
    boolean checkIpRateLimit(String ipAddress);

    /**
     * 是否处于"开发环境打印验证码"模式：为真时验证码只写日志、不会真实发信。
     * 生产 profile 下恒为 false。
     *
     * @return true=只打印不发信
     */
    boolean isDevPrintCodeEnabled();
}
