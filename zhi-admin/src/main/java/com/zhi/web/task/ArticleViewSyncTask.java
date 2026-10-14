package com.zhi.web.task;

import com.zhi.common.core.redis.RedisCache;
import com.zhi.system.mapper.BlogArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * 文章浏览量定时同步任务
 */
@Component("articleViewSyncTask")
public class ArticleViewSyncTask {
    private static final Logger log = LoggerFactory.getLogger(ArticleViewSyncTask.class);

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    /**
     * 定时将 Redis 中的浏览量同步到数据库
     * 每 10 分钟执行一次
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void syncArticleViewCount() {
        log.info("开始同步文章浏览量...");
        Collection<String> keys = redisCache.scanKeys("zhi:article:view:*");
        
        for (String key : keys) {
            try {
                String articleIdStr = key.substring(key.lastIndexOf(":") + 1);
                Long articleId = Long.parseLong(articleIdStr);
                Integer increment = redisCache.getAndDeleteCacheObject(key);
                
                if (increment != null && increment > 0) {
                    blogArticleMapper.addIncrementViewCount(articleId, increment.longValue());
                    log.debug("已同步文章 {} 的浏览量增量: {}", articleId, increment);
                }
            } catch (Exception e) {
                log.error("同步文章浏览量失败，key: {}", key, e);
            }
        }
        log.info("文章浏览量同步完成。");
    }
}
