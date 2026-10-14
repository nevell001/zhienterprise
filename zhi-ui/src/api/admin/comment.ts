import request from '@/utils/request'
import type { Comment, PageParams, QueryResult } from '@/types'

/**
 * 查询评论列表
 */
export function listComment(query?: PageParams): Promise<QueryResult<Comment>> {
  return request({
    url: '/system/comment/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询评论详细
 */
export function getComment(id: number): Promise<Comment> {
  return request({
    url: '/system/comment/' + id,
    method: 'get'
  })
}

/**
 * 新增评论
 */
export function addComment(data: Comment): Promise<any> {
  return request({
    url: '/system/comment',
    method: 'post',
    data: data
  })
}

/**
 * 修改评论
 */
export function updateComment(data: Comment): Promise<any> {
  return request({
    url: '/system/comment',
    method: 'put',
    data: data
  })
}

/**
 * 删除评论
 */
export function delComment(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/comment/' + ids,
    method: 'delete'
  })
}

/**
 * 审核通过评论
 */
export function auditComment(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/comment/audit/' + ids,
    method: 'put'
  })
}

/**
 * 审核拒绝评论
 */
export function rejectComment(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/comment/reject/' + ids,
    method: 'put'
  })
}
