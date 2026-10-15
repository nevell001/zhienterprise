import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import BizCardsSection from './BizCardsSection.vue'

/**
 * 核心业务两卡：config.cardOne/cardTwo 覆盖行业模板文案的行为。
 * 页面编辑器把空串当作「未填写」保存，这里锁定空串必须回退模板文案，
 * 而不是把卡片标题/说明渲染成空白（历史上是直接展开覆盖，空串会清空文案）。
 */
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/api/site', () => ({
  getArticleList: vi.fn().mockResolvedValue({ code: 200, rows: [] })
}))

function mountSection(config?: Record<string, any>) {
  return mount(BizCardsSection, {
    props: { config },
    global: { plugins: [createPinia()] }
  })
}

describe('BizCardsSection 卡片文案覆盖', () => {
  it('缺省时回退当前行业模板的两卡文案', async () => {
    const wrapper = mountSection()
    await flushPromises()

    expect(wrapper.findAll('.biz-card h3').map(n => n.text())).toEqual(['咨询中心', '顾问中心'])
    expect(wrapper.findAll('.biz-card > p').map(n => n.text())).toEqual([
      '战略诊断、组织优化、运营提升的结构化咨询服务。',
      '行业顾问全程陪跑，把方法论落成可执行的改进项。'
    ])
  })

  it('cardOne/cardTwo 标题与说明可整体覆盖模板', async () => {
    const wrapper = mountSection({
      cardOne: { title: '产品平台', desc: '自定义左卡说明' },
      cardTwo: { title: '交付服务', desc: '自定义右卡说明' }
    })
    await flushPromises()

    expect(wrapper.findAll('.biz-card h3').map(n => n.text())).toEqual(['产品平台', '交付服务'])
    expect(wrapper.findAll('.biz-card > p').map(n => n.text())).toEqual([
      '自定义左卡说明',
      '自定义右卡说明'
    ])
  })

  it('空串/空白串视为未填写：回退模板文案而不是渲染空标题', async () => {
    const wrapper = mountSection({
      cardOne: { title: '', desc: '   ' },
      cardTwo: {}
    })
    await flushPromises()

    expect(wrapper.findAll('.biz-card h3').map(n => n.text())).toEqual(['咨询中心', '顾问中心'])
    expect(wrapper.findAll('.biz-card > p').map(n => n.text())).toEqual([
      '战略诊断、组织优化、运营提升的结构化咨询服务。',
      '行业顾问全程陪跑，把方法论落成可执行的改进项。'
    ])
  })
})
