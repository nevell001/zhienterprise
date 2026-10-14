package com.zhi.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhi.system.domain.Message;
import com.zhi.system.service.IMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 留言板后台控制器单元测试
 *
 * @author test
 * @date 2026-09-10
 */
@WebMvcTest(controllers = MessageController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IMessageService messageService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private Message buildMessage() {
        Message message = new Message();
        message.setId(1L);
        message.setNickname("测试访客");
        message.setContent("这是一条留言");
        message.setStatus("1");
        return message;
    }

    @Test
    void testList() throws Exception {
        List<Message> list = new ArrayList<>();
        list.add(buildMessage());
        when(messageService.selectMessageList(any(Message.class))).thenReturn(list);

        mockMvc.perform(get("/system/message/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.rows[0].nickname").value("测试访客"));
    }

    @Test
    void testGetInfo() throws Exception {
        when(messageService.selectMessageById(1L)).thenReturn(buildMessage());

        mockMvc.perform(get("/system/message/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").value("这是一条留言"));
    }

    @Test
    void testEdit() throws Exception {
        when(messageService.updateMessage(any(Message.class))).thenReturn(1);

        mockMvc.perform(put("/system/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildMessage())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(messageService).updateMessage(any(Message.class));
    }

    @Test
    void testAudit() throws Exception {
        when(messageService.auditMessage(eq(1L), eq("1"))).thenReturn(1);

        mockMvc.perform(put("/system/message/audit/1/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(messageService).auditMessage(1L, "1");
    }

    @Test
    void testReply() throws Exception {
        when(messageService.replyMessage(eq(1L), eq("感谢留言"))).thenReturn(1);

        Message body = new Message();
        body.setReplyContent("感谢留言");

        mockMvc.perform(put("/system/message/reply/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(messageService).replyMessage(1L, "感谢留言");
    }

    @Test
    void testRemove() throws Exception {
        when(messageService.deleteMessageByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/system/message/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(messageService).deleteMessageByIds(any(Long[].class));
    }
}
