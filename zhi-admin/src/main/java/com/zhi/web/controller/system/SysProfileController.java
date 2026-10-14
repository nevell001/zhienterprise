package com.zhi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.zhi.common.annotation.Log;
import com.zhi.common.config.RuoYiConfig;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.system.domain.SysConfig;
import com.zhi.common.enums.BusinessType;
import com.zhi.common.utils.DateUtils;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.file.FileUploadUtils;
import com.zhi.common.utils.file.FileUtils;
import com.zhi.common.utils.file.MimeTypeUtils;
import com.zhi.framework.web.service.TokenService;
import com.zhi.system.service.ISysUserService;
import com.zhi.system.service.ISysConfigService;
import com.zhi.system.service.ISettingService;
import com.zhi.system.domain.Upload;
import com.zhi.system.service.IUploadService;
import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.constant.CacheConstants;

/**
 * 个人信息 业务处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISettingService settingService;

    @Autowired
    private IUploadService uploadService;

    @Autowired
    private UnifiedCacheManager unifiedCacheManager;

    @Autowired
    private ISysConfigService configService;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> params)
    {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        LoginUser loginUser = getLoginUser();
        Long userId = loginUser.getUserId();
        String password = loginUser.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userId, newPassword) > 0)
        {
            // 更新缓存用户密码&密码最后更新时间
            loginUser.getUser().setPwdUpdateDate(DateUtils.getNowDate());
            loginUser.getUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception
    {
        if (!file.isEmpty())
        {
            LoginUser loginUser = getLoginUser();
            String avatar = FileUploadUtils.uploadAvatar(RuoYiConfig.getAvatarPath(), file, true);
            recordUpload(file, avatar);
            if (userService.updateUserAvatar(loginUser.getUserId(), avatar))
            {
                // 同时更新站点设置中的头像，确保前台首页显示最新头像
                settingService.updateSettingValueByKey("blog_avatar", avatar);

                // 同时更新 sys_config 表中的 blog_avatar（前台API从这里读取）
                try {
                    // 先尝试获取现有配置
                    SysConfig config = new SysConfig();
                    config.setConfigKey("blog_avatar");
                    SysConfig existingConfig = configService.selectConfigList(config).stream()
                        .filter(c -> "blog_avatar".equals(c.getConfigKey()))
                        .findFirst()
                        .orElse(null);

                    if (existingConfig != null) {
                        // 更新现有配置
                        existingConfig.setConfigValue(avatar);
                        configService.updateConfig(existingConfig);
                        logger.info("已更新 sys_config 表中的 blog_avatar: {}", avatar);
                    } else {
                        // 创建新配置
                        config.setConfigName("站点头像");
                        config.setConfigValue(avatar);
                        config.setConfigType("Y");
                        configService.insertConfig(config);
                        logger.info("已在 sys_config 表中创建 blog_avatar: {}", avatar);
                    }
                } catch (Exception e) {
                    logger.error("更新 sys_config 表中的 blog_avatar 失败", e);
                }

                // 清除站点设置缓存，确保前台能立即看到新头像
                try {
                    unifiedCacheManager.delete(CacheConstants.SETTINGS_ALL);
                    logger.info("已清除站点设置缓存");
                } catch (Exception e) {
                    logger.error("清除站点设置缓存失败", e);
                }

                String oldAvatar = loginUser.getUser().getAvatar();
                if (StringUtils.isNotEmpty(oldAvatar))
                {
                    FileUtils.deleteFile(RuoYiConfig.getProfile() + FileUtils.stripPrefix(oldAvatar));
                }
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }

    /**
     * 记录用户头像上传到媒体库
     */
    private void recordUpload(MultipartFile file, String avatarPath)
    {
        try
        {
            Upload record = new Upload();
            record.setFileName(FileUtils.getName(avatarPath));
            record.setOriginalName(file.getOriginalFilename());
            record.setUrl(avatarPath);
            record.setMimeType(file.getContentType());
            record.setFileSize(file.getSize());
            record.setUploadType("avatar");
            uploadService.recordUpload(record);
        }
        catch (Exception e)
        {
            logger.error("记录头像上传信息失败", e);
        }
    }
}
