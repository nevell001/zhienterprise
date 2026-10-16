import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { DEFAULT_FOOTER_NAV, DEFAULT_HEADER_NAV, type SiteNavItem } from './siteNav'
import siteRoutes from '@/router/site'

/**
 * 兜底导航（site_nav_item 为空或接口异常时前台只剩这一份）的三道守卫。
 *
 * 注：前两条是「两份独立清单之间的跨产物一致性」校验（清单本身是文本/数据，
 * 行为断言覆盖不到），第三条是路由可达性；这是 AGENTS.md 允许保留源级断言的例外。
 */

interface NavTuple {
  label: string
  linkType: string
  linkValue: string
  sort: number
}

const SQL = resolve(process.cwd(), '../sql/00_init_database.sql')

/** 去掉 `SELECT 'x' AS location, ...` 这类列别名，让首行与后续 UNION ALL 行同形 */
function seedBlock(from: string, to: string): string {
  const source = readFileSync(SQL, 'utf-8')
  const start = source.indexOf(from)
  const end = source.indexOf(to)
  expect(start, `种子注释锚点丢失：${from}`).toBeGreaterThan(-1)
  expect(end, `种子注释锚点丢失：${to}`).toBeGreaterThan(start)
  return source.slice(start, end).replace(/\s+AS\s+(location|label|link_type|link_value|sort)/g, '')
}

/** 顶级项种子元组（location, label, link_type, link_value, sort），按位置取用 */
function seededTopTuples(from: string, to: string, location: 'header' | 'footer'): NavTuple[] {
  return [
    ...seedBlock(from, to).matchAll(
      /'(header|footer)',\s*'([^']+)',\s*'([^']+)',\s*'([^']+)',\s*(\d+)/g
    )
  ]
    .filter(m => m[1] === location)
    .map(([, , label, linkType, linkValue, sort]) => ({
      label,
      linkType,
      linkValue,
      sort: Number(sort)
    }))
}

/** 二级项种子元组（label, link_value, sort）；link_type 由外层 INSERT 统一写 'path' */
function seededChildTuples(from: string, to: string): NavTuple[] {
  return [
    ...seedBlock(from, to).matchAll(/(?:SELECT|UNION ALL SELECT)\s+'([^']+)',\s*'([^']+)',\s*(\d+)/g)
  ].map(([, label, linkValue, sort]) => ({
    label,
    linkType: 'path',
    linkValue,
    sort: Number(sort)
  }))
}

/** 兜底清单按数组顺序即为展示顺序，排序号取位序 +1（与种子的 sort 同义） */
function toTuples(items: SiteNavItem[]): NavTuple[] {
  return items.map((item, index) => ({
    label: item.label,
    linkType: item.linkType || 'path',
    linkValue: item.linkValue || '/',
    sort: index + 1
  }))
}

/** 兜底导航里的全部站内路径链接（含二级项，外链与 cta 除外） */
function pathLinks(items: SiteNavItem[]): string[] {
  return items
    .filter(item => (item.linkType || 'path') === 'path')
    .flatMap(item => [item.linkValue || '/', ...pathLinks(item.children || [])])
}

describe('企业官网兜底导航（DEFAULT_HEADER_NAV / DEFAULT_FOOTER_NAV）', () => {
  it('兜底导航覆盖企业模板的全部板块入口', () => {
    // 曾漏掉「解决方案」：/solutions 有独立板块与路由，兜底导航却没有入口
    expect(pathLinks(DEFAULT_HEADER_NAV)).toEqual(
      expect.arrayContaining(['/', '/business', '/solutions', '/news', '/about', '/contact'])
    )
    expect(pathLinks(DEFAULT_FOOTER_NAV)).toContain('/solutions')
  })

  it('兜底导航的每个链接都能在路由表里命中', () => {
    // 站点首页 '/' 定义在 router/index.ts（PublicRoot），不在 siteRoutes 里
    const registered = new Set([...siteRoutes.map(route => route.path), '/'])
    const unresolvable = [...pathLinks(DEFAULT_HEADER_NAV), ...pathLinks(DEFAULT_FOOTER_NAV)].filter(
      link => !registered.has(link)
    )
    expect(unresolvable, `兜底导航指向了不存在的路由：${unresolvable.join(', ')}`).toEqual([])
  })

  it('与 SQL 导航种子的清单逐项一致', () => {
    expect(toTuples(DEFAULT_HEADER_NAV)).toEqual(
      seededTopTuples('-- 头部/页脚导航默认项', '-- 关于我们的二级下拉项', 'header')
    )
    expect(toTuples(DEFAULT_FOOTER_NAV)).toEqual(
      seededTopTuples('-- 头部/页脚导航默认项', '-- 关于我们的二级下拉项', 'footer')
    )

    const about = DEFAULT_HEADER_NAV.find(item => item.label === '关于我们')
    expect(toTuples(about?.children || [])).toEqual(
      seededChildTuples('-- 关于我们的二级下拉项', '-- 导航管理菜单与权限')
    )
  })
})
