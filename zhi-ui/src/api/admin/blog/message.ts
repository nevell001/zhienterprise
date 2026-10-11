import request from '@/utils/request'
import type { DataResult, OperResult, PageParams, QueryResult } from '@/types'

/**
 * 留言状态：0待审核 1已发布 2已拒绝
 */
export type MessageStatus = '0' | '1' | '2'

/**
 * 留言审核状态：1通过 2拒绝
 */
export type MessageAuditStatus = '1' | '2'

/**
 * 留言
 */
export interface BlogMessage {
  id?: number
  userId?: number
  nickname?: string
  email?: string
  website?: string
  content?: string
  replyContent?: string
  replyTime?: string
  replyBy?: string
  status?: MessageStatus
  ip?: string
  userAgent?: string
  createTime?: string
  updateTime?: string
}

/**
 * 留言查询参数
 */
export interface MessageQueryParams extends PageParams {
  nickname?: string
  content?: string
  status?: MessageStatus
}

/**
 * 查询留言列表
 */
export function listMessage(query?: MessageQueryParams): Promise<QueryResult<BlogMessage>> {
  return request({
    url: '/system/message/list',
    method: 'get',
    params: query
  })
}

/**
 * 审核留言（status：1通过 2拒绝）
 */
export function auditMessage(id: number, status: MessageAuditStatus): Promise<OperResult> {
  return request({
    url: `/system/message/audit/${id}/${status}`,
    method: 'put'
  })
}

/**
 * 回复留言（回复即自动置为已发布）
 */
export function replyMessage(id: number, replyContent: string): Promise<OperResult> {
  return request({
    url: `/system/message/reply/${id}`,
    method: 'put',
    data: { replyContent }
  })
}

/**
 * 删除留言
 */
export function delMessage(ids: number | number[]): Promise<OperResult> {
  return request({
    url: '/system/message/' + ids,
    method: 'delete'
  })
}
