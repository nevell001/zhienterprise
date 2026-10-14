package com.zhi.system.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.zhi.common.core.redis.RedisCache;
import com.zhi.system.mapper.BlogDailyStatsMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BlogDailyStatsServiceImpl 单元测试
 *
 * @author nevell
 */
@ExtendWith(MockitoExtension.class)
class BlogDailyStatsServiceImplTest
{
    @Mock
    private BlogDailyStatsMapper blogDailyStatsMapper;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private BlogDailyStatsServiceImpl blogDailyStatsService;

    @Test
    void testUpsertMergesPvAndUvForSameArticle()
    {
        String pvKey = "zhi:article:pv:20260905:10";
        String uvKey = "zhi:article:uv:20260905:10";
        when(redisCache.scanKeys("zhi:article:pv:20260905:*")).thenReturn(Collections.singletonList(pvKey));
        when(redisCache.scanKeys("zhi:article:uv:20260905:*")).thenReturn(Collections.singletonList(uvKey));
        when(redisCache.getCacheObject(pvKey)).thenReturn(5L);
        when(redisCache.getCacheObject(uvKey)).thenReturn(2L);
        when(blogDailyStatsMapper.upsertDailyStat(10L, "2026-09-05", 5L, 2L)).thenReturn(1);

        int handled = blogDailyStatsService.upsertDailyFromRedis("20260905");

        assertEquals(1, handled);
        verify(blogDailyStatsMapper).upsertDailyStat(10L, "2026-09-05", 5L, 2L);
    }

    @Test
    void testUpsertEmptySkips()
    {
        when(redisCache.scanKeys(anyString())).thenReturn(Collections.emptyList());

        assertEquals(0, blogDailyStatsService.upsertDailyFromRedis("20260905"));
        verify(blogDailyStatsMapper, never()).upsertDailyStat(any(), anyString(), anyLong(), anyLong());
    }

    @Test
    void testCleanupDeletesAllDailyKeys()
    {
        String pvKey = "zhi:article:pv:20260904:10";
        String uvKey = "zhi:article:uv:20260904:10";
        String uviewKey = "zhi:article:uview:20260904:10:ip:1.2.3.4";
        when(redisCache.scanKeys("zhi:article:pv:20260904:*")).thenReturn(Collections.singletonList(pvKey));
        when(redisCache.scanKeys("zhi:article:uv:20260904:*")).thenReturn(Collections.singletonList(uvKey));
        when(redisCache.scanKeys("zhi:article:uview:20260904:*")).thenReturn(Collections.singletonList(uviewKey));
        when(redisCache.deleteObject(pvKey)).thenReturn(true);
        when(redisCache.deleteObject(uvKey)).thenReturn(true);
        when(redisCache.deleteObject(uviewKey)).thenReturn(true);

        int cleaned = blogDailyStatsService.cleanupDailyRedisKeys("20260904");
        assertEquals(3, cleaned);
    }

    @Test
    void testQueryDailyAggregate()
    {
        List<Map<String, Object>> rows = Arrays.asList(
                java.util.Collections.singletonMap("statDate", "2026-08-01"));
        when(blogDailyStatsMapper.selectDailyAggregate(anyString())).thenReturn(rows);

        List<Map<String, Object>> result = blogDailyStatsService.queryDailyAggregate(30);
        assertTrue(result.size() > 0);
        verify(blogDailyStatsMapper).selectDailyAggregate(anyString());
    }
}
