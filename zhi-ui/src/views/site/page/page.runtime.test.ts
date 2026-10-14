import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import CustomPage from './index.vue'
import SiteLayout from '@/components/SiteLayout.vue'

/**
 * 自定义页面（/page/:slug）行为测试。
 *
 * 取代原来的源码字符串断言：原来只检查"源码里有没有 renderMarkdown / sanitizeArticleContent
 * 这两行 import"，无法证明渲染结果真的被消毒。这里挂载组件并断言 DOM。
 */
const getPublishedPageBySlug = vi.fn()

vi.mock('@/api/site/page', () => ({
  getPublishedPageBySlug: (...args: unknown[]) => getPublishedPageBySlug(...args)
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { slug: 'about-me' }, query: {}, path: '/page/about-me' }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() })
}))

vi.mock('@/utils/seo', () => ({
  applySeo: vi.fn(),
  canonicalUrl: vi.fn(() => 'https://example.com/page/about-me')
}))

function mountPage() {
  return mount(CustomPage, {
    global: {
      plugins: [createPinia()],
      stubs: {
        SiteLayout: { template: '<div><slot /></div>' },
        'el-skeleton': { template: '<div><slot /></div>' },
        'el-empty': { template: '<div>暂无内容</div>' },
        'el-icon': { template: '<span><slot /></span>' }
      }
    }
  })
}

describe('自定义页面（行为）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('按 slug 拉取内容并渲染在 SiteLayout 内', async () => {
    getPublishedPageBySlug.mockResolvedValue({
      code: 200,
      data: { id: 1, title: '关于我', slug: 'about-me', content: '## 我是谁', format: 'markdown' }
    })

    const wrapper = mountPage()
    await flushPromises()

    expect(getPublishedPageBySlug).toHaveBeenCalledWith('about-me')
    expect(wrapper.findComponent(SiteLayout).exists()).toBe(true)
    expect(wrapper.text()).toContain('关于我')
  })

  it('Markdown 内容被渲染成 HTML', async () => {
    getPublishedPageBySlug.mockResolvedValue({
      code: 200,
      data: { id: 1, title: '标题', slug: 'about-me', content: '# 一级标题', format: 'markdown' }
    })

    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.html()).toContain('<h1')
    expect(wrapper.text()).toContain('一级标题')
  })

  it('渲染结果必须经过消毒：脚本与事件属性不能进入 DOM', async () => {
    getPublishedPageBySlug.mockResolvedValue({
      code: 200,
      data: {
        id: 1,
        title: '标题',
        slug: 'about-me',
        content:
          '正常内容\n\n<script>window.__pwned = true</script><img src=x onerror="window.__pwned = true">',
        format: 'html'
      }
    })

    const wrapper = mountPage()
    await flushPromises()

    const html = wrapper.html()
    expect(html).toContain('正常内容')
    expect(html).not.toContain('<script')
    expect(html).not.toContain('onerror')
    expect((window as any).__pwned).toBeUndefined()
  })

  it('页面不存在/接口失败时不崩，走空状态', async () => {
    getPublishedPageBySlug.mockRejectedValueOnce(new Error('404'))

    const wrapper = mountPage()
    await expect(flushPromises()).resolves.not.toThrow()
    expect(wrapper.findComponent(SiteLayout).exists()).toBe(true)
  })
})
