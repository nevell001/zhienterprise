/**
 * 应用主题：Mo-Blog 是当前唯一的皮肤，并作为默认值挂载。
 *
 * 背景（真实缺陷）：`theme-mo-blog` 类此前只被 remove、全仓库无处 add，导致
 * `themes/mo-blog.scss` 里 **519 处**以 `.theme-mo-blog` / `html.dark.theme-mo-blog`
 * 为前缀的规则**永不生效** —— 其中包含 `--el-*` 变量映射（主色、圆角、边框、文字色、
 * 页底）、登录页暗色适配、文章详情页大量样式。表现为：
 *   ① 站点设置 `theme_color` 种子值本就是 Mo-Blog 的 `#4f46e5`（`mo-blog.scss:30` 的
 *      `--mo-p600`），说明这套外观才是设计意图，却因类未挂载而未被完整应用；
 *   ② 布局漏底 —— `.theme-mo-blog .site-layout { background: var(--mo-n50) }` 不生效，
 *      页面落到 body 默认白底（企业模板首屏白字压浅底即由此而来）。
 *
 * 现改为挂载该类。`AppTheme` 仍保留类型与存储键以兼容历史偏好读写。
 */
export type AppTheme = 'default'

export const APP_THEME_STORAGE_KEY = 'app-theme'

const APP_THEMES: AppTheme[] = ['default']
const MO_BLOG_CLASS = 'theme-mo-blog'

export function normalizeAppTheme(value: unknown): AppTheme {
  return APP_THEMES.includes(value as AppTheme) ? (value as AppTheme) : 'default'
}

export function getStoredAppTheme(storage: Storage | undefined = localStorage): AppTheme {
  try {
    return normalizeAppTheme(storage?.getItem(APP_THEME_STORAGE_KEY))
  } catch {
    return 'default'
  }
}

export function applyAppTheme(
  theme: AppTheme,
  root: HTMLElement = document.documentElement,
  storage: Storage | undefined = localStorage
): void {
  const nextTheme = normalizeAppTheme(theme)
  // Mo-Blog 是唯一皮肤，恒定挂载；下方 519 处规则依赖它生效
  root.classList.add(MO_BLOG_CLASS)

  try {
    storage?.setItem(APP_THEME_STORAGE_KEY, nextTheme)
  } catch {
    // Storage can be unavailable in private mode or tests; class application still matters.
  }
}

// 处理主题样式
/** CSS 颜色（#hex / rgb / rgba）转 #rrggbb，供 SVG data URI 等不能使用 CSS 变量的场景 */
export function cssColorToHex(value: string, fallback = '#4f46e5'): string {
  const raw = String(value || '').trim()
  if (/^#[0-9a-f]{3,8}$/i.test(raw)) return raw
  const match = /^rgba?\(\s*(\d+)[,\s]+(\d+)[,\s]+(\d+)/i.exec(raw)
  if (!match) return fallback
  return (
    '#' +
    [match[1], match[2], match[3]]
      .map(channel => Math.min(255, Number(channel)).toString(16).padStart(2, '0'))
      .join('')
  )
}

export function handleThemeStyle(theme: string): void {
  // 添加主题参数有效性检查，避免undefined导致的错误
  if (!theme || typeof theme !== 'string') {
    theme = '#4f46e5' // 默认主题色作为备用（与 mo-blog 默认主色一致）
  }
  // --mo-p* 色阶由它派生（见 themes/mo-blog.scss），改主题色即整站换色
  document.documentElement.style.setProperty('--theme-primary', theme)
  document.documentElement.style.setProperty('--el-color-primary', theme)
  for (let i = 1; i <= 9; i++) {
    document.documentElement.style.setProperty(
      `--el-color-primary-light-${i}`,
      `${getLightColor(theme, i / 10)}`
    )
  }
  for (let i = 1; i <= 9; i++) {
    document.documentElement.style.setProperty(
      `--el-color-primary-dark-${i}`,
      `${getDarkColor(theme, i / 10)}`
    )
  }
  // 主色填充控件（主按钮/分页激活态等）的前景色，按对比度自动在白与深墨之间二选一
  document.documentElement.style.setProperty('--mo-on-primary', readableOnPrimary(theme))
}

/** 主色填充控件可用的前景色候选 */
const ON_PRIMARY_WHITE = '#ffffff'
const ON_PRIMARY_INK = '#06272e'

// 相对亮度（WCAG）
function relativeLuminance(rgb: number[]): number {
  const [r, g, b] = rgb.map(value => {
    const channel = value / 255
    return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4)
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

// 对比度（WCAG）
function contrastRatio(foreground: number[], background: number[]): number {
  const [lighter, darker] = [relativeLuminance(foreground), relativeLuminance(background)].sort(
    (a, b) => b - a
  )
  return (lighter + 0.05) / (darker + 0.05)
}

/**
 * 主色上的可读前景色。
 *
 * 默认主题的主色偏亮（Element Plus 默认 #409eff、Tech Blue 深色 #00d4ff），
 * 白字对比度只有 2.78:1 / 1.77:1；Mo-Blog 主题的靛蓝（#4f46e5 / #6366f1）
 * 则相反，白字更清楚。这里取两者中对比度更高的一个，避免任何一侧被改坏。
 */
export function readableOnPrimary(theme: string): string {
  const background = hexToRgb(theme)
  const white = contrastRatio(hexToRgb(ON_PRIMARY_WHITE), background)
  const ink = contrastRatio(hexToRgb(ON_PRIMARY_INK), background)
  return white >= ink ? ON_PRIMARY_WHITE : ON_PRIMARY_INK
}

// hex颜色转rgb颜色
export function hexToRgb(str: string): number[] {
  str = str.replace('#', '')
  const hexs = str.match(/../g)
  if (!hexs) return [0, 0, 0]

  const rgb: number[] = []
  for (let i = 0; i < 3; i++) {
    const value = parseInt(hexs[i], 16)
    if (Number.isNaN(value)) {
      return [0, 0, 0]
    }
    rgb[i] = value
  }
  return rgb
}

// rgb颜色转Hex颜色
export function rgbToHex(r: number, g: number, b: number): string {
  const hexs = [r.toString(16), g.toString(16), b.toString(16)]
  for (let i = 0; i < 3; i++) {
    if (hexs[i].length === 1) {
      hexs[i] = `0${hexs[i]}`
    }
  }
  return `#${hexs.join('')}`
}

// 变浅颜色值
export function getLightColor(color: string, level: number): string {
  const rgb = hexToRgb(color)
  for (let i = 0; i < 3; i++) {
    rgb[i] = Math.floor((255 - rgb[i]) * level + rgb[i])
  }
  return rgbToHex(rgb[0], rgb[1], rgb[2])
}

// 变深颜色值
export function getDarkColor(color: string, level: number): string {
  const rgb = hexToRgb(color)
  for (let i = 0; i < 3; i++) {
    rgb[i] = Math.floor(rgb[i] * (1 - level))
  }
  return rgbToHex(rgb[0], rgb[1], rgb[2])
}
