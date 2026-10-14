import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as pageApi from './page'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Page API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listPage', () => {
    it('应该导出 listPage 函数', () => {
      expect(pageApi.listPage).toBeDefined()
      expect(typeof pageApi.listPage).toBe('function')
    })

    it('应该调用页面列表接口', () => {
      pageApi.listPage({ pageNum: 1, pageSize: 10, title: '关于', status: '1' })
      expect(request).toHaveBeenCalledWith({
        url: '/system/page/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10, title: '关于', status: '1' }
      })
    })
  })

  describe('getPage', () => {
    it('应该导出 getPage 函数', () => {
      expect(pageApi.getPage).toBeDefined()
      expect(typeof pageApi.getPage).toBe('function')
    })

    it('应该调用页面详情接口', () => {
      pageApi.getPage(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/page/123',
        method: 'get'
      })
    })
  })

  describe('addPage', () => {
    it('应该导出 addPage 函数', () => {
      expect(pageApi.addPage).toBeDefined()
      expect(typeof pageApi.addPage).toBe('function')
    })

    it('应该调用新增页面接口', () => {
      const pageData = { title: '关于我们', slug: 'about-us', content: '# 关于' }
      pageApi.addPage(pageData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/page',
        method: 'post',
        data: pageData
      })
    })
  })

  describe('updatePage', () => {
    it('应该导出 updatePage 函数', () => {
      expect(pageApi.updatePage).toBeDefined()
      expect(typeof pageApi.updatePage).toBe('function')
    })

    it('应该调用修改页面接口', () => {
      const pageData = { id: 123, title: '关于我们（新）' }
      pageApi.updatePage(pageData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/page',
        method: 'put',
        data: pageData
      })
    })
  })

  describe('changePageStatus', () => {
    it('应该导出 changePageStatus 函数', () => {
      expect(pageApi.changePageStatus).toBeDefined()
      expect(typeof pageApi.changePageStatus).toBe('function')
    })

    it('应该调用发布页面接口', () => {
      pageApi.changePageStatus(123, '1')
      expect(request).toHaveBeenCalledWith({
        url: '/system/page/changeStatus/123/1',
        method: 'put'
      })
    })

    it('应该调用下架页面接口', () => {
      pageApi.changePageStatus(456, '0')
      expect(request).toHaveBeenCalledWith({
        url: '/system/page/changeStatus/456/0',
        method: 'put'
      })
    })
  })

  describe('delPage', () => {
    it('应该导出 delPage 函数', () => {
      expect(pageApi.delPage).toBeDefined()
      expect(typeof pageApi.delPage).toBe('function')
    })

    it('应该调用删除页面接口', () => {
      pageApi.delPage(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/page/123',
        method: 'delete'
      })
    })

    it('应该支持批量删除', () => {
      pageApi.delPage([1, 2, 3])
      expect(request).toHaveBeenCalledWith({
        url: '/system/page/1,2,3',
        method: 'delete'
      })
    })
  })
})
