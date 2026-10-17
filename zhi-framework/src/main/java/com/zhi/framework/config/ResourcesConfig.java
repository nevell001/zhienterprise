package com.zhi.framework.config;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.zhi.common.config.RuoYiConfig;
import com.zhi.common.constant.Constants;
import com.zhi.common.utils.StringUtils;
import com.zhi.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 通用配置
 * 
 * @author ruoyi
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    private static final Logger log = LoggerFactory.getLogger(ResourcesConfig.class);

    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    /**
     * CORS 允许来源（逗号分隔）。生产由环境变量 CORS_ALLOWED_ORIGINS 注入，留空则启动失败。
     */
    @Value("${security.cors.allowed-origins:}")
    private String allowedOrigins;

    /**
     * 当前激活的配置环境
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        /** 本地文件上传路径 */
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + RuoYiConfig.getProfile() + "/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .resourceChain(true)
                .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver() {
                    @Override
                    protected org.springframework.core.io.Resource getResource(String resourcePath, org.springframework.core.io.Resource location) throws java.io.IOException {
                        org.springframework.core.io.Resource requestedResource = location.createRelative(resourcePath);
                        return requestedResource.exists() && requestedResource.isReadable() ? requestedResource : null;
                    }
                });

        // 注意：SpringDoc OpenAPI 会自动处理 Swagger UI 资源，无需手动配置
    }

    /**
     * 自定义拦截规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**")
                .excludePathPatterns("/manage/actuator/**")
                .excludePathPatterns("/system/article/**")
                .excludePathPatterns("/api/article/**");
    }

    /**
     * 配置视图控制器
     * 将根路径映射到 index.html，但排除 actuator 端点
     */
    @Override
    public void addViewControllers(org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry)
    {
        // 将根路径映射到 index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        
        // 不配置 actuator 端点，让 Actuator 框架自己处理
    }

    /**
     * 跨域配置。
     *
     * <p>允许来源由 {@code security.cors.allowed-origins} 驱动（逗号分隔），生产从环境变量
     * {@code CORS_ALLOWED_ORIGINS} 注入。之所以不再写死 {@code addAllowedOriginPattern("*")}：
     * 前后端在生产是同源部署（nginx 反代），CORS 本不需要放开；而通配符会让任意站点在浏览器
     * 侧调用本 API（虽有 JWT 兜底，但会把"能发请求"的面放大到全互联网）。</p>
     *
     * <p>未配置时的行为按环境区分，且**刻意选择"不阻断启动"**：</p>
     * <ul>
     *   <li><b>非生产</b>：回退 {@code *}，保留本地跨端口（3000→8080）调试能力。</li>
     *   <li><b>生产</b>：回退为**不放开任何跨域来源**（同源部署下前端照常工作），
     *       并打印醒目的 WARN 提示配置。这里不抛异常：已有部署的 {@code .env} 可能没有该变量，
     *       直接拒绝启动会把"安全加固"变成"升级即宕机"；而回退为空白名单本身是安全的
     *       （比原来的 {@code *} 更严），只是失去了显式声明的可见性。</li>
     * </ul>
     */
    @Bean
    public CorsFilter corsFilter()
    {
        CorsConfiguration config = new CorsConfiguration();

        String origins = allowedOrigins == null ? "" : allowedOrigins.trim();
        if (StringUtils.isEmpty(origins))
        {
            if (isProduction())
            {
                // 空白名单 = 不放开任何跨域来源。同源部署（nginx 反代）不受影响。
                log.warn("⚠️  生产环境未配置 security.cors.allowed-origins / CORS_ALLOWED_ORIGINS："
                    + "已按「不放开任何跨域来源」处理（比通配符更安全，同源部署不受影响）。"
                    + "若确有跨域需求，请设置 CORS_ALLOWED_ORIGINS=https://your-domain.com（逗号分隔多个）。");
                config.setAllowedOriginPatterns(java.util.Collections.emptyList());
                config.addAllowedHeader("*");
                config.addAllowedMethod("*");
                config.setMaxAge(1800L);
                config.addExposedHeader("Content-Disposition");
                UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
                src.registerCorsConfiguration("/**", config);
                return new CorsFilter(src);
            }
            // 非生产：保留通配符以便本地跨端口调试
            log.info("CORS 未配置来源，非生产环境回退为通配符（仅限本地调试）");
            origins = "*";
        }

        for (String origin : origins.split(","))
        {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty())
            {
                config.addAllowedOriginPattern(trimmed);
            }
        }

        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 仅暴露必要响应头，避免把内部头信息透给跨域调用方
        config.addExposedHeader("Content-Disposition");
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }

    /**
     * 是否生产环境（与 SecurityConfigValidator 同口径）
     */
    private boolean isProduction()
    {
        return activeProfile != null && activeProfile.toLowerCase().contains("prod");
    }
}