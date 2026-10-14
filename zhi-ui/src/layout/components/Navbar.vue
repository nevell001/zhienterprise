<template>
  <div class="navbar">
    <hamburger
      id="hamburger-container"
      :is-active="appStore.sidebar.opened"
      class="hamburger-container"
      @toggle-click="toggleSideBar"
    />
    <breadcrumb
      v-if="!settingsStore.topNav"
      id="breadcrumb-container"
      class="breadcrumb-container"
    />
    <top-nav v-if="settingsStore.topNav" id="topmenu-container" class="topmenu-container" />

    <div class="right-menu">
      <template v-if="appStore.device !== 'mobile'">
        <header-search id="header-search" class="right-menu-item" />

        <screenfull id="screenfull" class="right-menu-item hover-effect" />

        <el-tooltip :content="themeModeTooltip" effect="dark" placement="bottom">
          <div class="right-menu-item hover-effect theme-switch-wrapper" @click="toggleTheme">
            <svg-icon v-if="settingsStore.themeMode === 'system'" icon-class="system" />
            <svg-icon v-else-if="settingsStore.isDark" icon-class="sunny" />
            <svg-icon v-else icon-class="moon" />
          </div>
        </el-tooltip>

        <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip>
      </template>

      <!-- 站内信通知 -->
      <el-dropdown
        v-if="userStore.token"
        class="right-menu-item hover-effect notification-container"
        trigger="click"
        placement="bottom-end"
        @visible-change="handleDropdownVisible"
      >
        <el-badge
          :value="unreadCount"
          :hidden="unreadCount === 0"
          :max="99"
          class="notification-badge"
        >
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
        <template #dropdown>
          <el-dropdown-menu class="notification-dropdown">
            <div class="notification-header">
              <span>站内信通知</span>
              <el-button
                v-if="unreadCount > 0"
                type="primary"
                link
                size="small"
                @click="handleMarkAllRead"
              >
                全部已读
              </el-button>
            </div>
            <div v-loading="notificationLoading" class="notification-list">
              <div v-if="notificationList.length === 0" class="empty-tip">暂无通知</div>
              <div
                v-for="item in notificationList"
                :key="item.id"
                class="notification-item"
                :class="{ unread: item.isRead === 0 }"
                @click="handleNotificationClick(item)"
              >
                <div class="notification-title">{{ item.title }}</div>
                <div class="notification-content">{{ item.content }}</div>
                <div class="notification-meta">
                  <span class="notification-article">{{ item.articleTitle || '系统通知' }}</span>
                  <span class="notification-time">{{ formatTime(item.createTime) }}</span>
                </div>
              </div>
            </div>
            <div class="notification-footer">
              <el-button type="primary" link size="small" @click="goToProfileNotifications">
                查看全部通知
              </el-button>
            </div>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-dropdown
        class="avatar-container right-menu-item hover-effect"
        trigger="hover"
        @command="handleCommand"
      >
        <div class="avatar-wrapper">
          <img :src="userStore.avatar" class="user-avatar" />
          <span class="user-nickname">{{ userStore.nickName }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <!-- 仅管理员显示管理后台入口 -->
            <el-dropdown-item v-if="userStore.userType === '00'" @click="goToAdmin">
              <span>管理后台</span>
            </el-dropdown-item>
            <!-- 个人中心 -->
            <router-link to="/user/profile">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <el-dropdown-item divided command="logout">
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <div
        v-if="settingsStore.showSettings"
        class="right-menu-item hover-effect setting"
        @click="setLayout"
      >
        <svg-icon icon-class="more-up" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from '@/plugins/element-plus-service'
import { Bell } from '@element-plus/icons-vue'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import TopNav from '@/components/TopNav/index.vue'
import Hamburger from '@/components/Hamburger/index.vue'
import Screenfull from '@/components/Screenfull/index.vue'
import SizeSelect from '@/components/SizeSelect/index.vue'
import HeaderSearch from '@/components/HeaderSearch/index.vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import {
  getUnreadCount,
  getNotificationList,
  markAsRead,
  markAllAsRead,
  type SiteNotification
} from '@/api/site/notification'

const appStore = useAppStore()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const router = useRouter()

const unreadCount = ref(0)
const notificationList = ref<SiteNotification[]>([])
const notificationLoading = ref(false)
let unreadTimer: ReturnType<typeof setInterval> | null = null

function toggleSideBar() {
  appStore.toggleSideBar()
}

function handleCommand(command: string) {
  switch (command) {
    case 'setLayout':
      setLayout()
      break
    case 'logout':
      logout()
      break
    default:
      break
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      userStore.logOut().then(() => {
        window.location.href = '/login'
      })
    })
    .catch(error => {
      if (error !== 'cancel') console.error('操作失败:', error)
    })
}

const emits = defineEmits(['setLayout'])
function setLayout() {
  emits('setLayout')
}

function toggleTheme() {
  settingsStore.toggleTheme()
}

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

function goToAdmin() {
  // 跳转到站点管理页面（正确的路由路径）
  router.push('/admin/article')
}

// ============ 通知相关 ============
function fetchUnreadCount() {
  getUnreadCount()
    .then(response => {
      unreadCount.value = Number(response.data) || 0
    })
    .catch(() => {
      // silent fail
    })
}

function fetchNotificationList() {
  notificationLoading.value = true
  getNotificationList({ pageNum: 1, pageSize: 10 })
    .then(response => {
      const data = (response && (response.rows || response.data)) as SiteNotification[] | undefined
      notificationList.value = Array.isArray(data) ? data : []
    })
    .catch(() => {
      notificationList.value = []
    })
    .finally(() => {
      notificationLoading.value = false
    })
}

function handleDropdownVisible(visible: boolean) {
  if (visible) {
    fetchNotificationList()
  }
}

function handleNotificationClick(item: SiteNotification) {
  if (item.isRead === 0) {
    markAsRead([item.id])
      .then(() => {
        item.isRead = 1
        fetchUnreadCount()
      })
      .catch(error => {
        if (error !== 'cancel') console.error('操作失败:', error)
      })
  }

  // 根据通知类型智能跳转
  if (item.type === 'comment' || item.type === 'reply') {
    // 新评论/回复 → 跳到评论管理页（管理员审核）
    router.push('/admin/comment')
  } else if (item.articleId) {
    // 审核结果通知 → 跳到文章详情（评论者查看自己的评论）
    window.open(`/article/${item.articleId}`, '_blank')
  }
}

function handleMarkAllRead() {
  markAllAsRead()
    .then(() => {
      ElMessage.success('已全部标记为已读')
      notificationList.value.forEach(n => (n.isRead = 1))
      fetchUnreadCount()
    })
    .catch(error => {
      if (error !== 'cancel') console.error('操作失败:', error)
    })
}

function goToProfileNotifications() {
  window.location.href = '/user/profile'
  setTimeout(() => {
    window.dispatchEvent(new CustomEvent('switchProfileTab', { detail: 'notifications' }))
  }, 300)
}

function formatTime(time: string): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return time.substring(0, 10)
}

onMounted(() => {
  if (userStore.token) {
    fetchUnreadCount()
    unreadTimer = setInterval(fetchUnreadCount, 30000)
  }
})

onUnmounted(() => {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
})
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: var(--navbar-bg);
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;
    display: flex;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: rgba(0, 0, 0, 0.025);
        }
      }

      &.theme-switch-wrapper {
        display: flex;
        align-items: center;

        svg {
          transition: transform 0.3s;

          &:hover {
            transform: scale(1.15);
          }
        }
      }
    }

    .notification-container {
      display: flex;
      align-items: center;
      cursor: pointer;

      .notification-badge {
        display: flex;
        align-items: center;
      }
    }

    .avatar-container {
      margin-right: 0px;
      padding-right: 0px;
      display: flex;
      align-items: center;
      height: 100%;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
        position: relative;
        cursor: pointer;

        .user-avatar {
          cursor: pointer;
          width: 30px;
          height: 30px;
          border-radius: 50%;
        }

        .user-nickname {
          margin-left: 5px;
          font-size: 14px;
          font-weight: bold;
        }

        i {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
        }
      }
    }
  }
}
</style>

<style lang="scss">
.notification-dropdown {
  width: 360px;
  padding: 0;
  max-height: 500px;

  .notification-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    font-size: 14px;
    font-weight: 600;
  }

  .notification-list {
    max-height: 360px;
    overflow-y: auto;
  }

  .empty-tip {
    padding: 32px 16px;
    text-align: center;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  .notification-item {
    padding: 12px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.unread {
      background: var(--el-color-primary-light-9);

      .notification-title {
        font-weight: 600;
      }
    }

    .notification-title {
      font-size: 13px;
      color: var(--el-text-color-primary);
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .notification-content {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      margin-bottom: 6px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .notification-meta {
      display: flex;
      justify-content: space-between;
      font-size: 11px;
      color: var(--el-text-color-placeholder);
    }
  }

  .notification-footer {
    padding: 8px 16px;
    text-align: center;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}
</style>
