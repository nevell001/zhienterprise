import { defineStore } from 'pinia'
import { constantRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import { useUserStore } from '@/stores/user'
import type { RouteRecordRaw } from 'vue-router'

const Layout = () => import('@/layout/index.vue')
const ParentView = () => import('@/components/ParentView')
const InnerLink = () => import('@/layout/components/InnerLink')

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('../views/**/*.vue')

interface ViewState {
  routes: RouteRecordRaw[]
  addRoutes: RouteRecordRaw[]
  defaultRoutes: RouteRecordRaw[]
  topbarRouters: RouteRecordRaw[]
  sidebarRouters: RouteRecordRaw[]
  routesGenerated: boolean
}

const usePermissionStore = defineStore('permission', {
  state: (): ViewState => ({
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: [],
    routesGenerated: false
  }),

  actions: {
    setRoutes(routes: RouteRecordRaw[]): void {
      this.addRoutes = routes
      this.routes = constantRoutes.concat(routes) as RouteRecordRaw[]
    },

    setDefaultRoutes(routes: RouteRecordRaw[]): void {
      this.defaultRoutes = constantRoutes.concat(routes) as RouteRecordRaw[]
    },

    setTopbarRoutes(routes: RouteRecordRaw[]): void {
      this.topbarRouters = routes
    },

    setSidebarRouters(routes: RouteRecordRaw[]): void {
      this.sidebarRouters = routes
    },

    generateRoutes(): Promise<RouteRecordRaw[]> {
      return new Promise((resolve, reject) => {
        // 向后端请求路由数据
        getRouters()
          .then((res: any) => {
            const sdata = JSON.parse(JSON.stringify(res.data))
            const rdata = JSON.parse(JSON.stringify(res.data))
            const sidebarRoutes = filterAsyncRouter(sdata)
            const rewriteRoutes = filterAsyncRouter(rdata, false, true)
            // 为所有路由生成唯一名称，使用父路由名称作为前缀避免冲突
            generateRouteNames(sidebarRoutes)
            generateRouteNames(rewriteRoutes)
            this.setRoutes(rewriteRoutes)
            this.setSidebarRouters(sidebarRoutes)
            this.setDefaultRoutes(sidebarRoutes)
            this.setTopbarRoutes(rewriteRoutes)
            this.routesGenerated = true
            resolve(rewriteRoutes)
          })
          .catch(error => {
            reject(error)
          })
      })
    }
  }
})

// 根据路径生成稳定的路由名称（斜杠转下划线，子路由以父名称做前缀）
function generateRouteNames(routes: any[], parentName = ''): void {
  routes.forEach(route => {
    if (route.path) {
      const pathName = route.path.replace(/\//g, '_').replace(/^_+|_+$/g, '')
      if (parentName) {
        route.name = `${parentName}_${pathName}`
      } else {
        route.name = pathName || `route_${Date.now()}`
      }
    }
    if (route.children && Array.isArray(route.children)) {
      generateRouteNames(route.children, route.name)
    }
  })
}

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap: any[], _lastRouter = false, type = false): any[] {
  return asyncRouterMap.filter(route => {
    // 过滤隐藏的路由
    if (route.hidden) {
      return false
    }

    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap: any[], lastRouter = false): any[] {
  let children: any[] = []
  childrenMap.forEach((el, _index) => {
    // 过滤隐藏的子路由
    if (el.hidden) {
      return
    }

    if (el.children && el.children.length) {
      if (el.component === 'ParentView' && !lastRouter) {
        el.children.forEach((c: any) => {
          c.path = el.path + '/' + c.path
          if (c.children && c.children.length) {
            children = children.concat(filterChildren(c.children, c))
            return
          }
          children.push(c)
        })
        return
      }
    }
    if (lastRouter && lastRouter !== true) {
      el.path = (lastRouter as any).path + '/' + el.path
    }
    children = children.concat(el)
  })
  return children
}

// RuoYi-Vue3 兼容的 loadView 实现
export const loadView = (view: string) => {
  let res: any

  // 优先匹配 admin/ 前缀的路径（管理后台组件）
  for (const path in modules) {
    const dir = path.split('views/')[1]?.split('.vue')[0]
    if (dir === `admin/${view}`) {
      res = () => modules[path]()
      break
    }
  }

  // 如果没有找到 admin/ 前缀的组件，尝试直接匹配（站点前台组件）
  if (!res) {
    for (const path in modules) {
      const dir = path.split('views/')[1]?.split('.vue')[0]
      if (dir === view) {
        res = () => modules[path]()
        break
      }
    }
  }

  // 如果没有找到匹配的组件，返回一个默认组件
  if (!res) {
    console.error(`无法加载组件: ${view}，请检查组件路径是否正确`)
  }

  return res
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes: any[]): any[] {
  const res: any[] = []
  routes.forEach(route => {
    if (route.permissions) {
      if (hasPermission(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (hasRole(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

// 验证是否有权限
function hasPermission(requiredPermissions: string[]): boolean {
  const all_permission = '*:*:*'
  const userPermissions = useUserStore().permissions

  // 如果没有设置权限要求，默认允许访问
  if (!requiredPermissions || requiredPermissions.length === 0) {
    return true
  }

  if (userPermissions && userPermissions.length > 0) {
    return userPermissions.some(v => {
      return all_permission === v || requiredPermissions.includes(v)
    })
  } else {
    return hasRole(['admin', 'editor'])
  }
}

// 验证是否有角色
function hasRole(requiredRoles: string[]): boolean {
  const super_admin = 'admin'
  const userRoles = useUserStore().roles

  if (!requiredRoles || requiredRoles.length === 0) {
    return true
  }

  if (userRoles && userRoles.length > 0) {
    return userRoles.some(v => {
      return super_admin === v || requiredRoles.includes(v)
    })
  } else {
    return Boolean(import.meta.env?.DEV)
  }
}

export { usePermissionStore }
