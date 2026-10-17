package com.zhi.web.controller.site;

import java.util.Map;
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.annotation.RateLimiter;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.SiteRegisterBody;
import com.zhi.common.enums.LimitType;
import com.zhi.common.utils.PasswordPolicy;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.ip.IpUtils;
import com.zhi.framework.web.service.SiteUserService;
import com.zhi.system.mapper.SysUserMapper;
import com.zhi.system.service.IEmailService;
import com.zhi.system.service.ISysConfigService;
import com.zhi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证控制器
 * 处理前台用户注册、登录、发送验证码等
 *
 * @author nevell
 * @date 2025-01-26
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController
{
    @Autowired
    private SiteUserService siteUserService;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ISysConfigService configService;

    /**
     * 用户注册
     */
    @Anonymous
    @RateLimiter(key = "auth:register:", time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/register")
    public AjaxResult register(@RequestBody SiteRegisterBody registerBody, HttpServletRequest request)
    {
        // 检查注册功能是否已开启
        if (!"true".equals(configService.selectConfigByKey("sys.account.registerUser")))
        {
            return AjaxResult.error("当前系统没有开启注册功能！");
        }

        String ipAddress = IpUtils.getIpAddr(request);
        logger.info("用户注册请求：username={}, email={}, ip={}",
                registerBody.getUsername(), registerBody.getEmail(), ipAddress);

        return siteUserService.registerUser(registerBody);
    }

    /**
     * 发送注册验证码
     */
    @Anonymous
    @RateLimiter(key = "auth:register-code:", time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/send-register-code")
    public AjaxResult sendRegisterCode(@RequestBody Map<String, String> params, HttpServletRequest request)
    {
        String email = params.get("email");
        if (StringUtils.isEmpty(email))
        {
            return AjaxResult.error("邮箱不能为空");
        }

        // 检查邮箱是否已被使用
        if (!siteUserService.checkEmailUnique(email))
        {
            return AjaxResult.error("该邮箱已被注册");
        }

        String ipAddress = IpUtils.getIpAddr(request);

        boolean success = emailService.sendRegisterCode(email, ipAddress);

        if (success)
        {
            return AjaxResult.success("验证码发送成功，请查收邮箱");
        }
        else
        {
            return AjaxResult.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 发送密码重置验证码
     */
    @Anonymous
    @RateLimiter(key = "auth:reset-code:", time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/send-reset-code")
    public AjaxResult sendResetCode(@RequestBody Map<String, String> params, HttpServletRequest request)
    {
        String email = params.get("email");
        if (StringUtils.isEmpty(email))
        {
            return AjaxResult.error("邮箱不能为空");
        }

        String ipAddress = IpUtils.getIpAddr(request);

        boolean success = emailService.sendResetCode(email, ipAddress);

        if (success)
        {
            return AjaxResult.success("验证码发送成功，请查收邮箱");
        }
        else
        {
            return AjaxResult.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 重置密码
     */
    @Anonymous
    @RateLimiter(key = "auth:reset-password:", time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/reset-password")
    public AjaxResult resetPassword(@RequestBody Map<String, String> params)
    {
        String email = params.get("email");
        String code = params.get("code");
        String newPassword = params.get("newPassword");
        String confirmPassword = params.get("confirmPassword");

        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(code) ||
                StringUtils.isEmpty(newPassword) || StringUtils.isEmpty(confirmPassword))
        {
            return AjaxResult.error("参数不能为空");
        }

        if (!newPassword.equals(confirmPassword))
        {
            return AjaxResult.error("两次输入的密码不一致");
        }

        // 口令策略校验：与注册路径共用同一入口（裸 Map 上 Bean Validation 注解不生效，
        // 此前这里只判空就写库，可把口令设成单字符）。必须在验证码校验**之前**做，
        // 否则弱口令会白烧掉一次性的邮箱验证码。
        String policyError = PasswordPolicy.check(newPassword);
        if (policyError != null)
        {
            return AjaxResult.error(policyError);
        }

        // 验证邮箱验证码
        if (!emailService.verifyCode(email, code, IEmailService.CODE_TYPE_RESET))
        {
            return AjaxResult.error("验证码错误或已过期");
        }

        try
        {
            // 根据邮箱查询用户（使用checkEmailUnique方法）
            SysUser existUser = userMapper.checkEmailUnique(email);

            if (existUser == null)
            {
                return AjaxResult.error("该邮箱未注册");
            }

            // 重置密码
            String encryptedPassword = SecurityUtils.encryptPassword(newPassword);
            userService.resetUserPwd(existUser.getUserId(), encryptedPassword);

            logger.info("密码重置成功：email={}, userId={}", email, existUser.getUserId());
            return AjaxResult.success("密码重置成功，请使用新密码登录");
        }
        catch (Exception e)
        {
            logger.error("密码重置失败：email={}, error={}", email, e.getMessage(), e);
            return AjaxResult.error("密码重置失败：" + e.getMessage());
        }
    }

}
