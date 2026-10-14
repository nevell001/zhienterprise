import { describe, it, expect } from 'vitest'
import { buildNavMenus, hasCustomHomePage } from './blogNav'

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
  const blogMode = {
    customHomePage: false,
    guestbookEnabled: false,
    categoryEnabled: true,
    tagEnabled: true,
    archiveEnabled: true,
    businessEnabled: false,
    contactEnabled: false
  }

  it('博客模式（首页未接管）保持 首页/分类/标签/归档，不出现「文章」', () => {
    const menus = buildNavMenus(blogMode)
    expect(menus).toEqual([
      { name: '首页', path: '/blog' },
      { name: '分类', path: '/blog/category' },
      { name: '标签', path: '/blog/tag' },
      { name: '归档', path: '/blog/archive' }
    ])
  })

  it('官网模式（首页被接管）紧跟首页补「文章」入口指向 /blog/articles', () => {
    const menus = buildNavMenus({ ...blogMode, customHomePage: true })
    expect(paths(menus)).toEqual([
      '/blog',
      '/blog/articles',
      '/blog/category',
      '/blog/tag',
      '/blog/archive'
    ])
  })

  it('导航开关关闭时对应入口被裁剪', () => {
    const menus = buildNavMenus({
      ...blogMode,
      customHomePage: true,
      categoryEnabled: false,
      tagEnabled: false,
      archiveEnabled: false
    })
    expect(menus).toEqual([
      { name: '首页', path: '/blog' },
      { name: '文章', path: '/blog/articles' }
    ])
  })

  it('留言板开启时插入在自定义页面前、归档后', () => {
    const menus = buildNavMenus({
      ...blogMode,
      guestbookEnabled: true,
      pages: [{ title: '关于我们', slug: 'about', showInNav: '1' }]
    })
    expect(paths(menus)).toEqual([
      '/blog',
      '/blog/category',
      '/blog/tag',
      '/blog/archive',
      '/blog/guestbook',
      '/blog/page/about'
    ])
  })

  it('企业官网模板模式下出现「核心业务」入口，名称取 business_label', () => {
    const menus = buildNavMenus({ ...blogMode, businessEnabled: true, businessLabel: '产品中心' })
    expect(menus[0]).toEqual({ name: '首页', path: '/blog' })
    expect(menus[1]).toEqual({ name: '产品中心', path: '/blog/business' })
  })

  it('businessLabel 缺省时回退「核心业务」，且排在文章入口之后、分类之前', () => {
    const menus = buildNavMenus({ ...blogMode, customHomePage: true, businessEnabled: true })
    expect(paths(menus)).toEqual([
      '/blog',
      '/blog/articles',
      '/blog/business',
      '/blog/category',
      '/blog/tag',
      '/blog/archive'
    ])
  })

  it('企业模板模式下核心业务之后跟「联系我们」', () => {
    const menus = buildNavMenus({ ...blogMode, businessEnabled: true, contactEnabled: true })
    expect(paths(menus)).toEqual([
      '/blog',
      '/blog/business',
      '/blog/contact',
      '/blog/category',
      '/blog/tag',
      '/blog/archive'
    ])
  })

  it('只有 show_in_nav=1 且 slug 非空的页面进入导航，标题缺失时回退 slug', () => {
    const menus = buildNavMenus({
      ...blogMode,
      pages: [
        { title: '不展示', slug: 'hidden', showInNav: '0' },
        { title: '没有 slug', slug: '', showInNav: '1' },
        { title: '', slug: 'fallback', showInNav: '1' }
      ]
    })
    expect(menus.at(-1)).toEqual({ name: 'fallback', path: '/blog/page/fallback' })
    expect(paths(menus).filter(path => path.startsWith('/blog/page/'))).toEqual([
      '/blog/page/fallback'
    ])
  })
})
