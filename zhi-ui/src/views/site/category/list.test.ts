import { describe, expect, it, vi, beforeEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import CategoryOverview from './list.vue'

const getCategoryList = vi.fn()

vi.mock('@/api/site/category', () => ({
  getCategoryList: (...args: unknown[]) => getCategoryList(...args)
}))

const stubs = {
  SiteLayout: { template: '<div><slot /></div>' },
  RouterLink: { template: '<a><slot /></a>' },
  'el-button': { template: '<button><slot /></button>' },
  'el-empty': {
    props: ['description'],
    template: '<div class="stub-empty">{{ description }}<slot /></div>'
  }
}

const mountPage = () =>
  mount(CategoryOverview, {
    global: {
      stubs,
      directives: { loading: {} },
      mocks: { $router: { push: vi.fn() } }
    }
  })

describe('分类总览页', () => {
  beforeEach(() => {
    getCategoryList.mockReset()
  })

  it('后端把分类放在 data 里时应该渲染分类卡片', async () => {
    // 回归：/blog/category/list 返回 AjaxResult.data（不分页），
    // 页面此前只读 rows，导致永远显示「暂无分类」
    getCategoryList.mockResolvedValue({
      code: 200,
      msg: '操作成功',
      data: [{ id: 1, name: '技术分享', description: '技术相关', articleCount: 2 }]
    })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findAll('.cat-card')).toHaveLength(1)
    expect(wrapper.text()).toContain('技术分享')
    expect(wrapper.text()).toContain('2 篇文章')
    expect(wrapper.text()).not.toContain('暂无分类')
  })

  it('应该兼容 rows 分页形态与裸数组', async () => {
    getCategoryList.mockResolvedValue({
      code: 200,
      rows: [{ id: 2, name: '生活随笔', description: '' }],
      total: 1
    })

    const rowsWrapper = mountPage()
    await flushPromises()
    expect(rowsWrapper.text()).toContain('生活随笔')

    getCategoryList.mockResolvedValue([{ id: 3, name: '学习笔记' }])
    const arrayWrapper = mountPage()
    await flushPromises()
    expect(arrayWrapper.text()).toContain('学习笔记')
  })

  it('没有分类时展示空状态', async () => {
    getCategoryList.mockResolvedValue({ code: 200, msg: '操作成功', data: [] })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findAll('.cat-card')).toHaveLength(0)
    expect(wrapper.text()).toContain('暂无分类')
  })

  it('接口异常时降级为空状态而不是抛错', async () => {
    getCategoryList.mockRejectedValue(new Error('network down'))

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findAll('.cat-card')).toHaveLength(0)
    expect(wrapper.text()).toContain('暂无分类')
  })
})
