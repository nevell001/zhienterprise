import request from '@/utils/request'

/**
 * 切换评论点赞（登录后可用；返回 { liked, likeCount }）
 */
export function toggleCommentLike(commentId: number): Promise<any> {
  return request({
    url: `/api/like/comment/${commentId}`,
    method: 'post'
  })
}

/**
 * 批量查询用户已点赞的评论ID集合（用于回显点赞态）
 */
export function getCommentLikedStatuses(commentIds: number[]): Promise<any> {
  return request({
    url: '/api/like/comments/status',
    method: 'get',
    params: { commentIds: commentIds.join(',') }
  })
}
