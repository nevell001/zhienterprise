import { describe, expect, it } from 'vitest'
import {
  getHomeSolutions,
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
