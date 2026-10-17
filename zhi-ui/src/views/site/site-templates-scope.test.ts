import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * 行业模板变量层的「作用域契约」（源码级，无法在 jsdom 断言）：
 * jsdom 不会真正执行 SCSS 级联，因此只能校验声明位置本身。
 *
 * 回归背景：基类变量原先声明在 `.tpl-site` 上，而各内页根节点也带 `.tpl-site`
 * （`.tpl-site contact-page` 等），页面根节点会重新声明基类值，把 <html> 上的
 * 模板值覆盖回默认蓝/基础圆角 —— 结果是只有首页换肤、内页仍旧是默认视觉。
 * 现在基类与模板覆盖都声明在 html.tpl-site / html.tpl-xxx 上。
 */
const scss = readFileSync(join(__dirname, '../../assets/styles/themes/site-templates.scss'), 'utf8')

/** 提取单条规则体（顶层选择器精确匹配，防止误命中后代规则） */
function ruleBody(selector: string): string {
  const start = scss.indexOf(selector + ' {')
  if (start < 0) return ''
  const open = scss.indexOf('{', start)
  let depth = 0
  for (let i = open; i < scss.length; i++) {
    if (scss[i] === '{') depth++
    else if (scss[i] === '}') {
      depth--
      if (depth === 0) return scss.slice(open + 1, i)
    }
  }
  return ''
}

describe('行业模板变量作用域', () => {
  it('基类变量声明在 html.tpl-site 上（而不是页面根节点的 .tpl-site）', () => {
    const htmlBase = ruleBody('html.tpl-site')
    expect(htmlBase, '缺少 html.tpl-site 基类变量块').toContain('--tpl-p:')
    expect(htmlBase).toContain('--tpl-r-lg:')

    // 页面根节点重复 .tpl-site 时不得再声明变量（否则内页被重置回基类值）。
    // 注意用行首锚点，避免把 `html.tpl-site {` 里的子串误当成裸选择器。
    expect(scss, '裸 .tpl-site 不应再有变量声明块').not.toMatch(/^\.tpl-site\s*\{/m)
    expect(scss, '裸 .tpl-xxx 不应再有模板覆盖块').not.toMatch(
      /^\.tpl-(consult|tech|mfg|trade|digital)\s*\{/m
    )
  })

  it('五套模板覆盖都声明在 html.tpl-xxx 上', () => {
    for (const tpl of ['consult', 'tech', 'mfg', 'trade', 'digital']) {
      const body = ruleBody(`html.tpl-${tpl}`)
      expect(body, `缺少 html.tpl-${tpl} 模板覆盖块`).toContain('--tpl-p:')
    }
  })

  it('前台 Element Plus 主色跟随模板（覆盖内联主题色）', () => {
    const htmlBase = ruleBody('html.tpl-site')
    expect(htmlBase).toContain('--el-color-primary: var(--tpl-p) !important;')
    expect(htmlBase).toContain('--el-color-primary-light-3:')
    expect(htmlBase).toContain('--el-color-primary-dark-2:')
  })
})
