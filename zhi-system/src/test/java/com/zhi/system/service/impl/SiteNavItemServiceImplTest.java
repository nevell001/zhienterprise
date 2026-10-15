package com.zhi.system.service.impl;

import com.zhi.common.exception.ServiceException;
import com.zhi.system.domain.SiteNavItem;
import com.zhi.system.mapper.SiteNavItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.NonTransientDataAccessResourceException;

import java.sql.SQLSyntaxErrorException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 站点导航项服务层单元测试
 *
 * @author test
 */
@ExtendWith(MockitoExtension.class)
class SiteNavItemServiceImplTest {

    @Mock
    private SiteNavItemMapper siteNavItemMapper;

    @InjectMocks
    private SiteNavItemServiceImpl siteNavItemService;

    private SiteNavItem nav(Long id, Long parentId, String label) {
        SiteNavItem item = new SiteNavItem();
        item.setId(id);
        item.setParentId(parentId);
        item.setLabel(label);
        return item;
    }

    /** 平铺列表按 parent_id 组装为两级树 */
    @Test
    void testSelectVisibleNavTree_BuildsTwoLevelTree() {
        SiteNavItem home = nav(1L, 0L, "首页");
        SiteNavItem about = nav(4L, 0L, "关于我们");
        SiteNavItem intro = nav(41L, 4L, "企业简介");
        when(siteNavItemMapper.selectVisibleByLocation("header"))
            .thenReturn(Arrays.asList(home, about, intro));

        List<SiteNavItem> tree = siteNavItemService.selectVisibleNavTree("header");

        assertEquals(2, tree.size());
        assertEquals("首页", tree.get(0).getLabel());
        assertNull(tree.get(0).getChildren());
        assertEquals(1, tree.get(1).getChildren().size());
        assertEquals("企业简介", tree.get(1).getChildren().get(0).getLabel());
        verify(siteNavItemMapper).selectVisibleByLocation("header");
    }

    /** 空结果返回空树 */
    @Test
    void testSelectVisibleNavTree_EmptyResult() {
        when(siteNavItemMapper.selectVisibleByLocation("footer"))
            .thenReturn(Collections.emptyList());

        List<SiteNavItem> tree = siteNavItemService.selectVisibleNavTree("footer");

        assertNotNull(tree);
        assertTrue(tree.isEmpty());
    }

    /** site_nav_item 表缺失（老库未升级）时降级为空树，前台回退内置导航 */
    @Test
    void testSelectVisibleNavTree_MissingTableDegradesToEmpty() {
        SQLSyntaxErrorException syntax = new SQLSyntaxErrorException(
            "Table 'zhienterprise.site_nav_item' doesn't exist");
        when(siteNavItemMapper.selectVisibleByLocation("header"))
            .thenThrow(new NonTransientDataAccessResourceException("bad SQL grammar", syntax));

        List<SiteNavItem> tree = siteNavItemService.selectVisibleNavTree("header");

        assertNotNull(tree);
        assertTrue(tree.isEmpty());
    }

    /** 其余数据库故障必须如实抛出，不能被降级吞掉 */
    @Test
    void testSelectVisibleNavTree_OtherDataAccessErrorPropagates() {
        when(siteNavItemMapper.selectVisibleByLocation("header"))
            .thenThrow(new InvalidDataAccessApiUsageException("connection pool exhausted"));

        assertThrows(InvalidDataAccessApiUsageException.class,
            () -> siteNavItemService.selectVisibleNavTree("header"));
    }

    /** 更新时以自身为父项应被拒绝 */
    @Test
    void testUpdateSiteNavItem_SelfParentRejected() {
        SiteNavItem item = nav(7L, 7L, "循环导航");

        assertThrows(ServiceException.class, () -> siteNavItemService.updateSiteNavItem(item));
        verify(siteNavItemMapper, never()).updateSiteNavItem(any(SiteNavItem.class));
    }

    /** 更新时缺少 ID 应被拒绝 */
    @Test
    void testUpdateSiteNavItem_MissingIdRejected() {
        SiteNavItem item = nav(null, 0L, "无ID");

        assertThrows(ServiceException.class, () -> siteNavItemService.updateSiteNavItem(item));
        verify(siteNavItemMapper, never()).updateSiteNavItem(any(SiteNavItem.class));
    }

    /** 导入导航时清空旧数据并回填默认 sort/visible */
    @Test
    void testImportNav_DefaultSortAndVisible() {
        SiteNavItem item = new SiteNavItem();
        item.setLabel("核心业务");
        item.setLinkType("path");
        item.setLinkValue("/business");

        siteNavItemService.importNav("header", Collections.singletonList(item));

        verify(siteNavItemMapper).deleteByLocation("header");
        assertEquals("header", item.getLocation());
        assertEquals(1, item.getSort());
        assertEquals(1, item.getVisible());
        verify(siteNavItemMapper).insertSiteNavItem(item);
    }
}