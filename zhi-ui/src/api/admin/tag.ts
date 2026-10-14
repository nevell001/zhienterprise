import request from '@/utils/request'
import type { DataResult, Tag, TagParams, QueryResult } from '@/types'

/**
 * 查询标签列表
 */
export function listTag(query?: TagParams): Promise<QueryResult<Tag>> {
  return request({
    url: '/system/tag/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询标签详细
 */
export function getTag(tagId: number): Promise<DataResult<Tag>> {
  return request({
    url: '/system/tag/' + tagId,
    method: 'get'
  })
}

/**
 * 新增标签
 */
export function addTag(data: Partial<Tag> | Record<string, any>): Promise<any> {
  return request({
    url: '/system/tag',
    method: 'post',
    data: data
  })
}

/**
 * 修改标签
 */
export function updateTag(data: Partial<Tag> | Record<string, any>): Promise<any> {
  return request({
    url: '/system/tag',
    method: 'put',
    data: data
  })
}

/**
 * 删除标签
 */
export function delTag(tagIds: number | number[]): Promise<any> {
  return request({
    url: '/system/tag/' + tagIds,
    method: 'delete'
  })
}
