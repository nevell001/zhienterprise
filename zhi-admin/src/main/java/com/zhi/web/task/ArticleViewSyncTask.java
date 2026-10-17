package com.zhi.web.task;

import com.zhi.common.core.redis.RedisCache;
import com.zhi.system.mapper.ArticleMapper;
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
    private ArticleMapper articleMapper;

    /**
     * 定时将 Redis 中的浏览量同步到数据库
     * 每 10 分钟执行一次
     *
     * 顺序约定（重要）：必须"先读增量 → 写库成功 → 再删键"，不能用 GETDEL 取出即删。
     * 原实现用 GETDEL（RedisCache.getAndDeleteCacheObject）在写库**之前**删键，一旦
     * addIncrementViewCount 抛异常（DB 抖动、连接池耗尽），增量只存在于局部变量且
     * catch 不回补 —— 该批浏览量**永久丢失**，view_count 永久偏小。
     *
     * 代价说明：写库成功但 deleteObject 失败时，下轮会重复累加同一批增量（偏大）。
     * 这是刻意的取舍：浏览量宁可偶发重复，也不能静默丢数。
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void syncArticleViewCount() {
        log.info("开始同步文章浏览量...");
        Collection<String> keys = redisCache.scanKeys("zhi:article:view:*");
        int synced = 0;
        int failed = 0;

        for (String key : keys) {
            try {
                String articleIdStr = key.substring(key.lastIndexOf(":") + 1);
                Long articleId = Long.parseLong(articleIdStr);

                // 1) 先读，不删 —— 读失败/为空都不影响键
                Integer increment = redisCache.getCacheObject(key);
                if (increment == null || increment <= 0) {
                    // 脏键（非正数）直接清掉，避免每轮空转
                    if (increment != null) {
                        redisCache.deleteObject(key);
                    }
                    continue;
                }

                // 2) 写库；抛异常则不删键，增量保留到下一轮重试
                articleMapper.addIncrementViewCount(articleId, increment.longValue());
                synced++;

                // 3) 写库成功后才删键
                redisCache.deleteObject(key);
                log.debug("已同步文章 {} 的浏览量增量: {}", articleId, increment);
            } catch (Exception e) {
                failed++;
                log.error("同步文章浏览量失败，增量保留待下轮重试，key: {}", key, e);
            }
        }

        log.info("文章浏览量同步完成。成功 {} 个，失败 {} 个。", synced, failed);
        if (failed > 0) {
            log.warn("本次有 {} 个浏览量键同步失败并已保留，将在下轮重试", failed);
        }
    }
}
