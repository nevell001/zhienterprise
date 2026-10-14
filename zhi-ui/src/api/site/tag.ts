import request from '@/utils/request'
import type { Tag, PageParams, QueryResult } from '@/types'

/**
 * 获取标签列表（前台用）
 */
export function getTagList(query?: PageParams): Promise<QueryResult<Tag>> {
  return request({
    url: '/blog/api/tag/list',
    method: 'get',
    params: query,
    headers: { isToken: false }
  })
}

/**
 * 获取标签详情（前台用）
 */
export function getTagDetail(id: number): Promise<Tag & { data?: Tag }> {
  return request({
    url: '/blog/api/tag/' + id,
    method: 'get',
    headers: { isToken: false }
  })
}

/**
 * 获取标签云
 */
export function getTagCloud(): Promise<Tag[] & { data?: Tag[] }> {
  return request({
    url: '/blog/api/tag/cloud',
    method: 'get',
    headers: { isToken: false }
  })
}

/**
 * 根据标签获取文章列表
 */
export function getArticlesByTag(tagId: number, query?: PageParams): Promise<any> {
  return request({
    url: '/blog/api/article/tag/' + tagId,
    method: 'get',
    params: query,
    headers: { isToken: false }
  })
}
