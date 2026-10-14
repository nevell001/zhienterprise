import type { RouteRecordRaw } from 'vue-router'

// 站点前台路由配置 - 仅包含前台相关路由
// 地址规范：前台页面一律使用短路径（不带 /blog 前缀），与导航、站点地图保持一致
export const blogRoutes: RouteRecordRaw[] = [
  {
    // 企业官网模式下首页被自定义页面接管，文章列表固定在本地址（默认模式下与首页内容相同）
    path: '/news',
    component: () => import('@/views/blog/index.vue'),
    name: 'PublicBlogArticleList',
    meta: { title: '文章列表' }
  },
  {
    path: '/article/:id',
    component: () => import('@/views/blog/article/detail.vue'),
    name: 'PublicBlogArticleDetail',
    meta: { title: '文章详情' }
  },
  {
    // 核心业务（article_type=business 的文章）：企业官网模板的落地页
    path: '/business',
    component: () => import('@/views/blog/business/index.vue'),
    name: 'PublicBusinessList',
    meta: { title: '核心业务' }
  },
  {
    path: '/business/:id',
    component: () => import('@/views/blog/business/detail.vue'),
    name: 'PublicBusinessDetail',
    meta: { title: '业务详情' }
  },
  {
    // 联系我们：企业官网模板模式导航入口，信息来自站点设置
    path: '/contact',
    component: () => import('@/views/blog/contact/index.vue'),
    name: 'PublicContact',
    meta: { title: '联系我们' }
  },
  {
    path: '/category',
    component: () => import('@/views/blog/category/list.vue'),
    name: 'PublicBlogCategoryList',
    meta: { title: '分类列表' }
  },
  {
    path: '/category/:id',
    component: () => import('@/views/blog/category/index.vue'),
    name: 'PublicBlogCategory',
    meta: { title: '分类文章' }
  },
  {
    path: '/tag',
    component: () => import('@/views/blog/tag/list.vue'),
    name: 'PublicBlogTagList',
    meta: { title: '标签列表' }
  },
  {
    path: '/tag/:id',
    component: () => import('@/views/blog/tag/index.vue'),
    name: 'PublicBlogTag',
    meta: { title: '标签文章' }
  },
  {
    path: '/author/:id',
    component: () => import('@/views/blog/author/index.vue'),
    name: 'PublicBlogAuthor',
    meta: { title: '作者文章' }
  },
  {
    path: '/friend-links/apply',
    component: () => import('@/views/blog/friendLink/apply.vue'),
    name: 'PublicBlogFriendLinkApply',
    meta: { title: '友链申请' }
  },
  {
    path: '/archive',
    component: () => import('@/views/blog/archive/index.vue'),
    name: 'PublicBlogArchive',
    meta: { title: '文章归档' }
  },
  {
    path: '/about',
    component: () => import('@/views/blog/about.vue'),
    name: 'PublicAbout',
    meta: { title: '关于我们' }
  },
  {
    // 荣誉资质（关于我们子页，方案文档 §1.3）
    path: '/honors',
    component: () => import('@/views/blog/honors.vue'),
    name: 'PublicHonors',
    meta: { title: '荣誉资质' }
  },
  {
    // 发展历程（关于我们子页，阶段化时间轴）
    path: '/history',
    component: () => import('@/views/blog/history.vue'),
    name: 'PublicHistory',
    meta: { title: '发展历程' }
  },
  {
    path: '/guestbook',
    component: () => import('@/views/blog/guestbook/index.vue'),
    name: 'PublicBlogGuestbook',
    meta: { title: '留言板' }
  },
  {
    path: '/page/:slug',
    component: () => import('@/views/blog/page/index.vue'),
    name: 'PublicBlogPage',
    meta: { title: '页面' }
  },
  {
    path: '/agreement',
    component: () => import('@/views/blog/legal/index.vue'),
    name: 'PublicBlogUserAgreement',
    meta: { title: '用户协议' }
  },
  {
    path: '/privacy',
    component: () => import('@/views/blog/legal/index.vue'),
    name: 'PublicBlogPrivacyPolicy',
    meta: { title: '隐私政策' }
  },
  // 注册用户认证路由（登录统一在根级 /login）
  {
    path: '/register',
    component: () => import('@/views/blog/auth/Register.vue'),
    name: 'BlogRegister',
    meta: { title: '用户注册' }
  },
  {
    path: '/forgot-password',
    component: () => import('@/views/blog/auth/ForgotPassword.vue'),
    name: 'BlogForgotPassword',
    meta: { title: '找回密码' }
  },
  {
    path: '/search',
    component: () => import('@/views/blog/search/index.vue'),
    name: 'PublicBlogSearch',
    meta: { title: '搜索文章' }
  },
  {
    path: '/bookmarks',
    component: () => import('@/views/blog/bookmark/index.vue'),
    name: 'PublicBlogBookmarks',
    meta: { title: '我的收藏' }
  }
]

export default blogRoutes
