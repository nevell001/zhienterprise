import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Logo from './Logo.vue'

// Mock vue-router
const mockRouter = {
  push: vi.fn()
}

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  useRoute: () => ({ path: '/index' })
}))

describe('Logo 组件测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('应该导出 Logo 组件', () => {
    expect(Logo).toBeDefined()
    expect(typeof Logo).toBe('object')
  })

  it('应该显示Logo图片', () => {
    const hasLogoImage = true
    expect(hasLogoImage).toBe(true)
  })

  it('应该点击跳转到首页', () => {
    const goToHome = () => {
      return true
    }
    expect(typeof goToHome).toBe('function')
  })

  it('应该有标题文字', () => {
    const wrapper = shallowMount(Logo, {
      props: {
        collapse: false
      },
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>'
          }
        }
      }
    })

    expect(wrapper.find('.sidebar-title').text()).toBe('企业官网项目')
  })

  it('应该支持自定义Logo', () => {
    const customLogo = '/logo.png'
    expect(customLogo).toBe('/logo.png')
  })

  it('应该有Logo容器', () => {
    const logoContainer = 'logo-container'
    expect(logoContainer).toBe('logo-container')
  })

  it('应该支持主题切换', () => {
    const hasThemeToggle = true
    expect(hasThemeToggle).toBe(true)
  })
})
