package com.zhi.system.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import com.zhi.system.domain.SysConfig;
import com.zhi.system.service.IBlogEmailService;
import com.zhi.system.service.ISysConfigService;
import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.utils.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Log;
import com.zhi.common.cache.annotation.BlogCacheEvict;
import com.zhi.common.constant.CacheConstants;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.enums.BusinessType;
import com.zhi.system.domain.BlogSetting;
import com.zhi.system.domain.MailConfigForm;
import com.zhi.system.service.IBlogSettingService;
import com.zhi.system.service.MailConfigService;
import com.zhi.common.core.page.TableDataInfo;

/**
 * 博客设置Controller
 * 
 * @author nevell
 * @date 2025-09-08
 */
@RestController
@RequestMapping("/system/setting")
public class BlogSettingController extends BaseController
{
    /** 注册开关的参数键与取值：两个注册入口都按"仅字面 true 开启"判定 */
    private static final String REGISTER_USER_CONFIG_KEY = "sys.account.registerUser";

    private static final String REGISTER_VALUE = "true";

    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private UnifiedCacheManager unifiedCacheManager;

    @Autowired
    private MailConfigService mailConfigService;

    @Autowired
    private IBlogEmailService blogEmailService;

    /**
     * 查询博客设置列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:list')")
    @GetMapping("/list")
    public TableDataInfo list(BlogSetting blogSetting)
    {
        startPage();
        List<BlogSetting> list = blogSettingService.selectBlogSettingList(blogSetting);
        list.forEach(this::maskMailPassword);
        return getDataTable(list);
    }

    /**
     * 通过设置键查询设置值
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:query')")
    @GetMapping("/value/{settingKey}")
    public AjaxResult getSettingValueByKey(@PathVariable("settingKey") String settingKey)
    {
        String value = blogSettingService.selectSettingValueByKey(settingKey);
        if (MailConfigService.KEY_PASSWORD.equals(settingKey))
        {
            value = (value == null || value.isEmpty()) ? "" : MailConfigService.PASSWORD_MASK;
        }
        // 显式调用 AjaxResult.success(Object)：success(String) 重载会把值当作 msg 返回，前端读 data 拿到 undefined
        return AjaxResult.success((Object) value);
    }

    /**
     * 获取博客设置详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        BlogSetting setting = blogSettingService.selectBlogSettingById(id);
        maskMailPassword(setting);
        return success(setting);
    }

    /** 管理端读取博客设置时，SMTP 密码永不返回明文 */
    private void maskMailPassword(BlogSetting setting)
    {
        if (setting != null && MailConfigService.KEY_PASSWORD.equals(setting.getSettingKey()))
        {
            String raw = setting.getSettingValue();
            setting.setSettingValue((raw == null || raw.isEmpty()) ? "" : MailConfigService.PASSWORD_MASK);
        }
    }

    /**
     * 新增博客设置
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:add')")
    @BlogCacheEvict(value = CacheConstants.BLOG_SETTINGS_ALL)
    @Log(title = "站点设置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogSetting blogSetting)
    {
        return toAjax(blogSettingService.insertBlogSetting(blogSetting));
    }

    /**
     * 修改博客设置
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:edit')")
    @BlogCacheEvict(value = CacheConstants.BLOG_SETTINGS_ALL)
    @Log(title = "站点设置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogSetting blogSetting)
    {
        return toAjax(blogSettingService.updateBlogSetting(blogSetting));
    }

    /**
     * 通过设置键修改设置值
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:edit')")
    @BlogCacheEvict(value = CacheConstants.BLOG_SETTINGS_ALL)
    @Log(title = "站点设置", businessType = BusinessType.UPDATE)
    @PutMapping("/updateByKey")
    public AjaxResult updateByKey(@RequestBody BlogSetting blogSetting)
    {
        return applySettingUpdateByKey(blogSetting);
    }

    /**
     * 通过设置键修改设置值 (POST方法支持)
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:edit')")
    @BlogCacheEvict(value = CacheConstants.BLOG_SETTINGS_ALL)
    @Log(title = "站点设置", businessType = BusinessType.UPDATE)
    @PostMapping("/updateByKey")
    public AjaxResult updateByKeyPost(@RequestBody BlogSetting blogSetting)
    {
        return applySettingUpdateByKey(blogSetting);
    }

    /**
     * 修改单个设置值，并同步到 sys_config 表。
     *
     * <p>前台公开设置 {@code GET /blog/setting}（BlogFrontController）以 sys_config 为**主数据源**，
     * 仅在 sys_config 为空时才回退 zhi_setting。因此这里的镜像同步失败会让改动"看似成功、前台却不生效"。
     * 之前把同步异常吞掉后仍返回 success，正是这个假成功；现在如实返回错误，让管理端提示用户重试。</p>
     */
    private AjaxResult applySettingUpdateByKey(BlogSetting blogSetting)
    {
        String settingKey = blogSetting.getSettingKey();
        // mail_* 走专用接口：它需要热更新 sender，且绝不能被镜像进 sys_config（含密码）
        if (settingKey != null && settingKey.startsWith("mail_"))
        {
            return AjaxResult.error("邮件配置请使用邮件服务专用设置入口");
        }

        int result = blogSettingService.updateSettingValueByKey(
            settingKey, blogSetting.getSettingValue());
        if (result <= 0)
        {
            return toAjax(result);
        }

        String settingValue = blogSetting.getSettingValue();

        // 同步 sys_config 表（前台设置的真实读取源）
        try
        {
            SysConfig query = new SysConfig();
            query.setConfigKey(settingKey);
            SysConfig existingConfig = configService.selectConfigList(query).stream()
                .filter(c -> settingKey.equals(c.getConfigKey()))
                .findFirst()
                .orElse(null);

            if (StringUtils.isEmpty(settingValue))
            {
                // 清空设置：updateConfig 的 mapper 对空值直接跳过（config_value = '' 写不进去），
                // 旧值留在 sys_config 会被前台读取的回退逻辑当作新值“复活”；而镜像行 configType='Y'
                // 又受内置参数删除守卫保护删不掉，因此用无条件写值的专用 statement 把镜像置空
                if (existingConfig != null)
                {
                    configService.updateConfigValueByKey(settingKey, "");
                    logger.info("已清空 sys_config 表中的设置镜像: {}", settingKey);
                }
            }
            else if (existingConfig != null)
            {
                existingConfig.setConfigValue(settingValue);
                configService.updateConfig(existingConfig);
                logger.info("已同步更新 sys_config 表中的 {}: {}", settingKey, settingValue);
            }
            else
            {
                SysConfig config = new SysConfig();
                config.setConfigKey(settingKey);
                config.setConfigName("站点设置 - " + settingKey);
                config.setConfigValue(settingValue);
                config.setConfigType("Y");
                configService.insertConfig(config);
                logger.info("已在 sys_config 表中创建 {}: {}", settingKey, settingValue);
            }

            // 清除缓存
            unifiedCacheManager.delete("sys_config:" + settingKey);
            logger.info("已清除缓存: sys_config:{}", settingKey);
        }
        catch (Exception e)
        {
            logger.error("同步更新 sys_config 表失败: {}", settingKey, e);
            return AjaxResult.error("站点设置已保存，但同步到公开配置失败，前台可能不会生效，请重试");
        }

        return toAjax(result);
    }

    /**
     * 用户注册开关的生效值。
     *
     * <p>该开关只存在于 sys_config（前台注册 BlogAuthController 与后台自助注册 SysRegisterController
     * 都读它），而本页其余设置读的是 zhi_setting，所以这里直接返回注册入口实际读到的值，
     * 避免卡片显示与真实拦截行为不一致。判定口径与两个注册入口相同：仅字面 "true" 为开启。</p>
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:query')")
    @GetMapping("/registration")
    public AjaxResult getRegistrationSwitch()
    {
        return success(REGISTER_VALUE.equals(configService.selectConfigByKey(REGISTER_USER_CONFIG_KEY)));
    }

    /**
     * 获取邮件（SMTP）服务配置（脱敏：不返回密码明文，仅返回是否已配置）
     *
     * <p>额外带上 dev-print-code 状态：该开关为真时验证码只打印到日志、不会真实发信，
     * 否则"测试连接成功却收不到信"在管理端无从判断。</p>
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:query')")
    @GetMapping("/mail")
    public AjaxResult getMailConfig()
    {
        Map<String, Object> view = new LinkedHashMap<>(mailConfigService.getMaskedView());
        view.put("devPrintCode", blogEmailService.isDevPrintCodeEnabled());
        return success(view);
    }

    /**
     * 保存邮件（SMTP）配置：写 zhi_setting 并热更新到活的邮件发送器（不重启、不进 sys_config）。
     * 提交空密码表示保留原密码。
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:edit')")
    @Log(title = "邮件服务配置", businessType = BusinessType.UPDATE)
    @PostMapping("/mail")
    public AjaxResult saveMailConfig(@RequestBody MailConfigForm form)
    {
        return success(mailConfigService.save(form));
    }

    /**
     * 测试邮件配置能否连接 SMTP 服务器。传入当前表单则测表单值（密码留空沿用已存密码），
     * 不传则测已保存的配置。
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:edit')")
    @PostMapping("/mail/test")
    public AjaxResult testMailConfig(@RequestBody(required = false) MailConfigForm form)
    {
        String reason = mailConfigService.testConnection(form);
        return reason == null ? success("连接成功") : error("连接失败：" + reason);
    }

    /**
     * 删除博客设置
     */
    @PreAuthorize("@ss.hasPermi('zhi:setting:remove')")
    @BlogCacheEvict(value = CacheConstants.BLOG_SETTINGS_ALL)
    @Log(title = "站点设置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogSettingService.deleteBlogSettingByIds(ids));
    }
}
