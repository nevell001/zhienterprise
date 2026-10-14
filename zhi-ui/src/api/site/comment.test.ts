import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  listComment,
  getComment,
  addComment,
  updateComment,
  delComment,
  getArticleComments,
  addSiteComment,
  auditComment,
  rejectComment
} from './comment'
import type { Comment } from '@/types'
import request from '@/utils/request'

// Mock request module
vi.mock('@/utils/request')
const mockRequest = vi.mocked(request)

describe('Comment API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listComment', () => {
    it('应该导出 listComment 函数', () => {
      expect(listComment).toBeDefined()
      expect(typeof listComment).toBe('function')
    })

    it('应该调用 GET /system/comment/list', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await listComment(query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/comment/list',
        method: 'get',
        params: query
      })
    })
  })

  describe('getComment', () => {
    it('应该导出 getComment 函数', () => {
      expect(getComment).toBeDefined()
      expect(typeof getComment).toBe('function')
    })

    it('应该调用 GET /system/comment/:id', async () => {
      const comment: Comment = {
        id: 1,
        content: '测试评论',
        articleId: 1,
        createTime: ''
      }
      mockRequest.mockResolvedValue(comment)

      await getComment(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/comment/1',
        method: 'get'
      })
    })
  })

  describe('addComment', () => {
    it('应该导出 addComment 函数', () => {
      expect(addComment).toBeDefined()
      expect(typeof addComment).toBe('function')
    })

    it('应该调用 POST /system/comment', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const comment: Comment = {
        id: 0,
        content: '新评论',
        articleId: 1,
        createTime: ''
      }
      await addComment(comment)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/comment',
        method: 'post',
        data: comment
      })
    })
  })

  describe('updateComment', () => {
    it('应该导出 updateComment 函数', () => {
      expect(updateComment).toBeDefined()
      expect(typeof updateComment).toBe('function')
    })

    it('应该调用 PUT /system/comment', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const comment: Comment = {
        id: 1,
        content: '更新评论',
        articleId: 1,
        createTime: ''
      }
      await updateComment(comment)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/comment',
        method: 'put',
        data: comment
      })
    })
  })

  describe('delComment', () => {
    it('应该导出 delComment 函数', () => {
      expect(delComment).toBeDefined()
      expect(typeof delComment).toBe('function')
    })

    it('应该调用 DELETE /system/comment/:id', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await delComment(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/comment/1',
        method: 'delete'
      })
    })
  })

  describe('getArticleComments', () => {
    it('应该导出 getArticleComments 函数', () => {
      expect(getArticleComments).toBeDefined()
      expect(typeof getArticleComments).toBe('function')
    })

    it('应该调用 GET /blog/comment/article/:articleId', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      await getArticleComments(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/comment/article/1',
        method: 'get',
        params: undefined
      })
    })

    it('应该支持分页参数', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 20 }
      await getArticleComments(1, query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/comment/article/1',
        method: 'get',
        params: query
      })
    })
  })

  describe('addSiteComment', () => {
    it('应该导出 addSiteComment 函数', () => {
      expect(addSiteComment).toBeDefined()
      expect(typeof addSiteComment).toBe('function')
    })

    it('应该调用 POST /blog/comment', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const comment: Comment = {
        id: 0,
        content: '新评论',
        articleId: 1,
        createTime: ''
      }
      await addSiteComment(comment)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/comment',
        method: 'post',
        data: comment
      })
    })
  })

  describe('auditComment', () => {
    it('应该导出 auditComment 函数', () => {
      expect(auditComment).toBeDefined()
      expect(typeof auditComment).toBe('function')
    })

    it('应该调用 PUT /system/comment/audit/:id', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await auditComment(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/comment/audit/1',
        method: 'put'
      })
    })
  })

  describe('rejectComment', () => {
    it('应该导出 rejectComment 函数', () => {
      expect(rejectComment).toBeDefined()
      expect(typeof rejectComment).toBe('function')
    })

    it('应该调用 PUT /system/comment/reject/:id', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await rejectComment(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/comment/reject/1',
        method: 'put'
      })
    })
  })
})
