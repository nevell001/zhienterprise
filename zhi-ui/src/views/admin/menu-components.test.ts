import { describe, expect, it } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * 后台菜单 → 前端视图 的存在性守卫（跨制品契约：一侧是 SQL 种子，一侧是视图文件）。
 *
 * 为什么不用行为断言：菜单是运行期由后端按 sys_menu 下发的，前端 `loadView`
 * 找不到组件时只在 console.error 后返回 undefined —— 组件路径写错的表现是
 * 页面白屏/404，单测环境里没有真实菜单数据可比。这里改做静态跨文件比对：
 * 种子里每条 C 类菜单的 component 都必须能找到视图文件，解析顺序与 loadView
 * 一致（views/admin/<component>.vue 优先，回退 views/site/<component>.vue）。
 *
 * 历史：站点管理菜单的 component 曾长期是 blog/xxx/index，视图目录迁到
 * views/admin/* 后只要漏改一条，对应后台菜单就会 404。
 */
const SQL = resolve(process.cwd(), '../sql/00_init_database.sql')
const VIEWS = resolve(process.cwd(), 'src/views')

interface SeededMenu {
  name: string
  path: string
  component: string
  type: string
}

/** 种子中非注释的 sys_menu 元组：(菜单名, path, component, menu_type) */
function seededMenus(): SeededMenu[] {
  const menus: SeededMenu[] = []
  for (const raw of readFileSync(SQL, 'utf-8').split('\n')) {
    const line = raw.trim()
    // 注释掉的菜单不算：如已删除页面的 druid / 表单构建，源码里没有对应视图
    if (line.startsWith('--')) continue
    const m = line.match(
      /\(\s*'?\d+'?,\s*'([^']*)',\s*'?\d+'?,\s*'?\d+'?,\s*'([^']*)',\s*(?:NULL|'([^']*)')/
    )
    if (!m) continue
    const type = (line.match(/'(?:M|C|F)'/) || [''])[0].replace(/'/g, '')
    menus.push({ name: m[1], path: m[2], component: m[3] || '', type })
  }
  return menus
}

describe('后台菜单组件路径与视图文件一致', () => {
  const menus = seededMenus()

  it('扫描应覆盖种子里的菜单（防止正则失效导致空集合通过）', () => {
    expect(menus.length).toBeGreaterThan(40)
    expect(menus.filter(m => m.type === 'C').length).toBeGreaterThan(25)
  })

  it('每条 C 类菜单的 component 都要有对应视图文件', () => {
    const missing = menus
      .filter(m => m.type === 'C' && m.component)
      .filter(
        m =>
          !existsSync(resolve(VIEWS, 'admin', `${m.component}.vue`)) &&
          !existsSync(resolve(VIEWS, 'blog', `${m.component}.vue`))
      )
      .map(m => `${m.name} → ${m.component}`)

    expect(
      missing,
      `以下后台菜单的组件文件不存在，点击会 404（要么补页面，要么改 component）：\n  ${missing.join('\n  ')}`
    ).toEqual([])
  })

  it('C 类菜单的 component 不得带 blog/ 前缀（视图目录在 views/admin/* 下）', () => {
    const stale = menus
      .filter(m => m.type === 'C' && m.component.startsWith('blog/'))
      .map(m => `${m.name} → ${m.component}`)

    expect(stale).toEqual([])
  })
})
