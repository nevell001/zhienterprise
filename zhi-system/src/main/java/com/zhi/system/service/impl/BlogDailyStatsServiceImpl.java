package com.zhi.system.service.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.common.utils.DateUtils;
import com.zhi.common.core.redis.RedisCache;
import com.zhi.system.mapper.BlogDailyStatsMapper;
import com.zhi.system.service.IBlogDailyStatsService;

/**
 * 文章按日 PV/UV 统计 Service 实现
 *
 * @author nevell
 * @date 2026-09-05
 */
@Service
public class BlogDailyStatsServiceImpl implements IBlogDailyStatsService
{
    private static final Logger log = LoggerFactory.getLogger(BlogDailyStatsServiceImpl.class);

    private static final String PV_PREFIX = "zhi:article:pv:";

    private static final String UV_PREFIX = "zhi:article:uv:";

    private static final String UVIEW_PREFIX = "zhi:article:uview:";

    @Autowired
    private BlogDailyStatsMapper blogDailyStatsMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    public int upsertDailyFromRedis(String dateKey)
    {
        Map<Long, long[]> counters = new HashMap<>();
        collect(PV_PREFIX, dateKey, 0, counters);
        collect(UV_PREFIX, dateKey, 1, counters);

        if (counters.isEmpty())
        {
            return 0;
        }

        String statDate = toDateString(dateKey);
        int handled = 0;
        for (Map.Entry<Long, long[]> entry : counters.entrySet())
        {
            long pv = entry.getValue()[0];
            long uv = entry.getValue()[1];
            try
            {
                handled += blogDailyStatsMapper.upsertDailyStat(entry.getKey(), statDate, pv, uv);
            }
            catch (Exception e)
            {
                log.warn("写入每日统计失败: articleId={}, date={}, error={}", entry.getKey(), statDate, e.getMessage());
            }
        }
        return handled;
    }

    private void collect(String prefix, String dateKey, int slot, Map<Long, long[]> counters)
    {
        try
        {
            java.util.Collection<String> keys = redisCache.scanKeys(prefix + dateKey + ":*");
            for (String key : keys)
            {
                Long articleId = Long.valueOf(key.substring(key.lastIndexOf(':') + 1));
                Object value = redisCache.getCacheObject(key);
                long count = value instanceof Number ? ((Number) value).longValue() : 0L;
                long[] arr = counters.computeIfAbsent(articleId, k -> new long[2]);
                arr[slot] = count;
            }
        }
        catch (Exception e)
        {
            log.warn("读取 Redis 每日计数失败: prefix={}, date={}, error={}", prefix, dateKey, e.getMessage());
        }
    }

    @Override
    public int cleanupDailyRedisKeys(String dateKey)
    {
        int cleaned = 0;
        cleaned += deleteKeys(PV_PREFIX + dateKey + ":*");
        cleaned += deleteKeys(UV_PREFIX + dateKey + ":*");
        cleaned += deleteKeys(UVIEW_PREFIX + dateKey + ":*");
        return cleaned;
    }

    private int deleteKeys(String pattern)
    {
        int cleaned = 0;
        try
        {
            java.util.Collection<String> keys = redisCache.scanKeys(pattern);
            for (String key : keys)
            {
                if (redisCache.deleteObject(key))
                {
                    cleaned++;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("清理每日计数键失败: pattern={}, error={}", pattern, e.getMessage());
        }
        return cleaned;
    }

    @Override
    public List<Map<String, Object>> queryDailyAggregate(int days)
    {
        if (days <= 0 || days > 366)
        {
            days = 30;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -(days - 1));
        String startDate = DateUtils.parseDateToStr("yyyy-MM-dd", calendar.getTime());
        return blogDailyStatsMapper.selectDailyAggregate(startDate);
    }

    /**
     * yyyyMMdd -> yyyy-MM-dd
     */
    private String toDateString(String dateKey)
    {
        if (dateKey == null || dateKey.length() != 8)
        {
            return dateKey;
        }
        return dateKey.substring(0, 4) + "-" + dateKey.substring(4, 6) + "-" + dateKey.substring(6, 8);
    }
}
