/**
 * 监控入口地址解析。
 *
 * 优先级（高 → 低）：
 *   1. 后台「站点设置 → 站点信息 → 监控入口」里配置的地址（留空表示自动推导）
 *   2. 构建期环境变量 `VITE_PROMETHEUS_URL` / `VITE_GRAFANA_URL` / `VITE_ACTUATOR_URL`
 *   3. 自动推导：取「站点访问地址」（blog_url）的协议 + 主机名，拼上各服务默认端口；
 *      blog_url 未配置时回退到当前访问域名，因此部署到任何域名都不需要改代码；
 *      Actuator 默认用同源相对路径 `/manage/actuator`（dev 由 Vite 代理、prod 由 Nginx 代理）。
 *
 * 说明：生产镜像里的前端是构建产物，容器 `environment` 传入的变量对 SPA 无效，
 * 所以「换域名不用重新构建」这件事必须靠后台设置或自动推导来保证。
 */

export type MonitorTarget = 'prometheus' | 'grafana' | 'actuator'

/** 各服务默认端口（Actuator 与站点同源，不需要端口） */
export const MONITOR_DEFAULT_PORTS: Record<MonitorTarget, number> = {
  prometheus: 9090,
  grafana: 3001,
  actuator: 0
}

/** Actuator 默认走同源反代路径 */
export const ACTUATOR_DEFAULT_PATH = '/manage/actuator'

export interface MonitorUrlOptions {
  /** 后台设置值（空串 / null 表示未配置） */
  configured?: string | null
  /** 构建期环境变量兜底 */
  envUrl?: string | null
  /** 站点访问地址（blog_url），用于推导协议与主机名 */
  siteUrl?: string | null
  /** 当前页面地址，默认取 window.location（测试可注入） */
  location?: { protocol?: string; hostname?: string } | null
}

/** 去掉首尾空白与结尾斜杠；缺协议的地址补成协议相对写法（//host:port） */
export function normalizeMonitorUrl(value?: string | null): string {
  const trimmed = String(value ?? '').trim()
  if (!trimmed) return ''
  const withoutSlash = trimmed.replace(/\/+$/, '')
  if (withoutSlash.startsWith('/')) return withoutSlash
  if (/^[a-z][a-z0-9+.-]*:\/\//i.test(withoutSlash)) return withoutSlash
  return `//${withoutSlash}`
}

/** 从任意地址里取出协议与主机名；无法解析返回 null */
function parseHost(value?: string | null): { protocol: string; hostname: string } | null {
  const raw = String(value ?? '').trim()
  if (!raw) return null
  const candidates = [raw, `http://${raw.replace(/^\/+/, '')}`]
  for (const candidate of candidates) {
    try {
      const url = new URL(candidate)
      if (url.hostname) {
        return { protocol: url.protocol || 'http:', hostname: url.hostname }
      }
    } catch {
      // 尝试下一个候选写法
    }
  }
  return null
}

function currentLocation(
  injected?: { protocol?: string; hostname?: string } | null
): { protocol: string; hostname: string } | null {
  // 显式传 null 表示"没有可用地址"；不传（undefined）才回退 window.location
  const source =
    injected !== undefined
      ? injected
      : typeof window !== 'undefined'
        ? (window.location as Location)
        : undefined
  if (!source?.hostname) return null
  return { protocol: source.protocol || 'http:', hostname: source.hostname }
}

/**
 * 解析某个监控入口的地址。
 *
 * @returns 可直接使用的地址；无法推导时返回空串（调用方展示配置提示）
 */
export function resolveMonitorUrl(target: MonitorTarget, options: MonitorUrlOptions = {}): string {
  const configured = normalizeMonitorUrl(options.configured)
  if (configured) return configured

  const fromEnv = normalizeMonitorUrl(options.envUrl)
  if (fromEnv) return fromEnv

  if (target === 'actuator') return ACTUATOR_DEFAULT_PATH

  // 同源部署时用站点访问地址推导；否则回退当前访问域名
  const host = parseHost(options.siteUrl) ?? currentLocation(options.location)
  if (!host) return ''

  const port = MONITOR_DEFAULT_PORTS[target]
  const hostname = host.hostname.includes(':') ? `[${host.hostname}]` : host.hostname
  return `${host.protocol}//${hostname}${port ? `:${port}` : ''}`
}

/** 取地址里的端口（用于界面展示），无端口时返回默认端口 */
export function monitorUrlPort(url: string, target: MonitorTarget): number {
  const candidate = url.startsWith('//') ? `http:${url}` : url
  try {
    const parsed = new URL(candidate)
    const port = Number(parsed.port)
    if (port) return port
  } catch {
    // 相对路径等无法解析的情况，落到默认端口
  }
  return MONITOR_DEFAULT_PORTS[target]
}

/** 从 Actuator 端点 href 里取出相对入口地址的路径（如 /health） */
function endpointPath(href: string): string {
  const raw = String(href ?? '').trim()
  if (!raw) return ''
  let pathname = raw
  try {
    pathname = new URL(raw).pathname
  } catch {
    // 已是相对路径，直接用
  }
  const index = pathname.indexOf(ACTUATOR_DEFAULT_PATH)
  if (index >= 0) return pathname.slice(index + ACTUATOR_DEFAULT_PATH.length)
  return pathname.startsWith('/') ? pathname : `/${pathname}`
}

/**
 * 把后端返回的 Actuator 端点 href（通常是容器内地址，如 http://zhi-admin:8080/manage/actuator/health）
 * 转换为当前可访问的地址：只保留端点路径，再接到解析出的入口地址上。
 */
export function resolveActuatorEndpointUrl(base: string, href: string): string {
  const normalizedBase = normalizeMonitorUrl(base) || ACTUATOR_DEFAULT_PATH
  const absoluteBase = /^(https?:)?\/\//i.test(normalizedBase)
    ? normalizedBase
    : typeof window !== 'undefined'
      ? `${window.location.origin}${normalizedBase}`
      : normalizedBase
  const path = endpointPath(href)
  return path ? `${absoluteBase.replace(/\/+$/, '')}${path}` : absoluteBase
}
