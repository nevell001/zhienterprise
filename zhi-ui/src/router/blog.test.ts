import { describe, it, expect } from 'vitest'
import { blogRoutes } from './blog'

describe('Blog Routes 详细测试', () => {
  it('文章列表路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/news')
    expect(route?.path).toBe('/news')
    expect(route?.name).toBe('PublicBlogArticleList')
    expect(route?.meta?.title).toBe('文章列表')
  })

  it('文章详情路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/article/:id')
    expect(route?.path).toBe('/article/:id')
    expect(route?.name).toBe('PublicBlogArticleDetail')
    expect(route?.meta?.title).toBe('文章详情')
  })

  it('核心业务路由应该有正确的路径、组件与标题（企业官网模板入口）', () => {
    const list = blogRoutes.find(r => r.path === '/business')
    expect(list?.name).toBe('PublicBusinessList')
    expect(list?.meta?.title).toBe('核心业务')
    const detail = blogRoutes.find(r => r.path === '/business/:id')
    expect(detail?.name).toBe('PublicBusinessDetail')
    expect(detail?.meta?.title).toBe('业务详情')
  })

  it('联系我们路由应该有正确的路径、组件与标题（企业官网模板入口）', () => {
    const route = blogRoutes.find(r => r.path === '/contact')
    expect(route?.name).toBe('PublicContact')
    expect(route?.meta?.title).toBe('联系我们')
  })

  it('分类列表路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/category')
    expect(route?.path).toBe('/category')
    expect(route?.name).toBe('PublicBlogCategoryList')
    expect(route?.meta?.title).toBe('分类列表')
  })

  it('分类详情路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/category/:id')
    expect(route?.path).toBe('/category/:id')
    expect(route?.name).toBe('PublicBlogCategory')
    expect(route?.meta?.title).toBe('分类文章')
  })

  it('标签列表路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/tag')
    expect(route?.path).toBe('/tag')
    expect(route?.name).toBe('PublicBlogTagList')
    expect(route?.meta?.title).toBe('标签列表')
  })

  it('标签详情路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/tag/:id')
    expect(route?.path).toBe('/tag/:id')
    expect(route?.name).toBe('PublicBlogTag')
    expect(route?.meta?.title).toBe('标签文章')
  })

  it('作者文章路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/author/:id')
    expect(route?.path).toBe('/author/:id')
    expect(route?.name).toBe('PublicBlogAuthor')
    expect(route?.meta?.title).toBe('作者文章')
  })

  it('友链申请路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/friend-links/apply')
    expect(route?.path).toBe('/friend-links/apply')
    expect(route?.name).toBe('PublicBlogFriendLinkApply')
    expect(route?.meta?.title).toBe('友链申请')
  })

  it('归档路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/archive')
    expect(route?.path).toBe('/archive')
    expect(route?.name).toBe('PublicBlogArchive')
    expect(route?.meta?.title).toBe('文章归档')
  })

  it('关于页面路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/about')
    expect(route?.path).toBe('/about')
    expect(route?.name).toBe('PublicAbout')
    expect(route?.meta?.title).toBe('关于我们')
  })

  it('荣誉资质/发展历程路由应该有正确的路径与标题（关于我们子页）', () => {
    const honors = blogRoutes.find(r => r.path === '/honors')
    expect(honors?.name).toBe('PublicHonors')
    expect(honors?.meta?.title).toBe('荣誉资质')
    const history = blogRoutes.find(r => r.path === '/history')
    expect(history?.name).toBe('PublicHistory')
    expect(history?.meta?.title).toBe('发展历程')
  })

  it('留言板路由应该有正确的路径、组件与标题', () => {
    const route = blogRoutes.find(r => r.path === '/guestbook')
    expect(route?.path).toBe('/guestbook')
    expect(route?.name).toBe('PublicBlogGuestbook')
    expect(route?.meta?.title).toBe('留言板')
  })

  it('自定义页面路由应该有正确的路径、组件与标题', () => {
    const route = blogRoutes.find(r => r.path === '/page/:slug')
    expect(route?.path).toBe('/page/:slug')
    expect(route?.name).toBe('PublicBlogPage')
    expect(route?.meta?.title).toBe('页面')
  })

  it('用户协议路由应该有正确的路径与标题', () => {
    const route = blogRoutes.find(r => r.path === '/agreement')
    expect(route?.name).toBe('PublicBlogUserAgreement')
    expect(route?.meta?.title).toBe('用户协议')
  })

  it('隐私政策路由应该有正确的路径与标题', () => {
    const route = blogRoutes.find(r => r.path === '/privacy')
    expect(route?.name).toBe('PublicBlogPrivacyPolicy')
    expect(route?.meta?.title).toBe('隐私政策')
  })

  it('用户注册路由应该有正确的路径和标题', () => {
    const route = blogRoutes.find(r => r.path === '/register')
    expect(route?.path).toBe('/register')
    expect(route?.name).toBe('BlogRegister')
    expect(route?.meta?.title).toBe('用户注册')
  })

  it('找回密码路由应该有正确的路径和标题', () => {
    const route = blogRoutes.find(r => r.path === '/forgot-password')
    expect(route?.path).toBe('/forgot-password')
    expect(route?.name).toBe('BlogForgotPassword')
    expect(route?.meta?.title).toBe('找回密码')
  })

  it('搜索路由应该有正确的路径与标题', () => {
    const route = blogRoutes.find(r => r.path === '/search')
    expect(route?.name).toBe('PublicBlogSearch')
    expect(route?.meta?.title).toBe('搜索文章')
  })

  it('收藏路由应该有正确的路径与标题', () => {
    const route = blogRoutes.find(r => r.path === '/bookmarks')
    expect(route?.name).toBe('PublicBlogBookmarks')
    expect(route?.meta?.title).toBe('我的收藏')
  })

  it('路由总数应该为 23 个', () => {
    expect(blogRoutes.length).toBe(23)
  })

  it('不再存在任何 /blog 前缀的页面路由（短路径为唯一规范）', () => {
    const legacyRoutes = blogRoutes.filter(r => r.path.startsWith('/blog'))

    expect(legacyRoutes).toEqual([])
  })

  it('不应该有别名或 redirect 路径（避免 /blog 旧路径残留）', () => {
    const routesWithAliasOrRedirect = blogRoutes.filter(r => r.alias || 'redirect' in r)

    expect(routesWithAliasOrRedirect).toEqual([])
  })

  it('所有路由都应该有 name 属性', () => {
    const routesWithoutName = blogRoutes.filter(r => !r.name)

    expect(routesWithoutName.length).toBe(0)
  })

  it('所有路由都应该有 meta.title 属性', () => {
    const routesWithoutTitle = blogRoutes.filter(r => !r.meta?.title)

    expect(routesWithoutTitle.length).toBe(0)
  })

  it('所有路由都应该定义组件', () => {
    const routesWithoutComponent = blogRoutes.filter(r => !r.component)

    expect(routesWithoutComponent.length).toBe(0)
  })

  it('路由 name 不应该重复', () => {
    const names = blogRoutes.map(r => r.name)
    const uniqueNames = new Set(names)

    expect(uniqueNames.size).toBe(names.length)
  })
})
