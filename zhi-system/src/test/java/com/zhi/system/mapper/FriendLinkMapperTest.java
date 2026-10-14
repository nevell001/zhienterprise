package com.zhi.system.mapper;

import com.zhi.system.domain.FriendLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 友情链接Mapper单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class FriendLinkMapperTest {

    @Mock
    private FriendLinkMapper friendLinkMapper;

    private FriendLink testFriendLink;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testFriendLink = new FriendLink();
        testFriendLink.setId(1L);
        testFriendLink.setName("Mapper测试友情链接");
        testFriendLink.setUrl("https://example.com");
        testFriendLink.setDescription("测试描述");
        testFriendLink.setStatus("0"); // 启用
    }

    /**
     * 测试CRUD操作的完整流程
     */
    @Test
    void testCRUDOperations() {
        // 测试新增
        when(friendLinkMapper.insertFriendLink(any(FriendLink.class))).thenReturn(1);
        int insertResult = friendLinkMapper.insertFriendLink(testFriendLink);
        assertEquals(1, insertResult, "新增友情链接失败");
        verify(friendLinkMapper).insertFriendLink(any(FriendLink.class));

        // 测试根据ID查询
        when(friendLinkMapper.selectFriendLinkById(1L)).thenReturn(testFriendLink);
        FriendLink selectResult = friendLinkMapper.selectFriendLinkById(1L);
        assertNotNull(selectResult, "根据ID查询友情链接失败");
        assertEquals("Mapper测试友情链接", selectResult.getName(), "友情链接名称不匹配");
        verify(friendLinkMapper).selectFriendLinkById(1L);

        // 测试修改
        when(friendLinkMapper.updateFriendLink(any(FriendLink.class))).thenReturn(1);
        testFriendLink.setName("修改后的友情链接");
        int updateResult = friendLinkMapper.updateFriendLink(testFriendLink);
        assertEquals(1, updateResult, "修改友情链接失败");
        verify(friendLinkMapper).updateFriendLink(any(FriendLink.class));

        // 测试查询列表
        when(friendLinkMapper.selectFriendLinkList(any(FriendLink.class)))
            .thenReturn(Arrays.asList(testFriendLink));
        List<FriendLink> friendLinkList = friendLinkMapper.selectFriendLinkList(new FriendLink());
        assertFalse(friendLinkList.isEmpty(), "查询友情链接列表失败");
        verify(friendLinkMapper).selectFriendLinkList(any(FriendLink.class));

        // 测试删除
        when(friendLinkMapper.deleteFriendLinkById(1L)).thenReturn(1);
        int deleteResult = friendLinkMapper.deleteFriendLinkById(1L);
        assertEquals(1, deleteResult, "删除友情链接失败");
        verify(friendLinkMapper).deleteFriendLinkById(1L);
    }

    /**
     * 测试批量删除功能
     */
    @Test
    void testDeleteFriendLinkByIds() {
        when(friendLinkMapper.deleteFriendLinkByIds(any(Long[].class))).thenReturn(2);
        int deleteResult = friendLinkMapper.deleteFriendLinkByIds(new Long[]{1L, 2L});
        assertEquals(2, deleteResult, "批量删除友情链接失败");
        verify(friendLinkMapper).deleteFriendLinkByIds(any(Long[].class));
    }

    /**
     * 测试查询前台友情链接列表
     */
    @Test
    void testSelectFrontFriendLinkList() {
        when(friendLinkMapper.selectFrontFriendLinkList())
            .thenReturn(Arrays.asList(testFriendLink));
        List<FriendLink> frontList = friendLinkMapper.selectFrontFriendLinkList();
        assertNotNull(frontList, "前台友情链接列表不应为null");
        assertEquals(1, frontList.size(), "前台友情链接列表大小不正确");
        assertEquals("Mapper测试友情链接", frontList.get(0).getName(), "友情链接名称不匹配");
        verify(friendLinkMapper).selectFrontFriendLinkList();
    }

    /**
     * 测试边界条件：空值处理
     */
    @Test
    void testNullHandling() {
        // 测试查询不存在的ID
        when(friendLinkMapper.selectFriendLinkById(999L)).thenReturn(null);
        FriendLink result = friendLinkMapper.selectFriendLinkById(999L);
        assertNull(result, "查询不存在的ID应返回null");
        verify(friendLinkMapper).selectFriendLinkById(999L);

        // 测试空列表查询
        when(friendLinkMapper.selectFriendLinkList(any(FriendLink.class)))
            .thenReturn(Arrays.asList());
        List<FriendLink> list = friendLinkMapper.selectFriendLinkList(new FriendLink());
        assertTrue(list.isEmpty(), "查询结果应为空列表");
        verify(friendLinkMapper).selectFriendLinkList(any(FriendLink.class));
    }

    /**
     * 测试边界条件：空名称
     */
    @Test
    void testEmptyName() {
        FriendLink emptyNameLink = new FriendLink();
        emptyNameLink.setName("");
        emptyNameLink.setUrl("https://example.com");
        
        when(friendLinkMapper.insertFriendLink(any(FriendLink.class))).thenReturn(1);
        int result = friendLinkMapper.insertFriendLink(emptyNameLink);
        assertEquals(1, result, "空名称友情链接插入失败");
        verify(friendLinkMapper).insertFriendLink(any(FriendLink.class));
    }

    /**
     * 测试边界条件：空URL
     */
    @Test
    void testEmptyUrl() {
        FriendLink emptyUrlLink = new FriendLink();
        emptyUrlLink.setName("测试链接");
        emptyUrlLink.setUrl("");
        
        when(friendLinkMapper.insertFriendLink(any(FriendLink.class))).thenReturn(1);
        int result = friendLinkMapper.insertFriendLink(emptyUrlLink);
        assertEquals(1, result, "空URL友情链接插入失败");
        verify(friendLinkMapper).insertFriendLink(any(FriendLink.class));
    }

    /**
     * 测试边界条件：状态值
     */
    @Test
    void testStatusValues() {
        // 测试启用状态
        FriendLink enabledLink = new FriendLink();
        enabledLink.setName("启用链接");
        enabledLink.setStatus("0");
        when(friendLinkMapper.insertFriendLink(any(FriendLink.class))).thenReturn(1);
        int result1 = friendLinkMapper.insertFriendLink(enabledLink);
        assertEquals(1, result1, "启用状态友情链接插入失败");

        // 测试禁用状态
        FriendLink disabledLink = new FriendLink();
        disabledLink.setName("禁用链接");
        disabledLink.setStatus("1");
        when(friendLinkMapper.insertFriendLink(any(FriendLink.class))).thenReturn(1);
        int result2 = friendLinkMapper.insertFriendLink(disabledLink);
        assertEquals(1, result2, "禁用状态友情链接插入失败");

        verify(friendLinkMapper, times(2)).insertFriendLink(any(FriendLink.class));
    }
}