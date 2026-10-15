import request from '@/utils/request'
import type { DataResult, OperResult, PageParams, QueryResult } from '@/types'

/**
 * 页面状态：0草稿 1已发布
 */
export type PageStatus = '0' | '1'

/**
 * 自定义页面
 */
export interface SitePage {
  id?: number
  title?: string
  slug?: string
  summary?: string
  content?: string
  pageType?: string
  sections?: string | null
  status?: PageStatus
  showInNav?: string
  sort?: number
  viewCount?: number
  seoTitle?: string
  seoKeywords?: string
  seoDescription?: string
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
 * 删除页面
 */
export function delPage(ids: number | number[]): Promise<OperResult> {
  return request({
    url: '/system/page/' + ids,
    method: 'delete'
  })
}
