package com.zhi.framework.config;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.zhi.common.utils.BlogSwitchUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.system.service.IBlogSettingService;

/**
 * 防盗链配置提供者
 *
 * <p>从站点设置（zhi_setting 表）运行时读取防盗链开关与允许域名白名单，
 * 使后台“站点设置 - 站点信息”可以直接管理，无需重启应用。
 * 数据库未配置（留空）时回退到 application.yml 的 referer 配置默认值。</p>
 *
 * <p>设置键：{@code referer_enabled}、{@code referer_allowed_domains}。</p>
 *
 * @author nevell
 */
@Component
public class RefererPolicy
{
    private static final Logger log = LoggerFactory.getLogger(RefererPolicy.class);

    /** 设置缓存键 */
    private static final String KEY_ENABLED = "referer_enabled";

    private static final String KEY_ALLOWED_DOMAINS = "referer_allowed_domains";

    /** 配置缓存有效期（毫秒），避免每个静态资源请求都查询数据库 */
    private static final long CACHE_TTL_MILLIS = 5000L;

    /** 配置文件中的默认开关 */
    @Value("${referer.enabled:false}")
    private boolean defaultEnabled;

    /** 配置文件中的默认允许域名（逗号分隔） */
    @Value("${referer.allowed-domains:}")
    private String defaultAllowedDomains;

    @Autowired
    private IBlogSettingService blogSettingService;

    /** 缓存值 */
    private volatile boolean enabledCache;

    private volatile List<String> allowedDomainsCache = new ArrayList<>();

    private volatile long loadedAtMillis = 0L;

    /**
     * 防盗链是否启用
     */
    public boolean isEnabled()
    {
        refreshIfNeeded();
        return enabledCache;
    }

    /**
     * 允许的域名列表
     */
    public List<String> getAllowedDomains()
    {
        refreshIfNeeded();
        return allowedDomainsCache;
    }

    /**
     * 缓存过期时重新读取配置
     */
    private void refreshIfNeeded()
    {
        long now = System.currentTimeMillis();
        if (now - loadedAtMillis >= CACHE_TTL_MILLIS)
        {
            refresh();
        }
    }

    /**
     * 从站点设置读取并解析配置，数据库未配置时回退到 yml 默认值
     */
    private synchronized void refresh()
    {
        String enabledSetting = readSetting(KEY_ENABLED);
        // 未配置时回退 yml 默认值；已配置时统一按全站开关口径判定（'1' 也视为开启）
        boolean enabled = StringUtils.isNotEmpty(enabledSetting)
                ? BlogSwitchUtils.isOn(enabledSetting)
                : defaultEnabled;

        String domainsSetting = readSetting(KEY_ALLOWED_DOMAINS);
        if (StringUtils.isEmpty(domainsSetting))
        {
            domainsSetting = defaultAllowedDomains;
        }

        this.enabledCache = enabled;
        this.allowedDomainsCache = parseDomains(domainsSetting);
        this.loadedAtMillis = System.currentTimeMillis();
        log.debug("防盗链配置已刷新: enabled={}, allowedDomains={}", enabledCache, allowedDomainsCache);
    }

    /**
     * 读取站点设置值，读取异常时返回 null（按未配置处理）
     */
    private String readSetting(String settingKey)
    {
        try
        {
            return blogSettingService.selectSettingValueByKey(settingKey);
        }
        catch (Exception e)
        {
            log.warn("读取防盗链设置失败: {}, 使用默认配置", settingKey);
            return null;
        }
    }

    /**
     * 解析允许域名：支持逗号、中文逗号、分号、换行分隔，自动去除空白与空项
     */
    private List<String> parseDomains(String domains)
    {
        List<String> result = new ArrayList<>();
        if (StringUtils.isEmpty(domains))
        {
            return result;
        }
        String[] parts = domains.split("[,\\uFF0C;\\r\\n]+");
        for (String part : parts)
        {
            String domain = part.trim();
            if (StringUtils.isNotEmpty(domain) && !result.contains(domain))
            {
                result.add(domain);
            }
        }
        return result;
    }
}
