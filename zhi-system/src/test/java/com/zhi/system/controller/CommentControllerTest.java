package com.zhi.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 博客评论控制器单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@WebMvcTest(controllers = CommentController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.zhi.system.service.ICommentService commentService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试获取评论列表接口
     */
    @Test
    void testGetCommentList() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Comment> commentList = new ArrayList<>();
        com.zhi.system.domain.Comment comment = new com.zhi.system.domain.Comment();
        comment.setId(1L);
        comment.setContent("测试评论");
        commentList.add(comment);

        when(commentService.selectCommentList(any(com.zhi.system.domain.Comment.class)))
            .thenReturn(commentList);

        // 执行测试
        mockMvc.perform(get("/system/comment/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].content").value("测试评论"));

        verify(commentService).selectCommentList(any(com.zhi.system.domain.Comment.class));
    }

    /**
     * 测试获取评论详情接口
     */
    @Test
    void testGetCommentDetail() throws Exception {
        // 模拟数据
        com.zhi.system.domain.Comment comment = new com.zhi.system.domain.Comment();
        comment.setId(1L);
        comment.setContent("测试评论");

        when(commentService.selectCommentById(1L)).thenReturn(comment);

        // 执行测试
        mockMvc.perform(get("/system/comment/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("测试评论"));

        verify(commentService).selectCommentById(1L);
    }

    /**
     * 测试新增评论接口
     */
    @Test
    void testAddComment() throws Exception {
        // 模拟成功添加
        when(commentService.insertComment(any(com.zhi.system.domain.Comment.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("articleId", 1L);
        params.put("content", "测试评论");
        params.put("userId", 1L);

        // 执行测试
        mockMvc.perform(post("/system/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(commentService).insertComment(any(com.zhi.system.domain.Comment.class));
    }

    /**
     * 测试更新评论接口
     */
    @Test
    void testEditComment() throws Exception {
        // 模拟成功更新
        when(commentService.updateComment(any(com.zhi.system.domain.Comment.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("content", "修改后的评论");

        // 执行测试
        mockMvc.perform(put("/system/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(commentService).updateComment(any(com.zhi.system.domain.Comment.class));
    }

    /**
     * 测试删除评论接口
     */
    @Test
    void testRemoveComment() throws Exception {
        // 模拟成功删除
        when(commentService.deleteCommentByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(delete("/system/comment/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(commentService).deleteCommentByIds(any(Long[].class));
    }

    /**
     * 测试审核通过评论接口
     */
    @Test
    void testAuditComment() throws Exception {
        // 模拟成功审核
        when(commentService.auditCommentByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(put("/system/comment/audit/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(commentService).auditCommentByIds(any(Long[].class));
    }

    /**
     * 测试审核拒绝评论接口
     */
    @Test
    void testRejectComment() throws Exception {
        // 模拟成功拒绝
        when(commentService.rejectCommentByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(put("/system/comment/reject/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(commentService).rejectCommentByIds(any(Long[].class));
    }

    /**
     * 测试导出评论接口
     */
    @Test
    void testExportComment() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Comment> commentList = new ArrayList<>();
        com.zhi.system.domain.Comment comment = new com.zhi.system.domain.Comment();
        comment.setId(1L);
        comment.setContent("测试评论");
        commentList.add(comment);

        when(commentService.selectCommentList(any(com.zhi.system.domain.Comment.class)))
            .thenReturn(commentList);

        // 执行测试
        mockMvc.perform(post("/system/comment/export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commentService).selectCommentList(any(com.zhi.system.domain.Comment.class));
    }

    /**
     * 测试新增评论接口 - 失败
     */
    @Test
    void testAddComment_Failure() throws Exception {
        // 模拟添加失败
        when(commentService.insertComment(any(com.zhi.system.domain.Comment.class)))
            .thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("articleId", 1L);
        params.put("content", "测试评论");
        params.put("userId", 1L);

        // 执行测试
        mockMvc.perform(post("/system/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(commentService).insertComment(any(com.zhi.system.domain.Comment.class));
    }

    /**
     * 测试更新评论接口 - 失败
     */
    @Test
    void testEditComment_Failure() throws Exception {
        // 模拟更新失败
        when(commentService.updateComment(any(com.zhi.system.domain.Comment.class)))
            .thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("content", "修改后的评论");

        // 执行测试
        mockMvc.perform(put("/system/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(commentService).updateComment(any(com.zhi.system.domain.Comment.class));
    }

    /**
     * 测试删除评论接口 - 失败
     */
    @Test
    void testRemoveComment_Failure() throws Exception {
        // 模拟删除失败
        when(commentService.deleteCommentByIds(any(Long[].class))).thenReturn(0);

        // 执行测试
        mockMvc.perform(delete("/system/comment/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(commentService).deleteCommentByIds(any(Long[].class));
    }

    /**
     * 测试审核通过评论接口 - 失败
     */
    @Test
    void testAuditComment_Failure() throws Exception {
        // 模拟审核失败
        when(commentService.auditCommentByIds(any(Long[].class))).thenReturn(0);

        // 执行测试
        mockMvc.perform(put("/system/comment/audit/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(commentService).auditCommentByIds(any(Long[].class));
    }

    /**
     * 测试审核拒绝评论接口 - 失败
     */
    @Test
    void testRejectComment_Failure() throws Exception {
        // 模拟拒绝失败
        when(commentService.rejectCommentByIds(any(Long[].class))).thenReturn(0);

        // 执行测试
        mockMvc.perform(put("/system/comment/reject/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(commentService).rejectCommentByIds(any(Long[].class));
    }
}