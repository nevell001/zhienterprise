package com.zhi.common.cache.aspect;

import com.zhi.common.cache.SiteCacheManager;
import com.zhi.common.cache.annotation.SiteCacheable;
import com.zhi.common.cache.annotation.SiteCacheEvict;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存切面处理器
 * 处理 @SiteCacheable 和 @SiteCacheEvict 注解
 * 
 * @author nevell
 * @since 2025-12-20
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class CacheAspect
{
    /** 键片段超过该长度就压缩成哈希，避免长正文进 Redis 键 */
    private static final int MAX_KEY_FRAGMENT_LENGTH = 64;

    @Autowired
    private SiteCacheManager siteCacheManager;

    /**
     * 处理 @SiteCacheable 注解
     */
    @Around("@annotation(siteCacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, SiteCacheable siteCacheable) throws Throwable {
        String cacheKey = buildCacheKey(joinPoint, siteCacheable.key());
        log.debug("🔍 CacheAspect: 缓存键 = {}", cacheKey);
        
        try {
            // 尝试从缓存获取
            Object cachedValue = siteCacheManager.get(cacheKey, Object.class);
            if (cachedValue != null) {
                log.debug("✅ CacheAspect: 缓存命中，key = {}", cacheKey);
                return cachedValue;
            }
            
            log.debug("❌ CacheAspect: 缓存未命中，key = {}", cacheKey);
            
            // 缓存未命中，执行原方法
            Object result = joinPoint.proceed();
            
            // 将结果存入缓存
            if (result != null) {
                long ttl = siteCacheable.ttl();
                TimeUnit timeUnit = siteCacheable.timeUnit();
                siteCacheManager.set(cacheKey, result, ttl, timeUnit);
                log.debug("CacheAspect: 已缓存结果，key = {}, TTL = {} {}", cacheKey, ttl, timeUnit);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ CacheAspect: 缓存操作失败，key = {}, 执行原方法", cacheKey, e);
            // 缓存操作失败时，直接执行原方法
            return joinPoint.proceed();
        }
    }

    /**
     * 处理 @SiteCacheEvict 注解
     */
    @Around("@annotation(siteCacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, SiteCacheEvict siteCacheEvict) throws Throwable {
        try {
            // 先执行原方法
            Object result = joinPoint.proceed();
            
            // 然后执行缓存清除
            if (siteCacheEvict.allEntries()) {
                // 清除所有站点相关缓存
                siteCacheManager.clearAllCache();
                log.debug("Evicted all site cache entries");
            } else {
                // 清除指定缓存
                String[] keys = siteCacheEvict.value();
                for (String key : keys) {
                    String resolvedKey = resolveSpEL(key, joinPoint);
                    siteCacheManager.delete(resolvedKey);
                    log.debug("Evicted cache key: {}", resolvedKey);
                }
                
                // 如果指定了keyPattern，按模式清除
                if (!siteCacheEvict.keyPattern().isEmpty()) {
                    siteCacheManager.deleteByPattern(siteCacheEvict.keyPattern());
                    log.debug("Evicted cache keys with pattern: {}", siteCacheEvict.keyPattern());
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Cache eviction failed", e);
            // 缓存清除失败时，仍然返回方法执行结果
            return joinPoint.proceed();
        }
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (keyExpression.isEmpty()) {
            // 如果没有指定key，使用方法签名生成默认key
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            return generateDefaultKey(signature, joinPoint.getArgs());
        }

        // 解析SpEL表达式或使用字符串模板
        String resolvedKey = resolveSpEL(keyExpression, joinPoint);
        resolvedKey = appendPageInfo(resolvedKey);
        log.debug("CacheAspect: 缓存键解析 - 原始表达式: {}, 解析结果: {}", keyExpression, resolvedKey);
        return resolvedKey;
    }

    /**
     * 键片段：字符串化参数，过长（例如 Article.toString() 含正文）时只保留哈希与长度，
     * 避免 Redis 键被撑到几十 KB，同时保持"参数不同 → 键不同"。
     */
    private String keyFragment(Object value) {
        if (value == null) {
            return "null";
        }
        String text = value.toString();
        if (text.length() <= MAX_KEY_FRAGMENT_LENGTH) {
            return text;
        }
        return "h" + Integer.toHexString(text.hashCode()) + "L" + text.length();
    }

    /**
     * 附加当前分页信息。
     *
     * <p>分页参数由 PageHelper 放在线程变量里（调用方先 {@code startPage()} 再进方法），
     * 并不在方法签名上，因此缓存键里原本没有页码 —— 第 1 页结果被缓存后，第 2/3 页会命中
     * 同一键并返回第 1 页数据（搜索接口就这样串过页）。这里统一把激活的页码/页大小并进键。</p>
     */
    private String appendPageInfo(String key) {
        try {
            com.github.pagehelper.Page<?> page = com.github.pagehelper.PageHelper.getLocalPage();
            if (page != null && page.getPageNum() > 0) {
                return key + ":p" + page.getPageNum() + ":s" + page.getPageSize();
            }
        } catch (Throwable ignored) {
            // PageHelper 不可用时保持原键
        }
        return key;
    }

    /**
     * 生成默认缓存键
     */
    private String generateDefaultKey(MethodSignature signature, Object[] args) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(signature.getDeclaringType().getSimpleName())
                  .append(".")
                  .append(signature.getMethod().getName());
        
        if (args.length > 0) {
            keyBuilder.append(":");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    keyBuilder.append(",");
                }
                keyBuilder.append(args[i] != null ? args[i].toString() : "null");
            }
        }
        
        return keyBuilder.toString();
    }

    /**
     * 解析SpEL表达式或字符串模板
     * 简化实现，支持常见的参数引用
     */
    private String resolveSpEL(String expression, ProceedingJoinPoint joinPoint) {
        log.debug("resolveSpEL: 开始解析表达式 = {}", expression);
        
        // 检查是否包含需要解析的参数引用（#开头的内容）
        if (expression.contains("#")) {
            // 简化的SpEL解析
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            log.debug("resolveSpEL: 参数名数组 = {}", java.util.Arrays.toString(paramNames));
            log.debug("resolveSpEL: 参数值数组 = {}", java.util.Arrays.toString(args));

            String result = expression;

            // 首先处理按索引的引用 #p0, #p1, ..., #a0, #a1, ...
            for (int i = 0; i < args.length; i++) {
                Object argValue = args[i];

                // 替换 #p[i]
                result = result.replace("#p" + i, keyFragment(argValue));
                // 替换 #a[i] (Spring 也支持这种格式)
                result = result.replace("#a" + i, keyFragment(argValue));
            }

            // 然后处理按参数名的引用 #paramName
            if (paramNames != null && paramNames.length > 0) {
                for (int i = 0; i < paramNames.length; i++) {
                    String paramName = paramNames[i];
                    Object argValue = args[i];

                    log.debug("resolveSpEL: 处理参数名 = {}, 值 = {}", paramName, argValue);
                    
                    result = result.replace("#" + paramName, keyFragment(argValue));
                }
            }

            log.debug("resolveSpEL: 解析完成，结果 = {}", result);
            return result;
        } else {
            // 普通字符串，直接返回
            log.debug("resolveSpEL: 普通字符串，直接返回 = {}", expression);
            return expression;
        }
    }
}
