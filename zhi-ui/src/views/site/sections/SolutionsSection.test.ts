import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import SolutionsSection from './SolutionsSection.vue'

/**
 * 首页解决方案板块点击行为测试（挂载真实组件）。
 * 覆盖：方案文章驱动时卡片/链接进对应详情（曾一律去 /business），
 * 手填与回退数据（无 id）回方案列表，键盘回车可达。
 */
const getArticleList = vi.fn()
const push = vi.fn()

vi.mock('@/api/site', () => ({
  getArticleList: (...args: unknown[]) => getArticleList(...args)
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

const RouterLinkStub = {
  props: { to: { type: [String, Object], required: true } },
  template: '<a class="lnk-stub" :href="String(to)"><slot /></a>'
}

async function mountSection(config?: Record<string, any>) {
  const wrapper = mount(SolutionsSection, {
    props: { config },
    global: {
      plugins: [createPinia()],
      stubs: { RouterLink: RouterLinkStub }
    }
  })
  await flushPromises()
  return wrapper
}

const articleRows = [
  { id: 7, title: '制造业数字化', summary: '打通产线数据' },
  { id: 8, title: '金融数据治理', summary: '统一数据标准' }
]

beforeEach(() => {
  vi.clearAllMocks()
})

describe('首页解决方案板块点击行为', () => {
  it('方案文章驱动时：卡片与「查看方案」链接都指向对应详情', async () => {
    getArticleList.mockResolvedValue({ code: 200, rows: [...articleRows] })
    const wrapper = await mountSection()

    expect(wrapper.findAll('.sol-card h3').map(h => h.text())).toEqual([
      '制造业数字化',
      '金融数据治理'
    ])
    expect(wrapper.findAll('.lnk-stub').map(a => a.attributes('href'))).toEqual([
      '/solutions/7',
      '/solutions/8'
    ])

    await wrapper.find('.sol-card').trigger('click')
    expect(push).toHaveBeenCalledWith('/solutions/7')
  })

  it('键盘回车同样进入详情（键盘可达）', async () => {
    getArticleList.mockResolvedValue({ code: 200, rows: [...articleRows] })
    const wrapper = await mountSection()

    const cards = wrapper.findAll('.sol-card')
    expect(cards[1].attributes('tabindex')).toBe('0')
    await cards[1].trigger('keydown.enter')
    expect(push).toHaveBeenCalledWith('/solutions/8')
  })

  it('无方案文章时回退手填项：点击回方案列表', async () => {
    getArticleList.mockResolvedValue({ code: 200, rows: [] })
    const wrapper = await mountSection({ items: [{ title: '手填方案', desc: '说明' }] })

    expect(wrapper.findAll('.sol-card h3').map(h => h.text())).toEqual(['手填方案'])
    expect(wrapper.find('.lnk-stub').attributes('href')).toBe('/solutions')

    await wrapper.find('.sol-card').trigger('click')
    expect(push).toHaveBeenCalledWith('/solutions')
  })

  it('手填项带数字 id 时点击进入详情', async () => {
    getArticleList.mockResolvedValue({ code: 200, rows: [] })
    const wrapper = await mountSection({ items: [{ id: 12, title: '关联方案', desc: '说明' }] })

    await wrapper.find('.sol-card').trigger('click')
    expect(push).toHaveBeenCalledWith('/solutions/12')
  })
})
