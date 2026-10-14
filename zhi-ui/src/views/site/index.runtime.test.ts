import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import SiteHome from './index.vue'
import { useUserStore } from '@/stores/user'
import { getSiteSettings } from '@/api/site'
import { getPublishedPageBySlug } from '@/api/site/page'

vi.mock('@/api/site', () => ({
  getArticleList: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getSiteSettings: vi.fn().mockResolvedValue({ data: {} }),
  getHotArticles: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  searchArticles: vi.fn().mockResolvedValue({ rows: [], total: 0 })
}))

vi.mock('@/api/site/category', () => ({
  // /blog/category/list 不分页，分类数组在 data 里
  getCategoryList: vi.fn().mockResolvedValue({ code: 200, data: [] })
}))

vi.mock('@/api/site/tag', () => ({
  getTagCloud: vi.fn().mockResolvedValue({ data: [] })
}))

vi.mock('@/api/site/friendLink', () => ({
  getFrontFriendLinkList: vi.fn().mockResolvedValue({ rows: [] })
}))

vi.mock('@/api/site/page', () => ({
  getPublishedPageBySlug: vi.fn().mockResolvedValue({ code: 200, data: null }),
  getPublishedPages: vi.fn().mockResolvedValue({ data: [] })
}))

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', component: SiteHome, name: 'PublicRoot' },
    { path: '/news', component: SiteHome, name: 'PublicSiteArticleList' }
  ]
})

const stubs = {
  'el-avatar': { template: '<span><slot /></span>' },
  'el-button': { template: '<button><slot /></button>' },
  'el-dropdown': { template: '<div><slot /><slot name="dropdown" /></div>' },
  'el-dropdown-item': { template: '<div><slot /></div>' },
  'el-dropdown-menu': { template: '<div><slot /></div>' },
  'el-icon': { template: '<span><slot /></span>' }
}

function mountHome() {
  return mount(SiteHome, {
    global: {
      plugins: [createPinia(), router],
      stubs
    }
  })
}

describe('博客首页运行时渲染', () => {
  it('没有后端数据时也应该渲染首屏结构', async () => {
    router.push('/')
    await router.isReady()

    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.find('.mo-home-page').exists()).toBe(true)
    expect(wrapper.find('.home-hero h1').text()).toBe('记录与分享')
    expect(wrapper.find('.site-top-nav').exists()).toBe(true)
  })

  it('存在旧 token 时也应该渲染首屏，不应被用户下拉菜单阻断', async () => {
    router.push('/')
    await router.isReady()

    const pinia = createPinia()
    const userStore = useUserStore(pinia)
    userStore.token = 'stale-token'

    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    const wrapper = mount(SiteHome, {
      global: {
        plugins: [pinia, router],
        stubs
      }
    })
    await flushPromises()

    expect(wrapper.find('.mo-home-page').exists()).toBe(true)
    expect(wrapper.find('.home-hero h1').text()).toBe('记录与分享')
    expect(consoleErrorSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('Vue运行时错误:'),
      expect.anything()
    )

    consoleErrorSpy.mockRestore()
  })

  it('配置了 home_page_slug 时首页渲染该自定义页面，导航补「文章」入口', async () => {
    vi.mocked(getSiteSettings).mockResolvedValueOnce({
      data: { home_page_slug: 'company' }
    } as any)
    vi.mocked(getPublishedPageBySlug).mockResolvedValueOnce({
      code: 200,
      data: {
        id: 9,
        title: '星海科技',
        slug: 'company',
        content: '## 欢迎来到星海科技',
        format: 'markdown'
      }
    } as any)

    router.push('/')
    await router.isReady()

    const wrapper = mountHome()
    await flushPromises()

    expect(vi.mocked(getPublishedPageBySlug)).toHaveBeenCalledWith('company')
    expect(wrapper.text()).toContain('星海科技')
    expect(wrapper.text()).toContain('欢迎来到星海科技')
    // 文章列表没有被渲染，且首页模式隐藏了 Page 标签与更新/浏览元信息
    expect(wrapper.find('.mo-home-page').exists()).toBe(false)
    expect(wrapper.html()).not.toContain('>Page<')
    expect(wrapper.find('a[href="/news"]').exists()).toBe(true)
  })

  it('home_page_slug 对应页面不存在时降级回文章列表', async () => {
    vi.mocked(getSiteSettings).mockResolvedValueOnce({
      data: { home_page_slug: 'missing' }
    } as any)
    vi.mocked(getPublishedPageBySlug).mockRejectedValueOnce(new Error('404'))

    router.push('/')
    await router.isReady()

    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.find('.mo-home-page').exists()).toBe(true)
    expect(wrapper.find('.home-hero h1').text()).toBe('记录与分享')
  })

  it('设置未加载完成时不先渲染文章列表（避免闪动）', async () => {
    let resolveSettings: (value: any) => void = () => {}
    vi.mocked(getSiteSettings).mockImplementationOnce(
      () => new Promise(resolve => (resolveSettings = resolve)) as any
    )

    router.push('/')
    await router.isReady()

    const wrapper = mountHome()
    await Promise.resolve()

    expect(wrapper.find('.mo-home-page').exists()).toBe(false)

    resolveSettings({ data: {} })
    await flushPromises()

    expect(wrapper.find('.mo-home-page').exists()).toBe(true)
  })
})
