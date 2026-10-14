package com.zhi.web.controller.site;

import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.service.ILikeService;
import com.zhi.system.service.ISettingService;
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
class LikeControllerTest
{
    private ILikeService likeService;
    private ISettingService settingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        likeService = mock(ILikeService.class);
        settingService = mock(ISettingService.class);

        LikeController controller = new LikeController();
        ReflectionTestUtils.setField(controller, "likeService", likeService);
        ReflectionTestUtils.setField(controller, "settingService", settingService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void toggleArticleLikeShouldBeRejectedWhenLikeDisabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("like_enabled")).thenReturn("false");

        mockMvc.perform(post("/api/like/article/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("点赞功能已关闭"));

        verify(likeService, never()).toggleArticleLike(anyLong(), anyLong());
    }

    @Test
    void toggleCommentLikeShouldBeRejectedWhenLikeDisabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("like_enabled")).thenReturn("0");

        mockMvc.perform(post("/api/like/comment/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("点赞功能已关闭"));

        verify(likeService, never()).toggleCommentLike(anyLong(), anyLong());
    }

    @Test
    void toggleArticleLikeShouldPassThroughWhenLikeEnabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("like_enabled")).thenReturn("true");
        when(likeService.toggleArticleLike(7L, 1L)).thenReturn(java.util.Map.of("liked", true));

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUserId).thenReturn(7L);

            mockMvc.perform(post("/api/like/article/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(likeService).toggleArticleLike(7L, 1L);
        }
    }
}
