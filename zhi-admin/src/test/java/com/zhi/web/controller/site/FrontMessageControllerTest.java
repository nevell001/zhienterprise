package com.zhi.web.controller.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.system.domain.Message;
import com.zhi.system.service.IMessageService;
import com.zhi.system.service.ISettingService;
import com.zhi.system.service.ICaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 留言板前台接口单元测试
 * 覆盖 /api/message 的服务端校验、审核开关与公开列表
 */
class FrontMessageControllerTest
{
    private IMessageService messageService;
    private ISettingService settingService;
    private ICaptchaService captchaService;
    private UnifiedCacheManager unifiedCacheManager;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp()
    {
        messageService = mock(IMessageService.class);
        settingService = mock(ISettingService.class);
        captchaService = mock(ICaptchaService.class);
        unifiedCacheManager = mock(UnifiedCacheManager.class);

        // 默认：验证码未启用（无法判定填写耗时），且没有冷却中的提交
        lenient().when(captchaService.validateAndGetAgeSeconds(any(), any())).thenReturn(-1L);

        FrontMessageController controller = new FrontMessageController();
        ReflectionTestUtils.setField(controller, "messageService", messageService);
        ReflectionTestUtils.setField(controller, "settingService", settingService);
        ReflectionTestUtils.setField(controller, "captchaService", captchaService);
        ReflectionTestUtils.setField(controller, "unifiedCacheManager", unifiedCacheManager);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void listShouldExposePublishedMessages() throws Exception
    {
        Message message = new Message();
        message.setId(1L);
        message.setNickname("访客");
        message.setContent("你好");
        when(messageService.selectPublishedMessageList()).thenReturn(Collections.singletonList(message));

        mockMvc.perform(get("/api/message/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.rows[0].nickname").value("访客"))
            // 公开接口不应返回邮箱等隐私字段
            .andExpect(jsonPath("$.rows[0].email").doesNotExist());
    }

    @Test
    void countShouldReturnPublishedCount() throws Exception
    {
        when(messageService.selectPublishedMessageCount()).thenReturn(7L);

        mockMvc.perform(get("/api/message/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(7));
    }

    /**
     * 留言板开关 guestbook_enabled：关闭后三个公开端点全部拒绝；
     * 未配置时按开启处理（与全站开关口径一致）。
     */
    @Test
    void listShouldBeRejectedWhenGuestbookDisabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("guestbook_enabled")).thenReturn("false");

        mockMvc.perform(get("/api/message/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("本站暂未开放留言板"))
            .andExpect(jsonPath("$.total").value(0));

        verify(messageService, never()).selectPublishedMessageList();
    }

    @Test
    void countShouldBeRejectedWhenGuestbookDisabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("guestbook_enabled")).thenReturn("0");

        mockMvc.perform(get("/api/message/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("本站暂未开放留言板"));

        verify(messageService, never()).selectPublishedMessageCount();
    }

    @Test
    void addShouldBeRejectedWhenGuestbookDisabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("guestbook_enabled")).thenReturn("false");

        perform(validMessage())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("本站暂未开放留言板"));

        verify(messageService, never()).insertMessage(any(Message.class));
        // 关闭判定在验证码之前，被拒不会消耗一次性验证码
        verify(captchaService, never()).validateAndGetAgeSeconds(any(), any());
    }

    @Test
    void shouldKeepGuestbookOpenWhenSettingMissing() throws Exception
    {
        when(settingService.selectSettingValueByKey("guestbook_enabled")).thenReturn(null);
        when(messageService.selectPublishedMessageCount()).thenReturn(3L);

        mockMvc.perform(get("/api/message/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void addShouldRejectMissingNickname() throws Exception
    {
        Message message = validMessage();
        message.setNickname("   ");

        perform(message)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));

        verify(messageService, never()).insertMessage(any(Message.class));
    }

    /**
     * 登录用户不带昵称提交留言：应按账号信息补全后放行。
     * 补全曾写在校验之后，登录态留言会被一律拒成"昵称不能为空"（与评论同一处顺序缺陷）。
     */
    @Test
    void addShouldFillNicknameFromAccountWhenLoggedIn() throws Exception
    {
        Message message = validMessage();
        message.setNickname(null);
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("false");
        when(messageService.insertMessage(any(Message.class))).thenReturn(1);

        SysUser account = new SysUser();
        account.setNickName("留言用户");
        LoginUser loginUser = new LoginUser(88L, null, account, null);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
        try
        {
            perform(message)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageService).insertMessage(captor.capture());
        assertEquals("留言用户", captor.getValue().getNickname());
        assertEquals(88L, captor.getValue().getUserId());
    }

    @Test
    void addShouldRejectTooLongNickname() throws Exception
    {
        Message message = validMessage();
        message.setNickname("a".repeat(51));

        perform(message).andExpect(jsonPath("$.code").value(500));

        verify(messageService, never()).insertMessage(any(Message.class));
    }

    @Test
    void addShouldRejectEmptyContent() throws Exception
    {
        Message message = validMessage();
        message.setContent("  ");

        perform(message).andExpect(jsonPath("$.code").value(500));

        verify(messageService, never()).insertMessage(any(Message.class));
    }

    @Test
    void addShouldRejectTooLongContent() throws Exception
    {
        Message message = validMessage();
        message.setContent("a".repeat(501));

        perform(message).andExpect(jsonPath("$.code").value(500));

        verify(messageService, never()).insertMessage(any(Message.class));
    }

    @Test
    void addShouldRejectTooLongEmail() throws Exception
    {
        Message message = validMessage();
        message.setEmail("a".repeat(101));

        perform(message).andExpect(jsonPath("$.code").value(500));

        verify(messageService, never()).insertMessage(any(Message.class));
    }

    @Test
    void addShouldRejectTooLongWebsite() throws Exception
    {
        Message message = validMessage();
        message.setWebsite("https://" + "a".repeat(250) + ".com");

        perform(message).andExpect(jsonPath("$.code").value(500));

        verify(messageService, never()).insertMessage(any(Message.class));
    }

    @Test
    void addShouldSetPendingStatusWhenReviewEnabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("true");
        when(messageService.insertMessage(any(Message.class))).thenReturn(1);

        perform(validMessage())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(messageService).insertMessage(argThatMessage("0"));
    }

    @Test
    void addShouldPublishDirectlyWhenReviewDisabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("false");
        when(messageService.insertMessage(any(Message.class))).thenReturn(1);

        perform(validMessage())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(messageService).insertMessage(argThatMessage("1"));
    }

    @Test
    void addShouldDefaultToReviewWhenSettingMissing() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn(null);
        when(messageService.insertMessage(any(Message.class))).thenReturn(1);

        perform(validMessage()).andExpect(jsonPath("$.code").value(200));

        verify(messageService).insertMessage(argThatMessage("0"));
    }

    @Test
    void addShouldCaptureClientInfoAndResetServerFields() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("false");
        when(messageService.insertMessage(any(Message.class))).thenReturn(1);

        Message message = validMessage();
        message.setId(99L);
        message.setDelFlag("1");
        message.setReplyContent("伪造回复");
        message.setReplyBy("伪造人");

        mockMvc.perform(post("/api/message")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "JUnit-Agent")
                .content(objectMapper.writeValueAsString(message)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(messageService).insertMessage(argThat(m ->
            m.getId() == null
                && "0".equals(m.getDelFlag())
                && m.getReplyContent() == null
                && m.getReplyBy() == null
                && "JUnit-Agent".equals(m.getUserAgent())
                && m.getIp() != null));
    }

    @Test
    void addShouldRejectSubmissionFasterThanMinimumFillTime() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("false");
        // 验证码签发后 1 秒即提交
        when(captchaService.validateAndGetAgeSeconds(any(), any())).thenReturn(1L);

        perform(validMessage())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("提交过快，请认真填写后再提交"));

        verify(messageService, never()).insertMessage(any(Message.class));
    }

    @Test
    void addShouldAcceptSubmissionReachingMinimumFillTime() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("false");
        when(captchaService.validateAndGetAgeSeconds(any(), any())).thenReturn(3L);
        when(messageService.insertMessage(any(Message.class))).thenReturn(1);

        perform(validMessage()).andExpect(jsonPath("$.code").value(200));

        verify(messageService).insertMessage(any(Message.class));
    }

    @Test
    void addShouldRejectWhileInCooldownWithoutConsumingCaptcha() throws Exception
    {
        when(unifiedCacheManager.exists(anyString())).thenReturn(true);
        when(unifiedCacheManager.getExpire(anyString())).thenReturn(42L);

        perform(validMessage())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("留言太频繁啦，请 42 秒后再试"));

        verify(messageService, never()).insertMessage(any(Message.class));
        // 冷却拦截发生在验证码校验之前，避免白白消耗一次验证码
        verify(captchaService, never()).validateAndGetAgeSeconds(any(), any());
    }

    @Test
    void addShouldFallbackToFullCooldownWhenTtlUnavailable() throws Exception
    {
        when(unifiedCacheManager.exists(anyString())).thenReturn(true);
        when(unifiedCacheManager.getExpire(anyString())).thenReturn(0L);

        perform(validMessage())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("留言太频繁啦，请 60 秒后再试"));
    }

    @Test
    void addShouldSetCooldownKeyAfterSuccessfulInsert() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("false");
        when(messageService.insertMessage(any(Message.class))).thenReturn(1);

        perform(validMessage()).andExpect(jsonPath("$.code").value(200));

        verify(unifiedCacheManager).set(
            argThat(key -> key != null && key.startsWith("zhi:message:cooldown:")),
            any(),
            eq(60L),
            eq(TimeUnit.SECONDS));
    }

    @Test
    void addShouldNotSetCooldownWhenInsertFails() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("false");
        when(messageService.insertMessage(any(Message.class))).thenReturn(0);

        perform(validMessage()).andExpect(jsonPath("$.code").value(500));

        verify(unifiedCacheManager, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void addShouldForwardCaptchaParamsToService() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_review")).thenReturn("false");
        when(messageService.insertMessage(any(Message.class))).thenReturn(1);

        Message message = validMessage();
        message.setCode("A1B2");
        message.setUuid("uuid-abc");

        perform(message).andExpect(jsonPath("$.code").value(200));

        verify(captchaService).validateAndGetAgeSeconds("A1B2", "uuid-abc");
    }

    private ResultActions perform(Message message) throws Exception
    {
        return mockMvc.perform(post("/api/message")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(message)));
    }

    private Message argThatMessage(String expectedStatus)
    {
        return argThat(m -> expectedStatus.equals(m.getStatus()) && "0".equals(m.getDelFlag()));
    }

    private Message validMessage()
    {
        Message message = new Message();
        message.setNickname("测试访客");
        message.setContent("这是一条测试留言");
        return message;
    }
}
