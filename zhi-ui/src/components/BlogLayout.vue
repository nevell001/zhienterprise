<template>
  <div class="blog-layout">
    <!-- 统一顶部导航 -->
    <nav class="blog-top-nav">
      <div class="nav-inner">
        <router-link to="/blog" class="nav-left">
          <div class="brand-logo">
            {{ blogSettings.blog_name?.charAt(0) || '知' }}
          </div>
          <span class="brand-name">{{ blogSettings.blog_name || '知企建站系统' }}</span>
        </router-link>
        <div class="nav-center">
          <router-link v-for="menu in menus" :key="menu.path" :to="menu.path" class="nav-link">
            {{ menu.name }}
          </router-link>
        </div>
        <div class="nav-right">
          <template v-if="userStore.token && userStore.name">
            <!-- 站内通知铃铛 -->
            <el-popover
              placement="bottom-end"
              :width="340"
              trigger="click"
              :teleported="false"
              @show="handleNotifShow"
            >
              <template #reference>
                <div class="notif-trigger">
                  <el-badge :value="unreadCount" :hidden="unreadCount <= 0" :max="99">
                    <el-icon :size="19"><Bell /></el-icon>
                  </el-badge>
                </div>
              </template>
              <div class="notif-panel">
                <div class="notif-panel-head">
                  <span class="notif-panel-title">通知</span>
                  <el-button
                    v-if="notifications.length > 0"
                    link
                    type="primary"
                    size="small"
                    @click="markAllNotifications"
                  >
                    全部已读
                  </el-button>
                </div>
                <div v-if="notificationLoading" class="notif-empty">加载中…</div>
                <div v-else-if="notifications.length === 0" class="notif-empty">暂无新通知</div>
                <div v-else class="notif-list">
                  <div
                    v-for="n in notifications"
                    :key="n.id"
                    class="notif-item"
                    :class="{ 'is-unread': n.isRead !== 1 }"
                    @click="handleNotifClick(n)"
                  >
                    <div class="notif-item-title">
                      <span v-if="n.isRead !== 1" class="notif-dot"></span>
                      {{ n.title || notifTypeText(n.type) }}
                    </div>
                    <div class="notif-item-content">{{ n.content }}</div>
                    <div class="notif-item-time">{{ formatNotifTime(n.createTime) }}</div>
                  </div>
                </div>
                <div class="notif-panel-foot">
                  <router-link to="/user/profile" class="notif-more">查看全部通知</router-link>
                </div>
              </div>
            </el-popover>

            <el-dropdown trigger="click" @command="handleUserCommand">
              <div class="user-info">
                <div class="user-avatar-wrapper">
                  <img v-if="userStore.avatar" :src="userStore.avatar" class="user-avatar" />
                  <div v-else class="avatar-placeholder">
                    <el-icon :size="16"><UserFilled /></el-icon>
                  </div>
                </div>
                <span class="username">{{ userStore.name }}</span>
                <el-icon class="dropdown-icon">
                  <ArrowDown />
                </el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="userStore.userType === '00'" @click="goToAdmin">
                    <el-icon><Setting /></el-icon>
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="bookmark">
                    <el-icon><CollectionTag /></el-icon>
                    我的收藏
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else size="small" type="primary" @click="$router.push('/login')">
            登录
          </el-button>
          <button class="theme-btn" :title="themeModeTooltip" @click="toggleTheme">
            <el-icon :size="18">
              <Monitor v-if="themeMode === 'system'" />
              <Sunny v-else-if="isDark" />
              <Moon v-else />
            </el-icon>
          </button>
          <button
            class="hamburger-btn"
            aria-label="展开菜单"
            :class="{ 'is-open': mobileMenuOpen }"
            @click="mobileMenuOpen = !mobileMenuOpen"
          >
            <el-icon :size="20"><Menu /></el-icon>
          </button>
        </div>
      </div>
      <!-- 移动端下拉菜单 -->
      <div v-show="mobileMenuOpen" class="mobile-nav-menu">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="mobile-nav-link"
          @click="mobileMenuOpen = false"
        >
          {{ m.name }}
        </router-link>
        <template v-if="userStore.token && userStore.name">
          <router-link to="/blog/bookmarks" class="mobile-nav-link" @click="mobileMenuOpen = false">
            我的收藏
          </router-link>
          <router-link to="/user/profile" class="mobile-nav-link" @click="mobileMenuOpen = false">
            个人中心
          </router-link>
        </template>
        <router-link v-else to="/login" class="mobile-nav-link" @click="mobileMenuOpen = false">
          登录
        </router-link>
      </div>
    </nav>

    <!-- 子页面内容 -->
    <slot></slot>

    <!-- 统一底部 -->
    <footer v-if="isFooterEnabled" class="blog-site-footer">
      <div class="footer-wave">
        <svg viewBox="0 0 1440 60" preserveAspectRatio="none">
          <path
            fill="currentColor"
            d="M0,30L80,25C160,20,320,15,480,20C640,25,800,35,960,30C1120,25,1280,15,1360,10L1440,5L1440,60L1360,60C1280,60,1120,60,960,60C800,60,640,60,480,60C320,60,160,60,80,60L0,60Z"
          />
        </svg>
      </div>
      <div class="footer-inner">
        <div class="footer-brand">
          <div class="footer-logo">
            {{ blogSettings.blog_name?.charAt(0) || '知' }}
          </div>
          <div class="brand-text">
            <span class="brand-name">{{ blogSettings.blog_name || '知企建站系统' }}</span>
            <p class="brand-desc">
              {{ blogSettings.blog_desc || '分享技术与生活的点滴' }}
            </p>
          </div>
        </div>
        <div class="footer-links">
          <div class="footer-col">
            <h4>快速导航</h4>
            <router-link to="/blog">首页</router-link>
            <router-link v-if="isCustomHomePage" to="/blog/articles">文章</router-link>
            <router-link v-if="isCategoryNavEnabled" to="/blog/category">分类</router-link>
            <router-link v-if="isTagNavEnabled" to="/blog/tag">标签</router-link>
            <router-link v-if="isArchiveNavEnabled" to="/blog/archive">归档</router-link>
            <router-link to="/blog/about">关于</router-link>
            <router-link v-if="isFriendLinkApplyEnabled" to="/blog/friend-links/apply">
              友链申请
            </router-link>
          </div>
          <div class="footer-col">
            <h4>社交平台</h4>
            <a
              v-if="blogSettings.github_url"
              :href="formatUrl(blogSettings.github_url)"
              target="_blank"
            >
              GitHub
            </a>
            <a
              v-if="blogSettings.weibo_url"
              :href="formatUrl(blogSettings.weibo_url)"
              target="_blank"
            >
              微博
            </a>
            <a :href="rssUrl" target="_blank">RSS订阅</a>
          </div>
          <div v-if="friendLinks.length > 0 && isFriendLinkEnabled" class="footer-col">
            <h4>友情链接</h4>
            <a
              v-for="link in friendLinks"
              :key="link.id"
              :href="formatUrl(link.url)"
              target="_blank"
              rel="noopener noreferrer"
              :title="link.description || link.name"
            >
              {{ link.name }}
            </a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p v-if="isCopyrightEnabled">
          © {{ currentYear }}
          {{ blogSettings.blog_author || blogSettings.blog_name || '知企建站系统' }} · 保留所有权利
        </p>
        <p v-if="isBeianEnabled && blogSettings.blog_beian" class="beian-info">
          <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">
            {{ blogSettings.blog_beian }}
          </a>
        </p>
        <p class="tech-info">Powered by 知企建站系统 & Element Plus</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-service'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import {
  Setting,
  UserFilled,
  ArrowDown,
  User,
  CollectionTag,
  SwitchButton,
  Monitor,
  Sunny,
  Moon,
  Bell,
  Menu
} from '@element-plus/icons-vue'
import { getFrontFriendLinkList } from '@/api/blog/friendLink'
import { getBlogSettingsAnonymous } from '@/api/blog/setting'
import { getPublishedPages, type BlogPage } from '@/api/blog/page'
import {
  getNotificationList,
  getUnreadCount,
  markAllAsRead,
  markAsRead,
  type BlogNotification
} from '@/api/blog/notification'
import { getApiBaseUrl } from '@/utils/index'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { buildNavMenus, hasCustomHomePage } from '@/utils/blogNav'
import { removeJsonLd, setJsonLd } from '@/utils/jsonld'

const router = useRouter()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const blogSettingsStore = useBlogSettingsStore()

const blogSettings = computed(() => blogSettingsStore.blogSettings)
const isDark = computed(() => settingsStore.isDark)
const themeMode = computed(() => settingsStore.themeMode)

// 主题模式提示文本
const themeModeTooltip = computed(() => {
  switch (settingsStore.themeMode) {
    case 'system':
      return '跟随系统 (点击切换)'
    case 'dark':
      return '深色模式 (点击切换)'
    case 'light':
      return '浅色模式 (点击切换)'
    default:
      return '主题模式'
  }
})

interface FriendLink {
  id: number
  name: string
  url: string
  logo?: string
  description?: string
}
const friendLinks = ref<FriendLink[]>([])

// 移动端菜单展开状态
const mobileMenuOpen = ref(false)

// 站内通知
const unreadCount = ref(0)
const notifications = ref<BlogNotification[]>([])
const notificationLoading = ref(false)
let notifTimer: ReturnType<typeof setInterval> | null = null

const notifTypeText = (type?: string) => {
  switch (type) {
    case 'comment':
      return '收到新评论'
    case 'reply':
      return '收到回复'
    case 'audit':
      return '评论已通过审核'
    case 'reject':
      return '评论未通过审核'
    default:
      return '新通知'
  }
}

const formatNotifTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return time
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const fetchUnreadCount = async () => {
  if (!userStore.token) {
    unreadCount.value = 0
    return
  }
  try {
    const response = await getUnreadCount()
    const count = Number((response && (response as any).data) ?? 0)
    unreadCount.value = Number.isFinite(count) && count > 0 ? count : 0
  } catch {
    unreadCount.value = 0
  }
}

const fetchNotifications = async () => {
  if (!userStore.token) {
    notifications.value = []
    return
  }
  notificationLoading.value = true
  try {
    const response = await getNotificationList({ pageNum: 1, pageSize: 10 })
    const rows = (response && (response as any).rows) || []
    notifications.value = Array.isArray(rows) ? rows : []
  } catch {
    // 静默失败：保持空列表
    notifications.value = []
  } finally {
    notificationLoading.value = false
  }
}

const handleNotifShow = () => {
  fetchNotifications()
  fetchUnreadCount()
}

const handleNotifClick = async (n: BlogNotification) => {
  if (n.isRead !== 1) {
    try {
      await markAsRead([n.id])
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // 标记失败不阻断跳转
    }
  }
  if (n.articleId) {
    router.push(`/blog/article/${n.articleId}`)
  }
}

const markAllNotifications = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => {
      n.isRead = 1
    })
    unreadCount.value = 0
  } catch {
    // 忽略
  }
}

// 页脚/版权/友链相关开关统一走 store 判定（store.isFeatureEnabled 为全站唯一口径），
// 避免多处判定规则不一致导致“开关不生效”
const isFriendLinkEnabled = computed(() =>
  blogSettingsStore.isFeatureEnabled('friend_link_enabled')
)
const isFriendLinkApplyEnabled = computed(() =>
  blogSettingsStore.isFeatureEnabled('friend_link_apply_enabled')
)
const isFooterEnabled = computed(() => blogSettingsStore.isFeatureEnabled('footer_enabled'))
const isCopyrightEnabled = computed(() => blogSettingsStore.isFeatureEnabled('copyright_enabled'))
const isBeianEnabled = computed(() => blogSettingsStore.isFeatureEnabled('beian_enabled'))
const isGuestbookEnabled = computed(() => blogSettingsStore.isFeatureEnabled('guestbook_enabled'))

// 企业官网模式：home_page_slug 非空时首页被自定义页面接管，文章列表迁到 /blog/articles，
// 导航补「文章」入口；分类/标签/归档三个入口由下方开关控制（裁剪导航用）
const isCustomHomePage = computed(() =>
  hasCustomHomePage(blogSettingsStore.blogSettings.home_page_slug)
)
const isCategoryNavEnabled = computed(() =>
  blogSettingsStore.isFeatureEnabled('nav_category_enabled')
)
const isTagNavEnabled = computed(() => blogSettingsStore.isFeatureEnabled('nav_tag_enabled'))
const isArchiveNavEnabled = computed(() =>
  blogSettingsStore.isFeatureEnabled('nav_archive_enabled')
)
// 企业官网模板模式（home_layout=business）：导航补「核心业务」入口，名称可配
const isBusinessNavEnabled = computed(
  () => String(blogSettingsStore.blogSettings.home_layout || '') === 'business'
)
const businessNavLabel = computed(
  () => String(blogSettingsStore.blogSettings.business_label || '').trim() || '核心业务'
)

const customPages = ref<BlogPage[]>([])

// 菜单组装规则在 utils/blogNav.ts（纯函数、与站点前台尺度解耦，便于单测）。
// 「关于」不设固定入口：它就是页面管理里 slug=about 的那一行（是否出现在导航由该行的
// 「导航显示」控制）。行被删/下架时导航与 /blog/about 一并空态，不存在第二数据源。
const menus = computed(() =>
  buildNavMenus({
    customHomePage: isCustomHomePage.value,
    guestbookEnabled: isGuestbookEnabled.value,
    categoryEnabled: isCategoryNavEnabled.value,
    tagEnabled: isTagNavEnabled.value,
    archiveEnabled: isArchiveNavEnabled.value,
    businessEnabled: isBusinessNavEnabled.value,
    businessLabel: businessNavLabel.value,
    contactEnabled: isBusinessNavEnabled.value,
    pages: customPages.value
  })
)

const currentYear = computed(() => new Date().getFullYear())

// JSON-LD 结构化数据：Organization 随站点设置注入（详情页各自注入 Article，卸载时移除）
watch(
  () => blogSettingsStore.blogSettings,
  settings => {
    if (!settings?.blog_name) return
    const contactPoint: Record<string, unknown> = {
      '@type': 'ContactPoint',
      contactType: 'customer service'
    }
    if (settings.contact_phone) contactPoint.telephone = settings.contact_phone
    if (settings.blog_email) contactPoint.email = settings.blog_email
    setJsonLd('zh-jsonld-organization', {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      name: settings.blog_name,
      description: settings.blog_desc || undefined,
      url: `${window.location.origin}/blog`,
      contactPoint: settings.contact_phone || settings.blog_email ? contactPoint : undefined
    })
  },
  { immediate: true }
)

// 企业官网模板模式：给 <html> 挂企业主题（稳重蓝色板），仅前台生效，离开公开站点摘除
watch(isBusinessNavEnabled, applyEnterpriseTheme, { immediate: true })

function applyEnterpriseTheme(enabled: boolean) {
  document.documentElement.classList.toggle('theme-enterprise', enabled)
}

onBeforeUnmount(() => {
  document.documentElement.classList.remove('theme-enterprise')
  removeJsonLd('zh-jsonld-organization')
})

const rssUrl = computed(() => {
  const baseUrl = getApiBaseUrl()
  return `${baseUrl}/blog/rss`
})

const formatUrl = (url: string) => {
  if (!url) return ''
  if (!url.startsWith('http://') && !url.startsWith('https://')) return 'https://' + url
  return url
}

const toggleTheme = () => {
  settingsStore.toggleTheme()
}

// 路由变化时收起移动端菜单
watch(
  () => router.currentRoute.value.path,
  () => {
    mobileMenuOpen.value = false
    applyDefaultSeo()
  }
)

// 站点设置加载后应用默认 SEO
watch(blogSettings, () => {
  applyDefaultSeo()
})

const applyDefaultSeo = () => {
  const s = (blogSettings.value as any) || {}
  const siteName = s.blog_name || '知企建站系统'
  const siteDesc = s.blog_desc || ''
  applySeo({
    title: s.seo_title || (siteDesc ? `${siteName} - ${siteDesc}` : siteName),
    description: s.seo_description || siteDesc,
    keywords: s.blog_keywords || '',
    canonical: canonicalUrl(s.seo_canonical_url),
    robots: s.seo_robots || '',
    favicon: s.seo_favicon || ''
  })
}

const goToAdmin = () => {
  router.push('/admin/blog/article')
}

const handleUserCommand = async (command: string) => {
  if (command === 'profile') {
    window.location.href = '/user/profile'
  } else if (command === 'bookmark') {
    router.push('/blog/bookmarks')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logOut()
      ElMessage.success('已退出登录')
      router.push('/blog')
    } catch {
      // User cancelled
    }
  }
}

onMounted(() => {
  // 加载站点设置：页脚/版权/友链开关依赖它，而本组件在所有前台页面都会渲染，
  // 必须自行拉取，不能依赖各页面各自加载
  fetchBlogSettings()

  // 加载友情链接
  fetchFriendLinks()

  // 加载自定义页面导航入口
  fetchPublishedPages()

  // 登录用户：拉取未读通知并定时刷新
  if (userStore.token) {
    fetchUnreadCount()
    notifTimer = setInterval(fetchUnreadCount, 30000)
  }
})

onBeforeUnmount(() => {
  if (notifTimer) {
    clearInterval(notifTimer)
    notifTimer = null
  }
})

function fetchFriendLinks() {
  getFrontFriendLinkList()
    .then(response => {
      const list = (response && (response.data || response)) as FriendLink[] | undefined
      friendLinks.value = Array.isArray(list) ? list : []
    })
    .catch(() => {
      friendLinks.value = []
    })
}

/** 拉取公开站点设置（60 秒内已拉过则复用，避免与页面自身的加载重复请求） */
function fetchBlogSettings() {
  if (Date.now() - (blogSettingsStore.lastUpdate || 0) < 60000) {
    return
  }
  getBlogSettingsAnonymous()
    .then(response => {
      const settings = response?.data
      if (settings && typeof settings === 'object') {
        blogSettingsStore.updateBlogSettings(settings)
      }
    })
    .catch(() => {
      // 拉取失败时沿用已有值（store 内有默认值），不影响页面展示
    })
}

function fetchPublishedPages() {
  getPublishedPages()
    .then(response => {
      const list = response?.data
      customPages.value = Array.isArray(list) ? list : []
    })
    .catch(() => {
      customPages.value = []
    })
}
</script>

<style scoped>
.blog-layout {
  min-height: 100vh;
  background: var(--mo-n50);
  color: var(--mo-n800);
  font-family: var(--mo-font-sans);
}

/* 导航栏 */
.blog-top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}
.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--mo-sp-6);
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-left {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}
.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--mo-p600), var(--mo-p800));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: var(--mo-fs-sm);
}
.brand-name {
  font-weight: 700;
  font-size: var(--mo-fs-lg);
  color: var(--mo-n900);
  letter-spacing: 0;
}
.nav-center {
  display: flex;
  gap: 6px;
}
.nav-link {
  padding: var(--mo-sp-2) var(--mo-sp-4);
  border-radius: var(--mo-r-md);
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  color: var(--mo-n600);
  text-decoration: none;
  transition: all 0.2s;
}
.nav-link:hover,
.nav-link.router-link-active {
  background: var(--mo-p50);
  color: var(--mo-p700);
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-2);
  cursor: pointer;
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: 20px;
  transition: background 0.2s;
}
.user-avatar-wrapper {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  overflow: hidden;
}
.user-avatar {
  width: 30px;
  height: 30px;
  object-fit: cover;
  display: block;
}
.avatar-placeholder {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--mo-n200);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mo-n500);
}
.user-info:hover {
  background: var(--mo-n100);
}
.username {
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--mo-n800);
}
.dropdown-icon {
  color: var(--mo-n500);
}
.theme-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--mo-p600);
  padding: 6px;
  border-radius: var(--mo-r-md);
  transition: background 0.2s;
}
.theme-btn:hover {
  background: var(--mo-p50);
}

/* 底部 */
.blog-site-footer {
  position: relative;
  border-top: 1px solid var(--mo-n200);
  background: var(--mo-n100);
  color: var(--mo-n600);
  padding: 0 var(--mo-sp-6) var(--mo-sp-6);
}
.footer-wave {
  position: relative;
  margin-bottom: -2px;
  color: var(--mo-n50);
}
.footer-wave svg {
  width: 100%;
  height: 52px;
  display: block;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--mo-sp-8);
  padding-bottom: var(--mo-sp-7);
  border-bottom: 1px solid var(--mo-n200);
}
.footer-brand {
  display: flex;
  align-items: flex-start;
  gap: var(--mo-sp-3);
}
.footer-logo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--mo-p100);
  color: var(--mo-p600);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: var(--mo-fs-md);
  flex-shrink: 0;
}
.brand-text {
  display: flex;
  flex-direction: column;
}
.brand-desc {
  font-size: 13px;
  color: var(--mo-n600);
  margin-top: var(--mo-sp-1);
}
.footer-links {
  display: flex;
  gap: 60px;
}
.footer-col h4 {
  font-size: 13px;
  font-weight: 700;
  color: var(--mo-n800);
  letter-spacing: 0;
  margin-bottom: var(--mo-sp-4);
}
.footer-col a {
  display: block;
  font-size: var(--mo-fs-sm);
  color: var(--mo-n600);
  text-decoration: none;
  margin-bottom: 10px;
  transition: color 0.2s;
}
.footer-col a:hover {
  color: var(--mo-p600);
}
.footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding-top: var(--mo-sp-6);
  text-align: center;
  font-size: 13px;
  color: var(--mo-n600);
}
.tech-info {
  margin-top: 6px;
  font-size: var(--mo-fs-xs);
}

/* 备案号链接：继承页脚文字色（备案规范要求链接到工信部 site，但不强调视觉） */
.beian-info a {
  color: inherit;
  text-decoration: none;
}

.beian-info a:hover {
  text-decoration: underline;
}

/* 通知铃铛 */
.notif-trigger {
  display: flex;
  align-items: center;
  padding: 6px;
  margin-right: var(--mo-sp-1);
  border-radius: var(--mo-r-md);
  cursor: pointer;
  color: var(--mo-p600);
  transition: background 0.2s;
}
.notif-trigger:hover {
  background: var(--mo-p50);
}
.notif-panel {
  width: 100%;
}
.notif-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--mo-sp-2);
  padding-bottom: var(--mo-sp-2);
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
}
.notif-panel-title {
  font-size: var(--mo-fs-sm);
  font-weight: 600;
  color: var(--mo-n800);
}
.notif-list {
  max-height: 320px;
  overflow-y: auto;
}
.notif-item {
  padding: var(--mo-sp-2) var(--mo-sp-1);
  border-radius: var(--mo-r-sm);
  cursor: pointer;
  transition: background 0.15s;
}
.notif-item:hover {
  background: var(--mo-p50);
}
.notif-item + .notif-item {
  border-top: 1px dashed var(--el-border-color-lighter, #f0f0f0);
}
.notif-item-title {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--mo-n800);
}
.notif-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-danger, #f56c6c);
  margin-right: 6px;
  flex-shrink: 0;
}
.notif-item-content {
  margin-top: var(--mo-sp-1);
  font-size: var(--mo-fs-xs);
  color: var(--mo-n500);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notif-item-time {
  margin-top: var(--mo-sp-1);
  font-size: 11px;
  color: var(--mo-n500);
}
.notif-empty {
  padding: 18px 0;
  text-align: center;
  font-size: 13px;
  color: var(--mo-n500);
}
.notif-panel-foot {
  margin-top: var(--mo-sp-2);
  padding-top: var(--mo-sp-2);
  border-top: 1px solid var(--el-border-color-lighter, #ebeef5);
  text-align: center;
}
.notif-more {
  font-size: 13px;
  color: var(--mo-p600);
  text-decoration: none;
}

/* 汉堡按钮（桌面端隐藏） */
.hamburger-btn {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--mo-p600);
  padding: 6px;
  border-radius: var(--mo-r-md);
  transition: background 0.2s;
}
.hamburger-btn:hover,
.hamburger-btn.is-open {
  background: var(--mo-p50);
}

/* 移动端菜单（桌面端隐藏） */
.mobile-nav-menu {
  display: none;
}

/* 响应式 */
@media (max-width: 768px) {
  .nav-center {
    display: none;
  }
  .hamburger-btn {
    display: inline-flex;
    align-items: center;
    margin-left: var(--mo-sp-1);
  }
  .mobile-nav-menu {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 10px 16px 14px;
    background: var(--mo-n50);
    border-top: 1px solid rgba(0, 0, 0, 0.06);
  }
  .mobile-nav-link {
    padding: 10px 8px;
    border-radius: var(--mo-r-md);
    font-size: 15px;
    color: var(--mo-n800);
    text-decoration: none;
    transition: background 0.15s;
  }
  .mobile-nav-link:hover {
    background: var(--mo-p50);
    color: var(--mo-p600);
  }
  .footer-inner {
    flex-direction: column;
    gap: var(--mo-sp-6);
  }
  .footer-links {
    gap: var(--mo-sp-7);
  }
  .footer-wave svg {
    height: 40px;
  }
}
</style>
