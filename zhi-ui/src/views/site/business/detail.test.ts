import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import BusinessDetail from './detail.vue'

/**
 * 业务/方案共用详情页的行为测试（挂载真实组件 + memory router）。
 * 覆盖：相关推荐查询按文章类型分流（曾硬编码 business）、相关卡点击进同类型详情、
 * 「返回」带回筛选 query（方案文档 §3.2 返回可复原）、键盘可达。
 */
const getArticleDetail = vi.fn()
const getArticleList = vi.fn()
const updateArticleViewCount = vi.fn()

vi.mock('@/api/site/article', () => ({
  getArticleDetail: (...args: unknown[]) => getArticleDetail(...args),
  getArticleList: (...args: unknown[]) => getArticleList(...args),
  updateArticleViewCount: (...args: unknown[]) => updateArticleViewCount(...args)
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/x')
}))

vi.mock('@/utils/jsonld', () => ({
  setJsonLd: vi.fn(),
  removeJsonLd: vi.fn()
}))

const stubs = {
  SiteLayout: { template: '<div class="layout-stub"><slot /></div>' },
  SiteBreadcrumb: { template: '<div class="crumb-stub" />' },
  'el-empty': {
    props: ['description'],
    template: '<div class="empty-stub">{{ description }}</div>'
  }
}

const articleOf = (overrides: Record<string, unknown> = {}) => ({
  id: 7,
  title: '制造业数字化',
  summary: '打通产线数据',
  articleType: 'solution',
  content: '<p>正文</p>',
  createTime: '2026-10-01 10:00:00',
  ...overrides
})

async function mountDetail(initialPath: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/solutions', component: { template: '<div class="list-stub" />' } },
      { path: '/solutions/:id', name: 'PublicSolutionDetail', component: BusinessDetail },
      { path: '/business', component: { template: '<div class="list-stub" />' } },
      { path: '/business/:id', name: 'PublicBusinessDetail', component: BusinessDetail },
      { path: '/contact', component: { template: '<div />' } }
    ]
  })
  router.push(initialPath)
  await router.isReady()
  const wrapper = mount(BusinessDetail, {
    global: {
      plugins: [createPinia(), router],
      stubs
    }
  })
  await flushPromises()
  return { wrapper, router }
}

beforeEach(() => {
  vi.clearAllMocks()
  updateArticleViewCount.mockResolvedValue({ code: 200 })
})

describe('详情页相关推荐与返回行为', () => {
  it('方案详情的相关推荐查询同类型 solution（排除自身）', async () => {
    getArticleDetail.mockResolvedValue({ code: 200, data: { article: articleOf() } })
    getArticleList.mockResolvedValue({
      code: 200,
      rows: [
        articleOf({ id: 7, title: '自身' }),
        articleOf({ id: 8, title: '金融数据治理', summary: '统一数据标准' })
      ]
    })
    const { wrapper } = await mountDetail('/solutions/7')

    expect(getArticleList).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 4,
      articleType: 'solution'
    })
    expect(wrapper.findAll('.bd-rel-card h3').map(h => h.text())).toEqual(['金融数据治理'])
  })

  it('业务详情的相关推荐仍查询 business', async () => {
    getArticleDetail.mockResolvedValue({
      code: 200,
      data: { article: articleOf({ id: 3, articleType: 'business', title: '站点托管服务' }) }
    })
    getArticleList.mockResolvedValue({ code: 200, rows: [] })
    await mountDetail('/business/3')

    expect(getArticleList).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 4,
      articleType: 'business'
    })
  })

  it('浏览量由详情接口内部计数，页面不重复提交计数请求（曾致 PV 翻倍）', async () => {
    getArticleDetail.mockResolvedValue({ code: 200, data: { article: articleOf() } })
    getArticleList.mockResolvedValue({ code: 200, rows: [] })
    await mountDetail('/solutions/7')

    expect(getArticleDetail).toHaveBeenCalled()
    expect(updateArticleViewCount).not.toHaveBeenCalled()
  })

  it('相关卡点击进入同类型详情并携带筛选 query（键盘可达）', async () => {
    getArticleDetail.mockResolvedValue({ code: 200, data: { article: articleOf() } })
    getArticleList.mockResolvedValue({
      code: 200,
      rows: [articleOf({ id: 8, title: '金融数据治理', summary: '统一数据标准' })]
    })
    const { wrapper, router } = await mountDetail('/solutions/7?industry=金融')

    const card = wrapper.find('.bd-rel-card')
    expect(card.attributes('tabindex')).toBe('0')
    await card.trigger('keydown.enter')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/solutions/8')
    expect(router.currentRoute.value.query.industry).toBe('金融')
  })

  it('返回列表带回筛选 query（返回可复原）', async () => {
    getArticleDetail.mockResolvedValue({ code: 200, data: { article: articleOf() } })
    getArticleList.mockResolvedValue({ code: 200, rows: [] })
    const { wrapper, router } = await mountDetail('/solutions/7?industry=金融')

    await wrapper.find('.bd-back').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/solutions')
    expect(router.currentRoute.value.query.industry).toBe('金融')
  })
})
