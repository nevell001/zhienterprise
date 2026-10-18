package com.zhi.framework.web.service;

import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.constant.CacheConstants;
import com.zhi.common.constant.Constants;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.entity.SysRole;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.SiteRegisterBody;
import com.zhi.common.exception.user.CaptchaException;
import com.zhi.common.exception.user.CaptchaExpireException;
import com.zhi.common.utils.DateUtils;
import com.zhi.common.utils.MessageUtils;
import com.zhi.common.utils.PasswordPolicy;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.framework.manager.AsyncManager;
import com.zhi.framework.manager.factory.AsyncFactory;
import com.zhi.system.mapper.SysRoleMapper;
import com.zhi.system.service.IEmailService;
import com.zhi.system.service.ISysConfigService;
import com.zhi.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注册用户服务
 * 用于前台用户注册登录
 *
 * @author nevell
 * @date 2025-01-26
 */
@Service
public class SiteUserService
{
    private static final Logger log = LoggerFactory.getLogger(SiteUserService.class);

    /** 注册用户类型标识 */
    private static final String USER_TYPE_REGISTERED = "01";

    /** 注册用户角色Key */
    private static final String SITE_USER_ROLE_KEY = "site_user";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private UnifiedCacheManager unifiedCacheManager;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private SysRoleMapper roleMapper;

    /**
     * 注册用户
     *
     * @param registerBody 注册信息
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult registerUser(SiteRegisterBody registerBody)
    {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        String email = registerBody.getEmail();
        String emailCode = registerBody.getEmailCode();

        // 1. 校验图形验证码
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        // 2. 校验邮箱验证码
        if (!emailService.verifyCode(email, emailCode, IEmailService.CODE_TYPE_REGISTER))
        {
            return AjaxResult.error("邮箱验证码错误或已过期");
        }

        // 3. 校验用户名和邮箱唯一性
        SysUser checkUser = new SysUser();
        checkUser.setUserName(username);
        if (!userService.checkUserNameUnique(checkUser))
        {
            return AjaxResult.error("用户名已存在");
        }

        if (!checkEmailUnique(email))
        {
            return AjaxResult.error("邮箱已被使用");
        }

        // 4. 校验密码一致性
        if (!password.equals(registerBody.getConfirmPassword()))
        {
            return AjaxResult.error("两次输入的密码不一致");
        }

        // 5. 口令策略（注册 / 重置 / 改密共用；DTO 的长度约束拦不住全同字符等弱口令）
        String policyError = PasswordPolicy.check(password);
        if (policyError != null)
        {
            return AjaxResult.error(policyError);
        }

        // 6. 创建注册用户
        SysUser siteUser = new SysUser();
        siteUser.setUserName(username);
        // 昵称优先使用用户输入的 nickname，如果没有则使用 username
        siteUser.setNickName(StringUtils.isNotEmpty(registerBody.getNickname())
            ? registerBody.getNickname()
            : username);
        siteUser.setEmail(email);
        siteUser.setPassword(SecurityUtils.encryptPassword(password));
        siteUser.setStatus("0"); // 正常状态
        siteUser.setPwdUpdateDate(DateUtils.getNowDate());
        siteUser.setUserType(USER_TYPE_REGISTERED); // 设置为注册用户类型

        try
        {
            // 7. 保存用户
            boolean regFlag = userService.registerUser(siteUser);
            if (!regFlag)
            {
                return AjaxResult.error("注册失败，请联系系统管理人员");
            }

            // 8. 分配注册用户角色（关键！）
            SysRole siteRole = roleMapper.checkRoleKeyUnique(SITE_USER_ROLE_KEY);
            if (siteRole == null)
            {
                log.error("注册用户角色不存在：role_key={}，请检查数据库初始化", SITE_USER_ROLE_KEY);
                return AjaxResult.error("系统配置异常，注册暂不可用，请联系管理员");
            }
            else
            {
                userService.insertUserAuth(siteUser.getUserId(), new Long[]{siteRole.getRoleId()});
                log.info("用户注册成功并分配角色：username={}, roleId={}", username, siteRole.getRoleId());
            }

            // 9. 记录注册日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER,
                    MessageUtils.message("user.register.success")));

            return AjaxResult.success("注册成功");
        }
        catch (Exception e)
        {
            log.error("用户注册失败：username={}, error={}", username, e.getMessage());
            return AjaxResult.error("注册失败：" + e.getMessage());
        }
    }

    /**
     * 校验邮箱是否唯一
     *
     * @param email 邮箱地址
     * @return true=唯一, false=不唯一
     */
    public boolean checkEmailUnique(String email)
    {
        if (StringUtils.isEmpty(email))
        {
            return false;
        }

        SysUser user = new SysUser();
        user.setEmail(email);
        return userService.checkEmailUnique(user);
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     */
    private void validateCaptcha(String username, String code, String uuid)
    {
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(uuid))
        {
            throw new CaptchaException();
        }

        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        String captcha = unifiedCacheManager.get(verifyKey, String.class);
        unifiedCacheManager.delete(verifyKey);

        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }

        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
