import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSettingsStore } from './settings'

describe('Settings Store 测试', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
    document.documentElement.classList.remove('theme-mo-blog')
    setActivePinia(createPinia())
  })

  describe('初始状态', () => {
    it('应该初始化为默认值', () => {
      const store = useSettingsStore()
      expect(store.theme).toBeDefined()
      expect(store.theme).toBe('#4f46e5')
      expect(store.appTheme).toBe('default')
      expect(store.sideTheme).toBeDefined()
      expect(store.showSettings).toBeDefined()
      expect(store.topNav).toBeDefined()
      expect(store.showTagsView).toBeDefined()
      expect(store.fixedHeader).toBeDefined()
      expect(store.showSidebarLogo).toBeDefined()
      expect(store.dynamicTitle).toBeDefined()
    })
  })

  describe('changeSetting', () => {
    it('应该更新设置', () => {
      const store = useSettingsStore()
      store.changeSetting({
        key: 'showSettings',
        value: true
      })
      expect(store.showSettings).toBe(true)
    })

    it('应该支持更新主题', () => {
      const store = useSettingsStore()
      store.changeSetting({
        key: 'theme',
        value: '#FF0000'
      })
      expect(store.theme).toBe('#FF0000')
    })

    it('历史 mo-blog 偏好应归一化为 default（Mo-Blog 为唯一皮肤）', () => {
      localStorage.setItem('app-theme', 'mo-blog')
      const store = useSettingsStore()
      expect(store.appTheme).toBe('default')
    })

    it('布局设置中的历史 mo-blog 偏好归一化为 default，但皮肤类仍挂载', () => {
      localStorage.setItem('layout-setting', JSON.stringify({ appTheme: 'mo-blog' }))
      const store = useSettingsStore()
      expect(store.appTheme).toBe('default')
      // Mo-Blog 是唯一皮肤，恒挂载（否则 mo-blog.scss 519 处规则失效）
      expect(document.documentElement.classList.contains('theme-mo-blog')).toBe(true)
    })

    it('设置应用主题时历史值归一化为 default，皮肤类仍挂载', () => {
      const store = useSettingsStore()
      store.setAppTheme('mo-blog')
      expect(store.appTheme).toBe('default')
      expect(document.documentElement.classList.contains('theme-mo-blog')).toBe(true)
    })

    it('切换主题后应同步 layout-setting，刷新不再回退', () => {
      // 模拟此前保存过 mo-blog
      localStorage.setItem('layout-setting', JSON.stringify({ appTheme: 'mo-blog' }))
      const store = useSettingsStore()
      store.setAppTheme('default')
      // 切换后 layout-setting 应同步更新
      const stored = JSON.parse(localStorage.getItem('layout-setting') || '{}')
      expect(stored.appTheme).toBe('default')
      // 模拟刷新：用全新 pinia 实例重新初始化
      setActivePinia(createPinia())
      const refreshed = useSettingsStore()
      expect(refreshed.appTheme).toBe('default')
    })

    it('应该支持切换侧边栏主题', () => {
      const store = useSettingsStore()
      store.changeSetting({
        key: 'sideTheme',
        value: 'theme-light'
      })
      expect(store.sideTheme).toBe('theme-light')
    })

    it('应该支持切换顶部导航', () => {
      const store = useSettingsStore()
      store.changeSetting({
        key: 'topNav',
        value: true
      })
      expect(store.topNav).toBe(true)
    })

    it('应该支持切换标签页视图', () => {
      const store = useSettingsStore()
      store.changeSetting({
        key: 'showTagsView',
        value: false
      })
      expect(store.showTagsView).toBe(false)
    })

    it('应该支持固定头部', () => {
      const store = useSettingsStore()
      store.changeSetting({
        key: 'fixedHeader',
        value: true
      })
      expect(store.fixedHeader).toBe(true)
    })
  })

  describe('setTitle', () => {
    it('应该设置标题', () => {
      const store = useSettingsStore()
      store.setTitle('New Title')
      expect(store.title).toBe('New Title')
    })
  })

  describe('toggleTheme', () => {
    it('应该按顺序循环切换主题模式 (light -> dark -> system)', () => {
      const store = useSettingsStore()
      // 初始应为 system
      expect(store.themeMode).toBe('system')

      store.toggleTheme() // -> light
      expect(store.themeMode).toBe('light')

      store.toggleTheme() // -> dark
      expect(store.themeMode).toBe('dark')
      expect(store.isDark).toBe(true)
      expect(document.documentElement.classList.contains('dark')).toBe(true)

      store.toggleTheme() // -> system
      expect(store.themeMode).toBe('system')
    })
  })

  describe('setServerMessage', () => {
    it('应该设置服务器消息', () => {
      const store = useSettingsStore()
      store.setServerMessage('Test message')
      expect(store.serverMessage).toBe('Test message')
    })
  })

  describe('setIsShowUpload', () => {
    it('应该设置上传状态', () => {
      const store = useSettingsStore()
      store.setIsShowUpload(true)
      expect(store.isShowUpload).toBe(true)
    })
  })
})
