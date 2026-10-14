import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  listCategory,
  getCategory,
  addCategory,
  updateCategory,
  delCategory,
  getCategoryList,
  getCategoryDetail
} from './category'
import type { Category } from '@/types'
import request from '@/utils/request'

// Mock request module
vi.mock('@/utils/request')
const mockRequest = vi.mocked(request)

describe('Category API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listCategory', () => {
    it('应该导出 listCategory 函数', () => {
      expect(listCategory).toBeDefined()
      expect(typeof listCategory).toBe('function')
    })

    it('应该调用 GET /system/category/list', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await listCategory(query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/category/list',
        method: 'get',
        params: query
      })
    })

    it('应该在没有参数时仍然调用 API', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      await listCategory()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/category/list',
        method: 'get',
        params: undefined
      })
    })
  })

  describe('getCategory', () => {
    it('应该导出 getCategory 函数', () => {
      expect(getCategory).toBeDefined()
      expect(typeof getCategory).toBe('function')
    })

    it('应该调用 GET /system/category/:id', async () => {
      mockRequest.mockResolvedValue({ id: 1, name: '测试分类' })

      await getCategory(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/category/1',
        method: 'get'
      })
    })

    it('应该正确传递不同的 ID', async () => {
      mockRequest.mockResolvedValue({ id: 99, name: '测试分类' })

      await getCategory(99)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/category/99',
        method: 'get'
      })
    })
  })

  describe('addCategory', () => {
    it('应该导出 addCategory 函数', () => {
      expect(addCategory).toBeDefined()
      expect(typeof addCategory).toBe('function')
    })

    it('应该调用 POST /system/category', async () => {
      const categoryData: Category = {
        id: 0,
        categoryName: '新分类',
        categoryDesc: '分类描述',
        parentId: 0,
        articleCount: 0,
        orderNum: 0,
        createTime: '',
        updateTime: ''
      }
      mockRequest.mockResolvedValue({ code: 200 })

      await addCategory(categoryData)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/category',
        method: 'post',
        data: categoryData
      })
    })
  })

  describe('updateCategory', () => {
    it('应该导出 updateCategory 函数', () => {
      expect(updateCategory).toBeDefined()
      expect(typeof updateCategory).toBe('function')
    })

    it('应该调用 PUT /system/category', async () => {
      const categoryData: Category = {
        id: 1,
        categoryName: '更新分类',
        categoryDesc: '更新描述',
        parentId: 0,
        articleCount: 5,
        orderNum: 1,
        createTime: '',
        updateTime: ''
      }
      mockRequest.mockResolvedValue({ code: 200 })

      await updateCategory(categoryData)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/category',
        method: 'put',
        data: categoryData
      })
    })
  })

  describe('delCategory', () => {
    it('应该导出 delCategory 函数', () => {
      expect(delCategory).toBeDefined()
      expect(typeof delCategory).toBe('function')
    })

    it('应该调用 DELETE /system/category/:id', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await delCategory(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/category/1',
        method: 'delete'
      })
    })

    it('应该处理不同的 ID', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await delCategory(100)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/category/100',
        method: 'delete'
      })
    })
  })

  describe('getCategoryList', () => {
    it('应该导出 getCategoryList 函数', () => {
      expect(getCategoryList).toBeDefined()
      expect(typeof getCategoryList).toBe('function')
    })

    it('应该调用 GET /blog/category/list 并设置 isToken: false', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await getCategoryList(query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/category/list',
        method: 'get',
        params: query,
        headers: { isToken: false }
      })
    })

    it('应该在没有参数时仍然调用 API', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      await getCategoryList()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/category/list',
        method: 'get',
        params: undefined,
        headers: { isToken: false }
      })
    })
  })

  describe('getCategoryDetail', () => {
    it('应该导出 getCategoryDetail 函数', () => {
      expect(getCategoryDetail).toBeDefined()
      expect(typeof getCategoryDetail).toBe('function')
    })

    it('应该调用 GET /blog/category/:id 并设置 isToken: false', async () => {
      mockRequest.mockResolvedValue({ id: 1, categoryName: '测试' })

      await getCategoryDetail(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/category/1',
        method: 'get',
        headers: { isToken: false }
      })
    })

    it('应该处理不同的 ID', async () => {
      mockRequest.mockResolvedValue({ id: 123, categoryName: '测试' })

      await getCategoryDetail(123)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/api/category/123',
        method: 'get',
        headers: { isToken: false }
      })
    })
  })
})
