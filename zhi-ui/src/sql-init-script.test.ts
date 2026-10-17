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

  /**
   * 顺序守卫（回归）：引用「表.列」的语句必须出现在该列的补齐守卫之后。
   * 2026-10 的真实故障：zhi_article_tag 种子在 ON 子句引用 zhi_article.slug，
   * 而 slug 的 ADD COLUMN 守卫在其后 1340 行 —— 早于 slug 特性建库的存量库重跑时
   * 抛 ERROR 1054 (Unknown column 'a.slug' in 'on clause') 并中断其后全部语句。
   * 注意 CREATE TABLE 里的列只对全新库生效，存量库依赖 ADD COLUMN 守卫，故必须校验顺序。
   */
  it('引用表.列 的语句必须晚于该列的补齐守卫（防前向引用）', () => {
    // 收集所有 information_schema.COLUMNS 守卫：键为 `表.列`，值为守卫所在行号
    const guardLine = new Map<string, number>()
    const guardRe =
      /information_schema\.COLUMNS[\s\S]{0,200}?TABLE_NAME\s*=\s*'(\w+)'\s+AND\s+COLUMN_NAME\s*=\s*'(\w+)'/gi
    for (const m of source.matchAll(guardRe)) {
      const line = source.slice(0, m.index).split('\n').length
      guardLine.set(`${m[1]}.${m[2]}`, line)
    }
    // 防止解析失效导致空集合通过
    expect(guardLine.size, '未解析到任何列补齐守卫，正则可能已失效').toBeGreaterThanOrEqual(15)

    // ⚠️ 关键：引用常经由**表别名**（`JOIN zhi_article a ON a.slug = ...`），
    // 直接用 `表.列` 匹配会漏掉全部此类引用 —— 首版守卫即因此放过缺陷（已验证）。
    // 这里按语句块解析「表 → 别名」映射，再把 `别名.列` 归一为 `表.列`。
    const problems: string[] = []
    const rawLines = source.split('\n')

    // 逐行维护别名映射：遇到 FROM/JOIN/UPDATE/INSERT INTO 时登记 `表 别名`
    const aliasToTable = new Map<string, string>()
    rawLines.forEach((raw, idx) => {
      const lineNo = idx + 1
      const line = raw.trim()
      if (line.startsWith('--')) return
      // 登记别名：FROM/JOIN/UPDATE `table` `alias` 或 table alias
      const aliasRe = /\b(?:FROM|JOIN|UPDATE|INTO)\s+`?(\w+)`?\s+(?:AS\s+)?`?([a-z]\w*)`?/gi
      for (const m of line.matchAll(aliasRe)) {
        const [, tbl, alias] = m
        // 排除把关键字当别名的情形（如 ON / WHERE / SET）
        if (/^(on|where|set|and|or|values|select|left|inner|group|order)$/i.test(alias)) continue
        if (alias.toLowerCase() === tbl.toLowerCase()) continue
        aliasToTable.set(alias, tbl)
      }
      // 检查本行是否引用了「别名.列」或「表.列」，且该列有守卫
      for (const [key, gLine] of guardLine) {
        if (lineNo >= gLine) continue // 守卫之后才允许引用
        const [table, column] = key.split('.')
        if (/information_schema|COLUMN_NAME|ADD COLUMN|COMMENT/i.test(line)) continue
        // 直接表名限定
        const direct = new RegExp(`\`${table}\`\\s*\\.\\s*\`${column}\`|\\b${table}\\.${column}\\b`)
        // 别名限定：本行出现的别名若映射到该表，且引用了 `别名.列`
        const viaAlias = [...aliasToTable.entries()].some(
          ([alias, tbl]) =>
            tbl === table &&
            new RegExp(`\`${alias}\`\\s*\\.\\s*\`${column}\`|\\b${alias}\\.${column}\\b`).test(line)
        )
        if (direct.test(line) || viaAlias) {
          problems.push(`${key}: 第 ${lineNo} 行引用了它，但补齐守卫在第 ${gLine} 行`)
        }
      }
    })
    expect(problems).toEqual([])
  })
})
