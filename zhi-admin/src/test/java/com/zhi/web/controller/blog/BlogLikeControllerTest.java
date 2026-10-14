package com.zhi.web.controller.blog;

import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.service.IBlogLikeService;
import com.zhi.system.service.IBlogSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 点赞前台接口单元测试
 * 覆盖 like_enabled 开关的服务端拦截
 */
class BlogLikeControllerTest
{
    private IBlogLikeService blogLikeService;
    private IBlogSettingService blogSettingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        blogLikeService = mock(IBlogLikeService.class);
        blogSettingService = mock(IBlogSettingService.class);

        BlogLikeController controller = new BlogLikeController();
        ReflectionTestUtils.setField(controller, "blogLikeService", blogLikeService);
        ReflectionTestUtils.setField(controller, "blogSettingService", blogSettingService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void toggleArticleLikeShouldBeRejectedWhenLikeDisabled() throws Exception
    {
        when(blogSettingService.selectSettingValueByKey("like_enabled")).thenReturn("false");

        mockMvc.perform(post("/api/like/article/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("点赞功能已关闭"));

        verify(blogLikeService, never()).toggleArticleLike(anyLong(), anyLong());
    }

    @Test
    void toggleCommentLikeShouldBeRejectedWhenLikeDisabled() throws Exception
    {
        when(blogSettingService.selectSettingValueByKey("like_enabled")).thenReturn("0");

        mockMvc.perform(post("/api/like/comment/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("点赞功能已关闭"));

        verify(blogLikeService, never()).toggleCommentLike(anyLong(), anyLong());
    }

    @Test
    void toggleArticleLikeShouldPassThroughWhenLikeEnabled() throws Exception
    {
        when(blogSettingService.selectSettingValueByKey("like_enabled")).thenReturn("true");
        when(blogLikeService.toggleArticleLike(7L, 1L)).thenReturn(java.util.Map.of("liked", true));

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUserId).thenReturn(7L);

            mockMvc.perform(post("/api/like/article/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(blogLikeService).toggleArticleLike(7L, 1L);
        }
    }
}
