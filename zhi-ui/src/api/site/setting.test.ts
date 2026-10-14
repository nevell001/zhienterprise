import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  listSetting,
  getSetting,
  addSetting,
  updateSetting,
  delSetting,
  updateSettingValueByKey,
  getSiteSettings,
  getSiteSettingsAnonymous,
  clearSiteCache
} from './setting'
import type { SiteSetting } from '@/types'
import request from '@/utils/request'

// Mock request module
vi.mock('@/utils/request')
const mockRequest = vi.mocked(request)

describe('Site Setting API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listSetting', () => {
    it('应该导出 listSetting 函数', () => {
      expect(listSetting).toBeDefined()
      expect(typeof listSetting).toBe('function')
    })

    it('应该调用 GET /system/setting/list', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      const query = { pageNum: 1, pageSize: 10 }
      await listSetting(query)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/setting/list',
        method: 'get',
        params: query
      })
    })

    it('应该在没有参数时仍然调用 API', async () => {
      mockRequest.mockResolvedValue({ rows: [], total: 0 })

      await listSetting()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/setting/list',
        method: 'get',
        params: undefined
      })
    })
  })

  describe('getSetting', () => {
    it('应该导出 getSetting 函数', () => {
      expect(getSetting).toBeDefined()
      expect(typeof getSetting).toBe('function')
    })

    it('应该调用 GET /system/setting/:id', async () => {
      const setting: SiteSetting = {
        id: 1,
        settingKey: 'blog_name',
        settingValue: '我的博客',
        remark: '博客名称',
        createTime: '',
        updateTime: ''
      }
      mockRequest.mockResolvedValue(setting)

      await getSetting(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/setting/1',
        method: 'get'
      })
    })
  })

  describe('addSetting', () => {
    it('应该导出 addSetting 函数', () => {
      expect(addSetting).toBeDefined()
      expect(typeof addSetting).toBe('function')
    })

    it('应该调用 POST /system/setting', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const setting: SiteSetting = {
        id: 0,
        settingKey: 'new_key',
        settingValue: 'new_value',
        remark: '新设置',
        createTime: '',
        updateTime: ''
      }
      await addSetting(setting)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/setting',
        method: 'post',
        data: setting
      })
    })
  })

  describe('updateSetting', () => {
    it('应该导出 updateSetting 函数', () => {
      expect(updateSetting).toBeDefined()
      expect(typeof updateSetting).toBe('function')
    })

    it('应该调用 PUT /system/setting', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      const setting: SiteSetting = {
        id: 1,
        settingKey: 'blog_name',
        settingValue: '我的博客更新',
        remark: '博客名称',
        createTime: '',
        updateTime: ''
      }
      await updateSetting(setting)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/setting',
        method: 'put',
        data: setting
      })
    })
  })

  describe('delSetting', () => {
    it('应该导出 delSetting 函数', () => {
      expect(delSetting).toBeDefined()
      expect(typeof delSetting).toBe('function')
    })

    it('应该调用 DELETE /system/setting/:id', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await delSetting(1)

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/setting/1',
        method: 'delete'
      })
    })

    it('应该支持批量删除（数组参数）', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await delSetting([1, 2, 3])

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/setting/1,2,3',
        method: 'delete'
      })
    })
  })
  describe('updateSettingValueByKey', () => {
    it('应该导出 updateSettingValueByKey 函数', () => {
      expect(updateSettingValueByKey).toBeDefined()
      expect(typeof updateSettingValueByKey).toBe('function')
    })

    it('应该调用 POST /system/setting/updateByKey', async () => {
      mockRequest.mockResolvedValue({ code: 200 })

      await updateSettingValueByKey('blog_name', '我的博客')

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/system/setting/updateByKey',
        method: 'post',
        data: { settingKey: 'blog_name', settingValue: '我的博客' }
      })
    })
  })

  describe('getSiteSettings', () => {
    it('应该导出 getSiteSettings 函数', () => {
      expect(getSiteSettings).toBeDefined()
      expect(typeof getSiteSettings).toBe('function')
    })

    it('应该调用 GET /common/blog/setting 并添加时间戳参数', async () => {
      mockRequest.mockResolvedValue({ blog_name: '我的博客' })

      await getSiteSettings()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/common/blog/setting',
        method: 'get',
        headers: { isToken: false },
        params: { _t: expect.any(Number) }
      })
    })
  })

  describe('getSiteSettingsAnonymous', () => {
    it('应该导出 getSiteSettingsAnonymous 函数', () => {
      expect(getSiteSettingsAnonymous).toBeDefined()
      expect(typeof getSiteSettingsAnonymous).toBe('function')
    })

    it('应该调用 GET /common/blog/setting 并设置 isToken: false', async () => {
      mockRequest.mockResolvedValue({ blog_name: '我的博客' })

      await getSiteSettingsAnonymous()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/common/blog/setting',
        method: 'get',
        headers: { isToken: false },
        params: { _t: expect.any(Number) }
      })
    })
  })

  describe('clearSiteCache', () => {
    it('应该导出 clearSiteCache 函数', () => {
      expect(clearSiteCache).toBeDefined()
      expect(typeof clearSiteCache).toBe('function')
    })

    it('应该调用 GET /common/blog/setting/clear-blog-cache', async () => {
      mockRequest.mockResolvedValue({ code: 200, msg: '缓存已清除' })

      await clearSiteCache()

      expect(mockRequest).toHaveBeenCalledWith({
        url: '/common/blog/setting/clear-blog-cache',
        method: 'get'
      })
    })
  })
})
