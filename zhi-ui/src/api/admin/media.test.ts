import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import { listMedia, getMediaReferences, uploadMedia, delMedia } from './media'

vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ rows: [], total: 0 }))
}))

describe('媒体库 API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('查询上传记录列表', () => {
    listMedia({ pageNum: 1, pageSize: 10 })
    expect(request).toHaveBeenCalledWith({
      url: '/system/media/list',
      method: 'get',
      params: { pageNum: 1, pageSize: 10 }
    })
  })

  it('查询文件引用位置', () => {
    getMediaReferences(7)
    expect(request).toHaveBeenCalledWith({
      url: '/system/media/7/references',
      method: 'get'
    })
  })

  it('上传文件使用 /common/upload 并携带 FormData', () => {
    const file = new File(['x'], 'logo.png', { type: 'image/png' })
    uploadMedia(file)

    const config = (request as unknown as ReturnType<typeof vi.fn>).mock.calls[0][0]
    expect(config.url).toBe('/common/upload')
    expect(config.method).toBe('post')
    expect(config.data).toBeInstanceOf(FormData)
    expect(config.data.get('file')).toBe(file)
  })

  it('删除上传记录支持单个与批量', () => {
    delMedia(3)
    expect(request).toHaveBeenCalledWith({ url: '/system/media/3', method: 'delete' })
    delMedia([1, 2, 3])
    expect(request).toHaveBeenCalledWith({ url: '/system/media/1,2,3', method: 'delete' })
  })
})
