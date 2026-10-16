import { describe, it, expect, vi, beforeEach } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { createPinia, setActivePinia } from 'pinia'
import router from '@/router/index'
import { siteRoutes } from '@/router/site'
import '@/permission'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const authToken = vi.hoisted(() => ({ value: undefined as string | undefined }))
const removeTokenMock = vi.hoisted(() => vi.fn())

vi.mock('@/utils/auth', () => ({
  getToken: () => authToken.value,
  getSiteToken: () => undefined,
  setToken: vi.fn(),
  setSiteToken: vi.fn(),
  removeToken: () => removeTokenMock(),
  removeSiteToken: vi.fn()
}))

// nprogress 的 done() 延迟约 200ms 才 remove()，vitest 先拆掉 jsdom 环境后该定时器读取
// document 抛 ReferenceError（测试全过但 vitest exit 1，CI 随机变红）。本文件不校验进度条，桩掉
vi.mock('nprogress', () => ({
  default: {
    configure: vi.fn(),
    start: vi.fn(),
    done: vi.fn()
  }
}))

const permissionSource = readFileSync(resolve(process.cwd(), 'src/permission.ts'), 'utf-8')

describe('Permission 模块测试', () => {
  it('应该导出 permission 模块', async () => {
    const module = await import('./permission')
    expect(module).toBeDefined()
  })

  it('进入后台时即使用户信息已存在也应确保动态菜单已生成', () => {
    expect(permissionSource).toContain('hasGeneratedRoutes')
    expect(permissionSource).toContain('permissionStore.routesGenerated')
    expect(permissionSource).toContain('!hasUserInfo || !hasGeneratedRoutes')
  })
})

describe('匿名访问站点前台（白名单行为）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authToken.value = undefined
    setActivePinia(createPinia())
  })

  // 白名单与路由表必须一一对应：一旦缺项，匿名访问会被重定向到 /login。
  // 从 siteRoutes 派生而非手维护清单——新增前台页面漏加白名单会直接在此跑红
  // （/solutions 曾因手维护表漏项而整板块匿名不可达）；路由表之外的根路径另列
  const publicPages: Array<{ path: string; name: string }> = [
    { path: '/', name: 'PublicRoot' },
    ...siteRoutes.map(route => ({
      path: route.path.replace(/:\w+/g, '1'),
      name: route.name as string
    }))
  ]

  it.each(publicPages)('匿名访问 $path 放行且解析到 $name', async ({ path, name }) => {
    // 唯一 query 避免 vue-router 对相同 location 去重短路守卫
    await router.push({ path, query: { case: path } })

    expect(router.currentRoute.value.path).toBe(path)
    expect(router.currentRoute.value.name).toBe(name)
  })
})

describe('登录页路由守卫（行为）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authToken.value = undefined
    setActivePinia(createPinia())
  })

  it('匿名状态点击登录：放行进入登录页', async () => {
    // 唯一 query 避免 vue-router 对相同 location 去重短路守卫
    await router.push({ path: '/login', query: { case: 'anon' } })
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('残留过期 token 且 getInfo 拿到 401 假 200 占位响应：清 token 并放行登录页', async () => {
    authToken.value = 'stale-token'
    const userStore = useUserStore()
    const getInfoSpy = vi
      .spyOn(userStore, 'getInfo')
      .mockResolvedValue({ code: 200, msg: '匿名访问' })

    await router.push({ path: '/login', query: { case: 'stale' } })

    expect(getInfoSpy).toHaveBeenCalled()
    // 关键断言：不被重定向回首页，登录页可达
    expect(router.currentRoute.value.path).toBe('/login')
    expect(userStore.token).toBe('')
    expect(removeTokenMock).toHaveBeenCalled()
  })

  it('token 有效（getInfo 填充了用户信息）：重定向到站点首页', async () => {
    authToken.value = 'valid-token'
    const userStore = useUserStore()
    vi.spyOn(userStore, 'getInfo').mockImplementation(async () => {
      userStore.name = 'admin'
      userStore.roles = ['ROLE_DEFAULT']
      userStore.permissions = []
      return { code: 200 }
    })
    usePermissionStore().routesGenerated = true

    await router.push({ path: '/login', query: { case: 'valid' } })

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('前台页（短路径）携带无效 token：按匿名清理半登录态后放行浏览', async () => {
    authToken.value = 'stale-token'
    const userStore = useUserStore()
    vi.spyOn(userStore, 'getInfo').mockResolvedValue({ code: 200, msg: '匿名访问' })

    await router.push({ path: '/news', query: { case: 'stale-front' } })

    expect(router.currentRoute.value.path).toBe('/news')
    expect(userStore.token).toBe('')
    expect(removeTokenMock).toHaveBeenCalled()
  })

  it('前台页携带有效 token：获取用户信息后仍停留在前台页', async () => {
    authToken.value = 'valid-token'
    const userStore = useUserStore()
    const getInfoSpy = vi.spyOn(userStore, 'getInfo').mockImplementation(async () => {
      userStore.name = 'admin'
      userStore.roles = ['ROLE_DEFAULT']
      userStore.permissions = []
      return { code: 200 }
    })
    usePermissionStore().routesGenerated = true

    await router.push({ path: '/archive', query: { case: 'valid-front' } })

    expect(getInfoSpy).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/archive')
    expect(userStore.token).toBe('valid-token')
  })
})
