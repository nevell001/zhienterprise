import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import Honors from './honors.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'

/**
 * 荣誉页页头文案（行为）：后台「官网内容 → 模板页头文案」的 site_page_copy
 * 设置驱动页头渲染，字段留空时回退内置文案。设置随接口异步到达，
 * 因此断言的是 store 更新后页头的即时变化。
 */
vi.mock('@/api/site/page', () => ({
  getPublishedPageBySlug: vi.fn().mockResolvedValue({ data: { content: '' } })
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/honors')
}))

function mountHonors() {
  const pinia = createPinia()
  const wrapper = mount(Honors, {
    global: {
      plugins: [pinia],
      stubs: {
        SiteLayout: { template: '<div class="layout-stub"><slot /></div>' }
      }
    }
  })
  return { wrapper, pinia }
}

describe('荣誉页页头文案（site_page_copy 行为）', () => {
  it('未配置时渲染内置文案', async () => {
    const { wrapper } = mountHonors()
    await flushPromises()

    expect(wrapper.get('.eyebrow').text()).toBe('Honor')
    expect(wrapper.get('.aph-title').text()).toBe('荣誉资质')
    expect(wrapper.get('.aph-sub').text()).toBe('资质证书、行业奖项与体系认证，是客户信任的注脚。')
  })

  it('配置后页头即时替换为设置值', async () => {
    const { wrapper, pinia } = mountHonors()
    await flushPromises()

    const store = useSiteSettingsStore(pinia)
    store.updateSiteSettings({
      site_page_copy: JSON.stringify({
        honors: { eyebrow: 'Awards', title: '资质荣誉墙', subtitle: '合作与认可的证明。' }
      })
    })
    await nextTick()

    expect(wrapper.get('.eyebrow').text()).toBe('Awards')
    expect(wrapper.get('.aph-title').text()).toBe('资质荣誉墙')
    expect(wrapper.get('.aph-sub').text()).toBe('合作与认可的证明。')
  })

  it('只配置部分字段时，其余字段仍回退内置文案', async () => {
    const { wrapper, pinia } = mountHonors()
    await flushPromises()

    const store = useSiteSettingsStore(pinia)
    store.updateSiteSettings({
      site_page_copy: JSON.stringify({ honors: { title: '资质荣誉墙' } })
    })
    await nextTick()

    expect(wrapper.get('.aph-title').text()).toBe('资质荣誉墙')
    expect(wrapper.get('.eyebrow').text()).toBe('Honor')
    expect(wrapper.get('.aph-sub').text()).toBe('资质证书、行业奖项与体系认证，是客户信任的注脚。')
  })
})
