import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getPublishedPages, getPublishedPageBySlug } from './page'
import type { SitePage } from './page'
import request from '@/utils/request'

// Mock request module
vi.mock('@/utils/request')
const mockRequest = vi.mocked(request)

const samplePage: SitePage = {
  id: 1,
  title: '关于本站',
  slug: 'about-site',
  summary: '站点介绍',
  content: '# 关于本站',
  status: '1',
  showInNav: '1',
  sort: 1,
  viewCount: 3,
  seoTitle: '关于本站 - 知企建站系统',
  seoKeywords: '知企建站系统,关于',
  seoDescription: '站点介绍',
  updateTime: '2026-09-10 12:00:00'
}

describe('自定义页面 API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getPublishedPages', () => {
    it('应该导出 getPublishedPages 函数', () => {
      expect(getPublishedPages).toBeDefined()
      expect(typeof getPublishedPages).toBe('function')
    })

    it('应该调用 GET /blog/page/list', async () => {
      mockRequest.mockResolvedValue({ code: 200, msg: 'ok', data: [samplePage] })

      const result = await getPublishedPages()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/page/list',
        method: 'get',
        headers: { isToken: false }
      })
      expect(result.data).toEqual([samplePage])
    })
  })

  describe('getPublishedPageBySlug', () => {
    it('应该导出 getPublishedPageBySlug 函数', () => {
      expect(getPublishedPageBySlug).toBeDefined()
      expect(typeof getPublishedPageBySlug).toBe('function')
    })

    it('应该调用 GET /blog/page/:slug', async () => {
      mockRequest.mockResolvedValue({ code: 200, msg: 'ok', data: samplePage })

      const result = await getPublishedPageBySlug('about-site')

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/page/about-site',
        method: 'get',
        headers: { isToken: false }
      })
      expect(result.data.slug).toBe('about-site')
    })

    it('slug 应该做 URL 编码', async () => {
      mockRequest.mockResolvedValue({ code: 200, msg: 'ok', data: samplePage })

      await getPublishedPageBySlug('a b')

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/api/page/a%20b',
        method: 'get',
        headers: { isToken: false }
      })
    })
  })
})
