package com.zhi.framework.config;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.zhi.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 通用配置
 * 
 * @author ruoyi
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

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
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter()
    {
        CorsConfiguration config = new CorsConfiguration();
        // 设置访问源地址
        config.addAllowedOriginPattern("*");
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }
}