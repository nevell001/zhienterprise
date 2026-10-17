import { describe, it, expect, vi, beforeEach } from 'vitest'
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

describe('关于页「负责人」区块已下线', () => {
  /**
   * 该区块的数据源是「站点设置 → 个人信息」tab，tab 已随需求移除，
   * 区块与 Person 结构化数据同步删除。锁住它们不会因残留代码悄悄渲染出来。
   */
  it('不再渲染负责人区块（.aph-profile）', async () => {
    const { wrapper, pinia } = mountAbout()
    const store = useSiteSettingsStore(pinia)
    // 即便旧键被注入，也不应渲染出负责人区块
    store.updateSiteSettings({
      author_title: '技术负责人',
      author_bio: '十年经验',
      author_location: '中国·北京',
      personal_website: 'https://example.com'
    })
    await nextTick()
    await nextTick()

    expect(wrapper.find('.aph-profile').exists()).toBe(false)
    expect(wrapper.find('.aph-profile-head').exists()).toBe(false)
    expect(wrapper.find('.aph-profile-bio').exists()).toBe(false)
  })

  it('不再注入 Person 结构化数据', async () => {
    mountAbout()
    await nextTick()
    await nextTick()

    expect(document.getElementById('zh-jsonld-person')).toBeNull()
  })
})
