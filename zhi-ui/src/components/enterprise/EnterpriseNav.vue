<template>
  <nav class="ent-nav" :class="{ solid: navSolid || !isHome, 'over-hero': isHome && !navSolid }">
    <div class="ent-nav-in">
      <router-link to="/" class="ent-logo">
        <img
          v-if="brandLogo"
          :src="brandLogo"
          class="ent-logo-img"
          :alt="blogSettings.blog_name || '站点 Logo'"
        />
        <svg v-else width="26" height="26" viewBox="0 0 26 26" fill="none" aria-hidden="true">
          <rect
            x="1.5"
            y="1.5"
            width="23"
            height="23"
            rx="6"
            stroke="currentColor"
            stroke-width="1.5"
          />
          <path
            d="M7 13h12M13 7v12"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
          />
        </svg>
        {{ blogSettings.blog_name || '知企建站系统' }}
      </router-link>

      <ul class="ent-links">
        <li v-for="item in menuItems" :key="item.linkValue" class="has-drop-slot">
          <!-- lv1 是下划线指示器的挂钩（.ent-links a.lv1::after）：导航改成 v-for
               数据驱动时曾漏掉，导致悬停/当前项下划线永不显示 -->
          <router-link
            :to="item.linkValue || '/'"
            class="lv1"
            :class="{ on: isActive(item.linkValue) }"
          >
            {{ item.label }}
          </router-link>
          <div v-if="item.children && item.children.length" class="drop">
            <router-link
              v-for="child in item.children"
              :key="child.linkValue"
              :to="child.linkValue || '/'"
            >
              {{ child.label }}
            </router-link>
          </div>
        </li>
      </ul>

      <div class="ent-nav-right">
        <el-dropdown
          v-if="userStore.token && userStore.name"
          trigger="click"
          @command="handleUserCommand"
        >
          <span class="ent-user">
            <el-icon :size="16"><UserFilled /></el-icon>
            <span class="ent-user-name">{{ userStore.name }}</span>
            <el-icon :size="12"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="userStore.userType === '00'" @click="goToAdmin">
                管理后台
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <router-link v-else to="/login" class="ent-login">登录</router-link>

        <router-link v-if="ctaItem" class="ent-cta" :to="ctaItem.linkValue || '/contact'">
          {{ ctaItem.label }}
        </router-link>

        <button
          class="ent-burger"
          aria-label="展开菜单"
          :class="{ 'is-open': mobileOpen }"
          @click="mobileOpen = !mobileOpen"
        >
          <el-icon :size="20"><Menu /></el-icon>
        </button>
      </div>
    </div>

    <!-- 移动端全屏抽屉（方案文档 §1.1：导航项纵向排布，底部固定 CTA） -->
    <div v-show="mobileOpen" class="ent-drawer">
      <router-link
        v-for="item in menuItems"
        :key="item.linkValue"
        :to="item.linkValue || '/'"
        class="ent-drawer-link"
        @click="mobileOpen = false"
      >
        {{ item.label }}
      </router-link>
      <router-link
        v-if="!userStore.token"
        to="/login"
        class="ent-drawer-link"
        @click="mobileOpen = false"
      >
        登录
      </router-link>
      <router-link
        v-if="ctaItem"
        class="ent-drawer-cta"
        :to="ctaItem.linkValue || '/contact'"
        @click="mobileOpen = false"
      >
        {{ ctaItem.label }}
      </router-link>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Menu, UserFilled } from '@element-plus/icons-vue'
import { ElMessageBox } from '@/plugins/element-plus-service'
import { useUserStore } from '@/stores/user'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteNav, type SiteNavItem } from '@/api/site/nav'
import { DEFAULT_HEADER_NAV } from '@/config/siteNav'
import { normalizeUploadUrl } from '@/utils/uploadUrl'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteSettingsStore = useSiteSettingsStore()

const blogSettings = computed(() => siteSettingsStore.siteSettings)

/** 站点 Logo：自定义 site_logo 优先，回退站点头像；都为空则用内联图形标记 */
const brandLogo = computed(() => {
  const s = blogSettings.value as Record<string, unknown>
  return normalizeUploadUrl(s?.site_logo || s?.blog_avatar)
})
const businessLabel = computed(
  () => String(blogSettings.value.business_label || '').trim() || '核心业务'
)

/* ---------- 导航数据：site_nav_item 表驱动（后台「菜单管理」维护） ---------- */
const navItems = ref<SiteNavItem[]>(DEFAULT_HEADER_NAV)

onMounted(async () => {
  try {
    const response = await getSiteNav('header')
    const tree = (response as { data?: SiteNavItem[] })?.data
    if (Array.isArray(tree) && tree.length > 0) {
      // 业务标签动态替换（business_label 设置），CTA 项单列渲染
      navItems.value = tree.map(item =>
        item.linkValue === '/business' ? { ...item, label: businessLabel.value } : item
      )
    }
  } catch {
    // 接口异常：保留内置默认导航
  }
})

watch(businessLabel, label => {
  navItems.value = navItems.value.map(item =>
    item.linkValue === '/business' ? { ...item, label } : item
  )
})

const menuItems = computed(() => navItems.value.filter(item => item.linkType !== 'cta'))
const ctaItem = computed(() => navItems.value.find(item => item.linkType === 'cta'))

/* ---------- 首页透明 → 滚动实底（方案文档 §1.1） ---------- */
// 根路径与 /blog 同为站点首页（企业模板导航使用短地址）
const isHome = computed(() => route.path === '/' || route.path === '/blog')
const navSolid = ref(false)

const onScroll = () => {
  navSolid.value = window.scrollY > 80
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})

const mobileOpen = ref(false)

// 路由切换时收起抽屉
watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
  }
)

const isActive = (link?: string) => Boolean(link) && route.path === link

const goToAdmin = () => router.push('/admin/article')

const handleUserCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logOut()
      router.push('/')
    } catch {
      // 用户取消
    }
  }
}
</script>

<style scoped>
.ent-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--tpl-bg);
  border-bottom: 1px solid var(--tpl-border);
  transition:
    background 0.25s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.ent-nav.solid {
  box-shadow: var(--tpl-sh-sm);
}

/* 首页未滚动：透明浮于 Hero 之上，文字用浅色 */
.ent-nav.over-hero {
  background: transparent;
  border-bottom-color: transparent;
}

.ent-nav-in {
  max-width: var(--tpl-grid);
  margin: 0 auto;
  padding: 0 var(--mo-sp-7);
  height: 64px;
  display: flex;
  align-items: center;
  gap: var(--mo-sp-9);
  transition: height 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 透明态高于实底态（方案文档 §1.1：滚动 80px 后 88px 收拢至 64px） */
.ent-nav.over-hero .ent-nav-in {
  height: 88px;
}

.ent-logo-img {
  /* 固定高度 + 自适应宽度：企业 Logo 多为横版（图形 + 文字），
     固定方框会把长图压成一条线；方形/竖版 Logo 由 max-width 与 contain 兜底。 */
  width: auto;
  max-width: 168px;
  height: 26px;
  object-fit: contain;
}

.ent-logo {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-2);
  font-weight: 600;
  color: var(--tpl-ink-900);
  font-size: var(--mo-fs-md);
  white-space: nowrap;
  text-decoration: none;
}

.ent-links {
  display: flex;
  gap: var(--mo-sp-6);
  margin-left: auto;
  list-style: none;
  margin-block: 0;
  padding-inline-start: 0;
}

.ent-links li {
  position: relative;
}

.ent-links a {
  display: block;
  padding: var(--mo-sp-1) 0;
  color: var(--tpl-ink-500);
  font-size: var(--mo-fs-md);
  text-decoration: none;
  position: relative;
}

.ent-links a.lv1::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1.5px;
  background: var(--tpl-p);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.ent-links a:hover,
.ent-links a.on {
  color: var(--tpl-ink-900);
}

.ent-links a:hover::after,
.ent-links a.on::after {
  transform: scaleX(1);
}

/* 二级下拉 */
.ent-links .has-drop-slot:hover .drop {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.drop {
  position: absolute;
  top: 100%;
  left: -16px;
  min-width: 180px;
  background: var(--tpl-bg);
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  box-shadow: var(--tpl-sh-md);
  padding: var(--mo-sp-2);
  opacity: 0;
  visibility: hidden;
  transform: translateY(-8px);
  transition:
    opacity 0.22s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.22s cubic-bezier(0.16, 1, 0.3, 1),
    visibility 0.22s;
}

.drop a {
  padding: var(--mo-sp-2) var(--mo-sp-3);
  border-radius: var(--tpl-r-sm);
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-700);
}

.drop a:hover {
  background: var(--tpl-surface);
  color: var(--tpl-p-text);
}

/* 右侧区 */
.ent-nav-right {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-4);
}

.ent-user {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-1);
  color: var(--tpl-ink-500);
  cursor: pointer;
  font-size: var(--mo-fs-sm);
}

.ent-user-name {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ent-login {
  color: var(--tpl-ink-500);
  font-size: var(--mo-fs-sm);
  text-decoration: none;
}

.ent-login:hover {
  color: var(--tpl-p-text);
}

.ent-cta {
  display: inline-flex;
  align-items: center;
  height: 40px;
  padding: 0 var(--mo-sp-4);
  border-radius: var(--tpl-r-md);
  background: var(--tpl-p);
  color: var(--tpl-on-hero);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
  text-decoration: none;
  transition:
    transform 0.2s cubic-bezier(0.16, 1, 0.3, 1),
    background 0.2s,
    box-shadow 0.2s;
}

.ent-cta:hover {
  background: var(--tpl-p-h);
  transform: translateY(-2px);
  box-shadow: var(--tpl-sh-md);
}

.ent-burger {
  display: none;
  border: none;
  background: transparent;
  color: var(--tpl-ink-700);
  cursor: pointer;
  padding: var(--mo-sp-1);
}

/* 首页透明态：文字/图标反白 */
.ent-nav.over-hero .ent-logo,
.ent-nav.over-hero .ent-links a,
.ent-nav.over-hero .ent-user,
.ent-nav.over-hero .ent-login,
.ent-nav.over-hero .ent-burger {
  color: var(--tpl-on-hero);
}

.ent-nav.over-hero .ent-links a.on,
.ent-nav.over-hero .ent-links a:hover {
  color: var(--tpl-on-hero);
}

.ent-nav.over-hero .ent-links a.lv1::after {
  /* 必须用随模式取值的 --tpl-on-hero：亮色模式 Hero 是浅底（site-templates.scss
     的 html:not(.dark) 覆盖），写死白色会让下划线在浅底上不可见（1:1） */
  background: var(--tpl-on-hero);
}

/* ---------- 移动端抽屉 ---------- */
.ent-drawer {
  display: flex;
  flex-direction: column;
  padding: var(--mo-sp-4) var(--mo-sp-5) var(--mo-sp-6);
  background: var(--tpl-bg);
  border-top: 1px solid var(--tpl-border);
}

.ent-drawer-link {
  padding: var(--mo-sp-4) 0;
  color: var(--tpl-ink-700);
  font-size: var(--mo-fs-md);
  text-decoration: none;
  border-bottom: 1px solid var(--tpl-border);
}

.ent-drawer-cta {
  margin-top: var(--mo-sp-6);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  border-radius: var(--tpl-r-md);
  background: var(--tpl-p);
  color: var(--tpl-on-hero);
  font-weight: 600;
  text-decoration: none;
}

@media (prefers-reduced-motion: reduce) {
  .ent-nav-in {
    transition: none;
  }
}

@media (max-width: 991px) {
  .ent-nav-in {
    padding: 0 var(--mo-sp-5);
    gap: var(--mo-sp-4);
  }

  .ent-links {
    display: none;
  }

  .ent-cta {
    display: none;
  }

  .ent-burger {
    display: block;
  }
}
</style>
