import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBlogSettingsStore } from './blogSettings'

// Mock processAvatarUrl
vi.mock('@/api/blog/avatar', () => ({
  processAvatarUrl: vi.fn(url => url)
}))

// Mock listSetting
vi.mock('@/api/admin/blog/setting', () => ({
  listSetting: vi.fn()
}))

describe('BlogSettings Store 测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('应该有默认的博客设置', () => {
      const store = useBlogSettingsStore()
      expect(store.blogSettings.blog_name).toBe('企业官网项目')
      expect(store.blogSettings.blog_author).toBe('nevell')
      expect(store.blogSettings.comment_enabled).toBe(true)
      expect(store.blogSettings.sidebar_enabled).toBe(true)
    })

    it('lastUpdate 应该初始化为 0', () => {
      const store = useBlogSettingsStore()
      expect(store.lastUpdate).toBe(0)
    })
  })

  describe('getBlogAvatar getter', () => {
    it('当 avatar 为空时应该返回默认头像', () => {
      const store = useBlogSettingsStore()
      const avatar = store.getBlogAvatar
      expect(avatar).toContain('data:image/svg+xml')
      expect(avatar).toContain('%23409EFF')
    })

    it('当 avatar 不为空时应该返回处理后的 URL', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({ blog_avatar: '/avatar/test.jpg' })
      const avatar = store.getBlogAvatar
      expect(avatar).toContain('/avatar/test.jpg')
      expect(avatar).toContain('t=')
    })

    it('应该添加时间戳避免缓存', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({ blog_avatar: '/avatar/test.jpg' })
      const avatar = store.getBlogAvatar
      expect(avatar).toMatch(/\?t=\d+|&t=\d+/)
    })
  })

  describe('getBlogAuthor getter', () => {
    it('应该返回博客作者', () => {
      const store = useBlogSettingsStore()
      expect(store.getBlogAuthor).toBe('nevell')
    })

    it('当作者为空时应该返回默认值', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({ blog_author: '' })
      expect(store.getBlogAuthor).toBe('nevell')
    })
  })

  describe('getBlogName getter', () => {
    it('应该返回博客名称', () => {
      const store = useBlogSettingsStore()
      expect(store.getBlogName).toBe('企业官网项目')
    })

    it('当名称为空时应该返回默认值', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({ blog_name: '' })
      expect(store.getBlogName).toBe('企业官网项目')
    })
  })

  describe('isFeatureEnabled getter', () => {
    it('应该正确判断功能是否启用', () => {
      const store = useBlogSettingsStore()
      expect(store.isFeatureEnabled('comment_enabled')).toBe(true)
      expect(store.isFeatureEnabled('sidebar_enabled')).toBe(true)
    })

    it('当功能被禁用时应该返回 false', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({ comment_enabled: false })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
    })

    it('当功能设置为字符串 "false" 时应该返回 false', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({ comment_enabled: 'false' as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
    })

    it('字符串 "1"/"0" 与数字 1/0 的口径应与后端 BlogSwitchUtils 一致', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({ comment_enabled: '1' as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(true)
      store.updateBlogSettings({ comment_enabled: '0' as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
      store.updateBlogSettings({ comment_enabled: 0 as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
      store.updateBlogSettings({ comment_enabled: undefined as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(true)
    })
  })

  describe('updateBlogSettings action', () => {
    it('应该更新博客设置', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({ blog_name: '新博客' })
      expect(store.blogSettings.blog_name).toBe('新博客')
    })

    it('应该更新 lastUpdate 时间戳', () => {
      const store = useBlogSettingsStore()
      const before = store.lastUpdate
      store.updateBlogSettings({ blog_name: '新博客' })
      expect(store.lastUpdate).toBeGreaterThan(before)
    })

    it('应该合并多个设置', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({
        blog_name: '新博客',
        blog_author: '新作者',
        comment_enabled: false
      })
      expect(store.blogSettings.blog_name).toBe('新博客')
      expect(store.blogSettings.blog_author).toBe('新作者')
      expect(store.blogSettings.comment_enabled).toBe(false)
    })
  })

  describe('setBlogSettings action', () => {
    it('应该设置完整的博客设置', () => {
      const store = useBlogSettingsStore()
      const newSettings = {
        blog_name: '新博客',
        blog_desc: '新描述',
        blog_author: '新作者',
        blog_email: 'test@example.com',
        blog_avatar: null,
        blog_copyright: '版权',
        blog_beian: '备案',
        comment_enabled: false,
        comment_review: false,
        like_enabled: false,
        view_count_enabled: false,
        share_enabled: false,
        search_enabled: false,
        sidebar_enabled: false,
        footer_enabled: false,
        copyright_enabled: false,
        beian_enabled: false,
        friend_link_apply_enabled: false,
        guestbook_enabled: false,
        author_title: '标题',
        author_bio: '简介',
        github_url: 'https://github.com',
        weibo_url: 'https://weibo.com',
        wechat_qr: 'qr',
        author_location: '北京',
        personal_website: 'https://example.com'
      }
      store.setBlogSettings(newSettings)
      expect(store.blogSettings).toEqual(newSettings)
    })
  })

  describe('refreshFromAdmin action', () => {
    it('应该从后台刷新设置', async () => {
      const { listSetting } = await import('@/api/admin/blog/setting')
      vi.mocked(listSetting).mockResolvedValue({
        code: 200,
        rows: [
          { configKey: 'blog_name', configValue: '后台博客' },
          { configKey: 'blog_author', configValue: 'admin' }
        ]
      })

      const store = useBlogSettingsStore()
      const result = await store.refreshFromAdmin()

      expect(result).toBe(true)
      expect(store.blogSettings.blog_name).toBe('后台博客')
      expect(store.blogSettings.blog_author).toBe('admin')
    })

    it('当后台返回失败时应该返回 false', async () => {
      const { listSetting } = await import('@/api/admin/blog/setting')
      vi.mocked(listSetting).mockResolvedValue({
        code: 500,
        rows: []
      })

      const store = useBlogSettingsStore()
      const result = await store.refreshFromAdmin()

      expect(result).toBe(false)
    })

    it('当发生错误时应该返回 false', async () => {
      const { listSetting } = await import('@/api/admin/blog/setting')
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      vi.mocked(listSetting).mockRejectedValue(new Error('Network error'))

      const store = useBlogSettingsStore()
      const result = await store.refreshFromAdmin()

      expect(result).toBe(false)
      consoleErrorSpy.mockRestore()
    })
  })

  describe('功能设置', () => {
    it('应该支持所有功能开关', () => {
      const store = useBlogSettingsStore()
      const features = [
        'comment_enabled',
        'comment_review',
        'like_enabled',
        'view_count_enabled',
        'share_enabled',
        'search_enabled',
        'sidebar_enabled',
        'footer_enabled',
        'copyright_enabled',
        'beian_enabled',
        'friend_link_apply_enabled',
        'guestbook_enabled'
      ]

      features.forEach(feature => {
        expect(store.blogSettings).toHaveProperty(feature)
      })
    })

    it('应该能够切换所有功能', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({
        comment_enabled: false,
        like_enabled: false,
        sidebar_enabled: false
      })

      expect(store.blogSettings.comment_enabled).toBe(false)
      expect(store.blogSettings.like_enabled).toBe(false)
      expect(store.blogSettings.sidebar_enabled).toBe(false)
    })
  })

  describe('博主信息', () => {
    it('应该包含博主信息字段', () => {
      const store = useBlogSettingsStore()
      expect(store.blogSettings.author_title).toBeDefined()
      expect(store.blogSettings.author_bio).toBeDefined()
      expect(store.blogSettings.github_url).toBeDefined()
      expect(store.blogSettings.weibo_url).toBeDefined()
      expect(store.blogSettings.wechat_qr).toBeDefined()
      expect(store.blogSettings.author_location).toBeDefined()
      expect(store.blogSettings.personal_website).toBeDefined()
    })

    it('应该能够更新博主信息', () => {
      const store = useBlogSettingsStore()
      store.updateBlogSettings({
        author_title: '全栈工程师',
        github_url: 'https://github.com/test'
      })

      expect(store.blogSettings.author_title).toBe('全栈工程师')
      expect(store.blogSettings.github_url).toBe('https://github.com/test')
    })
  })

  it('isFeatureEnabled 与后端口径一致（大小写、首尾空白、缺失都按开启处理）', () => {
    const store = useBlogSettingsStore()
    store.setBlogSettings({
      comment_enabled: 'FALSE',
      like_enabled: ' 0 ',
      search_enabled: undefined
    } as any)

    expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
    expect(store.isFeatureEnabled('like_enabled')).toBe(false)
    expect(store.isFeatureEnabled('search_enabled')).toBe(true)
    expect(store.isFeatureEnabled('nothing_configured')).toBe(true)
  })
})
