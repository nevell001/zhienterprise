import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as tagApi from './tag'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Tag API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listTag', () => {
    it('应该导出 listTag 函数', () => {
      expect(tagApi.listTag).toBeDefined()
      expect(typeof tagApi.listTag).toBe('function')
    })

    it('应该调用标签列表接口', () => {
      tagApi.listTag({ pageNum: 1, pageSize: 10 })
      expect(request).toHaveBeenCalledWith({
        url: '/system/tag/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })

  describe('getTag', () => {
    it('应该导出 getTag 函数', () => {
      expect(tagApi.getTag).toBeDefined()
      expect(typeof tagApi.getTag).toBe('function')
    })

    it('应该调用标签详情接口', () => {
      tagApi.getTag(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/tag/123',
        method: 'get'
      })
    })
  })

  describe('addTag', () => {
    it('应该导出 addTag 函数', () => {
      expect(tagApi.addTag).toBeDefined()
      expect(typeof tagApi.addTag).toBe('function')
    })

    it('应该调用新增标签接口', () => {
      const tagData = { tagName: '测试标签', tagSort: 0 }
      tagApi.addTag(tagData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/tag',
        method: 'post',
        data: tagData
      })
    })
  })

  describe('updateTag', () => {
    it('应该导出 updateTag 函数', () => {
      expect(tagApi.updateTag).toBeDefined()
      expect(typeof tagApi.updateTag).toBe('function')
    })

    it('应该调用修改标签接口', () => {
      const tagData = { tagId: 123, tagName: '更新的标签' }
      tagApi.updateTag(tagData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/tag',
        method: 'put',
        data: tagData
      })
    })
  })

  describe('delTag', () => {
    it('应该导出 delTag 函数', () => {
      expect(tagApi.delTag).toBeDefined()
      expect(typeof tagApi.delTag).toBe('function')
    })

    it('应该调用删除标签接口', () => {
      tagApi.delTag(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/tag/123',
        method: 'delete'
      })
    })
  })
})
