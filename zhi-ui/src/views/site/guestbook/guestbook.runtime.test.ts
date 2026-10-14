import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import Guestbook from './index.vue'
import SiteLayout from '@/components/SiteLayout.vue'

/**
 * 留言板页面行为测试（挂载真实组件）。
 *
 * 取代原来的"读 index.vue 源码再 toContain 字符串"写法：那种断言在重构模板、
 * 改类名或换实现时不会失效，也发现不了运行时问题。
 */
const getMessageList = vi.fn()
const getMessageCount = vi.fn()
const addMessage = vi.fn()
const getSiteSettingsAnonymous = vi.fn()
const getCodeImg = vi.fn()

vi.mock('@/api/site/message', () => ({
  getMessageList: (...args: unknown[]) => getMessageList(...args),
  getMessageCount: (...args: unknown[]) => getMessageCount(...args),
  addMessage: (...args: unknown[]) => addMessage(...args)
}))

vi.mock('@/api/site/setting', () => ({
  getSiteSettingsAnonymous: (...args: unknown[]) => getSiteSettingsAnonymous(...args)
}))

vi.mock('@/api/site/page', () => ({ getPageList: vi.fn().mockResolvedValue({ data: [] }) }))

vi.mock('@/api/site/auth', () => ({
  getCodeImg: (...args: unknown[]) => getCodeImg(...args)
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/guestbook')
}))

function mountGuestbook() {
  return mount(Guestbook, {
    global: {
      plugins: [createPinia()],
      stubs: {
        SiteLayout: { template: '<div class="layout-stub"><slot /></div>' },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<input />' },
        'el-button': { template: '<button><slot /></button>' },
        'el-avatar': { template: '<span><slot /></span>' },
        'el-icon': { template: '<span><slot /></span>' },
        'el-pagination': { template: '<div />' },
        'el-empty': { props: ['description'], template: '<div>{{ description }}</div>' },
        'el-tag': { template: '<span><slot /></span>' }
      }
    }
  })
}

describe('留言板页面（行为）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 默认不带开关数据（store 初始 guestbook_enabled=false：默认关闭口径，缺失即关闭）
    getSiteSettingsAnonymous.mockResolvedValue({ data: { comment_enabled: 'true' } })
    getCodeImg.mockResolvedValue({ img: '', uuid: 'uuid-1' })
    getMessageList.mockResolvedValue({
      rows: [
        {
          id: 1,
          nickname: '测试访客',
          content: '这条留言来自测试',
          createTime: '2026-01-01 10:00:00'
        }
      ],
      total: 1
    })
    getMessageCount.mockResolvedValue({ data: 1 })
    addMessage.mockResolvedValue({ code: 200 })
  })

  it('挂载后渲染在 SiteLayout 里，并展示留言板标题', async () => {
    const wrapper = mountGuestbook()
    await flushPromises()

    expect(wrapper.findComponent(SiteLayout).exists()).toBe(true)
    expect(wrapper.text()).toContain('留言板')
  })

  it('开关开启时挂载拉取留言列表并把内容渲染出来', async () => {
    getSiteSettingsAnonymous.mockResolvedValue({ data: { guestbook_enabled: 'true' } })
    const wrapper = mountGuestbook()
    await flushPromises()

    expect(getMessageList).toHaveBeenCalled()
    expect(wrapper.text()).toContain('这条留言来自测试')
    expect(wrapper.text()).toContain('测试访客')
  })

  it('接口失败时不抛出未捕获异常（页面仍可用）', async () => {
    getSiteSettingsAnonymous.mockResolvedValue({ data: { guestbook_enabled: 'true' } })
    getMessageList.mockRejectedValueOnce(new Error('500'))
    const wrapper = mountGuestbook()

    await expect(flushPromises()).resolves.not.toThrow()
    expect(wrapper.text()).toContain('留言板')
  })

  it('开关关闭时隐藏表单与列表，且不请求留言数据', async () => {
    getSiteSettingsAnonymous.mockResolvedValue({ data: { guestbook_enabled: 'false' } })

    const wrapper = mountGuestbook()
    await flushPromises()

    expect(wrapper.text()).toContain('本站暂未开放留言板')
    expect(wrapper.find('form').exists()).toBe(false)
    expect(getMessageList).not.toHaveBeenCalled()
    expect(getMessageCount).not.toHaveBeenCalled()
  })

  it('开关开启时渲染留言表单并拉取列表', async () => {
    getSiteSettingsAnonymous.mockResolvedValue({ data: { guestbook_enabled: 'true' } })

    const wrapper = mountGuestbook()
    await flushPromises()

    expect(wrapper.find('form').exists()).toBe(true)
    expect(getMessageList).toHaveBeenCalled()
  })

  it('开关缺失时按关闭处理（默认关闭口径）', async () => {
    const wrapper = mountGuestbook()
    await flushPromises()

    expect(wrapper.text()).toContain('本站暂未开放留言板')
    expect(wrapper.find('form').exists()).toBe(false)
    expect(getMessageList).not.toHaveBeenCalled()
  })
})
