import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import TagPage from './index.vue'

/**
 * 标签页行为测试（挂载真实组件 + memory router）。
 * 重点：/tag/:id 复用同一组件实例，快速切换标签时先发出的慢响应
 * 不得覆盖新标签的名称与文章列表。
 */
const getTagDetail = vi.fn()
const getArticlesByTag = vi.fn()
const getTagCloud = vi.fn()
const getArticleList = vi.fn()
const getSiteSettingsAnonymous = vi.fn()

vi.mock('@/api/site/tag', () => ({
  getTagDetail: (...args: unknown[]) => getTagDetail(...args),
  getArticlesByTag: (...args: unknown[]) => getArticlesByTag(...args),
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
  // 点击即请求下一页，便于验证翻页竞态
  SitePager: {
    props: ['total', 'pageSize', 'pageNum'],
    emits: ['page-change'],
    template:
      '<button type="button" class="pager-next" @click="$emit(\'page-change\', pageNum + 1)">下一页</button>'
  },
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

async function mountTag(path = '/tag/1') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'PublicSiteHome', component: { template: '<div />' } },
      { path: '/tag', name: 'PublicSiteTagList', component: { template: '<div />' } },
      { path: '/tag/:id', name: 'PublicSiteTag', component: TagPage }
    ]
  })
  router.push(path)
  await router.isReady()
  const wrapper = mount(TagPage, {
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
  getArticleList.mockResolvedValue({ code: 200, rows: [], total: 0 })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('标签页路由切换', () => {
  it('快速切换标签时丢弃慢的旧响应（名称与文章列表不被覆盖）', async () => {
    const first = deferred<{ data: { id: number; name: string } }>()
    const second = deferred<{ data: { id: number; name: string } }>()
    getTagDetail.mockImplementation((id: number) => (id === 1 ? first.promise : second.promise))
    getArticlesByTag.mockImplementation((id: number) =>
      Promise.resolve({
        rows: [{ id, title: `标签${id}文章` }],
        total: 1
      })
    )

    const { wrapper, router } = await mountTag('/tag/1')

    await router.push('/tag/2')
    second.resolve({ data: { id: 2, name: '标签二' } })
    await flushPromises()
    expect(wrapper.find('.tag-title').text()).toBe('标签二')
    expect(wrapper.findAll('.card-stub').map(card => card.text())).toEqual(['标签2文章'])

    // 旧标签 1 的响应此刻才返回：必须整体作废
    first.resolve({ data: { id: 1, name: '标签一' } })
    await flushPromises()
    expect(wrapper.find('.tag-title').text()).toBe('标签二')
    expect(wrapper.findAll('.card-stub').map(card => card.text())).toEqual(['标签2文章'])
  })

  it('切换标签后，旧标签迟到的翻页响应不会追加到新列表', async () => {
    const slowPage = deferred<{
      rows: { id: number; title: string }[]
      total: number
    }>()
    getTagDetail.mockImplementation((id: number) =>
      Promise.resolve({ data: { id, name: `标签${id}` } })
    )
    getArticlesByTag.mockImplementation((id: number, params: { pageNum?: number }) => {
      if (id === 1 && params.pageNum === 2) return slowPage.promise
      return Promise.resolve({
        rows: [{ id, title: `标签${id}第${params.pageNum}页` }],
        total: id === 1 ? 30 : 1
      })
    })

    const { wrapper, router } = await mountTag('/tag/1')
    expect(wrapper.findAll('.card-stub').map(card => card.text())).toEqual(['标签1第1页'])

    // 标签 1 的第 2 页在途；此时切到标签 2
    await wrapper.find('.pager-next').trigger('click')
    await router.push('/tag/2')
    await flushPromises()
    expect(wrapper.find('.tag-title').text()).toBe('标签2')
    expect(wrapper.findAll('.card-stub').map(card => card.text())).toEqual(['标签2第1页'])

    // 旧标签第 2 页此刻才返回：不得追加到标签 2 的列表
    slowPage.resolve({ rows: [{ id: 1, title: '标签1第2页' }], total: 30 })
    await flushPromises()
    expect(wrapper.findAll('.card-stub').map(card => card.text())).toEqual(['标签2第1页'])
  })
})
