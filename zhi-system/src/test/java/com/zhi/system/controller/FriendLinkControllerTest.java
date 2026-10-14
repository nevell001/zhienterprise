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
 * 友情链接控制器单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@WebMvcTest(controllers = FriendLinkController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class FriendLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.zhi.system.service.IFriendLinkService friendLinkService;

    @MockBean
    private com.zhi.system.service.ISettingService settingService;

    @MockBean
    private com.zhi.system.service.ICaptchaService captchaService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试获取友情链接列表接口
     */
    @Test
    void testGetFriendLinkList() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.FriendLink> linkList = new ArrayList<>();
        com.zhi.system.domain.FriendLink link = new com.zhi.system.domain.FriendLink();
        link.setId(1L);
        link.setName("测试友情链接");
        link.setUrl("https://example.com");
        linkList.add(link);

        when(friendLinkService.selectFriendLinkList(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(linkList);

        // 执行测试
        mockMvc.perform(get("/system/friendLink/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].name").value("测试友情链接"));

        verify(friendLinkService).selectFriendLinkList(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试获取前台友情链接列表接口
     */
    @Test
    void testGetFrontFriendLinkList() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.FriendLink> linkList = new ArrayList<>();
        com.zhi.system.domain.FriendLink link = new com.zhi.system.domain.FriendLink();
        link.setId(1L);
        link.setName("测试友情链接");
        link.setUrl("https://example.com");
        link.setStatus("0"); // 启用状态
        linkList.add(link);

        when(friendLinkService.selectFrontFriendLinkList()).thenReturn(linkList);

        // 执行测试
        mockMvc.perform(get("/system/friendLink/front/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("测试友情链接"));

        verify(friendLinkService).selectFrontFriendLinkList();
    }

    /**
     * 测试获取友情链接详情接口
     */
    @Test
    void testGetFriendLinkDetail() throws Exception {
        // 模拟数据
        com.zhi.system.domain.FriendLink link = new com.zhi.system.domain.FriendLink();
        link.setId(1L);
        link.setName("测试友情链接");
        link.setUrl("https://example.com");

        when(friendLinkService.selectFriendLinkById(1L)).thenReturn(link);

        // 执行测试
        mockMvc.perform(get("/system/friendLink/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试友情链接"));

        verify(friendLinkService).selectFriendLinkById(1L);
    }

    /**
     * 测试新增友情链接接口
     */
    @Test
    void testAddFriendLink() throws Exception {
        // 模拟成功添加
        when(friendLinkService.insertFriendLink(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("name", "测试友情链接");
        params.put("url", "https://example.com");
        params.put("status", "0");

        // 执行测试
        mockMvc.perform(post("/system/friendLink")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(friendLinkService).insertFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试更新友情链接接口
     */
    @Test
    void testEditFriendLink() throws Exception {
        // 模拟成功更新
        when(friendLinkService.updateFriendLink(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("name", "修改后的友情链接");
        params.put("url", "https://example.com");

        // 执行测试
        mockMvc.perform(put("/system/friendLink")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(friendLinkService).updateFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试删除友情链接接口
     */
    @Test
    void testRemoveFriendLink() throws Exception {
        // 模拟成功删除
        when(friendLinkService.deleteFriendLinkByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(delete("/system/friendLink/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(friendLinkService).deleteFriendLinkByIds(any(Long[].class));
    }

    /**
     * 测试导出友情链接接口
     */
    @Test
    void testExportFriendLink() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.FriendLink> linkList = new ArrayList<>();
        com.zhi.system.domain.FriendLink link = new com.zhi.system.domain.FriendLink();
        link.setId(1L);
        link.setName("测试友情链接");
        link.setUrl("https://example.com");
        linkList.add(link);

        when(friendLinkService.selectFriendLinkList(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(linkList);

        // 执行测试
        mockMvc.perform(post("/system/friendLink/export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(friendLinkService).selectFriendLinkList(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试新增友情链接接口 - 失败
     */
    @Test
    void testAddFriendLink_Failure() throws Exception {
        // 模拟添加失败
        when(friendLinkService.insertFriendLink(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("name", "测试友情链接");
        params.put("url", "https://example.com");
        params.put("status", "0");

        // 执行测试
        mockMvc.perform(post("/system/friendLink")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(friendLinkService).insertFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试更新友情链接接口 - 失败
     */
    @Test
    void testEditFriendLink_Failure() throws Exception {
        // 模拟更新失败
        when(friendLinkService.updateFriendLink(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("name", "修改后的友情链接");
        params.put("url", "https://example.com");

        // 执行测试
        mockMvc.perform(put("/system/friendLink")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(friendLinkService).updateFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试删除友情链接接口 - 失败
     */
    @Test
    void testRemoveFriendLink_Failure() throws Exception {
        // 模拟删除失败
        when(friendLinkService.deleteFriendLinkByIds(any(Long[].class))).thenReturn(0);

        // 执行测试
        mockMvc.perform(delete("/system/friendLink/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(friendLinkService).deleteFriendLinkByIds(any(Long[].class));
    }

    /**
     * 测试前台申请友情链接
     */
    @Test
    void testApplyFriendLink() throws Exception {
        when(friendLinkService.insertFriendLink(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(1);

        Map<String, Object> params = new HashMap<>();
        params.put("name", "申请站点");
        params.put("url", "https://blog.example.com");
        params.put("description", "一个示例站点");
        params.put("email", "owner@example.com");

        mockMvc.perform(post("/system/friendLink/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(friendLinkService).insertFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试前台申请友情链接 - 申请入口关闭时拒绝提交
     */
    @Test
    void testApplyFriendLink_RejectedWhenApplyDisabled() throws Exception {
        when(settingService.selectSettingValueByKey("friend_link_apply_enabled")).thenReturn("false");

        Map<String, Object> params = new HashMap<>();
        params.put("name", "申请站点");
        params.put("url", "https://blog.example.com");

        mockMvc.perform(post("/system/friendLink/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("本站暂未开放友链申请"));

        verify(friendLinkService, never()).insertFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试前台申请友情链接 - 启用验证码时把 code/uuid 交给验证码服务校验
     */
    @Test
    void testApplyFriendLink_ValidatesCaptcha() throws Exception {
        when(friendLinkService.insertFriendLink(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(1);

        Map<String, Object> params = new HashMap<>();
        params.put("name", "申请站点");
        params.put("url", "https://blog.example.com");
        params.put("code", "1234");
        params.put("uuid", "uuid-abc");

        mockMvc.perform(post("/system/friendLink/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(captchaService).validate("1234", "uuid-abc");
    }

    /**
     * 测试前台申请友情链接 - 缺少名称与地址
     */
    @Test
    void testApplyFriendLink_InvalidParams() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "");
        params.put("url", "");

        mockMvc.perform(post("/system/friendLink/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(friendLinkService, never()).insertFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试审核通过友情链接
     */
    @Test
    void testAuditFriendLink_Pass() throws Exception {
        when(friendLinkService.updateFriendLink(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(1);

        mockMvc.perform(put("/system/friendLink/audit/1/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(friendLinkService).updateFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试审核驳回友情链接
     */
    @Test
    void testAuditFriendLink_Reject() throws Exception {
        when(friendLinkService.updateFriendLink(any(com.zhi.system.domain.FriendLink.class)))
            .thenReturn(1);

        mockMvc.perform(put("/system/friendLink/audit/1/0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(friendLinkService).updateFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }

    /**
     * 测试审核参数不合法
     */
    @Test
    void testAuditFriendLink_InvalidStatus() throws Exception {
        mockMvc.perform(put("/system/friendLink/audit/1/3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(friendLinkService, never()).updateFriendLink(any(com.zhi.system.domain.FriendLink.class));
    }
}