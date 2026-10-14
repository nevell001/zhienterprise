package com.zhi.framework.interceptor.impl;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.zhi.common.annotation.RepeatSubmit;
import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.constant.CacheConstants;
import com.zhi.common.filter.RepeatedlyRequestWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 同 URL 同参数防重复提交单元测试。
 *
 * <p>判定依据是「请求体/参数指纹 + 间隔时间」：命中重复要拒绝且**不能**续期缓存窗口
 * （否则连续重复提交会被无限延长），文章 JSON 接口则整体绕过（读请求体会干扰 Jackson 解析）。</p>
 */
class SameUrlDataInterceptorTest
{
    /** 只用于提供注解元数据 */
    static class Handler
    {
        @RepeatSubmit(interval = 10000, message = "请勿重复提交")
        public void submit()
        {
        }
    }

    private static RepeatSubmit annotation() throws NoSuchMethodException
    {
        Method method = Handler.class.getDeclaredMethod("submit");
        return method.getAnnotation(RepeatSubmit.class);
    }

    private final SameUrlDataInterceptor interceptor = new SameUrlDataInterceptor();

    private final UnifiedCacheManager cacheManager = Mockito.mock(UnifiedCacheManager.class);

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(interceptor, "header", "Authorization");
        ReflectionTestUtils.setField(interceptor, "unifiedCacheManager", cacheManager);
    }

    private void preloadCache(String cacheKey, String url, String params, long repeatTime)
    {
        Map<String, Object> previous = new HashMap<>();
        previous.put(interceptor.REPEAT_PARAMS, params);
        previous.put(interceptor.REPEAT_TIME, repeatTime);
        Map<String, Object> session = new HashMap<>();
        session.put(url, previous);
        Mockito.when(cacheManager.get(cacheKey, Object.class)).thenReturn(session);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureWrittenEntry() throws Exception
    {
        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(cacheManager).set(Mockito.anyString(), value.capture(), Mockito.anyLong(), Mockito.any());
        Map<String, Object> session = (Map<String, Object>) value.getValue();
        return (Map<String, Object>) session.values().iterator().next();
    }

    @Test
    void firstSubmitIsCachedWithTheAnnotationInterval() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.addParameter("userName", "zhangsan");

        boolean repeat = interceptor.isRepeatSubmit(request, annotation());

        assertThat(repeat).isFalse();
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(cacheManager).set(key.capture(), value.capture(), Mockito.eq(10000L), Mockito.eq(TimeUnit.MILLISECONDS));
        assertThat(key.getValue()).isEqualTo(CacheConstants.REPEAT_SUBMIT_KEY + "/system/user");
        assertThat(value.getValue().toString()).contains("system/user").contains("userName");
    }

    @Test
    void identicalSubmitInsideTheWindowIsRejectedWithoutExtendingTheWindow() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.addParameter("userName", "zhangsan");
        String fingerprint = com.alibaba.fastjson2.JSON.toJSONString(request.getParameterMap());
        preloadCache(CacheConstants.REPEAT_SUBMIT_KEY + "/system/user", "/system/user", fingerprint,
                System.currentTimeMillis());

        assertThat(interceptor.isRepeatSubmit(request, annotation())).isTrue();
        verify(cacheManager, never()).set(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    void identicalSubmitAfterTheWindowPassesAndRefreshesTheCache() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.addParameter("userName", "zhangsan");
        String fingerprint = com.alibaba.fastjson2.JSON.toJSONString(request.getParameterMap());
        preloadCache(CacheConstants.REPEAT_SUBMIT_KEY + "/system/user", "/system/user", fingerprint,
                System.currentTimeMillis() - 30000L);

        assertThat(interceptor.isRepeatSubmit(request, annotation())).isFalse();
        verify(cacheManager).set(Mockito.anyString(), Mockito.any(), Mockito.eq(10000L),
                Mockito.eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void differentPayloadOnTheSameUrlIsNotADuplicate() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.addParameter("userName", "lisi");
        preloadCache(CacheConstants.REPEAT_SUBMIT_KEY + "/system/user", "/system/user", "{\"userName\":[\"zhangsan\"]}",
                System.currentTimeMillis());

        assertThat(interceptor.isRepeatSubmit(request, annotation())).isFalse();
    }

    @Test
    void cachedEntryForAnotherUrlDoesNotBlockTheSubmit() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.addParameter("userName", "zhangsan");
        String fingerprint = com.alibaba.fastjson2.JSON.toJSONString(request.getParameterMap());
        preloadCache(CacheConstants.REPEAT_SUBMIT_KEY + "/system/user", "/system/role", fingerprint,
                System.currentTimeMillis());

        assertThat(interceptor.isRepeatSubmit(request, annotation())).isFalse();
    }

    @Test
    void articleJsonRequestsBypassTheCheckWithoutTouchingTheCache() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/article/article");
        request.setContentType("application/json");
        request.setContent("{\"title\":\"标题\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(interceptor.isRepeatSubmit(request, annotation())).isFalse();
        verify(cacheManager, never()).get(Mockito.anyString(), Mockito.any());
        verify(cacheManager, never()).set(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    void articleJsonRequestsAlsoBypass() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/article");
        request.setContentType("application/json");

        assertThat(interceptor.isRepeatSubmit(request, annotation())).isFalse();
        verify(cacheManager, never()).get(Mockito.anyString(), Mockito.any());
    }

    @Test
    void jsonRequestsToUnrelatedUrlsAreStillChecked() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.setContentType("application/json;charset=UTF-8");
        request.setContent("{\"userName\":\"zhangsan\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(interceptor.isRepeatSubmit(request, annotation())).isFalse();
        verify(cacheManager).get(CacheConstants.REPEAT_SUBMIT_KEY + "/system/user", Object.class);
    }

    @Test
    void wrappedRequestBodyBecomesTheFingerprint() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/message");
        request.setContentType("application/json");
        request.setContent("{\"content\":\"第一条留言\"}".getBytes(StandardCharsets.UTF_8));
        RepeatedlyRequestWrapper wrapper = new RepeatedlyRequestWrapper(request, new MockHttpServletResponse());

        assertThat(interceptor.isRepeatSubmit(wrapper, annotation())).isFalse();
        assertThat(captureWrittenEntry().get(interceptor.REPEAT_PARAMS).toString()).contains("第一条留言");
    }

    @Test
    void tokenHeaderKeepsDifferentVisitorsApartOnTheSameUrl() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.addHeader("Authorization", "Bearer token-a");

        assertThat(interceptor.isRepeatSubmit(request, annotation())).isFalse();
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(cacheManager).set(key.capture(), Mockito.any(), Mockito.anyLong(), Mockito.any());
        assertThat(key.getValue()).isEqualTo(CacheConstants.REPEAT_SUBMIT_KEY + "/system/userBearer token-a");
    }
}