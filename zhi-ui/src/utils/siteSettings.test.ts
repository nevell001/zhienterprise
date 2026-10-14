import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { initSiteSettings } from './siteSettings'
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
