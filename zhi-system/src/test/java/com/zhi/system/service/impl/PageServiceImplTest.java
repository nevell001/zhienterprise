package com.zhi.system.service.impl;

import com.zhi.common.core.redis.RedisCache;
import com.zhi.common.exception.ServiceException;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.domain.Page;
import com.zhi.system.mapper.PageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 自定义页面服务层单元测试
 *
 * @author test
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
class PageServiceImplTest {

    @Mock
    private PageMapper pageMapper;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private PageServiceImpl pageService;

    private Page testPage;

    @BeforeEach
    void setUp() {
        testPage = new Page();
        testPage.setId(1L);
        testPage.setTitle("关于本站");
        testPage.setSlug("about-site");
        testPage.setContent("# 关于");
        testPage.setStatus("1");
    }

    @Test
    void testSelectPageList() {
        when(pageMapper.selectPageList(any(Page.class))).thenReturn(Arrays.asList(testPage));

        List<Page> result = pageService.selectPageList(new Page());

        assertEquals(1, result.size());
        assertEquals("about-site", result.get(0).getSlug());
    }

    @Test
    void testSelectPublishedPageList() {
        when(pageMapper.selectPublishedPageList()).thenReturn(Collections.singletonList(testPage));

        assertEquals(1, pageService.selectPublishedPageList().size());
    }

    @Test
    void testSelectPageById() {
        when(pageMapper.selectPageById(1L)).thenReturn(testPage);

        assertEquals("关于本站", pageService.selectPageById(1L).getTitle());
    }

    @Test
    void testSelectPageBySlug() {
        when(pageMapper.selectPageBySlug("about-site")).thenReturn(testPage);

        Page result = pageService.selectPageBySlug(" about-site ");

        assertNotNull(result);
        // 入参会做 trim
        verify(pageMapper).selectPageBySlug("about-site");
    }

    @Test
    void testSelectPageBySlugWithEmptySlugReturnsNull() {
        assertNull(pageService.selectPageBySlug("  "));
        verify(pageMapper, never()).selectPageBySlug(anyString());
    }

    @Test
    void testInsertPageAppliesDefaults() {
        Page page = new Page();
        page.setTitle("新页面");
        page.setSlug("new-page");

        when(pageMapper.checkSlugUnique("new-page")).thenReturn(null);
        when(pageMapper.insertPage(any(Page.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            int result = pageService.insertPage(page);

            assertEquals(1, result);
            assertEquals("0", page.getStatus());
            assertEquals("1", page.getShowInNav());
            assertEquals(0, page.getSort());
            assertEquals(0L, page.getViewCount());
            assertEquals("0", page.getDelFlag());
            assertEquals("admin", page.getCreateBy());
            assertNotNull(page.getCreateTime());
        }
    }

    @Test
    void testInsertPageRejectsEmptyTitle() {
        Page page = new Page();
        page.setSlug("slug-only");

        ServiceException exception = assertThrows(ServiceException.class, () -> pageService.insertPage(page));

        assertEquals("页面标题不能为空", exception.getMessage());
    }

    @Test
    void testInsertPageRejectsTooLongTitle() {
        Page page = new Page();
        page.setTitle("a".repeat(101));
        page.setSlug("slug-only");

        ServiceException exception = assertThrows(ServiceException.class, () -> pageService.insertPage(page));

        assertEquals("页面标题长度不能超过100个字符", exception.getMessage());
    }

    @Test
    void testInsertPageRejectsEmptySlug() {
        Page page = new Page();
        page.setTitle("标题");

        ServiceException exception = assertThrows(ServiceException.class, () -> pageService.insertPage(page));

        assertEquals("页面别名不能为空", exception.getMessage());
    }

    @Test
    void testInsertPageRejectsIllegalSlug() {
        Page page = new Page();
        page.setTitle("标题");
        page.setSlug("非法 slug!");

        ServiceException exception = assertThrows(ServiceException.class, () -> pageService.insertPage(page));

        assertTrue(exception.getMessage().contains("页面别名只能包含字母"));
    }

    @Test
    void testInsertPageRejectsDuplicateSlug() {
        Page exist = new Page();
        exist.setId(9L);
        exist.setSlug("about-site");

        Page page = new Page();
        page.setTitle("标题");
        page.setSlug("about-site");

        when(pageMapper.checkSlugUnique("about-site")).thenReturn(exist);

        ServiceException exception = assertThrows(ServiceException.class, () -> pageService.insertPage(page));

        assertEquals("页面别名已存在：about-site", exception.getMessage());
        verify(pageMapper, never()).insertPage(any(Page.class));
    }

    @Test
    void testUpdatePageRequiresId() {
        Page page = new Page();
        page.setTitle("标题");

        ServiceException exception = assertThrows(ServiceException.class, () -> pageService.updatePage(page));

        assertEquals("页面ID不能为空", exception.getMessage());
    }

    @Test
    void testUpdatePageWithoutSlugSkipsUniqueCheck() {
        Page page = new Page();
        page.setId(1L);
        page.setTitle("只改标题");

        when(pageMapper.updatePage(any(Page.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            assertEquals(1, pageService.updatePage(page));
            verify(pageMapper, never()).checkSlugUnique(anyString());
            assertNotNull(page.getUpdateTime());
        }
    }

    @Test
    void testUpdatePageAllowsSameSlugOnItself() {
        Page page = new Page();
        page.setId(1L);
        page.setSlug("about-site");

        Page exist = new Page();
        exist.setId(1L);

        when(pageMapper.checkSlugUnique("about-site")).thenReturn(exist);
        when(pageMapper.updatePage(any(Page.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            assertEquals(1, pageService.updatePage(page));
        }
    }

    @Test
    void testUpdatePageRejectsSlugTakenByOther() {
        Page page = new Page();
        page.setId(1L);
        page.setSlug("taken");

        Page exist = new Page();
        exist.setId(2L);

        when(pageMapper.checkSlugUnique("taken")).thenReturn(exist);

        ServiceException exception = assertThrows(ServiceException.class, () -> pageService.updatePage(page));

        assertEquals("页面别名已存在：taken", exception.getMessage());
    }

    @Test
    void testChangePageStatus() {
        when(pageMapper.updatePage(any(Page.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

            assertEquals(1, pageService.changePageStatus(1L, "0"));
            verify(pageMapper).updatePage(argThat(p -> "0".equals(p.getStatus())));
        }
    }

    @Test
    void testChangePageStatusRejectsIllegalStatus() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> pageService.changePageStatus(1L, "5"));

        assertEquals("页面状态不合法", exception.getMessage());
    }

    @Test
    void testIncreaseViewCount() {
        when(pageMapper.increaseViewCount(1L)).thenReturn(1);

        assertEquals(1, pageService.increaseViewCount(1L));
    }

    @Test
    void testIncreaseViewCountWithNullId() {
        assertEquals(0, pageService.increaseViewCount(null));
        verify(pageMapper, never()).increaseViewCount(anyLong());
    }

    @Test
    void testAddViewCountNullId() {
        assertEquals(0, pageService.addViewCount(null, "u1"));
        verify(pageMapper, never()).increaseViewCount(anyLong());
    }

    @Test
    void testAddViewCountWithoutViewerKeyIncrements() {
        when(pageMapper.increaseViewCount(1L)).thenReturn(1);

        // 无访客标识时不去重，直接累加
        assertEquals(1, pageService.addViewCount(1L, null));
        verify(pageMapper).increaseViewCount(1L);
    }

    @Test
    void testAddViewCountFirstVisitIncrements() {
        when(redisCache.hasKey(any())).thenReturn(false);
        when(pageMapper.increaseViewCount(1L)).thenReturn(1);

        assertEquals(1, pageService.addViewCount(1L, "u1"));
        verify(pageMapper).increaseViewCount(1L);
    }

    @Test
    void testAddViewCountDedupedWithin24h() {
        when(redisCache.hasKey(any())).thenReturn(true);

        // 24 小时内同一访客再次访问不累加
        assertEquals(0, pageService.addViewCount(1L, "u1"));
        verify(pageMapper, never()).increaseViewCount(anyLong());
    }

    @Test
    void testDeletePageById() {
        when(pageMapper.deletePageById(1L)).thenReturn(1);

        assertEquals(1, pageService.deletePageById(1L));
    }

    @Test
    void testDeletePageByIds() {
        Long[] ids = {1L, 2L};
        when(pageMapper.deletePageByIds(ids)).thenReturn(2);

        assertEquals(2, pageService.deletePageByIds(ids));
    }
}
