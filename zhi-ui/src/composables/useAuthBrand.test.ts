import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthBrand } from './useAuthBrand'
import { useSiteSettingsStore } from '@/stores/siteSettings'

/**
 * 认证页品牌信息取值优先级（登录/注册/找回密码三页共用）：
 * site_logo（自定义 Logo）→ blog_avatar（站点头像）→ 站名首字；
 * 站名未配置时回退默认名。
 */
describe('useAuthBrand 认证页品牌信息', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('站名取 blog_name', () => {
    const store = useSiteSettingsStore()
    store.updateSiteSettings({ blog_name: '知企科技' } as never)

    const { brandName } = useAuthBrand()
    expect(brandName.value).toBe('知企科技')
  })

  it('站名为空时回退默认站名', () => {
    const store = useSiteSettingsStore()
    store.updateSiteSettings({ blog_name: '   ' } as never)

    const { brandName, brandInitial } = useAuthBrand()
    expect(brandName.value).toBe('知企建站系统')
    expect(brandInitial.value).toBe('知')
  })

  it('自定义 site_logo 优先于站点头像 blog_avatar', () => {
    const store = useSiteSettingsStore()
    store.updateSiteSettings({
      site_logo: '/profile/upload/logo.png',
      blog_avatar: '/profile/upload/avatar.png'
    } as never)

    expect(useAuthBrand().brandLogo.value).toBe('/profile/upload/logo.png')
  })

  it('未传 site_logo 时回退 blog_avatar', () => {
    const store = useSiteSettingsStore()
    store.updateSiteSettings({
      site_logo: '',
      blog_avatar: '/profile/upload/avatar.png'
    } as never)

    expect(useAuthBrand().brandLogo.value).toBe('/profile/upload/avatar.png')
  })

  it('两者都为空时不输出图片地址（页面回退站名首字）', () => {
    const store = useSiteSettingsStore()
    store.updateSiteSettings({ site_logo: '', blog_avatar: '' } as never)

    expect(useAuthBrand().brandLogo.value).toBe('')
  })
})
