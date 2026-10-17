import { describe, expect, it } from 'vitest'
import { normalizeUploadUrl } from './uploadUrl'

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
