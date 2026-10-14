import { describe, it, expect } from 'vitest'
import router, { constantRoutes, dynamicRoutes } from './index'
import { siteRoutes } from './site'

const flattenRoutes = (routes: any[], parentPath = ''): any[] =>
  routes.flatMap(route => {
    const path = route.path.startsWith('/')
      ? route.path
      : `${parentPath.replace(/\/$/, '')}/${route.path}`
    const current = { ...route, path }
    return [current, ...flattenRoutes(route.children || [], path)]
  })

describe('Router Index 测试', () => {
  it('应该导出 constantRoutes', () => {
    expect(constantRoutes).toBeDefined()
    expect(Array.isArray(constantRoutes)).toBe(true)
    expect(constantRoutes.length).toBeGreaterThan(0)
  })

  it('应该导出 dynamicRoutes', () => {
    expect(dynamicRoutes).toBeDefined()
    expect(Array.isArray(dynamicRoutes)).toBe(true)
  })

  it('应该导出 router 实例', () => {
    expect(router).toBeDefined()
    expect(typeof router).toBe('object')
  })

  it('constantRoutes 应该完整包含前台短路径路由表', () => {
    const paths = constantRoutes.map(route => route.path)
    const missing = siteRoutes.map(route => route.path).filter(path => !paths.includes(path))
    expect(missing).toEqual([])
  })

  it('constantRoutes 不应该出现 /blog 前缀的页面路由', () => {
    const legacyRoutes = constantRoutes.filter(route => route.path.startsWith('/blog'))
    expect(legacyRoutes.map(route => route.path)).toEqual([])
  })

  // 移除对动态管理路由在 constantRoutes 中存在的硬性要求，改为在 permission store 中验证
  // it('constantRoutes 应该包含后台管理路由', () => { ... })
  // it('constantRoutes 应该包含系统管理路由', () => { ... })
  // ... 等等

  it('所有路由都应该有 path 属性', () => {
    const allRoutes = [...constantRoutes, ...dynamicRoutes]
    const routesWithoutPath = allRoutes.filter(route => !route.path)

    expect(routesWithoutPath.length).toBe(0)
  })

  it('所有路由都应该有 component 或 redirect（纯中转合法）', () => {
    const allRoutes = [...constantRoutes, ...dynamicRoutes]
    const invalidRoutes = allRoutes.filter(route => !route.component && !route.redirect)

    expect(invalidRoutes.map(r => r.path)).toEqual([])
  })

  it('根路径应该直接渲染站点首页（导航地址不出现 /blog 前缀）', () => {
    const rootRoute = constantRoutes.find(route => route.path === '/')
    expect(rootRoute?.component).toBeDefined()
    expect(rootRoute?.name).toBe('PublicRoot')
  })

  it('应该包含后台隐藏子页面路由（否则页面内跳转必然 404）', () => {
    const paths = flattenRoutes(constantRoutes).map(route => route.path)

    expect(paths).toContain('/redirect/:path(.*)')
    expect(paths).toContain('/401')
    expect(paths).toContain('/admin/system/user-auth/role/:userId(\\d+)')
    expect(paths).toContain('/admin/system/role-auth/user/:roleId(\\d+)')
    expect(paths).toContain('/admin/system/dict-data/index/:dictId(\\d+)')
    expect(paths).toContain('/admin/monitor/job-log/index/:jobId(\\d+)')
    expect(paths).toContain('/admin/tool/gen-edit/index/:tableId(\\d+)')
  })

  it('隐藏子页面路由应该能解析出对应名称', () => {
    expect(router.resolve('/redirect/admin/article').name).toBe('Redirect')
    expect(router.resolve('/401').name).toBe('Page401')
    expect(router.resolve('/admin/system/user-auth/role/1').name).toBe('AuthRole')
    expect(router.resolve('/admin/system/role-auth/user/1').name).toBe('AuthUser')
    expect(router.resolve('/admin/system/dict-data/index/1').name).toBe('DictData')
    expect(router.resolve('/admin/monitor/job-log/index/1').name).toBe('JobLog')
    expect(router.resolve('/admin/tool/gen-edit/index/1').name).toBe('GenEdit')
  })

  it('隐藏子页面路由不应该出现在菜单中', () => {
    const hiddenParents = constantRoutes.filter(route =>
      [
        '/redirect',
        '/admin/system/user-auth',
        '/admin/system/role-auth',
        '/admin/system/dict-data',
        '/admin/monitor/job-log',
        '/admin/tool/gen-edit'
      ].includes(route.path)
    )

    expect(hiddenParents).toHaveLength(6)
    hiddenParents.forEach(route => {
      expect(route.hidden).toBe(true)
    })
  })

  // 移除所有关于管理后台静态存在的断言
})
