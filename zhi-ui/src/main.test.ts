import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const mainSource = readFileSync(resolve(process.cwd(), 'src/main.ts'), 'utf-8')
const bootstrapSource = readFileSync(resolve(process.cwd(), 'src/bootstrap.ts'), 'utf-8')

describe('main bootstrap', () => {
  it('应该在应用挂载后立即清理启动遮罩，再异步初始化博客设置', () => {
    const mountIndex = bootstrapSource.indexOf("app.mount('#app')")
    const markLoadedIndex = bootstrapSource.indexOf('markAppLoaded()')
    const initSiteSettingsIndex = bootstrapSource.indexOf('void initSiteSettings()')

    expect(mountIndex).toBeGreaterThan(-1)
    expect(markLoadedIndex).toBeGreaterThan(mountIndex)
    expect(initSiteSettingsIndex).toBeGreaterThan(markLoadedIndex)
  })

  it('应该用 finally 保护启动遮罩清理，避免初始化异常卡住首屏', () => {
    expect(bootstrapSource).toContain('finally {')
    expect(bootstrapSource).toContain('markAppLoaded()')
  })

  it('入口文件不应静态加载应用依赖，避免依赖顶层异常卡住启动遮罩', () => {
    expect(mainSource).not.toMatch(/^import\s+.+\s+from\s+['"]vue['"]/m)
    expect(mainSource).not.toContain("from './router'")
    expect(mainSource).not.toContain("from '@/stores/settings'")
    expect(mainSource).toContain("import('./bootstrap')")
    expect(mainSource).toContain('renderStartupError(error)')
  })

  it('启动失败时应该渲染错误面板，避免用户只看到空白页', () => {
    expect(bootstrapSource).toContain('app.config.errorHandler')
    expect(bootstrapSource).toContain('renderStartupError(error)')
    expect(mainSource).toContain('renderStartupError(error)')
  })

  it('路由守卫应该在安装 router 前注册，避免直达登录页时绕过 token 重定向', () => {
    const storeInstallIndex = bootstrapSource.indexOf('app.use(store)')
    const permissionImportIndex = bootstrapSource.indexOf("await import('./permission')")
    const routerInstallIndex = bootstrapSource.indexOf('app.use(router)')

    expect(storeInstallIndex).toBeGreaterThan(-1)
    expect(permissionImportIndex).toBeGreaterThan(storeInstallIndex)
    expect(routerInstallIndex).toBeGreaterThan(permissionImportIndex)
  })
})
