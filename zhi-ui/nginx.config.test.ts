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

  it.each(CONFIGS)('%s 必须覆盖生产构建的接口前缀且剥掉 /prod-api', name => {
    const source = readConfig(name)
    const index = source.indexOf('location ^~ /prod-api/ {')
    expect(index).toBeGreaterThanOrEqual(0)

    // 必须剥前缀：/prod-api/captchaImage、/prod-api/system/** 要变成后端的 /captchaImage、/system/**
    const block = source.slice(index)
    const proxyPass = block.match(/proxy_pass\s+([^;]+);/)?.[1] ?? ''
    expect(proxyPass).toBe('http://zhi-admin:8080/')
  })

  it('生产构建的 VITE_APP_BASE_API 必须是 /prod-api（/api 是站点公开 API 直通前缀）', () => {
    // /api 在 nginx 里是「站点公开 API 原样转发」，写成 base 会把 /captchaImage、/auth/login、
    // /system/** 变成 /api/xxx 而 401（登录页验证码不显示就是这个原因）
    const env = readFileSync(resolve(process.cwd(), '.env.production'), 'utf-8')
    expect(env).toMatch(/VITE_APP_BASE_API\s*=\s*'\/prod-api'/)
    expect(readConfig('nginx.conf')).toContain('proxy_pass http://zhi-admin:8080/api/;')
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

describe('安全响应头与 CORS 边界', () => {
  const countOf = (source: string, needle: string) => source.split(needle).length - 1

  it.each(CONFIGS)('%s 在 server 与自带 add_header 的 location 都下发基础安全头', name => {
    const source = readConfig(name)

    // server + 静态资源 + /profile/ + /health；/api/ 已不自带 add_header，继承 server 级配置
    expect(
      countOf(source, 'add_header X-Content-Type-Options nosniff always;')
    ).toBeGreaterThanOrEqual(4)
    expect(countOf(source, 'add_header X-Frame-Options SAMEORIGIN always;')).toBeGreaterThanOrEqual(
      4
    )
    expect(
      countOf(source, 'add_header Referrer-Policy strict-origin-when-cross-origin always;')
    ).toBeGreaterThanOrEqual(4)
  })

  it('HSTS 只在 HTTPS 配置下发（HTTP 响应中的 HSTS 会被浏览器忽略）', () => {
    expect(readConfig('nginx-https.conf')).toContain('Strict-Transport-Security')
    expect(readConfig('nginx.conf')).not.toContain('Strict-Transport-Security')
  })

  it.each(CONFIGS)('%s 不得覆盖后端 CORS 白名单', name => {
    // CORS 由后端 ResourcesConfig（CORS_ALLOWED_ORIGINS）统一裁决；
    // nginx 再加 ACAO 会让生产空白名单失效（曾对全互联网放开）
    expect(readConfig(name)).not.toMatch(/add_header\s+Access-Control-Allow-Origin/)
  })
})

describe('上传体积上限必须跨产物一致', () => {
  // 跨产物契约：nginx / dev / prod / 前端提示分属不同文件，只能静态比对
  const yml = (name: string) =>
    readFileSync(resolve(process.cwd(), '..', 'zhi-admin/src/main/resources', name), 'utf-8')

  it.each(['application.yml', 'application-prod.yml'])('%s 使用 5MB 单文件 / 10MB 请求', name => {
    const source = yml(name)

    expect(source).toContain('max-file-size: 5MB')
    expect(source).toContain('max-request-size: 10MB')
  })

  it.each(CONFIGS)('%s 的 client_max_body_size 不小于后端请求上限', name => {
    const source = readConfig(name)
    const nginxLimit = Number(source.match(/client_max_body_size\s+(\d+)m;/)?.[1])
    const requestLimit = Number(yml('application.yml').match(/max-request-size:\s*(\d+)MB/)?.[1])

    expect(nginxLimit).toBeGreaterThanOrEqual(requestLimit)
  })
})
