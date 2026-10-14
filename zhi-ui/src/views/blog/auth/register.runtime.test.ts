import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import Register from './Register.vue'
import BlogLayout from '@/components/BlogLayout.vue'

/**
 * 注册页行为测试（挂载真实组件）。
 *
 * 取代原来的源码字符串断言（检查源码里有没有 placeholder="请输入用户名" 之类文本），
 * 这里断言挂载后的真实行为：组件树、验证码请求与绑定、失败降级。
 */
const getCodeImg = vi.fn()

vi.mock('@/api/blog/auth', () => ({
  getCodeImg: (...args: unknown[]) => getCodeImg(...args),
  sendEmailCode: vi.fn(),
  register: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  useRoute: () => ({ query: {}, params: {}, path: '/register' })
}))

vi.mock('@/plugins/element-plus-service', () => ({
  ElMessage: Object.assign(vi.fn(), { success: vi.fn(), error: vi.fn(), warning: vi.fn() })
}))

function mountRegister() {
  return mount(Register, {
    global: {
      plugins: [createPinia()],
      stubs: {
        BlogLayout: { template: '<div><slot /></div>' },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<input />' },
        'el-button': { template: '<button><slot /></button>' },
        'el-icon': { template: '<span><slot /></span>' },
        'el-checkbox': { template: '<label><slot /></label>' },
        'el-link': { template: '<a><slot /></a>' }
      }
    }
  })
}

describe('注册页（行为）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getCodeImg.mockResolvedValue({ img: 'BBBB', uuid: 'uuid-register' })
  })

  it('渲染在 BlogLayout 内并展示注册表单', async () => {
    const wrapper = mountRegister()
    await flushPromises()

    expect(wrapper.findComponent(BlogLayout).exists()).toBe(true)
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('挂载时请求图形验证码并绑定到 img', async () => {
    const wrapper = mountRegister()
    await flushPromises()

    expect(getCodeImg).toHaveBeenCalled()
    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('data:image/jpeg;base64,BBBB')
  })

  it('验证码接口失败时页面仍可用', async () => {
    getCodeImg.mockRejectedValueOnce(new Error('500'))

    const wrapper = mountRegister()
    await expect(flushPromises()).resolves.not.toThrow()
    expect(wrapper.findComponent(BlogLayout).exists()).toBe(true)
  })
})
