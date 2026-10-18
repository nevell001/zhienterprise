import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import EnterpriseHome from './EnterpriseHome.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getFrontFriendLinkList } from '@/api/site/friendLink'

vi.mock('@/api/site', () => ({
  getArticleList: vi.fn().mockResolvedValue({ code: 200, rows: [] })
}))

vi.mock('@/api/site/friendLink', () => ({
  getFrontFriendLinkList: vi.fn().mockResolvedValue({ rows: [{ name: '示例客户' }] })
}))

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', component: EnterpriseHome },
    { path: '/business', component: { template: '<div />' } },
    { path: '/business/:id', component: { template: '<div />' } },
    { path: '/contact', component: { template: '<div />' } },
    { path: '/article/:id', component: { template: '<div />' } },
    { path: '/solutions/:id', component: { template: '<div />' } }
  ]
})

async function mountHome(settings: Record<string, unknown> = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useSiteSettingsStore().updateSiteSettings(settings as never)

  await router.push('/')
  await router.isReady()

  const wrapper = mount(EnterpriseHome, {
    global: { plugins: [pinia, router], stubs: { BannerCarousel: true } }
  })
  await flushPromises()
  return wrapper
}

describe('企业首页 · 客户 Logo 墙', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('友链关闭时不请求也不渲染 Logo 墙', async () => {
    const wrapper = await mountHome({ friend_link_enabled: 'false' })

    expect(getFrontFriendLinkList).not.toHaveBeenCalled()
    expect(wrapper.find('.logos').exists()).toBe(false)
  })

  it('友链开启时请求并渲染已审核友链', async () => {
    const wrapper = await mountHome({ friend_link_enabled: 'true' })

    expect(getFrontFriendLinkList).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.logos').exists()).toBe(true)
    expect(wrapper.find('.logos').text()).toContain('示例客户')
  })
})
