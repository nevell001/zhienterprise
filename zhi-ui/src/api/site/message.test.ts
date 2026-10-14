import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getMessageList, getMessageCount, addMessage } from './message'
import type { SiteMessage, SiteMessageForm } from './message'
import request from '@/utils/request'

// Mock request module
vi.mock('@/utils/request')
const mockRequest = vi.mocked(request)

describe('留言板 API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getMessageList', () => {
    it('应该导出 getMessageList 函数', () => {
      expect(getMessageList).toBeDefined()
      expect(typeof getMessageList).toBe('function')
    })

    it('应该调用 GET /blog/message/list 并透传分页参数', async () => {
      mockRequest.mockResolvedValue({ code: 200, msg: 'ok', rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await getMessageList(query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/message/list',
        method: 'get',
        params: query,
        headers: { isToken: false }
      })
    })

    it('应该返回留言列表数据', async () => {
      const rows: SiteMessage[] = [
        {
          id: 1,
          nickname: '访客',
          website: 'https://example.com',
          content: '你好',
          replyContent: '谢谢',
          replyTime: '2026-09-10 12:00:00',
          createTime: '2026-09-10 11:00:00'
        }
      ]
      mockRequest.mockResolvedValue({ code: 200, msg: 'ok', rows, total: 1 })

      const result = await getMessageList()

      expect(result.rows).toEqual(rows)
      expect(result.total).toBe(1)
    })
  })

  describe('getMessageCount', () => {
    it('应该导出 getMessageCount 函数', () => {
      expect(getMessageCount).toBeDefined()
      expect(typeof getMessageCount).toBe('function')
    })

    it('应该调用 GET /blog/message/count', async () => {
      mockRequest.mockResolvedValue({ code: 200, msg: 'ok', data: 8 })

      const result = await getMessageCount()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/message/count',
        method: 'get',
        headers: { isToken: false }
      })
      expect(result.data).toBe(8)
    })
  })

  describe('addMessage', () => {
    it('应该导出 addMessage 函数', () => {
      expect(addMessage).toBeDefined()
      expect(typeof addMessage).toBe('function')
    })

    it('应该调用 POST /blog/message', async () => {
      mockRequest.mockResolvedValue({ code: 200, msg: '操作成功' })

      const data: SiteMessageForm = {
        nickname: '访客',
        email: 'guest@example.com',
        website: 'https://example.com',
        content: '这是一条留言'
      }
      await addMessage(data)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/blog/message',
        method: 'post',
        data,
        headers: { isToken: false }
      })
    })
  })
})
