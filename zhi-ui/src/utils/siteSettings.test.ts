import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { initSiteSettings, readConfigValue } from './siteSettings'
import { useSettingsStore } from '@/stores/settings'
import { getConfigKey } from '@/api/system/config'

vi.mock('@/api/system/config', () => ({
  getConfigKey: vi.fn()
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'token')
}))

describe('siteSettings 工具', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    document.documentElement.removeAttribute('style')
    document.documentElement.classList.remove('theme-mo-blog')
    setActivePinia(createPinia())
  })

  it('兼容后端 AjaxResult 把配置值放在 msg 的返回形态（真实接口）', async () => {
    // GET /system/config/configKey/theme_color 实际返回 { code: 200, msg: '#409eff' }
    vi.mocked(getConfigKey).mockImplementation(async () => ({ code: 200, msg: '#409eff' }) as never)

    await initSiteSettings()

    expect(document.documentElement.style.getPropertyValue('--el-color-primary')).toBe('#409eff')
    expect(useSettingsStore().theme).toBe('#409eff')
  })

  it('readConfigValue 归一化 data / configValue / msg 三种形态', () => {
    expect(readConfigValue({ data: '#0f6e56' })).toBe('#0f6e56')
    expect(readConfigValue({ configValue: '#7c4a9e' })).toBe('#7c4a9e')
    expect(readConfigValue({ msg: '#c2410c' })).toBe('#c2410c')
    expect(readConfigValue('#b0442a')).toBe('#b0442a')
    expect(readConfigValue({})).toBe('')
    expect(readConfigValue(null)).toBe('')
    // 操作成功之类的通用提示不能当成配置值
    expect(readConfigValue({ msg: '操作成功' })).toBe('')
  })

  it('应该从博客设置的 theme_color 键初始化主题色', async () => {
    vi.mocked(getConfigKey).mockImplementation(async key => ({
      configKey: key,
      configName: key,
      configValue: key === 'theme_color' ? '#4f46e5' : ''
    }))

    await initSiteSettings()

    const settingsStore = useSettingsStore()
    expect(getConfigKey).toHaveBeenCalledWith('theme_color', { silent: true })
    expect(getConfigKey).not.toHaveBeenCalledWith('blog.custom.themeColor', { silent: true })
    expect(settingsStore.theme).toBe('#4f46e5')
    expect(document.documentElement.style.getPropertyValue('--el-color-primary')).toBe('#4f46e5')
  })
})
