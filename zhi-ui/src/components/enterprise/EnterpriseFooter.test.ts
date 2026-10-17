import { describe, expect, it, vi, beforeEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import EnterpriseFooter from './EnterpriseFooter.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'

/**
 * 企业官网模式页脚的「显示底部 / 显示版权」开关（行为）：
 * 企业模式曾绕过 SiteLayout 的经典页脚分支，两个开关对 EnterpriseFooter 完全无效。
 */

const { getSiteNav } = vi.hoisted(() => ({ getSiteNav: vi.fn() }))

vi.mock('@/api/site/nav', () => ({
  getSiteNav
}))

beforeEach(() => {
  getSiteNav.mockResolvedValue({ data: [] })
})

const stubs = {
  RouterLink: {
    props: ['to'],
    template: '<a class="stub-link" :data-to="to"><slot /></a>'
  }
}

function mountFooter(settings: Record<string, unknown> = {}) {
  const pinia = createPinia()
  const wrapper = mount(EnterpriseFooter, {
    global: { plugins: [pinia], stubs }
  })
  const store = useSiteSettingsStore(pinia)
  store.updateSiteSettings(settings as never)
  return { wrapper, pinia }
}

describe('EnterpriseFooter 显示开关', () => {
  it('默认（开关开启）渲染页脚与版权行', () => {
    const { wrapper } = mountFooter()

    expect(wrapper.find('.ent-footer').exists()).toBe(true)
    expect(wrapper.find('.fbot').text()).toContain('保留所有权利')
  })

  it('「显示底部」关闭：整个页脚不渲染', async () => {
    const { wrapper, pinia } = mountFooter()
    const store = useSiteSettingsStore(pinia)

    store.updateSiteSettings({ footer_enabled: false })
    await nextTick()

    expect(wrapper.find('.ent-footer').exists()).toBe(false)
  })

  it('快速导航中 /business 项的文案跟随「栏目名称」', async () => {
    getSiteNav.mockResolvedValue({
      data: [
        {
          id: 1,
          location: 'footer',
          parentId: 0,
          label: '首页',
          linkType: 'path',
          linkValue: '/',
          sort: 1,
          visible: 1
        },
        {
          id: 2,
          location: 'footer',
          parentId: 0,
          label: '核心业务',
          linkType: 'path',
          linkValue: '/business',
          sort: 2,
          visible: 1
        }
      ]
    })
    const { wrapper } = mountFooter({ business_label: '产品中心' })
    await flushPromises()

    const quickNav = wrapper.findAll('.fcol')[0]
    expect(quickNav.text()).toContain('产品中心')
  })

  it('business_label 为空时快速导航回退「核心业务」', async () => {
    getSiteNav.mockResolvedValue({
      data: [
        {
          id: 2,
          location: 'footer',
          parentId: 0,
          label: '核心业务',
          linkType: 'path',
          linkValue: '/business',
          sort: 2,
          visible: 1
        }
      ]
    })
    const { wrapper } = mountFooter({ business_label: '' })
    await flushPromises()

    expect(wrapper.findAll('.fcol')[0].text()).toContain('核心业务')
  })

  it('「显示版权」关闭：仅隐藏版权行，底部链接保留', async () => {
    const { wrapper, pinia } = mountFooter()
    const store = useSiteSettingsStore(pinia)

    store.updateSiteSettings({ copyright_enabled: false })
    await nextTick()

    expect(wrapper.find('.ent-footer').exists()).toBe(true)
    expect(wrapper.find('.fbot').text()).not.toContain('保留所有权利')
    expect(wrapper.find('.fbot-links').text()).toContain('隐私政策')
  })
})
