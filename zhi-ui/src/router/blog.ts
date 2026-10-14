import type { RouteRecordRaw } from 'vue-router'

// 站点前台路由配置 - 仅包含前台相关路由
export const blogRoutes: RouteRecordRaw[] = [
  {
    path: '/blog',
    component: () => import('@/views/blog/index.vue'),
    name: 'PublicBlogHome',
    meta: { title: '站点首页' }
  },
  {
    // 企业官网模式下首页被自定义页面接管，文章列表固定在本地址（默认模式下与首页内容相同）
    path: '/blog/articles',
    component: () => import('@/views/blog/index.vue'),
    name: 'PublicBlogArticleList',
    meta: { title: '文章列表' }
  },
  {
    path: '/blog/article/:id',
    component: () => import('@/views/blog/article/detail.vue'),
    name: 'PublicBlogArticleDetail',
    meta: { title: '文章详情' },
    alias: ['/article/:id']
  },
  {
    // 核心业务（article_type=business 的文章）：企业官网模板的落地页
    path: '/blog/business',
    component: () => import('@/views/blog/business/index.vue'),
    name: 'PublicBusinessList',
    meta: { title: '核心业务' }
  },
  {
    path: '/blog/business/:id',
    component: () => import('@/views/blog/business/detail.vue'),
    name: 'PublicBusinessDetail',
    meta: { title: '业务详情' }
  },
  {
    // 联系我们：企业官网模板模式导航入口，信息来自站点设置
    path: '/blog/contact',
    component: () => import('@/views/blog/contact/index.vue'),
    name: 'PublicContact',
    meta: { title: '联系我们' }
  },
  {
    path: '/blog/category',
    component: () => import('@/views/blog/category/list.vue'),
    name: 'PublicBlogCategoryList',
    meta: { title: '分类列表' }
  },
  {
    path: '/blog/category/:id',
    component: () => import('@/views/blog/category/index.vue'),
    name: 'PublicBlogCategory',
    meta: { title: '分类文章' }
  },
  {
    path: '/blog/tag',
    component: () => import('@/views/blog/tag/list.vue'),
    name: 'PublicBlogTagList',
    meta: { title: '标签列表' }
  },
  {
    path: '/blog/tag/:id',
    component: () => import('@/views/blog/tag/index.vue'),
    name: 'PublicBlogTag',
    meta: { title: '标签文章' }
  },
  {
    path: '/blog/author/:id',
    component: () => import('@/views/blog/author/index.vue'),
    name: 'PublicBlogAuthor',
    meta: { title: '作者文章' }
  },
  {
    path: '/blog/friend-links/apply',
    component: () => import('@/views/blog/friendLink/apply.vue'),
    name: 'PublicBlogFriendLinkApply',
    meta: { title: '友链申请' }
  },
  {
    path: '/blog/archive',
    component: () => import('@/views/blog/archive/index.vue'),
    name: 'PublicBlogArchive',
    meta: { title: '文章归档' }
  },
  {
    path: '/blog/about',
    component: () => import('@/views/blog/about.vue'),
    name: 'PublicAbout',
    meta: { title: '关于我们' }
  },
  {
    path: '/blog/guestbook',
    component: () => import('@/views/blog/guestbook/index.vue'),
    name: 'PublicBlogGuestbook',
    meta: { title: '留言板' }
  },
  {
    path: '/blog/page/:slug',
    component: () => import('@/views/blog/page/index.vue'),
    name: 'PublicBlogPage',
    meta: { title: '页面' }
  },
  {
    path: '/blog/agreement',
    component: () => import('@/views/blog/legal/index.vue'),
    name: 'PublicBlogUserAgreement',
    meta: { title: '用户协议' }
  },
  {
    path: '/blog/privacy',
    component: () => import('@/views/blog/legal/index.vue'),
    name: 'PublicBlogPrivacyPolicy',
    meta: { title: '隐私政策' }
  },
  // 注册用户认证路由
  {
    path: '/blog/auth/login',
    redirect: '/login',
    meta: { title: '用户登录' }
  },
  {
    path: '/blog/auth/register',
    component: () => import('@/views/blog/auth/Register.vue'),
    name: 'BlogRegister',
    meta: { title: '用户注册' }
  },
  {
    path: '/blog/auth/forgot-password',
    component: () => import('@/views/blog/auth/ForgotPassword.vue'),
    name: 'BlogForgotPassword',
    meta: { title: '找回密码' }
  },
  {
    path: '/blog/search',
    component: () => import('@/views/blog/search/index.vue'),
    name: 'PublicBlogSearch',
    meta: { title: '搜索文章' }
  },
  {
    path: '/blog/bookmarks',
    component: () => import('@/views/blog/bookmark/index.vue'),
    name: 'PublicBlogBookmarks',
    meta: { title: '我的收藏' }
  },
  {
    path: '/index',
    component: () => import('@/views/blog/index.vue'),
    name: 'PublicIndex',
    meta: { title: '首页' }
  },
  {
    path: '/about',
    component: () => import('@/views/blog/about.vue'),
    name: 'PublicAboutPage',
    meta: { title: '关于' }
  }
]

export default blogRoutes
