import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
  type RouterScrollBehavior
} from 'vue-router'
import blogRoutes from './blog'

// 扩展 RouteRecordRaw 类型，添加 hidden 属性
interface ExtendedRouteRecordRaw extends Omit<RouteRecordRaw, 'children'> {
  hidden?: boolean
  permissions?: string[]
  children?: ExtendedRouteRecordRaw[]
}

const Layout = () => import('@/layout/index.vue')

const routes: ExtendedRouteRecordRaw[] = [
  {
    // 根路径直接渲染站点首页（导航地址不出现 /blog 前缀）
    path: '/',
    component: () => import('@/views/blog/index.vue'),
    name: 'PublicRoot'
  },
  {
    path: '/login',
    component: () => import('@/views/UnifiedLogin.vue')
  },
  {
    path: '/index',
    redirect: '/'
  },
  // 标签页刷新中转站（$tab.refreshPage 依赖该路由）
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        name: 'Redirect',
        component: () => import('@/views/redirect/index.vue'),
        meta: { title: '跳转中' }
      }
    ]
  },
  {
    path: '/401',
    name: 'Page401',
    component: () => import('@/views/error/401.vue'),
    hidden: true,
    meta: { title: '401' }
  },
  // 后台功能子页面：仅由页面内跳转进入，不展示在菜单中
  {
    path: '/admin/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        name: 'AuthRole',
        component: () => import('@/views/admin/system/user/user/authRole.vue'),
        meta: { title: '分配角色', activeMenu: '/admin/system/user' }
      }
    ]
  },
  {
    path: '/admin/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        name: 'AuthUser',
        component: () => import('@/views/admin/system/role/role/authUser.vue'),
        meta: { title: '分配用户', activeMenu: '/admin/system/role' }
      }
    ]
  },
  {
    path: '/admin/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        name: 'DictData',
        component: () => import('@/views/admin/system/dict/dict/data.vue'),
        meta: { title: '字典数据', activeMenu: '/admin/system/dict' }
      }
    ]
  },
  {
    path: '/admin/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        name: 'JobLog',
        component: () => import('@/views/admin/monitor/job/log.vue'),
        meta: { title: '调度日志', activeMenu: '/admin/monitor/job' }
      }
    ]
  },
  {
    path: '/admin/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        name: 'GenEdit',
        component: () => import('@/views/admin/tool/gen/editTable.vue'),
        meta: { title: '修改生成配置', activeMenu: '/admin/tool/gen' }
      }
    ]
  },
  // 个人中心（所有登录用户可访问）
  {
    path: '/user',
    component: () => import('@/layout/index.vue'),
    redirect: '/user/profile',
    hidden: true,
    children: [
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/admin/system/user/user/profile/index.vue'),
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  },
  // 站点前台路由
  ...blogRoutes,
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  }
]

const scrollBehavior: RouterScrollBehavior = (_to, _from, savedPosition) => {
  // 解决页面跳转后滚动位置问题
  if (savedPosition) {
    return savedPosition
  } else {
    return { top: 0 }
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: routes as RouteRecordRaw[],
  // 添加错误处理
  scrollBehavior
})

// 导出路由配置
export const constantRoutes = routes
// 动态路由（从后端获取，根据用户权限动态添加）
export const dynamicRoutes: RouteRecordRaw[] = []

export default router
