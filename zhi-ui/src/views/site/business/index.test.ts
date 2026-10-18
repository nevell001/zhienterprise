import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import BusinessList from './index.vue'

/**
 * 核心业务列表行为测试：案例（case）内容必须与 business 一起出现在前台列表，
 * 否则后台能发布案例、sitemap 也收录 /business/{id}，但站点上没有任何入口。
 */
const getArticleList = vi.fn()
const getCategoryList = vi.fn()

vi.mock('@/api/site', () => ({
  getArticleList: (...args: unknown[]) => getArticleList(...args)
}))

vi.mock('@/api/site/category', () => ({
  getCategoryList: (...args: unknown[]) => getCategoryList(...args)
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/business')
}))

const articleOf = (id: number, title: string, articleType: string) => ({
  id,
  title,
  summary: `摘要${id}`,
  articleType
})

async function mountList(initialPath = '/business') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/business', name: 'PublicBusinessList', component: BusinessList },
      {
        path: '/business/:id',
        name: 'PublicBusinessDetail',
        component: { template: '<div class="detail-stub" />' }
      }
    ]
  })
  router.push(initialPath)
  await router.isReady()
  const wrapper = mount(BusinessList, {
    global: {
      plugins: [createPinia(), router],
      stubs: {
        SiteLayout: { template: '<div class="layout-stub"><slot /></div>' },
        'el-empty': {
          props: ['description'],
          template: '<div class="empty-stub">{{ description }}</div>'
        },
        'el-pagination': { template: '<div class="pager-stub" />' },
        'el-button': { template: '<button type="button"><slot /></button>' }
      }
    }
  })
  await flushPromises()
  return { wrapper, router }
}

beforeEach(() => {
  vi.clearAllMocks()
  getCategoryList.mockResolvedValue({ code: 200, data: [] })
  getArticleList.mockResolvedValue({
    code: 200,
    rows: [
      articleOf(1, '站点运维托管服务', 'business'),
      articleOf(2, '客户案例：某制造企业官网升级实践', 'case')
    ],
    total: 2
  })
})

describe('核心业务列表的案例入口', () => {
  it('查询同时包含 business 与 case，并渲染案例卡片', async () => {
    const { wrapper } = await mountList()

    expect(getArticleList).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 9,
      articleTypes: 'business,case',
      categoryId: undefined
    })
    expect(wrapper.findAll('.biz-card h2').map(h => h.text())).toEqual([
      '站点运维托管服务',
      '客户案例：某制造企业官网升级实践'
    ])
  })

  it('点击案例卡片进入 /business/:id（案例与业务共用详情路由）', async () => {
    const { wrapper, router } = await mountList()

    await wrapper.findAll('.biz-card')[1].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/business/2')
  })
})
