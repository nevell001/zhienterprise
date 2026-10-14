import request from '@/utils/request'
import type { QueryResult } from '@/types'

export interface SiteNotification {
  id: number
  recipientId: number
  senderName: string
  type: 'comment' | 'reply' | 'audit' | 'reject'
  title: string
  content: string
  articleId: number
  articleTitle: string
  commentId: number
  isRead: number
  createTime: string
}

export interface NotificationListParams {
  pageNum?: number
  pageSize?: number
  type?: string
  isRead?: number
}

/**
 * 获取当前用户的通知列表
 */
export function getNotificationList(
  params?: NotificationListParams
): Promise<QueryResult<SiteNotification>> {
  return request({
    url: '/api/notification/list',
    method: 'get',
    params
  })
}

/**
 * 获取未读通知数量
 */
export function getUnreadCount(): Promise<any> {
  return request({
    url: '/api/notification/unread-count',
    method: 'get'
  })
}

/**
 * 批量标记通知为已读
 */
export function markAsRead(ids: number[]) {
  return request({
    url: `/api/notification/read/${ids.join(',')}`,
    method: 'put'
  })
}

/**
 * 标记所有通知为已读
 */
export function markAllAsRead() {
  return request({
    url: '/api/notification/read-all',
    method: 'put'
  })
}
