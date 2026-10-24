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

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(r => {
    resolve = r
  })
  return { promise, resolve }
}

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

describe('分类筛选与 URL 双向同步', () => {
  beforeEach(() => {
    getCategoryList.mockResolvedValue({
      code: 200,
      data: [{ id: 3, name: '运维服务', alias: 'ops' }]
    })
  })

  it('筛选后点击卡片进入详情携带筛选 query（返回可复原）', async () => {
    const { wrapper, router } = await mountList('/business?cat=ops')

    await wrapper.findAll('.biz-card')[0].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/business/1')
    expect(router.currentRoute.value.query.cat).toBe('ops')
  })

  it('同一路由内 query 变化时同步分类筛选并重新加载', async () => {
    const { wrapper, router } = await mountList()

    await router.replace({ path: '/business', query: { cat: 'ops' } })
    await flushPromises()

    expect(wrapper.find('.biz-tabs .tag.on').text()).toBe('运维服务')
    expect(getArticleList).toHaveBeenLastCalledWith(
      expect.objectContaining({ categoryId: 3, pageNum: 1 })
    )
  })
})

describe('列表筛选竞态', () => {
  it('快速切换分类时慢的旧筛选响应不覆盖新结果', async () => {
    const slowAll = deferred<{
      code: number
      rows: ReturnType<typeof articleOf>[]
      total: number
    }>()
    getCategoryList.mockResolvedValue({
      code: 200,
      data: [{ id: 3, name: '运维服务', alias: 'ops' }]
    })
    getArticleList.mockImplementation((params: { categoryId?: number }) => {
      // 首次「全部」请求故意挂起，模拟慢响应
      if (params.categoryId === undefined) return slowAll.promise
      return Promise.resolve({
        code: 200,
        rows: [articleOf(9, '运维服务内容', 'business')],
        total: 1
      })
    })

    const { wrapper } = await mountList()
    const opsTab = wrapper.findAll('.biz-tabs .tag').find(t => t.text() === '运维服务')
    expect(opsTab).toBeTruthy()

    await opsTab!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('运维服务内容')

    // 「全部」的慢响应此刻才返回：不得覆盖筛选后的列表
    slowAll.resolve({ code: 200, rows: [articleOf(1, '全部内容', 'business')], total: 1 })
    await flushPromises()
    expect(wrapper.text()).toContain('运维服务内容')
    expect(wrapper.text()).not.toContain('全部内容')
  })
})
