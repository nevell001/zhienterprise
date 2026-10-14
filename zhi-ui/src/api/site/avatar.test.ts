import { describe, it, expect, vi, beforeEach } from 'vitest'
import { processAvatarUrl, uploadAvatar, checkAvatarExists } from './avatar'

// Mock request module
vi.mock('@/utils/request', () => ({
  default: vi.fn(() =>
    Promise.resolve({ code: 200, url: '/uploads/avatar.jpg', fileName: 'avatar.jpg' })
  )
}))

describe('Avatar API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn(() => Promise.resolve({ ok: true } as Response)) as any
  })

  describe('processAvatarUrl', () => {
    it('应该导出 processAvatarUrl 函数', () => {
      expect(processAvatarUrl).toBeDefined()
      expect(typeof processAvatarUrl).toBe('function')
    })

    it('应该处理空头像URL', () => {
      expect(processAvatarUrl('')).toBe('')
      expect(processAvatarUrl(null as any)).toBe('')
      expect(processAvatarUrl(undefined as any)).toBe('')
    })

    it('应该处理空白字符串', () => {
      expect(processAvatarUrl('   ')).toBe('')
    })

    it('应该返回完整的 HTTP/HTTPS URL', () => {
      expect(processAvatarUrl('http://example.com/avatar.jpg')).toBe(
        'http://example.com/avatar.jpg'
      )
      expect(processAvatarUrl('https://example.com/avatar.jpg')).toBe(
        'https://example.com/avatar.jpg'
      )
    })

    it('应该为相对路径添加基础路径', () => {
      expect(processAvatarUrl('/uploads/avatar.jpg')).toBe('/dev-api/uploads/avatar.jpg')
    })

    it('应该为非 / 开头的路径添加 / 前缀', () => {
      expect(processAvatarUrl('uploads/avatar.jpg')).toBe('/dev-api/uploads/avatar.jpg')
    })
  })
  describe('uploadAvatar', () => {
    it('应该导出 uploadAvatar 函数', () => {
      expect(uploadAvatar).toBeDefined()
      expect(typeof uploadAvatar).toBe('function')
      expect(uploadAvatar.length).toBeGreaterThan(0)
    })

    it('应该返回成功结果', async () => {
      const file = new File(['test'], 'avatar.jpg', { type: 'image/jpeg' })
      const result = await uploadAvatar(file)

      expect(result.success).toBe(true)
      expect(result.data).toBeDefined()
      expect(result.data?.url).toBeDefined()
    })

    it('应该接受上传进度回调选项', () => {
      const file = new File(['test'], 'avatar.jpg', { type: 'image/jpeg' })
      const onProgress = vi.fn()

      // 验证函数可以接受onProgress参数
      expect(() => uploadAvatar(file, { onProgress })).not.toThrow()
    })
  })

  describe('checkAvatarExists', () => {
    it('应该导出 checkAvatarExists 函数', () => {
      expect(checkAvatarExists).toBeDefined()
      expect(typeof checkAvatarExists).toBe('function')
    })

    it('应该为空URL返回 true', async () => {
      const result = await checkAvatarExists('')
      expect(result).toBe(true)
    })

    it('应该为默认头像返回 true', async () => {
      const result = await checkAvatarExists('data:image/svg+xml;base64,test')
      expect(result).toBe(true)
    })

    it('应该检查外部头像是否存在', async () => {
      ;(global.fetch as any).mockResolvedValueOnce({ ok: true })
      const result = await checkAvatarExists('http://example.com/avatar.jpg')
      expect(result).toBe(true)
      expect(global.fetch).toHaveBeenCalledWith('http://example.com/avatar.jpg', { method: 'HEAD' })
    })

    it('应该在检查失败时返回 false', async () => {
      ;(global.fetch as any).mockRejectedValueOnce(new Error('Network error'))
      const result = await checkAvatarExists('http://example.com/avatar.jpg')
      expect(result).toBe(false)
    })
  })
})
