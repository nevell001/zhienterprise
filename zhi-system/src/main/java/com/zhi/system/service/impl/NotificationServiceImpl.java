package com.zhi.system.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.utils.SiteSwitchUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.system.domain.Message;
import com.zhi.system.domain.Notification;
import com.zhi.system.mapper.NotificationMapper;
import com.zhi.system.mapper.SysUserMapper;
import com.zhi.system.service.IEmailService;
import com.zhi.system.service.INotificationService;
import com.zhi.system.service.ISettingService;

/**
 * 站内信通知Service业务层处理
 *
 * @author nevell
 * @date 2026-07-26
 */
@Service
public class NotificationServiceImpl implements INotificationService
{
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    /** 联系页咨询表单在正文首部写入的标记（与前端 contact/index.vue 同源） */
    private static final String CONTACT_FORM_PREFIX = "【咨询表单】";

    /** 通知正文摘要长度（与评论通知同口径） */
    private static final int CONTENT_PREVIEW_LENGTH = 200;

    private static final String KEY_EMAIL_NOTIFY_ENABLED = "email_notify_enabled";

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ISettingService settingService;

    @Autowired
    private IEmailService emailService;

    @Override
    public List<Notification> selectNotificationList(Notification notification)
    {
        return notificationMapper.selectNotificationList(notification);
    }

    @Override
    public int createNotification(Notification notification)
    {
        return notificationMapper.insertNotification(notification);
    }

    @Override
    public int notifyNewMessage(Message message)
    {
        if (message == null)
        {
            return 0;
        }
        try
        {
            List<SysUser> recipients = sysUserMapper.selectEnabledSystemUsers();
            if (recipients == null || recipients.isEmpty())
            {
                return 0;
            }

            boolean inquiry = message.getContent() != null
                    && message.getContent().startsWith(CONTACT_FORM_PREFIX);
            String label = inquiry ? "咨询" : "留言";
            String title = "收到新的网站" + label;
            String senderName = StringUtils.isEmpty(message.getNickname())
                    ? "匿名访客"
                    : message.getNickname();
            String preview = message.getContent() == null ? "" : message.getContent().trim();
            if (preview.length() > CONTENT_PREVIEW_LENGTH)
            {
                preview = preview.substring(0, CONTENT_PREVIEW_LENGTH) + "...";
            }
            boolean emailEnabled = !SiteSwitchUtils.isOff(
                    settingService.selectSettingValueByKey(KEY_EMAIL_NOTIFY_ENABLED));

            int sent = 0;
            for (SysUser user : recipients)
            {
                if (user == null || user.getUserId() == null)
                {
                    continue;
                }
                // 登录用户自己提交的留言不通知本人
                if (message.getUserId() != null && message.getUserId().equals(user.getUserId()))
                {
                    continue;
                }
                try
                {
                    Notification notification = new Notification();
                    notification.setRecipientId(user.getUserId());
                    notification.setSenderName(senderName);
                    notification.setType("message");
                    notification.setTitle(title);
                    notification.setContent(preview);
                    notification.setIsRead(0);
                    notificationMapper.insertNotification(notification);
                    sent++;

                    if (emailEnabled && StringUtils.isNotEmpty(user.getEmail()))
                    {
                        emailService.sendNotificationMail(user.getEmail(), title,
                                senderName + " 提交了新的网站" + label + "：\n" + preview);
                    }
                }
                catch (Exception e)
                {
                    // 单个接收人失败不影响其他管理员收到通知
                    logger.warn("留言通知发送失败: messageId={}, recipientId={}, error={}",
                            message.getId(), user.getUserId(), e.getMessage());
                }
            }
            logger.info("留言通知已发送: messageId={}, type={}, recipients={}",
                    message.getId(), inquiry ? "inquiry" : "guestbook", sent);
            return sent;
        }
        catch (Exception e)
        {
            // 通知失败不能让访客的提交回滚
            logger.error("留言通知发送异常: messageId={}", message.getId(), e);
            return 0;
        }
    }

    @Override
    public int markAsRead(Long[] ids, Long recipientId)
    {
        return notificationMapper.markAsRead(ids, recipientId);
    }

    @Override
    public int markAllAsRead(Long recipientId)
    {
        return notificationMapper.markAllAsRead(recipientId);
    }

    @Override
    public int selectUnreadCount(Long recipientId)
    {
        return notificationMapper.selectUnreadCount(recipientId);
    }
}
