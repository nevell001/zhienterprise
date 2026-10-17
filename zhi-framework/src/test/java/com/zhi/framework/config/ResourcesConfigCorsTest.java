package com.zhi.framework.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CORS 配置守卫。
 *
 * 背景：原实现写死 {@code addAllowedOriginPattern("*")}，且 {@code .env.example} 里
 * 早已记录 {@code CORS_ALLOWED_ORIGINS} 却从未被读取 —— 配置项是摆设。
 * 现改为由 {@code security.cors.allowed-origins} 驱动，生产留空即拒绝启动。
 */
class ResourcesConfigCorsTest
{
    private CorsConfiguration configOf(ResourcesConfig cfg)
    {
        CorsFilter filter = cfg.corsFilter();
        assertNotNull(filter);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                (org.springframework.web.cors.UrlBasedCorsConfigurationSource)
                        ReflectionTestUtils.getField(filter, "configSource");
        assertNotNull(source, "无法取到 CorsFilter 的配置源");
        return source.getCorsConfigurations().get("/**");
    }

    private ResourcesConfig build(String profile, String origins)
    {
        ResourcesConfig cfg = new ResourcesConfig();
        ReflectionTestUtils.setField(cfg, "activeProfile", profile);
        ReflectionTestUtils.setField(cfg, "allowedOrigins", origins);
        return cfg;
    }

    /**
     * 生产未配置来源时：**不阻断启动**，但必须回退为「不放开任何跨域来源」。
     *
     * <p>刻意不抛异常：已有部署的 .env 可能没有该变量，升级即宕机的代价过高；
     * 而空白名单本身就是安全的（比原来的 {@code *} 更严），同源部署不受影响。</p>
     */
    @Test
    void productionWithoutOriginsRestrictsInsteadOfWildcard()
    {
        CorsConfiguration config = configOf(build("prod", ""));

        assertFalse(config.getAllowedOriginPatterns().contains("*"),
                "生产未配置时绝不能回退为通配符（那正是本次要修的问题）");
        assertTrue(config.getAllowedOriginPatterns().isEmpty(),
                "生产未配置时应回退为空白名单（不放开任何跨域来源）");
    }

    @Test
    void devFallsBackToWildcardWhenOriginsMissing()
    {
        CorsConfiguration config = configOf(build("dev", ""));
        assertTrue(config.getAllowedOriginPatterns().contains("*"),
                "非生产留空应回退为通配符，保留本地跨端口调试能力");
    }

    @Test
    void configuredOriginsAreAppliedAndWildcardAbsent()
    {
        CorsConfiguration config = configOf(build("prod", "https://a.example.com,https://b.example.com"));

        assertFalse(config.getAllowedOriginPatterns().contains("*"),
                "显式配置来源后不得再出现通配符");
        assertTrue(config.getAllowedOriginPatterns().contains("https://a.example.com"));
        assertTrue(config.getAllowedOriginPatterns().contains("https://b.example.com"));
    }

    @Test
    void blankItemsAreIgnored()
    {
        CorsConfiguration config = configOf(build("prod", " https://a.example.com , ,"));

        assertTrue(config.getAllowedOriginPatterns().contains("https://a.example.com"));
        assertFalse(config.getAllowedOriginPatterns().contains(""),
                "逗号产生的空项不得成为允许来源");
    }

    @Test
    void credentialsRemainDisabled()
    {
        CorsConfiguration config = configOf(build("dev", ""));
        // 通配符 + allowCredentials 是危险组合；本项目二者从未同时开启，锁住该约定
        assertFalse(Boolean.TRUE.equals(config.getAllowCredentials()),
                "不应开启 allowCredentials（配合通配符会成为高危组合）");
    }
}