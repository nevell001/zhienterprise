package com.zhi.system.service.impl;

import com.zhi.system.domain.Article;
import com.zhi.system.domain.Comment;
import com.zhi.system.domain.Notification;
import com.zhi.system.mapper.ArticleMapper;
import com.zhi.system.mapper.CommentMapper;
import com.zhi.system.service.IArticleService;
import com.zhi.system.service.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 博客评论服务层单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private IArticleService articleService;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment testComment;

    @BeforeEach
    void setUp() {
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setContent("测试评论");
        testComment.setArticleId(1L);
        testComment.setUserId(1L);
        testComment.setStatus("0"); // 待审核
    }

    /**
     * 测试查询评论列表
     */
    @Test
    void testSelectCommentList() {
        // 准备数据
        List<Comment> commentList = Arrays.asList(testComment);
        when(commentMapper.selectCommentList(any(Comment.class)))
            .thenReturn(commentList);

        // 执行测试
        List<Comment> result = commentService.selectCommentList(new Comment());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试评论", result.get(0).getContent());
        verify(commentMapper).selectCommentList(any(Comment.class));
    }

    /**
     * 测试查询评论列表 - 空结果
     */
    @Test
    void testSelectCommentList_Empty() {
        // 模拟空结果
        when(commentMapper.selectCommentList(any(Comment.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<Comment> result = commentService.selectCommentList(new Comment());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(commentMapper).selectCommentList(any(Comment.class));
    }

    /**
     * 测试根据ID查询评论
     */
    @Test
    void testSelectCommentById() {
        // 准备数据
        when(commentMapper.selectCommentById(1L)).thenReturn(testComment);

        // 执行测试
        Comment result = commentService.selectCommentById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("测试评论", result.getContent());
        verify(commentMapper).selectCommentById(1L);
    }

    /**
     * 测试根据ID查询评论 - 不存在
     */
    @Test
    void testSelectCommentById_NotFound() {
        // 模拟评论不存在
        when(commentMapper.selectCommentById(999L)).thenReturn(null);

        // 执行测试
        Comment result = commentService.selectCommentById(999L);

        // 验证结果
        assertNull(result);
        verify(commentMapper).selectCommentById(999L);
    }

    /**
     * 测试新增评论 - 成功
     */
    @Test
    void testInsertComment_Success() {
        // 准备数据
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.insertComment(testComment);

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper).insertComment(any(Comment.class));
    }

    /**
     * 测试更新评论 - 成功
     */
    @Test
    void testUpdateComment_Success() {
        // 准备数据
        when(commentMapper.updateComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.updateComment(testComment);

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper).updateComment(any(Comment.class));
    }

    /**
     * 测试删除评论 - 成功
     */
    @Test
    void testDeleteCommentById_Success() {
        // 准备数据
        when(commentMapper.deleteCommentById(1L)).thenReturn(1);

        // 执行测试
        int result = commentService.deleteCommentById(1L);

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper).deleteCommentById(1L);
    }

    /**
     * 测试删除评论 - 不存在
     */
    @Test
    void testDeleteCommentById_NotFound() {
        // 准备数据
        when(commentMapper.deleteCommentById(999L)).thenReturn(0);

        // 执行测试
        int result = commentService.deleteCommentById(999L);

        // 验证结果
        assertEquals(0, result);
        verify(commentMapper).deleteCommentById(999L);
    }

    /**
     * 测试批量删除评论 - 成功
     */
    @Test
    void testDeleteCommentByIds_Success() {
        // 准备数据
        when(commentMapper.deleteCommentByIds(any(Long[].class))).thenReturn(2);

        // 执行测试
        int result = commentService.deleteCommentByIds(new Long[]{1L, 2L});

        // 验证结果
        assertEquals(2, result);
        verify(commentMapper).deleteCommentByIds(any(Long[].class));
    }

    /**
     * 测试批量删除评论 - 空列表
     */
    @Test
    void testDeleteCommentByIds_EmptyList() {
        // 执行测试
        int result = commentService.deleteCommentByIds(new Long[]{});

        // 验证结果
        assertEquals(0, result);
        verify(commentMapper).deleteCommentByIds(any(Long[].class));
    }

    /**
     * 测试查询评论数量
     */
    @Test
    void testSelectCommentCount() {
        // 准备数据
        when(commentMapper.selectCommentCount(any(Comment.class))).thenReturn(10L);

        // 执行测试
        Long result = commentService.selectCommentCount(new Comment());

        // 验证结果
        assertNotNull(result);
        assertEquals(10L, result);
        verify(commentMapper).selectCommentCount(any(Comment.class));
    }

    /**
     * 测试查询评论数量 - 空结果
     */
    @Test
    void testSelectCommentCount_Empty() {
        // 模拟空结果
        when(commentMapper.selectCommentCount(any(Comment.class))).thenReturn(0L);

        // 执行测试
        Long result = commentService.selectCommentCount(new Comment());

        // 验证结果
        assertNotNull(result);
        assertEquals(0L, result);
        verify(commentMapper).selectCommentCount(any(Comment.class));
    }

    /**
     * 测试评论内容长度限制
     */
    @Test
    void testInsertComment_LongContent() {
        // 设置超长内容
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("测试内容");
        }
        testComment.setContent(longContent.toString());

        // 准备数据
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.insertComment(testComment);

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper).insertComment(any(Comment.class));
    }

    /**
     * 测试评论状态更新
     */
    @Test
    void testUpdateComment_Status() {
        // 更新状态为已审核
        testComment.setStatus("1");
        when(commentMapper.updateComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.updateComment(testComment);

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper).updateComment(any(Comment.class));
    }

    /**
     * 测试评论内容包含特殊字符
     */
    @Test
    void testInsertComment_SpecialCharacters() {
        // 设置包含特殊字符的内容
        testComment.setContent("测试评论@#$%^&*()_+-={}[]|\\:\";'<>?,./~`");

        // 准备数据
        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.insertComment(testComment);

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper).insertComment(any(Comment.class));
    }

    /**
     * 测试审核通过博客评论 - 成功
     */
    @Test
    void testAuditCommentByIds_Success() {
        // 准备数据
        when(commentMapper.updateComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.auditCommentByIds(new Long[]{1L, 2L, 3L});

        // 验证结果
        assertEquals(3, result);
        verify(commentMapper, times(3)).updateComment(any(Comment.class));
    }

    /**
     * 测试审核通过博客评论 - 单个评论
     */
    @Test
    void testAuditCommentByIds_Single() {
        // 准备数据
        when(commentMapper.updateComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.auditCommentByIds(new Long[]{1L});

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper, times(1)).updateComment(any(Comment.class));
    }

    /**
     * 测试审核通过博客评论 - 空列表
     */
    @Test
    void testAuditCommentByIds_Empty() {
        // 执行测试
        int result = commentService.auditCommentByIds(new Long[]{});

        // 验证结果
        assertEquals(0, result);
        verify(commentMapper, never()).updateComment(any(Comment.class));
    }

    /**
     * 测试审核通过博客评论 - 验证状态
     */
    @Test
    void testAuditCommentByIds_VerifyStatus() {
        // 准备数据
        when(commentMapper.updateComment(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            assertEquals("1", comment.getStatus()); // 验证状态为已审核通过
            return 1;
        });

        // 执行测试
        int result = commentService.auditCommentByIds(new Long[]{1L});

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper).updateComment(any(Comment.class));
    }

    /**
     * 测试审核拒绝博客评论 - 成功
     */
    @Test
    void testRejectCommentByIds_Success() {
        // 准备数据
        when(commentMapper.updateComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.rejectCommentByIds(new Long[]{1L, 2L, 3L});

        // 验证结果
        assertEquals(3, result);
        verify(commentMapper, times(3)).updateComment(any(Comment.class));
    }

    /**
     * 测试审核拒绝博客评论 - 单个评论
     */
    @Test
    void testRejectCommentByIds_Single() {
        // 准备数据
        when(commentMapper.updateComment(any(Comment.class))).thenReturn(1);

        // 执行测试
        int result = commentService.rejectCommentByIds(new Long[]{1L});

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper, times(1)).updateComment(any(Comment.class));
    }

    /**
     * 测试审核拒绝博客评论 - 空列表
     */
    @Test
    void testRejectCommentByIds_Empty() {
        // 执行测试
        int result = commentService.rejectCommentByIds(new Long[]{});

        // 验证结果
        assertEquals(0, result);
        verify(commentMapper, never()).updateComment(any(Comment.class));
    }

    /**
     * 测试审核拒绝博客评论 - 验证状态
     */
    @Test
    void testRejectCommentByIds_VerifyStatus() {
        // 准备数据
        when(commentMapper.updateComment(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            assertEquals("2", comment.getStatus()); // 验证状态为已删除（拒绝）
            return 1;
        });

        // 执行测试
        int result = commentService.rejectCommentByIds(new Long[]{1L});

        // 验证结果
        assertEquals(1, result);
        verify(commentMapper).updateComment(any(Comment.class));
    }

    /**
     * 测试审核通过和拒绝博客评论 - 批量操作
     */
    @Test
    void testAuditAndRejectComment_Batch() {
        // 准备数据
        when(commentMapper.updateComment(any(Comment.class))).thenReturn(1);

        // 执行测试 - 审核通过
        int auditCount = commentService.auditCommentByIds(new Long[]{1L, 2L});
        assertEquals(2, auditCount);

        // 执行测试 - 审核拒绝
        int rejectCount = commentService.rejectCommentByIds(new Long[]{3L, 4L, 5L});
        assertEquals(3, rejectCount);

        // 验证结果
        verify(commentMapper, times(5)).updateComment(any(Comment.class));
    }

    /**
     * 测试审核通过博客评论 - 包含失败的更新
     */
    @Test
    void testAuditCommentByIds_WithFailures() {
        // 准备数据 - 部分更新失败
        when(commentMapper.updateComment(any(Comment.class)))
            .thenReturn(1)
            .thenReturn(0)
            .thenReturn(1);

        // 执行测试
        int result = commentService.auditCommentByIds(new Long[]{1L, 2L, 3L});

        // 验证结果 - 应该返回实际更新的数量
        assertEquals(2, result);
        verify(commentMapper, times(3)).updateComment(any(Comment.class));
    }

    /**
     * 测试审核拒绝博客评论 - 包含失败的更新
     */
    @Test
    void testRejectCommentByIds_WithFailures() {
        // 准备数据 - 部分更新失败
        when(commentMapper.updateComment(any(Comment.class)))
            .thenReturn(1)
            .thenReturn(0)
            .thenReturn(0);

        // 执行测试
        int result = commentService.rejectCommentByIds(new Long[]{1L, 2L, 3L});

        // 验证结果 - 应该返回实际更新的数量
        assertEquals(1, result);
        verify(commentMapper, times(3)).updateComment(any(Comment.class));
    }

    /**
     * 测试新增回复评论 - 生成回复通知
     */
    @Test
    void testInsertComment_ReplyNotification() {
        // 准备数据
        Article article = new Article();
        article.setId(1L);
        article.setTitle("测试文章");
        article.setAuthorId(10L);

        Comment parentComment = new Comment();
        parentComment.setId(2L);
        parentComment.setUserId(20L);

        Comment reply = new Comment();
        reply.setId(3L);
        reply.setArticleId(1L);
        reply.setParentId(2L);
        reply.setUserId(1L);
        reply.setContent("回复内容");
        reply.setNickname("评论者");

        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        when(articleService.selectArticleById(1L)).thenReturn(article);
        when(commentMapper.selectCommentById(2L)).thenReturn(parentComment);

        // 执行测试
        int result = commentService.insertComment(reply);

        // 验证结果
        assertEquals(1, result);
        verify(notificationService).createNotification(argThat(notification ->
            "reply".equals(notification.getType()) && Long.valueOf(20L).equals(notification.getRecipientId())));
    }

    /**
     * 测试给自己评论/回复 - 跳过通知
     */
    @Test
    void testInsertComment_SelfNotificationSkipped() {
        // 准备数据：评论者与文章作者相同
        Article article = new Article();
        article.setId(1L);
        article.setTitle("测试文章");
        article.setAuthorId(1L);

        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        when(articleService.selectArticleById(1L)).thenReturn(article);

        // 执行测试
        int result = commentService.insertComment(testComment);

        // 验证结果
        assertEquals(1, result);
        verify(notificationService, never()).createNotification(any(Notification.class));
    }

    /**
     * 测试匿名用户发表超长评论 - 昵称兜底与内容截断
     */
    @Test
    void testInsertComment_AnonymousLongContent() {
        // 准备数据
        Article article = new Article();
        article.setId(1L);
        article.setTitle("测试文章");
        article.setAuthorId(10L);

        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longContent.append("很长很长的评论内容");
        }

        Comment comment = new Comment();
        comment.setId(4L);
        comment.setArticleId(1L);
        comment.setUserId(1L);
        comment.setContent(longContent.toString());
        comment.setNickname(null);

        when(commentMapper.insertComment(any(Comment.class))).thenReturn(1);
        when(articleService.selectArticleById(1L)).thenReturn(article);

        // 执行测试
        int result = commentService.insertComment(comment);

        // 验证结果
        assertEquals(1, result);
        verify(notificationService).createNotification(argThat(notification ->
            "匿名用户".equals(notification.getSenderName())
                && notification.getContent() != null
                && notification.getContent().length() <= 203
                && notification.getContent().endsWith("...")));
    }

    /**
     * 测试编辑评论状态：待审核 → 已发布 时同步文章评论数
     */
    @Test
    void testUpdateCommentStatusToPublishedAdjustsCount() {
        Comment existing = new Comment();
        existing.setId(1L);
        existing.setArticleId(10L);
        existing.setStatus("0");

        Comment update = new Comment();
        update.setId(1L);
        update.setStatus("1");

        when(commentMapper.selectCommentById(1L)).thenReturn(existing);
        when(commentMapper.updateComment(update)).thenReturn(1);
        when(articleMapper.changeCommentCount(10L, 1)).thenReturn(1);

        commentService.updateComment(update);

        verify(articleMapper).changeCommentCount(10L, 1);
    }

    /**
     * 测试编辑评论状态：已发布 → 待审核 时减少文章评论数
     */
    @Test
    void testUpdateCommentStatusAwayFromPublishedAdjustsCount() {
        Comment existing = new Comment();
        existing.setId(1L);
        existing.setArticleId(10L);
        existing.setStatus("1");

        Comment update = new Comment();
        update.setId(1L);
        update.setStatus("0");

        when(commentMapper.selectCommentById(1L)).thenReturn(existing);
        when(commentMapper.updateComment(update)).thenReturn(1);
        when(articleMapper.changeCommentCount(10L, -1)).thenReturn(1);

        commentService.updateComment(update);

        verify(articleMapper).changeCommentCount(10L, -1);
    }

    /**
     * 测试编辑评论但状态未变化时不同步文章评论数
     */
    @Test
    void testUpdateCommentWithoutStatusChangeKeepsCount() {
        Comment existing = new Comment();
        existing.setId(1L);
        existing.setArticleId(10L);
        existing.setStatus("0");

        Comment update = new Comment();
        update.setId(1L);
        update.setStatus("0");

        when(commentMapper.selectCommentById(1L)).thenReturn(existing);
        when(commentMapper.updateComment(update)).thenReturn(1);

        commentService.updateComment(update);

        verify(articleMapper, never()).changeCommentCount(any(), anyInt());
    }

    @Test
    void countSyncFailureMustPropagateSoTransactionRollsBack() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setArticleId(9L);
        comment.setUserId(5L);
        comment.setStatus("1"); // 已发布 → 需要同步文章评论数

        when(commentMapper.insertComment(comment)).thenReturn(1);
        doThrow(new RuntimeException("changeCommentCount failed"))
            .when(articleMapper).changeCommentCount(9L, 1);

        // 旧实现把异常吞掉并返回 1：评论已可见但 comment_count 永久少 1
        assertThrows(RuntimeException.class, () -> commentService.insertComment(comment),
            "评论数与真实评论不一致时，写路径必须抛出让事务回滚");
    }

    @Test
    void writeMethodsMustBeTransactional() throws Exception {
        String[] writeMethods = { "insertComment", "updateComment", "deleteCommentByIds",
            "deleteCommentById", "auditCommentByIds", "rejectCommentByIds" };
        for (String name : writeMethods) {
            boolean found = java.util.Arrays.stream(CommentServiceImpl.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .anyMatch(m -> m.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
            assertTrue(found, name + " 必须带 @Transactional（评论写入与 comment_count 增减要原子）");
        }
    }
}
