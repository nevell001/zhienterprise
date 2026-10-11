package com.zhi.web.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.zhi.common.config.RuoYiConfig;

/**
 * SpringDoc OpenAPI 配置
 * 替代 Springfox，支持 Spring Boot 3.x
 *
 * @author ruoyi
 */
@Configuration
public class OpenApiConfig {

    /** 系统基础配置 */
    @Autowired
    private RuoYiConfig ruoyiConfig;

    /** 是否开启swagger */
    @Value("${springdoc.api-docs.enabled:true}")
    private boolean enabled;

    /** 服务器地址 */
    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * OpenAPI 3.0 配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 添加 server 配置，解决 base url 推断问题
                .addServersItem(new Server().url("/").description("本地服务器"))
                // API 信息
                .info(new Info()
                        .title("知企建站系统接口文档")
                        .description("知企建站系统（ZhiEnterprise）——面向企业官网的建站系统，包括文章管理、分类标签、评论互动、友情链接等完整功能模块")
                        .version("版本号:" + ruoyiConfig.getVersion())
                        .contact(new Contact()
                                .name(ruoyiConfig.getName())
                                .email(null)
                                .url(null))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // 安全配置（JWT Token）
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入 JWT Token，格式：Bearer {token}")));
    }
}