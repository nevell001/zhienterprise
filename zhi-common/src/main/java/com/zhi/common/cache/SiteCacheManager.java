package com.zhi.common.cache;

import com.zhi.common.cache.CacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 博客专用缓存管理器
 * 提供博客系统常用的缓存操作方法
 * 
 * @author nevell
 * @since 2025-12-20
 */
@Slf4j
@Component
public class SiteCacheManager {

    private final UnifiedCacheManager unifiedCacheManager;

    @Autowired
    public SiteCacheManager(UnifiedCacheManager unifiedCacheManager) {
        this.unifiedCacheManager = unifiedCacheManager;
    }

    // ========== 文章缓存 ==========

    /**
     * 缓存文章
     */
    public void cacheArticle(Object article) {
        unifiedCacheManager.set(CacheConstants.ARTICLE + getArticleId(article), 
                                article, CacheConstants.ARTICLE_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的文章
     */
    public <T> T getCachedArticle(Long articleId, Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.ARTICLE + articleId, type);
    }

    /**
     * 缓存文章列表
     */
    public void cacheArticleList(String key, List<?> articleList) {
        unifiedCacheManager.set(CacheConstants.ARTICLE_LIST + key, 
                                articleList, CacheConstants.ARTICLE_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的文章列表
     */
    public <T> List<T> getCachedArticleList(String key, Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.ARTICLE_LIST + key, List.class);
    }

    /**
     * 缓存热门文章
     */
    public void cacheHotArticles(List<?> articles) {
        unifiedCacheManager.set(CacheConstants.ARTICLE_HOT, 
                                articles, CacheConstants.HOT_ARTICLE_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的热门文章
     */
    public <T> List<T> getCachedHotArticles(Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.ARTICLE_HOT, List.class);
    }

    // ========== 分类缓存 ==========

    /**
     * 缓存分类
     */
    public void cacheCategory(Object category) {
        unifiedCacheManager.set(CacheConstants.CATEGORY + getCategoryId(category), 
                                category, CacheConstants.CATEGORY_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的分类
     */
    public <T> T getCachedCategory(Long categoryId, Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.CATEGORY + categoryId, type);
    }

    /**
     * 缓存分类列表
     */
    public void cacheCategoryList(List<?> categories) {
        unifiedCacheManager.set(CacheConstants.CATEGORY_LIST + "all", 
                                categories, CacheConstants.CATEGORY_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的分类列表
     */
    public <T> List<T> getCachedCategoryList(Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.CATEGORY_LIST + "all", List.class);
    }

    // ========== 标签缓存 ==========

    /**
     * 缓存标签
     */
    public void cacheTag(Object tag) {
        unifiedCacheManager.set(CacheConstants.TAG + getTagId(tag), 
                                tag, CacheConstants.TAG_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的标签
     */
    public <T> T getCachedTag(Long tagId, Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.TAG + tagId, type);
    }

    /**
     * 缓存标签列表
     */
    public void cacheTagList(List<?> tags) {
        unifiedCacheManager.set(CacheConstants.TAG_LIST + "all", 
                                tags, CacheConstants.TAG_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的标签列表
     */
    public <T> List<T> getCachedTagList(Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.TAG_LIST + "all", List.class);
    }

    /**
     * 缓存标签云
     */
    public void cacheTagCloud(List<?> tagCloud) {
        unifiedCacheManager.set(CacheConstants.TAG + "cloud", 
                                tagCloud, CacheConstants.TAG_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的标签云
     */
    public <T> List<T> getCachedTagCloud(Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.TAG + "cloud", List.class);
    }

    // ========== 设置缓存 ==========

    /**
     * 缓存设置
     */
    public void cacheSetting(String key, Object value) {
        unifiedCacheManager.set(CacheConstants.SETTING + key, 
                                value, CacheConstants.SETTING_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的设置
     */
    public <T> T getCachedSetting(String key, Class<T> type) {
        return unifiedCacheManager.get(CacheConstants.SETTING + key, type);
    }

    // ========== 缓存删除 ==========

    /**
     * 删除指定文章的缓存
     */
    public void evictArticleCache(Long articleId) {
        unifiedCacheManager.delete(CacheConstants.ARTICLE + articleId);
    }

    /**
     * 删除所有文章相关缓存
     */
    public void evictAllArticleCache() {
        unifiedCacheManager.deleteByPattern(CacheConstants.ARTICLE + "*");
        unifiedCacheManager.deleteByPattern(CacheConstants.ARTICLE_LIST + "*");
        unifiedCacheManager.deleteByPattern(CacheConstants.ARTICLE_HOT);
    }

    /**
     * 删除指定分类的缓存
     */
    public void evictCategoryCache(Long categoryId) {
        unifiedCacheManager.delete(CacheConstants.CATEGORY + categoryId);
    }

    /**
     * 删除所有分类相关缓存
     */
    public void evictAllCategoryCache() {
        unifiedCacheManager.deleteByPattern(CacheConstants.CATEGORY + "*");
    }

    /**
     * 删除指定标签的缓存
     */
    public void evictTagCache(Long tagId) {
        unifiedCacheManager.delete(CacheConstants.TAG + tagId);
    }

    /**
     * 删除所有标签相关缓存
     */
    public void evictAllTagCache() {
        unifiedCacheManager.deleteByPattern(CacheConstants.TAG + "*");
    }

    /**
     * 删除指定设置的缓存
     */
    public void evictSettingCache(String key) {
        unifiedCacheManager.delete(CacheConstants.SETTING + key);
    }

    /**
     * 删除所有设置相关缓存
     */
    public void evictAllSettingCache() {
        unifiedCacheManager.deleteByPattern(CacheConstants.SETTING + "*");
    }

    /**
     * 清除所有博客相关缓存
     */
    public void clearAllCache() {
        Set<String> patterns = new HashSet<>(Arrays.asList(
            CacheConstants.ARTICLE + "*",
            CacheConstants.CATEGORY + "*",
            CacheConstants.TAG + "*",
            CacheConstants.COMMENT + "*",
            CacheConstants.SETTING + "*",
            CacheConstants.FRIEND_LINK + "*",
            CacheConstants.STATS + "*"
        ));
        
        for (String pattern : patterns) {
            unifiedCacheManager.deleteByPattern(pattern);
        }
        
        log.info("Cleared all site cache");
    }

    // ========== 通用缓存操作 ==========

    /**
     * 设置缓存
     */
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        unifiedCacheManager.set(key, value, ttl, timeUnit);
    }

    /**
     * 获取缓存
     */
    public <T> T get(String key, Class<T> type) {
        return unifiedCacheManager.get(key, type);
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        unifiedCacheManager.delete(key);
    }

    /**
     * 按模式删除缓存
     */
    public void deleteByPattern(String pattern) {
        unifiedCacheManager.deleteByPattern(pattern);
    }

    // ========== 工具方法 ==========

    /**
     * 从对象中获取文章ID
     */
    private Long getArticleId(Object article) {
        try {
            // 使用反射获取ID
            return (Long) article.getClass().getMethod("getId").invoke(article);
        } catch (Exception e) {
            log.error("Failed to get article ID from object", e);
            return null;
        }
    }

    /**
     * 从对象中获取分类ID
     */
    private Long getCategoryId(Object category) {
        try {
            return (Long) category.getClass().getMethod("getId").invoke(category);
        } catch (Exception e) {
            log.error("Failed to get category ID from object", e);
            return null;
        }
    }

    /**
     * 从对象中获取标签ID
     */
    private Long getTagId(Object tag) {
        try {
            return (Long) tag.getClass().getMethod("getId").invoke(tag);
        } catch (Exception e) {
            log.error("Failed to get tag ID from object", e);
            return null;
        }
    }
}