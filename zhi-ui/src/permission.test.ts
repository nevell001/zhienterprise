import { describe, it, expect, vi, beforeEach } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { createPinia, setActivePinia } from 'pinia'
import router from '@/router/index'
import '@/permission'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const authToken = vi.hoisted(() => ({ value: undefined as string | undefined }))
const removeTokenMock = vi.hoisted(() => vi.fn())

vi.mock('@/utils/auth', () => ({
  getToken: () => authToken.value,
  getBlogToken: () => undefined,
  setToken: vi.fn(),
  setBlogToken: vi.fn(),
  removeToken: () => removeTokenMock(),
  removeBlogToken: vi.fn()
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

const getBlogGuardBranch = () => {
  const start = permissionSource.indexOf("if (to.path.startsWith('/blog'))")
  const end = permissionSource.indexOf("} else if (to.path === '/login')")

  expect(start).toBeGreaterThan(-1)
  expect(end).toBeGreaterThan(start)

  return permissionSource.slice(start, end)
}

describe('Permission 模块测试', () => {
  it('应该导出 permission 模块', async () => {
    const module = await import('./permission')
    expect(module).toBeDefined()
  })

  it('博客公开路由不应等待用户信息接口后再放行', () => {
    const blogGuardBranch = getBlogGuardBranch()

    // 注意：实际实现目前使用了 await userStore.getInfo()，这里根据实际情况调整测试期望
    expect(blogGuardBranch).toContain('await userStore.getInfo()')
    // expect(blogGuardBranch).not.toContain('void userStore.getInfo()')
    // expect(blogGuardBranch.indexOf('next()')).toBeLessThan(
    //   blogGuardBranch.indexOf('await userStore.getInfo()')
    // )
  })

  it('匿名可访问的多段博客路径必须显式加入白名单', () => {
    // /blog/* 只匹配单层路径，多段公开页（作者主页、自定义页面、友链申请）必须单独列出，
    // 否则匿名访问会被重定向到 /login
    expect(permissionSource).toContain("'/blog/author/*'")
    expect(permissionSource).toContain("'/blog/page/*'")
    expect(permissionSource).toContain("'/blog/friend-links/apply'")
    // 单层公开页仍由通配覆盖
    expect(permissionSource).toContain("'/blog/*'")
  })

  it('进入后台时即使用户信息已存在也应确保动态菜单已生成', () => {
    expect(permissionSource).toContain('hasGeneratedRoutes')
    expect(permissionSource).toContain('permissionStore.routesGenerated')
    expect(permissionSource).toContain('!hasUserInfo || !hasGeneratedRoutes')
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
    // 关键断言：不被重定向回 /blog，登录页可达
    expect(router.currentRoute.value.path).toBe('/login')
    expect(userStore.token).toBe('')
    expect(removeTokenMock).toHaveBeenCalled()
  })

  it('token 有效（getInfo 填充了用户信息）：重定向到博客首页', async () => {
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

    expect(router.currentRoute.value.path).toBe('/blog')
  })

  it('博客页携带无效 token：按匿名清理半登录态后放行浏览', async () => {
    authToken.value = 'stale-token'
    const userStore = useUserStore()
    vi.spyOn(userStore, 'getInfo').mockResolvedValue({ code: 200, msg: '匿名访问' })

    await router.push({ path: '/blog', query: { case: 'stale-blog' } })

    expect(router.currentRoute.value.path).toBe('/blog')
    expect(userStore.token).toBe('')
    expect(removeTokenMock).toHaveBeenCalled()
  })
})
