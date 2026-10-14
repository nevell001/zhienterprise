import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import FriendLinkApply from './apply.vue'
import SiteLayout from '@/components/SiteLayout.vue'

/**
 * 友链申请页行为测试（挂载真实组件）。
 *
 * 取代原来的源码字符串断言：原来断言源码里有没有 `prop="url"`、`captchaUrl` 之类文本，
 * 无法验证验证码是否真的加载、失败路径是否安全。
 */
const applyFriendLink = vi.fn()
const getCodeImg = vi.fn()

vi.mock('@/api/site/friendLink', () => ({
  applyFriendLink: (...args: unknown[]) => applyFriendLink(...args)
}))

vi.mock('@/api/site/setting', () => ({
  getSiteSettingsAnonymous: vi.fn().mockResolvedValue({
    data: { friend_link_apply_enabled: 'true', friend_link_apply_notice: '请先加友链' }
  })
}))

vi.mock('@/api/site/auth', () => ({
  getCodeImg: (...args: unknown[]) => getCodeImg(...args)
}))

function mountPage() {
  return mount(FriendLinkApply, {
    global: {
      plugins: [createPinia()],
      stubs: {
        SiteLayout: { template: '<div><slot /></div>' },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<input />' },
        'el-button': { template: '<button><slot /></button>' },
        'el-icon': { template: '<span><slot /></span>' },
        'el-alert': { template: '<div><slot /></div>' }
      }
    }
  })
}

describe('友链申请页（行为）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getCodeImg.mockResolvedValue({ img: 'AAAA', uuid: 'uuid-friend' })
    applyFriendLink.mockResolvedValue({ code: 200 })
  })

  it('渲染在 SiteLayout 内并展示表单', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findComponent(SiteLayout).exists()).toBe(true)
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('挂载时请求验证码，并把接口返回的图片地址绑到 img 上', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(getCodeImg).toHaveBeenCalled()
    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    // 接口返回裸 base64，页面负责补 data URI 前缀（真实契约）
    expect(img.attributes('src')).toBe('data:image/jpeg;base64,AAAA')
  })

  it('验证码接口失败时页面仍可用（不抛未捕获异常）', async () => {
    getCodeImg.mockRejectedValueOnce(new Error('500'))

    const wrapper = mountPage()
    await expect(flushPromises()).resolves.not.toThrow()
    expect(wrapper.findComponent(SiteLayout).exists()).toBe(true)
  })
})
