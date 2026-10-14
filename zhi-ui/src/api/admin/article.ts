import request from '@/utils/request'
import type { Article, ArticleParams, DataResult, QueryResult } from '@/types'

/**
 * 查询文章列表
 */
export function listArticle(query?: ArticleParams): Promise<QueryResult<Article>> {
  return request({
    url: '/system/article/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取当前用户的文章列表（用于个人中心）
 */
export function getMyArticles(query?: ArticleParams): Promise<QueryResult<Article>> {
  return request({
    url: '/system/article/my',
    method: 'get',
    params: query
  })
}

/**
 * 查询文章详细
 */
export function getArticle(id: number): Promise<DataResult<Article>> {
  return request({
    url: '/system/article/' + id,
    method: 'get'
  })
}

/**
 * 新增文章
 */
export function addArticle(data: Partial<Article> | Record<string, any>): Promise<any> {
  return request({
    url: '/system/article',
    method: 'post',
    data: data
  })
}

/**
 * 修改文章
 */
export function updateArticle(data: Partial<Article> | Record<string, any>): Promise<any> {
  return request({
    url: '/system/article',
    method: 'put',
    data: data
  })
}

/**
 * 删除文章
 */
export function delArticle(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/article/' + ids,
    method: 'delete'
  })
}

/**
 * 更新文章浏览量
 */
export function updateArticleViewCount(id: number): Promise<any> {
  return request({
    url: '/system/article/view/' + id,
    method: 'put'
  })
}

/**
 * 批量更新文章置顶状态
 */
export function updateArticleTopStatus(
  ids: number[] | { ids: number[]; isTop: number },
  isTop = 1
): Promise<any> {
  const data = Array.isArray(ids) ? { ids, isTop } : ids
  return request({
    url: '/system/article/top',
    method: 'put',
    data
  })
}

/**
 * 批量更新文章推荐状态
 */
export function updateArticleRecommendStatus(
  ids: number[] | { ids: number[]; isRecommend: number },
  isRecommend = 1
): Promise<any> {
  const data = Array.isArray(ids) ? { ids, isRecommend } : ids
  return request({
    url: '/system/article/recommend',
    method: 'put',
    data
  })
}
