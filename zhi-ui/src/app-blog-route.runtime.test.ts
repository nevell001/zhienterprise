import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

vi.mock('@/api/blog', () => ({
  getArticleList: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getBlogSettings: vi.fn().mockResolvedValue({ data: {} }),
  getHotArticles: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  searchArticles: vi.fn().mockResolvedValue({ rows: [], total: 0 })
}))

vi.mock('@/api/blog/category', () => ({
  // /blog/category/list 不分页，分类数组在 data 里
  getCategoryList: vi.fn().mockResolvedValue({ code: 200, data: [] })
}))

vi.mock('@/api/blog/tag', () => ({
  getTagCloud: vi.fn().mockResolvedValue({ data: [] })
}))

vi.mock('@/api/blog/friendLink', () => ({
  getFrontFriendLinkList: vi.fn().mockResolvedValue({ rows: [] })
}))

describe('App 博客路由运行时渲染', () => {
  it('真实路由进入 /blog 时应该渲染博客首页', async () => {
    window.scrollTo = vi.fn()

    await router.push('/blog')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), router],
        stubs: {
          'el-avatar': { template: '<span><slot /></span>' },
          'el-button': { template: '<button><slot /></button>' },
          'el-dropdown': { template: '<div><slot /><slot name="dropdown" /></div>' },
          'el-dropdown-item': { template: '<div><slot /></div>' },
          'el-dropdown-menu': { template: '<div><slot /></div>' },
          'el-icon': { template: '<span><slot /></span>' }
        }
      }
    })

    // 首页等设置返回后才决定渲染文章列表还是自定义页面（未返回时是骨架屏）
    await flushPromises()

    expect(wrapper.find('.mo-home-page').exists()).toBe(true)
    expect(wrapper.text()).toContain('记录与分享')
  })
})
