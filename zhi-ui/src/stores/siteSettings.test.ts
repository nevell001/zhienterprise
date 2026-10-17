import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSiteSettingsStore } from './siteSettings'

// Mock processAvatarUrl
vi.mock('@/api/site/avatar', () => ({
  processAvatarUrl: vi.fn(url => url)
}))

// Mock listSetting
vi.mock('@/api/admin/setting', () => ({
  listSetting: vi.fn()
}))

describe('SiteSettings Store 测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('应该有默认的站点设置', () => {
      const store = useSiteSettingsStore()
      expect(store.siteSettings.blog_name).toBe('知企建站系统')
      // 企业官网定位：评论默认关闭（与 SQL 种子一致）
      expect(store.siteSettings.comment_enabled).toBe(false)
    })

    it('lastUpdate 应该初始化为 0', () => {
      const store = useSiteSettingsStore()
      expect(store.lastUpdate).toBe(0)
    })
  })

  describe('getSiteAvatar getter', () => {
    it('当 avatar 为空时应该返回默认头像', () => {
      const store = useSiteSettingsStore()
      const avatar = store.getSiteAvatar
      expect(avatar).toContain('data:image/svg+xml')
      expect(avatar).toContain('%23409EFF')
    })

    it('当 avatar 不为空时应该返回处理后的 URL', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({ blog_avatar: '/avatar/test.jpg' })
      const avatar = store.getSiteAvatar
      expect(avatar).toContain('/avatar/test.jpg')
      expect(avatar).toContain('t=')
    })

    it('应该添加时间戳避免缓存', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({ blog_avatar: '/avatar/test.jpg' })
      const avatar = store.getSiteAvatar
      expect(avatar).toMatch(/\?t=\d+|&t=\d+/)
    })
  })

  describe('getSiteName getter', () => {
    it('应该返回博客名称', () => {
      const store = useSiteSettingsStore()
      expect(store.getSiteName).toBe('知企建站系统')
    })

    it('当名称为空时应该返回默认值', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({ blog_name: '' })
      expect(store.getSiteName).toBe('知企建站系统')
    })
  })

  describe('isFeatureEnabled getter', () => {
    it('应该正确判断功能是否启用', () => {
      const store = useSiteSettingsStore()
      // 评论默认关闭（企业官网定位），搜索默认开启
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
      expect(store.isFeatureEnabled('search_enabled')).toBe(true)
    })

    it('当功能被禁用时应该返回 false', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({ comment_enabled: false })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
    })

    it('当功能设置为字符串 "false" 时应该返回 false', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({ comment_enabled: 'false' as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
    })

    it('字符串 "1"/"0" 与数字 1/0 的口径应与后端 SiteSwitchUtils 一致', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({ comment_enabled: '1' as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(true)
      store.updateSiteSettings({ comment_enabled: '0' as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
      store.updateSiteSettings({ comment_enabled: 0 as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
      store.updateSiteSettings({ comment_enabled: undefined as any })
      expect(store.isFeatureEnabled('comment_enabled')).toBe(true)
    })
  })

  describe('updateSiteSettings action', () => {
    it('应该更新博客设置', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({ blog_name: '新博客' })
      expect(store.siteSettings.blog_name).toBe('新博客')
    })

    it('应该更新 lastUpdate 时间戳', () => {
      const store = useSiteSettingsStore()
      const before = store.lastUpdate
      store.updateSiteSettings({ blog_name: '新博客' })
      expect(store.lastUpdate).toBeGreaterThan(before)
    })

    it('应该合并多个设置', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({
        blog_name: '新博客',
        blog_author: '新作者',
        comment_enabled: false
      })
      expect(store.siteSettings.blog_name).toBe('新博客')
      expect(store.siteSettings.blog_author).toBe('新作者')
      expect(store.siteSettings.comment_enabled).toBe(false)
    })
  })

  describe('setSiteSettings action', () => {
    it('应该设置完整的站点设置', () => {
      const store = useSiteSettingsStore()
      const newSettings = {
        blog_name: '新站点',
        blog_desc: '新描述',
        blog_email: 'test@example.com',
        blog_avatar: null,
        blog_copyright: '版权',
        blog_beian: '备案',
        comment_enabled: false,
        comment_review: false,
        view_count_enabled: false,
        search_enabled: false,
        footer_enabled: false,
        copyright_enabled: false,
        beian_enabled: false,
        friend_link_apply_enabled: false,
        guestbook_enabled: false
      }
      store.setSiteSettings(newSettings)
      expect(store.siteSettings).toEqual(newSettings)
    })
  })

  describe('refreshFromAdmin action', () => {
    it('应该从后台刷新设置', async () => {
      const { listSetting } = await import('@/api/admin/setting')
      vi.mocked(listSetting).mockResolvedValue({
        code: 200,
        rows: [
          { configKey: 'blog_name', configValue: '后台博客' },
          { configKey: 'blog_author', configValue: 'admin' }
        ]
      })

      const store = useSiteSettingsStore()
      const result = await store.refreshFromAdmin()

      expect(result).toBe(true)
      expect(store.siteSettings.blog_name).toBe('后台博客')
      expect(store.siteSettings.blog_author).toBe('admin')
    })

    it('当后台返回失败时应该返回 false', async () => {
      const { listSetting } = await import('@/api/admin/setting')
      vi.mocked(listSetting).mockResolvedValue({
        code: 500,
        rows: []
      })

      const store = useSiteSettingsStore()
      const result = await store.refreshFromAdmin()

      expect(result).toBe(false)
    })

    it('当发生错误时应该返回 false', async () => {
      const { listSetting } = await import('@/api/admin/setting')
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      vi.mocked(listSetting).mockRejectedValue(new Error('Network error'))

      const store = useSiteSettingsStore()
      const result = await store.refreshFromAdmin()

      expect(result).toBe(false)
      consoleErrorSpy.mockRestore()
    })
  })

  describe('功能设置', () => {
    it('应该支持所有功能开关', () => {
      const store = useSiteSettingsStore()
      const features = [
        'comment_enabled',
        'comment_review',
        'view_count_enabled',
        'search_enabled',
        'footer_enabled',
        'copyright_enabled',
        'beian_enabled',
        'friend_link_apply_enabled',
        'guestbook_enabled'
      ]

      features.forEach(feature => {
        expect(store.siteSettings).toHaveProperty(feature)
      })
    })

    it('应该能够切换所有功能', () => {
      const store = useSiteSettingsStore()
      store.updateSiteSettings({
        comment_enabled: false,
        search_enabled: false
      })

      expect(store.siteSettings.comment_enabled).toBe(false)
      expect(store.siteSettings.search_enabled).toBe(false)
    })
  })

  describe('站长信息已下线', () => {
    // 「站点设置 → 个人信息」tab 已随关于页「负责人」区块一起移除，
    // 这些键不再有录入入口与消费方，锁住它们不会悄悄回来。
    it('store 不应再携带已下线的站长/社交字段', () => {
      const store = useSiteSettingsStore()
      for (const key of [
        'author_title',
        'author_bio',
        'author_location',
        'personal_website',
        'github_url',
        'weibo_url'
      ]) {
        expect(store.siteSettings[key as keyof typeof store.siteSettings]).toBeUndefined()
      }
    })

    it('blog_author（站长）也已移除：页脚版权署名改用站点名称', () => {
      const store = useSiteSettingsStore()
      expect(store.siteSettings.blog_author).toBeUndefined()
      // 署名的新取值来源必须仍在
      expect(store.siteSettings.blog_name).toBeDefined()
    })
  })

  it('isFeatureEnabled 与后端口径一致（大小写、首尾空白、缺失都按开启处理）', () => {
    const store = useSiteSettingsStore()
    store.setSiteSettings({
      comment_enabled: 'FALSE',
      view_count_enabled: ' 0 ',
      search_enabled: undefined
    } as any)

    expect(store.isFeatureEnabled('comment_enabled')).toBe(false)
    expect(store.isFeatureEnabled('view_count_enabled')).toBe(false)
    expect(store.isFeatureEnabled('search_enabled')).toBe(true)
    expect(store.isFeatureEnabled('nothing_configured')).toBe(true)
  })
})
