package com.zhi.common.cache.aspect;

import com.zhi.common.cache.BlogCacheManager;
import com.zhi.common.cache.annotation.BlogCacheEvict;
import com.zhi.common.constant.CacheConstants;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * 缓存切面单元测试
 * 
 * <p>重点覆盖 @BlogCacheEvict：博客设置写入后必须清除公开设置聚合缓存，
 * 否则前台最多 1 小时拿不到新值（曾导致后台开关“不生效”）。</p>
 *
 * @author test
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
class CacheAspectTest {

    @Mock
    private BlogCacheManager blogCacheManager;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private CacheAspect cacheAspect;

    @BeforeEach
    void setUp() throws Throwable {
        lenient().when(joinPoint.proceed()).thenReturn("ok");
    }

    private BlogCacheEvict evictAnnotation(String[] value, String keyPattern, boolean allEntries) {
        BlogCacheEvict annotation = mock(BlogCacheEvict.class);
        lenient().when(annotation.value()).thenReturn(value);
        lenient().when(annotation.keyPattern()).thenReturn(keyPattern);
        lenient().when(annotation.allEntries()).thenReturn(allEntries);
        return annotation;
    }

    @Test
    void testEvictDeletesConfiguredKey() throws Throwable {
        Object result = cacheAspect.handleCacheEvict(joinPoint,
            evictAnnotation(new String[] { CacheConstants.BLOG_SETTINGS_ALL }, "", false));

        assertEquals("ok", result);
        verify(blogCacheManager).delete(CacheConstants.BLOG_SETTINGS_ALL);
    }

    @Test
    void testEvictDeletesPatternWhenProvided() throws Throwable {
        cacheAspect.handleCacheEvict(joinPoint,
            evictAnnotation(new String[] {}, "zhi:tag:*", false));

        verify(blogCacheManager).deleteByPattern("zhi:tag:*");
    }

    @Test
    void testEvictAllEntries() throws Throwable {
        cacheAspect.handleCacheEvict(joinPoint, evictAnnotation(new String[] {}, "", true));

        verify(blogCacheManager).clearAllBlogCache();
    }

    @Test
    void testEvictStillReturnsResultWhenCacheFails() throws Throwable {
        doThrow(new RuntimeException("redis down")).when(blogCacheManager)
            .delete(CacheConstants.BLOG_SETTINGS_ALL);
        when(joinPoint.proceed()).thenReturn("ok");

        // 缓存清除失败不应影响业务结果（异常分支会再次执行目标方法）
        Object result = cacheAspect.handleCacheEvict(joinPoint,
            evictAnnotation(new String[] { CacheConstants.BLOG_SETTINGS_ALL }, "", false));

        assertEquals("ok", result);
    }

    @Test
    void cacheKeyMustIncludeActivePage() throws Exception {
        com.github.pagehelper.PageHelper.startPage(2, 5);
        try {
            // 分页信息在 PageHelper 线程变量里，直接验证键拼接工具
            String key = (String) invokePrivate("appendPageInfo", new Class<?>[] { String.class },
                new Object[] { "zhi:search:redis:null" });
            assertTrue(key.endsWith(":p2:s5"), "分页参数在 PageHelper 线程变量里，必须并进缓存键，否则第 2 页命中第 1 页：" + key);
        } finally {
            com.github.pagehelper.PageHelper.clearPage();
        }
    }

    @Test
    void cacheKeyMustNotEmbedHugeContent() throws Exception {
        String huge = "x".repeat(50_000);
        String fragment = (String) invokePrivate("keyFragment", new Class<?>[] { Object.class }, new Object[] { huge });

        assertTrue(fragment.length() <= 80, "长对象（如含正文的 BlogArticle.toString()）只应入哈希：" + fragment.length());
        assertTrue(fragment.startsWith("h"), fragment);

        String other = (String) invokePrivate("keyFragment", new Class<?>[] { Object.class }, new Object[] { huge + "y" });
        assertNotEquals(fragment, other, "不同参数必须得到不同的键片段");
        assertEquals("null", invokePrivate("keyFragment", new Class<?>[] { Object.class }, new Object[] { null }));
        assertEquals("short", invokePrivate("keyFragment", new Class<?>[] { Object.class }, new Object[] { "short" }));
    }

    private Object invokePrivate(String name, Class<?>[] types, Object[] args) throws Exception {
        java.lang.reflect.Method method = CacheAspect.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(cacheAspect, args);
    }

    @Test
    void buildCacheKeyMustWirePageInfoAndHashLongArgs() throws Exception {
        org.aspectj.lang.reflect.MethodSignature signature = mock(org.aspectj.lang.reflect.MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[] { "keyword", "blogArticle" });
        when(joinPoint.getSignature()).thenReturn(signature);
        String hugeFilter = "filter-" + "x".repeat(5_000);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "redis", hugeFilter });

        com.github.pagehelper.PageHelper.startPage(3, 20);
        String page3;
        try {
            page3 = (String) invokePrivate("buildCacheKey",
                new Class<?>[] { ProceedingJoinPoint.class, String.class },
                new Object[] { joinPoint, "zhi:search:#keyword:#blogArticle" });
        } finally {
            com.github.pagehelper.PageHelper.clearPage();
        }

        assertTrue(page3.contains("redis"), page3);
        assertTrue(page3.endsWith(":p3:s20"), "第 3 页必须与第 1 页不同键：" + page3);
        assertTrue(page3.length() < 200, "长参数只应入哈希，键不能膨胀：" + page3.length());

        String noPage = (String) invokePrivate("buildCacheKey",
            new Class<?>[] { ProceedingJoinPoint.class, String.class },
            new Object[] { joinPoint, "zhi:search:#keyword:#blogArticle" });
        assertFalse(noPage.contains(":p3:s20"), "无分页时不应带页码：" + noPage);
    }
}
