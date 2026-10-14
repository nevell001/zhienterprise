<template>
  <nav class="ent-nav" :class="{ solid: navSolid || !isHome, 'over-hero': isHome && !navSolid }">
    <div class="ent-nav-in">
      <router-link to="/" class="ent-logo">
        <svg width="26" height="26" viewBox="0 0 26 26" fill="none" aria-hidden="true"><rect x="1.5" y="1.5" width="23" height="23" rx="6" stroke="currentColor" stroke-width="1.5" /><path d="M7 13h12M13 7v12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" /></svg>
        {{ blogSettings.blog_name || '知企建站系统' }}
      </router-link>

      <ul class="ent-links">
        <li>
          <router-link to="/" :class="{ on: isHome }">首页</router-link>
        </li>
        <li>
          <router-link to="/business" :class="{ on: route.path.startsWith('/business') }">
            {{ businessLabel }}
          </router-link>
        </li>
        <li>
          <router-link to="/news" :class="{ on: route.path === '/news' }">
            新闻动态
          </router-link>
        </li>
        <li class="has-drop">
          <router-link to="/about" class="lv1" :class="{ on: route.path.startsWith('/about') }">
            关于我们
          </router-link>
          <div class="drop">
            <router-link to="/about">企业简介</router-link>
            <router-link to="/honors">荣誉资质</router-link>
            <router-link to="/history">发展历程</router-link>
          </div>
        </li>
        <li>
          <router-link to="/contact" :class="{ on: route.path === '/contact' }">
            联系我们
          </router-link>
        </li>
      </ul>

      <div class="ent-nav-right">
        <el-dropdown v-if="userStore.token && userStore.name" trigger="click" @command="handleUserCommand">
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

        <router-link class="ent-cta" to="/contact">咨询合作</router-link>

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
      <router-link to="/" class="ent-drawer-link" @click="mobileOpen = false">首页</router-link>
      <router-link to="/business" class="ent-drawer-link" @click="mobileOpen = false">
        {{ businessLabel }}
      </router-link>
      <router-link to="/news" class="ent-drawer-link" @click="mobileOpen = false">
        新闻动态
      </router-link>
      <router-link to="/about" class="ent-drawer-link" @click="mobileOpen = false">
        关于我们
      </router-link>
      <router-link to="/contact" class="ent-drawer-link" @click="mobileOpen = false">
        联系我们
      </router-link>
      <router-link v-if="!userStore.token" to="/login" class="ent-drawer-link" @click="mobileOpen = false">
        登录
      </router-link>
      <router-link class="ent-drawer-cta" to="/contact" @click="mobileOpen = false">
        咨询合作
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
import { useBlogSettingsStore } from '@/stores/blogSettings'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const blogSettingsStore = useBlogSettingsStore()

const blogSettings = computed(() => blogSettingsStore.blogSettings)
const businessLabel = computed(
  () => String(blogSettings.value.business_label || '').trim() || '核心业务'
)

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

const goToAdmin = () => router.push('/admin')

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
.ent-links .has-drop:hover .drop {
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
  color: var(--tpl-white);
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
  color: var(--tpl-white);
}

.ent-nav.over-hero .ent-links a.on,
.ent-nav.over-hero .ent-links a:hover {
  color: var(--tpl-white);
}

.ent-nav.over-hero .ent-links a.lv1::after {
  background: var(--tpl-white);
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
  color: var(--tpl-white);
  font-weight: 600;
  text-decoration: none;
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
