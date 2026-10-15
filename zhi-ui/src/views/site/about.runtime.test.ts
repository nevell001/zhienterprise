import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { nextTick } from 'vue'
import About from './about.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'

/**
 * 关于页页头文案（行为）：默认标题跟随站点名称、副题跟随站点描述
 * （保持既有行为），配置 site_page_copy.about 后页面文案优先。
 */
vi.mock('@/api/site/page', () => ({
  getPublishedPageBySlug: vi.fn().mockResolvedValue({ data: { content: '' } })
}))

vi.mock('@/api/statistics', () => ({
  getPublicStatisticsOverview: vi.fn().mockResolvedValue({ data: {} })
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/about')
}))

function mountAbout() {
  const pinia = createPinia()
  const wrapper = mount(About, {
    global: {
      plugins: [pinia],
      stubs: {
        SiteLayout: { template: '<div class="layout-stub"><slot /></div>' }
      }
    }
  })
  return { wrapper, pinia }
}

describe('关于页页头文案（site_page_copy 行为）', () => {
  it('未配置页面文案时：标题跟随站点名称、副题跟随站点描述', async () => {
    const { wrapper, pinia } = mountAbout()
    const store = useSiteSettingsStore(pinia)
    store.updateSiteSettings({ blog_name: '示例科技', blog_desc: '专注数字化交付' })
    await nextTick()

    expect(wrapper.get('.eyebrow').text()).toBe('About Us')
    expect(wrapper.get('.aph-title').text()).toBe('示例科技')
    expect(wrapper.get('.aph-sub').text()).toBe('专注数字化交付')
  })

  it('页面文案优先于站点名称与描述', async () => {
    const { wrapper, pinia } = mountAbout()
    await flushPromises()

    const store = useSiteSettingsStore(pinia)
    store.updateSiteSettings({
      blog_name: '示例科技',
      blog_desc: '专注数字化交付',
      site_page_copy: JSON.stringify({
        about: { eyebrow: '关于我们', title: '认识示例科技', subtitle: '十二年行业深耕。' }
      })
    })
    await nextTick()

    expect(wrapper.get('.eyebrow').text()).toBe('关于我们')
    expect(wrapper.get('.aph-title').text()).toBe('认识示例科技')
    expect(wrapper.get('.aph-sub').text()).toBe('十二年行业深耕。')
  })
})
