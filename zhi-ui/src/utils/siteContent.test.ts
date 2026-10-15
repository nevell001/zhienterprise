import { describe, expect, it } from 'vitest'
import {
  DEFAULT_PAGE_COPY,
  getHomeSolutions,
  getPageCopy,
  getSiteChannels,
  getSiteFaqs,
  getSiteHistoryStages,
  getSiteHonors,
  getSiteMetrics,
  getSiteOutcomes
} from './siteContent'

describe('siteContent（官网内容 JSON 设置解析）', () => {
  it('空值/非字符串回退内置占位', () => {
    expect(getSiteMetrics(undefined).length).toBeGreaterThan(0)
    expect(getSiteMetrics('').length).toBeGreaterThan(0)
    expect(getSiteMetrics(null).length).toBeGreaterThan(0)
  })

  it('非法 JSON 回退内置占位，不抛错', () => {
    expect(getSiteFaqs('{broken')).toEqual(getSiteFaqs(undefined))
    expect(getSiteHonors('[1,2,3]').length).toBeGreaterThan(0)
    expect(getSiteChannels('"not-array"')).toEqual(getSiteChannels(undefined))
  })

  it('合法 JSON 通过且字段保留', () => {
    const raw = JSON.stringify([
      { value: '30', label: '年深耕' },
      { value: '500+', label: '客户' }
    ])
    const metrics = getSiteMetrics(raw)
    expect(metrics).toHaveLength(2)
    expect(metrics[0]).toEqual({ value: '30', label: '年深耕' })
  })

  it('元素缺关键字段被过滤；全不合法时回退占位', () => {
    const half = JSON.stringify([{ value: '10', label: '年' }, { label: '缺 value' }])
    expect(getSiteMetrics(half)).toHaveLength(1)
    expect(getSiteMetrics('[{"value":""},{"label":"x"}]')).toEqual(getSiteMetrics(undefined))
  })

  it('历程阶段：无事件或缺名的阶段整体不合法', () => {
    const ok = JSON.stringify([
      {
        range: '2020 -',
        name: '生态期',
        theme: 't',
        events: [{ year: '2024', title: '认证', desc: 'd', honor: true }]
      },
      { name: '缺事件阶段', events: [] }
    ])
    const stages = getSiteHistoryStages(ok)
    expect(stages).toHaveLength(1)
    expect(stages[0].events[0].honor).toBe(true)
  })

  it('七类内容的回退占位互为独立副本（修改解析结果不污染默认）', () => {
    const a = getSiteHonors(undefined)
    a[0].name = '污染'
    expect(getSiteHonors(undefined)[0].name).not.toBe('污染')
  })

  it('成果/渠道/FAQ 的回退占位齐全', () => {
    expect(getSiteOutcomes(undefined).length).toBe(3)
    expect(getSiteChannels(undefined).length).toBe(4)
    expect(getSiteFaqs(undefined).length).toBe(4)
    expect(getHomeSolutions(undefined).length).toBe(4)
  })
})

describe('siteContent（页面文案 site_page_copy 解析）', () => {
  it('缺失/损坏/形状不对时全字段为空串（页面据此回退内置文案）', () => {
    const empty = { eyebrow: '', title: '', subtitle: '' }
    expect(getPageCopy(undefined, 'contact')).toEqual(empty)
    expect(getPageCopy('', 'contact')).toEqual(empty)
    expect(getPageCopy('{broken', 'contact')).toEqual(empty)
    // 数组是旧的数组型 JSON 键形状，不能当成页面文案对象
    expect(getPageCopy('[{"title":"x"}]', 'contact')).toEqual(empty)
    expect(getPageCopy('"text"', 'contact')).toEqual(empty)
  })

  it('取指定页面并保留已填字段，未填字段为未设置（空串）', () => {
    const raw = JSON.stringify({
      business: { eyebrow: 'Business', title: '业务中心', subtitle: '' },
      contact: { title: '快速联系' }
    })
    expect(getPageCopy(raw, 'business')).toEqual({
      eyebrow: 'Business',
      title: '业务中心',
      subtitle: ''
    })
    expect(getPageCopy(raw, 'contact')).toEqual({ eyebrow: '', title: '快速联系', subtitle: '' })
    // 未配置的页面同样落在回退态
    expect(getPageCopy(raw, 'honors')).toEqual({ eyebrow: '', title: '', subtitle: '' })
  })

  it('空白与非法字段类型按未设置处理', () => {
    const raw = JSON.stringify({ honors: { eyebrow: '  ', title: 123, subtitle: ' 已填 ' } })
    expect(getPageCopy(raw, 'honors')).toEqual({ eyebrow: '', title: '', subtitle: '已填' })
  })

  it('内置文案覆盖五个模板页面且字段齐全', () => {
    for (const key of ['business', 'contact', 'about', 'history', 'honors']) {
      expect(DEFAULT_PAGE_COPY[key]?.title, `缺少 ${key} 的内置标题`).toBeTruthy()
    }
  })
})
