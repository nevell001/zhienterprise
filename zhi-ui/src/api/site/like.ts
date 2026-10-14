import request from '@/utils/request'

/**
 * 切换文章点赞（登录后可用；返回 { liked, likeCount }）
 */
export function toggleArticleLike(articleId: number): Promise<any> {
  return request({
    url: `/blog/like/article/${articleId}`,
    method: 'post'
  })
}

/**
 * 查询文章点赞状态（登录后可用；返回 { liked, likeCount }）
 */
export function getArticleLikeStatus(articleId: number): Promise<any> {
  return request({
    url: `/blog/like/article/${articleId}/status`,
    method: 'get'
  })
}

/**
 * 切换评论点赞（登录后可用；返回 { liked, likeCount }）
 */
export function toggleCommentLike(commentId: number): Promise<any> {
  return request({
    url: `/blog/like/comment/${commentId}`,
    method: 'post'
  })
}

/**
 * 批量查询用户已点赞的评论ID集合（用于回显点赞态）
 */
export function getCommentLikedStatuses(commentIds: number[]): Promise<any> {
  return request({
    url: '/blog/like/comments/status',
    method: 'get',
    params: { commentIds: commentIds.join(',') }
  })
}
