import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * 示例文章种子的契约守卫（读 SQL，属"跨产物一致性"检查，不是行为断言）。
 *
 * 守的是三条曾经真实踩过的坑：
 * 1. 示例正文写的是 Markdown，而 `blog_article.content` 的契约是 HTML
 *    （前台详情只做 sanitize + v-html，不做 Markdown 渲染，页面会显示原始 `#`/代码围栏）；
 * 2. `blog_article_tag` 种子里引用了不存在的 tag_id（幽灵关联，表上没有外键，静默失效）；
 * 3. 示例文章的 category_id 挂错分类（发布/服务类文章没进「产品发布」、招聘没进「团队风采」）。
 */

const SQL = resolve(process.cwd(), '../sql/00_init_database.sql')
const source = readFileSync(SQL, 'utf-8')
const lines = source.split('\n')

/** 示例文章 INSERT 块：返回每篇的标题与正文字面量（已还原 SQL 转义） */
function sampleArticles(): {
  title: string
  content: string
  categoryId: number
  articleType: string
  slug: string
}[] {
  const start = lines.findIndex(l =>
    l.startsWith('INSERT IGNORE INTO `blog_article` (`title`, `summary`, `content`')
  )
  expect(start, '找不到示例文章 INSERT 块').toBeGreaterThanOrEqual(0)

  const tailRe = /^'', (\d+), 1, 'admin', \d+, \d+, 1, \d+, \d+(?:, '([a-z]+)')?(?:, '([a-z0-9-]+)')?\)([,;])$/
  const result: {
    title: string
    content: string
    categoryId: number
    articleType: string
    slug: string
  }[] = []
  let cursor = start + 1
  for (let i = start; i < lines.length; i++) {
    const m = tailRe.exec(lines[i])
    if (!m) continue
    while (lines[cursor] === '' || lines[cursor].startsWith('--')) cursor++
    expect(lines[cursor], '元组应以 ( 开头').toMatch(/^\('/)
    result.push({
      title: lines[cursor].match(/^\('([^']+)'/)?.[1] ?? '',
      categoryId: Number(m[1]),
      articleType: m[2] || 'news',
      slug: m[3] || '',
      content: lines
        .slice(cursor + 1, i)
        .join('\n')
        .replace(/',$/, '')
        .replace(/^'/, '')
        .replace(/\\\\/g, '\\')
        .replace(/''/g, "'")
    })
    cursor = i + 1
    if (m[4] === ';') break
  }
  return result
}

/** 取某个 INSERT 块里种下的主键 id 集合 */
function seededIds(table: string): Set<number> {
  const start = lines.findIndex(l => l.startsWith(`INSERT IGNORE INTO \`${table}\``))
  expect(start, `找不到 ${table} 的种子`).toBeGreaterThanOrEqual(0)
  const ids = new Set<number>()
  for (let i = start; i < lines.length; i++) {
    const m = /^\((\d+), '/.exec(lines[i])
    if (m) ids.add(Number(m[1]))
    if (lines[i].trim().endsWith(';')) break
  }
  return ids
}

/** 标签关联块里引用的 article_id / tag_id 对 */
function articleTagLinks(): { articleId: number; tagId: number }[] {
  const start = lines.findIndex(l =>
    l.startsWith('INSERT IGNORE INTO `blog_article_tag` (`article_id`, `tag_id`) VALUES')
  )
  expect(start, '找不到标签关联块').toBeGreaterThanOrEqual(0)
  const links: { articleId: number; tagId: number }[] = []
  for (let i = start; i < lines.length; i++) {
    for (const m of lines[i].matchAll(/\((\d+),\s*(\d+)\)/g)) {
      links.push({ articleId: Number(m[1]), tagId: Number(m[2]) })
    }
    if (lines[i].trim().endsWith(';')) break
  }
  return links
}

describe('示例文章种子', () => {
  const articles = sampleArticles()

  it('至少覆盖 6 篇示例文章', () => {
    expect(articles.length).toBe(6)
  })

  it('正文必须是 HTML，不能残留 Markdown 标题', () => {
    for (const a of articles) {
      // 代码块里的 `#` 是示例内容，只看代码块之外
      const outsideCode = a.content.replace(/<pre>[\s\S]*?<\/pre>/g, '')
      expect(outsideCode, `${a.title} 的正文不是 HTML`).toMatch(/^<h1>/)
      expect(outsideCode, `${a.title} 仍含 Markdown 标题`).not.toMatch(/(^|\n)#\s/)
      expect(outsideCode, `${a.title} 仍含 Markdown 代码围栏`).not.toMatch(/(^|\n)```/)
    }
  })

  it('分类必须挂在种子里真实存在的分类上', () => {
    const categories = seededIds('blog_category')
    for (const a of articles) {
      expect(categories.has(a.categoryId), `${a.title} 的 category_id=${a.categoryId} 不存在`).toBe(
        true
      )
    }
    // 主题一致性：发布/服务类进「产品发布」，趋势观察进「市场洞察」，招聘进「团队风采」
    const byTitle = new Map(articles.map(a => [a.title, a.categoryId]))
    expect(byTitle.get('知企建站系统 v1.0 正式发布：一站式企业官网搭建平台')).toBe(7) // 产品发布
    expect(byTitle.get('站点运维托管服务正式上线')).toBe(7) // 产品发布
    expect(byTitle.get('2026 企业数字化转型趋势观察')).toBe(10) // 市场洞察
    expect(byTitle.get('加入我们：与知企一起成长')).toBe(8) // 团队风采
  })

  it('内容类型标注：发布/服务类是核心业务，客户案例是 case，其余是资讯', () => {
    const byTitle = new Map(articles.map(a => [a.title, a.articleType]))
    expect(byTitle.get('知企建站系统 v1.0 正式发布：一站式企业官网搭建平台')).toBe('business')
    expect(byTitle.get('站点运维托管服务正式上线')).toBe('business')
    expect(byTitle.get('客户案例：某制造企业官网升级实践')).toBe('case')
    expect(byTitle.get('2026 企业数字化转型趋势观察')).toBe('news')
  })

  it('示例文章都带 slug 伪静态别名且互不重复', () => {
    const slugs = articles.map(a => a.slug).filter(Boolean)
    expect(slugs.length).toBe(articles.length)
    expect(new Set(slugs).size).toBe(slugs.length)
  })

  it('标签关联只能引用真实存在的标签', () => {
    const tags = seededIds('blog_tag')
    const links = articleTagLinks()
    expect(links.length).toBeGreaterThan(10)
    for (const l of links) {
      expect(tags.has(l.tagId), `幽灵关联 article_id=${l.articleId} tag_id=${l.tagId}`).toBe(true)
    }
  })

  it('每篇示例文章都至少有一个标签，且主题相关', () => {
    const links = articleTagLinks()
    for (const [i, a] of articles.entries()) {
      const ids = links.filter(l => l.articleId === i + 1).map(l => l.tagId)
      expect(ids.length, `${a.title} 没有标签`).toBeGreaterThan(0)
    }
    const idsOf = (n: number) => links.filter(l => l.articleId === n).map(l => l.tagId)
    expect(idsOf(6), '《加入我们》应挂 招聘信息(18)').toContain(18) // 招聘信息
    expect(idsOf(5), '《运维托管》应挂 技术服务(16)').toContain(16)
    expect(idsOf(3), '《客户案例》应挂 客户案例(4)').toContain(4)
  })
})
