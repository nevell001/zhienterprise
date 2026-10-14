import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getTagList, getTagDetail, getTagCloud, getArticlesByTag } from './tag'
import type { Tag } from '@/types'
import request from '@/utils/request'

// Mock request module
vi.mock('@/utils/request')
const mockRequest = vi.mocked(request)

describe('Tag API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getTagList', () => {
    it('应该导出 getTagList 函数', () => {
      expect(getTagList).toBeDefined()
      expect(typeof getTagList).toBe('function')
    })

    it('应该调用 GET /blog/tag/list 并设置 isToken: false', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await getTagList(query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/tag/list',
        method: 'get',
        params: query,
        headers: { isToken: false }
      })
    })

    it('应该在没有参数时仍然调用 API', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      await getTagList()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/tag/list',
        method: 'get',
        params: undefined,
        headers: { isToken: false }
      })
    })
  })

  describe('getTagDetail', () => {
    it('应该导出 getTagDetail 函数', () => {
      expect(getTagDetail).toBeDefined()
      expect(typeof getTagDetail).toBe('function')
    })

    it('应该调用 GET /blog/api/tag/:id 并设置 isToken: false', async () => {
      const tagData: Tag = {
        id: 1,
        tagName: 'JavaScript',
        articleCount: 5,
        createTime: '',
        updateTime: ''
      }
      mockRequest.mockResolvedValue(tagData)

      await getTagDetail(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/tag/1',
        method: 'get',
        headers: { isToken: false }
      })
    })

    it('应该处理不同的 ID', async () => {
      const tagData: Tag = {
        id: 99,
        tagName: 'Vue',
        articleCount: 10,
        createTime: '',
        updateTime: ''
      }
      mockRequest.mockResolvedValue(tagData)

      await getTagDetail(99)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/tag/99',
        method: 'get',
        headers: { isToken: false }
      })
    })
  })

  describe('getTagCloud', () => {
    it('应该导出 getTagCloud 函数', () => {
      expect(getTagCloud).toBeDefined()
      expect(typeof getTagCloud).toBe('function')
    })

    it('应该调用 GET /blog/tag/cloud 并设置 isToken: false', async () => {
      const tagCloud: Tag[] = [
        { id: 1, tagName: 'JavaScript', articleCount: 10, createTime: '', updateTime: '' },
        { id: 2, tagName: 'Vue', articleCount: 8, createTime: '', updateTime: '' }
      ]
      mockRequest.mockResolvedValue(tagCloud)

      const result = await getTagCloud()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/tag/cloud',
        method: 'get',
        headers: { isToken: false }
      })
      expect(result).toEqual(tagCloud)
    })
  })

  describe('getArticlesByTag', () => {
    it('应该导出 getArticlesByTag 函数', () => {
      expect(getArticlesByTag).toBeDefined()
      expect(typeof getArticlesByTag).toBe('function')
    })

    it('应该调用 GET /blog/api/article/tag/:tagId 并设置 isToken: false', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await getArticlesByTag(1, query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/article/tag/1',
        method: 'get',
        params: query,
        headers: { isToken: false }
      })
    })

    it('应该处理不同的标签 ID', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await getArticlesByTag(99, query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/article/tag/99',
        method: 'get',
        params: query,
        headers: { isToken: false }
      })
    })

    it('应该在没有查询参数时仍然调用 API', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      await getArticlesByTag(5)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/article/tag/5',
        method: 'get',
        params: undefined,
        headers: { isToken: false }
      })
    })
  })
})
