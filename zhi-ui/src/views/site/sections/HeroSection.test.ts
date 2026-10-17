import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import HeroSection from './HeroSection.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'

/**
 * Hero 视觉区的 Banner 行为：
 * 站点设置里上传了 `site_banner` 就渲染自定义大图（替换模板母题插画），
 * 未配置时回退母题插画 —— 后台「站点信息 → 首页 Banner」的消费端契约。
 */
function mountWith(settings: Record<string, unknown>) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useSiteSettingsStore()
  store.updateSiteSettings(settings as never)
  return mount(HeroSection, { global: { plugins: [pinia] } })
}

describe('HeroSection 自定义 Banner', () => {
  it('配置 site_banner 时渲染自定义 Banner 图片', () => {
    const banner = 'data:image/svg+xml;base64,PHN2Zy8+'
    const wrapper = mountWith({ site_banner: banner })

    const img = wrapper.find('.hero-banner img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe(banner)
    // 有 Banner 时不再渲染母题插画
    expect(wrapper.find('.hero-viz svg').exists()).toBe(false)
  })

  it('未配置 site_banner 时回退模板母题插画', () => {
    const wrapper = mountWith({ site_banner: '' })

    expect(wrapper.find('.hero-banner').exists()).toBe(false)
    expect(wrapper.find('.hero-viz svg').exists()).toBe(true)
  })

  it('只有空白字符的 Banner 也应视为未配置', () => {
    const wrapper = mountWith({ site_banner: '   ' })

    expect(wrapper.find('.hero-banner').exists()).toBe(false)
    expect(wrapper.find('.hero-viz svg').exists()).toBe(true)
  })
})
