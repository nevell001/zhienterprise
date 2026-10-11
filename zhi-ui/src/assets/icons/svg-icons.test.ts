import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { optimize } from 'svgo'

/**
 * SVG 图标守卫测试。
 *
 * 构建期的 `vite-plugin-svg-icons` 会用 svgo 逐个解析 `src/assets/icons/svg/*.svg`：
 * 任何一个图标不是严格合法的 SVG，`vite build` 就会整个失败，而且报错位置会指向
 * **正在加载的 index.html**（插件对每个模块都调用扫描），根本看不出是哪个图标的问题
 * （svgo 被 overrides 提到 4.x 后严格解析放大了这个问题：button.svg 里
 * 多写的一个 `</path>` 让 `npm run build:prod` 直接挂掉）。
 *
 * 这里用与构建完全相同的解析方式提前校验，失败时直接给出文件名。
 */

const ICON_DIR = resolve(process.cwd(), 'src/assets/icons/svg')

const iconFiles = readdirSync(ICON_DIR).filter(file => file.toLowerCase().endsWith('.svg'))

describe('SVG 图标资源', () => {
  it('图标目录不应为空', () => {
    expect(iconFiles.length).toBeGreaterThan(0)
  })

  it.each(iconFiles)('%s 必须是 svgo 可解析的合法 SVG', async file => {
    const content = readFileSync(resolve(ICON_DIR, file), 'utf-8')

    // 包一层 Promise：svgo 2.x 的 optimize 返回 Promise，3/4.x 为同步实现，这里两者都能覆盖
    await expect(Promise.resolve().then(() => optimize(content, {}))).resolves.toBeTruthy()
  })

  it.each(iconFiles)('%s 的标签必须成对闭合', file => {
    const content = readFileSync(resolve(ICON_DIR, file), 'utf-8')

    for (const tag of ['svg', 'path', 'g', 'defs', 'style']) {
      const open = content.match(new RegExp(`<${tag}\\b`, 'g'))?.length ?? 0
      const selfClosed = content.match(new RegExp(`<${tag}\\b[^>]*/>`, 'g'))?.length ?? 0
      const close = content.match(new RegExp(`</${tag}>`, 'g'))?.length ?? 0
      expect(open - selfClosed, `${file} 的 <${tag}> 与 </${tag}> 数量不匹配`).toBe(close)
    }
  })
})
