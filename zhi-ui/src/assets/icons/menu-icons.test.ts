import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * 菜单图标契约守卫（跨 SQL 种子 ↔ 前端 svg 图标集）。
 *
 * 侧栏菜单图标统一由 `components/SvgIcon` 渲染成 `<use xlink:href="#icon-xxx">`，
 * 图标名必须在 `src/assets/icons/svg/` 里有对应文件；否则 svg 为空，菜单会出现
 * 一块看不见的空白（「解决方案管理」就曾用 `lightning`，前端根本没有这个图标）。
 *
 * 这是天然的跨文件契约检查，行为测试无法覆盖：只有把 SQL 种子里声明的图标
 * 与实际的 svg 资源比对，才能在改菜单数据时提前失败。
 */
const ICON_DIR = resolve(process.cwd(), 'src/assets/icons/svg')
const SQL_PATH = resolve(process.cwd(), '../sql/00_init_database.sql')

/** svg 文件表（去掉扩展名），与 vite-plugin-svg-icons 的 symbol id 一致 */
const iconFiles = new Set(
  readdirSync(ICON_DIR)
    .filter(file => file.toLowerCase().endsWith('.svg'))
    .map(file => file.replace(/\.svg$/i, ''))
)

/** 从 sys_menu 的 INSERT 语句中提取 icon 列（icon 后紧跟 create_by='admin'） */
function extractMenuIcons(sql: string): Set<string> {
  const icons = new Set<string>()
  const statements = sql
    .split(';')
    .filter(statement => /INSERT\s+(?:IGNORE\s+)?INTO\s+sys_menu\b/i.test(statement))

  for (const statement of statements) {
    for (const match of statement.matchAll(/,\s*'([^']*)'\s*,\s*'admin'\s*,\s*NOW\(\)/g)) {
      const icon = match[1].trim()
      if (icon && icon !== '#' && !icon.startsWith('#')) {
        icons.add(icon)
      }
    }
  }
  return icons
}

describe('菜单图标资源契约', () => {
  it('SQL 种子里的 sys_menu 图标都能在前端 svg 图标集中找到', () => {
    const sql = readFileSync(SQL_PATH, 'utf-8')
    const icons = extractMenuIcons(sql)

    expect(icons.size).toBeGreaterThan(0)
    const missing = [...icons].filter(icon => !iconFiles.has(icon))
    expect(missing).toEqual([])
  })

  it('解决方案管理使用存在的图标（回归：曾配置不存在的 lightning）', () => {
    const sql = readFileSync(SQL_PATH, 'utf-8')
    // 只检查 2012 这条种子的 icon 字面量，防止再被改回不存在的图标
    const match = sql.match(/VALUES\s*\(\s*2012\s*,[^;]*?'admin'\s*,\s*NOW\(\)/s)
    expect(match).toBeTruthy()
    const icon = match![0].match(/,\s*'([^']*)'\s*,\s*'admin'\s*,\s*NOW\(\)/)?.[1]
    expect(icon).toBeTruthy()
    expect(iconFiles.has(icon!)).toBe(true)
  })
})
