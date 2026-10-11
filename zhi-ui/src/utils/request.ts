import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig
} from 'axios'
import { ElNotification, ElMessageBox, ElMessage, ElLoading } from '@/plugins/element-plus-service'
import { getToken, getBlogToken, removeToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/zhi'
import cache from '@/plugins/cache'
import { saveAs } from 'file-saver'
import { useUserStore } from '@/stores/user'
import { logger } from '@/utils/logger'

interface DownloadConfig extends AxiosRequestConfig {
  [key: string]: any
}

let downloadLoadingInstance: any
// 是否显示重新登录
export const isRelogin = { show: false }

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const baseApi = import.meta.env?.VITE_APP_BASE_API || '/dev-api'
const service: AxiosInstance = axios.create({
  // axios中请求配置有baseURL选项，表示请求URL公共部分
  baseURL: baseApi,
  // 超时
  timeout: 10000
})

// request拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (config.url && config.url.startsWith('/blog/api/')) {
      config.baseURL = ''
    }

    // 如果是 FormData，删除 Content-Type 让浏览器自动设置为 multipart/form-data
    if (isFormDataRequest(config)) {
      delete (config.headers as any)['Content-Type']
    }

    // 是否需要设置 token
    const isToken = (config.headers as any)?.isToken === false
    // 是否需要防止数据重复提交
    const isRepeatSubmit = (config.headers as any)?.repeatSubmit === false
    // 优先使用管理员token，如果没有则尝试注册用户token
    const token = getToken() || getBlogToken()
    if (token && !isToken) {
      ;(config.headers as any).Authorization = 'Bearer ' + token // 让每个请求携带自定义token 请根据实际情况自行修改
    }
    // get请求映射params参数
    if (config.method === 'get' && config.params) {
      let url = config.url + '?' + tansParams(config.params)
      url = url.slice(0, -1)
      config.params = {}
      config.url = url
    }
    if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
      // 对于 FormData 请求，跳过重复提交检查（FormData 包含文件数据，不适合序列化）
      if (isFormDataRequest(config)) {
        return config
      }

      // 对于包含HTML内容的请求，完全跳过重复提交检查的所有相关操作
      const isHtmlContent =
        config.data &&
        typeof config.data === 'object' &&
        (config.data as any).content &&
        (config.data as any).content.includes('<')

      if (isHtmlContent) {
        // 对于HTML内容，直接跳过整个重复提交检查逻辑
        return config
      }

      // 对于非HTML内容，进行完整的重复提交检查
      const requestObj = {
        url: config.url,
        data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
        time: new Date().getTime()
      }

      const requestSize = JSON.stringify(requestObj).length // 请求数据大小
      const limitSize = 5 * 1024 * 1024 // 限制存放数据5M
      if (requestSize >= limitSize) {
        logger.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
        return config
      }

      const sessionObj = cache.session.getJSON('sessionObj')
      if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
        cache.session.setJSON('sessionObj', requestObj)
      } else {
        const s_url = sessionObj.url // 请求地址
        const s_data = sessionObj.data // 请求数据
        const s_time = sessionObj.time // 请求时间
        const interval = 1000 // 间隔时间(ms)，小于此时间视为重复提交
        if (
          s_data === requestObj.data &&
          requestObj.time - s_time < interval &&
          s_url === requestObj.url
        ) {
          const message = '数据正在处理，请勿重复提交'
          logger.warn(`[${s_url}]: ` + message)
          return Promise.reject(new Error(message))
        } else {
          cache.session.setJSON('sessionObj', requestObj)
        }
      }
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (res: AxiosResponse) => {
    // 未设置状态码则默认成功状态
    const code = res.data.code || 200
    // 获取错误信息
    const msg = errorCode[code] || res.data.msg || errorCode['default']
    // 二进制数据则直接返回
    if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
      return res.data
    }
    if (code === 401) {
      // 检查当前访问的路径是否为站点前台页面
      const currentPath = window.location.pathname
      const isBlogPath =
        currentPath.startsWith('/blog') ||
        currentPath === '/' ||
        currentPath === '/index' ||
        currentPath.startsWith('/blog/article/') ||
        currentPath.startsWith('/blog/category/') ||
        currentPath.startsWith('/blog/tag/')

      // 如果是站点前台页面，不显示登录提示，直接返回数据或空结果
      if (isBlogPath) {
        logger.warn('匿名用户访问站点页面，返回空数据或默认数据')
        // 对于站点前台页面，返回空数据而不是强制登录
        return Promise.resolve({ code: 200, data: null, msg: '匿名访问' })
      }

      // 对于后台管理页面，清除用户状态并重定向到登录页
      logger.warn('登录状态已过期，清除用户状态并重定向到登录页')
      const userStore = useUserStore()
      userStore.token = ''
      userStore.roles = []
      userStore.permissions = []
      removeToken()
      // 使用 window.location.replace 避免重复历史记录
      window.location.replace('/login')
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')
    } else if (code === 500) {
      logger.error('服务器错误:', {
        url: res.config.url,
        method: res.config.method,
        status: res.status,
        response: res.data
      })
      ElMessage({
        message: `服务器错误: ${msg}`,
        type: 'error',
        duration: 5000
      })
      return Promise.reject(new Error(msg))
    } else if (code === 601) {
      ElMessage({ message: msg, type: 'warning' })
      return Promise.reject(new Error(msg))
    } else if (code !== 200) {
      if (!(res.config as any)?.silent) {
        ElNotification.error({ title: msg })
      }
      return Promise.reject('error')
    } else {
      return Promise.resolve(res.data)
    }
  },
  error => {
    let { message } = error
    if (message == 'Network Error') {
      message = '后端接口连接异常'
    } else if (message.includes('timeout')) {
      message = '系统接口请求超时'
    } else if (message.includes('Request failed with status code')) {
      message = '系统接口' + message.substr(message.length - 3) + '异常'
    }
    ElMessage({ message: message, type: 'error', duration: 5 * 1000 })
    return Promise.reject(error)
  }
)

// 通用下载方法
export function download(
  url: string,
  params: any,
  filename: string,
  config?: DownloadConfig
): void {
  downloadLoadingInstance = ElLoading.service({
    text: '正在下载数据，请稍候',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  service
    .post(url, params, {
      transformRequest: [
        params => {
          return tansParams(params)
        }
      ],
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      responseType: 'blob',
      ...config
    } as AxiosRequestConfig)
    .then(async (data: any) => {
      const isBlob = blobValidate(data)
      if (isBlob) {
        const blob = new Blob([data])
        saveAs(blob, filename)
      } else {
        const resText = await (data as Blob).text()
        const rspObj = JSON.parse(resText)
        const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
        ElMessage.error(errMsg)
      }
      downloadLoadingInstance.close()
    })
    .catch(r => {
      logger.error(r)
      ElMessage.error('下载文件出现错误，请联系管理员！')
      downloadLoadingInstance.close()
    })
}

/**
 * 是否 FormData 请求（上传文件）：这类请求不能序列化，因此跳过请求体序列化与重复提交检查。
 * 抽成纯函数以便直接断言行为，而不是在测试里复述判定条件。
 */
export function isFormDataRequest(config: { data?: unknown } | null | undefined): boolean {
  if (!config || typeof FormData === 'undefined') return false
  return config.data instanceof FormData
}

export default service
