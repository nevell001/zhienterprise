import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative, resolve } from 'node:path'

/**
 * 主题色硬编码守卫（源码级契约）。
 *
 * 「界面主题 → 主题颜色」与行业模板主色都是 UI 色，必须走令牌：
 *   - 后台：`var(--el-color-primary)` / `var(--mo-p*)`（由 handleThemeStyle / color-mix 派生）
 *   - 企业前台：`var(--tpl-*)` / `var(--mo-p*)`（模板主色派生）
 * 直接写死品牌色（#409eff / #4f46e5 / 靛蓝 rgba）会让换主题色、切换行业模板时局部不跟随。
 *
 * 说明：`var(--x, #409eff)` 这类 **fallback** 不算违规（运行时变量总是存在），
 * 本测试会把位于 `var(...)` 内的字面量排除；仅拦截真正写死的声明与图表 option。
 */
const SRC = resolve(__dirname)

/** 允许出现品牌字面量的位置：预设选择器/默认配置，不是渲染色 */
const ALLOWLIST = new Set([
  'settings.ts',
  'utils/theme.ts',
  'config/siteTemplates.ts', // 行业模板注册表：color 仅用于后台色点展示，非渲染色
  'utils/echarts.ts', // 图表取色 helper：cssVar('--el-color-primary') 的默认值集中在此

  'views/admin/setting/index.vue',
  'layout/components/Settings/index.vue'
])

const BRAND_LITERAL =
  /#(?:409eff|4f46e5|6366f1|4338ca|1a56db|2563eb|1d4ed8|185fa5)\b|rgba?\(\s*(?:64,\s*158,\s*255|79,\s*70,\s*229|99,\s*102,\s*241|0,\s*212,\s*255)/i

function collectFiles(dir: string): string[] {
  const found: string[] = []
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) {
      found.push(...collectFiles(full))
    } else if (/\.(vue|ts|scss)$/.test(entry) && !/\.test\.ts$/.test(entry)) {
      found.push(full)
    }
  }
  return found
}

/** 判断字面量是否落在 var(...) 的 fallback 位置（括号平衡） */
function insideVarFallback(line: string, index: number): boolean {
  const prefix = line.slice(0, index)
  let depth = 0
  let lastVarOpen = -1
  for (let i = 0; i < prefix.length; i++) {
    if (prefix.startsWith('var(', i)) {
      lastVarOpen = i
      depth++
      i += 3
    } else if (prefix[i] === '(') {
      depth++
    } else if (prefix[i] === ')') {
      depth = Math.max(0, depth - 1)
    }
  }
  return lastVarOpen >= 0 && depth > 0
}

describe('主题色硬编码守卫', () => {
  it('源码中不得写死品牌色（除 var() fallback / 预设选择器 / 默认配置）', () => {
    const violations: string[] = []

    for (const file of collectFiles(SRC)) {
      const rel = relative(SRC, file)
      if (ALLOWLIST.has(rel)) continue
      if (rel.startsWith('assets/styles/themes/') || rel === 'assets/styles/theme-dark.css')
        continue

      // 去掉 CSS 块注释，避免注释里的说明文字误报
      const source = readFileSync(file, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '')
      source.split('\n').forEach((line, lineNo) => {
        const trimmed = line.trim()
        if (!trimmed || trimmed.startsWith('//') || trimmed.startsWith('*')) return
        const regex = new RegExp(BRAND_LITERAL.source, 'gi')
        let match: RegExpExecArray | null
        while ((match = regex.exec(line))) {
          if (insideVarFallback(line, match.index)) continue
          violations.push(`${rel}:${lineNo + 1} → ${trimmed.slice(0, 80)}`)
        }
      })
    }

    expect(violations, violations.join('\n')).toEqual([])
  })
})
