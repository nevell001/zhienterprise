package com.zhi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.system.domain.Notification;
import com.zhi.system.mapper.NotificationMapper;
import com.zhi.system.service.INotificationService;

/**
 * 站内信通知Service业务层处理
 *
 * @author nevell
 * @date 2026-07-26
 */
@Service
public class NotificationServiceImpl implements INotificationService
{
    @Autowired
    private NotificationMapper notificationMapper;

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
