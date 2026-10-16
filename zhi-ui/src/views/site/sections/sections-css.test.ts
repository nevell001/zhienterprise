import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * sections.css 结构守卫（源文本契约检查）。
 *
 * 为什么是源文本级：CSS 写错选择器或过渡值时组件照常渲染、无任何报错，
 * 挂载行为测试读不到「样式从未生效」——只有计算样式会悄悄失效。
 * 这个文件在 M1 期被一次批量替换污染过：`.dark`/`.ico`/`.2s` 等 token
 * 被展开成 `.tpl-site X`（如 `transform 0tpl-site 0.2s`、`.pad.tpl-site .dark`），
 * 深色区块底色与全部悬停过渡在 builder 首页失效。本测试锁定文件头声明的
 * 约定：选择器统一 .tpl-site 前缀（且只出现一次）、声明值里绝不出现类名。
 */
const css = readFileSync(resolve(__dirname, 'sections.css'), 'utf8')
const source = css.replace(/\/\*[\s\S]*?\*\//g, '')

interface CssRule {
  selector: string
  body: string
}

/** 极简 CSS 规则解析：忽略注释，能正确处理 @media 嵌套（与 dark-mode-tokens.test.ts 同款） */
function parseRules(input: string): CssRule[] {
  const rules: CssRule[] = []
  const stack: string[] = []
  let buffer = ''
  for (const ch of input) {
    if (ch === '{') {
      stack.push(buffer.trim())
      buffer = ''
    } else if (ch === '}') {
      const selector = stack.pop() ?? ''
      if (selector && !selector.startsWith('@')) {
        rules.push({ selector, body: buffer })
      }
      buffer = ''
    } else {
      buffer += ch
    }
  }
  return rules
}

const rules = parseRules(source)
const normalize = (selector: string) => selector.replace(/\s+/g, ' ')

describe('sections.css 结构守卫', () => {
  it('扫描应覆盖全部规则（解析器失效时直接失败）', () => {
    expect(rules.length).toBeGreaterThan(45)
  })

  it('每个选择器以 .tpl-site 开头且只含一个 .tpl-site（防 .pad.tpl-site / li.tpl-site 类污染）', () => {
    // 例外：html.motion-off 是 §4.7 手动「减弱动效」开关的 <html> 级降级前缀
    // （有意作用于全站，非类连接污染），允许其后代选择器以它开头。
    const allowedPrefix = 'html.motion-off .tpl-site'
    const bad = rules
      .filter(rule =>
        rule.selector.split(',').some(part => {
          const trimmed = part.trim()
          const count = trimmed.match(/\.tpl-site\b/g)?.length ?? 0
          if (trimmed.startsWith(allowedPrefix)) return false
          return !trimmed.startsWith('.tpl-site') || count !== 1
        })
      )
      .map(rule => normalize(rule.selector))
    expect(bad).toEqual([])
  })

  it('声明值不得包含 tpl-site（防 0tpl-site 0.2s 类被写坏的过渡值）', () => {
    const bad = rules
      .filter(rule => rule.body.includes('tpl-site'))
      .map(rule => normalize(rule.selector))
    expect(bad).toEqual([])
  })

  it('类名不得与标识符粘连、括号必须配平', () => {
    expect(source).not.toMatch(/[A-Za-z0-9]tpl-site/)
    expect((source.match(/{/g) ?? []).length).toBe((source.match(/}/g) ?? []).length)
  })
})
