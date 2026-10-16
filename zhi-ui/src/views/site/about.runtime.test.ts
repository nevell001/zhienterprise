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

describe('关于页负责人区块（站点设置「个人信息」行为）', () => {
  it('填了简介/位置/个人网站时渲染负责人区块，并带上作者职位抬头', async () => {
    const { wrapper, pinia } = mountAbout()
    const store = useSiteSettingsStore(pinia)
    store.updateSiteSettings({
      author_title: '技术负责人',
      author_bio: '十年工业软件与数字化交付经验。',
      author_location: '中国·杭州',
      personal_website: 'https://team.example.com'
    })
    await nextTick()

    const profile = wrapper.get('.aph-profile')
    expect(profile.get('.aph-profile-head').text()).toBe('技术负责人 · 中国·杭州')
    expect(profile.get('.aph-profile-bio').text()).toBe('十年工业软件与数字化交付经验。')
    const link = profile.get('.aph-profile-link')
    expect(link.attributes('href')).toBe('https://team.example.com')
    expect(link.attributes('rel')).toContain('noopener')
  })

  it('只有作者职位（内容字段全空）时不渲染，避免空卡片', async () => {
    const { wrapper, pinia } = mountAbout()
    const store = useSiteSettingsStore(pinia)
    store.updateSiteSettings({ author_title: '网站管理员' })
    await nextTick()

    expect(wrapper.find('.aph-profile').exists()).toBe(false)
  })

  it.each([
    { name: '个人简介', patch: { author_bio: '十年工业软件与数字化交付经验。' } },
    { name: '位置信息', patch: { author_location: '中国·杭州' } }
  ])('只填$name也能让负责人区块出现', async ({ patch }) => {
    const { wrapper, pinia } = mountAbout()
    useSiteSettingsStore(pinia).updateSiteSettings(patch)
    await nextTick()

    expect(wrapper.find('.aph-profile').exists()).toBe(true)
  })

  it('只填个人网站时，区块渲染抬头与网站入口、不渲染简介', async () => {
    const { wrapper, pinia } = mountAbout()
    const store = useSiteSettingsStore(pinia)
    store.updateSiteSettings({
      author_title: '技术负责人',
      personal_website: 'https://team.example.com'
    })
    await nextTick()

    const profile = wrapper.get('.aph-profile')
    expect(profile.get('.aph-profile-head').text()).toBe('技术负责人')
    expect(profile.find('.aph-profile-bio').exists()).toBe(false)
    expect(profile.get('.aph-profile-link').attributes('href')).toBe('https://team.example.com')
  })
})
