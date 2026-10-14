import { describe, expect, it, vi, beforeEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import TagOverview from './list.vue'

const getTagCloud = vi.fn()

vi.mock('@/api/site/tag', () => ({
  getTagCloud: (...args: unknown[]) => getTagCloud(...args)
}))

const stubs = {
  SiteLayout: { template: '<div><slot /></div>' },
  RouterLink: { template: '<a><slot /></a>' },
  'el-icon': { template: '<i><slot /></i>' },
  'el-button': { template: '<button><slot /></button>' },
  'el-empty': {
    props: ['description'],
    template: '<div class="stub-empty">{{ description }}<slot /></div>'
  }
}

const mountPage = () =>
  mount(TagOverview, {
    global: {
      stubs,
      directives: { loading: {} },
      mocks: { $router: { push: vi.fn() } }
    }
  })

const cloud = (data: unknown) => ({ code: 200, msg: '操作成功', data })

const chipTexts = (wrapper: ReturnType<typeof mountPage>) =>
  wrapper.findAll('.tag-chip').map(chip => chip.text())

const chipClasses = (wrapper: ReturnType<typeof mountPage>) =>
  wrapper.findAll('.tag-chip').map(chip => chip.classes())

describe('标签总览页', () => {
  beforeEach(() => {
    getTagCloud.mockReset()
  })

  it('应该按接口的 article_count 渲染文章数（回归：此前读 articleCount 恒为空）', async () => {
    getTagCloud.mockResolvedValue(
      cloud([
        { id: 6, name: '后端开发', color: '#337ecc', article_count: 3 },
        { id: 2, name: 'Vue.js', color: '#4fc08d', article_count: 2 }
      ])
    )

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findAll('.tag-chip')).toHaveLength(2)
    expect(chipTexts(wrapper)).toEqual(['后端开发3', 'Vue.js2'])
    expect(wrapper.findAll('.tag-count').map(node => node.text())).toEqual(['3', '2'])
  })

  it('字号分级应由真实文章数决定，而不是按索引取模', async () => {
    // 索引顺序刻意与热度顺序错开：若仍按索引分级，Vue.js 会得到比后端开发更大的级别
    getTagCloud.mockResolvedValue(
      cloud([
        { id: 2, name: 'Vue.js', article_count: 1 },
        { id: 6, name: '后端开发', article_count: 3 },
        { id: 1, name: 'Java', article_count: 2 }
      ])
    )

    const wrapper = mountPage()
    await flushPromises()

    // 热度 3/2/1（max=3）→ 级别 4/3/1
    expect(chipClasses(wrapper)).toEqual([
      expect.arrayContaining(['is-level-4']),
      expect.arrayContaining(['is-level-3']),
      expect.arrayContaining(['is-level-1'])
    ])
  })

  it('应该按文章数降序排列，与接口返回顺序无关', async () => {
    getTagCloud.mockResolvedValue(
      cloud([
        { id: 1, name: '少', article_count: 1 },
        { id: 2, name: '多', article_count: 5 },
        { id: 3, name: '中', article_count: 3 }
      ])
    )

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findAll('.tag-name').map(node => node.text())).toEqual(['多', '中', '少'])
  })

  it('应该把标签自身颜色用作强调色，未配置时不写内联变量', async () => {
    getTagCloud.mockResolvedValue(
      cloud([
        { id: 1, name: '有颜色', color: '#f7df1e', article_count: 1 },
        { id: 2, name: '无颜色', article_count: 1 }
      ])
    )

    const wrapper = mountPage()
    await flushPromises()

    const chipNamed = (name: string) =>
      wrapper.findAll('.tag-chip').find(chip => chip.text().includes(name))!
    expect(chipNamed('有颜色').attributes('style')).toContain('--tag-accent: #f7df1e')
    expect(chipNamed('无颜色').attributes('style') ?? '').not.toContain('--tag-accent')
  })

  it('没有已发布文章的标签应降噪但保留入口', async () => {
    getTagCloud.mockResolvedValue(
      cloud([
        { id: 1, name: '有文章', article_count: 2 },
        { id: 2, name: '空标签', article_count: 0 }
      ])
    )

    const wrapper = mountPage()
    await flushPromises()

    const [filled, empty] = wrapper.findAll('.tag-chip')
    expect(filled.classes()).not.toContain('is-empty')
    expect(empty.classes()).toContain('is-empty')
    expect(empty.find('.tag-count').exists()).toBe(false)
    expect(empty.text()).toContain('空标签')
  })

  it('全站尚无文章时统一普通尺寸，不出现空白级别', async () => {
    getTagCloud.mockResolvedValue(
      cloud([
        { id: 1, name: '甲', article_count: 0 },
        { id: 2, name: '乙', article_count: 0 }
      ])
    )

    const wrapper = mountPage()
    await flushPromises()

    const levels = chipClasses(wrapper).map(classes =>
      classes.find(name => name.startsWith('is-level-'))
    )
    expect(levels).toEqual(['is-level-1', 'is-level-1'])
  })

  it('应该兼容 data 包装、rows 分页与裸数组三种形态', async () => {
    getTagCloud.mockResolvedValue(cloud([{ id: 1, name: 'data 形态', article_count: 1 }]))
    const dataWrapper = mountPage()
    await flushPromises()
    expect(dataWrapper.text()).toContain('data 形态')

    getTagCloud.mockResolvedValue({ code: 200, rows: [{ id: 2, name: 'rows 形态' }], total: 1 })
    const rowsWrapper = mountPage()
    await flushPromises()
    expect(rowsWrapper.text()).toContain('rows 形态')

    getTagCloud.mockResolvedValue([{ id: 3, name: '裸数组' }])
    const arrayWrapper = mountPage()
    await flushPromises()
    expect(arrayWrapper.text()).toContain('裸数组')
  })

  it('没有标签时展示空状态，标签名缺失的脏数据被丢弃', async () => {
    getTagCloud.mockResolvedValue(cloud([]))
    const emptyWrapper = mountPage()
    await flushPromises()
    expect(emptyWrapper.findAll('.tag-chip')).toHaveLength(0)
    expect(emptyWrapper.text()).toContain('暂无标签')

    getTagCloud.mockResolvedValue(cloud([{ id: 9 }, { id: 10, name: '有效', article_count: 1 }]))
    const dirtyWrapper = mountPage()
    await flushPromises()
    expect(chipTexts(dirtyWrapper)).toEqual(['有效1'])
  })

  it('接口异常时降级为空状态而不是抛错', async () => {
    getTagCloud.mockRejectedValue(new Error('network down'))

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findAll('.tag-chip')).toHaveLength(0)
    expect(wrapper.text()).toContain('暂无标签')
  })
})
