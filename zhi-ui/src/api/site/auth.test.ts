import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as authApi from './auth'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Site Auth API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('siteRegister', () => {
    it('应该导出 siteRegister 函数', () => {
      expect(authApi.siteRegister).toBeDefined()
      expect(typeof authApi.siteRegister).toBe('function')
    })

    it('应该调用博客注册接口', () => {
      const registerData = {
        username: 'test',
        password: 'password',
        confirmPassword: 'password',
        email: 'test@example.com',
        emailCode: '123456'
      }
      authApi.siteRegister(registerData)
      expect(request).toHaveBeenCalledWith({
        url: '/blog/auth/register',
        headers: {
          isToken: false,
          repeatSubmit: false
        },
        method: 'post',
        data: registerData
      })
    })
  })

  describe('sendRegisterCode', () => {
    it('应该导出 sendRegisterCode 函数', () => {
      expect(authApi.sendRegisterCode).toBeDefined()
      expect(typeof authApi.sendRegisterCode).toBe('function')
    })

    it('应该调用发送注册验证码接口', () => {
      authApi.sendRegisterCode('test@example.com')
      expect(request).toHaveBeenCalledWith({
        url: '/blog/auth/send-register-code',
        headers: {
          isToken: false,
          repeatSubmit: false
        },
        method: 'post',
        data: { email: 'test@example.com' }
      })
    })
  })

  describe('sendResetCode', () => {
    it('应该导出 sendResetCode 函数', () => {
      expect(authApi.sendResetCode).toBeDefined()
      expect(typeof authApi.sendResetCode).toBe('function')
    })

    it('应该调用发送密码重置验证码接口', () => {
      authApi.sendResetCode('test@example.com')
      expect(request).toHaveBeenCalledWith({
        url: '/blog/auth/send-reset-code',
        headers: {
          isToken: false,
          repeatSubmit: false
        },
        method: 'post',
        data: { email: 'test@example.com' }
      })
    })
  })

  describe('resetPassword', () => {
    it('应该导出 resetPassword 函数', () => {
      expect(authApi.resetPassword).toBeDefined()
      expect(typeof authApi.resetPassword).toBe('function')
    })

    it('应该调用重置密码接口', () => {
      const resetData = {
        email: 'test@example.com',
        code: '123456',
        newPassword: 'newPassword',
        confirmPassword: 'newPassword'
      }
      authApi.resetPassword(resetData)
      expect(request).toHaveBeenCalledWith({
        url: '/blog/auth/reset-password',
        headers: {
          isToken: false,
          repeatSubmit: false
        },
        method: 'post',
        data: resetData
      })
    })
  })

  describe('getCodeImg', () => {
    it('应该导出 getCodeImg 函数', () => {
      expect(authApi.getCodeImg).toBeDefined()
      expect(typeof authApi.getCodeImg).toBe('function')
    })

    it('应该调用获取验证码图片接口', () => {
      authApi.getCodeImg()
      expect(request).toHaveBeenCalledWith({
        url: '/captchaImage',
        headers: {
          isToken: false
        },
        method: 'get',
        timeout: 20000
      })
    })
  })
})
