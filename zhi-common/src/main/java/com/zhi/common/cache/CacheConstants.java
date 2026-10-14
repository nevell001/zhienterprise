package com.zhi.common.cache;

/**
 * 缓存常量定义
 * 
 * @author nevell
 */
public class CacheConstants
{
    // 缓存前缀
    public static final String BLOG_PREFIX = "zhi:";
    
    // 站点文章相关缓存
    public static final String BLOG_ARTICLE = BLOG_PREFIX + "article:";
    public static final String BLOG_ARTICLE_LIST = BLOG_PREFIX + "article:list:";
    public static final String BLOG_ARTICLE_HOT = BLOG_PREFIX + "article:hot";
    public static final String BLOG_ARTICLE_LATEST = BLOG_PREFIX + "article:latest";
    public static final String BLOG_ARTICLE_BY_CATEGORY = BLOG_PREFIX + "article:category:";
    public static final String BLOG_ARTICLE_BY_TAG = BLOG_PREFIX + "article:tag:";
    public static final String BLOG_ARTICLE_BY_AUTHOR = BLOG_PREFIX + "article:author:";
    
    // 站点分类相关缓存
    public static final String BLOG_CATEGORY = BLOG_PREFIX + "category:";
    public static final String BLOG_CATEGORY_LIST = BLOG_PREFIX + "category:list:";
    public static final String BLOG_CATEGORY_TREE = BLOG_PREFIX + "category:tree";
    
    // 站点标签相关缓存
    public static final String BLOG_TAG = BLOG_PREFIX + "tag:";
    public static final String BLOG_TAG_LIST = BLOG_PREFIX + "tag:list:";
    public static final String BLOG_TAG_POPULAR = BLOG_PREFIX + "tag:popular";
    
    // 站点评论相关缓存
    public static final String BLOG_COMMENT = BLOG_PREFIX + "comment:";
    public static final String BLOG_COMMENT_LIST = BLOG_PREFIX + "comment:list:";
    public static final String BLOG_COMMENT_BY_ARTICLE = BLOG_PREFIX + "comment:article:";
    
    // 站点设置相关缓存
    public static final String BLOG_SETTING = BLOG_PREFIX + "setting:";
    public static final String BLOG_SETTING_LIST = BLOG_PREFIX + "setting:list:";
    public static final String BLOG_SETTING_VALUE = BLOG_PREFIX + "setting:value:";
    
    // 站点友情链接相关缓存
    public static final String BLOG_FRIEND_LINK = BLOG_PREFIX + "friend_link:";
    public static final String BLOG_FRIEND_LINK_LIST = BLOG_PREFIX + "friend_link:list:";
    
    // 统计相关缓存
    public static final String BLOG_STATS = BLOG_PREFIX + "stats:";
    public static final String BLOG_STATS_DAILY = BLOG_PREFIX + "stats:daily:";
    public static final String BLOG_STATS_TOTAL = BLOG_PREFIX + "stats:total";
    
    // 系统相关缓存
    public static final String SYS_USER = "sys:user:";
    public static final String SYS_ROLE = "sys:role:";
    public static final String SYS_MENU = "sys:menu:";
    public static final String SYS_DICT = "sys:dict:";
    public static final String SYS_CONFIG = "sys:config:";
    
    // 缓存过期时间（秒）
    public static final long DEFAULT_EXPIRE = 1800;          // 30分钟
    public static final long LONG_EXPIRE = 3600;             // 1小时
    public static final long VERY_LONG_EXPIRE = 86400;       // 24小时
    public static final long SHORT_EXPIRE = 300;            // 5分钟
    
    // 特定缓存过期时间
    public static final long ARTICLE_EXPIRE = 3600;           // 文章缓存1小时
    public static final long HOT_ARTICLE_EXPIRE = 7200;        // 热门文章缓存2小时
    public static final long SETTING_EXPIRE = 86400;         // 设置缓存24小时
    public static final long CATEGORY_EXPIRE = 7200;           // 分类缓存2小时
    public static final long TAG_EXPIRE = 7200;               // 标签缓存2小时
    public static final long COMMENT_EXPIRE = 1800;            // 评论缓存30分钟
    public static final long STATS_EXPIRE = 600;               // 统计缓存10分钟
}