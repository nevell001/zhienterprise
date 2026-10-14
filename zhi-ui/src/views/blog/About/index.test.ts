import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import About from '../about.vue'

const source = readFileSync(resolve(__dirname, '../about.vue'), 'utf8')

describe('About 页面测试', () => {
  it('应该导出 About 组件', () => {
    expect(About).toBeDefined()
    expect(typeof About).toBe('object')
  })

  it('模板化根结构：tpl-site 根类 + 页头（eyebrow / 主标题 / 副标题）', () => {
    expect(source).toContain('class="tpl-site about-page mo-home-page"')
    expect(source).toContain('class="aph"')
    expect(source).toContain('eyebrow')
    expect(source).toContain('aph-title')
    expect(source).toContain('aph-sub')
  })

  it('企业简介正文来自页面管理 slug=about 的已发布页面', () => {
    expect(source).toContain("getPublishedPageBySlug('about')")
    expect(source).toContain('customAboutContent')
  })

  it('正文必须经与前台统一的消毒管线渲染', () => {
    expect(source).toContain('sanitizeArticleContent(renderMarkdown(raw))')
    expect(source).toContain('v-html="aboutContentHtml"')
  })

  it('数据概览接入公开统计接口（不虚构数字）', () => {
    expect(source).toContain('getPublicStatisticsOverview')
    expect(source).toContain('statItems')
  })

  it('提供荣誉资质/发展历程入口', () => {
    expect(source).toContain('to="/blog/honors"')
    expect(source).toContain('to="/blog/history"')
  })
})
