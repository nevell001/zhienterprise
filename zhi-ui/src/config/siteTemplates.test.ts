import { describe, expect, it } from 'vitest'
import {
  getSiteTemplate,
  normalizeSiteTemplate,
  SITE_TEMPLATES,
  SITE_TEMPLATE_CLASS_NAMES
} from './siteTemplates'

describe('siteTemplates（5 套行业模板）', () => {
  it('注册表恰好 5 套且 id 唯一', () => {
    expect(SITE_TEMPLATES.length).toBe(5)
    expect(new Set(SITE_TEMPLATES.map(t => t.id)).size).toBe(5)
    expect(SITE_TEMPLATES.map(t => t.id)).toEqual(['consult', 'tech', 'mfg', 'trade', 'digital'])
  })

  it('每套模板都有完整配置：主色 / 母题 / 文案 / 插画', () => {
    for (const t of SITE_TEMPLATES) {
      expect(t.className).toBe(`tpl-${t.id}`)
      expect(t.name, `${t.id} 缺 name`).toBeTruthy()
      expect(t.color, `${t.id} 缺主色`).toMatch(/^#[0-9A-Fa-f]{6}$/)
      expect(t.hero.eyebrow, `${t.id} 缺 eyebrow`).toBeTruthy()
      expect(t.hero.titleHtml, `${t.id} 缺主标题`).toContain('<em>')
      expect(t.hero.subtitle.length, `${t.id} 副标题过短`).toBeGreaterThan(10)
      expect(t.solutionsTitle, `${t.id} 缺解决方案标题`).toBeTruthy()
      expect(t.biz.fallbackItems.length, `${t.id} 占位列表应为 3 项`).toBe(3)
      expect(t.art, `${t.id} 缺方案卡插画`).toContain('<')
      expect(t.motif(t.color), `${t.id} 母题未内插主色`).toContain(t.color)
    }
  })

  it('normalizeSiteTemplate：非法值回退默认模板，合法值原样通过', () => {
    expect(normalizeSiteTemplate('tech')).toBe('tech')
    expect(normalizeSiteTemplate('consult')).toBe('consult')
    expect(normalizeSiteTemplate('blog')).toBe('consult')
    expect(normalizeSiteTemplate('')).toBe('consult')
    expect(normalizeSiteTemplate(null)).toBe('consult')
    expect(normalizeSiteTemplate(undefined)).toBe('consult')
    expect(normalizeSiteTemplate(123)).toBe('consult')
  })

  it('getSiteTemplate 与 normalize 口径一致', () => {
    expect(getSiteTemplate('mfg').id).toBe('mfg')
    expect(getSiteTemplate('whatever').id).toBe('consult')
  })

  it('类名清单与注册表一一对应（供 <html> 挂载/清理）', () => {
    expect(SITE_TEMPLATE_CLASS_NAMES).toEqual(SITE_TEMPLATES.map(t => `tpl-${t.id}`))
  })

  it('模板默认眉题不得写死年限（年限只在官网内容指标维护）', () => {
    for (const tpl of SITE_TEMPLATES) {
      expect(tpl.hero.eyebrow).not.toMatch(/\d+\s*年/)
    }
  })

  it('五套主色互不相同（行业心智差异的基础）', () => {
    expect(new Set(SITE_TEMPLATES.map(t => t.color)).size).toBe(5)
  })
})
