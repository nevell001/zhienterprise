import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as messageApi from './message'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Message API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listMessage', () => {
    it('应该导出 listMessage 函数', () => {
      expect(messageApi.listMessage).toBeDefined()
      expect(typeof messageApi.listMessage).toBe('function')
    })

    it('应该调用留言列表接口', () => {
      messageApi.listMessage({ pageNum: 1, pageSize: 10, nickname: '张三', status: '0' })
      expect(request).toHaveBeenCalledWith({
        url: '/system/message/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10, nickname: '张三', status: '0' }
      })
    })
  })
  describe('auditMessage', () => {
    it('应该导出 auditMessage 函数', () => {
      expect(messageApi.auditMessage).toBeDefined()
      expect(typeof messageApi.auditMessage).toBe('function')
    })

    it('应该调用留言审核通过接口', () => {
      messageApi.auditMessage(123, '1')
      expect(request).toHaveBeenCalledWith({
        url: '/system/message/audit/123/1',
        method: 'put'
      })
    })

    it('应该调用留言审核拒绝接口', () => {
      messageApi.auditMessage(456, '2')
      expect(request).toHaveBeenCalledWith({
        url: '/system/message/audit/456/2',
        method: 'put'
      })
    })
  })

  describe('replyMessage', () => {
    it('应该导出 replyMessage 函数', () => {
      expect(messageApi.replyMessage).toBeDefined()
      expect(typeof messageApi.replyMessage).toBe('function')
    })

    it('应该调用留言回复接口并携带回复内容', () => {
      messageApi.replyMessage(123, '感谢留言')
      expect(request).toHaveBeenCalledWith({
        url: '/system/message/reply/123',
        method: 'put',
        data: { replyContent: '感谢留言' }
      })
    })
  })

  describe('delMessage', () => {
    it('应该导出 delMessage 函数', () => {
      expect(messageApi.delMessage).toBeDefined()
      expect(typeof messageApi.delMessage).toBe('function')
    })

    it('应该调用删除留言接口', () => {
      messageApi.delMessage(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/message/123',
        method: 'delete'
      })
    })

    it('应该支持批量删除', () => {
      messageApi.delMessage([1, 2, 3])
      expect(request).toHaveBeenCalledWith({
        url: '/system/message/1,2,3',
        method: 'delete'
      })
    })
  })
})
