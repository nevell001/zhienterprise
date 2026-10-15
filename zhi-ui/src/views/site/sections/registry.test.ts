import { describe, expect, it } from 'vitest'
import { parseSections, SECTION_REGISTRY } from './registry'

describe('sections registry（模块注册表与解析）', () => {
  it('注册表包含全部 14 个模块类型', () => {
    expect(Object.keys(SECTION_REGISTRY).sort()).toEqual([
      'biz-cards',
      'channels',
      'contact-info',
      'cta',
      'faq',
      'hero',
      'history',
      'honors',
      'logos',
      'metrics',
      'news',
      'outcomes',
      'rich-text',
      'solutions'
    ])
  })

  it('parseSections：JSON 字符串解析、不可见与未注册类型被过滤', () => {
    const raw = JSON.stringify([
      { type: 'hero', config: { eyebrow: 'x' }, visible: true },
      { type: 'metrics', visible: false },
      { type: 'unknown-type', visible: true },
      { type: 'cta', visible: true }
    ])
    const sections = parseSections(raw)
    expect(sections.map(s => s.type)).toEqual(['hero', 'cta'])
    expect(sections[0].config).toEqual({ eyebrow: 'x' })
    expect(sections[0].component).toBeDefined()
  })

  it('parseSections：非 JSON/非数组/空值返回空数组（渲染层据此回退旧版首页）', () => {
    expect(parseSections(undefined)).toEqual([])
    expect(parseSections('broken')).toEqual([])
    expect(parseSections('{"a":1}')).toEqual([])
  })
})
