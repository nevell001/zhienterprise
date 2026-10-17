import { describe, it, expect } from 'vitest'
import siteRoutes from '@/router/site'
import {
  buildNavMenus,
  hasCustomHomePage,
  pagePath,
  pageNavPaths,
  pageSlugFromPath,
  preferredPageNavPath,
  flattenNavTree,
  navItemsOfPage,
  nextTopSort
} from './siteNav'

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

describe('pagePath / pageSlugFromPath', () => {
  it('slug 与访问路径互转', () => {
    expect(pagePath('about-us')).toBe('/page/about-us')
    expect(pageSlugFromPath('/page/about-us')).toBe('about-us')
    expect(pageSlugFromPath('/page/about-us/')).toBe('about-us')
    expect(pageSlugFromPath('  /page/honors  ')).toBe('honors')
  })

  it('非 /page/ 路径与其他入口不误判', () => {
    expect(pageSlugFromPath('/about')).toBe('')
    expect(pageSlugFromPath('/business')).toBe('')
    expect(pageSlugFromPath('https://example.com/page/x')).toBe('')
    expect(pageSlugFromPath('')).toBe('')
    expect(pageSlugFromPath(null)).toBe('')
    expect(pageSlugFromPath(undefined)).toBe('')
    expect(pageSlugFromPath(42)).toBe('')
  })

  it('slug 字符集与后端约束一致（字母/数字/下划线/连字符）', () => {
    expect(pageSlugFromPath('/page/a_b-1')).toBe('a_b-1')
    expect(pageSlugFromPath('/page/中文')).toBe('')
    expect(pageSlugFromPath('/page/a b')).toBe('')
  })
})

describe('pageNavPaths / preferredPageNavPath', () => {
  it('模板页短路径排在 /page/{slug} 之前', () => {
    expect(pageNavPaths('about')).toEqual(['/about', '/page/about'])
    expect(pageNavPaths('honors')).toEqual(['/honors', '/page/honors'])
    expect(pageNavPaths('home')).toEqual(['/', '/page/home'])
    expect(preferredPageNavPath('about')).toBe('/about')
  })

  it('非模板页只有 /page/{slug}', () => {
    expect(pageNavPaths('terms')).toEqual(['/page/terms'])
    expect(preferredPageNavPath('terms')).toBe('/page/terms')
  })

  it('空 slug 不产生路径', () => {
    expect(pageNavPaths('')).toEqual([])
  })

  it('模板页短路径必须都是站点真实路由（与 router/site.ts 对齐）', () => {
    // /page/{slug} 在路由表里写作 /page/:slug，只有短路径需要逐个核对
    const registered = new Set([...siteRoutes.map(route => route.path), '/'])
    const aliases = ['home', 'about', 'honors'].map(slug => preferredPageNavPath(slug))
    expect(aliases.filter(path => !registered.has(path))).toEqual([])
  })
})

describe('导航树联动（页面管理 ↔ 导航管理）', () => {
  const tree = [
    { id: 1, parentId: 0, label: '首页', linkValue: '/', sort: 1 },
    {
      id: 4,
      parentId: 0,
      label: '关于我们',
      linkValue: '/page/about',
      sort: 4,
      children: [
        { id: 41, parentId: 4, label: '企业简介', linkValue: '/page/about', sort: 1 },
        { id: 42, parentId: 4, label: '荣誉资质', linkValue: '/honors', sort: 2 }
      ]
    },
    { id: 5, parentId: 0, label: '服务条款', linkValue: '/page/terms', sort: 5 }
  ]

  it('flattenNavTree 含二级项且保持顺序', () => {
    expect(flattenNavTree(tree).map(i => i.label)).toEqual([
      '首页',
      '关于我们',
      '企业简介',
      '荣誉资质',
      '服务条款'
    ])
  })

  it('navItemsOfPage 找出指向该页面的全部项（含二级项与重复项）', () => {
    expect(navItemsOfPage(tree, 'about').map(i => i.id)).toEqual([4, 41])
    expect(navItemsOfPage(tree, 'terms').map(i => i.id)).toEqual([5])
    expect(navItemsOfPage(tree, 'nope')).toEqual([])
    expect(navItemsOfPage(tree, '')).toEqual([])
  })

  it('模板页短路径同样命中，且两种写法一起算重复项', () => {
    const mixed = [
      { id: 4, parentId: 0, label: '关于我们', linkValue: '/about' },
      { id: 5, parentId: 0, label: '关于我们', linkValue: '/page/about/' },
      { id: 6, parentId: 0, label: '首页', linkValue: '/' }
    ]
    // 尾斜杠/空白差异不算另一页（导航管理允许手输）
    expect(navItemsOfPage(mixed, 'about').map(i => i.id)).toEqual([4, 5])
    expect(navItemsOfPage(mixed, 'home').map(i => i.id)).toEqual([6])
  })

  it('nextTopSort 取顶级项最大排序号 +1（不受二级项影响）', () => {
    expect(nextTopSort(tree)).toBe(6)
    expect(nextTopSort([{ id: 9, parentId: 0, sort: 8 }])).toBe(9)
    expect(nextTopSort([])).toBe(1)
  })
})

describe('buildNavMenus', () => {
  const siteMode = {
    customHomePage: false,
    guestbookEnabled: false,
    categoryEnabled: true,
    tagEnabled: true,
    businessEnabled: false,
    contactEnabled: false
  }

  it('博客模式（首页未接管）保持 首页/分类/标签，不出现「文章」', () => {
    const menus = buildNavMenus(siteMode)
    expect(menus).toEqual([
      { name: '首页', path: '/' },
      { name: '分类', path: '/category' },
      { name: '标签', path: '/tag' }
    ])
  })

  it('官网模式（首页被接管）紧跟首页补「文章」入口指向 /news', () => {
    const menus = buildNavMenus({ ...siteMode, customHomePage: true })
    expect(paths(menus)).toEqual(['/', '/news', '/category', '/tag'])
  })

  it('导航开关关闭时对应入口被裁剪', () => {
    const menus = buildNavMenus({
      ...siteMode,
      customHomePage: true,
      categoryEnabled: false,
      tagEnabled: false
    })
    expect(menus).toEqual([
      { name: '首页', path: '/' },
      { name: '文章', path: '/news' }
    ])
  })

  it('留言板开启时插入在自定义页面前、标签后', () => {
    const menus = buildNavMenus({
      ...siteMode,
      guestbookEnabled: true,
      pages: [{ title: '关于我们', slug: 'about', showInNav: '1' }]
    })
    expect(paths(menus)).toEqual(['/', '/category', '/tag', '/guestbook', '/page/about'])
  })

  it('企业官网模板模式下出现「核心业务」入口，名称取 business_label', () => {
    const menus = buildNavMenus({ ...siteMode, businessEnabled: true, businessLabel: '产品中心' })
    expect(menus[0]).toEqual({ name: '首页', path: '/' })
    expect(menus[1]).toEqual({ name: '产品中心', path: '/business' })
  })

  it('businessLabel 缺省时回退「核心业务」，且排在文章入口之后、分类之前', () => {
    const menus = buildNavMenus({ ...siteMode, customHomePage: true, businessEnabled: true })
    expect(paths(menus)).toEqual(['/', '/news', '/business', '/category', '/tag'])
  })

  it('企业模板模式下核心业务之后跟「联系我们」', () => {
    const menus = buildNavMenus({ ...siteMode, businessEnabled: true, contactEnabled: true })
    expect(paths(menus)).toEqual(['/', '/business', '/contact', '/category', '/tag'])
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
