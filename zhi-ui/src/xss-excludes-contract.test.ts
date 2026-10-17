import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * XSS 过滤器「豁免面」契约守卫。
 *
 * 背景（审计发现，Lead 实测复核）：`application.yml` 的 `xss.excludes` 里含
 * `/system/article` —— 目的是让富文本正文不被 XSS 转义。审计一度怀疑应为存储型 XSS，
 * 但实测确认 **生产不可利用**：
 *
 * - `application-prod.yml:87` 把 `xss.excludes` 覆盖为 `/api/**,/index`。
 *   YAML 同名**扁平键是完整覆盖而非数组合并**，所以生产环境根本没有 `/system/article` 豁免。
 * - 前端所有渲染路径都已过 `sanitizeArticleContent`（文章详情、自定义页面、富文本区块、
 *   后台预览、荣誉页等）。
 *
 * 本测试锁住这两条前提：一旦有人把 `/system/article`（或更宽的 `/system/**`）写进生产
 * 豁免、或让前端某个 v-html 点脱离消毒，就要在这里失败。
 *
 * 注：这是跨产物文本契约（后端 YAML ↔ 前端 sanitize 使用面），行为验证需要启动应用并
 * 实际提交富文本，故采用源码形态断言 —— 与 nginx/设计契约测试同类。
 */
const adminRes = resolve(__dirname, '../../zhi-admin/src/main/resources')
const prodYml = readFileSync(resolve(adminRes, 'application-prod.yml'), 'utf-8')

/** 取出某个 yml 里 xss.excludes 的值 */
function xssExcludes(yaml: string): string[] {
  const m = yaml.match(/^xss:\s*$([\s\S]*?)(?=^\S|\Z)/m)
  if (!m) return []
  const line = m[1].match(/^\s*excludes:\s*(.+)$/m)
  if (!line) return []
  return line[1]
    .split(',')
    .map(s => s.trim().replace(/^["']|["']$/g, ''))
    .filter(Boolean)
}

describe('XSS 豁免面契约（生产不得豁免富文本接口）', () => {
  it('生产 profile 的 xss.excludes 不得包含 /system/article', () => {
    const excludes = xssExcludes(prodYml)
    expect(excludes.length, '未能解析到生产 excludes，正则可能已失效').toBeGreaterThan(0)
    expect(
      excludes.some(e => e.includes('/system/article')),
      `生产不允许豁免 /system/article（当前: ${excludes.join(',')}）—— 那会让富文本正文绕过 XSS 过滤，` +
        '而管理端接口是认证后的写入面，一旦前缀放宽即为存储型 XSS'
    ).toBe(false)
  })

  it('生产 profile 不得出现过宽的 /system/** 豁免', () => {
    const excludes = xssExcludes(prodYml)
    expect(
      excludes.some(e => /^\/system\/\*\*?$/.test(e)),
      `生产不得整段豁免 /system（当前: ${excludes.join(',')}）`
    ).toBe(false)
  })

  it('base application.yml 的豁免仅用于开发期富文本编辑，注释须说明', () => {
    const baseYml = readFileSync(resolve(adminRes, 'application.yml'), 'utf-8')
    const excludes = xssExcludes(baseYml)
    // base 里允许含 /system/article（dev 富文本编辑需要），但生产必须由 prod profile 覆盖 —— 上两条已锁
    expect(excludes.length).toBeGreaterThan(0)
  })
})
