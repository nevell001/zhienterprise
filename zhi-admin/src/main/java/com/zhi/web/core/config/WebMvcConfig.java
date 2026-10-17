package com.zhi.web.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import java.io.IOException;

/**
 * Web MVC配置
 * 用于支持SPA应用的前端路由
 * @author ruoyi
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置资源处理器，支持SPA应用history模式
     * 注意：/profile/** 的资源映射在 ResourcesConfig 中已配置，这里不需要重复配置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理，支持SPA应用history模式
        // 站点前台为短路径（与 zhi-ui/src/router/site.ts 一致），后台为 /admin
        // 注意：排除 /profile 路径，该路径由 ResourcesConfig 处理上传文件
        registry.addResourceHandler(
                        "/login", "/register", "/forgot-password", "/about",
                        "/news", "/article/**", "/business/**", "/solutions/**", "/contact", "/honors", "/history",
                        "/category/**", "/tag/**", "/guestbook", "/page/**",
                        "/friend-links/**", "/agreement", "/privacy", "/search",
                        "/admin/**")
                .addResourceLocations("classpath:/META-INF/resources/")
                .addResourceLocations("classpath:/resources/")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/public/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * 自定义资源解析器，用于支持SPA应用的history模式
     * 当请求的资源不存在时，返回index.html，让前端路由处理
     */
    private static class SpaResourceResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);

            // 如果资源不存在，且不是以下路径，返回index.html：
            // 1. API 请求（包含 /api）
            // 2. Actuator 监控端点（包含 /manage/actuator）
            // 3. 静态资源（包含 .）
            // 4. Profile 路径（包含 /profile）
            // 5. Swagger 相关路径（包含 /swagger）
            if (resource == null &&
                !resourcePath.contains("/api") &&
                !resourcePath.contains("/manage/actuator") &&
                !resourcePath.contains("/swagger") &&
                !resourcePath.contains(".") &&
                !resourcePath.startsWith("profile")) {
                return super.getResource("index.html", location);
            }

            return resource;
        }
    }
}