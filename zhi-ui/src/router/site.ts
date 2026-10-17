import type { RouteRecordRaw } from 'vue-router'

// 站点前台路由配置 - 仅包含前台相关路由
// 地址规范：前台页面一律使用短路径（不带 /blog 前缀），与导航、站点地图保持一致
export const siteRoutes: RouteRecordRaw[] = [
  {
    // 企业官网模式下首页被自定义页面接管，文章列表固定在本地址（默认模式下与首页内容相同）
    path: '/news',
    component: () => import('@/views/site/index.vue'),
    name: 'PublicSiteArticleList',
    meta: { title: '文章列表' }
  },
  {
    path: '/article/:id',
    component: () => import('@/views/site/article/detail.vue'),
    name: 'PublicSiteArticleDetail',
    meta: { title: '文章详情' }
  },
  {
    // 核心业务（article_type=business 的文章）：企业官网模板的落地页
    path: '/business',
    component: () => import('@/views/site/business/index.vue'),
    name: 'PublicBusinessList',
    meta: { title: '核心业务' }
  },
  {
    path: '/business/:id',
    component: () => import('@/views/site/business/detail.vue'),
    name: 'PublicBusinessDetail',
    meta: { title: '业务详情' }
  },
  {
    // 解决方案板块（article_type=solution 的文章，行业/场景筛选）
    path: '/solutions',
    component: () => import('@/views/site/solutions/index.vue'),
    name: 'PublicSolutionList',
    meta: { title: '解决方案' }
  },
  {
    path: '/solutions/:id',
    component: () => import('@/views/site/business/detail.vue'),
    name: 'PublicSolutionDetail',
    meta: { title: '方案详情' }
  },
  {
    // 联系我们：企业官网模板模式导航入口，信息来自站点设置
    path: '/contact',
    component: () => import('@/views/site/contact/index.vue'),
    name: 'PublicContact',
    meta: { title: '联系我们' }
  },
  {
    path: '/category',
    component: () => import('@/views/site/category/list.vue'),
    name: 'PublicSiteCategoryList',
    meta: { title: '分类列表' }
  },
  {
    path: '/category/:id',
    component: () => import('@/views/site/category/index.vue'),
    name: 'PublicSiteCategory',
    meta: { title: '分类文章' }
  },
  {
    path: '/tag',
    component: () => import('@/views/site/tag/list.vue'),
    name: 'PublicSiteTagList',
    meta: { title: '标签列表' }
  },
  {
    path: '/tag/:id',
    component: () => import('@/views/site/tag/index.vue'),
    name: 'PublicSiteTag',
    meta: { title: '标签文章' }
  },
  {
    path: '/author/:id',
    component: () => import('@/views/site/author/index.vue'),
    name: 'PublicSiteAuthor',
    meta: { title: '作者文章' }
  },
  {
    path: '/friend-links/apply',
    component: () => import('@/views/site/friendLink/apply.vue'),
    name: 'PublicSiteFriendLinkApply',
    meta: { title: '友链申请' }
  },
  {
    path: '/about',
    component: () => import('@/views/site/about.vue'),
    name: 'PublicAbout',
    meta: { title: '关于我们' }
  },
  {
    // 荣誉资质（关于我们子页，方案文档 §1.3）
    path: '/honors',
    component: () => import('@/views/site/honors.vue'),
    name: 'PublicHonors',
    meta: { title: '荣誉资质' }
  },
  {
    // 发展历程（关于我们子页，阶段化时间轴）
    path: '/history',
    component: () => import('@/views/site/history.vue'),
    name: 'PublicHistory',
    meta: { title: '发展历程' }
  },
  {
    path: '/guestbook',
    component: () => import('@/views/site/guestbook/index.vue'),
    name: 'PublicSiteGuestbook',
    meta: { title: '留言板' }
  },
  {
    path: '/page/:slug',
    component: () => import('@/views/site/page/index.vue'),
    name: 'PublicSitePage',
    meta: { title: '页面' }
  },
  {
    path: '/agreement',
    component: () => import('@/views/site/legal/index.vue'),
    name: 'PublicSiteUserAgreement',
    meta: { title: '用户协议' }
  },
  {
    path: '/privacy',
    component: () => import('@/views/site/legal/index.vue'),
    name: 'PublicSitePrivacyPolicy',
    meta: { title: '隐私政策' }
  },
  // 注册用户认证路由（登录统一在根级 /login）
  {
    path: '/register',
    component: () => import('@/views/site/auth/Register.vue'),
    name: 'SiteRegister',
    meta: { title: '用户注册' }
  },
  {
    path: '/forgot-password',
    component: () => import('@/views/site/auth/ForgotPassword.vue'),
    name: 'SiteForgotPassword',
    meta: { title: '找回密码' }
  },
  {
    path: '/search',
    component: () => import('@/views/site/search/index.vue'),
    name: 'PublicSiteSearch',
    meta: { title: '搜索文章' }
  },
  {
    path: '/bookmarks',
    component: () => import('@/views/site/bookmark/index.vue'),
    name: 'PublicSiteBookmarks',
    meta: { title: '我的收藏' }
  }
]

export default siteRoutes
