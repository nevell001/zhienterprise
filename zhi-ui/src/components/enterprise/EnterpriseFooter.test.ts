import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import EnterpriseFooter from './EnterpriseFooter.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'

/**
 * 企业官网模式页脚的「显示底部 / 显示版权」开关（行为）：
 * 企业模式曾绕过 SiteLayout 的经典页脚分支，两个开关对 EnterpriseFooter 完全无效。
 */

vi.mock('@/api/site/nav', () => ({
  getSiteNav: vi.fn().mockResolvedValue({ data: [] })
}))

const stubs = {
  RouterLink: {
    props: ['to'],
    template: '<a class="stub-link" :data-to="to"><slot /></a>'
  }
}

function mountFooter() {
  const pinia = createPinia()
  const wrapper = mount(EnterpriseFooter, {
    global: { plugins: [pinia], stubs }
  })
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
