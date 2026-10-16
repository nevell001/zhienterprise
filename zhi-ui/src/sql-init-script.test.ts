import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * `sql/00_init_database.sql` 的两条硬性约定守卫：
 *
 * 1. 脚本内不得出现 `CREATE DATABASE` / `USE` 语句。库名写进脚本后，`mysql <副本库> < 00…sql`
 *    会被 `USE` 静默改回写死的库 —— 在副本库上做演练会实际写进生产库（真实发生过），
 *    自定义 `DB_NAME` 的部署也会建错库。库名只应由调用方给出（命令行位置参数或 Docker 的
 *    `MYSQL_DATABASE`）。
 * 2. 维护型计数同步语句（文章评论数、分类文章数）必须带「值确实不一致」的守卫。
 *    `zhi_article` 上有 BEFORE UPDATE 触发器无条件 `SET NEW.update_time = NOW()`，语句里把
 *    update_time 赋回自身是无效的；整表刷会让每次升级重跑都把全部文章顶到「最近更新」首位。
 *
 * 注：这里刻意做跨产物的一致性校验（脚本是文本产物，断言的是它的形态），不是行为断言 ——
 * 行为验证需要真库，已按 mysql 修复演练流程在 scratch 副本上跑过（见 sql/README.md）。
 */

const SQL = resolve(process.cwd(), '../sql/00_init_database.sql')
const source = readFileSync(SQL, 'utf-8')

/** 剥掉整行 `--` 注释与字符串字面量后的语句列表（注释/提示文案里允许出现这些关键字） */
const statements = source
  .split('\n')
  .filter(line => !line.trim().startsWith('--'))
  .join('\n')
  .replace(/'[^']*'/g, "''")
  .split(';')
  .map(s => s.trim())
  .filter(Boolean)

describe('数据库初始化脚本的库名与重跑安全约定', () => {
  it('脚本内不得建库或切换库（库名只能由调用方给出）', () => {
    const offenders = statements.filter(s =>
      /^(CREATE\s+DATABASE|USE)\b/i.test(s.replace(/^\s+/, ''))
    )
    expect(offenders).toEqual([])
  })

  it('维护型计数同步语句必须带值守卫，否则重跑会重写全部行', () => {
    // 语句片段：含 UPDATE，且目标表是 zhi_article / zhi_category（含存储过程体内的缩进语句）
    const touching = statements.filter(s => /UPDATE\s+`?zhi_(article|category)`?/i.test(s))
    // 防止正则失效导致「空集合也通过」：脚本里至少有评论数校正、NULL 兜底、演示数据回填、
    // 分类数批量校正与存储过程里的两条分支
    expect(touching.length).toBeGreaterThanOrEqual(10)

    // 合法形态只有三种：值守卫（<> / IS NULL）、或按主键的单行更新（触发器路径的分类计数增减）
    const legal = /(<>|IS NULL|WHERE\s+[`\w.]*id\s*=\s*[`\w.]+)/i
    const unguarded = touching.filter(s => !legal.test(s))
    expect(unguarded.map(s => s.slice(0, 120))).toEqual([])
  })
})
