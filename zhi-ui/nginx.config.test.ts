import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { constantRoutes } from './src/router'

const CONFIGS = ['nginx.conf', 'nginx-https.conf']

function readConfig(name: string): string {
  return readFileSync(resolve(process.cwd(), name), 'utf-8')
}

function readSpaRoutePatterns(source: string): RegExp[] {
  const start = source.indexOf('map $uri $zhi_spa_route {')
  expect(start).toBeGreaterThanOrEqual(0)
  const end = source.indexOf('\n}', start)
  expect(end).toBeGreaterThan(start)
  return source
    .slice(start, end)
    .split('\n')
    .map(line => line.trim().match(/^~(\S+)\s+1;$/)?.[1])
    .filter((pattern): pattern is string => Boolean(pattern))
    .map(pattern => new RegExp(pattern))
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

describe('SPA 软 404 守卫', () => {
  // 跨产物契约：nginx 白名单与 Vue 路由分属两份文件，只能用静态比对覆盖
  type RouteLike = { path: string; children?: RouteLike[] }

  // 只取叶子路由：父级是 Layout 包装（如 /admin/system/user-auth），本身不是可访问页面
  const leafPaths = (routes: RouteLike[], parentPath = ''): string[] =>
    routes.flatMap(route => {
      const path = route.path.startsWith('/')
        ? route.path
        : `${parentPath.replace(/\/$/, '')}/${route.path}`
      return route.children?.length ? leafPaths(route.children, path) : [path]
    })

  const toSampleUrl = (path: string): string =>
    path
      .replace(/:path\(\.\*\)/g, 'a/b')
      .replace(/:\w+\(\\d\+\)/g, '1')
      .replace(/:\w+/g, '1')

  it.each(CONFIGS)('%s 的 SPA 白名单覆盖全部已注册页面路由', name => {
    const patterns = readSpaRoutePatterns(readConfig(name))
    const urls = leafPaths(constantRoutes as unknown as RouteLike[]).map(toSampleUrl)
    const uncovered = urls.filter(url => !patterns.some(re => re.test(url)))

    // 漏掉的路由会被当作未知路径返回 404，表现为刷新/直达页面打不开
    expect(uncovered).toEqual([])
  })

  it.each(CONFIGS)('%s 未注册路径不得命中 SPA 白名单', name => {
    const patterns = readSpaRoutePatterns(readConfig(name))
    const unknown = [
      '/no-such-page',
      '/articles/1',
      '/article',
      '/business/1/extra',
      '/page',
      '/friend-links/nope',
      '/adminx'
    ]

    for (const url of unknown) {
      expect(
        patterns.some(re => re.test(url)),
        url
      ).toBe(false)
    }
  })

  it.each(CONFIGS)('%s 未知路径必须以 404 状态回落 index.html', name => {
    const source = readConfig(name)

    expect(source).toContain('map $uri $zhi_spa_route {')
    expect(source).toContain('default 0;')
    expect(source).toContain('try_files $uri $uri/ @spa;')
    expect(source).toContain('location @spa {')
    expect(source).toContain('if ($zhi_spa_route = 0) {')
    // error_page 不带 =200，保留 404 状态码，同时渲染 SPA 的 404 页
    expect(source).toContain('error_page 404 /index.html;')
    expect(source).not.toContain('try_files $uri $uri/ /index.html;')
  })
})

describe('旧博客 URL 301 映射', () => {
  type LegacyRewrite = { pattern: RegExp; replacement: string }

  /** 解析 server 级的 `rewrite <regex> <target> permanent;`，按出现顺序（nginx 首个匹配生效） */
  function readLegacyRewrites(source: string): LegacyRewrite[] {
    const rewrites: LegacyRewrite[] = []
    for (const rawLine of source.split('\n')) {
      const match = rawLine.trim().match(/^rewrite\s+(\S+)\s+(\S+)\s+permanent;$/)
      if (match) {
        rewrites.push({ pattern: new RegExp(match[1]), replacement: match[2] })
      }
    }
    return rewrites
  }

  function resolveLegacy(rewrites: LegacyRewrite[], url: string): string | null {
    for (const { pattern, replacement } of rewrites) {
      if (pattern.test(url)) {
        return url.replace(pattern, replacement)
      }
    }
    return null
  }

  // 旧路径取自去 /blog 前缀之前的 router/blog.ts；已下线页面按产品口径回落
  const LEGACY_CASES: Array<[string, string]> = [
    ['/blog', '/'],
    ['/blog/', '/'],
    ['/blog/articles', '/news'],
    ['/blog/article/42', '/article/42'],
    ['/blog/article/some-slug/', '/article/some-slug'],
    ['/blog/business', '/business'],
    ['/blog/business/7', '/business/7'],
    ['/blog/contact', '/contact'],
    ['/blog/category', '/category'],
    ['/blog/category/3', '/category/3'],
    ['/blog/tag', '/tag'],
    ['/blog/tag/5', '/tag/5'],
    ['/blog/friend-links/apply', '/friend-links/apply'],
    ['/blog/page/home', '/page/home'],
    ['/blog/about', '/about'],
    ['/blog/honors', '/honors'],
    ['/blog/history', '/history'],
    ['/blog/guestbook', '/guestbook'],
    ['/blog/agreement', '/agreement'],
    ['/blog/privacy', '/privacy'],
    ['/blog/auth/login', '/login'],
    ['/blog/auth/register', '/register'],
    ['/blog/auth/forgot-password', '/forgot-password'],
    ['/blog/search', '/search'],
    ['/blog/author/9', '/news'],
    ['/blog/archive', '/news'],
    ['/blog/bookmarks', '/'],
    ['/archive', '/news'],
    ['/index', '/']
  ]

  it.each(CONFIGS)('%s 把旧博客 URL 301 到当前短路径', name => {
    const rewrites = readLegacyRewrites(readConfig(name))
    for (const [from, to] of LEGACY_CASES) {
      expect(resolveLegacy(rewrites, from), from).toBe(to)
    }
  })

  it.each(CONFIGS)('%s 的 301 目标必须落在 SPA 白名单内', name => {
    const source = readConfig(name)
    const patterns = readSpaRoutePatterns(source)
    const rewrites = readLegacyRewrites(source)
    for (const [from] of LEGACY_CASES) {
      const target = resolveLegacy(rewrites, from)
      expect(target, from).not.toBeNull()
      // 跳到一个未注册路径等于把旧死链换成新死链
      expect(
        patterns.some(re => re.test(target!)),
        `${from} → ${target}`
      ).toBe(true)
    }
  })

  it.each(CONFIGS)('%s 未知 /blog 子路径不得被 301（保持 404）', name => {
    const source = readConfig(name)
    expect(resolveLegacy(readLegacyRewrites(source), '/blog/no-such-page')).toBeNull()
    expect(readSpaRoutePatterns(source).some(re => re.test('/blog/no-such-page'))).toBe(false)
  })

  it.each(CONFIGS)('%s 永久重定向必须保留查询串（replacement 不能以 ? 结尾）', name => {
    // nginx 的 rewrite ... permanent 默认追加原查询串；以 ? 结尾会主动丢弃
    for (const { pattern, replacement } of readLegacyRewrites(readConfig(name))) {
      expect(replacement.endsWith('?'), pattern.source).toBe(false)
    }
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
