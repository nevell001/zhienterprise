import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as friendLinkApi from './friendLink'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Friend Link API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listFriendLink', () => {
    it('应该导出 listFriendLink 函数', () => {
      expect(friendLinkApi.listFriendLink).toBeDefined()
      expect(typeof friendLinkApi.listFriendLink).toBe('function')
    })

    it('应该调用友链列表接口', () => {
      friendLinkApi.listFriendLink({ pageNum: 1, pageSize: 10 })
      expect(request).toHaveBeenCalledWith({
        url: '/system/friendLink/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })

  describe('getFriendLink', () => {
    it('应该导出 getFriendLink 函数', () => {
      expect(friendLinkApi.getFriendLink).toBeDefined()
      expect(typeof friendLinkApi.getFriendLink).toBe('function')
    })

    it('应该调用友链详情接口', () => {
      friendLinkApi.getFriendLink(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/friendLink/123',
        method: 'get'
      })
    })
  })

  describe('addFriendLink', () => {
    it('应该导出 addFriendLink 函数', () => {
      expect(friendLinkApi.addFriendLink).toBeDefined()
      expect(typeof friendLinkApi.addFriendLink).toBe('function')
    })

    it('应该调用新增友链接口', () => {
      const friendLinkData = { linkName: '测试友链', linkUrl: 'http://example.com' }
      friendLinkApi.addFriendLink(friendLinkData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/friendLink',
        method: 'post',
        data: friendLinkData
      })
    })
  })

  describe('updateFriendLink', () => {
    it('应该导出 updateFriendLink 函数', () => {
      expect(friendLinkApi.updateFriendLink).toBeDefined()
      expect(typeof friendLinkApi.updateFriendLink).toBe('function')
    })

    it('应该调用修改友链接口', () => {
      const friendLinkData = { id: 123, linkName: '更新的友链' }
      friendLinkApi.updateFriendLink(friendLinkData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/friendLink',
        method: 'put',
        data: friendLinkData
      })
    })
  })

  describe('delFriendLink', () => {
    it('应该导出 delFriendLink 函数', () => {
      expect(friendLinkApi.delFriendLink).toBeDefined()
      expect(typeof friendLinkApi.delFriendLink).toBe('function')
    })

    it('应该调用删除友链接口', () => {
      friendLinkApi.delFriendLink(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/friendLink/123',
        method: 'delete'
      })
    })

    it('应该支持批量删除', () => {
      friendLinkApi.delFriendLink([1, 2, 3])
      expect(request).toHaveBeenCalledWith({
        url: '/system/friendLink/1,2,3',
        method: 'delete'
      })
    })
  })
})
