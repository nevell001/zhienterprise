import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourcePath = resolve(process.cwd(), 'vite.config.ts')

describe('Vite dev proxy route boundaries', () => {
  it('should keep public site API routes out of front page route matching', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    // 公开接口统一在 /api/*（后端同前缀，代理不改写），页面路由都是短路径
    expect(source).toContain("'^/api/'")
    expect(source).not.toContain("'^/blog/(article|tag|category|setting|comment)/'")
  })

  it('should forward the real client IP to the backend through dev proxies', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    // 没有 xfwd 时，后端看到的直连方是前端容器（内网），限流/留言冷却会按容器 IP 计，
    // 与生产（nginx 覆盖 X-Forwarded-For）行为不一致
    expect(source.match(/xfwd: true/g)?.length ?? 0).toBeGreaterThanOrEqual(6)
  })

  it('should only generate visualizer report when ANALYZE is enabled', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain("process.env.ANALYZE === 'true'")
    expect(source).toContain('analyzePlugins')
    expect(source).toContain('...analyzePlugins')
  })

  it('should keep chart chunks explicit without forcing UI/editor libraries into entry preload', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).not.toContain("'element-plus': ['element-plus']")
    expect(source).not.toContain("quill: ['@vueup/vue-quill']")
    expect(source).toContain("'element-icons': ['@element-plus/icons-vue']")
    expect(source).toContain("echarts: ['echarts']")
  })

  it('should not hard-disable HMR in development', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).not.toContain('hmr: false')
  })

  it('should keep import.meta available for Vue HMR transforms', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain("target: 'es2020'")
    expect(source).not.toContain("target: 'es2015'")
  })

  it('should poll for file changes inside Docker bind mounts', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    // 容器内 bind mount 收不到 inotify 事件时，dev server 会一直返回旧模块
    expect(source).toContain('usePolling: true')
    expect(source).toContain('inDocker ? { watch: { usePolling: true')
  })
})
