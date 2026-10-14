import { describe, it, expect } from 'vitest'
import { buildNavMenus, hasCustomHomePage } from './siteNav'

const paths = (items: { path: string }[]) => items.map(item => item.path)

describe('hasCustomHomePage', () => {
  it('非空 slug 视为首页被接管', () => {
    expect(hasCustomHomePage('about')).toBe(true)
    expect(hasCustomHomePage('  about  ')).toBe(true)
  })

  it('空值（含纯空白、null、非字符串）视为未接管', () => {
    expect(hasCustomHomePage('')).toBe(false)
    expect(hasCustomHomePage('   ')).toBe(false)
    expect(hasCustomHomePage(null)).toBe(false)
    expect(hasCustomHomePage(undefined)).toBe(false)
    expect(hasCustomHomePage(1)).toBe(false)
  })
})

describe('buildNavMenus', () => {
  const siteMode = {
    customHomePage: false,
    guestbookEnabled: false,
    categoryEnabled: true,
    tagEnabled: true,
    archiveEnabled: true,
    businessEnabled: false,
    contactEnabled: false
  }

  it('博客模式（首页未接管）保持 首页/分类/标签/归档，不出现「文章」', () => {
    const menus = buildNavMenus(siteMode)
    expect(menus).toEqual([
      { name: '首页', path: '/' },
      { name: '分类', path: '/category' },
      { name: '标签', path: '/tag' },
      { name: '归档', path: '/archive' }
    ])
  })

  it('官网模式（首页被接管）紧跟首页补「文章」入口指向 /news', () => {
    const menus = buildNavMenus({ ...siteMode, customHomePage: true })
    expect(paths(menus)).toEqual(['/', '/news', '/category', '/tag', '/archive'])
  })

  it('导航开关关闭时对应入口被裁剪', () => {
    const menus = buildNavMenus({
      ...siteMode,
      customHomePage: true,
      categoryEnabled: false,
      tagEnabled: false,
      archiveEnabled: false
    })
    expect(menus).toEqual([
      { name: '首页', path: '/' },
      { name: '文章', path: '/news' }
    ])
  })

  it('留言板开启时插入在自定义页面前、归档后', () => {
    const menus = buildNavMenus({
      ...siteMode,
      guestbookEnabled: true,
      pages: [{ title: '关于我们', slug: 'about', showInNav: '1' }]
    })
    expect(paths(menus)).toEqual([
      '/',
      '/category',
      '/tag',
      '/archive',
      '/guestbook',
      '/page/about'
    ])
  })

  it('企业官网模板模式下出现「核心业务」入口，名称取 business_label', () => {
    const menus = buildNavMenus({ ...siteMode, businessEnabled: true, businessLabel: '产品中心' })
    expect(menus[0]).toEqual({ name: '首页', path: '/' })
    expect(menus[1]).toEqual({ name: '产品中心', path: '/business' })
  })

  it('businessLabel 缺省时回退「核心业务」，且排在文章入口之后、分类之前', () => {
    const menus = buildNavMenus({ ...siteMode, customHomePage: true, businessEnabled: true })
    expect(paths(menus)).toEqual(['/', '/news', '/business', '/category', '/tag', '/archive'])
  })

  it('企业模板模式下核心业务之后跟「联系我们」', () => {
    const menus = buildNavMenus({ ...siteMode, businessEnabled: true, contactEnabled: true })
    expect(paths(menus)).toEqual(['/', '/business', '/contact', '/category', '/tag', '/archive'])
  })

  it('只有 show_in_nav=1 且 slug 非空的页面进入导航，标题缺失时回退 slug', () => {
    const menus = buildNavMenus({
      ...siteMode,
      pages: [
        { title: '不展示', slug: 'hidden', showInNav: '0' },
        { title: '没有 slug', slug: '', showInNav: '1' },
        { title: '', slug: 'fallback', showInNav: '1' }
      ]
    })
    expect(menus.at(-1)).toEqual({ name: 'fallback', path: '/page/fallback' })
    expect(paths(menus).filter(path => path.startsWith('/page/'))).toEqual(['/page/fallback'])
  })

  it('所有导航地址都使用短路径（不出现 /blog 前缀）', () => {
    const menus = buildNavMenus({
      ...siteMode,
      customHomePage: true,
      guestbookEnabled: true,
      businessEnabled: true,
      contactEnabled: true,
      pages: [{ title: '关于我们', slug: 'about', showInNav: '1' }]
    })
    const legacy = paths(menus).filter(path => path.startsWith('/blog'))

    expect(legacy).toEqual([])
  })
})
