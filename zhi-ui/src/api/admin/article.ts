import request from '@/utils/request'
import type {
  Article,
  ArticleImportResult,
  ArticleParams,
  ArticleRevision,
  DataResult,
  QueryResult
} from '@/types'

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
 * 获取当前用户的文章列表
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
 * 删除文章（逻辑删除，进入回收站）
 */
export function delArticle(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/article/' + ids,
    method: 'delete'
  })
}

/**
 * 从回收站恢复文章
 */
export function restoreArticle(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/article/restore/' + ids,
    method: 'put'
  })
}

/**
 * 彻底删除回收站中的文章（物理删除，不可恢复）
 */
export function purgeArticle(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/article/purge/' + ids,
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
/**
 * 查询文章的修订历史（不含正文，按版本号倒序）
 */
export function listArticleRevisions(articleId: number): Promise<DataResult<ArticleRevision[]>> {
  return request({
    url: `/system/article/revisions/${articleId}`,
    method: 'get'
  })
}

/**
 * 获取单个修订版本详情（含正文）
 */
export function getArticleRevision(revisionId: number): Promise<DataResult<ArticleRevision>> {
  return request({
    url: '/system/article/revision/' + revisionId,
    method: 'get'
  })
}

/**
 * 恢复到指定历史版本（当前内容会先保存为新版本）
 */
export function restoreArticleRevision(revisionId: number): Promise<any> {
  return request({
    url: `/system/article/revision/${revisionId}/restore`,
    method: 'post'
  })
}

/**
 * 批量导入 Markdown 文件（每个 .md 生成一篇文章）
 *
 * 以 FormData 提交；request 拦截器对 FormData 会删除 Content-Type，
 * 交给浏览器补 multipart 边界（不要手动设置 Content-Type）。
 */
export function importMarkdownArticles(data: FormData): Promise<DataResult<ArticleImportResult>> {
  return request({
    url: '/system/article/import',
    method: 'post',
    data
  })
}

/**
 * 批量更新文章状态（发布 / 草稿 / 定时）
 *
 * 只提交 ids + status：局部更新不会触碰 format / contentMd 等字段
 * （用 updateArticle 做状态切换曾把 Markdown 文章静默改成 HTML）。
 */
export function updateArticleStatus(ids: number[], status: number): Promise<any> {
  return request({
    url: '/system/article/status',
    method: 'put',
    data: { ids, status }
  })
}
