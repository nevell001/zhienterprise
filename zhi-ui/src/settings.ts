import type { SettingsConfig } from './settings.d'

const settings: SettingsConfig = {
  /**
   * 网页标题
   */
  title: import.meta.env?.VITE_APP_TITLE || '知企建站系统',

  /**
   * 主题
   */
  theme: '#4f46e5',

  /**
   * 侧边栏主题 深色主题theme-dark，浅色主题theme-light
   */
  sideTheme: 'theme-dark',

  /**
   * 是否系统布局配置
   */
  showSettings: true,

  /**
   * 是否显示顶部导航
   */
  topNav: false,

  /**
   * 是否显示 tagsView
   */
  tagsView: true,

  /**
   * 显示页签图标
   */
  tagsIcon: false,

  /**
   * 是否固定头部
   */
  fixedHeader: false,

  /**
   * 是否显示logo
   */
  sidebarLogo: true,

  /**
   * 是否显示动态标题
   */
  dynamicTitle: false,

  /**
   * 是否显示底部版权
   */
  footerVisible: false,

  /**
   * 底部版权文本内容
   */
  footerContent: 'Copyright © 2025-2026 Nevell. All Rights Reserved.',

  /**
   * 侧边栏文字主题
   */
  sidebarTextTheme: '#fff'
}

export default settings
