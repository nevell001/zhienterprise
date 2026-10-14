import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const homeView = readFileSync(resolve(__dirname, 'index.vue'), 'utf8')
const articleDetailView = readFileSync(resolve(__dirname, 'article/detail.vue'), 'utf8')
const blogLayout = readFileSync(resolve(__dirname, '../../components/BlogLayout.vue'), 'utf8')
const articleTOC = readFileSync(resolve(__dirname, '../../components/ArticleTOC.vue'), 'utf8')
const forgotPasswordView = readFileSync(resolve(__dirname, 'auth/ForgotPassword.vue'), 'utf8')
const registerView = readFileSync(resolve(__dirname, 'auth/Register.vue'), 'utf8')
const categoryView = readFileSync(resolve(__dirname, 'category/index.vue'), 'utf8')
const tagView = readFileSync(resolve(__dirname, 'tag/index.vue'), 'utf8')
const archiveView = readFileSync(resolve(__dirname, 'archive/index.vue'), 'utf8')
const aboutView = readFileSync(resolve(__dirname, 'about.vue'), 'utf8')
const honorsView = readFileSync(resolve(__dirname, 'honors.vue'), 'utf8')
const historyView = readFileSync(resolve(__dirname, 'history.vue'), 'utf8')
const contactView = readFileSync(resolve(__dirname, 'contact/index.vue'), 'utf8')
const businessListView = readFileSync(resolve(__dirname, 'business/index.vue'), 'utf8')
const businessDetailView = readFileSync(resolve(__dirname, 'business/detail.vue'), 'utf8')
const enterpriseNav = readFileSync(
  resolve(__dirname, '../../components/enterprise/EnterpriseNav.vue'),
  'utf8'
)

const publicBlogSurfaces = [
  homeView,
  articleDetailView,
  blogLayout,
  articleTOC,
  forgotPasswordView,
  registerView,
  categoryView,
  tagView,
  archiveView,
  aboutView
]

describe('blog design layout guards', () => {
  it('首页搜索框应在 hero 区域居中', () => {
    expect(homeView).toContain('text-align: center;')
    // 上外边距走 --mo-sp-6（=24px），左右 auto 保持水平居中
    expect(homeView).toContain('margin: var(--mo-sp-6) auto 0;')
  })

  it('首页分类导航和热门标签应能跳转到对应列表页', () => {
    expect(homeView).toContain('class="cat-item active"')
    // 「全部文章」的目标取决于首页是否被自定义页面接管（/ 或 /news）
    expect(homeView).toContain(':to="listHomePath"')
    // 分类链接 alias 伪静态优先，回退数字 ID
    expect(homeView).toContain(':to="`/category/${category.alias || category.id}`"')
    expect(homeView).toContain(':to="`/tag/${tag.id}`"')
  })

  it('文章详情页不应被旧卡片布局类压缩', () => {
    expect(articleDetailView).toContain('class="mo-article-page"')
    expect(articleDetailView).not.toContain('class="article-detail mo-article-page"')
    expect(articleDetailView).not.toContain('padding-top: 64px;')
  })

  it('文章详情页应支持主题深色变量并允许内容列收缩', () => {
    expect(articleDetailView).toContain('html.dark .mo-article-page')
    expect(articleDetailView).toContain('.mo-article-page .article-content {\n  min-width: 0;')
    expect(articleDetailView).toContain('.mo-article-page .article-side {\n  min-width: 0;')
    expect(articleDetailView).toContain(
      '.mo-article-page .content-body :deep(table) {\n  display: block;'
    )
  })

  it('博客核心页面不应混入旧版高饱和配色', () => {
    const legacyBlue = /#4a7bff|#6b8cff/i

    publicBlogSurfaces.forEach(surface => {
      expect(surface).not.toMatch(legacyBlue)
    })
    expect(homeView).not.toContain('tag-pink')
    expect(homeView).not.toContain('tag-green')
    expect(homeView).not.toContain('tag-amber')
    expect(homeView).not.toContain('.hero-section')
    expect(articleDetailView).not.toContain('.article-detail {')
    expect(articleDetailView).not.toContain('html.dark .article-detail {')
  })

  it('公共博客页面不应直接输出生产调试日志', () => {
    publicBlogSurfaces.forEach(surface => {
      expect(surface).not.toMatch(/console\.(warn|error|log)/)
    })
  })

  it('目录组件应使用当前主题色和可清理的滚动监听', () => {
    expect(articleTOC).not.toContain('@media (prefers-color-scheme: dark)')
    expect(articleTOC).toContain('html.dark .article-toc')
    expect(articleTOC).toContain('const handleScroll')
    expect(articleTOC).not.toContain("window.addEventListener('scroll', () =>")
  })

  it('分类和标签页主布局不应横向溢出', () => {
    expect(categoryView).toContain('min-width: 0;')
    expect(tagView).toContain('min-width: 0;')
    expect(categoryView).toContain('grid-template-columns: minmax(0, 1fr) 300px;')
    expect(tagView).toContain('grid-template-columns: minmax(0, 1fr) 300px;')
    expect(categoryView).not.toContain('minmax(380px, 1fr)')
    expect(tagView).not.toContain('minmax(380px, 1fr)')
    expect(categoryView).toContain('minmax(min(100%, 380px), 1fr)')
    expect(tagView).toContain('minmax(min(100%, 380px), 1fr)')
  })

  it('找回密码页应复用当前博客认证主题布局', () => {
    expect(forgotPasswordView).toContain('class="blog-forgot-password-container mo-auth-page"')
    expect(forgotPasswordView).toContain('<BlogLayout>')
    expect(forgotPasswordView).not.toContain('<BlogNav')
    expect(forgotPasswordView).not.toContain("from '@/components/BlogNav.vue'")
    expect(forgotPasswordView).toContain("from '@/components/BlogLayout.vue'")
    expect(forgotPasswordView).toContain('html.dark .mo-auth-page')
    expect(forgotPasswordView).toContain("router.push('/login')")
  })

  it('关于页应为模板化叙事页（tpl 体系 + 企业简介/荣誉/历程结构）', () => {
    expect(aboutView).toContain('class="tpl-site about-page mo-home-page"')
    expect(aboutView).toContain('class="aph"')
    expect(aboutView).toContain('to="/honors"')
    expect(aboutView).toContain('to="/history"')
    // 样式走 --tpl-* 变量层，不再引用 mo 主题组件皮肤 token
    expect(aboutView).toContain('sanitizeArticleContent(renderMarkdown(raw))')
    expect(aboutView).not.toContain('email-icon')
    expect(aboutView).not.toContain('github-icon')
    expect(aboutView).not.toContain('weibo-icon')
    expect(aboutView).not.toContain('hero-section')
    expect(aboutView).not.toContain('hero-wave-bg')
    expect(aboutView).not.toContain('hero-bottom-wave')
    expect(aboutView).not.toContain('linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)')
  })

  it('归档页不应保留旧浅紫和深紫独立配色', () => {
    const archiveLegacyColors = /#f8f9ff|#f5f5ff|#e8e9ff|#e5e5ff|#2e2e45|#3a3a55/i

    expect(archiveView).not.toMatch(archiveLegacyColors)
    expect(archiveView).toContain('background: var(--mo-n50);')
    expect(archiveView).toContain('html.dark .archive-page')
  })

  it('博客用户菜单进入管理后台应直接访问后台路由', () => {
    expect(blogLayout).toContain("router.push('/admin/article')")
    expect(blogLayout).not.toContain("window.location.href = '/login?redirect=/admin'")
    // 企业导航同款入口：/admin 本身不是页面路由，直达后台列表
    expect(enterpriseNav).toContain("router.push('/admin/article')")
  })

  it('企业模板页应挂载 BlogLayout 复用导航与页脚', () => {
    // 跨文件结构契约：6 个页面各自在 mounted 里请求不同接口，
    // 逐一挂载需要为每页搭全套 API/路由桩，而这里断言的是
    // 「与 /news 同一布局链路」这一结构性约束，行为测试覆盖不到
    const templatePages: Array<[string, string]> = [
      ['about', aboutView],
      ['honors', honorsView],
      ['history', historyView],
      ['contact', contactView],
      ['business/list', businessListView],
      ['business/detail', businessDetailView]
    ]
    templatePages.forEach(([name, source]) => {
      expect(source, name).toContain('<BlogLayout>')
      expect(source, name).toContain("import BlogLayout from '@/components/BlogLayout.vue'")
    })
  })

  it('列表型页面头部和封面标记应保持低饱和', () => {
    const heavyHeaderGradient = 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)'
    const heavyBadgeGradient =
      'linear-gradient(135deg, rgba(79, 70, 229, 0.95), rgba(67, 56, 202, 0.95))'
    const heavyOverlayGradient =
      'linear-gradient(135deg, rgba(79, 70, 229, 0.3) 0%, rgba(67, 56, 202, 0.3) 100%)'

    expect(categoryView).not.toContain(heavyHeaderGradient)
    expect(tagView).not.toContain(heavyHeaderGradient)
    expect(archiveView).not.toContain(heavyHeaderGradient)
    expect(categoryView).not.toContain(heavyBadgeGradient)
    expect(tagView).not.toContain(heavyBadgeGradient)
    expect(archiveView).not.toContain(heavyOverlayGradient)
    expect(categoryView).toContain('background: var(--mo-n50);')
    expect(tagView).toContain('background: var(--mo-n50);')
    expect(archiveView).toContain('background: var(--mo-n50);')
  })

  it('公共博客页面应减少装饰性动效和过大圆角', () => {
    expect(archiveView).not.toContain('animation: bounce 2s ease-in-out infinite')
    expect(archiveView).not.toContain('animation: pulse 2s ease-in-out infinite')
    expect(archiveView).not.toMatch(/border-radius:\s*(1[2-9]|[2-9][0-9])px/)
    expect(categoryView).not.toMatch(/border-radius:\s*(1[2-9]|[2-9][0-9])px/)
    expect(tagView).not.toMatch(/border-radius:\s*(1[2-9]|[2-9][0-9])px/)
  })

  it('博客布局不应在只有旧 token 且用户信息为空时渲染用户下拉菜单', () => {
    expect(blogLayout).toContain('v-if="userStore.token && userStore.name"')
  })

  it('首页和公共布局应复用全局 mo 主题 token', () => {
    // 检查组件是否使用了全局变量
    expect(homeView).toContain('var(--mo-p600)')
    expect(homeView).toContain('var(--mo-n50)')
    expect(blogLayout).toContain('var(--mo-p600)')
    expect(blogLayout).toContain('var(--mo-n50)')
  })

  it('文章详情页应复用全局 mo 主题 token', () => {
    expect(articleDetailView).not.toMatch(/--[pn][0-9]+:/)
    expect(articleDetailView).not.toContain('var(--p')
    expect(articleDetailView).not.toContain('var(--n')
    expect(articleDetailView).toContain('var(--mo-p600)')
    expect(articleDetailView).toContain('var(--mo-n50)')
  })

  it('分类和标签页应复用全局 mo 主题 token', () => {
    const themeHardcodes = /#4f46e5|#3730a3|#4338ca|#1c1917|#292524|#fafaf9|#eef2ff|#e7e5e4/i

    expect(categoryView).not.toMatch(themeHardcodes)
    expect(tagView).not.toMatch(themeHardcodes)
    expect(categoryView).toContain('var(--mo-p600)')
    expect(categoryView).toContain('var(--mo-n50)')
    expect(tagView).toContain('var(--mo-p600)')
    expect(tagView).toContain('var(--mo-n50)')
  })

  it('归档页应复用全局 mo 主题 token', () => {
    const themeHardcodes = /#4f46e5|#3730a3|#4338ca|#1c1917|#292524|#fafaf9|#eef2ff|#e7e5e4/i

    expect(archiveView).not.toMatch(themeHardcodes)
    expect(archiveView).toContain('var(--mo-p600)')
    expect(archiveView).toContain('var(--mo-n50)')
  })

  it('博客认证页应复用全局 mo 主题 token', () => {
    const themeHardcodes = /#4f46e5|#3730a3|#4338ca|#1c1917|#292524|#fafaf9|#eef2ff|#e7e5e4/i

    expect(registerView).not.toMatch(themeHardcodes)
    expect(forgotPasswordView).not.toMatch(themeHardcodes)
    expect(registerView).toContain('var(--mo-p600)')
    expect(forgotPasswordView).toContain('var(--mo-p600)')
  })
})
