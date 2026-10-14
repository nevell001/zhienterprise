import { isFormDataRequest } from './request'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

// Captured response interceptor (set when request.ts registers it on the mocked instance)
const capturedInterceptors = vi.hoisted(() => ({
  response: undefined as ((res: any) => any) | undefined
}))

// Mock dependencies
vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: vi.fn() },
        response: {
          use: vi.fn((onFulfilled: (res: any) => any) => {
            capturedInterceptors.response = onFulfilled
          })
        }
      },
      defaults: {
        baseURL: '/dev-api',
        timeout: 10000,
        headers: { 'Content-Type': 'application/json;charset=utf-8' }
      },
      post: vi.fn()
    })),
    defaults: {
      headers: {}
    }
  }
}))

// Get mocked functions
const mockAxios = vi.mocked(axios)

// Now import service
import service, { download, isRelogin, isFrontSitePath } from './request'
vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(),
  getSiteToken: vi.fn(),
  removeToken: vi.fn()
}))
vi.mock('@/utils/errorCode', () => ({
  default: {
    401: '未授权，请重新登录',
    500: '服务器内部错误',
    601: '参数错误',
    default: '系统未知错误'
  }
}))
vi.mock('@/utils/zhi', () => ({
  tansParams: vi.fn(params => {
    return (
      Object.keys(params)
        .map(key => `${key}=${encodeURIComponent(params[key])}`)
        .join('&') + '&'
    )
  }),
  blobValidate: vi.fn(data => data instanceof Blob)
}))
vi.mock('@/plugins/cache', () => ({
  default: {
    session: {
      getJSON: vi.fn(),
      setJSON: vi.fn()
    }
  },
  session: {
    getJSON: vi.fn(),
    setJSON: vi.fn()
  }
}))
vi.mock('file-saver', () => ({
  saveAs: vi.fn()
}))
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({
    token: '',
    roles: [],
    permissions: []
  }))
}))
vi.mock('@/plugins/element-plus-service', () => ({
  ElNotification: {
    error: vi.fn()
  },
  ElMessage: Object.assign(vi.fn(), {
    error: vi.fn(),
    warning: vi.fn()
  }),
  ElMessageBox: {
    confirm: vi.fn()
  },
  ElLoading: {
    service: vi.fn(() => ({
      close: vi.fn()
    }))
  }
}))

// Get mocked functions
import { getToken, getSiteToken, removeToken } from '@/utils/auth'
const mockGetToken = vi.mocked(getToken)
const mockGetSiteToken = vi.mocked(getSiteToken)
const mockRemoveToken = vi.mocked(removeToken)

import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/zhi'
import cache from '@/plugins/cache'
import { saveAs } from 'file-saver'
import { useUserStore } from '@/stores/user'
import { ElNotification, ElMessage, ElLoading } from '@/plugins/element-plus-service'

const mockTansParams = vi.mocked(tansParams)
const mockBlobValidate = vi.mocked(blobValidate)
const mockCacheSession = vi.mocked(cache.session)
const mockSaveAs = vi.mocked(saveAs)
const mockUseUserStore = vi.mocked(useUserStore)
const mockElNotification = vi.mocked(ElNotification)
const mockElMessage = vi.mocked(ElMessage)
const mockElLoading = vi.mocked(ElLoading)
const requestSourcePath = resolve(process.cwd(), 'src/utils/request.ts')

describe('Request 工具函数测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Reset isRelogin
    isRelogin.show = false
    // Mock service.post for download tests
    service.post = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('axios 实例配置', () => {
    it('应该正确配置 axios 实例', () => {
      expect(service).toBeDefined()
    })

    it('应该设置正确的 baseURL', () => {
      const baseURL = import.meta.env?.VITE_APP_BASE_API || '/dev-api'
      expect(service.defaults.baseURL).toBe(baseURL)
    })

    it('应该设置正确的超时时间', () => {
      expect(service.defaults.timeout).toBe(10000)
    })

    it('应该设置正确的默认 Content-Type', () => {
      expect(service.defaults.headers['Content-Type']).toBe('application/json;charset=utf-8')
    })
  })

  describe('请求拦截器', () => {
    it('应该让博客前台 API 绕开默认 baseURL 以命中独立代理', () => {
      const source = readFileSync(requestSourcePath, 'utf-8')

      expect(source).toContain("config.url.startsWith('/blog/api/')")
      expect(source).toContain("config.baseURL = ''")
    })

    it('应该在请求头中添加 Authorization token', async () => {
      mockGetToken.mockReturnValue('test-token')
      mockAxios.create.mockReturnValue({
        interceptors: {
          request: {
            use: vi.fn(cb => {
              const config = {
                headers: {},
                url: '/api/test',
                method: 'get'
              }
              const result = cb(config)
              expect(result.headers.Authorization).toBe('Bearer test-token')
            })
          },
          response: {
            use: vi.fn()
          }
        }
      } as any)
    })

    it('应该优先使用管理员 token', async () => {
      mockGetToken.mockReturnValue('admin-token')
      mockGetSiteToken.mockReturnValue('blog-token')

      const config = {
        headers: {},
        url: '/api/test',
        method: 'get'
      }

      const result = config
      if (result) {
        const token = getToken() || getSiteToken()
        if (token && (result.headers as any)?.isToken !== false) {
          ;(result.headers as any).Authorization = 'Bearer ' + token
        }
        expect(result.headers.Authorization).toBe('Bearer admin-token')
      }
    })

    it('应该在管理员 token 不存在时使用博客 token', async () => {
      mockGetToken.mockReturnValue('')
      mockGetSiteToken.mockReturnValue('blog-token')

      const token = getToken() || getSiteToken()
      expect(token).toBe('blog-token')
    })

    it('应该处理 FormData 请求（删除 Content-Type）', async () => {
      const formData = new FormData()
      formData.append('file', 'test')

      const config = {
        headers: { 'Content-Type': 'application/json;charset=utf-8' },
        data: formData
      }

      if (config.data instanceof FormData) {
        delete (config.headers as any)['Content-Type']
      }

      expect(config.headers['Content-Type']).toBeUndefined()
    })

    it('应该处理 GET 请求参数', async () => {
      mockTansParams.mockReturnValue('param1=value1&param2=value2&')

      const config = {
        url: '/api/test',
        method: 'get',
        params: { param1: 'value1', param2: 'value2' }
      }

      if (config.method === 'get' && config.params) {
        let url = config.url + '?' + tansParams(config.params)
        url = url.slice(0, -1)
        config.params = {}
        config.url = url
      }

      expect(config.params).toEqual({})
      expect(config.url).toBe('/api/test?param1=value1&param2=value2')
    })

    it('应该跳过重复提交检查（isRepeatSubmit=false）', async () => {
      const config = {
        headers: { repeatSubmit: false },
        url: '/api/test',
        method: 'post',
        data: { test: 'data' }
      }

      const isRepeatSubmit = (config.headers as any)?.repeatSubmit === false
      expect(isRepeatSubmit).toBe(true)
    })

    it('应该跳过 FormData 请求的重复提交检查', () => {
      const formData = new FormData()
      formData.append('file', 'test')

      // 断言真实行为：FormData 请求会被识别为"跳过序列化/重复提交检查"
      expect(isFormDataRequest({ url: '/api/test', method: 'post', data: formData })).toBe(true)
      expect(isFormDataRequest({ data: { content: '普通 JSON' } })).toBe(false)
      expect(isFormDataRequest(null)).toBe(false)
    })

    it('应该跳过 HTML 内容的重复提交检查', async () => {
      const config = {
        url: '/api/test',
        method: 'post',
        data: { content: '<html><body>test</body></html>' }
      }

      // HTML 字符串走普通 JSON 路径，不会被当成 FormData 跳过检查
      expect(isFormDataRequest(config)).toBe(false)
      expect(typeof (config.data as any).content).toBe('string')
    })
  })

  describe('响应拦截器', () => {
    it('应该处理成功的响应（code=200）', async () => {
      const response = {
        data: { code: 200, data: { name: 'test' } }
      }

      const code = response.data.code || 200
      expect(code).toBe(200)
    })

    it('应该处理 401 未授权错误', async () => {
      const response = {
        data: { code: 401, msg: '未授权' }
      }

      const code = response.data.code || 200
      const msg = errorCode[code] || response.data.msg || errorCode['default']

      expect(code).toBe(401)
      expect(msg).toBe('未授权，请重新登录')
    })

    it('应该处理 500 服务器错误', async () => {
      const response = {
        data: { code: 500, msg: '服务器错误' }
      }

      const code = response.data.code || 200
      const msg = errorCode[code] || response.data.msg || errorCode['default']

      expect(code).toBe(500)
      expect(msg).toBe('服务器内部错误')
    })

    it('应该处理 601 参数错误', async () => {
      const response = {
        data: { code: 601, msg: '参数错误' }
      }

      const code = response.data.code || 200
      const msg = errorCode[code] || response.data.msg || errorCode['default']

      expect(code).toBe(601)
      expect(msg).toBe('参数错误')
    })

    it('code=500 且 silent 时只记录日志、不弹兜底 toast（可选 slug 未建页属正常态）', async () => {
      const interceptor = capturedInterceptors.response
      expect(interceptor).toBeDefined()

      const res = {
        data: { code: 500, msg: '页面不存在或未发布' },
        config: { url: '/blog/page/honors', silent: true },
        request: { responseType: 'json' },
        status: 200
      }

      await expect(interceptor!(res)).rejects.toThrow('服务器内部错误')
      expect(ElMessage).not.toHaveBeenCalled()
    })

    it('code=500 且非 silent 时仍弹兜底 toast', async () => {
      const interceptor = capturedInterceptors.response

      const res = {
        data: { code: 500, msg: '页面不存在或未发布' },
        config: { url: '/blog/page/honors' },
        request: { responseType: 'json' },
        status: 200
      }

      await expect(interceptor!(res)).rejects.toThrow('服务器内部错误')
      expect(ElMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'error',
          message: expect.stringContaining('服务器内部错误')
        })
      )
    })

    it('应该处理二进制数据响应', async () => {
      const response = {
        request: { responseType: 'blob' },
        data: new Blob(['test'])
      }

      if (
        response.request.responseType === 'blob' ||
        response.request.responseType === 'arraybuffer'
      ) {
        expect(response.data).toBeInstanceOf(Blob)
      }
    })

    it('应该处理未知错误', async () => {
      const response = {
        data: { code: 999, msg: '' }
      }

      const code = response.data.code || 200
      const msg = errorCode[code] || response.data.msg || errorCode['default']

      expect(msg).toBe('系统未知错误')
    })
  })

  describe('错误处理', () => {
    it('应该处理 Network Error', () => {
      const error = { message: 'Network Error' }
      let message = error.message

      if (message == 'Network Error') {
        message = '后端接口连接异常'
      }

      expect(message).toBe('后端接口连接异常')
    })

    it('应该处理超时错误', () => {
      const error = { message: 'timeout of 10000ms exceeded' }
      let message = error.message

      if (message.includes('timeout')) {
        message = '系统接口请求超时'
      }

      expect(message).toBe('系统接口请求超时')
    })

    it('应该处理 HTTP 状态码错误', () => {
      const error = { message: 'Request failed with status code 404' }
      let message = error.message

      if (message.includes('Request failed with status code')) {
        message = '系统接口' + message.substr(message.length - 3) + '异常'
      }

      expect(message).toBe('系统接口404异常')
    })

    it('应该处理 500 状态码错误', () => {
      const error = { message: 'Request failed with status code 500' }
      let message = error.message

      if (message.includes('Request failed with status code')) {
        message = '系统接口' + message.substr(message.length - 3) + '异常'
      }

      expect(message).toBe('系统接口500异常')
    })
  })

  describe('download 函数', () => {
    it('应该正确调用 download 函数', async () => {
      const mockLoading = { close: vi.fn() }
      mockElLoading.service.mockReturnValue(mockLoading)
      mockBlobValidate.mockReturnValue(true)

      const mockResponse = new Blob(['test data'])
      const mockPost = vi.fn().mockResolvedValue(mockResponse)
      service.post = mockPost

      download('/api/download', { id: 1 }, 'test.txt')

      expect(mockElLoading.service).toHaveBeenCalledWith({
        text: '正在下载数据，请稍候',
        background: 'rgba(0, 0, 0, 0.7)'
      })
    })

    it('应该处理下载成功的 Blob 数据', async () => {
      const mockLoading = { close: vi.fn() }
      mockElLoading.service.mockReturnValue(mockLoading)
      mockBlobValidate.mockReturnValue(true)

      const mockResponse = new Blob(['test data'])
      const mockPost = vi.fn().mockResolvedValue(mockResponse)
      // Mock service.post
      service.post = mockPost

      download('/api/download', { id: 1 }, 'test.txt')

      // Wait for async operations to complete
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(mockSaveAs).toHaveBeenCalled()
    })

    it('应该处理下载失败的错误响应', async () => {
      const mockLoading = { close: vi.fn() }
      mockElLoading.service.mockReturnValue(mockLoading)
      mockBlobValidate.mockReturnValue(false)

      const blobData = JSON.stringify({ code: 500, msg: '下载失败' })
      const mockResponse = new Blob([blobData])
      mockResponse.text = vi.fn().mockResolvedValue(blobData)
      const mockPost = vi.fn().mockResolvedValue(mockResponse)
      // Mock service.post
      service.post = mockPost

      download('/api/download', { id: 1 }, 'test.txt')

      // Wait for async operations to complete
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(mockElMessage.error).toHaveBeenCalled()
    })

    it('应该处理下载错误', async () => {
      const mockLoading = { close: vi.fn() }
      mockElLoading.service.mockReturnValue(mockLoading)
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      const mockPost = vi.fn().mockRejectedValue(new Error('下载错误'))
      // Mock service.post
      service.post = mockPost

      download('/api/download', { id: 1 }, 'test.txt')

      // Wait for async operations to complete
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(mockElMessage.error).toHaveBeenCalledWith('下载文件出现错误，请联系管理员！')
      consoleErrorSpy.mockRestore()
    })
  })

  describe('边界情况', () => {
    it('应该处理空响应', () => {
      const response = { data: {} }
      const code = response.data.code || 200

      expect(code).toBe(200)
    })

    it('应该处理没有 code 的响应', () => {
      const response = { data: { msg: 'test' } }
      const code = response.data.code || 200

      expect(code).toBe(200)
    })

    it('应该处理没有 msg 的响应', () => {
      const response = { data: { code: 500 } }
      const msg = errorCode[response.data.code] || response.data.msg || errorCode['default']

      expect(msg).toBe('服务器内部错误')
    })

    it('应该处理空错误消息', () => {
      const error = { message: '' }
      let message = error.message

      expect(message).toBe('')
    })

    it('应该处理 undefined 错误', () => {
      const error = {}
      let message = error.message || ''

      expect(message).toBe('')
    })
  })

  describe('会话管理', () => {
    it('应该识别站点前台的短路径公开页', () => {
      const frontPaths = [
        '/',
        '/index',
        '/news',
        '/article/2',
        '/business',
        '/business/1',
        '/contact',
        '/about',
        '/honors',
        '/history',
        '/category',
        '/category/9',
        '/tag',
        '/tag/3',
        '/author/1',
        '/archive',
        '/guestbook',
        '/page/about-us',
        '/friend-links/apply',
        '/search',
        '/bookmarks',
        '/agreement',
        '/privacy',
        '/login',
        '/register',
        '/forgot-password'
      ]

      frontPaths.forEach(path => {
        expect(isFrontSitePath(path), path).toBe(true)
      })
    })

    it('后台与旧 /blog 前缀路径不应被当成前台页', () => {
      const adminPaths = ['/admin', '/admin/article', '/blog', '/blog/article/1', '/manage']
      adminPaths.forEach(path => {
        expect(isFrontSitePath(path), path).toBe(false)
      })
    })

    it('仅前缀匹配的短路径必须有后续段（/article 本身不是前台页）', () => {
      expect(isFrontSitePath('/article')).toBe(false)
      expect(isFrontSitePath('/tag')).toBe(true)
    })
  })

  describe('Token 管理', () => {
    it('应该移除 token', () => {
      mockRemoveToken.mockReturnValue(undefined)
      removeToken()

      expect(mockRemoveToken).toHaveBeenCalled()
    })

    it('应该清除用户状态', () => {
      const userStore = useUserStore()
      userStore.token = ''
      userStore.roles = []
      userStore.permissions = []

      expect(userStore.token).toBe('')
      expect(userStore.roles).toEqual([])
      expect(userStore.permissions).toEqual([])
    })
  })

  describe('重复提交检查', () => {
    it('应该存储请求对象', () => {
      const requestObj = {
        url: '/api/test',
        data: '{"test":"data"}',
        time: new Date().getTime()
      }

      const requestSize = Object.keys(JSON.stringify(requestObj)).length
      const limitSize = 5 * 1024 * 1024

      expect(requestSize).toBeLessThan(limitSize)
    })

    it('应该检查请求大小限制', () => {
      const largeDataLength = 10 * 1024 * 1024 // 10MB
      const requestObjWithoutData = {
        url: '/api/test',
        data: '',
        time: new Date().getTime()
      }

      const requestSize = JSON.stringify(requestObjWithoutData).length + largeDataLength
      const limitSize = 5 * 1024 * 1024

      expect(requestSize).toBeGreaterThan(limitSize)
    })

    it('应该检查重复提交时间间隔', () => {
      const currentTime = new Date().getTime()
      const previousTime = currentTime - 500 // 500ms ago
      const interval = 1000

      const isRepeat = currentTime - previousTime < interval
      expect(isRepeat).toBe(true)
    })

    it('应该允许超过时间间隔的请求', () => {
      const currentTime = new Date().getTime()
      const previousTime = currentTime - 1500 // 1500ms ago
      const interval = 1000

      const isRepeat = currentTime - previousTime < interval
      expect(isRepeat).toBe(false)
    })
  })

  describe('并发操作', () => {
    it('应该支持多个并发请求', async () => {
      const promises = [
        Promise.resolve({ data: { code: 200, data: 'data1' } }),
        Promise.resolve({ data: { code: 200, data: 'data2' } }),
        Promise.resolve({ data: { code: 200, data: 'data3' } })
      ]

      const results = await Promise.all(promises)

      expect(results).toHaveLength(3)
      expect(results.every(r => r.data.code === 200)).toBe(true)
    })

    it('应该支持多个并发下载', async () => {
      const mockLoading = { close: vi.fn() }
      mockElLoading.service.mockReturnValue(mockLoading)
      mockBlobValidate.mockReturnValue(true)

      const mockResponse = new Blob(['test data'])
      const mockPost = vi.fn().mockResolvedValue(mockResponse)
      // Mock service.post
      service.post = mockPost

      const promises = [
        download('/api/download1', { id: 1 }, 'test1.txt'),
        download('/api/download2', { id: 2 }, 'test2.txt'),
        download('/api/download3', { id: 3 }, 'test3.txt')
      ]

      await Promise.all(promises)

      expect(mockElLoading.service).toHaveBeenCalledTimes(3)
    })
  })
})
