import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as articleApi from './article'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Article API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listArticle', () => {
    it('应该导出 listArticle 函数', () => {
      expect(articleApi.listArticle).toBeDefined()
      expect(typeof articleApi.listArticle).toBe('function')
    })

    it('应该调用文章列表接口', () => {
      articleApi.listArticle({ pageNum: 1, pageSize: 10 })
      expect(request).toHaveBeenCalledWith({
        url: '/system/article/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })

  describe('getArticle', () => {
    it('应该导出 getArticle 函数', () => {
      expect(articleApi.getArticle).toBeDefined()
      expect(typeof articleApi.getArticle).toBe('function')
    })

    it('应该调用文章详情接口', () => {
      articleApi.getArticle(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/article/123',
        method: 'get'
      })
    })
  })

  describe('addArticle', () => {
    it('应该导出 addArticle 函数', () => {
      expect(articleApi.addArticle).toBeDefined()
      expect(typeof articleApi.addArticle).toBe('function')
    })

    it('应该调用新增文章接口', () => {
      const articleData = { title: '测试文章', content: '内容' }
      articleApi.addArticle(articleData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/article',
        method: 'post',
        data: articleData
      })
    })
  })

  describe('updateArticle', () => {
    it('应该导出 updateArticle 函数', () => {
      expect(articleApi.updateArticle).toBeDefined()
      expect(typeof articleApi.updateArticle).toBe('function')
    })

    it('应该调用修改文章接口', () => {
      const articleData = { articleId: 123, title: '更新的文章' }
      articleApi.updateArticle(articleData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/article',
        method: 'put',
        data: articleData
      })
    })
  })

  describe('delArticle', () => {
    it('应该导出 delArticle 函数', () => {
      expect(articleApi.delArticle).toBeDefined()
      expect(typeof articleApi.delArticle).toBe('function')
    })

    it('应该调用删除文章接口', () => {
      articleApi.delArticle(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/article/123',
        method: 'delete'
      })
    })

    it('应该支持批量删除', () => {
      articleApi.delArticle([1, 2, 3])
      expect(request).toHaveBeenCalledWith({
        url: '/system/article/1,2,3',
        method: 'delete'
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
        url: '/system/article/view/123',
        method: 'put'
      })
    })
  })
  describe('updateArticleTopStatus', () => {
    it('应该导出 updateArticleTopStatus 函数', () => {
      expect(articleApi.updateArticleTopStatus).toBeDefined()
      expect(typeof articleApi.updateArticleTopStatus).toBe('function')
    })

    it('应该调用更新置顶状态接口', () => {
      articleApi.updateArticleTopStatus([1, 2], 1)
      expect(request).toHaveBeenCalledWith({
        url: '/system/article/top',
        method: 'put',
        data: { ids: [1, 2], isTop: 1 }
      })
    })
  })

  describe('updateArticleRecommendStatus', () => {
    it('应该导出 updateArticleRecommendStatus 函数', () => {
      expect(articleApi.updateArticleRecommendStatus).toBeDefined()
      expect(typeof articleApi.updateArticleRecommendStatus).toBe('function')
    })

    it('应该调用更新推荐状态接口', () => {
      articleApi.updateArticleRecommendStatus([1, 2], 1)
      expect(request).toHaveBeenCalledWith({
        url: '/system/article/recommend',
        method: 'put',
        data: { ids: [1, 2], isRecommend: 1 }
      })
    })
  })
})
