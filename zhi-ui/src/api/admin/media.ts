import request from '@/utils/request'
import type { PageParams, QueryResult } from '@/types'

export interface MediaItem {
  id: number
  fileName?: string
  originalName?: string
  url?: string
  mimeType?: string
  fileSize?: number
  uploadType?: string
  createBy?: string
  createTime?: string
}

/**
 * 查询上传记录列表
 */
export function listMedia(query?: PageParams): Promise<QueryResult<MediaItem>> {
  return request({
    url: '/system/media/list',
    method: 'get',
    params: query
  })
}

/**
 * 删除上传记录（同时尝试删除物理文件）
 */
export function delMedia(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/media/' + ids,
    method: 'delete'
  })
}
