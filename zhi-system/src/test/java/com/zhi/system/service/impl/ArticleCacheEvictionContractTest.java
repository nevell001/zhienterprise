package com.zhi.system.service.impl;

import com.zhi.common.cache.annotation.SiteCacheEvict;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缓存失效契约测试。
 *
 * 背景（真实缺陷）：{@code searchArticles} 带 {@code @SiteCacheable(key = "zhi:search:...", ttl = 10min)}，
 * 但 {@code insertArticle} 曾**不带任何失效注解** —— 新增文章后前台搜索最长 10 分钟返回旧结果
 * （"新发布内容搜不到"）。当时只有 update/delete/置顶/推荐 等路径恰好带 {@code keyPattern="zhi:*"}
 * 顺带清掉搜索缓存，属"靠巧合成立"。
 *
 * 本测试锁定：写文章的方法必须带能覆盖 {@code zhi:search:*} / {@code zhi:hot:*} 等
 * {@code @SiteCacheable} 前缀的失效注解。
 *
 * 注意语义：{@code keyPattern} 走 deleteByPattern（Redis SCAN MATCH，支持通配符），
 * 而 {@code value()} 是**字面**删除、不支持通配符 —— 所以带 {@code *} 的 value 条目是无效的，
 * 本测试据此只认 keyPattern / allEntries。
 */
class ArticleCacheEvictionContractTest
{
    /** 这些是 @SiteCacheable 实际写入的键前缀，写路径必须能清掉它们 */
    private static final String[] CACHEABLE_PREFIXES = {
        "zhi:search:", "zhi:hot:", "zhi:article", "zhi:article:list"
    };

    /** 写文章的方法：新增/修改/删除/状态变更都会影响前台列表与搜索结果 */
    private static final String[] WRITE_METHODS = {
        "insertArticle", "updateArticle", "deleteArticleByIds", "deleteArticleById",
        "restoreArticleByIds", "purgeArticleByIds",
        "updateArticleStatus", "updateArticleTopStatus", "updateArticleRecommendStatus",
        "publishScheduledArticles"
    };

    @Test
    void writeMethodsMustEvictCachesCoveringEveryCacheablePrefix()
    {
        List<String> problems = new ArrayList<>();
        for (String name : WRITE_METHODS)
        {
            Method m = findMethod(name);
            assertTrue(m != null, "未找到方法 " + name + "，方法可能已改名");
            SiteCacheEvict evict = m.getAnnotation(SiteCacheEvict.class);
            if (evict == null)
            {
                problems.add(name + " 缺少 @SiteCacheEvict（写路径必须清前台缓存）");
                continue;
            }
            if (evict.allEntries())
            {
                continue; // 清全部，覆盖所有前缀
            }
            String pattern = evict.keyPattern();
            if (pattern.isEmpty())
            {
                problems.add(name + " 的 @SiteCacheEvict 未设置 keyPattern（value() 不支持通配符）");
                continue;
            }
            for (String prefix : CACHEABLE_PREFIXES)
            {
                // keyPattern 形如 "zhi:*"，须能匹配该前缀
                String head = prefix.replaceAll(":$", "");
                if (!matches(pattern, head))
                {
                    problems.add(name + " 的 keyPattern=\"" + pattern + "\" 覆盖不到 " + prefix);
                }
            }
        }
        assertTrue(problems.isEmpty(), "缓存失效覆盖不足：\n  " + String.join("\n  ", problems));
    }

    /**
     * 回归守卫：{@code insertArticle} 必须带失效注解 —— 这是本次修复的具体缺陷。
     */
    @Test
    void insertArticleMustEvictSearchCache()
    {
        Method m = findMethod("insertArticle");
        assertTrue(m != null, "未找到 insertArticle");
        SiteCacheEvict evict = m.getAnnotation(SiteCacheEvict.class);
        assertFalse(evict == null, "insertArticle 必须带 @SiteCacheEvict，否则新增文章后搜索/热门列表过期");
        assertTrue(evict.allEntries() || !evict.keyPattern().isEmpty(),
            "insertArticle 的失效注解必须真正生效（allEntries=true 或非空 keyPattern）");
    }

    /**
     * 语义守卫：value() 是字面删除，带通配符的条目会静默失效 —— 禁止这种写法。
     */
    @Test
    void evictValueMustNotContainWildcards()
    {
        List<String> problems = new ArrayList<>();
        for (Method m : ArticleServiceImpl.class.getDeclaredMethods())
        {
            SiteCacheEvict evict = m.getAnnotation(SiteCacheEvict.class);
            if (evict == null)
            {
                continue;
            }
            for (String v : evict.value())
            {
                if (v.contains("*"))
                {
                    problems.add(m.getName() + " 的 value() 含通配符 \"" + v
                        + "\"（value 为字面删除，通配符不会生效，应改用 keyPattern）");
                }
            }
        }
        assertTrue(problems.isEmpty(), "无效的失效写法：\n  " + String.join("\n  ", problems));
    }

    /** 极简 glob 匹配：仅需支持 `zhi:*` 形式，且 `*` 不跨越冒号边界之外的判断已足够 */
    private static boolean matches(String pattern, String keyHead)
    {
        String regex = "^" + pattern.replace(".", "\\.").replace("*", ".*") + "$";
        // pattern=zhi:* 时，regex=^zhi:.*$，keyHead=zhi:search 应命中
        return keyHead.matches(regex) || (keyHead + ":x").matches(regex);
    }

    private static Method findMethod(String name)
    {
        return Arrays.stream(ArticleServiceImpl.class.getDeclaredMethods())
            .filter(m -> m.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}