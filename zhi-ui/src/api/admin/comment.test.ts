import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as commentApi from './comment'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Comment API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listComment', () => {
    it('应该导出 listComment 函数', () => {
      expect(commentApi.listComment).toBeDefined()
      expect(typeof commentApi.listComment).toBe('function')
    })

    it('应该调用评论列表接口', () => {
      commentApi.listComment({ pageNum: 1, pageSize: 10 })
      expect(request).toHaveBeenCalledWith({
        url: '/system/comment/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })

  describe('getComment', () => {
    it('应该导出 getComment 函数', () => {
      expect(commentApi.getComment).toBeDefined()
      expect(typeof commentApi.getComment).toBe('function')
    })

    it('应该调用评论详情接口', () => {
      commentApi.getComment(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/comment/123',
        method: 'get'
      })
    })
  })

  describe('addComment', () => {
    it('应该导出 addComment 函数', () => {
      expect(commentApi.addComment).toBeDefined()
      expect(typeof commentApi.addComment).toBe('function')
    })

    it('应该调用新增评论接口', () => {
      const commentData = { content: '测试评论', articleId: 1 }
      commentApi.addComment(commentData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/comment',
        method: 'post',
        data: commentData
      })
    })
  })

  describe('updateComment', () => {
    it('应该导出 updateComment 函数', () => {
      expect(commentApi.updateComment).toBeDefined()
      expect(typeof commentApi.updateComment).toBe('function')
    })

    it('应该调用修改评论接口', () => {
      const commentData = { commentId: 123, content: '更新的评论' }
      commentApi.updateComment(commentData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/comment',
        method: 'put',
        data: commentData
      })
    })
  })

  describe('delComment', () => {
    it('应该导出 delComment 函数', () => {
      expect(commentApi.delComment).toBeDefined()
      expect(typeof commentApi.delComment).toBe('function')
    })

    it('应该调用删除评论接口', () => {
      commentApi.delComment(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/comment/123',
        method: 'delete'
      })
    })

    it('应该支持批量删除', () => {
      commentApi.delComment([1, 2, 3])
      expect(request).toHaveBeenCalledWith({
        url: '/system/comment/1,2,3',
        method: 'delete'
      })
    })
  })

  describe('auditComment', () => {
    it('应该导出 auditComment 函数', () => {
      expect(commentApi.auditComment).toBeDefined()
      expect(typeof commentApi.auditComment).toBe('function')
    })

    it('应该调用审核通过接口', () => {
      commentApi.auditComment([1, 2])
      expect(request).toHaveBeenCalledWith({
        url: '/system/comment/audit/1,2',
        method: 'put'
      })
    })
  })

  describe('rejectComment', () => {
    it('应该导出 rejectComment 函数', () => {
      expect(commentApi.rejectComment).toBeDefined()
      expect(typeof commentApi.rejectComment).toBe('function')
    })

    it('应该调用审核拒绝接口', () => {
      commentApi.rejectComment([1, 2])
      expect(request).toHaveBeenCalledWith({
        url: '/system/comment/reject/1,2',
        method: 'put'
      })
    })
  })
})
