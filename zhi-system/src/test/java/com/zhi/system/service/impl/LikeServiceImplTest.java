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
import com.zhi.system.mapper.LikeMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LikeServiceImpl 单元测试
 *
 * @author nevell
 */
@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest
{
    @Mock
    private LikeMapper likeMapper;

    @InjectMocks
    private LikeServiceImpl likeService;

    private static final Long USER_ID = 1L;

    private static final Long ARTICLE_ID = 100L;

    private static final Long COMMENT_ID = 200L;

    @Test
    void testToggleArticleLikeAddsWhenNotLiked()
    {
        when(likeMapper.selectArticleLikeCount(ARTICLE_ID)).thenReturn(5L, 6L);
        when(likeMapper.existsArticleLike(ARTICLE_ID, USER_ID)).thenReturn(0);
        when(likeMapper.insertArticleLike(ARTICLE_ID, USER_ID)).thenReturn(1);

        Map<String, Object> result = likeService.toggleArticleLike(USER_ID, ARTICLE_ID);

        assertEquals(Boolean.TRUE, result.get("liked"));
        assertEquals(6L, result.get("likeCount"));
        verify(likeMapper).insertArticleLike(ARTICLE_ID, USER_ID);
        verify(likeMapper).changeArticleLikeCount(ARTICLE_ID, 1);
        verify(likeMapper, never()).deleteArticleLike(anyLong(), anyLong());
    }

    @Test
    void testToggleArticleLikeRemovesWhenAlreadyLiked()
    {
        when(likeMapper.selectArticleLikeCount(ARTICLE_ID)).thenReturn(5L, 4L);
        when(likeMapper.existsArticleLike(ARTICLE_ID, USER_ID)).thenReturn(1);
        when(likeMapper.deleteArticleLike(ARTICLE_ID, USER_ID)).thenReturn(1);

        Map<String, Object> result = likeService.toggleArticleLike(USER_ID, ARTICLE_ID);

        assertEquals(Boolean.FALSE, result.get("liked"));
        assertEquals(4L, result.get("likeCount"));
        verify(likeMapper).deleteArticleLike(ARTICLE_ID, USER_ID);
        verify(likeMapper).changeArticleLikeCount(ARTICLE_ID, -1);
        verify(likeMapper, never()).insertArticleLike(anyLong(), anyLong());
    }

    @Test
    void testToggleArticleLikeSkipsWhenArticleMissing()
    {
        when(likeMapper.selectArticleLikeCount(999L)).thenReturn(null);

        Map<String, Object> result = likeService.toggleArticleLike(USER_ID, 999L);

        assertEquals(Boolean.FALSE, result.get("liked"));
        assertEquals(0L, result.get("likeCount"));
        verify(likeMapper, never()).insertArticleLike(anyLong(), anyLong());
        verify(likeMapper, never()).deleteArticleLike(anyLong(), anyLong());
    }

    @Test
    void testToggleArticleLikeHandlesNullArgs()
    {
        Map<String, Object> result = likeService.toggleArticleLike(null, ARTICLE_ID);
        assertEquals(Boolean.FALSE, result.get("liked"));
        assertEquals(0L, result.get("likeCount"));

        Map<String, Object> result2 = likeService.toggleArticleLike(USER_ID, null);
        assertEquals(Boolean.FALSE, result2.get("liked"));
        assertEquals(0L, result2.get("likeCount"));
    }

    @Test
    void testGetArticleLikeStatus()
    {
        when(likeMapper.selectArticleLikeCount(ARTICLE_ID)).thenReturn(5L);
        when(likeMapper.existsArticleLike(ARTICLE_ID, USER_ID)).thenReturn(1);

        Map<String, Object> liked = likeService.getArticleLikeStatus(USER_ID, ARTICLE_ID);
        assertTrue((Boolean) liked.get("liked"));
        assertEquals(5L, liked.get("likeCount"));

        when(likeMapper.existsArticleLike(ARTICLE_ID, USER_ID)).thenReturn(0);

        Map<String, Object> unliked = likeService.getArticleLikeStatus(USER_ID, ARTICLE_ID);
        assertFalse((Boolean) unliked.get("liked"));
        assertEquals(5L, unliked.get("likeCount"));
    }

    @Test
    void testToggleCommentLikeAddsWhenNotLiked()
    {
        when(likeMapper.selectCommentLikeCount(COMMENT_ID)).thenReturn(3L, 4L);
        when(likeMapper.existsCommentLike(COMMENT_ID, USER_ID)).thenReturn(0);
        when(likeMapper.insertCommentLike(COMMENT_ID, USER_ID)).thenReturn(1);

        Map<String, Object> result = likeService.toggleCommentLike(USER_ID, COMMENT_ID);

        assertEquals(Boolean.TRUE, result.get("liked"));
        assertEquals(4L, result.get("likeCount"));
        verify(likeMapper).insertCommentLike(COMMENT_ID, USER_ID);
        verify(likeMapper).changeCommentLikeCount(COMMENT_ID, 1);
    }

    @Test
    void testToggleCommentLikeRemovesWhenAlreadyLiked()
    {
        when(likeMapper.selectCommentLikeCount(COMMENT_ID)).thenReturn(3L, 2L);
        when(likeMapper.existsCommentLike(COMMENT_ID, USER_ID)).thenReturn(1);
        when(likeMapper.deleteCommentLike(COMMENT_ID, USER_ID)).thenReturn(1);

        Map<String, Object> result = likeService.toggleCommentLike(USER_ID, COMMENT_ID);

        assertEquals(Boolean.FALSE, result.get("liked"));
        assertEquals(2L, result.get("likeCount"));
        verify(likeMapper).deleteCommentLike(COMMENT_ID, USER_ID);
        verify(likeMapper).changeCommentLikeCount(COMMENT_ID, -1);
    }

    @Test
    void testToggleCommentLikeSkipsWhenCommentMissing()
    {
        when(likeMapper.selectCommentLikeCount(999L)).thenReturn(null);

        Map<String, Object> result = likeService.toggleCommentLike(USER_ID, 999L);

        assertEquals(Boolean.FALSE, result.get("liked"));
        assertEquals(0L, result.get("likeCount"));
        verify(likeMapper, never()).insertCommentLike(anyLong(), anyLong());
    }

    @Test
    void testGetLikedCommentIds()
    {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(likeMapper.selectLikedCommentIds(USER_ID, ids)).thenReturn(Arrays.asList(2L));

        List<Long> liked = likeService.getLikedCommentIds(USER_ID, ids);
        assertEquals(Collections.singletonList(2L), liked);

        // 空参数直接返回空列表，不查询
        List<Long> empty = likeService.getLikedCommentIds(USER_ID, Collections.emptyList());
        assertTrue(empty.isEmpty());
        List<Long> nullUser = likeService.getLikedCommentIds(null, ids);
        assertTrue(nullUser.isEmpty());
        verify(likeMapper, times(1)).selectLikedCommentIds(any(), any());
    }
}
