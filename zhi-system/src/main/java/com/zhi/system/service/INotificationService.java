package com.zhi.system.service;

import java.util.List;
import com.zhi.system.domain.Message;
import com.zhi.system.domain.Notification;

/**
 * 站内信通知Service接口
 *
 * @author nevell
 * @date 2026-07-26
 */
public interface INotificationService
{
    /**
     * 查询站内信通知列表
     *
     * @param notification 站内信通知
     * @return 站内信通知集合
     */
    List<Notification> selectNotificationList(Notification notification);

    /**
     * 创建通知
     *
     * @param notification 通知对象
     * @return 结果
     */
    int createNotification(Notification notification);

    /**
     * 新留言/咨询到达时通知后台系统用户（站内信；邮件受 email_notify_enabled 开关控制）
     *
     * @param message 已落库的留言
     * @return 成功创建的站内信数量
     */
    int notifyNewMessage(Message message);

    /**
     * 标记为已读（批量）
     *
     * @param ids 通知ID数组
     * @param recipientId 接收用户ID
     * @return 结果
     */
    int markAsRead(Long[] ids, Long recipientId);

    /**
     * 标记所有为已读
     *
     * @param recipientId 接收用户ID
     * @return 结果
     */
    int markAllAsRead(Long recipientId);

    /**
     * 获取未读数量
     *
     * @param recipientId 接收用户ID
     * @return 未读数量
     */
    int selectUnreadCount(Long recipientId);
}
