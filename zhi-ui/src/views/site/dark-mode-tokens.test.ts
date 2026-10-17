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
const siteRoot = resolve(__dirname)
const files = [
  ...collectVueFiles(siteRoot),
  ...collectVueFiles(resolve(__dirname, '../../components'))
].sort()

/**
 * 主题样式表（.scss）也必须纳入扫描 —— 此前只收集 .vue，导致 mo-blog.scss /
 * theme-dark.css 里的同类违规「恰好命中正则却从未被扫描」：
 * 实测 mo-blog.scss 有 2 处暗色下用 --mo-n900（背景色阶）作文字色。
 */
const themeScssFiles = ['themes/mo-blog.scss', 'themes/site-templates.scss', 'theme-dark.css']
  .map(rel => resolve(__dirname, '../../assets/styles', rel))
  .filter(f => {
    try {
      return statSync(f).isFile()
    } catch {
      return false
    }
  })

const scanFiles = [...files, ...themeScssFiles]

/**
 * 装饰性图形例外：.footer-wave 是 SVG 波浪分隔条（fill="currentColor"），
 * 其 color 是**填充色**而非文字色 —— 浅色下取 --mo-n50、暗色下取 --mo-n900，
 * 正是为了让它与相邻区块底色衔接。给 SVG 图形设"背景色"是正确用法，不属违规。
 */
const DECORATIVE_SHAPE_SELECTORS = ['.footer-wave']

const violations = (pattern: RegExp) =>
  scanFiles.flatMap(file =>
    parseRules(readFileSync(file, 'utf8'))
      .filter(rule => rule.selector.includes('html.dark') && pattern.test(rule.body))
      .filter(rule => !DECORATIVE_SHAPE_SELECTORS.some(sel => rule.selector.includes(sel)))
      .map(rule => `${file.replace(`${siteRoot}/`, '')} → ${rule.selector.replace(/\s+/g, ' ')}`)
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
      .map(rule => `${file.replace(`${siteRoot}/`, '')} → ${rule.selector.replace(/\s+/g, ' ')}`)
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

/**
 * 皮肤类挂载契约（跨文件检查，故为源码级）：
 * `themes/mo-blog.scss` 有 500+ 处以 `.theme-mo-blog` / `html.dark.theme-mo-blog` 为前缀的规则
 * （含 --el-* 变量映射、登录页暗色适配、文章详情页样式、`.site-layout` 页底）。
 * 这些规则**只在 `<html>` 挂了该类时才生效** —— 一旦 `utils/theme.ts` 退回到"只 remove 不 add"，
 * 全部规则静默失效（这正是此前企业模板首屏白字压浅底的成因）。
 * 两端必须同时成立：CSS 里有规则 + JS 里有 add。
 */
describe('Mo-Blog 皮肤类挂载契约', () => {
  const themeTs = readFileSync(join(__dirname, '../../utils/theme.ts'), 'utf8')

  it('utils/theme.ts 必须 add MO_BLOG_CLASS（不能只 remove）', () => {
    expect(
      /classList\.add\(\s*MO_BLOG_CLASS\s*\)/.test(themeTs),
      'theme.ts 必须挂载 .theme-mo-blog；只 remove 会让 mo-blog.scss 的数百条规则全部失效'
    ).toBe(true)
    expect(
      /classList\.remove\(\s*MO_BLOG_CLASS\s*\)/.test(themeTs),
      'theme.ts 不应再 remove .theme-mo-blog（该皮肤是唯一且默认的）'
    ).toBe(false)
  })

  it('mo-blog.scss 中确实存在依赖该前缀的规则（说明挂载是必要的）', () => {
    const moBlogScss = readFileSync(
      join(__dirname, '../../assets/styles/themes/mo-blog.scss'),
      'utf8'
    )
    const prefixed = parseRules(moBlogScss).filter(r => r.selector.includes('.theme-mo-blog'))
    expect(prefixed.length, '未找到 .theme-mo-blog 前缀规则，皮肤类可能已无必要').toBeGreaterThan(
      100
    )
  })
})

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
        .map(rule => `${file.replace(`${siteRoot}/`, '')} → ${rule.selector.replace(/\s+/g, ' ')}`)
    )
    expect(bad).toEqual([])
  })

  /**
   * 回归守卫：--tpl-on-hero 必须随深浅模式取值。
   * 企业模板的 .eh-hero 在亮色下没有深色底（浅底），此时若沿用基类的 #ffffff 反白，
   * 导航文字与 Hero 同色（实测 1.04:1，整块不可见）；暗色下 Hero 才是深色面，需要白字。
   * 此前 TPL_KEY_TOKENS 只锁 5 个 token、漏掉 --tpl-on-hero，所以该缺陷长期未被告警。
   */
  it('--tpl-on-hero 必须按深浅模式分别取值（亮色不得沿用白色反白）', () => {
    const rules = parseRules(templatesScss)
    const lightRule = rules.find(r =>
      r.selector.split(',').some(s => /^html:not\(\.dark\)\s*\.tpl-site$/.test(s.trim()))
    )
    expect(
      lightRule,
      '缺少亮色覆盖块：须用 html:not(.dark) .tpl-site 把 --tpl-on-hero 改为深墨，否则亮色下导航白字压浅底不可见'
    ).toBeTruthy()
    expect(lightRule?.body, '--tpl-on-hero 在亮色下必须取深色文字令牌（而非白色）').toContain(
      '--tpl-on-hero: var(--tpl-ink-900)'
    )

    // 暗色侧：基类仍为白色，且深色重映射块不得把 --tpl-on-hero 改成深色
    const baseRule = rules.find(r => r.selector.split(',').some(s => s.trim() === '.tpl-site'))
    expect(baseRule?.body, '基类 --tpl-on-hero 应为 #ffffff，供暗色模式使用').toContain(
      '--tpl-on-hero: #ffffff'
    )
  })
})
