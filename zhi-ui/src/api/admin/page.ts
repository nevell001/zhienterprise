import request from '@/utils/request'
import type { DataResult, OperResult, PageParams, QueryResult } from '@/types'

/**
 * 页面状态：0草稿 1已发布 2定时发布
 */
export type PageStatus = '0' | '1' | '2'

/**
 * 自定义页面
 */
export interface SitePage {
  id?: number
  title?: string
  slug?: string
  deletedSlug?: string | null
  summary?: string
  content?: string
  pageType?: string
  sections?: string | null
  status?: PageStatus
  publishTime?: string | null
  showInNav?: string
  sort?: number
  viewCount?: number
  seoTitle?: string
  seoKeywords?: string
  seoDescription?: string
  delFlag?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

/**
 * 页面查询参数
 */
export interface PageQueryParams extends PageParams {
  title?: string
  slug?: string
  status?: PageStatus
  delFlag?: string
}

/**
 * 查询页面列表
 */
export function listPage(query?: PageQueryParams): Promise<QueryResult<SitePage>> {
  return request({
    url: '/system/page/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询页面详细
 */
export function getPage(id: number): Promise<DataResult<SitePage>> {
  return request({
    url: '/system/page/' + id,
    method: 'get'
  })
}

/**
 * 新增页面
 */
export function addPage(data: Partial<SitePage>): Promise<OperResult> {
  return request({
    url: '/system/page',
    method: 'post',
    data: data
  })
}

/**
 * 修改页面
 */
export function updatePage(data: Partial<SitePage>): Promise<OperResult> {
  return request({
    url: '/system/page',
    method: 'put',
    data: data
  })
}

/**
 * 发布/下架页面（status：0草稿 1已发布）
 */
export function changePageStatus(id: number, status: PageStatus): Promise<OperResult> {
  return request({
    url: `/system/page/changeStatus/${id}/${status}`,
    method: 'put'
  })
}

/**
 * 设置页面定时发布
 */
export function schedulePage(id: number, publishTime: string): Promise<OperResult> {
  return request({
    url: `/system/page/schedule/${id}`,
    method: 'put',
    params: { publishTime }
  })
}

/**
 * 从回收站恢复页面
 */
export function restorePage(ids: number | number[]): Promise<OperResult> {
  return request({
    url: '/system/page/restore/' + ids,
    method: 'put'
  })
}

/**
 * 彻底删除回收站中的页面（物理删除，不可恢复）
 */
export function purgePage(ids: number | number[]): Promise<OperResult> {
  return request({
    url: '/system/page/purge/' + ids,
    method: 'delete'
  })
}

/**
 * 删除页面（逻辑删除，进入回收站）
 */
export function delPage(ids: number | number[]): Promise<OperResult> {
  return request({
    url: '/system/page/' + ids,
    method: 'delete'
  })
}
