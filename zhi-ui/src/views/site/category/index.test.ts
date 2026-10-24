import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import CategoryPage from './index.vue'

/**
 * 分类页行为测试（挂载真实组件 + memory router）。
 * 重点：/category/:id 复用同一组件实例，快速切换分类时先发出的慢响应
 * 不得覆盖新分类的名称与文章列表（同类问题曾在详情页/搜索页出现）。
 */
const getCategoryDetail = vi.fn()
const getCategoryList = vi.fn()
const getTagCloud = vi.fn()
const getArticleList = vi.fn()
const getSiteSettingsAnonymous = vi.fn()

vi.mock('@/api/site/category', () => ({
  getCategoryDetail: (...args: unknown[]) => getCategoryDetail(...args),
  getCategoryList: (...args: unknown[]) => getCategoryList(...args)
}))
vi.mock('@/api/site/tag', () => ({
  getTagCloud: (...args: unknown[]) => getTagCloud(...args)
}))
vi.mock('@/api/site/article', () => ({
  getArticleList: (...args: unknown[]) => getArticleList(...args)
}))
vi.mock('@/api/site/setting', () => ({
  getSiteSettingsAnonymous: (...args: unknown[]) => getSiteSettingsAnonymous(...args)
}))

const stubs = {
  SiteLayout: { template: '<div><slot /></div>' },
  ArticleCard: {
    props: ['article'],
    template: '<div class="card-stub">{{ article.title }}</div>'
  },
  ArticleEmptyState: true,
  ArticleListSkeleton: true,
  SitePager: true,
  'el-icon': true,
  'el-button': true
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(r => {
    resolve = r
  })
  return { promise, resolve }
}

async function mountCategory(path = '/category/1') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'PublicSiteHome', component: { template: '<div />' } },
      { path: '/category', name: 'PublicSiteCategoryList', component: { template: '<div />' } },
      { path: '/category/:id', name: 'PublicSiteCategory', component: CategoryPage }
    ]
  })
  router.push(path)
  await router.isReady()
  const wrapper = mount(CategoryPage, {
    global: { plugins: [createPinia(), router], stubs }
  })
  await flushPromises()
  return { wrapper, router }
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('scrollTo', vi.fn())
  getSiteSettingsAnonymous.mockResolvedValue({})
  getTagCloud.mockResolvedValue({ data: [] })
  getCategoryList.mockResolvedValue({ data: [] })
  getArticleList.mockResolvedValue({ code: 200, rows: [], total: 0 })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('分类页路由切换', () => {
  it('快速切换分类时丢弃慢的旧响应（名称与文章列表不被覆盖）', async () => {
    const first = deferred<{ data: { id: number; name: string } }>()
    const second = deferred<{ data: { id: number; name: string } }>()
    getCategoryDetail.mockImplementation((key: string) =>
      key === '1' ? first.promise : second.promise
    )
    getArticleList.mockImplementation((params: { categoryId?: number }) =>
      Promise.resolve({
        code: 200,
        rows: [{ id: params.categoryId || 0, title: `分类${params.categoryId}文章` }],
        total: 1
      })
    )

    const { wrapper, router } = await mountCategory('/category/1')

    await router.push('/category/2')
    second.resolve({ data: { id: 2, name: '分类二' } })
    await flushPromises()
    expect(wrapper.find('.category-title').text()).toBe('分类二')
    expect(wrapper.findAll('.card-stub').map(card => card.text())).toEqual(['分类2文章'])

    // 旧分类 1 的响应此刻才返回：必须整体作废
    first.resolve({ data: { id: 1, name: '分类一' } })
    await flushPromises()
    expect(wrapper.find('.category-title').text()).toBe('分类二')
    expect(wrapper.findAll('.card-stub').map(card => card.text())).toEqual(['分类2文章'])
  })
})
