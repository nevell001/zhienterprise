package com.zhi.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 统一缓存管理器
 * 提供统一的缓存操作接口，封装Redis操作
 * 
 * @author nevell
 * @since 2025-12-20
 */
@Slf4j
@Component
public class UnifiedCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    public UnifiedCacheManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     * @param timeUnit 时间单位
     */
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
            log.debug("Cache set: key={}, ttl={} {}", key, ttl, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set cache: key={}", key, e);
        }
    }

    /**
     * 原子"仅当键不存在时设置"（Redis SET NX EX）。
     *
     * 用于冷却/防重复提交等 check-then-set 场景：先 exists() 再 set() 之间存在竞态，
     * 并发请求都能通过检查。此方法把"判断+占位"合成一次原子操作。
     *
     * @return true 表示抢占成功（此前不存在）；false 表示键已存在、抢占失败
     */
    public boolean setIfAbsent(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, ttl, timeUnit);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.error("Failed to setIfAbsent cache: key={}", key, e);
            // Redis 故障时保守返回 false（视为已占用），避免放行超出频控的请求
            return false;
        }
    }

    /**
     * 设置缓存（永不过期）
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Cache set: key={}, ttl=never", key);
        } catch (Exception e) {
            log.error("Failed to set cache: key={}", key, e);
        }
    }

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit: key={}", key);
                return (T) value;
            } else {
                log.debug("Cache miss: key={}", key);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to get cache: key={}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Cache delete: key={}, result={}", key, result);
        } catch (Exception e) {
            log.error("Failed to delete cache: key={}", key, e);
        }
    }

    /**
     * 批量删除缓存
     */
    public void delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            log.debug("Cache batch delete: keys={}, count={}", keys, count);
        } catch (Exception e) {
            log.error("Failed to batch delete cache: keys={}", keys, e);
        }
    }

    /**
     * 按模式删除缓存
     */
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = scanKeys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cache delete by pattern: pattern={}, keys={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to delete cache by pattern: pattern={}", pattern, e);
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check cache existence: key={}", key, e);
            return false;
        }
    }

    /**
     * 设置过期时间
     */
    public void expire(String key, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.expire(key, ttl, timeUnit);
            log.debug("Cache expire set: key={}, ttl={} {}", key, ttl, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set cache expiration: key={}", key, e);
        }
    }

    /**
     * 获取剩余过期时间
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("Failed to get cache expiration: key={}", key, e);
            return -1;
        }
    }

    /**
     * 获取所有匹配的键
     */
    public Set<String> keys(String pattern) {
        try {
            return scanKeys(pattern);
        } catch (Exception e) {
            log.error("Failed to get cache keys: pattern={}", pattern, e);
            return null;
        }
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        try {
            Set<String> keys = scanKeys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cache cleared: {} keys", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getStats() {
        try {
            // 获取Redis的内存使用情况
            Properties info = redisTemplate.getConnectionFactory()
                    .getConnection().serverCommands().info();
            
            return Map.of(
                "total_keys", keys("*") != null ? keys("*").size() : 0,
                "redis_memory", info.getProperty("used_memory_human", "N/A"),
                "redis_version", info.getProperty("redis_version", "N/A")
            );
        } catch (Exception e) {
            log.error("Failed to get cache stats", e);
            return Map.of("error", e.getMessage());
        }
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new LinkedHashSet<>();
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                while (cursor.hasNext()) {
                    keys.add(redisTemplate.getStringSerializer().deserialize(cursor.next()));
                }
            }
            return null;
        });
        return keys;
    }
}
