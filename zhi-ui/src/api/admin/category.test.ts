import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as categoryApi from './category'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Category API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listCategory', () => {
    it('应该导出 listCategory 函数', () => {
      expect(categoryApi.listCategory).toBeDefined()
      expect(typeof categoryApi.listCategory).toBe('function')
    })

    it('应该调用分类列表接口', () => {
      categoryApi.listCategory({ pageNum: 1, pageSize: 10 })
      expect(request).toHaveBeenCalledWith({
        url: '/system/category/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })

  describe('getCategory', () => {
    it('应该导出 getCategory 函数', () => {
      expect(categoryApi.getCategory).toBeDefined()
      expect(typeof categoryApi.getCategory).toBe('function')
    })

    it('应该调用分类详情接口', () => {
      categoryApi.getCategory(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/category/123',
        method: 'get'
      })
    })
  })

  describe('addCategory', () => {
    it('应该导出 addCategory 函数', () => {
      expect(categoryApi.addCategory).toBeDefined()
      expect(typeof categoryApi.addCategory).toBe('function')
    })

    it('应该调用新增分类接口', () => {
      const categoryData = { categoryName: '测试分类', categorySort: 0 }
      categoryApi.addCategory(categoryData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/category',
        method: 'post',
        data: categoryData
      })
    })
  })

  describe('updateCategory', () => {
    it('应该导出 updateCategory 函数', () => {
      expect(categoryApi.updateCategory).toBeDefined()
      expect(typeof categoryApi.updateCategory).toBe('function')
    })

    it('应该调用修改分类接口', () => {
      const categoryData = { categoryId: 123, categoryName: '更新的分类' }
      categoryApi.updateCategory(categoryData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/category',
        method: 'put',
        data: categoryData
      })
    })
  })

  describe('delCategory', () => {
    it('应该导出 delCategory 函数', () => {
      expect(categoryApi.delCategory).toBeDefined()
      expect(typeof categoryApi.delCategory).toBe('function')
    })

    it('应该调用删除分类接口', () => {
      categoryApi.delCategory(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/category/123',
        method: 'delete'
      })
    })

    it('应该支持批量删除', () => {
      categoryApi.delCategory([1, 2, 3])
      expect(request).toHaveBeenCalledWith({
        url: '/system/category/1,2,3',
        method: 'delete'
      })
    })
  })
})
