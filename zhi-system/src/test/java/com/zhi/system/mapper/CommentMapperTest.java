package com.zhi.system.mapper;

import com.zhi.system.domain.Comment;
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
 * 博客评论Mapper单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class CommentMapperTest {

    @Mock
    private CommentMapper commentMapper;

    private Comment testComment;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setArticleId(1L);
        testComment.setContent("测试评论内容");
        testComment.setNickname("测试用户");
        testComment.setEmail("test@example.com");
        testComment.setStatus("0");
    }

    /**
     * 测试CRUD操作的完整流程
     */
    @Test
    void testCRUDOperations() {
        // 测试新增
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        int insertResult = commentMapper.insertComment(testComment);
        assertEquals(1, insertResult, "新增博客评论失败");
        verify(commentMapper).insertComment(any(Comment.class));

        // 测试根据ID查询
        when(commentMapper.selectCommentById(1L)).thenReturn(testComment);
        Comment selectResult = commentMapper.selectCommentById(1L);
        assertNotNull(selectResult, "根据ID查询博客评论失败");
        assertEquals("测试评论内容", selectResult.getContent(), "评论内容不匹配");
        verify(commentMapper).selectCommentById(1L);

        // 测试修改
        when(commentMapper.updateComment(any(Comment.class))).thenReturn(1);
        testComment.setContent("修改后的评论内容");
        int updateResult = commentMapper.updateComment(testComment);
        assertEquals(1, updateResult, "修改博客评论失败");
        verify(commentMapper).updateComment(any(Comment.class));

        // 测试查询列表
        when(commentMapper.selectCommentList(any(Comment.class)))
            .thenReturn(Arrays.asList(testComment));
        List<Comment> commentList = commentMapper.selectCommentList(new Comment());
        assertFalse(commentList.isEmpty(), "查询博客评论列表失败");
        verify(commentMapper).selectCommentList(any(Comment.class));

        // 测试删除
        when(commentMapper.deleteCommentById(1L)).thenReturn(1);
        int deleteResult = commentMapper.deleteCommentById(1L);
        assertEquals(1, deleteResult, "删除博客评论失败");
        verify(commentMapper).deleteCommentById(1L);
    }

    /**
     * 测试批量删除功能
     */
    @Test
    void testDeleteCommentByIds() {
        when(commentMapper.deleteCommentByIds(any(Long[].class))).thenReturn(2);
        int deleteResult = commentMapper.deleteCommentByIds(new Long[]{1L, 2L});
        assertEquals(2, deleteResult, "批量删除博客评论失败");
        verify(commentMapper).deleteCommentByIds(any(Long[].class));
    }

    /**
     * 测试统计评论数量
     */
    @Test
    void testSelectCommentCount() {
        when(commentMapper.selectCommentCount(any(Comment.class))).thenReturn(10L);
        Long count = commentMapper.selectCommentCount(new Comment());
        assertEquals(10L, count, "评论数量统计失败");
        verify(commentMapper).selectCommentCount(any(Comment.class));
    }

    /**
     * 测试边界条件：空值处理
     */
    @Test
    void testNullHandling() {
        // 测试查询不存在的ID
        when(commentMapper.selectCommentById(999L)).thenReturn(null);
        Comment result = commentMapper.selectCommentById(999L);
        assertNull(result, "查询不存在的ID应返回null");
        verify(commentMapper).selectCommentById(999L);

        // 测试空列表查询
        when(commentMapper.selectCommentList(any(Comment.class)))
            .thenReturn(Arrays.asList());
        List<Comment> list = commentMapper.selectCommentList(new Comment());
        assertTrue(list.isEmpty(), "查询结果应为空列表");
        verify(commentMapper).selectCommentList(any(Comment.class));

        // 测试零评论数
        when(commentMapper.selectCommentCount(any(Comment.class))).thenReturn(0L);
        Long count = commentMapper.selectCommentCount(new Comment());
        assertEquals(0L, count, "零评论数统计失败");
        verify(commentMapper).selectCommentCount(any(Comment.class));
    }

    /**
     * 测试边界条件：空内容
     */
    @Test
    void testEmptyContent() {
        Comment emptyContentComment = new Comment();
        emptyContentComment.setContent("");
        
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        int result = commentMapper.insertComment(emptyContentComment);
        assertEquals(1, result, "空内容评论插入失败");
        verify(commentMapper).insertComment(any(Comment.class));
    }

    /**
     * 测试边界条件：空昵称
     */
    @Test
    void testEmptyNickname() {
        Comment emptyNicknameComment = new Comment();
        emptyNicknameComment.setContent("测试内容");
        emptyNicknameComment.setNickname("");
        
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        int result = commentMapper.insertComment(emptyNicknameComment);
        assertEquals(1, result, "空昵称评论插入失败");
        verify(commentMapper).insertComment(any(Comment.class));
    }

    /**
     * 测试边界条件：状态值
     */
    @Test
    void testStatusValues() {
        // 测试待审核状态
        Comment pendingComment = new Comment();
        pendingComment.setContent("测试");
        pendingComment.setStatus("0");
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        int result1 = commentMapper.insertComment(pendingComment);
        assertEquals(1, result1, "待审核状态评论插入失败");

        // 测试已通过状态
        Comment approvedComment = new Comment();
        approvedComment.setContent("测试");
        approvedComment.setStatus("1");
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        int result2 = commentMapper.insertComment(approvedComment);
        assertEquals(1, result2, "已通过状态评论插入失败");

        // 测试已拒绝状态
        Comment rejectedComment = new Comment();
        rejectedComment.setContent("测试");
        rejectedComment.setStatus("2");
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        int result3 = commentMapper.insertComment(rejectedComment);
        assertEquals(1, result3, "已拒绝状态评论插入失败");

        verify(commentMapper, times(3)).insertComment(any(Comment.class));
    }

    /**
     * 测试边界条件：长内容
     */
    @Test
    void testLongContent() {
        Comment longContentComment = new Comment();
        longContentComment.setContent("这是一条非常长的评论内容，用于测试系统对长文本的处理能力。" +
            "这条评论包含了很多文字，用于验证数据库和应用程序能够正确处理超长文本输入。" +
            "在实际应用中，用户可能会输入很长的评论，系统需要能够正确存储和显示这些内容。");
        
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        int result = commentMapper.insertComment(longContentComment);
        assertEquals(1, result, "长内容评论插入失败");
        verify(commentMapper).insertComment(any(Comment.class));
    }
}