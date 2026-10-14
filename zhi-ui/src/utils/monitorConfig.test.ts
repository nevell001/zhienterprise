import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearMonitorUrlCache, loadMonitorUrl } from './monitorConfig'

const getConfigByKey = vi.fn()

vi.mock('@/api/admin/setting', () => ({
  getConfigByKey: (key: string) => getConfigByKey(key)
}))

vi.mock('./logger', () => ({
  logger: { warn: vi.fn(), error: vi.fn(), info: vi.fn(), log: vi.fn() }
}))

const settingValues: Record<string, string> = {
  prometheus_url: '',
  grafana_url: '',
  actuator_url: '',
  blog_url: ''
}

describe('监控入口配置读取', () => {
  beforeEach(() => {
    getConfigByKey.mockReset()
    clearMonitorUrlCache()
    getConfigByKey.mockImplementation((key: string) =>
      Promise.resolve({ code: 200, data: settingValues[key] ?? '' })
    )
  })

  it('应该按站点访问地址推导 Prometheus / Grafana 地址', async () => {
    settingValues.blog_url = 'https://blog.example.com'
    await expect(loadMonitorUrl('prometheus')).resolves.toBe('https://blog.example.com:9090')
    await expect(loadMonitorUrl('grafana')).resolves.toBe('https://blog.example.com:3001')
  })

  it('应该优先使用后台配置的地址（可指向其它域名）', async () => {
    settingValues.blog_url = 'https://blog.example.com'
    settingValues.grafana_url = 'https://monitor.example.com/grafana'
    await expect(loadMonitorUrl('grafana')).resolves.toBe('https://monitor.example.com/grafana')
  })

  it('Actuator 默认走同源相对路径，也可显式配置', async () => {
    settingValues.blog_url = 'https://blog.example.com'
    await expect(loadMonitorUrl('actuator')).resolves.toBe('/manage/actuator')

    clearMonitorUrlCache()
    settingValues.actuator_url = 'https://ops.example.com/actuator'
    await expect(loadMonitorUrl('actuator')).resolves.toBe('https://ops.example.com/actuator')
  })

  it('单个设置读取失败时不应抛错，其余配置仍可参与推导', async () => {
    settingValues.blog_url = 'https://blog.example.com'
    getConfigByKey.mockImplementation((key: string) =>
      key === 'prometheus_url'
        ? Promise.reject(new Error('403 forbidden'))
        : Promise.resolve({ code: 200, data: settingValues[key] ?? '' })
    )
    await expect(loadMonitorUrl('prometheus')).resolves.toBe('https://blog.example.com:9090')
  })

  it('全部设置都读不到时应回退当前访问域名', async () => {
    getConfigByKey.mockRejectedValue(new Error('403 forbidden'))
    await expect(loadMonitorUrl('prometheus')).resolves.toBe(
      `${window.location.protocol}//${window.location.hostname}:9090`
    )
  })

  it('应缓存设置读取结果，避免同一页面重复请求', async () => {
    settingValues.blog_url = 'https://blog.example.com'
    await loadMonitorUrl('prometheus')
    const callsAfterFirst = getConfigByKey.mock.calls.length
    await loadMonitorUrl('grafana')
    expect(getConfigByKey.mock.calls.length).toBe(callsAfterFirst)
  })
})
