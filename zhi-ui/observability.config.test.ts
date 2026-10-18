import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

// 跨产物契约：Prometheus 规则、Grafana provisioning、compose 挂载与 logback 分属
// 不同文件/镜像，无法用组件行为覆盖，只能静态比对（与 nginx.config.test.ts 同理）。
const root = (...parts: string[]) => resolve(process.cwd(), '..', ...parts)
const read = (...parts: string[]) => readFileSync(root(...parts), 'utf-8')

const COMPOSE_FILES = [
  'docker-compose.dev.yml',
  'docker-compose.prod.yml',
  'docker-compose.release.yml'
]

describe('Prometheus 告警规则', () => {
  it('prometheus.yml 加载 alerts.yml，compose 也挂了该文件', () => {
    expect(read('prometheus/prometheus.yml')).toContain('rule_files:')
    expect(read('prometheus/prometheus.yml')).toContain('/etc/prometheus/alerts.yml')
    // release 的 deploy-assets.zip 会整目录拷贝 prometheus/，挂载路径必须与之一致
    expect(read('docker-compose.base.yml')).toContain(
      './prometheus/alerts.yml:/etc/prometheus/alerts.yml:ro'
    )
  })

  it('每条规则都有 expr / for / severity / summary', () => {
    const source = read('prometheus/alerts.yml')
    const rules = source.split(/\n\s*- alert:/).slice(1)
    expect(rules.length).toBeGreaterThanOrEqual(6)
    for (const rule of rules) {
      const name = rule.trim().split('\n')[0]
      for (const key of ['expr:', 'for:', 'severity:', 'summary:']) {
        expect(rule, `${name} 缺少 ${key}`).toContain(key)
      }
    }
  })

  it('覆盖可用性、5xx、延迟、堆内存、连接池与 GC', () => {
    const source = read('prometheus/alerts.yml')
    const alerts = [
      'ZhiBackendDown',
      'ZhiHighHttp5xxRate',
      'ZhiHighHttpLatencyP95',
      'ZhiJvmHeapHigh',
      'ZhiDbPoolSaturated',
      'ZhiGcPauseHigh'
    ]
    for (const alert of alerts) {
      expect(source).toContain(`alert: ${alert}`)
    }
  })
})

describe('Grafana 预置仪表盘', () => {
  it('数据源 uid 与面板引用一致，且每个面板都有查询', () => {
    expect(read('grafana/provisioning/datasources/prometheus.yml')).toContain('uid: prometheus')

    const dashboard = JSON.parse(read('grafana/provisioning/dashboards/zhi-overview.json')) as {
      uid: string
      panels: Array<{
        title: string
        datasource?: { uid?: string }
        targets?: Array<{ expr?: string }>
      }>
    }
    expect(dashboard.uid).toBe('zhi-enterprise-overview')
    expect(dashboard.panels.length).toBeGreaterThanOrEqual(10)
    for (const panel of dashboard.panels) {
      expect(panel.datasource?.uid, panel.title).toBe('prometheus')
      expect(panel.targets?.length, panel.title).toBeGreaterThan(0)
      for (const target of panel.targets ?? []) {
        expect(target.expr, panel.title).toBeTruthy()
      }
    }
  })

  it('provider 的扫描目录与 compose 挂载一致', () => {
    expect(read('grafana/provisioning/dashboards/default.yml')).toContain(
      'path: /etc/grafana/provisioning/dashboards'
    )
    expect(read('docker-compose.base.yml')).toContain(
      './grafana/provisioning:/etc/grafana/provisioning:ro'
    )
  })

  it('P95 面板/告警依赖的直方图已在 application.yml 开启', () => {
    const application = read('zhi-admin/src/main/resources/application.yml')
    expect(application).toContain('percentiles-histogram:')
    expect(application).toContain('http.server.requests: true')
    // 未开启时只有 _count/_sum，没有 _bucket，P95 查询会一直为空
    expect(read('grafana/provisioning/dashboards/zhi-overview.json')).toContain(
      'http_server_requests_seconds_bucket'
    )
    expect(read('prometheus/alerts.yml')).toContain('http_server_requests_seconds_bucket')
  })

  it('发布产物包含监控目录（告警与面板随包交付）', () => {
    const gitee = read('.workflow/ReleasePipeline.yml')
    const github = read('.github/workflows/release.yml')
    expect(gitee).toContain('cp -r prometheus grafana deploy-assets/')
    expect(github).toContain('cp -r prometheus deploy-assets/')
    expect(github).toContain('cp -r grafana deploy-assets/')
  })
})

describe('后端日志持久化', () => {
  it.each(COMPOSE_FILES)('%s 把 /app/logs 挂到 admin_logs 卷并限制 docker 日志大小', file => {
    const source = read(file)
    expect(source).toContain('admin_logs:/app/logs')
    expect(source).toMatch(/^ {2}admin_logs:$/m)
    expect(source).toContain('max-size: "20m"')
    expect(source).toContain('max-file: "5"')
  })

  it('release 编排固定工作目录，logback 的相对路径 ./logs 才落到挂载点', () => {
    expect(read('docker-compose.release.yml')).toContain('working_dir: /app')
    expect(read('zhi-admin/src/main/resources/logback.xml')).toContain('value="./logs"')
  })

  it('镜像预建 /app/logs，非 root 用户可写命名卷', () => {
    expect(read('Dockerfile-admin')).toContain('mkdir -p /app/uploadPath /app/logs')
  })
})
