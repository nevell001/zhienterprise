package com.zhi.framework.config;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.zhi.common.constant.Constants;
import com.zhi.common.filter.RefererFilter;
import com.zhi.common.filter.RepeatableFilter;
import com.zhi.common.filter.XssFilter;
import com.zhi.common.utils.StringUtils;

/**
 * Filter配置
 *
 * @author ruoyi
 */
@Configuration
public class FilterConfig
{
    @Value("${xss.excludes}")
    private String excludes;

    @Value("${xss.urlPatterns}")
    private String urlPatterns;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    @ConditionalOnProperty(value = "xss.enabled", havingValue = "true")
    public FilterRegistrationBean xssFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns(StringUtils.split(urlPatterns, ","));
        registration.setName("xssFilter");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("excludes", excludes);
        registration.setInitParameters(initParameters);
        return registration;
    }

    // Spring Boot 3.x 默认已配置字符编码过滤器，无需重复注册
//    @SuppressWarnings({ "rawtypes", "unchecked" })
//    @Bean
//    public FilterRegistrationBean characterEncodingFilterRegistration()
//    {
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
//        characterEncodingFilter.setEncoding("UTF-8");
//        characterEncodingFilter.setForceEncoding(true);
//        registration.setFilter(characterEncodingFilter);
//        registration.addUrlPatterns("/*");
//        registration.setName("characterEncodingFilter");
//        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE + 1);
//        return registration;
//    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public FilterRegistrationBean refererFilterRegistration(RefererPolicy refererPolicy)
    {
        // 防盗链开关与允许域名由 RefererPolicy 在运行时提供（后台可管理），
        // 因此过滤器始终注册，是否生效由 referer_enabled 站点设置决定
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.setFilter(new RefererFilter(refererPolicy::isEnabled, refererPolicy::getAllowedDomains));
        registration.addUrlPatterns(Constants.RESOURCE_PREFIX + "/*");
        registration.setName("refererFilter");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        return registration;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public FilterRegistrationBean someFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        registration.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        return registration;
    }

}
