import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ArticleDetail from './detail.vue'
import { applySeo } from '@/utils/seo'
import { setJsonLd } from '@/utils/jsonld'

/**
 * 文章详情页评论行为测试（挂载真实组件）。
 * 覆盖：楼层嵌套渲染、顶级评论分页、排序切换、回复 parentId、评论数同步、越界页回退。
 */
const getArticleDetail = vi.fn()
const getArticlePreview = vi.fn()
const getRelatedArticles = vi.fn()
const getArticleComments = vi.fn()
const addSiteComment = vi.fn()
const updateMyComment = vi.fn()
const deleteMyComment = vi.fn()
const getArticleLikeStatus = vi.fn()
const getCommentLikedStatuses = vi.fn()
const toggleCommentLike = vi.fn()
const toggleArticleLike = vi.fn()
const getSiteSettings = vi.fn()
const getSiteSettingsAnonymous = vi.fn()

vi.mock('@/api/site/article', () => ({
  getArticleDetail: (...args: unknown[]) => getArticleDetail(...args),
  getArticlePreview: (...args: unknown[]) => getArticlePreview(...args),
  getRelatedArticles: (...args: unknown[]) => getRelatedArticles(...args)
}))

vi.mock('@/api/site/like', () => ({
  getArticleLikeStatus: (...args: unknown[]) => getArticleLikeStatus(...args),
  toggleArticleLike: (...args: unknown[]) => toggleArticleLike(...args),
  toggleCommentLike: (...args: unknown[]) => toggleCommentLike(...args),
  getCommentLikedStatuses: (...args: unknown[]) => getCommentLikedStatuses(...args)
}))

vi.mock('@/api/site/comment', () => ({
  getArticleComments: (...args: unknown[]) => getArticleComments(...args),
  addSiteComment: (...args: unknown[]) => addSiteComment(...args),
  updateMyComment: (...args: unknown[]) => updateMyComment(...args),
  deleteMyComment: (...args: unknown[]) => deleteMyComment(...args)
}))

vi.mock('@/api/site/setting', () => ({
  getSiteSettings: (...args: unknown[]) => getSiteSettings(...args),
  getSiteSettingsAnonymous: (...args: unknown[]) => getSiteSettingsAnonymous(...args)
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/blog')
}))

vi.mock('@/utils/jsonld', () => ({
  setJsonLd: vi.fn(),
  removeJsonLd: vi.fn()
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
  SiteLayout: { template: '<div class="layout-stub"><slot /></div>' },
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
  // SitePager 内部用它渲染页码，点击等价于用户翻到第 2 页
  'el-pagination': {
    props: ['total', 'pageSize', 'currentPage'],
    emits: ['current-change'],
    template:
      '<button type="button" class="pager-page-2" @click="$emit(\'current-change\', 2)">2</button>'
  }
}

async function mountDetail(path = '/article/1') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/article/:id', name: 'PublicSiteArticleDetail', component: ArticleDetail },
      {
        path: '/preview/article/:id',
        name: 'AdminArticlePreview',
        component: ArticleDetail,
        meta: { preview: true }
      },
      { path: '/admin/article', component: { template: '<div />' } },
      { path: '/contact', component: { template: '<div />' } }
    ]
  })
  router.push(path)
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
    getSiteSettings.mockResolvedValue({
      code: 200,
      data: { comment_enabled: 'true', comment_review: 'true' }
    })
    getSiteSettingsAnonymous.mockResolvedValue({ data: {} })
    getArticleComments.mockResolvedValue({ rows: [], total: 0 })
    addSiteComment.mockResolvedValue({ code: 200, data: { commentCount: 3 } })
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

    expect(addSiteComment).toHaveBeenCalledWith({
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
    addSiteComment.mockResolvedValue({ code: 200, data: { commentCount: 2 } })

    const wrapper = await mountDetail()
    await flushPromises()

    wrapper.vm.commentForm.nickname = '访客甲'
    wrapper.vm.commentForm.content = '新评论'
    await wrapper.vm.submitComment()
    await flushPromises()

    expect(addSiteComment).toHaveBeenCalledWith({
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

describe('文章详情页企业官网改造（行为）', () => {
  /**
   * 博客元素已被移除：目录（TOC）、作者卡（头像/关注/获赞/评论数）、收藏按钮、
   * 阅读进度条、阅读时长、标签云侧栏、侧栏迷你相关推荐。
   * 结构改为新闻详情：面包屑 → 分类/日期 → 标题 → 摘要 → 正文 → 关键词 →
   * 相关阅读卡片 → 咨询 CTA → 上/下一篇。
   */
  beforeEach(() => {
    vi.clearAllMocks()
    getArticleDetail.mockResolvedValue({
      code: 200,
      data: {
        article: {
          id: 1,
          title: '企业官网改版实践',
          content: '<h2>改版目标</h2><p>正文内容</p>',
          summary: '一篇讲官网改版的文章',
          categoryName: '行业资讯',
          tags: [
            { id: 3, name: '企业官网' },
            { id: 4, name: '品牌升级' }
          ],
          commentCount: 0,
          likeCount: 7,
          viewCount: 156,
          createTime: '2026-01-01 10:00:00'
        },
        extraInfo: {
          prevArticle: { id: 2, title: '上一篇标题' },
          nextArticle: { id: 3, title: '下一篇标题' }
        }
      }
    })
    getRelatedArticles.mockResolvedValue({
      data: [
        { id: 2, title: '相关内容一', summary: '摘要一', categoryName: '企业动态' },
        { id: 3, title: '相关内容二', summary: '摘要二', categoryName: '行业资讯' }
      ]
    })
    getSiteSettings.mockResolvedValue({
      code: 200,
      data: { comment_enabled: 'false', view_count_enabled: 'true' }
    })
    getSiteSettingsAnonymous.mockResolvedValue({ data: {} })
    getArticleComments.mockResolvedValue({ rows: [], total: 0 })
  })

  it('不再渲染博客元素（目录/作者卡/收藏/阅读进度/阅读时长/标签云）', async () => {
    const wrapper = await mountDetail()
    await flushPromises()

    expect(wrapper.find('.toc').exists()).toBe(false)
    expect(wrapper.find('.author-card').exists()).toBe(false)
    expect(wrapper.find('.read-progress').exists()).toBe(false)
    expect(wrapper.find('.tag-cloud').exists()).toBe(false)
    expect(wrapper.find('.related-mini').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('收藏')
    expect(wrapper.text()).not.toContain('关注')
    expect(wrapper.text()).not.toMatch(/约\s*\d+\s*分钟/)
  })

  it('渲染企业官网新闻结构：分类/日期/关键词/相关阅读/CTA/上下篇', async () => {
    const wrapper = await mountDetail()
    await flushPromises()

    // 头部：分类徽标 + 日期 + 标题 + 摘要
    expect(wrapper.find('.na-kicker .cat-badge').text()).toContain('行业资讯')
    expect(wrapper.find('.na-kicker').text()).toContain('2026')
    expect(wrapper.find('.na-head h1').text()).toContain('企业官网改版实践')
    expect(wrapper.find('.na-lead').text()).toContain('一篇讲官网改版的文章')

    // 关键词以胶囊形式保留（SEO 需要，不再是博客标签云侧栏）
    const tags = wrapper.findAll('.na-tags .na-tag')
    expect(tags).toHaveLength(2)
    expect(tags[0].text()).toBe('企业官网')

    // 相关阅读卡片（最多 3 条）
    expect(wrapper.findAll('.na-related-card').length).toBe(2)
    expect(wrapper.find('.na-related').text()).toContain('相关内容一')

    // 咨询 CTA 与上/下一篇
    expect(wrapper.find('.na-cta').exists()).toBe(true)
    const navItems = wrapper.findAll('.na-prevnext-item')
    expect(navItems.length).toBeGreaterThanOrEqual(2)
    expect(wrapper.find('.na-prevnext').text()).toContain('上一篇标题')
    expect(wrapper.find('.na-prevnext').text()).toContain('下一篇标题')
  })
})

describe('未发布文章站点预览', () => {
  it('预览路由调用后台预览接口，跳过评论与结构化数据，并标记 noindex', async () => {
    getArticlePreview.mockResolvedValue({
      code: 200,
      data: {
        article: {
          id: 1,
          title: '未发布草稿',
          content: '<p>草稿正文</p>',
          status: 0,
          createTime: '2026-01-01 10:00:00'
        },
        extraInfo: {}
      }
    })

    const wrapper = await mountDetail('/preview/article/1')
    await flushPromises()

    expect(getArticlePreview).toHaveBeenCalledWith('1')
    expect(getArticleDetail).not.toHaveBeenCalled()
    expect(getArticleComments).not.toHaveBeenCalled()
    expect(wrapper.find('.preview-banner').exists()).toBe(true)
    expect(wrapper.find('.comment-section').exists()).toBe(false)
    expect(applySeo).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '【预览】未发布草稿',
        robots: 'noindex,nofollow',
        canonical: undefined
      })
    )
    // 面包屑结构化数据仍由 SiteBreadcrumb 输出，但文章主体不得向搜索引擎声明
    expect(setJsonLd).not.toHaveBeenCalledWith('zh-jsonld-article', expect.anything())
  })
})
