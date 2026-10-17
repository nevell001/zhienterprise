import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * 前台路由页面「必须挂 SiteLayout」契约。
 *
 * 为什么是源文本级：这是**路由表 ↔ 页面文件**的跨产物契约（路由里配的每个
 * 页面组件都必须自己包 SiteLayout，导航/页脚/主题类都由它挂载）。用组件挂载
 * 测不到"某个新页面临时忘了包"这类遗漏 —— 页面照样渲染，只是整站导航消失。
 * 这个 bug 真实发生过：`views/site/solutions/index.vue` 漏了 SiteLayout，
 * /solutions 直接没有顶部菜单、没有页脚，`<html>` 也没有企业主题类。
 *
 * 例外：`views/site/index.vue` 等页面内部渲染的**子组件**（EnterpriseHome /
 * EnterpriseNewsList / PageRenderer / sections/*）不在此列 —— 它们由父页面
 * 的 SiteLayout 提供导航，路由表里也不会直接指向它们。
 */

const SITE_ROUTES = resolve(process.cwd(), 'src/router/site.ts')
const UI_ROOT = resolve(process.cwd(), 'src')

/** 从路由表里抽出所有 `component: () => import('@/views/...')` 的目标文件 */
function routedComponents(): string[] {
  const source = readFileSync(SITE_ROUTES, 'utf-8')
  const re = /component:\s*\(\)\s*=>\s*import\('(@\/[^']+\.vue)'\)/g
  return [...new Set([...source.matchAll(re)].map(m => m[1]))].sort()
}

describe('前台路由页面布局契约', () => {
  it('路由表里的每个页面都必须自己包裹 SiteLayout', () => {
    const components = routedComponents()

    // 防正则失效导致"空集合也通过"：当前前台路由有 20+ 个页面组件
    expect(components.length).toBeGreaterThan(15)

    const missing = components.filter(alias => {
      const file = resolve(UI_ROOT, alias.replace('@/', ''))
      const source = readFileSync(file, 'utf-8')
      return !source.includes('<SiteLayout')
    })

    expect(
      missing,
      `以下前台路由页面缺少 <SiteLayout>（会导致该页没有导航/页脚/企业主题）：${missing.join(', ')}`
    ).toEqual([])
  })

  it('解决方案页必须复用站点布局（回归：曾漏 SiteLayout 导致菜单消失）', () => {
    const solutions = resolve(UI_ROOT, 'views/site/solutions/index.vue')
    const source = readFileSync(solutions, 'utf-8')

    expect(source).toContain('<SiteLayout>')
    expect(source).toContain("import SiteLayout from '@/components/SiteLayout.vue'")
  })
})
