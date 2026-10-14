import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const CONFIGS = ['nginx.conf', 'nginx-https.conf']

function readConfig(name: string): string {
  return readFileSync(resolve(process.cwd(), name), 'utf-8')
}

describe('Nginx 反向代理配置守卫', () => {
  it.each(CONFIGS)('%s 必须放开上传体积上限', name => {
    const source = readConfig(name)

    // nginx 默认 1MB，后端限的是 10MB/20MB；缺了它就是"开发能传、生产 413"
    expect(source).toContain('client_max_body_size 20m;')
  })

  it.each(CONFIGS)('%s 必须把后端根路径的非 SPA 端点转发出去', name => {
    const source = readConfig(name)

    // 否则会被 location / 的 try_files 回落成 index.html（搜索引擎抓 sitemap 拿到 HTML）
    for (const path of ['/sitemap.xml', '/robots.txt']) {
      expect(source).toContain(`location = ${path} {`)
    }
    // RSS 与全部公开接口统一在 /api/ 前缀 location 下原样转发，同样不能被 SPA 回落
    expect(source).toContain('location ^~ /api/ {')
    expect(source).toContain('proxy_pass http://zhi-admin:8080/api/;')
  })

  it.each(CONFIGS)('%s 必须覆盖 X-Forwarded-For 而不是追加', name => {
    const source = readConfig(name)

    expect(source).not.toContain('$proxy_add_x_forwarded_for')
    expect(source).toContain('proxy_set_header X-Forwarded-For $remote_addr;')
  })

  it.each(CONFIGS)('%s 必须覆盖 staging 构建的接口前缀', name => {
    const source = readConfig(name)

    // src/.env.staging 用 /stage-api（npm run build:stage），没有 location 会被 SPA 回落成 HTML
    expect(source).toContain('location ^~ /stage-api/ {')
  })

  it.each(CONFIGS)('%s 的块结构必须自洽（括号配平）', name => {
    const source = readConfig(name)
    let depth = 0
    for (const rawLine of source.split('\n')) {
      const code = rawLine.split('#')[0]
      depth += (code.match(/\{/g)?.length ?? 0) - (code.match(/\}/g)?.length ?? 0)
      expect(depth).toBeGreaterThanOrEqual(0)
    }
    expect(depth).toBe(0)
  })
})
