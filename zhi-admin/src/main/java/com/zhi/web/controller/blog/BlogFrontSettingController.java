package com.zhi.web.controller.blog;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.zhi.common.annotation.Anonymous;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.constant.CacheConstants;
import com.zhi.common.utils.StringUtils;
import com.zhi.system.domain.SysConfig;
import com.zhi.system.service.ISysConfigService;
import com.zhi.system.service.IBlogSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 站点前台设置控制器
 * 提供前台站点设置匿名访问接口
 *
 * @author nevell
 * @date 2025-12-18
 */
@Tag(name = "站点前台设置管理")
@RestController
@RequestMapping("/common/blog/setting")
public class BlogFrontSettingController extends BaseController {

    /**
     * 站点配置键白名单（与 /blog/setting 共用，统一收敛到 {@link BlogFrontConfigKeys}）
     */
    private static final String[] BLOG_CONFIG_KEYS = BlogFrontConfigKeys.BLOG_CONFIG_KEYS;

    /**
     * 站点设置缓存键
     */
    private static final String BLOG_SETTINGS_CACHE_KEY = CacheConstants.BLOG_SETTINGS_ALL;

    /**
     * 缓存过期时间（秒）
     */
    private static final int CACHE_EXPIRE_SECONDS = 600;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired
    private UnifiedCacheManager unifiedCacheManager;

    /**
     * 获取站点设置（前台匿名访问）
     */
    @Anonymous
    @Operation(summary = "获取站点设置（前台用）")
    @GetMapping
    @SuppressWarnings("unchecked")
    public AjaxResult getBlogSettings() {
        try {
            // 先从缓存读取
            Map<String, Object> cached = unifiedCacheManager.get(BLOG_SETTINGS_CACHE_KEY, Map.class);
            if (cached != null) {
                return AjaxResult.success(cached);
            }

            // 缓存不存在，查询数据库
            Map<String, Object> blogSettings = new HashMap<>();

            // 一次性查询所有系统配置，然后过滤
            try {
                List<SysConfig> allConfigs = configService.selectConfigList(new SysConfig());
                Map<String, String> configMap = allConfigs.stream()
                    .filter(c -> java.util.Arrays.asList(BLOG_CONFIG_KEYS).contains(c.getConfigKey()))
                    .collect(java.util.stream.Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));

                // 批量查询站点设置
                for (String configKey : BLOG_CONFIG_KEYS) {
                    try {
                        // 优先从 zhi_setting 表读取
                        String configValue = blogSettingService.selectSettingValueByKey(configKey);

                        // 如果 zhi_setting 表中没有，尝试从 sys_config 表读取
                        if (StringUtils.isEmpty(configValue)) {
                            configValue = configMap.get(configKey);
                        }

                        if (StringUtils.isNotEmpty(configValue)) {
                            blogSettings.put(configKey, configValue);
                        } else {
                            // 设置默认值
                            setDefaultValue(blogSettings, configKey);
                        }
                    } catch (Exception e) {
                        logger.warn("获取站点设置失败: {}, 错误: {}", configKey, e.getMessage());
                        // 设置默认值
                        setDefaultValue(blogSettings, configKey);
                    }
                }
            } catch (Exception e) {
                logger.error("批量查询系统配置失败，回退到逐个查询", e);
                // 回退到逐个查询
                for (String configKey : BLOG_CONFIG_KEYS) {
                    try {
                        String configValue = blogSettingService.selectSettingValueByKey(configKey);
                        if (StringUtils.isEmpty(configValue)) {
                            configValue = configService.selectConfigByKey(configKey);
                        }
                        if (StringUtils.isNotEmpty(configValue)) {
                            blogSettings.put(configKey, configValue);
                        } else {
                            setDefaultValue(blogSettings, configKey);
                        }
                    } catch (Exception ex) {
                        logger.warn("获取站点设置失败: {}, 错误: {}", configKey, ex.getMessage());
                        setDefaultValue(blogSettings, configKey);
                    }
                }
            }

            // 存入缓存，设置过期时间
            unifiedCacheManager.set(BLOG_SETTINGS_CACHE_KEY, blogSettings, CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

            return AjaxResult.success(blogSettings);
        } catch (Exception e) {
            logger.error("获取站点设置失败", e);
            return AjaxResult.error("获取站点设置失败");
        }
    }

    /**
     * 设置默认值
     */
    private void setDefaultValue(Map<String, Object> settings, String configKey) {
        switch (configKey) {
            case "blog_name":
                settings.put(configKey, "知企建站系统");
                break;
            case "blog_desc":
                settings.put(configKey, "欢迎来到我的站点");
                break;
            case "blog_author":
                settings.put(configKey, "nevell");
                break;
            case "blog_avatar":
                settings.put(configKey, "");
                break;
            case "blog_copyright":
                settings.put(configKey, "");
                break;
            case "blog_beian":
                settings.put(configKey, "");
                break;
            case "author_title":
                settings.put(configKey, "全栈开发工程师");
                break;
            case "blog_email":
                settings.put(configKey, "");
                break;
            case "github_url":
                settings.put(configKey, "");
                break;
            case "weibo_url":
                settings.put(configKey, "");
                break;
            case "author_location":
                settings.put(configKey, "");
                break;
            case "personal_website":
                settings.put(configKey, "");
                break;
            case "wechat_qr":
                settings.put(configKey, "");
                break;
            case "comment_enabled":
                settings.put(configKey, "true");
                break;
            case "like_enabled":
                settings.put(configKey, "true");
                break;
            case "view_count_enabled":
                settings.put(configKey, "true");
                break;
            case "share_enabled":
                settings.put(configKey, "true");
                break;
            case "search_enabled":
                settings.put(configKey, "true");
                break;
            case "sidebar_enabled":
                settings.put(configKey, "true");
                break;
            case "footer_enabled":
                settings.put(configKey, "true");
                break;
            case "beian_enabled":
                settings.put(configKey, "false");
                break;
            case "home_page_slug":
                settings.put(configKey, "");
                break;
            case "nav_category_enabled":
                settings.put(configKey, "true");
                break;
            case "nav_tag_enabled":
                settings.put(configKey, "true");
                break;
            case "nav_archive_enabled":
                settings.put(configKey, "true");
                break;
            case "copyright_enabled":
                settings.put(configKey, "true");
                break;
            case "friend_link_enabled":
                settings.put(configKey, "true");
                break;
            case "friend_link_apply_enabled":
                settings.put(configKey, "false");
                break;
            case "guestbook_enabled":
                settings.put(configKey, "false");
                break;
            default:
                settings.put(configKey, "");
                break;
        }
    }

    /**
     * 根据键获取单个设置值
     */
    @Anonymous
    @Operation(summary = "根据键获取设置值")
    @GetMapping("/value/{configKey}")
    public AjaxResult getSettingValueByKey(@PathVariable("configKey") String configKey) {
        if (!containsKey(BLOG_CONFIG_KEYS, configKey)) {
            return AjaxResult.error("不支持的配置键");
        }
        try {
            // 与 getBlogSettings() 同口径：优先读 zhi_setting，回退 sys_config，最后取默认值
            String configValue = blogSettingService.selectSettingValueByKey(configKey);
            if (StringUtils.isEmpty(configValue)) {
                configValue = configService.selectConfigByKey(configKey);
            }
            if (StringUtils.isNotEmpty(configValue)) {
                return AjaxResult.success(configValue);
            } else {
                // 返回默认值
                Map<String, Object> defaultValueMap = new HashMap<>();
                setDefaultValue(defaultValueMap, configKey);
                return AjaxResult.success(defaultValueMap.get(configKey));
            }
        } catch (Exception e) {
            logger.error("获取设置值失败: {}", configKey, e);
            return AjaxResult.error("获取设置值失败");
        }
    }

    @PreAuthorize("@ss.hasPermi('zhi:setting:edit')")
    @GetMapping("/clear-blog-cache")
    @Operation(summary = "清除站点设置缓存")
    public AjaxResult clearBlogCache() {
        try {
            // 清除站点设置主缓存
            unifiedCacheManager.delete(BLOG_SETTINGS_CACHE_KEY);
            
            // 同时也清除单个配置的缓存（兼容旧逻辑）
            for (String configKey : BLOG_CONFIG_KEYS) {
                unifiedCacheManager.delete("sys_config:" + configKey);
            }

            logger.info("所有站点设置缓存已清除，包括主缓存: " + BLOG_SETTINGS_CACHE_KEY);
            return AjaxResult.success("所有站点设置缓存已清除");
        } catch (Exception e) {
            logger.error("清除站点设置缓存失败", e);
            return AjaxResult.error("清除站点设置缓存失败: " + e.getMessage());
        }
    }

    private boolean containsKey(String[] keys, String key) {
        if (key == null) {
            return false;
        }
        return java.util.Arrays.asList(keys).contains(key);
    }
}