import { describe, expect, it } from 'vitest'
import { MAX_BANNER_IMAGES, normalizeUploadUrl, parseBannerUrls } from './uploadUrl'

/**
 * 上传图片地址规范化。
 *
 * 真实缺陷：后端 `/common/upload` 返回 `url` 是「请求 Host + 路径」的绝对地址。
 * 请求经 Docker/Vite 代理时 Host 是容器名（`zhi-admin:8080`），浏览器解析不了，
 * 图片 `naturalWidth = 0`；而上传组件又把这串绝对地址存进了站点设置。
 */
describe('normalizeUploadUrl', () => {
  it('站内相对路径原样返回（后端 fileName 的正常形态）', () => {
    expect(normalizeUploadUrl('/profile/upload/2026/10/17/logo.png')).toBe(
      '/profile/upload/2026/10/17/logo.png'
    )
  })

  it('容器名绝对地址收敛为路径（Docker/Vite 代理下的真实回归值）', () => {
    expect(
      normalizeUploadUrl('http://zhi-admin:8080/profile/upload/2026/10/17/logo_A002.png')
    ).toBe('/profile/upload/2026/10/17/logo_A002.png')
  })

  it('内网 IP / 域名 / 端口都收敛为路径', () => {
    expect(normalizeUploadUrl('http://127.0.0.1:8080/profile/upload/b.png')).toBe(
      '/profile/upload/b.png'
    )
    expect(normalizeUploadUrl('https://www.example.com/profile/upload/c.png')).toBe(
      '/profile/upload/c.png'
    )
  })

  it('带查询串的地址保留 search（如 CDN 版本参数）', () => {
    expect(normalizeUploadUrl('https://cdn.example.com/profile/a.png?v=2')).toBe(
      '/profile/a.png?v=2'
    )
  })

  it('data: / blob: 内联地址原样返回', () => {
    expect(normalizeUploadUrl('data:image/png;base64,AAAA')).toBe('data:image/png;base64,AAAA')
    expect(normalizeUploadUrl('blob:http://localhost/abc')).toBe('blob:http://localhost/abc')
  })

  it('空值/空白返回空串（避免渲染出 src="" 的破图）', () => {
    expect(normalizeUploadUrl('')).toBe('')
    expect(normalizeUploadUrl('   ')).toBe('')
    expect(normalizeUploadUrl(null)).toBe('')
    expect(normalizeUploadUrl(undefined)).toBe('')
  })

  it('首尾空白会被裁剪', () => {
    expect(normalizeUploadUrl('  /profile/upload/d.png  ')).toBe('/profile/upload/d.png')
  })
})

describe('parseBannerUrls 首页 Banner 多图解析', () => {
  it('历史单图值解析为一张（无需数据迁移）', () => {
    expect(parseBannerUrls('/profile/upload/a.png')).toEqual(['/profile/upload/a.png'])
  })

  it('逗号分隔的多图按顺序解析，并裁剪空白', () => {
    expect(parseBannerUrls(' /profile/upload/a.png , /profile/upload/b.png ')).toEqual([
      '/profile/upload/a.png',
      '/profile/upload/b.png'
    ])
  })

  it('兼容容器名绝对地址（逐项收敛）', () => {
    expect(
      parseBannerUrls('http://zhi-admin:8080/profile/upload/a.png,/profile/upload/b.png')
    ).toEqual(['/profile/upload/a.png', '/profile/upload/b.png'])
  })

  it('去重并忽略空项', () => {
    expect(parseBannerUrls('/a.png,,/a.png,/b.png,')).toEqual(['/a.png', '/b.png'])
  })

  it('超过上限截断（默认 5 张）', () => {
    const six = Array.from({ length: 6 }, (_, i) => `/p/${i}.png`).join(',')
    expect(parseBannerUrls(six)).toHaveLength(MAX_BANNER_IMAGES)
    expect(parseBannerUrls(six, 2)).toEqual(['/p/0.png', '/p/1.png'])
  })

  it('data:/blob: 内联地址不按逗号拆分（自身含逗号）', () => {
    const dataUri = 'data:image/svg+xml;base64,PHN2Zy8+'
    expect(parseBannerUrls(dataUri)).toEqual([dataUri])
    expect(parseBannerUrls(` ${dataUri} `)).toEqual([dataUri])
  })

  it('空值/空数组返回空列表', () => {
    expect(parseBannerUrls('')).toEqual([])
    expect(parseBannerUrls('   ')).toEqual([])
    expect(parseBannerUrls(null)).toEqual([])
    expect(parseBannerUrls([])).toEqual([])
  })
})
