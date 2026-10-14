package com.zhi.common.cache.annotation;

import java.lang.annotation.*;

/**
 * 博客系统缓存清除注解
 * 
 * @author nevell
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface SiteCacheEvict
{
    /**
     * 要清除的缓存键名，支持SpEL表达式
     */
    String[] value() default {};

    /**
     * 缓存键模式（支持通配符）
     */
    String keyPattern() default "";

    /**
     * 是否清除所有相关缓存
     */
    boolean allEntries() default false;

    /**
     * 缓存清除的条件，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 缓存清除的排除条件，支持SpEL表达式
     */
    String unless() default "";
}