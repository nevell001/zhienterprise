package com.zhi.common.cache.config;

import com.zhi.common.cache.SiteCacheManager;
import com.zhi.common.cache.UnifiedCacheManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 缓存配置类
 * 配置统一的缓存管理组件
 * 
 * @author nevell
 * @since 2025-12-20
 */
@Configuration
@EnableAspectJAutoProxy
public class CacheConfiguration {

    /**
     * 统一缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public UnifiedCacheManager unifiedCacheManager(@Qualifier("cacheRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        return new UnifiedCacheManager(redisTemplate);
    }

    /**
     * 博客专用缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.redis.host", matchIfMissing = false)
    public SiteCacheManager siteCacheManager(UnifiedCacheManager unifiedCacheManager) {
        return new SiteCacheManager(unifiedCacheManager);
    }
}