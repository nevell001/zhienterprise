package com.zhi.framework.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;
import com.zhi.framework.config.properties.PermitAllUrlProperties;
import com.zhi.framework.security.filter.JwtAuthenticationTokenFilter;
import com.zhi.framework.security.handle.AuthenticationEntryPointImpl;
import com.zhi.framework.security.handle.LogoutSuccessHandlerImpl;

/**
 * spring security配置
 * 
 * @author ruoyi
 */
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfig
{
    /**
     * 自定义用户认证逻辑
     */
    @Autowired
    private UserDetailsService userDetailsService;
    
    /**
     * 认证失败处理类
     */
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    /**
     * 退出处理类
     */
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    /**
     * token认证过滤器
     */
    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;
    
    /**
     * 跨域过滤器
     */
    @Autowired
    private CorsFilter corsFilter;

    /**
     * 允许匿名访问的地址
     */
    @Autowired
    private PermitAllUrlProperties permitAllUrl;

    /**
     * 当前环境配置
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 身份验证实现
     */
    @Bean
    public AuthenticationManager authenticationManager()
    {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder());
        return new ProviderManager(daoAuthenticationProvider);
    }

    /**
     * anyRequest          |   匹配所有请求路径
     * access              |   SpringEl表达式结果为true时可以访问
     * anonymous           |   匿名可以访问
     * denyAll             |   用户不能访问
     * fullyAuthenticated  |   用户完全认证可以访问（非remember-me下自动登录）
     * hasAnyAuthority     |   如果有参数，参数表示权限，则其中任何一个权限可以访问
     * hasAnyRole          |   如果有参数，参数表示角色，则其中任何一个角色可以访问
     * hasAuthority        |   如果有参数，参数表示权限，则其权限可以访问
     * hasIpAddress        |   如果有参数，参数表示IP地址，如果用户IP和参数匹配，则可以访问
     * hasRole             |   如果有参数，参数表示角色，则其角色可以访问
     * permitAll           |   用户可以任意访问
     * rememberMe          |   允许通过remember-me登录的用户访问
     * authenticated       |   用户登录后可访问
     */
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception
    {
        return httpSecurity
            // CSRF禁用，因为不使用session
            .csrf(csrf -> csrf.disable())
            // 禁用HTTP响应标头
            .headers((headersCustomizer) -> {
                headersCustomizer.cacheControl(cache -> cache.disable()).frameOptions(options -> options.disable());
            })
            // 认证失败处理类
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            // 基于token，所以不需要session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 注解标记允许匿名访问的url
            .authorizeHttpRequests((requests) -> {
                permitAllUrl.getUrls().forEach(url -> {
                    requests.requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher(url)).permitAll();
                });
                // 对于登录login 注册register 验证码captchaImage 统一认证auth 允许匿名访问
                requests.requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/login"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/register"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/captchaImage"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/auth/**")).permitAll()
                    // 静态资源与 SEO 文件，可匿名访问
                    .requestMatchers(org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/*.html"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/**/*.html"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/**/*.css"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/**/*.js"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/sitemap.xml"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/robots.txt"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/profile/**")).permitAll();

                // 错误转发端点必须放行：过滤器/静态资源用 sendError() 触发的是 ERROR 转发，
                // 若 /error 被鉴权拦截，返回体与状态码会被 401 处理器覆盖
                // （防盗链 403 曾被包装成 HTTP 200 + code 401）
                requests.requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/error")).permitAll();
                
                // Actuator 安全监控端点（所有环境允许访问，不含敏感信息）
                requests.requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator/health/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator/info"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator/metrics/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator/prometheus")
                ).permitAll();

                // 开发环境允许访问Swagger和Actuator敏感端点
                if (!"prod".equals(activeProfile)) {
                    requests.requestMatchers(
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/swagger-ui.html"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/v3/api-docs/**"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/swagger-resources/**"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/webjars/**"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator/env"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator/env/**"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator/configprops"),
                            org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/manage/actuator/configprops/**")
                    ).permitAll();
                }

                // 站点前台接口：@Anonymous注解机制自动处理匿名访问
                // 不使用 /blog/** 和 /common/blog/** 全路径permitAll，
                // 以确保写操作（如修改设置、清除缓存）需要认证
                requests.requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/index"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/about"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/system/friendLink/front/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/system/friendLink/apply")
                ).permitAll()
                    // 除上面外的所有请求全部需要鉴权认证
                    .anyRequest().authenticated();
            })
            // 添加Logout filter
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler))
            // 添加JWT filter
            .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
            // 添加CORS filter
            .addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class)
            .addFilterBefore(corsFilter, LogoutFilter.class)
            .build();
    }

    /**
     * 强散列哈希加密实现
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}
