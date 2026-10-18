package com.zhi.system.service.impl;

import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.system.domain.Message;
import com.zhi.system.domain.Notification;
import com.zhi.system.mapper.NotificationMapper;
import com.zhi.system.mapper.SysUserMapper;
import com.zhi.system.service.IEmailService;
import com.zhi.system.service.ISettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 留言/咨询通知：站内信必达（每个启用的后台用户一条），邮件受开关控制。
 */
class NotificationServiceImplTest
{
    private NotificationMapper notificationMapper;
    private SysUserMapper sysUserMapper;
    private ISettingService settingService;
    private IEmailService emailService;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp()
    {
        notificationMapper = mock(NotificationMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        settingService = mock(ISettingService.class);
        emailService = mock(IEmailService.class);

        service = new NotificationServiceImpl();
        ReflectionTestUtils.setField(service, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(service, "sysUserMapper", sysUserMapper);
        ReflectionTestUtils.setField(service, "settingService", settingService);
        ReflectionTestUtils.setField(service, "emailService", emailService);
    }

    @Test
    @DisplayName("留言通知写入每个启用的后台用户，并给有邮箱的用户发邮件")
    void notifyCreatesInAppNotificationForEverySystemUser() throws Exception
    {
        when(sysUserMapper.selectEnabledSystemUsers())
            .thenReturn(List.of(user(1L, "admin", "admin@example.com"), user(2L, "ops", null)));
        when(settingService.selectSettingValueByKey("email_notify_enabled")).thenReturn("true");

        int sent = service.notifyNewMessage(message(null, "访客甲", "请问企业官网报价"));

        assertEquals(2, sent);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper, org.mockito.Mockito.times(2)).insertNotification(captor.capture());
        Notification first = captor.getAllValues().get(0);
        assertEquals(1L, first.getRecipientId());
        assertEquals("message", first.getType());
        assertEquals("收到新的网站留言", first.getTitle());
        assertEquals("访客甲", first.getSenderName());
        assertEquals("请问企业官网报价", first.getContent());
        assertEquals(0, first.getIsRead());
        assertNull(first.getArticleId());
        verify(emailService).sendNotificationMail(eq("admin@example.com"), eq("收到新的网站留言"),
            org.mockito.ArgumentMatchers.contains("请问企业官网报价"));
        // 没有邮箱的用户只收站内信
        verify(emailService, never()).sendNotificationMail(eq(null), anyString(), anyString());
    }

    @Test
    @DisplayName("联系我们页的咨询表单按「咨询」命名，便于管理员区分来源")
    void notifyLabelsContactFormAsInquiry() throws Exception
    {
        when(sysUserMapper.selectEnabledSystemUsers()).thenReturn(List.of(user(1L, "admin", null)));
        when(settingService.selectSettingValueByKey("email_notify_enabled")).thenReturn("false");

        service.notifyNewMessage(message(null, "采购负责人",
            "【咨询表单】\n公司：示例科技\n电话：13800000000\n咨询类型：商务合作\n\n需要官网方案"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insertNotification(captor.capture());
        assertEquals("收到新的网站咨询", captor.getValue().getTitle());
        assertTrue(captor.getValue().getContent().startsWith("【咨询表单】"));
    }

    @Test
    @DisplayName("登录用户自己提交的留言不通知本人")
    void notifySkipsSubmitter() throws Exception
    {
        when(sysUserMapper.selectEnabledSystemUsers())
            .thenReturn(List.of(user(1L, "admin", "admin@example.com"), user(2L, "ops", null)));
        when(settingService.selectSettingValueByKey("email_notify_enabled")).thenReturn("false");

        int sent = service.notifyNewMessage(message(1L, "管理员", "我自己留的言"));

        assertEquals(1, sent);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insertNotification(captor.capture());
        assertEquals(2L, captor.getValue().getRecipientId());
        verify(emailService, never()).sendNotificationMail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("email_notify_enabled 关闭时只发站内信")
    void notifySkipsEmailWhenSwitchOff() throws Exception
    {
        when(sysUserMapper.selectEnabledSystemUsers())
            .thenReturn(List.of(user(1L, "admin", "admin@example.com")));
        when(settingService.selectSettingValueByKey("email_notify_enabled")).thenReturn("0");

        service.notifyNewMessage(message(null, "访客", "留言内容"));

        verify(notificationMapper).insertNotification(any(Notification.class));
        verify(emailService, never()).sendNotificationMail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("超长留言截断为 200 字摘要，避免撑爆通知表字段")
    void notifyTruncatesLongContent() throws Exception
    {
        when(sysUserMapper.selectEnabledSystemUsers()).thenReturn(List.of(user(1L, "admin", null)));
        when(settingService.selectSettingValueByKey("email_notify_enabled")).thenReturn("false");

        service.notifyNewMessage(message(null, "访客", "长".repeat(260)));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insertNotification(captor.capture());
        assertEquals(203, captor.getValue().getContent().length());
        assertTrue(captor.getValue().getContent().endsWith("..."));
    }

    @Test
    @DisplayName("通知失败不向调用方抛错：访客提交不能因通知服务异常而失败")
    void notifySwallowsRecipientQueryFailure() throws Exception
    {
        when(sysUserMapper.selectEnabledSystemUsers()).thenThrow(new RuntimeException("db down"));

        int sent = service.notifyNewMessage(message(null, "访客", "留言内容"));

        assertEquals(0, sent);
        verify(notificationMapper, never()).insertNotification(any(Notification.class));
    }

    private Message message(Long userId, String nickname, String content)
    {
        Message message = new Message();
        message.setId(100L);
        message.setUserId(userId);
        message.setNickname(nickname);
        message.setContent(content);
        return message;
    }

    private SysUser user(Long userId, String userName, String email)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setEmail(email);
        user.setUserType("00");
        return user;
    }
}
