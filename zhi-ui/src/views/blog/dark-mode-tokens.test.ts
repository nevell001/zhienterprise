import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'

/**
 * 深色模式下 --mo-* 色阶会被重映射（见 src/assets/styles/theme-dark.css）：
 *   n0 / n50 / n800 / n900  → 背景色（深色下是深色！）
 *   n100 / n200 / n600      → 主文字色
 *   n300 / n400 / n500      → 次要文字色
 *   p300 / p500             → 主题主色（默认主题 = --el-color-primary，Mo-Blog = #a5b4fc）
 *   p600 / p700 / p800 / p900 → 不参与重映射，仍是浅色主题的深靛蓝
 *
 * 因此在 `html.dark` 规则里把「背景色阶」或「未重映射的深色强调色」当作 `color` 用，
 * 深色模式下文字会几乎看不见（曾导致关于页/留言板标题对比度降到 1.0:1）。
 * 本测试锁定这条约定，防止再次写回。
 */
const BG_SCALE_AS_TEXT = /(?:^|[;{\s])color:\s*var\(--mo-n(?:0|50|800|900)\)/
const DARK_ACCENT_AS_TEXT = /(?:^|[;{\s])color:\s*var\(--mo-p(?:600|700|800|900)\)/

interface CssRule {
  selector: string
  body: string
}

function collectVueFiles(dir: string): string[] {
  const found: string[] = []
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) {
      found.push(...collectVueFiles(full))
    } else if (full.endsWith('.vue')) {
      found.push(full)
    }
  }
  return found
}

/** 极简 CSS 规则解析：忽略注释，能正确处理 @media 嵌套 */
function parseRules(css: string): CssRule[] {
  const rules: CssRule[] = []
  const source = css.replace(/\/\*[\s\S]*?\*\//g, '')
  const stack: string[] = []
  let buffer = ''
  for (const ch of source) {
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

// 覆盖博客前台页面与公共组件（后台编辑器有自己的一套作用域内色阶映射，另行约定）
const blogRoot = resolve(__dirname)
const files = [
  ...collectVueFiles(blogRoot),
  ...collectVueFiles(resolve(__dirname, '../../components'))
].sort()

const violations = (pattern: RegExp) =>
  files.flatMap(file =>
    parseRules(readFileSync(file, 'utf8'))
      .filter(rule => rule.selector.includes('html.dark') && pattern.test(rule.body))
      .map(rule => `${file.replace(`${blogRoot}/`, '')} → ${rule.selector.replace(/\s+/g, ' ')}`)
  )

/**
 * 浅色模式下 n300/n400 是浅灰、p200/p300 是淡紫，直接当文字用只有 1.5~2.5:1
 * （曾导致首页元信息、分类/标签计数、文章正文链接等 169 处不达标）。
 * 结构性图标（空态插画）不参与该约定。
 */
const PALE_SCALE_AS_TEXT = /(?:^|[;{\s])color:\s*var\(--mo-(?:n300|n400|p200|p300)\)/
const DECORATIVE_ICON_SELECTORS = ['.empty-icon', '.not-found-icon']

const lightViolations = () =>
  files.flatMap(file =>
    parseRules(readFileSync(file, 'utf8'))
      .filter(
        rule =>
          !rule.selector.includes('html.dark') &&
          !DECORATIVE_ICON_SELECTORS.some(icon => rule.selector.includes(icon)) &&
          PALE_SCALE_AS_TEXT.test(rule.body)
      )
      .map(rule => `${file.replace(`${blogRoot}/`, '')} → ${rule.selector.replace(/\s+/g, ' ')}`)
  )

describe('深色模式 --mo-* 色阶使用约定', () => {
  it('深色规则不得把背景色阶（n0/n50/n800/n900）当作文字颜色', () => {
    expect(violations(BG_SCALE_AS_TEXT)).toEqual([])
  })

  it('深色规则不得用未重映射的深色强调色（p600~p900）作文字颜色', () => {
    expect(violations(DARK_ACCENT_AS_TEXT)).toEqual([])
  })

  it('浅色规则不得把淡色阶（n300/n400/p200/p300）当作文字颜色', () => {
    expect(lightViolations()).toEqual([])
  })

  it('扫描范围应覆盖博客前台页面与公共组件', () => {
    expect(files.length).toBeGreaterThan(30)
  })
})

/**
 * 模板层（--tpl-*）自成一套色阶，不在 theme-dark.css 的 --mo-* 重映射范围内，
 * 所以深色模式必须由 site-templates.scss 自己同步重映射：漏掉时标题/正文仍是
 * 浅色模式的深色值，落在深色页底上（企业官网模板实测 1.04:1）。
 * 同源约束：over-hero 导航浮在页面底色之上，用 --tpl-white 反白在浅色模式下
 * 等于白底白字；而以 --tpl-ink-900 作底色的反色按钮一旦前景用 --tpl-white，
 * 深色模式下 ink-900 被重映射成近白后同样不可读。
 */
const TPL_KEY_TOKENS = [
  '--tpl-bg',
  '--tpl-surface',
  '--tpl-ink-900',
  '--tpl-ink-700',
  '--tpl-ink-500'
]
const templatesScss = readFileSync(
  join(__dirname, '../../assets/styles/themes/site-templates.scss'),
  'utf8'
)
const navFile = join(__dirname, '../../components/enterprise/EnterpriseNav.vue')

describe('模板层 --tpl-* 深浅模式约定', () => {
  it('site-templates.scss 必须为 html.dark 下的模板层重映射核心色阶', () => {
    const rule = parseRules(templatesScss).find(r => {
      const parts = r.selector.split(',').map(s => s.replace(/\s+/g, ' ').trim())
      return parts.includes('html.dark.tpl-site') && parts.includes('html.dark .tpl-site')
    })
    expect(
      rule,
      '缺少深色重映射块：须同时命中 <html> 与页面根容器（模板页根节点也声明了 .tpl-site，只写 html 会被它的浅色值覆盖）'
    ).toBeTruthy()
    for (const token of TPL_KEY_TOKENS) {
      expect(rule?.body, `${token} 未在深色块中重映射`).toContain(`${token}:`)
    }
  })

  it('over-hero（首页透明态）不得用 --tpl-white 反白文字', () => {
    const overHero = parseRules(readFileSync(navFile, 'utf8')).filter(r =>
      r.selector.includes('.over-hero')
    )
    expect(overHero.length, '未找到 over-hero 规则，选择器可能已改名').toBeGreaterThan(0)
    const bad = overHero
      .filter(r => /(?:^|[;{\s])color:\s*var\(--tpl-white\)/.test(r.body))
      .map(r => r.selector.replace(/\s+/g, ' '))
    expect(bad).toEqual([])
  })

  it('以 --tpl-ink-900 作背景的反色按钮不得配 --tpl-white 前景', () => {
    const bad = files.flatMap(file =>
      parseRules(readFileSync(file, 'utf8'))
        .filter(
          rule =>
            /background:\s*var\(--tpl-ink-900\)/.test(rule.body) &&
            /(?:^|[;{\s])color:\s*var\(--tpl-white\)/.test(rule.body)
        )
        .map(rule => `${file.replace(`${blogRoot}/`, '')} → ${rule.selector.replace(/\s+/g, ' ')}`)
    )
    expect(bad).toEqual([])
  })
})
