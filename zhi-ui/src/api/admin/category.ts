import request from '@/utils/request'
import type { Category, DataResult, PageParams, QueryResult } from '@/types'

/**
 * 查询分类列表
 */
export function listCategory(query?: PageParams): Promise<QueryResult<Category>> {
  return request({
    url: '/system/category/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询分类详细
 */
export function getCategory(id: number): Promise<DataResult<Category>> {
  return request({
    url: '/system/category/' + id,
    method: 'get'
  })
}

/**
 * 新增分类
 */
export function addCategory(data: Partial<Category> | Record<string, any>): Promise<any> {
  return request({
    url: '/system/category',
    method: 'post',
    data: data
  })
}

/**
 * 修改分类
 */
export function updateCategory(data: Partial<Category> | Record<string, any>): Promise<any> {
  return request({
    url: '/system/category',
    method: 'put',
    data: data
  })
}

/**
 * 删除分类
 */
export function delCategory(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/category/' + ids,
    method: 'delete'
  })
}
