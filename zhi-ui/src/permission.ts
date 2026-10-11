import router from './router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken, removeToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { useSettingsStore } from '@/stores/settings'
import type { RouteLocationNormalized, NavigationGuardNext } from 'vue-router'

// 确保 NProgress 配置
NProgress.configure({ showSpinner: false })

const whiteList = [
  '/login',
  '/register',
  '/index',
  '/blog',
  '/blog/*',
  '/blog/auth/login',
  '/blog/auth/register',
  '/blog/auth/forgot-password',
  '/about',
  '/',
  '/blog/article/*',
  '/blog/category/*',
  '/blog/tag/*',
  // 多段路径必须单独列出：/blog/* 只匹配一层（* 编译为 [^/]*）
  '/blog/author/*',
  '/blog/page/*',
  '/blog/friend-links/apply',
  '/blog/archive',
  '/blog/simple',
  '/blog/article',
  '/blog/category',
  '/blog/tag'
]

const isWhiteList = (path: string): boolean => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

// 防止重复导航的标志
let isNavigating = false

router.beforeEach(
  async (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    // 如果已经在导航中，且目标路径相同，则取消导航
    if (isNavigating && to.path === from.path) {
      return next(false)
    }

    isNavigating = true
    NProgress.start()

    try {
      if (getToken()) {
        to.meta.title && useSettingsStore().setTitle(to.meta.title as string)
        /* has token*/
        // 如果访问站点前台页面，需要获取用户信息
        // 同时预加载后台路由，避免点击"管理后台"时出现404
        if (to.path.startsWith('/blog')) {
          const userStore = useUserStore()
          const permissionStore = usePermissionStore()

          // 如果用户信息为空，则获取用户信息
          if (!userStore.name || userStore.roles.length === 0) {
            await userStore.getInfo().catch(err => {
              console.error('获取用户信息失败:', err)
              // 获取用户信息失败，清除 token 但继续访问站点页面（作为匿名用户）
              userStore.token = ''
              userStore.roles = []
              userStore.permissions = []
              removeToken()
            })
          }
          // 401 拦截器对站点路径返回假 200 占位响应时 getInfo 不会抛错：
          // 有 cookie 却拿不到用户信息 = token 实际无效，同样按匿名清理，
          // 避免留下「登录按钮与残留 token 并存」的半登录态
          if (!userStore.name && getToken()) {
            userStore.token = ''
            userStore.roles = []
            userStore.permissions = []
            removeToken()
          }

          // 如果后台路由还没生成，则预生成
          const hasGeneratedRoutes = permissionStore.routesGenerated
          if (!hasGeneratedRoutes && userStore.roles.length > 0) {
            try {
              const accessRoutes = await permissionStore.generateRoutes()
              if (accessRoutes && Array.isArray(accessRoutes)) {
                // 为所有路由生成唯一名称，使用父路由名称作为前缀避免冲突
                const generateNames = (routes: any[], parentName = '') => {
                  routes.forEach((route: any) => {
                    if (route.path) {
                      const pathName = route.path.replace(/\//g, '_').replace(/^_+|_+$/g, '')
                      if (parentName) {
                        route.name = `${parentName}_${pathName}`
                      } else {
                        route.name = pathName || `route_${Date.now()}`
                      }
                    }
                    if (route.children && Array.isArray(route.children)) {
                      generateNames(route.children, route.name)
                    }
                  })
                }
                generateNames(accessRoutes)
                accessRoutes.forEach((route: any) => {
                  if (route && route.name && !router.hasRoute(route.name as string)) {
                    router.addRoute(route)
                  }
                })
                router.addRoute({
                  name: 'NotFound',
                  path: '/:pathMatch(.*)*',
                  redirect: '/404'
                })
              }
            } catch (err) {
              console.error('预加载后台路由失败:', err)
            }
          }

          next()
        } else if (to.path === '/login') {
          // 访问登录页时，先验证 token 是否有效
          const userStore = useUserStore()
          try {
            // 尝试获取用户信息以验证 token
            if (userStore.token && (!userStore.name || userStore.roles.length === 0)) {
              await userStore.getInfo()
            }
          } catch (error) {
            console.warn('token 验证失败，允许访问登录页:', error)
          }
          // 校验 getInfo 的结果而非依赖它抛错：401 拦截器对站点路径会返回
          // 「匿名访问」假 200 占位响应，getInfo 静默成功但拿不到用户信息。
          // 此时 token 实际无效，必须清掉，否则点击「登录」会被重定向回 /blog，
          // 登录页永远打不开
          if (!userStore.name) {
            userStore.token = ''
            userStore.roles = []
            userStore.permissions = []
            removeToken()
            next()
          } else {
            // token 有效，已获取到用户信息，重定向到首页或 redirect 参数指定的页面
            const redirect = to.query.redirect as string
            if (redirect && redirect !== '/login' && redirect !== '/' && redirect !== '/index') {
              next({ path: redirect, replace: true })
            } else {
              next({ path: '/blog', replace: true })
            }
          }
        } else {
          const userStore = useUserStore()
          const permissionStore = usePermissionStore()
          const hasUserInfo = userStore.roles.length > 0
          const hasGeneratedRoutes = permissionStore.routesGenerated

          if (!hasUserInfo || !hasGeneratedRoutes) {
            // 判断当前用户信息和后台动态菜单是否已初始化
            try {
              if (!hasUserInfo) {
                permissionStore.routesGenerated = false
                await userStore.getInfo()
              }
              // 生成可访问的路由表
              const accessRoutes = await permissionStore.generateRoutes()
              // 根据roles权限生成可访问的路由表
              if (accessRoutes && Array.isArray(accessRoutes)) {
                // 为所有路由生成唯一名称，使用父路由名称作为前缀避免冲突
                const generateNames = (routes: any[], parentName = '') => {
                  routes.forEach((route: any) => {
                    if (route.path) {
                      const pathName = route.path.replace(/\//g, '_').replace(/^_+|_+$/g, '')
                      if (parentName) {
                        route.name = `${parentName}_${pathName}`
                      } else {
                        route.name = pathName || `route_${Date.now()}`
                      }
                    }
                    if (route.children && Array.isArray(route.children)) {
                      generateNames(route.children, route.name)
                    }
                  })
                }
                generateNames(accessRoutes)

                // 添加路由到 router
                accessRoutes.forEach((route: any) => {
                  if (route && route.name && !router.hasRoute(route.name as string)) {
                    router.addRoute(route)
                  }
                })
                // 动态路由添加完成后，添加404通配符路由
                router.addRoute({
                  name: 'NotFound',
                  path: '/:pathMatch(.*)*',
                  redirect: '/404'
                })
              }
              // 确保addRoutes已完成
              next({ ...to, replace: true })
              NProgress.done()
            } catch (err) {
              console.error('获取用户信息或生成路由失败:', err)
              // 直接清除用户信息并重定向
              userStore.token = ''
              userStore.roles = []
              userStore.permissions = []
              removeToken()
              next({ path: '/login', replace: true })
            }
          } else {
            next()
          }
        }
      } else {
        // 没有token
        if (isWhiteList(to.path)) {
          // 在免登录白名单，直接进入
          next()
        } else {
          // 避免循环重定向
          if (to.path !== '/login') {
            next(`/login?redirect=${to.fullPath}`)
            NProgress.done()
          } else {
            next()
          }
        }
      }
    } catch (error) {
      console.error('路由守卫错误:', error)
      next(false)
    } finally {
      isNavigating = false
    }
  }
)

router.afterEach(() => {
  NProgress.done()
})

export default router
