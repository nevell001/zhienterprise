import { describe, expect, it } from 'vitest'
import { articleDetailPath } from './articlePath'

describe('articleDetailPath', () => {
  it('资讯与未知类型走资讯详情', () => {
    expect(articleDetailPath({ id: 42 })).toBe('/article/42')
    expect(articleDetailPath({ id: 42, articleType: 'news' })).toBe('/article/42')
    expect(articleDetailPath({ id: 42, articleType: 'unknown' })).toBe('/article/42')
  })

  it('业务与案例共用业务详情，解决方案走方案详情', () => {
    expect(articleDetailPath({ id: 42, articleType: 'business' })).toBe('/business/42')
    expect(articleDetailPath({ id: 42, articleType: 'case' })).toBe('/business/42')
    expect(articleDetailPath({ id: 42, articleType: 'solution' })).toBe('/solutions/42')
  })

  it('有 slug 时优先使用 slug，与 sitemap 的规范入口一致', () => {
    expect(articleDetailPath({ id: 42, articleType: 'business', slug: ' smart-factory ' })).toBe(
      '/business/smart-factory'
    )
    expect(articleDetailPath({ id: 42, articleType: 'solution', slug: ' ' })).toBe('/solutions/42')
  })

  it('缺少 id 与 slug 时回退首页，避免生成无效路径', () => {
    expect(articleDetailPath({})).toBe('/')
  })
})
