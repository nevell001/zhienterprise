import { createApp } from 'vue'
import type { App as VueApp } from 'vue'

import Cookies from 'js-cookie'

import 'element-plus/theme-chalk/dark/css-vars.css'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'
import locale from 'element-plus/es/locale/lang/zh-cn'

import '@/assets/styles/index.scss' // global css
import '@/assets/styles/theme-dark.css'
// 企业风格认证页（登录/注册/找回密码共用）：需在 theme-dark.css 之后加载，
// 其暗色覆盖才能压过全局旧认证页规则
import '@/assets/styles/auth-enterprise.scss'

import App from './App.vue'
import { createPinia } from 'pinia'
import router from './router'
import { useSettingsStore } from '@/stores/settings'
import { initSiteSettings } from '@/utils/siteSettings'
import directive from './directive' // directive

// 导入自定义动画指令
import animateDirective from './directives/animate'

// 注册指令
import plugins from './plugins' // plugins
import installElementPlus from './plugins/element-plus'
import { download } from '@/utils/request'

// svg图标
import 'virtual:svg-icons-register'
import elementIcons from '@/components/SvgIcon/svgicon'

import { isIgnorableStartupError, markAppLoaded, renderStartupError } from '@/utils/appLoading'
import { useDict } from '@/utils/dict'
import { getConfigKey } from '@/api/system/config'
import {
  parseTime,
  resetForm,
  addDateRange,
  handleTree,
  selectDictLabel,
  selectDictLabels
} from '@/utils/zhi'

async function bootstrap(): Promise<void> {
  let mounted = false

  try {
    const app: VueApp = createApp(App)
    app.config.errorHandler = error => {
      if (isIgnorableStartupError(error)) {
        console.warn('已忽略 Vue .on() 运行时错误')
        return
      }

      console.error('Vue运行时错误:', error)
      renderStartupError(error)
    }

    // 创建并挂载Pinia实例
    const store = createPinia()
    app.use(store)

    // 全局方法挂载
    app.config.globalProperties.useDict = useDict
    app.config.globalProperties.download = download
    app.config.globalProperties.parseTime = parseTime
    app.config.globalProperties.resetForm = resetForm
    app.config.globalProperties.handleTree = handleTree
    app.config.globalProperties.addDateRange = addDateRange
    app.config.globalProperties.getConfigKey = getConfigKey
    app.config.globalProperties.selectDictLabel = selectDictLabel
    app.config.globalProperties.selectDictLabels = selectDictLabels

    app.use(plugins)
    app.use(elementIcons)

    // 注册自定义动画指令
    app.directive('animate', animateDirective)

    // 初始化设置store
    const _settingsStore = useSettingsStore()

    // 初始化系统主题监听器（如果是跟随系统模式）
    if (_settingsStore.themeMode === 'system') {
      _settingsStore.setThemeMode('system')
    }

    directive(app)

    // 使用按需注册的 Element Plus 组件，并设置全局大小与语言
    installElementPlus(app, {
      locale: locale,
      // 支持 large、default、small
      size: Cookies.get('size') || 'default'
    })

    // permission control - 必须在 Pinia 初始化之后、Router 安装之前导入
    await import('./permission')
    app.use(router)

    app.mount('#app')
    mounted = true
  } catch (error) {
    console.error('应用启动失败:', error)
    renderStartupError(error)
  } finally {
    markAppLoaded()
  }

  if (mounted) {
    void initSiteSettings()
  }
}

void bootstrap()
