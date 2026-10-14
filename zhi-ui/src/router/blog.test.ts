import { describe, it, expect } from 'vitest'
import { blogRoutes } from './blog'

describe('Blog Routes 详细测试', () => {
  it('首页路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/blog')
    expect(route?.path).toBe('/blog')
    expect(route?.name).toBe('PublicBlogHome')
    expect(route?.meta?.title).toBe('站点首页')
  })

  it('文章详情路由应该有正确的路径、组件和别名', () => {
    const route = blogRoutes.find(r => r.path === '/blog/article/:id')
    expect(route?.path).toBe('/blog/article/:id')
    expect(route?.name).toBe('PublicBlogArticleDetail')
    expect(route?.alias).toEqual(['/article/:id'])
  })

  it('分类列表路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/blog/category')
    expect(route?.path).toBe('/blog/category')
    expect(route?.name).toBe('PublicBlogCategoryList')
    expect(route?.meta?.title).toBe('分类列表')
  })

  it('分类详情路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/blog/category/:id')
    expect(route?.path).toBe('/blog/category/:id')
    expect(route?.name).toBe('PublicBlogCategory')
    expect(route?.meta?.title).toBe('分类文章')
  })

  it('标签列表路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/blog/tag')
    expect(route?.path).toBe('/blog/tag')
    expect(route?.name).toBe('PublicBlogTagList')
    expect(route?.meta?.title).toBe('标签列表')
  })

  it('标签详情路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/blog/tag/:id')
    expect(route?.path).toBe('/blog/tag/:id')
    expect(route?.name).toBe('PublicBlogTag')
    expect(route?.meta?.title).toBe('标签文章')
  })

  it('归档路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/blog/archive')
    expect(route?.path).toBe('/blog/archive')
    expect(route?.name).toBe('PublicBlogArchive')
    expect(route?.meta?.title).toBe('文章归档')
  })

  it('关于页面路由应该有正确的路径和组件', () => {
    const route = blogRoutes.find(r => r.path === '/blog/about')
    expect(route?.path).toBe('/blog/about')
    expect(route?.name).toBe('PublicAbout')
    expect(route?.meta?.title).toBe('关于我们')
  })

  it('用户登录路由应该重定向到后台登录', () => {
    const route = blogRoutes.find(r => r.path === '/blog/auth/login')
    expect(route?.redirect).toBe('/login')
    expect(route?.meta?.title).toBe('用户登录')
  })

  it('用户注册路由应该有正确的路径和标题', () => {
    const route = blogRoutes.find(r => r.path === '/blog/auth/register')
    expect(route?.path).toBe('/blog/auth/register')
    expect(route?.name).toBe('BlogRegister')
    expect(route?.meta?.title).toBe('用户注册')
  })

  it('找回密码路由应该有正确的路径和标题', () => {
    const route = blogRoutes.find(r => r.path === '/blog/auth/forgot-password')
    expect(route?.path).toBe('/blog/auth/forgot-password')
    expect(route?.name).toBe('BlogForgotPassword')
    expect(route?.meta?.title).toBe('找回密码')
  })

  it('首页路由应该有 index 组件', () => {
    const route = blogRoutes.find(r => r.path === '/index')
    expect(route?.name).toBe('PublicIndex')
  })

  it('应该有 /about 路由', () => {
    const route = blogRoutes.find(r => r.path === '/about')
    expect(route?.name).toBe('PublicAboutPage')
    expect(route?.meta?.title).toBe('关于')
  })

  it('文章列表路由应该有正确的路径和标题（企业官网模式下的文章入口）', () => {
    const route = blogRoutes.find(r => r.path === '/blog/articles')
    expect(route?.name).toBe('PublicBlogArticleList')
    expect(route?.meta?.title).toBe('文章列表')
  })

  it('核心业务路由应该有正确的路径、组件与标题（企业官网模板入口）', () => {
    const list = blogRoutes.find(r => r.path === '/blog/business')
    expect(list?.name).toBe('PublicBusinessList')
    expect(list?.meta?.title).toBe('核心业务')
    const detail = blogRoutes.find(r => r.path === '/blog/business/:id')
    expect(detail?.name).toBe('PublicBusinessDetail')
    expect(detail?.meta?.title).toBe('业务详情')
  })

  it('荣誉资质/发展历程路由应该有正确的路径与标题（关于我们子页）', () => {
    const honors = blogRoutes.find(r => r.path === '/blog/honors')
    expect(honors?.name).toBe('PublicHonors')
    expect(honors?.meta?.title).toBe('荣誉资质')
    const history = blogRoutes.find(r => r.path === '/blog/history')
    expect(history?.name).toBe('PublicHistory')
    expect(history?.meta?.title).toBe('发展历程')
  })

  it('联系我们路由应该有正确的路径、组件与标题（企业官网模板入口）', () => {
    const route = blogRoutes.find(r => r.path === '/blog/contact')
    expect(route?.name).toBe('PublicContact')
    expect(route?.meta?.title).toBe('联系我们')
  })

  it('路由总数应该为 27 个', () => {
    expect(blogRoutes.length).toBe(27)
  })

  it('留言板路由应该有正确的路径、组件与标题', () => {
    const route = blogRoutes.find(r => r.path === '/blog/guestbook')
    expect(route?.path).toBe('/blog/guestbook')
    expect(route?.name).toBe('PublicBlogGuestbook')
    expect(route?.meta?.title).toBe('留言板')
  })

  it('自定义页面路由应该有正确的路径、组件与标题', () => {
    const route = blogRoutes.find(r => r.path === '/blog/page/:slug')
    expect(route?.path).toBe('/blog/page/:slug')
    expect(route?.name).toBe('PublicBlogPage')
    expect(route?.meta?.title).toBe('页面')
  })

  it('博客路由应该包含前台路由', () => {
    const blogFrontendRoutes = blogRoutes.filter(
      r => r.path.startsWith('/blog') && !r.path.includes('/auth')
    )

    expect(blogFrontendRoutes.length).toBeGreaterThan(0)
  })

  it('博客路由应该包含认证路由', () => {
    const blogAuthRoutes = blogRoutes.filter(r => r.path.includes('/auth'))

    expect(blogAuthRoutes.length).toBeGreaterThan(0)
  })

  it('所有有 component 的路由都应该定义组件', () => {
    const routesWithoutComponent = blogRoutes.filter(r => r.component && !r.redirect)

    expect(routesWithoutComponent.length).toBeGreaterThan(0)
  })

  it('所有路由都应该有 name 属性（除了重定向路由）', () => {
    const routesWithoutRedirect = blogRoutes.filter(r => !r.redirect)
    const routesWithoutName = routesWithoutRedirect.filter(r => !r.name)

    expect(routesWithoutName.length).toBe(0)
  })

  it('所有路由都应该有 meta.title 属性（除了重定向路由）', () => {
    const routesWithTitle = blogRoutes.filter(r => r.meta?.title)

    expect(routesWithTitle.length).toBeGreaterThan(0)
  })
})
