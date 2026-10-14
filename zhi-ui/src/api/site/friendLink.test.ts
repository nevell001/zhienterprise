import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  listFriendLink,
  getFriendLink,
  addFriendLink,
  updateFriendLink,
  delFriendLink,
  getFrontFriendLinkList,
  applyFriendLink
} from './friendLink'
import type { FriendLinkApplyForm } from './friendLink'
import type { FriendLink } from '@/types'
import request from '@/utils/request'

// Mock request module
vi.mock('@/utils/request')
const mockRequest = vi.mocked(request)

describe('Friend Link API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listFriendLink', () => {
    it('应该导出 listFriendLink 函数', () => {
      expect(listFriendLink).toBeDefined()
      expect(typeof listFriendLink).toBe('function')
    })

    it('应该调用 GET /system/friendLink/list', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await listFriendLink(query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/friendLink/list',
        method: 'get',
        params: query
      })
    })
  })

  describe('getFriendLink', () => {
    it('应该导出 getFriendLink 函数', () => {
      expect(getFriendLink).toBeDefined()
      expect(typeof getFriendLink).toBe('function')
    })

    it('应该调用 GET /system/friendLink/:id', async () => {
      const friendLink: FriendLink = {
        id: 1,
        name: '测试友链',
        url: 'https://example.com',
        logo: '',
        orderNum: 0,
        createTime: '',
        updateTime: ''
      }
      mockRequest.mockResolvedValue(friendLink)

      await getFriendLink(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/friendLink/1',
        method: 'get'
      })
    })
  })

  describe('addFriendLink', () => {
    it('应该导出 addFriendLink 函数', () => {
      expect(addFriendLink).toBeDefined()
      expect(typeof addFriendLink).toBe('function')
    })

    it('应该调用 POST /system/friendLink', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const friendLink: FriendLink = {
        id: 0,
        name: '新友链',
        url: 'https://newlink.com',
        logo: '',
        orderNum: 0,
        createTime: '',
        updateTime: ''
      }
      await addFriendLink(friendLink)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/friendLink',
        method: 'post',
        data: friendLink
      })
    })
  })

  describe('updateFriendLink', () => {
    it('应该导出 updateFriendLink 函数', () => {
      expect(updateFriendLink).toBeDefined()
      expect(typeof updateFriendLink).toBe('function')
    })

    it('应该调用 PUT /system/friendLink', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const friendLink: FriendLink = {
        id: 1,
        name: '更新友链',
        url: 'https://updated.com',
        logo: '',
        orderNum: 1,
        createTime: '',
        updateTime: ''
      }
      await updateFriendLink(friendLink)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/friendLink',
        method: 'put',
        data: friendLink
      })
    })
  })

  describe('delFriendLink', () => {
    it('应该导出 delFriendLink 函数', () => {
      expect(delFriendLink).toBeDefined()
      expect(typeof delFriendLink).toBe('function')
    })

    it('应该调用 DELETE /system/friendLink/:id', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await delFriendLink(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/friendLink/1',
        method: 'delete'
      })
    })
  })

  describe('getFrontFriendLinkList', () => {
    it('应该导出 getFrontFriendLinkList 函数', () => {
      expect(getFrontFriendLinkList).toBeDefined()
      expect(typeof getFrontFriendLinkList).toBe('function')
    })

    it('应该调用 GET /system/friendLink/front/list', async () => {
      const friendLinks: FriendLink[] = [
        {
          id: 1,
          name: '友链1',
          url: 'https://link1.com',
          logo: '',
          orderNum: 0,
          createTime: '',
          updateTime: ''
        }
      ]
      mockRequest.mockResolvedValue(friendLinks)

      const result = await getFrontFriendLinkList()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/friendLink/front/list',
        method: 'get',
        headers: { isToken: false }
      })
      expect(result).toEqual(friendLinks)
    })
  })

  describe('applyFriendLink', () => {
    it('应该导出 applyFriendLink 函数', () => {
      expect(applyFriendLink).toBeDefined()
      expect(typeof applyFriendLink).toBe('function')
    })

    it('应该调用 POST /system/friendLink/apply 并携带 code 与 uuid', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const payload: FriendLinkApplyForm = {
        name: '测试站点',
        url: 'https://apply.example.com',
        email: 'apply@example.com',
        description: '一句话介绍',
        code: '1234',
        uuid: 'uuid-abc-123'
      }
      await applyFriendLink(payload)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/friendLink/apply',
        method: 'post',
        data: payload,
        headers: { isToken: false }
      })
    })

    it('未开启验证码时应该允许不传 code 与 uuid', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const payload: FriendLinkApplyForm = {
        name: '无验证码站点',
        url: 'https://nocaptcha.example.com'
      }
      await applyFriendLink(payload)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/friendLink/apply',
        method: 'post',
        data: payload,
        headers: { isToken: false }
      })
    })
  })
})
