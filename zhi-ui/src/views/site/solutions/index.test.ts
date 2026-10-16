import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import SolutionsList from './index.vue'

/**
 * 方案列表页筛选行为测试（挂载真实组件 + memory router）。
 * 覆盖：行业/场景筛选过滤、筛选状态与 URL query 双向同步（方案文档 §3.3）、
 * 非法 query 值收敛、卡片点击携带 query 进详情、键盘可达、空状态清除筛选。
 */
const getArticleList = vi.fn()

vi.mock('@/api/site', () => ({
  getArticleList: (...args: unknown[]) => getArticleList(...args)
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/solutions')
}))

const articleOf = (id: number, title: string, industry: string, scenario: string) => ({
  id,
  title,
  summary: `摘要${id}`,
  industry,
  scenario
})

const ROWS = [
  articleOf(1, '制造业数字化', '制造业', '数字化转型,降本增效'),
  articleOf(2, '金融数据治理', '金融', '风险管控,数据治理'),
  articleOf(3, '零售全渠道', '零售', '降本增效,数据治理')
]

async function mountList(initialPath = '/solutions') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/solutions', name: 'PublicSolutionList', component: SolutionsList },
      {
        path: '/solutions/:id',
        name: 'PublicSolutionDetail',
        component: { template: '<div class="detail-stub" />' }
      }
    ]
  })
  router.push(initialPath)
  await router.isReady()
  const wrapper = mount(SolutionsList, {
    global: {
      plugins: [createPinia(), router],
      stubs: {
        'el-empty': {
          props: ['description'],
          template: '<div class="empty-stub">{{ description }}<slot /></div>'
        },
        'el-button': { template: '<button type="button"><slot /></button>' }
      }
    }
  })
  await flushPromises()
  return { wrapper, router }
}

const cardTitles = (wrapper: VueWrapper) => wrapper.findAll('.sol-board-card h2').map(h => h.text())

const tabByText = (wrapper: VueWrapper, label: string) =>
  wrapper.findAll('.sol-tabs .tag').find(b => b.text() === label)!

const scenarioByText = (wrapper: VueWrapper, label: string) =>
  wrapper.findAll('.sol-scenarios .tag').find(b => b.text() === label)!

beforeEach(() => {
  vi.clearAllMocks()
  getArticleList.mockResolvedValue({ code: 200, rows: [...ROWS], total: ROWS.length })
})

describe('方案列表页筛选与 URL 同步', () => {
  it('渲染全部方案并按数据聚合行业 Tab', async () => {
    const { wrapper } = await mountList()
    expect(cardTitles(wrapper)).toEqual(['制造业数字化', '金融数据治理', '零售全渠道'])
    expect(wrapper.findAll('.sol-tabs .tag').map(b => b.text())).toEqual([
      '全部',
      '制造业',
      '金融',
      '零售'
    ])
  })

  it('从 URL query 恢复筛选状态', async () => {
    const { wrapper, router } = await mountList('/solutions?industry=金融&scenario=风险管控')
    expect(cardTitles(wrapper)).toEqual(['金融数据治理'])
    expect(wrapper.find('.sol-tabs .tag.on').text()).toBe('金融')
    expect(scenarioByText(wrapper, '风险管控').classes()).toContain('on')
    expect(router.currentRoute.value.query.industry).toBe('金融')
  })

  it('切换行业写入 URL query（replace 同路径）', async () => {
    const { wrapper, router } = await mountList()
    await tabByText(wrapper, '金融').trigger('click')
    await flushPromises()
    expect(cardTitles(wrapper)).toEqual(['金融数据治理'])
    expect(router.currentRoute.value.path).toBe('/solutions')
    expect(router.currentRoute.value.query.industry).toBe('金融')
  })

  it('场景多选为或语义，选中值逗号连接写入 URL', async () => {
    const { wrapper, router } = await mountList()
    await scenarioByText(wrapper, '数据治理').trigger('click')
    await flushPromises()
    expect(cardTitles(wrapper)).toEqual(['金融数据治理', '零售全渠道'])
    expect(router.currentRoute.value.query.scenario).toBe('数据治理')

    await scenarioByText(wrapper, '降本增效').trigger('click')
    await flushPromises()
    expect(cardTitles(wrapper)).toEqual(['制造业数字化', '金融数据治理', '零售全渠道'])
    expect(router.currentRoute.value.query.scenario).toBe('数据治理,降本增效')

    // 取消「数据治理」：只剩降本增效命中
    await scenarioByText(wrapper, '数据治理').trigger('click')
    await flushPromises()
    expect(cardTitles(wrapper)).toEqual(['制造业数字化', '零售全渠道'])
    expect(router.currentRoute.value.query.scenario).toBe('降本增效')
  })

  it('URL 中不存在的数据值收敛为全部并清空 query', async () => {
    const { wrapper, router } = await mountList('/solutions?industry=不存在&scenario=不存在场景')
    expect(cardTitles(wrapper)).toHaveLength(3)
    expect(router.currentRoute.value.query).toEqual({})
  })

  it('点击卡片进入详情并携带筛选 query', async () => {
    const { wrapper, router } = await mountList('/solutions?industry=金融')
    await wrapper.find('.sol-board-card').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/solutions/2')
    expect(router.currentRoute.value.query.industry).toBe('金融')
  })

  it('卡片键盘回车同样进入详情（键盘可达）', async () => {
    const { wrapper, router } = await mountList()
    const card = wrapper.findAll('.sol-board-card')[1]
    expect(card.attributes('tabindex')).toBe('0')
    await card.trigger('keydown.enter')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/solutions/2')
  })

  it('空结果时显示空状态，「清除筛选」恢复全部并清空 query', async () => {
    const { wrapper, router } = await mountList('/solutions?industry=制造业&scenario=风险管控')
    expect(wrapper.findAll('.sol-board-card')).toHaveLength(0)
    expect(wrapper.find('.empty-stub').text()).toContain('该筛选下暂无方案')

    await wrapper.find('.empty-stub button').trigger('click')
    await flushPromises()
    expect(cardTitles(wrapper)).toHaveLength(3)
    expect(router.currentRoute.value.query).toEqual({})
  })

  it('总数超过已加载数时结果计数标注总数（截断透明化）', async () => {
    getArticleList.mockResolvedValue({ code: 200, rows: [...ROWS], total: 80 })
    const { wrapper } = await mountList()
    expect(wrapper.find('.sol-count').text()).toContain('正在展示 3 个方案')
    expect(wrapper.find('.sol-count').text()).toContain('共 80 个，已加载 3 个')
  })
})
