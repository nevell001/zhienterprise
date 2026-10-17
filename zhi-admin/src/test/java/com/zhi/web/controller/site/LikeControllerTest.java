package com.zhi.web.controller.site;

import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.service.ILikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

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
 * 点赞前台接口单元测试。
 *
 * <p>点赞开关（like_enabled）已随博客功能移除：企业官网定位下去掉了点赞/分享的
 * 设置项与前台按钮，服务端不再做开关拦截。本测试锁定剩余契约：</p>
 * <ul>
 *   <li>已登录用户可直接点赞（文章 / 评论）；</li>
 *   <li>未登录返回「请先登录」，且不得调用业务层。</li>
 * </ul>
 */
class LikeControllerTest
{
    private ILikeService likeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        likeService = mock(ILikeService.class);

        LikeController controller = new LikeController();
        ReflectionTestUtils.setField(controller, "likeService", likeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void toggleArticleLikeShouldPassThroughWhenLoggedIn() throws Exception
    {
        when(likeService.toggleArticleLike(7L, 1L)).thenReturn(Map.of("liked", true));

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUserId).thenReturn(7L);

            mockMvc.perform(post("/api/like/article/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(likeService).toggleArticleLike(7L, 1L);
        }
    }

    @Test
    void toggleCommentLikeShouldPassThroughWhenLoggedIn() throws Exception
    {
        when(likeService.toggleCommentLike(7L, 2L)).thenReturn(Map.of("liked", true));

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUserId).thenReturn(7L);

            mockMvc.perform(post("/api/like/comment/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(likeService).toggleCommentLike(7L, 2L);
        }
    }

    @Test
    void toggleArticleLikeShouldRequireLogin() throws Exception
    {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class))
        {
            mocked.when(SecurityUtils::getUserId).thenReturn(null);

            mockMvc.perform(post("/api/like/article/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("请先登录"));

            verify(likeService, never()).toggleArticleLike(anyLong(), anyLong());
        }
    }
}
