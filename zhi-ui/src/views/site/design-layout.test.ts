import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const homeView = readFileSync(resolve(__dirname, 'index.vue'), 'utf8')
const articleDetailView = readFileSync(resolve(__dirname, 'article/detail.vue'), 'utf8')
const siteLayout = readFileSync(resolve(__dirname, '../../components/SiteLayout.vue'), 'utf8')
const forgotPasswordView = readFileSync(resolve(__dirname, 'auth/ForgotPassword.vue'), 'utf8')
const registerView = readFileSync(resolve(__dirname, 'auth/Register.vue'), 'utf8')
const categoryView = readFileSync(resolve(__dirname, 'category/index.vue'), 'utf8')
const tagView = readFileSync(resolve(__dirname, 'tag/index.vue'), 'utf8')
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

const publicSiteSurfaces = [
  homeView,
  articleDetailView,
  siteLayout,
  forgotPasswordView,
  registerView,
  categoryView,
  tagView,
  aboutView
]

describe('blog design layout guards', () => {
  it('首页搜索框应在 hero 区域居中', () => {
    expect(homeView).toContain('text-align: center;')
    // 上外边距走 --mo-sp-6（=24px），左右 auto 保持水平居中
    expect(homeView).toContain('margin: var(--mo-sp-6) auto 0;')
  })

  it('首页侧边栏已移除，文章卡片标签仍可跳转对应列表页', () => {
    // 侧边栏（含分类导航/热门标签/最新文章）已随 sidebar_enabled 一并移除
    expect(homeView).not.toMatch(/class="sidebar"/)
    expect(homeView).not.toContain('cat-item active')
    // 文章卡片内的标签仍走 /tag/:id
    expect(homeView).toContain(':to="`/tag/${tag.id}`"')
  })

  it('文章详情页不应被旧卡片布局类压缩', () => {
    expect(articleDetailView).toContain('class="mo-article-page news-article"')
    expect(articleDetailView).not.toContain('class="article-detail mo-article-page"')
    expect(articleDetailView).not.toContain('padding-top: 64px;')
  })

  it('文章详情页应支持主题深色变量并允许内容列收缩', () => {
    expect(articleDetailView).toContain('html.dark .mo-article-page')
    // 内容列允许收缩（表格/代码块在窄屏内横向滚动，不撑破布局）
    expect(articleDetailView).toContain('.mo-article-page .content-body {\n  min-width: 0;')
    expect(articleDetailView).toContain(
      '.mo-article-page .content-body :deep(table) {\n  display: block;'
    )
  })

  it('博客核心页面不应混入旧版高饱和配色', () => {
    const legacyBlue = /#4a7bff|#6b8cff/i

    publicSiteSurfaces.forEach(surface => {
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
    publicSiteSurfaces.forEach(surface => {
      expect(surface).not.toMatch(/console\.(warn|error|log)/)
    })
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

  it('找回密码页复用企业认证页布局（与登录/注册同一套 ent-auth 样式）', () => {
    expect(forgotPasswordView).toContain('class="ent-auth-page tpl-site"')
    expect(forgotPasswordView).toContain('<SiteLayout>')
    expect(forgotPasswordView).not.toContain('<SiteNav')
    expect(forgotPasswordView).not.toContain("from '@/components/SiteNav.vue'")
    expect(forgotPasswordView).toContain("from '@/components/SiteLayout.vue'")
    expect(forgotPasswordView).toContain('ent-auth-card')
    expect(forgotPasswordView).toContain('ent-auth-steps')
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

  it('博客用户菜单进入管理后台应直接访问后台路由', () => {
    expect(siteLayout).toContain("router.push('/admin/article')")
    expect(siteLayout).not.toContain("window.location.href = '/login?redirect=/admin'")
    // 企业导航同款入口：/admin 本身不是页面路由，直达后台列表
    expect(enterpriseNav).toContain("router.push('/admin/article')")
  })

  it('企业模板页应挂载 SiteLayout 复用导航与页脚', () => {
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
      expect(source, name).toContain('<SiteLayout>')
      expect(source, name).toContain("import SiteLayout from '@/components/SiteLayout.vue'")
    })
  })

  it('详情页应输出面包屑（方案文档 §3.4 头部顺序 + §6.2 BreadcrumbList）', () => {
    // 跨文件契约：BreadcrumbList 注入行为由 SiteBreadcrumb.test.ts 覆盖，
    // 这里只锁定「详情页确实接入了该组件」这一页面级集成点
    expect(articleDetailView).toContain('<SiteBreadcrumb')
    expect(articleDetailView).toContain(
      "import SiteBreadcrumb from '@/components/SiteBreadcrumb.vue'"
    )
    expect(businessDetailView).toContain('<SiteBreadcrumb')
    // 头部顺序：面包屑 → 分类标签 → 标题（§3.4），面包屑必须先于 cat-badge
    const headerStart = articleDetailView.indexOf('class="na-head"')
    const crumbAt = articleDetailView.indexOf('<SiteBreadcrumb', headerStart)
    const badgeAt = articleDetailView.indexOf('class="cat-badge"', headerStart)
    expect(headerStart).toBeGreaterThan(-1)
    expect(crumbAt).toBeGreaterThan(headerStart)
    expect(badgeAt).toBeGreaterThan(crumbAt)
  })

  it('站点级结构化数据应含 WebSite + SearchAction（方案文档 §6.2 首页）', () => {
    // 跨文件契约：SiteLayout 挂载需整套 store/路由/接口桩，行为测试成本高于收益
    expect(siteLayout).toContain("setJsonLd('zh-jsonld-organization'")
    expect(siteLayout).toContain("setJsonLd('zh-jsonld-website'")
    expect(siteLayout).toContain('/search?q={search_term_string}')
  })

  it('列表型页面头部和封面标记应保持低饱和', () => {
    const heavyHeaderGradient = 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)'
    const heavyBadgeGradient =
      'linear-gradient(135deg, rgba(79, 70, 229, 0.95), rgba(67, 56, 202, 0.95))'
    const heavyOverlayGradient =
      'linear-gradient(135deg, rgba(79, 70, 229, 0.3) 0%, rgba(67, 56, 202, 0.3) 100%)'

    expect(categoryView).not.toContain(heavyHeaderGradient)
    expect(tagView).not.toContain(heavyHeaderGradient)
    expect(categoryView).not.toContain(heavyBadgeGradient)
    expect(tagView).not.toContain(heavyBadgeGradient)
    expect(categoryView).toContain('background: var(--mo-n50);')
    expect(tagView).toContain('background: var(--mo-n50);')
  })

  it('公共博客页面应减少装饰性动效和过大圆角', () => {
    expect(categoryView).not.toMatch(/border-radius:\s*(1[2-9]|[2-9][0-9])px/)
    expect(tagView).not.toMatch(/border-radius:\s*(1[2-9]|[2-9][0-9])px/)
  })

  it('博客布局不应在只有旧 token 且用户信息为空时渲染用户下拉菜单', () => {
    expect(siteLayout).toContain('v-if="userStore.token && userStore.name"')
  })

  it('首页和公共布局应复用全局 mo 主题 token', () => {
    // 检查组件是否使用了全局变量
    expect(homeView).toContain('var(--mo-p600)')
    expect(homeView).toContain('var(--mo-n50)')
    expect(siteLayout).toContain('var(--mo-p600)')
    expect(siteLayout).toContain('var(--mo-n50)')
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

  it('认证页统一走企业官网 --tpl-* 变量层（共享样式表承载配色）', () => {
    const authScss = readFileSync(
      resolve(__dirname, '../../assets/styles/auth-enterprise.scss'),
      'utf-8'
    )
    // 三个认证页（登录/注册/找回密码）都用同一套 ent-auth 类，配色集中在共享表
    expect(registerView).toContain('class="ent-auth-page tpl-site"')
    expect(forgotPasswordView).toContain('class="ent-auth-page tpl-site"')
    expect(registerView).toContain('ent-auth-card')
    expect(forgotPasswordView).toContain('ent-auth-card')

    // 不再硬编码靛蓝/棕色字面量，一律走 --tpl-* 令牌
    const themeHardcodes = /#4f46e5|#3730a3|#4338ca|#1c1917|#292524|#fafaf9|#eef2ff|#e7e5e4/i
    expect(registerView).not.toMatch(themeHardcodes)
    expect(forgotPasswordView).not.toMatch(themeHardcodes)
    expect(authScss).not.toMatch(themeHardcodes)
    expect(authScss).toContain('var(--tpl-ink-900')
    expect(authScss).toContain('var(--tpl-p')
    // 深色由 .tpl-site 变量重映射 + 共享表的 html.dark 覆盖负责
    expect(authScss).toContain('html.dark .ent-auth-page')
  })
})
