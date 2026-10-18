import request from '@/utils/request'
import type { AjaxResult, PageParams, QueryResult } from '@/types'

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

export interface MediaReference {
  referenceType: string
  referenceId: number
  referenceTitle: string
}

export interface MediaReferenceResult extends AjaxResult {
  data: MediaReference[]
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
 * 查询文件被哪些内容引用（删除前影响检查）
 */
export function getMediaReferences(id: number): Promise<MediaReferenceResult> {
  return request({
    url: '/system/media/' + id + '/references',
    method: 'get'
  })
}

/**
 * 上传文件到媒体库（后端 /common/upload 会自动写入上传记录）
 */
export function uploadMedia(file: File): Promise<any> {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: '/common/upload',
    method: 'post',
    data
  })
}

/**
 * 删除上传记录（被引用时后端会拒绝，并同时删除物理文件）
 */
export function delMedia(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/media/' + ids,
    method: 'delete'
  })
}
