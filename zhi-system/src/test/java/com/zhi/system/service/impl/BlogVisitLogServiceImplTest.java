package com.zhi.system.service.impl;

import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.system.domain.BlogVisitLog;
import com.zhi.system.mapper.BlogVisitLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 访问明细服务层单元测试
 *
 * @author test
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
class BlogVisitLogServiceImplTest {

    @Mock
    private BlogVisitLogMapper blogVisitLogMapper;

    @Mock
    private UnifiedCacheManager unifiedCacheManager;

    @InjectMocks
    private BlogVisitLogServiceImpl blogVisitLogService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "JUnit-Agent");
        request.addHeader("Referer", "https://example.com/list");
        request.setRemoteAddr("10.0.0.8");
    }

    @Test
    void testRecordVisitMarksUniqueWhenRedisKeyAbsent() {
        when(unifiedCacheManager.exists(anyString())).thenReturn(false);
        when(blogVisitLogMapper.insertBlogVisitLog(any(BlogVisitLog.class))).thenReturn(1);

        blogVisitLogService.recordVisit("article", 9L, "/article/9", "ip:1.2.3.4", request);

        ArgumentCaptor<BlogVisitLog> captor = ArgumentCaptor.forClass(BlogVisitLog.class);
        verify(blogVisitLogMapper).insertBlogVisitLog(captor.capture());
        BlogVisitLog saved = captor.getValue();
        assertEquals("article", saved.getTargetType());
        assertEquals(9L, saved.getTargetId());
        assertEquals("ip:1.2.3.4", saved.getVisitorKey());
        assertEquals("1", saved.getIsUnique());
        assertEquals("JUnit-Agent", saved.getUserAgent());
        assertEquals("https://example.com/list", saved.getReferer());
        assertNotNull(saved.getVisitDate());
        // 首次访问需要占位去重键
        verify(unifiedCacheManager).set(anyString(), eq("1"), eq(48L), any());
    }

    @Test
    void testRecordVisitMarksNotUniqueWhenRedisKeyExists() {
        when(unifiedCacheManager.exists(anyString())).thenReturn(true);
        when(blogVisitLogMapper.insertBlogVisitLog(any(BlogVisitLog.class))).thenReturn(1);

        blogVisitLogService.recordVisit("article", 9L, null, "ip:1.2.3.4", request);

        ArgumentCaptor<BlogVisitLog> captor = ArgumentCaptor.forClass(BlogVisitLog.class);
        verify(blogVisitLogMapper).insertBlogVisitLog(captor.capture());
        assertEquals("0", captor.getValue().getIsUnique());
        verify(unifiedCacheManager, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void testRecordVisitFallsBackToRequestIp() {
        when(unifiedCacheManager.exists(anyString())).thenReturn(false);
        when(blogVisitLogMapper.insertBlogVisitLog(any(BlogVisitLog.class))).thenReturn(1);

        blogVisitLogService.recordVisit("page", 3L, "/page/about", null, request);

        ArgumentCaptor<BlogVisitLog> captor = ArgumentCaptor.forClass(BlogVisitLog.class);
        verify(blogVisitLogMapper).insertBlogVisitLog(captor.capture());
        assertEquals("ip:10.0.0.8", captor.getValue().getVisitorKey());
        assertEquals("10.0.0.8", captor.getValue().getIp());
    }

    @Test
    void testRecordVisitSkipsWhenNoTarget() {
        blogVisitLogService.recordVisit(null, 9L, "/x", "ip:1.2.3.4", request);
        blogVisitLogService.recordVisit("article", null, "/x", "ip:1.2.3.4", request);

        verify(blogVisitLogMapper, never()).insertBlogVisitLog(any(BlogVisitLog.class));
    }

    @Test
    void testRecordVisitSkipsWhenNoVisitorIdentity() {
        // 既没有登录身份、也拿不到请求（因而拿不到 IP）时无法判定独立访客，应跳过记录
        blogVisitLogService.recordVisit("article", 9L, "/x", null, null);

        verify(blogVisitLogMapper, never()).insertBlogVisitLog(any(BlogVisitLog.class));
    }

    @Test
    void testRecordVisitTruncatesLongHeaders() {
        when(unifiedCacheManager.exists(anyString())).thenReturn(false);
        when(blogVisitLogMapper.insertBlogVisitLog(any(BlogVisitLog.class))).thenReturn(1);
        MockHttpServletRequest longRequest = new MockHttpServletRequest();
        longRequest.addHeader("User-Agent", "a".repeat(300));
        longRequest.addHeader("Referer", "b".repeat(600));
        longRequest.setRemoteAddr("10.0.0.9");

        blogVisitLogService.recordVisit("article", 9L, "/x", "ip:10.0.0.9", longRequest);

        ArgumentCaptor<BlogVisitLog> captor = ArgumentCaptor.forClass(BlogVisitLog.class);
        verify(blogVisitLogMapper).insertBlogVisitLog(captor.capture());
        assertEquals(255, captor.getValue().getUserAgent().length());
        assertEquals(500, captor.getValue().getReferer().length());
    }

    @Test
    void testRecordVisitSwallowsMapperFailure() {
        when(unifiedCacheManager.exists(anyString())).thenReturn(false);
        when(blogVisitLogMapper.insertBlogVisitLog(any(BlogVisitLog.class)))
            .thenThrow(new RuntimeException("db down"));

        // 不应抛出异常，避免影响正常访问
        assertDoesNotThrow(() ->
            blogVisitLogService.recordVisit("article", 9L, "/x", "ip:1.2.3.4", request));
    }

    @Test
    void testSelectBlogVisitLogList() {
        BlogVisitLog log = new BlogVisitLog();
        log.setId(1L);
        when(blogVisitLogMapper.selectBlogVisitLogList(any(BlogVisitLog.class)))
            .thenReturn(Arrays.asList(log));

        List<BlogVisitLog> result = blogVisitLogService.selectBlogVisitLogList(new BlogVisitLog());

        assertEquals(1, result.size());
    }

    @Test
    void testSelectVisitSummaryBuildsRangeAndTopTargets() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("pv", 120L);
        summary.put("uv", 45L);
        when(blogVisitLogMapper.selectVisitSummary(anyMap())).thenReturn(summary);
        when(blogVisitLogMapper.selectTopTargets(anyMap())).thenReturn(Arrays.asList(new HashMap<>()));

        Map<String, Object> result = blogVisitLogService.selectVisitSummary(7);

        assertEquals(120L, result.get("pv"));
        assertEquals(45L, result.get("uv"));
        assertEquals(7, result.get("days"));
        assertNotNull(result.get("beginDate"));
        assertNotNull(result.get("endDate"));
        assertNotNull(result.get("todayPv"));
        assertEquals(1, ((List<?>) result.get("topTargets")).size());
    }

    @Test
    void testSelectVisitSummaryHandlesMissingRow() {
        when(blogVisitLogMapper.selectVisitSummary(anyMap())).thenReturn(null);
        when(blogVisitLogMapper.selectTopTargets(anyMap())).thenReturn(null);

        Map<String, Object> result = blogVisitLogService.selectVisitSummary(null);

        assertEquals(30, result.get("days"));
        assertEquals(0L, result.get("pv"));
        assertEquals(0L, result.get("uv"));
        assertTrue(((List<?>) result.get("topTargets")).isEmpty());
    }

    @Test
    void testSelectVisitSummaryConvertsNullUvToZero() {
        java.util.Map<String, Object> summary = new HashMap<>();
        summary.put("pv", 5L);
        summary.put("uv", null); // 空区间时 SQL sum() 返回 NULL
        when(blogVisitLogMapper.selectVisitSummary(anyMap())).thenReturn(summary);
        when(blogVisitLogMapper.selectTopTargets(anyMap())).thenReturn(java.util.Collections.emptyList());

        Map<String, Object> result = blogVisitLogService.selectVisitSummary(30);

        assertEquals(5L, result.get("pv"));
        assertEquals(0L, result.get("uv"));
        assertEquals(0L, result.get("todayUv"));
    }

    @Test
    void testDeleteBlogVisitLogByIds() {
        Long[] ids = {1L, 2L};
        when(blogVisitLogMapper.deleteBlogVisitLogByIds(ids)).thenReturn(2);

        assertEquals(2, blogVisitLogService.deleteBlogVisitLogByIds(ids));
    }

    @Test
    void testDeleteBlogVisitLogByIdsWithEmpty() {
        assertEquals(0, blogVisitLogService.deleteBlogVisitLogByIds(new Long[0]));
        assertEquals(0, blogVisitLogService.deleteBlogVisitLogByIds(null));
        verify(blogVisitLogMapper, never()).deleteBlogVisitLogByIds(any(Long[].class));
    }

    @Test
    void testCleanVisitLogsUsesDefaultRetention() {
        when(blogVisitLogMapper.deleteBlogVisitLogBefore(anyString())).thenReturn(5);

        assertEquals(5, blogVisitLogService.cleanVisitLogs(null));
        verify(blogVisitLogMapper).deleteBlogVisitLogBefore(anyString());
    }

    @Test
    void testCleanVisitLogsClampsIllegalRetention() {
        when(blogVisitLogMapper.deleteBlogVisitLogBefore(anyString())).thenReturn(3);

        // 传入 0/负数时不能清空全部，按默认保留期处理
        assertEquals(3, blogVisitLogService.cleanVisitLogs(0));
        assertEquals(3, blogVisitLogService.cleanVisitLogs(-5));
        verify(blogVisitLogMapper, times(2)).deleteBlogVisitLogBefore(anyString());
    }
}
