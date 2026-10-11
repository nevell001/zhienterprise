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
export function delComment(id: number): Promise<any> {
  return request({
    url: '/system/comment/' + id,
    method: 'delete'
  })
}

/**
 * 获取文章评论列表（前台用：顶级评论分页，回复随楼层返回；sort 支持 newest/oldest）
 */
export function getArticleComments(
  articleId: number,
  query?: PageParams & { sort?: 'newest' | 'oldest' }
): Promise<any> {
  return request({
    url: '/blog/comment/article/' + articleId,
    method: 'get',
    params: query
  })
}

/**
 * 添加评论（前台用）
 */
export function addBlogComment(data: Comment): Promise<any> {
  return request({
    url: '/blog/comment',
    method: 'post',
    data: data
  })
}

/**
 * 修改本人评论（前台用，需登录 + 服务端归属校验）
 */
export function updateMyComment(id: number, data: { content: string }): Promise<any> {
  return request({
    url: '/blog/comment/' + id,
    method: 'put',
    data: data
  })
}

/**
 * 删除本人评论（前台用，需登录 + 服务端归属校验）
 */
export function deleteMyComment(id: number): Promise<any> {
  return request({
    url: '/blog/comment/' + id,
    method: 'delete'
  })
}

/**
 * 审核评论
 */
export function auditComment(id: number): Promise<any> {
  return request({
    url: '/system/comment/audit/' + id,
    method: 'put'
  })
}

/**
 * 驳回评论
 */
export function rejectComment(id: number): Promise<any> {
  return request({
    url: '/system/comment/reject/' + id,
    method: 'put'
  })
}
