package com.zhi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.zhi.system.domain.Notification;

/**
 * 站内信通知Mapper接口
 *
 * @author nevell
 * @date 2026-07-26
 */
public interface NotificationMapper
{
    /**
     * 查询站内信通知列表
     *
     * @param notification 站内信通知
     * @return 站内信通知集合
     */
    List<Notification> selectNotificationList(Notification notification);

    /**
     * 插入站内信通知
     *
     * @param notification 站内信通知
     * @return 结果
     */
    int insertNotification(Notification notification);

    /**
     * 标记为已读（批量）
     *
     * @param ids 通知ID数组
     * @param recipientId 接收用户ID
     * @return 结果
     */
    int markAsRead(@Param("ids") Long[] ids, @Param("recipientId") Long recipientId);

    /**
     * 标记所有为已读
     *
     * @param recipientId 接收用户ID
     * @return 结果
     */
    int markAllAsRead(@Param("recipientId") Long recipientId);

    /**
     * 获取未读数量
     *
     * @param recipientId 接收用户ID
     * @return 未读数量
     */
    int selectUnreadCount(@Param("recipientId") Long recipientId);
}
