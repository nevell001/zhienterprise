import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import EnterpriseNewsList from './EnterpriseNewsList.vue'

/**
 * 新闻列表侧栏回归：归档页已随博客功能下线，侧栏不得再输出 /blog 死链；
 * 热门标签必须走公开短路由 /tag/:id。
 */
const getArticleList = vi.fn()
const getTagCloud = vi.fn()

vi.mock('@/api/site', () => ({
  getArticleList: (...args: unknown[]) => getArticleList(...args)
}))

vi.mock('@/api/site/tag', () => ({
  getTagCloud: (...args: unknown[]) => getTagCloud(...args)
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/news')
}))

async function mountNews() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/news', name: 'PublicSiteArticleList', component: EnterpriseNewsList },
      { path: '/article/:id', name: 'PublicSiteArticleDetail', component: { template: '<div />' } },
      { path: '/tag/:id', name: 'PublicSiteTag', component: { template: '<div />' } }
    ]
  })
  router.push('/news')
  await router.isReady()
  const wrapper = mount(EnterpriseNewsList, {
    global: {
      plugins: [createPinia(), router],
      stubs: {
        'el-pagination': { template: '<div class="pager-stub" />' }
      }
    }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined)
  getArticleList.mockResolvedValue({ code: 200, rows: [], total: 0 })
  getTagCloud.mockResolvedValue({
    data: [
      { id: 5, name: '行业资讯' },
      { id: 8, name: '数字化' }
    ]
  })
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('新闻列表侧栏', () => {
  it('热门标签指向 /tag/:id，不再引用已下线的 /blog 路由', async () => {
    const wrapper = await mountNews()
    const links = wrapper.findAll('.enh-side-link')
    expect(links.map(link => link.text())).toEqual(['行业资讯', '数字化'])
    expect(links.map(link => link.attributes('href'))).toEqual(['/tag/5', '/tag/8'])
    expect(wrapper.html()).not.toContain('/blog/')
  })

  it('归档功能下线后不再渲染归档入口', async () => {
    const wrapper = await mountNews()
    expect(wrapper.text()).not.toContain('归档')
    expect(wrapper.find('.enh-side-block h4').text()).toBe('热门标签')
  })
})
