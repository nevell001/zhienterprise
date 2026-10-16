import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * 后台设置页与 SQL 种子的一致性守卫。
 *
 * 设置页读写的每个 blog_setting 键都必须在 `sql/00_init_database.sql` 里有种子，
 * 否则全新安装时该字段会缺失（保存时才由 upsert 补建），而 `docs`/README 又宣称
 * SQL 是唯一的初始化来源。审计发现 `author_bio` / `author_location` /
 * `personal_website` 就漏了种子。
 *
 * 注：这里刻意做"跨文件一致性"校验（两个产物都是文本），不是行为断言 ——
 * 行为断言请用组件挂载测试。
 */

const PAGE = resolve(process.cwd(), 'src/views/admin/setting/index.vue')
const SQL = resolve(process.cwd(), '../sql/00_init_database.sql')

/** 设置页里出现的设置键（含下划线的 snake_case 字符串） */
function pageSettingKeys(): string[] {
  const source = readFileSync(PAGE, 'utf-8')
  return [...new Set([...source.matchAll(/'([a-z][a-z0-9]*_[a-z0-9_]+)'/g)].map(m => m[1]))].sort()
}

/** SQL 种子里 INSERT 的 blog_setting 键 */
function seededSettingKeys(): Set<string> {
  const source = readFileSync(SQL, 'utf-8')
  return new Set([...source.matchAll(/^\('([a-z][a-z0-9_]*)',/gm)].map(m => m[1]))
}

describe('后台设置页与 SQL 种子一致性', () => {
  it('设置页引用的每个键都要有 SQL 种子', () => {
    const pageKeys = pageSettingKeys()
    const seeded = seededSettingKeys()

    // 防止正则失效导致"空集合也通过"
    expect(pageKeys.length).toBeGreaterThan(30)
    expect(seeded.size).toBeGreaterThan(40)

    const missing = pageKeys.filter(key => !seeded.has(key))
    expect(
      missing,
      `以下设置键在 sql/00_init_database.sql 里没有种子：${missing.join(', ')}`
    ).toEqual([])
  })
})
