import { getConfigByKey } from '@/api/admin/setting'
import { logger } from './logger'
import { resolveMonitorUrl, type MonitorTarget } from './monitorUrl'

/**
 * 监控入口配置读取。
 *
 * 后台「站点设置 → 站点信息 → 监控入口」保存的是三个 blog_setting：
 * `prometheus_url` / `grafana_url` / `actuator_url`（留空表示自动推导）。
 * 这里统一读取并交给 `resolveMonitorUrl` 按「设置 → 构建期环境变量 → 站点访问地址推导」解析。
 *
 * 读取失败（例如当前账号没有 zhi:setting:query 权限）不会报错打断页面，
 * 只记一条日志并继续走环境变量/自动推导。
 */

/** 构建期兜底地址（生产镜像里 SPA 读不到容器运行时变量，换域名请用后台设置或自动推导） */
const ENV_URLS: Record<MonitorTarget, string | undefined> = {
  prometheus: import.meta.env?.VITE_PROMETHEUS_URL,
  grafana: import.meta.env?.VITE_GRAFANA_URL,
  actuator: import.meta.env?.VITE_ACTUATOR_URL
}

/** 配置缓存（毫秒），避免一个页面里重复请求 */
const CACHE_TTL = 30_000
let cache: { at: number; value: Record<string, string> } | null = null

async function readSetting(key: string): Promise<string> {
  try {
    const response: any = await getConfigByKey(key)
    const value = response?.data ?? response?.configValue ?? response?.msg
    return typeof value === 'string' ? value : ''
  } catch (error) {
    logger.warn(`读取站点设置 ${key} 失败，将改用环境变量或自动推导:`, error)
    return ''
  }
}

async function readMonitorSettings(): Promise<Record<string, string>> {
  if (cache && Date.now() - cache.at < CACHE_TTL) {
    return cache.value
  }
  const keys = ['prometheus_url', 'grafana_url', 'actuator_url', 'blog_url']
  const values = await Promise.all(keys.map(key => readSetting(key)))
  const result: Record<string, string> = {}
  keys.forEach((key, index) => {
    result[key] = values[index]
  })
  cache = { at: Date.now(), value: result }
  return result
}

/** 解析某个监控入口的最终地址 */
export async function loadMonitorUrl(target: MonitorTarget): Promise<string> {
  const settings = await readMonitorSettings()
  return resolveMonitorUrl(target, {
    configured: settings[`${target}_url`],
    envUrl: ENV_URLS[target],
    siteUrl: settings.blog_url
  })
}

/** 清空缓存（后台保存设置后调用，或测试使用） */
export function clearMonitorUrlCache(): void {
  cache = null
}
