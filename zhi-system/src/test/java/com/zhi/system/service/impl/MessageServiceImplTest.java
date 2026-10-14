package com.zhi.system.service.impl;

import com.zhi.common.exception.ServiceException;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.domain.Message;
import com.zhi.system.mapper.MessageMapper;
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
import static org.mockito.Mockito.*;

/**
 * 留言板服务层单元测试
 *
 * @author test
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageServiceImpl messageService;

    private Message testMessage;

    @BeforeEach
    void setUp() {
        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setNickname("测试访客");
        testMessage.setContent("这是一条测试留言");
        testMessage.setStatus("1");
    }

    @Test
    void testSelectMessageList() {
        when(messageMapper.selectMessageList(any(Message.class)))
            .thenReturn(Arrays.asList(testMessage));

        List<Message> result = messageService.selectMessageList(new Message());

        assertEquals(1, result.size());
        assertEquals("测试访客", result.get(0).getNickname());
    }

    @Test
    void testSelectPublishedMessageList() {
        when(messageMapper.selectPublishedMessageList()).thenReturn(Collections.singletonList(testMessage));

        List<Message> result = messageService.selectPublishedMessageList();

        assertEquals(1, result.size());
        verify(messageMapper).selectPublishedMessageList();
    }

    @Test
    void testSelectPublishedMessageCount() {
        when(messageMapper.selectPublishedMessageCount()).thenReturn(5L);

        assertEquals(5L, messageService.selectPublishedMessageCount());
    }

    @Test
    void testSelectMessageById() {
        when(messageMapper.selectMessageById(1L)).thenReturn(testMessage);

        Message result = messageService.selectMessageById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testInsertMessageAppliesDefaults() {
        Message message = new Message();
        message.setNickname("访客");
        message.setContent("内容");

        when(messageMapper.insertMessage(any(Message.class))).thenReturn(1);

        int result = messageService.insertMessage(message);

        assertEquals(1, result);
        // 未显式指定状态时默认待审核，并补齐 delFlag 与创建时间
        assertEquals("0", message.getStatus());
        assertEquals("0", message.getDelFlag());
        assertNotNull(message.getCreateTime());
    }

    @Test
    void testInsertMessageKeepsGivenStatus() {
        Message message = new Message();
        message.setNickname("访客");
        message.setContent("内容");
        message.setStatus("1");

        when(messageMapper.insertMessage(any(Message.class))).thenReturn(1);

        messageService.insertMessage(message);

        assertEquals("1", message.getStatus());
    }

    @Test
    void testUpdateMessageSetsUpdateTime() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");
            when(messageMapper.updateMessage(any(Message.class))).thenReturn(1);

            int result = messageService.updateMessage(testMessage);

            assertEquals(1, result);
            assertNotNull(testMessage.getUpdateTime());
        }
    }

    @Test
    void testAuditMessageAcceptsPublished() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("admin");
            when(messageMapper.updateMessage(any(Message.class))).thenReturn(1);

            int result = messageService.auditMessage(1L, "1");

            assertEquals(1, result);
            verify(messageMapper).updateMessage(argThat(m -> "1".equals(m.getStatus()) && "admin".equals(m.getUpdateBy())));
        }
    }

    @Test
    void testAuditMessageAcceptsRejected() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("admin");
            when(messageMapper.updateMessage(any(Message.class))).thenReturn(1);

            assertEquals(1, messageService.auditMessage(1L, "2"));
        }
    }

    @Test
    void testAuditMessageRejectsIllegalStatus() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> messageService.auditMessage(1L, "9"));

        assertEquals("留言状态不合法", exception.getMessage());
        verify(messageMapper, never()).updateMessage(any(Message.class));
    }

    @Test
    void testReplyMessage() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("admin");
            when(messageMapper.updateMessage(any(Message.class))).thenReturn(1);

            int result = messageService.replyMessage(1L, "感谢留言");

            assertEquals(1, result);
            verify(messageMapper).updateMessage(argThat(m ->
                "感谢留言".equals(m.getReplyContent())
                    && "admin".equals(m.getReplyBy())
                    && "1".equals(m.getStatus())
                    && m.getReplyTime() != null));
        }
    }

    @Test
    void testReplyMessageRejectsEmptyContent() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> messageService.replyMessage(1L, "  "));

        assertEquals("回复内容不能为空", exception.getMessage());
        verify(messageMapper, never()).updateMessage(any(Message.class));
    }

    @Test
    void testReplyMessageRejectsTooLongContent() {
        String longContent = "a".repeat(501);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> messageService.replyMessage(1L, longContent));

        assertEquals("回复内容长度不能超过500个字符", exception.getMessage());
        verify(messageMapper, never()).updateMessage(any(Message.class));
    }

    @Test
    void testDeleteMessageById() {
        when(messageMapper.deleteMessageById(1L)).thenReturn(1);

        assertEquals(1, messageService.deleteMessageById(1L));
    }

    @Test
    void testDeleteMessageByIds() {
        Long[] ids = {1L, 2L};
        when(messageMapper.deleteMessageByIds(ids)).thenReturn(2);

        assertEquals(2, messageService.deleteMessageByIds(ids));
    }
}
