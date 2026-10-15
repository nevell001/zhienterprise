import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import * as epIcons from '@element-plus/icons-vue'

// 源码契约测试（AGENTS.md 允许的唯一例外形态）：它比较的是「模板标记」与「全局样式表 / 图标注册表」两个产物，
// 挂载单个组件覆盖不到——失效的 Element UI 字体图标、解析不到的 EP 图标、永不命中的 scoped 选择器，
// 都是从上游模板整段复制进来、且只在部分页面暴露的。

const SRC = __dirname

const filesUnder = (dir: string, accept: (name: string) => boolean): string[] => {
  if (!statSync(dir, { throwIfNoEntry: false })?.isDirectory()) return []
  return readdirSync(dir).flatMap(name => {
    const full = join(dir, name)
    if (statSync(full).isDirectory()) return filesUnder(full, accept)
    return accept(name) ? [full] : []
  })
}

const vueFiles = filesUnder(SRC, name => name.endsWith('.vue'))
const relative = (file: string) => file.slice(SRC.length + 1)

// TagsView 的 <close class="el-icon-close"> 依赖全局注册的 EP 图标 + 非 scoped 的 .el-icon-close 复位样式。
// 新增命中必须显式登记在此。
const LEGACY_ICON_ALLOWLIST = ['layout/components/TagsView/index.vue']

// 每个文件「深色块内裸 hex」的当前数量上限，只允许下降（见下方棘轮测试）。
// 2026-10-02 B 批基线：127 处 / 13 个文件；B2 令牌化后剩 39 处 / 5 个文件。
// 降到 0 的文件从表里删除，再写回来就会被当成「新增」直接报错。
const DARK_HEX_CEILING: Record<string, number> = {
  'views/admin/system/user/user/profile/index.vue': 30,
  'components/ArticleTOC.vue': 1,
  'assets/styles/themes/mo-blog.scss': 2,
  'views/admin/article/index.vue': 1
}

// 另一类漏网：整页/整组件只有浅色字面量、文件内**没有任何**深色分支（下面棘轮的目标）。
// 2026-10-04 批次 A 发现监控三页（actuator/prometheus/grafana）就是这类：20 处浅色字面量、
// 零 `var(--el-*)`、零 `html.dark`，`#303133` 落在深色卡片 `#112240` 上只有 1.22:1。
// 三页令牌化后归零，所以不出现在本表；本表是其余文件的现状快照（只允许下降）。
// 降为 0 的文件从表里删除，再写回来会被当成「新增」直接报错。
const LIGHT_ONLY_HEX_CEILING: Record<string, number> = {
  'components/AvatarUpload.vue': 5,
  'components/SiteLayout.vue': 1,
  'components/Breadcrumb/index.vue': 1,
  'components/Crontab/index.vue': 2,
  'components/Editor/index.vue': 5,
  'components/HeaderSearch/index.vue': 1,
  'components/IconSelect/index.vue': 2,
  'components/Pagination/index.vue': 1,
  'components/RightToolbar/index.vue': 1,
  'components/TopNav/index.vue': 4,
  'layout/components/AppMain.vue': 2,
  'layout/components/Copyright/index.vue': 2,
  'layout/components/Navbar.vue': 1,
  'layout/components/Settings/index.vue': 1,
  'layout/components/Sidebar/SidebarItem.vue': 1,
  'layout/components/TagsView/index.vue': 4,
  'layout/index.vue': 1,
  'views/error/401.vue': 8,
  'views/error/404.vue': 4
}

// svgicon.ts 的 import 名单 = 运行时全局注册的 EP 图标
const globallyRegistered = new Set(
  readFileSync(join(SRC, 'components/SvgIcon/svgicon.ts'), 'utf8')
    .split("from '@element-plus/icons-vue'")[0]
    .split('\n')
    .map(line => line.trim().replace(/,$/, ''))
    .filter(line => /^[A-Z][A-Za-z0-9]*$/.test(line))
)

const kebab = (name: string) => name.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()
const iconByTag = new Map<string, string>()
Object.keys(epIcons)
  .filter(name => name !== 'default')
  .forEach(name => {
    iconByTag.set(kebab(name).toLowerCase(), name)
    iconByTag.set(name.toLowerCase(), name)
  })

describe('设计源码契约', () => {
  it('模板中不得出现 Element UI 2 的字体图标类（Element Plus 下不渲染）', () => {
    const hits = vueFiles
      .filter(file => /class="[^"]*\bel-icon-[a-z][\w-]*\b/.test(readFileSync(file, 'utf8')))
      .map(relative)
      .sort()

    expect(hits).toEqual([...LEGACY_ICON_ALLOWLIST].sort())
  })

  it('模板里的 EP 图标标签必须能解析（全局注册或本文件引入）', () => {
    const unresolved: string[] = []

    vueFiles.forEach(file => {
      const source = readFileSync(file, 'utf8')
      const scriptStart = source.indexOf('<script')
      const template = (scriptStart > 0 ? source.slice(0, scriptStart) : source).replace(
        /<!--[\s\S]*?-->/g,
        ''
      )
      const script = scriptStart > 0 ? source.slice(scriptStart) : ''

      for (const [, tag] of template.matchAll(/<([A-Za-z][A-Za-z0-9-]*)[\s/>]/g)) {
        if (tag.startsWith('el-') || tag.startsWith('El')) continue
        const icon = iconByTag.get(tag.toLowerCase())
        if (!icon) continue
        if (!globallyRegistered.has(icon) && !new RegExp(`\\b${icon}\\b`).test(script)) {
          unresolved.push(`${relative(file)}  <${tag}>`)
        }
      }
    })

    expect(unresolved).toEqual([])
  })

  it('scoped 样式不得写 html.* :deep(...)（scope 属性会落到 html 上，永不命中）', () => {
    const dead: string[] = []

    vueFiles.forEach(file => {
      const source = readFileSync(file, 'utf8')
      if (!/<style[^>]*\bscoped\b/.test(source)) return
      source.split('\n').forEach(line => {
        if (/^\s*html(\.[\w-]+)+\s+:deep\(/.test(line))
          dead.push(`${relative(file)}  ${line.trim()}`)
      })
    })

    expect(dead).toEqual([])
  })

  it('全局样式不得用 outline: none 抹掉焦点环', () => {
    const offenders: string[] = []
    const styleFiles = filesUnder(
      join(SRC, 'assets/styles'),
      name => name.endsWith('.scss') || name.endsWith('.css')
    )

    styleFiles.forEach(file => {
      const source = readFileSync(file, 'utf8')
      for (const block of source.matchAll(/([^{}]*:focus[^{}]*)\{([^{}]*)\}/g)) {
        if (/outline\s*:\s*(none|0)/.test(block[2]))
          offenders.push(`${relative(file)} -> ${block[1].trim()}`)
      }
    })

    expect(offenders).toEqual([])
  })

  it('index.scss 保留 :focus-visible 描边规则', () => {
    const index = readFileSync(join(SRC, 'assets/styles/index.scss'), 'utf8')
    expect(index).toContain(':focus-visible')
    expect(index).toContain('outline: 2px solid')
  })

  // 全局描边一旦命中 Element Plus 控件，就会在圆角输入框内部再套一个直角框，
  // 看起来像「选中后框子变大/变形」。EP 会把 tabindex="0" 放在 .el-input__inner
  // 这类内部元素上，所以只判断类名不够，必须真的拿选择器去匹配这些元素。
  it('全局焦点描边不得命中 Element Plus 控件（自带焦点样式）', () => {
    const index = readFileSync(join(SRC, 'assets/styles/index.scss'), 'utf8').replace(
      /\/\*[\s\S]*?\*\//g,
      ''
    )
    const block = index.match(/([^{}]*:focus-visible[^{}]*)\{[^}]*outline:\s*2px solid/)
    expect(block).not.toBeNull()

    const selectors = block![1]
      .split(',')
      .map(s => s.trim())
      .filter(Boolean)
    expect(selectors.length).toBeGreaterThan(0)

    const epElements = [
      ['input', 'el-input__inner', { tabindex: '0', type: 'text' }],
      ['textarea', 'el-textarea__inner', { tabindex: '0' }],
      ['div', 'el-select__wrapper', { tabindex: '0', role: 'combobox' }],
      ['span', 'el-select__caret el-input__icon', {}],
      ['button', 'el-button', { tabindex: '0' }],
      ['input', 'el-checkbox__original', { tabindex: '0', type: 'checkbox' }]
    ] as const

    const hits = epElements.flatMap(([tag, className, attrs]) => {
      const node = document.createElement(tag)
      node.setAttribute('class', className)
      Object.entries(attrs).forEach(([name, value]) => node.setAttribute(name, value))
      return selectors
        .filter(selector => {
          try {
            return node.matches(selector.replace(/:focus-visible/g, ''))
          } catch {
            return false
          }
        })
        .map(selector => `${selector} -> ${tag}.${className}`)
    })

    expect(hits).toEqual([])
  })

  // 深色样式的裸 hex 棘轮。深色规则里的字面色值不会跟随主题令牌：默认主题与
  // Mo-Blog 主题共用一条 `html.dark .x` 时，写死的棕色就会渗进默认主题。
  // 令牌定义（属性以 -- 开头）是色板的唯一来源，不计入。
  // 上限只允许下降：新增硬编码、或新建一个带硬编码的文件，都会让这条测试变红。
  it('深色块的裸 hex 数量不得超过登记上限', () => {
    const styleFiles = filesUnder(SRC, name => /\.(vue|scss|css)$/.test(name))
    const actual = new Map<string, number>()

    for (const file of styleFiles) {
      const source = readFileSync(file, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '')
      const dark: boolean[] = []
      let pending = ''
      let count = 0

      for (const rawLine of source.split('\n')) {
        const line = rawLine.replace(/^\s*\/\/.*$/, '')
        for (const ch of line) {
          if (ch === '{') {
            dark.push(/html\.dark|html\.theme-mo-blog/.test(pending) || (dark.at(-1) ?? false))
            pending = ''
          } else if (ch === '}') {
            dark.pop()
            pending = ''
          } else pending += ch
        }
        if (dark.at(-1) && !/^\s*--/.test(line)) {
          count += (line.replace(/var\([^()]*\)/g, 'V()').match(/#[0-9a-fA-F]{3,8}\b/g) || [])
            .length
        }
      }

      if (count) actual.set(relative(file), count)
    }

    const grown: string[] = []
    for (const [file, count] of actual) {
      const ceiling = DARK_HEX_CEILING[file]
      if (ceiling === undefined) grown.push(`${file}: 新增 ${count} 处`)
      else if (count > ceiling) grown.push(`${file}: ${ceiling} → ${count}`)
    }
    // 上限本身过期也要被发现：文件已降到上限以下时提示收紧到实际值。
    const stale = Object.entries(DARK_HEX_CEILING)
      .filter(([file, ceiling]) => (actual.get(file) ?? 0) < ceiling)
      .map(([file, ceiling]) => `${file}: 上限 ${ceiling} → 实际 ${actual.get(file) ?? 0}`)

    expect({ grown, stale }).toEqual({ grown: [], stale: [] })
  })

  // 「整页无深色适配」棘轮。actuator/prometheus/grafana 三个监控页曾整页用浅色字面量
  // （#303133 落在深色卡片 #112240 上仅 1.22:1）且文件内没有任何 html.dark 覆盖，
  // 深色模式下几乎不可读 —— 而上一条棘轮管不到它们，因为它要求先有 html.dark 块。
  // 这条测试盯的就是这一类漏网：只有浅色字面量、完全没有深色分支的页面/组件。
  // 只统计 color / background / background-color（可见性相关），且 var(--x, #hex) 里的
  // 兜底不算裸值；border/outline/fill 不计入，否则表会被大量刻意的细边框撑爆。
  // 范围只含 .vue 的 <style>：全局样式层（assets/styles/**、styles/**）是令牌/主题的定义处，
  // script 段的字面量（ECharts 色板等）也合法，都不在此列。
  // 判定只看文件自身有无深色分支：zhi.scss 里按类名挂的 `html.dark .x` 覆盖不算「文件有覆盖」——
  // 那种写法一改类名就静默失效，正是这条测试要逼出来的东西。
  // 上限只允许下降：新增整页浅色硬编码、或新建一个这样的文件，都会让这条测试变红。
  it('无深色覆盖的样式块裸 hex 不得超过登记上限', () => {
    const actual = new Map<string, number>()

    for (const file of vueFiles) {
      const source = readFileSync(file, 'utf8')
      // 文件里已有 html.dark / html.theme-mo-blog 分支的，交给上一条深色棘轮管。
      if (/html\.dark|html\.theme-mo-blog/.test(source)) continue
      const styles = [...source.matchAll(/<style[^>]*>([\s\S]*?)<\/style>/g)]
        .map(match => match[1])
        .join('\n')
        .replace(/\/\*[\s\S]*?\*\//g, '')

      let count = 0
      for (const match of styles.matchAll(
        /(?:^|[;{\s])(color|background|background-color)\s*:\s*([^;}]+)/g
      )) {
        count += (match[2].replace(/var\([^()]*\)/g, 'V()').match(/#[0-9a-fA-F]{3,8}\b/g) || [])
          .length
      }
      if (count) actual.set(relative(file), count)
    }

    const grown: string[] = []
    for (const [file, count] of actual) {
      const ceiling = LIGHT_ONLY_HEX_CEILING[file]
      if (ceiling === undefined) grown.push(`${file}: 新增 ${count} 处`)
      else if (count > ceiling) grown.push(`${file}: ${ceiling} → ${count}`)
    }
    // 上限本身过期也要被发现：文件已降到上限以下时提示收紧到实际值。
    const stale = Object.entries(LIGHT_ONLY_HEX_CEILING)
      .filter(([file, ceiling]) => (actual.get(file) ?? 0) < ceiling)
      .map(([file, ceiling]) => `${file}: 上限 ${ceiling} → 实际 ${actual.get(file) ?? 0}`)

    expect({ grown, stale }).toEqual({ grown: [], stale: [] })
  })

  // 博客前台的「未走令牌」字号/间距字面量棘轮。前台字号走 --mo-fs-*（rem）、
  // 间距走 --mo-sp-*（px），但只归并了本来就在刻度上的值（Option B）；
  // 刻度外的畸零值（13px / 0.9rem / 6px / 18px …）暂时保留字面量。
  // 未走令牌的数量只允许下降：新增硬编码字号/间距会让这条测试变红。
  // 范围刻意只含博客前台——后台（views/admin、layout）不在这套尺度内。
  //
  // 前台组件集合从 views/site/** 的 import 派生，不写死名单：写死过一次，
  // 结果 C2 新抽的 SitePager 整条漏在尺度与棘轮之外。
  const BLOG_COMPONENTS = new Set<string>()
  for (const file of filesUnder(join(SRC, 'views/blog'), name => name.endsWith('.vue'))) {
    for (const m of readFileSync(file, 'utf8').matchAll(
      /from ['"]@\/components\/([A-Za-z0-9/]+)\.vue['"]/g
    )) {
      BLOG_COMPONENTS.add(`components/${m[1]}.vue`)
    }
  }

  it('博客前台未令牌化的字号/间距字面量不得超过登记上限', () => {
    const FRONT_END = (rel: string) =>
      rel.startsWith('views/site/') || BLOG_COMPONENTS.has(rel) || rel.includes('themes/mo-blog')

    const FONT_SIZE_CEILING = 141
    const SPACING_CEILING = 389
    const SPACING_DECL =
      /(?:padding|margin|gap|row-gap|column-gap)(?:-top|-right|-bottom|-left|-inline|-block)?\s*:\s*([^;{}]+);/g
    const LITERAL = /^-?[0-9.]+(px|rem)$/

    let fontSize = 0
    let spacing = 0

    for (const file of filesUnder(SRC, name => /\.(vue|scss|css)$/.test(name))) {
      if (!FRONT_END(relative(file))) continue
      const source = readFileSync(file, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '')

      for (const rawLine of source.split('\n')) {
        const line = rawLine.replace(/^\s*\/\/.*$/, '')
        for (const m of line.matchAll(/font-size:\s*([^;{}]+);/g)) {
          if (!/^\s*var\(/.test(m[1])) fontSize++
        }
        for (const m of line.matchAll(SPACING_DECL)) {
          for (const token of m[1].trim().split(/\s+/)) {
            if (LITERAL.test(token) && parseFloat(token) !== 0) spacing++
          }
        }
      }
    }

    const grown: string[] = []
    if (fontSize > FONT_SIZE_CEILING) grown.push(`字号字面量: ${FONT_SIZE_CEILING} → ${fontSize}`)
    if (spacing > SPACING_CEILING) grown.push(`间距字面量: ${SPACING_CEILING} → ${spacing}`)

    expect(grown).toEqual([])
  })
})
