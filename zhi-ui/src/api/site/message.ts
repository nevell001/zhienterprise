import request from '@/utils/request'
import type { AjaxResult, PageParams, TableDataInfo } from '@/types'

/**
 * 公开留言（前台留言板）
 *
 * 说明：接口只返回已发布留言，响应中不包含 email / ip 字段，
 * 前端也不应展示邮箱等隐私信息。
 */
export interface SiteMessage {
  id: number
  userId?: number | null
  nickname: string
  website?: string | null
  content: string
  replyContent?: string | null
  replyTime?: string | null
  createTime?: string
}

/** 留言提交参数（昵称、内容必填，邮箱与网站选填；验证码启用时需带 code/uuid） */
export interface SiteMessageForm {
  nickname: string
  email?: string
  website?: string
  content: string
  code?: string
  uuid?: string
}

/** 已发布留言总数响应 */
export interface SiteMessageCountResult {
  code: number
  msg: string
  data: number
}

/**
 * 查询留言列表（仅已发布，支持分页）
 */
export function getMessageList(query?: PageParams): Promise<TableDataInfo<SiteMessage>> {
  return request({
    url: '/blog/message/list',
    method: 'get',
    params: query,
    headers: { isToken: false }
  })
}

/**
 * 查询已发布留言总数
 */
export function getMessageCount(): Promise<SiteMessageCountResult> {
  return request({
    url: '/blog/message/count',
    method: 'get',
    headers: { isToken: false }
  })
}

/**
 * 提交留言（匿名可提交，是否需审核由 comment_review 开关决定）
 */
export function addMessage(data: SiteMessageForm): Promise<AjaxResult> {
  return request({
    url: '/blog/message',
    method: 'post',
    data,
    headers: { isToken: false }
  })
}
