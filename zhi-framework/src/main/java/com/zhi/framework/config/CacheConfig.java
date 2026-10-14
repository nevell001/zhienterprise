package com.zhi.framework.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis缓存配置
 * 
 * @author nevell
 */
@Configuration
@EnableCaching
public class CacheConfig
{
    /**
     * 配置缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory)
    {
        // 默认缓存配置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 默认30分钟过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // 不缓存空值

        // 不同业务的缓存配置
        RedisCacheConfiguration siteCacheConfig = defaultCacheConfig.entryTtl(Duration.ofMinutes(60)); // 文章缓存1小时
        RedisCacheConfiguration hotCacheConfig = defaultCacheConfig.entryTtl(Duration.ofMinutes(120)); // 热门内容缓存2小时
        RedisCacheConfiguration settingCacheConfig = defaultCacheConfig.entryTtl(Duration.ofMinutes(1440)); // 设置缓存24小时

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withCacheConfiguration("zhi:article", siteCacheConfig)
                .withCacheConfiguration("zhi:hot", hotCacheConfig)
                .withCacheConfiguration("zhi:setting", settingCacheConfig)
                .build();
    }
}