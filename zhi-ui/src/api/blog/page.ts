import request from '@/utils/request'
import type { AjaxResult } from '@/types'

/**
 * 自定义页面（前台公开接口）
 *
 * 注意：status / showInNav 是字符串 '0' / '1'。
 */
export interface BlogPage {
  id: number
  title: string
  slug: string
  summary?: string | null
  content?: string | null
  status?: string
  showInNav?: string
  sort?: number | null
  viewCount?: number | null
  seoTitle?: string | null
  seoKeywords?: string | null
  seoDescription?: string | null
  updateTime?: string | null
}

/** 已发布页面列表响应 */
export interface BlogPageListResult extends AjaxResult {
  data: BlogPage[]
}

/** 页面详情响应 */
export interface BlogPageDetailResult extends AjaxResult {
  data: BlogPage
}

/**
 * 查询已发布页面列表（用于导航与页面索引）
 */
export function getPublishedPages(): Promise<BlogPageListResult> {
  return request({
    url: '/blog/page/list',
    method: 'get',
    headers: { isToken: false }
  })
}

/**
 * 按 slug 查询已发布页面详情
 *
 * 页面不存在或未发布时后端返回 code != 200 与消息「页面不存在或未发布」，
 * 请求封装会 reject，调用方需要捕获并展示友好提示。
 * 可选 slug（如 about/honors 这类站点自带页面 slug 可能尚未在后台建页）传
 * config.silent 抑制请求封装的兜底 toast，由调用方自行降级为默认文案。
 */
export function getPublishedPageBySlug(
  slug: string,
  config?: { silent?: boolean }
): Promise<BlogPageDetailResult> {
  return request({
    url: '/blog/page/' + encodeURIComponent(slug),
    method: 'get',
    headers: { isToken: false },
    ...config
  })
}
