package com.zhi.web.controller.blog;

import com.zhi.system.domain.BlogPage;
import com.zhi.system.service.IBlogPageService;
import com.zhi.system.service.IBlogSettingService;
import com.zhi.system.service.IBlogVisitLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 自定义页面前台接口单元测试
 */
class BlogFrontPageControllerTest
{
    private IBlogPageService blogPageService;
    private IBlogVisitLogService blogVisitLogService;
    private IBlogSettingService blogSettingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        blogPageService = mock(IBlogPageService.class);
        blogVisitLogService = mock(IBlogVisitLogService.class);
        blogSettingService = mock(IBlogSettingService.class);

        BlogFrontPageController controller = new BlogFrontPageController();
        ReflectionTestUtils.setField(controller, "blogPageService", blogPageService);
        ReflectionTestUtils.setField(controller, "blogVisitLogService", blogVisitLogService);
        ReflectionTestUtils.setField(controller, "blogSettingService", blogSettingService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listShouldReturnPublishedPages() throws Exception
    {
        BlogPage page = new BlogPage();
        page.setId(1L);
        page.setTitle("关于本站");
        page.setSlug("about-site");
        page.setShowInNav("1");
        when(blogPageService.selectPublishedPageList()).thenReturn(Collections.singletonList(page));

        mockMvc.perform(get("/api/page/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].slug").value("about-site"))
            .andExpect(jsonPath("$.data[0].showInNav").value("1"));
    }

    @Test
    void getBySlugShouldReturnPageAndCountView() throws Exception
    {
        BlogPage page = new BlogPage();
        page.setId(5L);
        page.setTitle("关于本站");
        page.setSlug("about-site");
        page.setContent("# 关于");
        page.setViewCount(10L);
        when(blogPageService.selectBlogPageBySlug("about-site")).thenReturn(page);

        mockMvc.perform(get("/api/page/about-site"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").value("# 关于"))
            // 返回体中的浏览数应包含本次访问
            .andExpect(jsonPath("$.data.viewCount").value(11));

        verify(blogPageService).addViewCount(eq(5L), any());
        verify(blogVisitLogService).recordVisit(eq("page"), eq(5L), anyString(), any(), any());
    }

    @Test
    void getBySlugShouldHandleNullViewCount() throws Exception
    {
        BlogPage page = new BlogPage();
        page.setId(6L);
        page.setSlug("fresh-page");
        page.setViewCount(null);
        when(blogPageService.selectBlogPageBySlug("fresh-page")).thenReturn(page);

        mockMvc.perform(get("/api/page/fresh-page"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.viewCount").value(1));

        verify(blogPageService).addViewCount(eq(6L), any());
    }

    @Test
    void getBySlugShouldSkipCountWhenViewCountDisabled() throws Exception
    {
        BlogPage page = new BlogPage();
        page.setId(7L);
        page.setSlug("quiet-page");
        page.setViewCount(20L);
        when(blogPageService.selectBlogPageBySlug("quiet-page")).thenReturn(page);
        when(blogSettingService.selectSettingValueByKey("view_count_enabled")).thenReturn("false");

        mockMvc.perform(get("/api/page/quiet-page"))
            .andExpect(status().isOk())
            // 开关关闭时返回体浏览数保持不变
            .andExpect(jsonPath("$.data.viewCount").value(20));

        verify(blogPageService, never()).addViewCount(anyLong(), any());
        verify(blogVisitLogService, never()).recordVisit(anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void getBySlugShouldReturnErrorWhenMissing() throws Exception
    {
        when(blogPageService.selectBlogPageBySlug("missing")).thenReturn(null);

        mockMvc.perform(get("/api/page/missing"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("页面不存在或未发布"));

        verify(blogPageService, never()).addViewCount(anyLong(), any());
    }
}
