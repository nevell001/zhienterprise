import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ArticleDetail from './detail.vue'

/**
 * 文章详情页评论行为测试（挂载真实组件）。
 * 覆盖：楼层嵌套渲染、顶级评论分页、排序切换、回复 parentId、评论数同步、越界页回退。
 */
const getArticleDetail = vi.fn()
const getRelatedArticles = vi.fn()
const getArticleComments = vi.fn()
const addBlogComment = vi.fn()
const updateMyComment = vi.fn()
const deleteMyComment = vi.fn()
const getArticleLikeStatus = vi.fn()
const getCommentLikedStatuses = vi.fn()
const toggleCommentLike = vi.fn()
const toggleArticleLike = vi.fn()
const toggleBookmark = vi.fn()
const getBlogSettings = vi.fn()
const getBlogSettingsAnonymous = vi.fn()

vi.mock('@/api/blog/article', () => ({
  getArticleDetail: (...args: unknown[]) => getArticleDetail(...args),
  getRelatedArticles: (...args: unknown[]) => getRelatedArticles(...args)
}))

vi.mock('@/api/blog/like', () => ({
  getArticleLikeStatus: (...args: unknown[]) => getArticleLikeStatus(...args),
  toggleArticleLike: (...args: unknown[]) => toggleArticleLike(...args),
  toggleCommentLike: (...args: unknown[]) => toggleCommentLike(...args),
  getCommentLikedStatuses: (...args: unknown[]) => getCommentLikedStatuses(...args)
}))

vi.mock('@/api/blog/bookmark', () => ({
  toggleBookmark: (...args: unknown[]) => toggleBookmark(...args)
}))

vi.mock('@/api/blog/comment', () => ({
  getArticleComments: (...args: unknown[]) => getArticleComments(...args),
  addBlogComment: (...args: unknown[]) => addBlogComment(...args),
  updateMyComment: (...args: unknown[]) => updateMyComment(...args),
  deleteMyComment: (...args: unknown[]) => deleteMyComment(...args)
}))

vi.mock('@/api/blog/setting', () => ({
  getBlogSettings: (...args: unknown[]) => getBlogSettings(...args),
  getBlogSettingsAnonymous: (...args: unknown[]) => getBlogSettingsAnonymous(...args)
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/blog')
}))

const messageSuccess = vi.fn()
const messageError = vi.fn()
const messageInfo = vi.fn()

vi.mock('@/plugins/element-plus-service', () => ({
  ElMessage: {
    success: (...args: unknown[]) => messageSuccess(...args),
    error: (...args: unknown[]) => messageError(...args),
    info: (...args: unknown[]) => messageInfo(...args)
  },
  ElMessageBox: {
    confirm: vi.fn(),
    prompt: vi.fn()
  }
}))

const stubs = {
  BlogLayout: { template: '<div class="layout-stub"><slot /></div>' },
  ArticleTOC: { template: '<div />' },
  ShareButton: { template: '<button type="button" />' },
  'el-skeleton': { template: '<div><slot name="template" /><slot /></div>' },
  'el-skeleton-item': { template: '<div />' },
  'el-icon': { template: '<span><slot /></span>' },
  'el-button': { template: '<button type="button"><slot /></button>' },
  'el-form': {
    template: '<form><slot /></form>',
    methods: { validate: () => Promise.resolve() }
  },
  'el-form-item': { template: '<div><slot /></div>' },
  'el-input': { template: '<input />' },
  // BlogPager 内部用它渲染页码，点击等价于用户翻到第 2 页
  'el-pagination': {
    props: ['total', 'pageSize', 'currentPage'],
    emits: ['current-change'],
    template:
      '<button type="button" class="pager-page-2" @click="$emit(\'current-change\', 2)">2</button>'
  }
}

async function mountDetail() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/article/:id', name: 'PublicBlogArticleDetail', component: ArticleDetail }
    ]
  })
  router.push('/article/1')
  await router.isReady()
  return mount(ArticleDetail, {
    global: {
      plugins: [createPinia(), router],
      stubs
    }
  })
}

const commentRow = (overrides: Record<string, unknown> = {}) => ({
  id: 1,
  nickname: '楼主',
  content: '这是顶楼',
  likeCount: 0,
  createTime: '2026-01-01 10:00:00',
  replies: [],
  ...overrides
})

describe('文章详情页评论（行为）', () => {
  beforeAll(() => {
    // jsdom 不实现 scrollIntoView（回复/翻页会滚动到表单/评论区）
    Element.prototype.scrollIntoView = vi.fn()
  })

  beforeEach(() => {
    vi.clearAllMocks()
    getArticleDetail.mockResolvedValue({
      code: 200,
      data: {
        article: {
          id: 1,
          title: '测试文章',
          content: '<p>正文内容</p>',
          commentCount: 3,
          likeCount: 0,
          viewCount: 0,
          createTime: '2026-01-01 10:00:00'
        },
        extraInfo: {}
      }
    })
    getRelatedArticles.mockResolvedValue({ data: [] })
    getBlogSettings.mockResolvedValue({
      code: 200,
      data: { comment_enabled: 'true', comment_review: 'true' }
    })
    getBlogSettingsAnonymous.mockResolvedValue({ data: {} })
    getArticleComments.mockResolvedValue({ rows: [], total: 0 })
    addBlogComment.mockResolvedValue({ code: 200, data: { commentCount: 3 } })
  })

  it('顶级评论下嵌套展示其回复，标题显示含回复的评论总数', async () => {
    getArticleComments.mockResolvedValue({
      rows: [
        commentRow({
          replies: [
            {
              id: 11,
              parentId: 1,
              nickname: '回复者',
              content: '一层回复',
              likeCount: 0,
              createTime: '2026-01-01 11:00:00'
            },
            {
              id: 12,
              parentId: 11,
              nickname: '追评者',
              content: '子回复',
              likeCount: 0,
              createTime: '2026-01-01 12:00:00'
            }
          ]
        })
      ],
      total: 1
    })

    const wrapper = await mountDetail()
    await flushPromises()

    expect(wrapper.findAll('.comment-item')).toHaveLength(1)
    expect(wrapper.findAll('.reply-item')).toHaveLength(2)
    expect(wrapper.text()).toContain('一层回复')
    expect(wrapper.text()).toContain('子回复')
    // 标题统计含回复的已发布总数（article.commentCount）
    expect(wrapper.find('.comment-head h3').text()).toContain('评论 (3)')
    // 顶级评论分页：第一页、10 条/页、默认最新排序
    expect(getArticleComments).toHaveBeenCalledWith(1, {
      pageNum: 1,
      pageSize: 10,
      sort: 'newest'
    })
  })

  it('顶级评论超过一页时显示分页，翻页请求对应页码', async () => {
    const pageRows = (n: number) =>
      Array.from({ length: 10 }, (_, i) =>
        commentRow({ id: n * 100 + i, nickname: `用户${i}`, content: `第${n}页评论${i}` })
      )
    getArticleComments
      .mockResolvedValueOnce({ rows: pageRows(1), total: 25 })
      .mockResolvedValueOnce({ rows: pageRows(2), total: 25 })

    const wrapper = await mountDetail()
    await flushPromises()

    expect(wrapper.find('.pagination-container').exists()).toBe(true)
    expect(wrapper.text()).toContain('第1页评论0')

    await wrapper.find('.pager-page-2').trigger('click')
    await flushPromises()

    expect(getArticleComments).toHaveBeenLastCalledWith(1, {
      pageNum: 2,
      pageSize: 10,
      sort: 'newest'
    })
    expect(wrapper.text()).toContain('第2页评论0')
  })

  it('评论只有一页时不渲染分页器', async () => {
    getArticleComments.mockResolvedValue({ rows: [commentRow()], total: 1 })

    const wrapper = await mountDetail()
    await flushPromises()

    expect(wrapper.find('.pagination-container').exists()).toBe(false)
  })

  it('切换排序回到第一页并按最早排序重新拉取', async () => {
    getArticleComments.mockResolvedValue({ rows: [commentRow()], total: 25 })

    const wrapper = await mountDetail()
    await flushPromises()

    const sortButtons = wrapper.findAll('.comment-sort button')
    expect(sortButtons).toHaveLength(2)
    expect(sortButtons[0].classes()).toContain('active')

    await sortButtons[1].trigger('click')
    await flushPromises()

    expect(getArticleComments).toHaveBeenLastCalledWith(1, {
      pageNum: 1,
      pageSize: 10,
      sort: 'oldest'
    })
    expect(wrapper.findAll('.comment-sort button')[1].classes()).toContain('active')
  })

  it('回复楼层内的回复时，提交的 parentId 指向被回复的评论', async () => {
    getArticleComments.mockResolvedValue({
      rows: [
        commentRow({
          replies: [{ id: 11, parentId: 1, nickname: '回复者', content: '一层回复' }]
        })
      ],
      total: 1
    })

    const wrapper = await mountDetail()
    await flushPromises()

    const replyAction = wrapper
      .findAll('.reply-item .c-actions span')
      .find(el => el.text().includes('回复'))
    expect(replyAction).toBeTruthy()
    await replyAction!.trigger('click')

    wrapper.vm.commentForm.nickname = '访客甲'
    wrapper.vm.commentForm.content = '这是一条回复'
    await wrapper.vm.submitComment()
    await flushPromises()

    expect(addBlogComment).toHaveBeenCalledWith({
      articleId: 1,
      nickname: '访客甲',
      content: '这是一条回复',
      parentId: 11
    })
    // 默认开启审核：提示语不能承诺立即可见
    expect(messageSuccess).toHaveBeenCalledWith('回复已提交，审核通过后展示')
  })

  it('发表评论后使用服务端返回的评论数刷新计数，并清空输入', async () => {
    getArticleComments.mockResolvedValue({ rows: [commentRow()], total: 1 })
    addBlogComment.mockResolvedValue({ code: 200, data: { commentCount: 2 } })

    const wrapper = await mountDetail()
    await flushPromises()

    wrapper.vm.commentForm.nickname = '访客甲'
    wrapper.vm.commentForm.content = '新评论'
    await wrapper.vm.submitComment()
    await flushPromises()

    expect(addBlogComment).toHaveBeenCalledWith({
      articleId: 1,
      nickname: '访客甲',
      content: '新评论'
    })
    expect(messageSuccess).toHaveBeenCalledWith('评论已提交，审核通过后展示')
    expect(wrapper.find('.comment-head h3').text()).toContain('评论 (2)')
    expect(wrapper.vm.commentForm.content).toBe('')
  })

  it('删除导致当前页越界时自动回退到最后一页', async () => {
    const pageRows = (n: number, count: number) =>
      Array.from({ length: count }, (_, i) =>
        commentRow({ id: n * 100 + i, nickname: `用户${i}`, content: `评论${n}-${i}` })
      )
    getArticleComments
      .mockResolvedValueOnce({ rows: pageRows(1, 10), total: 25 })
      .mockResolvedValueOnce({ rows: [], total: 10 })
      .mockResolvedValueOnce({ rows: pageRows(1, 10), total: 10 })

    const wrapper = await mountDetail()
    await flushPromises()

    await wrapper.find('.pager-page-2').trigger('click')
    await flushPromises()

    expect(getArticleComments).toHaveBeenCalledTimes(3)
    expect(getArticleComments).toHaveBeenNthCalledWith(3, 1, {
      pageNum: 1,
      pageSize: 10,
      sort: 'newest'
    })
    expect(wrapper.find('.pagination-container').exists()).toBe(false)
  })
})
