import { describe, expect, it } from 'vitest'
import { applySiteAppearanceClasses } from './siteAppearance'

/** 用 Set 模拟 DOMTokenList（jsdom 的 classList 也可，但 Set 更直观） */
function createClassList(initial: string[] = []): DOMTokenList {
  const set = new Set(initial)
  const api = {
    add: (...names: string[]) => names.forEach(name => set.add(name)),
    remove: (...names: string[]) => names.forEach(name => set.delete(name)),
    toggle: (name: string, force?: boolean) => {
      const on = force ?? !set.has(name)
      if (on) set.add(name)
      else set.delete(name)
      return on
    },
    contains: (name: string) => set.has(name),
    values: () => [...set]
  }
  return api as unknown as DOMTokenList
}

describe('企业官网外观类挂载', () => {
  it('企业模板开启：挂 theme-enterprise + tpl-site + tpl-xxx', () => {
    const root = createClassList()
    applySiteAppearanceClasses(root, true, 'consult')

    expect(root.contains('theme-enterprise')).toBe(true)
    expect(root.contains('tpl-site')).toBe(true)
    expect(root.contains('tpl-consult')).toBe(true)
  })

  it('切到经典模式：必须摘掉 tpl-site（否则经典站被模板变量接管、主题色失效）', () => {
    const root = createClassList(['theme-enterprise', 'tpl-site', 'tpl-consult'])
    applySiteAppearanceClasses(root, false, 'consult')

    expect(root.contains('theme-enterprise')).toBe(false)
    expect(root.contains('tpl-site')).toBe(false)
    expect(root.contains('tpl-consult')).toBe(false)
  })

  it('切换行业模板：只保留当前模板类', () => {
    const root = createClassList(['theme-enterprise', 'tpl-site', 'tpl-consult'])
    applySiteAppearanceClasses(root, true, 'digital')

    expect(root.contains('tpl-consult')).toBe(false)
    expect(root.contains('tpl-digital')).toBe(true)
    expect(root.contains('tpl-site')).toBe(true)
  })
})
