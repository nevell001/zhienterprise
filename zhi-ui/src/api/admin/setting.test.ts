import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import * as settingApi from './setting'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

describe('Site Setting API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listSetting', () => {
    it('应该导出 listSetting 函数', () => {
      expect(settingApi.listSetting).toBeDefined()
      expect(typeof settingApi.listSetting).toBe('function')
    })

    it('应该调用博客设置列表接口', () => {
      settingApi.listSetting({ pageNum: 1, pageSize: 10 })
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })

  describe('getSetting', () => {
    it('应该导出 getSetting 函数', () => {
      expect(settingApi.getSetting).toBeDefined()
      expect(typeof settingApi.getSetting).toBe('function')
    })

    it('应该调用博客设置详情接口', () => {
      settingApi.getSetting(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting/123',
        method: 'get'
      })
    })
  })

  describe('getConfigByKey', () => {
    it('应该导出 getConfigByKey 函数', () => {
      expect(settingApi.getConfigByKey).toBeDefined()
      expect(typeof settingApi.getConfigByKey).toBe('function')
    })

    it('应该调用根据配置键查询值接口', () => {
      settingApi.getConfigByKey('test.key')
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting/value/test.key',
        method: 'get'
      })
    })
  })

  describe('addSetting', () => {
    it('应该导出 addSetting 函数', () => {
      expect(settingApi.addSetting).toBeDefined()
      expect(typeof settingApi.addSetting).toBe('function')
    })

    it('应该调用新增博客设置接口', () => {
      const settingData = { settingKey: 'test', settingValue: '1' }
      settingApi.addSetting(settingData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting',
        method: 'post',
        data: settingData
      })
    })
  })

  describe('updateSetting', () => {
    it('应该导出 updateSetting 函数', () => {
      expect(settingApi.updateSetting).toBeDefined()
      expect(typeof settingApi.updateSetting).toBe('function')
    })

    it('应该调用修改博客设置接口', () => {
      const settingData = { settingId: 123, settingValue: 'updated' }
      settingApi.updateSetting(settingData)
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting',
        method: 'put',
        data: settingData
      })
    })
  })

  describe('delSetting', () => {
    it('应该导出 delSetting 函数', () => {
      expect(settingApi.delSetting).toBeDefined()
      expect(typeof settingApi.delSetting).toBe('function')
    })

    it('应该调用删除博客设置接口', () => {
      settingApi.delSetting(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting/123',
        method: 'delete'
      })
    })

    it('应该支持批量删除', () => {
      settingApi.delSetting([1, 2, 3])
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting/1,2,3',
        method: 'delete'
      })
    })
  })

  describe('refreshCache', () => {
    it('应该导出 refreshCache 函数', () => {
      expect(settingApi.refreshCache).toBeDefined()
      expect(typeof settingApi.refreshCache).toBe('function')
    })

    it('应该调用刷新缓存接口', () => {
      settingApi.refreshCache()
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting/refreshCache',
        method: 'delete'
      })
    })
  })

  describe('updateSettingValueByKey', () => {
    it('应该导出 updateSettingValueByKey 函数', () => {
      expect(settingApi.updateSettingValueByKey).toBeDefined()
      expect(typeof settingApi.updateSettingValueByKey).toBe('function')
    })

    it('应该调用根据键更新设置值接口', () => {
      settingApi.updateSettingValueByKey('test.key', 'new.value')
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting/updateByKey',
        method: 'post',
        data: { settingKey: 'test.key', settingValue: 'new.value' }
      })
    })
  })

  describe('getRegistrationSwitch', () => {
    it('应该调用注册开关接口', () => {
      settingApi.getRegistrationSwitch()
      expect(request).toHaveBeenCalledWith({
        url: '/system/setting/registration',
        method: 'get'
      })
    })
  })
})
