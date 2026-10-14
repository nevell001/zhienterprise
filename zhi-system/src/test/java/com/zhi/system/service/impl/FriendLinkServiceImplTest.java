package com.zhi.system.service.impl;

import com.zhi.system.domain.FriendLink;
import com.zhi.system.mapper.FriendLinkMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zhi.common.utils.SecurityUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 友情链接服务层单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class FriendLinkServiceImplTest {

    @Mock
    private FriendLinkMapper friendLinkMapper;

    @InjectMocks
    private FriendLinkServiceImpl friendLinkService;

    private FriendLink testFriendLink;

    @BeforeEach
    void setUp() {
        testFriendLink = new FriendLink();
        testFriendLink.setId(1L);
        testFriendLink.setName("测试友情链接");
        testFriendLink.setUrl("https://example.com");
        testFriendLink.setDescription("测试描述");
        testFriendLink.setStatus("0"); // 启用
    }

    /**
     * 测试查询友情链接列表
     */
    @Test
    void testSelectFriendLinkList() {
        // 准备数据
        List<FriendLink> linkList = Arrays.asList(testFriendLink);
        when(friendLinkMapper.selectFriendLinkList(any(FriendLink.class)))
            .thenReturn(linkList);

        // 执行测试
        List<FriendLink> result = friendLinkService.selectFriendLinkList(new FriendLink());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试友情链接", result.get(0).getName());
        verify(friendLinkMapper).selectFriendLinkList(any(FriendLink.class));
    }

    /**
     * 测试查询友情链接列表 - 空结果
     */
    @Test
    void testSelectFriendLinkList_Empty() {
        // 模拟空结果
        when(friendLinkMapper.selectFriendLinkList(any(FriendLink.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<FriendLink> result = friendLinkService.selectFriendLinkList(new FriendLink());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(friendLinkMapper).selectFriendLinkList(any(FriendLink.class));
    }

    /**
     * 测试根据ID查询友情链接
     */
    @Test
    void testSelectFriendLinkById() {
        // 准备数据
        when(friendLinkMapper.selectFriendLinkById(1L)).thenReturn(testFriendLink);

        // 执行测试
        FriendLink result = friendLinkService.selectFriendLinkById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("测试友情链接", result.getName());
        verify(friendLinkMapper).selectFriendLinkById(1L);
    }

    /**
     * 测试根据ID查询友情链接 - 不存在
     */
    @Test
    void testSelectFriendLinkById_NotFound() {
        // 模拟友情链接不存在
        when(friendLinkMapper.selectFriendLinkById(999L)).thenReturn(null);

        // 执行测试
        FriendLink result = friendLinkService.selectFriendLinkById(999L);

        // 验证结果
        assertNull(result);
        verify(friendLinkMapper).selectFriendLinkById(999L);
    }

    /**
     * 测试新增友情链接 - 成功
     */
    @Test
    void testInsertFriendLink_Success() {
        // 准备数据
        when(friendLinkMapper.insertFriendLink(any(FriendLink.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = friendLinkService.insertFriendLink(testFriendLink);

            // 验证结果
            assertEquals(1, result);
            verify(friendLinkMapper).insertFriendLink(any(FriendLink.class));
        }
    }

    /**
     * 测试修改友情链接 - 成功
     */
    @Test
    void testUpdateFriendLink_Success() {
        // 准备数据
        when(friendLinkMapper.updateFriendLink(any(FriendLink.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = friendLinkService.updateFriendLink(testFriendLink);

            // 验证结果
            assertEquals(1, result);
            verify(friendLinkMapper).updateFriendLink(any(FriendLink.class));
        }
    }

    /**
     * 测试删除友情链接 - 成功
     */
    @Test
    void testDeleteFriendLinkById_Success() {
        // 准备数据
        when(friendLinkMapper.deleteFriendLinkById(1L)).thenReturn(1);

        // 执行测试
        int result = friendLinkService.deleteFriendLinkById(1L);

        // 验证结果
        assertEquals(1, result);
        verify(friendLinkMapper).deleteFriendLinkById(1L);
    }

    /**
     * 测试删除友情链接 - 不存在
     */
    @Test
    void testDeleteFriendLinkById_NotFound() {
        // 准备数据
        when(friendLinkMapper.deleteFriendLinkById(999L)).thenReturn(0);

        // 执行测试
        int result = friendLinkService.deleteFriendLinkById(999L);

        // 验证结果
        assertEquals(0, result);
        verify(friendLinkMapper).deleteFriendLinkById(999L);
    }

    /**
     * 测试批量删除友情链接 - 成功
     */
    @Test
    void testDeleteFriendLinkByIds_Success() {
        // 准备数据
        when(friendLinkMapper.deleteFriendLinkByIds(any(Long[].class))).thenReturn(2);

        // 执行测试
        int result = friendLinkService.deleteFriendLinkByIds(new Long[]{1L, 2L});

        // 验证结果
        assertEquals(2, result);
        verify(friendLinkMapper).deleteFriendLinkByIds(any(Long[].class));
    }

    /**
     * 测试批量删除友情链接 - 空列表
     */
    @Test
    void testDeleteFriendLinkByIds_EmptyList() {
        // 执行测试
        int result = friendLinkService.deleteFriendLinkByIds(new Long[]{});

        // 验证结果
        assertEquals(0, result);
        verify(friendLinkMapper).deleteFriendLinkByIds(any(Long[].class));
    }

    /**
     * 测试查询前台友情链接列表
     */
    @Test
    void testSelectFrontFriendLinkList() {
        // 准备数据
        List<FriendLink> linkList = Arrays.asList(testFriendLink);
        when(friendLinkMapper.selectFrontFriendLinkList()).thenReturn(linkList);

        // 执行测试
        List<FriendLink> result = friendLinkService.selectFrontFriendLinkList();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(friendLinkMapper).selectFrontFriendLinkList();
    }

    /**
     * 测试查询前台友情链接列表 - 空结果
     */
    @Test
    void testSelectFrontFriendLinkList_Empty() {
        // 模拟空结果
        when(friendLinkMapper.selectFrontFriendLinkList()).thenReturn(Collections.emptyList());

        // 执行测试
        List<FriendLink> result = friendLinkService.selectFrontFriendLinkList();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(friendLinkMapper).selectFrontFriendLinkList();
    }

    /**
     * 测试友情链接状态更新
     */
    @Test
    void testUpdateFriendLink_Status() {
        // 更新状态
        testFriendLink.setStatus("1");
        when(friendLinkMapper.updateFriendLink(any(FriendLink.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = friendLinkService.updateFriendLink(testFriendLink);

            // 验证结果
            assertEquals(1, result);
            verify(friendLinkMapper).updateFriendLink(any(FriendLink.class));
        }
    }

    /**
     * 测试友情链接URL格式
     */
    @Test
    void testInsertFriendLink_DifferentUrls() {
        // 测试不同格式的URL
        String[] urls = {
            "https://example.com",
            "http://example.com",
            "https://www.example.com",
            "https://example.com/path"
        };

        // 准备数据
        when(friendLinkMapper.insertFriendLink(any(FriendLink.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            for (String url : urls) {
                testFriendLink.setUrl(url);
                int result = friendLinkService.insertFriendLink(testFriendLink);
                assertEquals(1, result);
            }
        }
    }
}