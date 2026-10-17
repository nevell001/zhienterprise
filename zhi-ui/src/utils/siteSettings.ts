import { getConfigKey } from '@/api/system/config'
import { useSettingsStore } from '@/stores/settings'
import { handleThemeStyle } from '@/utils/theme'
import { getToken } from '@/utils/auth'
import type { Config } from '@/api/system/config'
import { normalizeUploadUrl } from '@/utils/uploadUrl'

/**
 * 初始化站点个性化设置
 */
export async function initSiteSettings(): Promise<void> {
  try {
    // 检查是否有token，只有有token时才发起API请求
    if (!getToken()) {
      // 未登录状态，使用默认配置，不发起API请求
      return
    }

    // 获取站点个性化设置（silent: 无权限时不弹通知，站点前台有自己的公开设置接口）
    const silentCfg = { silent: true }
    const [themeColor, customLogo, logoUrl, sidebarStyle, customCss] = await Promise.all([
      getConfigKey('theme_color', silentCfg).catch((): Config => ({
        configKey: '',
        configValue: '',
        configName: ''
      })),
      getConfigKey('site_logo', silentCfg).catch((): Config => ({
        configKey: '',
        configValue: '',
        configName: ''
      })),
      getConfigKey('blog_avatar', silentCfg).catch((): Config => ({
        configKey: '',
        configValue: '',
        configName: ''
      })),
      getConfigKey('sidebar_style', silentCfg).catch((): Config => ({
        configKey: '',
        configValue: '',
        configName: ''
      })),
      getConfigKey('custom_css', silentCfg).catch((): Config => ({
        configKey: '',
        configValue: '',
        configName: ''
      }))
    ])

    const settingsStore = useSettingsStore()

    // 应用主题颜色
    if (themeColor.configValue && themeColor.configValue !== '') {
      settingsStore.changeSetting({ key: 'theme', value: themeColor.configValue })
      handleThemeStyle(themeColor.configValue)
    }

    // 应用 Logo：自定义 site_logo 优先，回退站点头像 blog_avatar
    const effectiveLogo = customLogo.configValue || logoUrl.configValue
    if (effectiveLogo && effectiveLogo !== '') {
      applyLogo(effectiveLogo)
    }

    // 应用侧边栏样式
    if (sidebarStyle.configValue && sidebarStyle.configValue !== '') {
      applySidebarStyle(sidebarStyle.configValue)
    }

    // 应用自定义CSS
    if (customCss.configValue && customCss.configValue !== '') {
      applyCustomCss(customCss.configValue)
    }
  } catch {
    // 静默处理错误，避免影响页面功能
  }
}

/**
 * 应用Logo
 * @param logoUrl Logo URL
 */
function applyLogo(logoUrl: string): void {
  const normalized = normalizeUploadUrl(logoUrl)
  const logoElements = document.querySelectorAll('.sidebar-logo-container img, .navbar-brand img')
  logoElements.forEach((element: Element) => {
    const imgElement = element as HTMLImageElement
    imgElement.src = normalized
    imgElement.style.height = '32px' // 设置合适的高度
    imgElement.style.width = 'auto' // 自动宽度
  })
}

/**
 * 应用侧边栏样式
 * @param style 侧边栏样式 (dark/light)
 */
function applySidebarStyle(style: string): void {
  const sidebarElement = document.querySelector('.sidebar-container')
  if (sidebarElement) {
    if (style === 'dark') {
      sidebarElement.classList.add('theme-dark')
      sidebarElement.classList.remove('theme-light')
    } else {
      sidebarElement.classList.add('theme-light')
      sidebarElement.classList.remove('theme-dark')
    }

    const settingsStore = useSettingsStore()
    settingsStore.changeSetting({ key: 'sideTheme', value: `theme-${style}` })
  }
}

/**
 * 应用自定义CSS
 * @param css 自定义CSS内容
 */
function applyCustomCss(css: string): void {
  let styleElement = document.getElementById('site-custom-css')
  if (!styleElement) {
    styleElement = document.createElement('style')
    styleElement.id = 'site-custom-css'
    document.head.appendChild(styleElement)
  }
  styleElement.textContent = css
}
