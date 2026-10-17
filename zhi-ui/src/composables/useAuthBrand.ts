import { computed } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { normalizeUploadUrl } from '@/utils/uploadUrl'

/**
 * 认证页（登录 / 注册 / 找回密码）的品牌信息。
 *
 * 取值优先级：站点设置 `site_logo`（自定义 Logo）→ `blog_avatar`（站点头像）→ 站名首字。
 * 站名取 `blog_name`，未配置时回退默认名 —— 三页共用，避免各自硬编码品牌。
 */
export function useAuthBrand() {
  const siteSettingsStore = useSiteSettingsStore()
  const settings = computed(() => siteSettingsStore.siteSettings || ({} as never))

  const brandName = computed(() => {
    const name = String((settings.value as Record<string, unknown>).blog_name || '').trim()
    return name || '知企建站系统'
  })

  const brandLogo = computed(() => {
    const s = settings.value as Record<string, unknown>
    // 兼容历史绝对地址（http://zhi-admin:8080/profile/...）→ 站内相对路径
    return normalizeUploadUrl(s.site_logo || s.blog_avatar)
  })

  const brandInitial = computed(() => brandName.value.slice(0, 1))

  return { brandName, brandLogo, brandInitial }
}
