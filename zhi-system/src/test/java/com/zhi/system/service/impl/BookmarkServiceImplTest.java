package com.zhi.system.service.impl;

import com.zhi.system.domain.Bookmark;
import com.zhi.system.mapper.BookmarkMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文章收藏服务层单元测试
 */
@ExtendWith(MockitoExtension.class)
class BookmarkServiceImplTest {

    @Mock
    private BookmarkMapper bookmarkMapper;

    @InjectMocks
    private BookmarkServiceImpl bookmarkService;

    @Test
    void testSelectBookmarkById() {
        Bookmark bookmark = new Bookmark();
        bookmark.setId(1L);
        when(bookmarkMapper.selectBookmarkById(1L)).thenReturn(bookmark);

        assertSame(bookmark, bookmarkService.selectBookmarkById(1L));
        verify(bookmarkMapper).selectBookmarkById(1L);
    }

    @Test
    void testSelectBookmarkList() {
        Bookmark query = new Bookmark();
        List<Bookmark> bookmarks = Collections.singletonList(new Bookmark());
        when(bookmarkMapper.selectBookmarkList(query)).thenReturn(bookmarks);

        assertSame(bookmarks, bookmarkService.selectBookmarkList(query));
        verify(bookmarkMapper).selectBookmarkList(query);
    }

    @Test
    void testInsertAndDeleteDelegateToMapper() {
        Bookmark bookmark = new Bookmark();
        when(bookmarkMapper.insertBookmark(bookmark)).thenReturn(1);
        when(bookmarkMapper.deleteBookmarkById(2L)).thenReturn(1);
        when(bookmarkMapper.deleteBookmarkByUserIdAndArticleId(3L, 4L)).thenReturn(1);

        assertEquals(1, bookmarkService.insertBookmark(bookmark));
        assertEquals(1, bookmarkService.deleteBookmarkById(2L));
        assertEquals(1, bookmarkService.deleteByUserIdAndArticleId(3L, 4L));
    }

    @Test
    void testToggleBookmarkCancelsExistingBookmark() {
        when(bookmarkMapper.existsBookmark(10L, 20L)).thenReturn(1);
        when(bookmarkMapper.deleteBookmarkByUserIdAndArticleId(10L, 20L)).thenReturn(1);

        assertEquals(0, bookmarkService.toggleBookmark(10L, 20L));
        verify(bookmarkMapper).deleteBookmarkByUserIdAndArticleId(10L, 20L);
        verify(bookmarkMapper, never()).insertBookmark(any(Bookmark.class));
    }

    @Test
    void testToggleBookmarkCreatesMissingBookmark() {
        when(bookmarkMapper.existsBookmark(10L, 20L)).thenReturn(0);
        when(bookmarkMapper.insertBookmark(any(Bookmark.class))).thenReturn(1);

        assertEquals(1, bookmarkService.toggleBookmark(10L, 20L));
        verify(bookmarkMapper).insertBookmark(any(Bookmark.class));
        verify(bookmarkMapper, never()).deleteBookmarkByUserIdAndArticleId(10L, 20L);
    }

    @Test
    void testIsBookmarked() {
        when(bookmarkMapper.existsBookmark(10L, 20L)).thenReturn(1);
        when(bookmarkMapper.existsBookmark(10L, 21L)).thenReturn(0);

        assertTrue(bookmarkService.isBookmarked(10L, 20L));
        assertFalse(bookmarkService.isBookmarked(10L, 21L));
    }

    @Test
    void testSelectBookmarksByUserId() {
        List<Bookmark> bookmarks = Collections.singletonList(new Bookmark());
        when(bookmarkMapper.selectBookmarksByUserId(10L)).thenReturn(bookmarks);

        assertSame(bookmarks, bookmarkService.selectBookmarksByUserId(10L));
        verify(bookmarkMapper).selectBookmarksByUserId(10L);
    }
}
