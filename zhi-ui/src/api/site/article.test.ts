import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as articleApi from './article'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Site Article API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })
  describe('getArticleList', () => {
    it('应该导出 getArticleList 函数', () => {
      expect(articleApi.getArticleList).toBeDefined()
      expect(typeof articleApi.getArticleList).toBe('function')
    })

    it('应该调用文章列表接口', () => {
      articleApi.getArticleList({ pageNum: 1, pageSize: 10 })

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 },
        headers: { isToken: false }
      })
    })
  })
  describe('getHotArticles', () => {
    it('应该导出 getHotArticles 函数', () => {
      expect(articleApi.getHotArticles).toBeDefined()
      expect(typeof articleApi.getHotArticles).toBe('function')
    })

    it('应该调用热门文章接口并设置默认 pageSize', () => {
      articleApi.getHotArticles({ pageNum: 1 })

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article/hot',
        method: 'get',
        params: { pageNum: 1, pageSize: 5 },
        headers: { isToken: false }
      })
    })

    it('应该使用自定义 pageSize', () => {
      articleApi.getHotArticles({ pageNum: 1, pageSize: 10 })

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article/hot',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 },
        headers: { isToken: false }
      })
    })
  })
  describe('getArticleDetail', () => {
    it('应该导出 getArticleDetail 函数', () => {
      expect(articleApi.getArticleDetail).toBeDefined()
      expect(typeof articleApi.getArticleDetail).toBe('function')
    })

    it('应该调用文章详情接口', () => {
      articleApi.getArticleDetail(123)

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article/123',
        method: 'get',
        headers: { isToken: false }
      })
    })
  })

  describe('updateArticleViewCount', () => {
    it('应该导出 updateArticleViewCount 函数', () => {
      expect(articleApi.updateArticleViewCount).toBeDefined()
      expect(typeof articleApi.updateArticleViewCount).toBe('function')
    })

    it('应该调用更新浏览量接口', () => {
      articleApi.updateArticleViewCount(123)

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article/view/123',
        method: 'post',
        headers: { isToken: false }
      })
    })
  })

  describe('getArticleArchive', () => {
    it('应该导出 getArticleArchive 函数', () => {
      expect(articleApi.getArticleArchive).toBeDefined()
      expect(typeof articleApi.getArticleArchive).toBe('function')
    })

    it('应该调用文章归档接口', () => {
      articleApi.getArticleArchive()

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article-archive',
        method: 'get',
        headers: { isToken: false }
      })
    })
  })

  describe('getArticlesByArchive', () => {
    it('应该导出 getArticlesByArchive 函数', () => {
      expect(articleApi.getArticlesByArchive).toBeDefined()
      expect(typeof articleApi.getArticlesByArchive).toBe('function')
    })

    it('应该根据归档月份获取文章列表', () => {
      articleApi.getArticlesByArchive(2024, 3, { pageNum: 1, pageSize: 10 })

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article/archive-month/2024/3',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 },
        headers: { isToken: false }
      })
    })
  })

  describe('searchArticles', () => {
    it('应该导出 searchArticles 函数', () => {
      expect(articleApi.searchArticles).toBeDefined()
      expect(typeof articleApi.searchArticles).toBe('function')
    })

    it('应该调用搜索文章接口', () => {
      articleApi.searchArticles('测试', { pageNum: 1, pageSize: 10 })

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article/search',
        method: 'get',
        params: { pageNum: 1, pageSize: 10, keyword: '测试' },
        headers: { isToken: false }
      })
    })
  })

  describe('getRelatedArticles', () => {
    it('应该导出 getRelatedArticles 函数', () => {
      expect(articleApi.getRelatedArticles).toBeDefined()
      expect(typeof articleApi.getRelatedArticles).toBe('function')
    })

    it('应该调用相关文章接口', () => {
      articleApi.getRelatedArticles(123)

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/article/related/123',
        method: 'get',
        headers: { isToken: false }
      })
    })
  })

  describe('submitComment', () => {
    it('应该导出 submitComment 函数', () => {
      expect(articleApi.submitComment).toBeDefined()
      expect(typeof articleApi.submitComment).toBe('function')
    })

    it('应该调用提交评论接口', () => {
      const commentData = { articleId: 123, content: '测试评论' }
      articleApi.submitComment(commentData)

      expect(request).toHaveBeenCalledWith({
        url: '/blog/api/comment',
        method: 'post',
        data: commentData
      })
    })
  })
})
