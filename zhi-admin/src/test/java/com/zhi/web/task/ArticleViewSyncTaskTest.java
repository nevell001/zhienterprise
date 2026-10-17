package com.zhi.web.task;

import com.zhi.common.core.redis.RedisCache;
import com.zhi.system.mapper.ArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 浏览量同步任务的"不丢增量"守卫。
 *
 * 背景（真实缺陷）：原实现用 GETDEL（先删键）再写库，写库抛异常时增量只存在于局部变量、
 * catch 不回补 → 该批浏览量永久丢失，view_count 永久偏小。修复后必须"写库成功才删键"。
 */
@ExtendWith(MockitoExtension.class)
class ArticleViewSyncTaskTest
{
    @Mock
    private RedisCache redisCache;

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private ArticleViewSyncTask task;

    private static final String KEY = "zhi:article:view:42";

    @BeforeEach
    void setUp()
    {
        when(redisCache.scanKeys("zhi:article:view:*")).thenReturn(List.of(KEY));
    }

    @Test
    void writesIncrementThenDeletesKeyOnSuccess()
    {
        when(redisCache.getCacheObject(KEY)).thenReturn(5);

        task.syncArticleViewCount();

        verify(articleMapper).addIncrementViewCount(eq(42L), eq(5L));
        verify(redisCache).deleteObject(KEY);
    }

    /**
     * 核心回归：写库失败时**不得删键** —— 增量必须留到下一轮重试，否则永久丢失。
     */
    @Test
    void keepsKeyWhenDatabaseWriteFails()
    {
        when(redisCache.getCacheObject(KEY)).thenReturn(7);
        doThrow(new RuntimeException("DB 抖动")).when(articleMapper)
                .addIncrementViewCount(anyLong(), anyLong());

        task.syncArticleViewCount();

        verify(articleMapper).addIncrementViewCount(eq(42L), eq(7L));
        verify(redisCache, never()).deleteObject(KEY);
    }

    /**
     * 不得使用"取出即删"（GETDEL）—— 一旦出现则回归到丢失增量的老实现。
     */
    @Test
    void mustNotUseGetAndDelete()
    {
        when(redisCache.getCacheObject(KEY)).thenReturn(3);

        task.syncArticleViewCount();

        verify(redisCache, never()).getAndDeleteCacheObject(KEY);
    }

    @Test
    void skipsNullAndNonPositiveIncrements()
    {
        when(redisCache.getCacheObject(KEY)).thenReturn(null);
        task.syncArticleViewCount();
        verify(articleMapper, never()).addIncrementViewCount(anyLong(), anyLong());

        when(redisCache.getCacheObject(KEY)).thenReturn(0);
        task.syncArticleViewCount();
        verify(articleMapper, never()).addIncrementViewCount(anyLong(), anyLong());
        // 非正数的脏键应被清掉，避免每轮空转
        verify(redisCache).deleteObject(KEY);
    }
}