package com.zhi.common.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 博客系统专用缓存注解
 * 
 * @author nevell
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface SiteCacheable
{
    /**
     * 缓存的键名，支持SpEL表达式
     */
    String key() default "";

    /**
     * 缓存的过期时间数值
     */
    long ttl() default 30;

    /**
     * 缓存过期时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * 缓存的条件，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 缓存的排除条件，支持SpEL表达式
     */
    String unless() default "";
}