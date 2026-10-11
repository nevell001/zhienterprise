<template>
  <div class="profile-page">
    <div class="profile-shell">
      <section class="profile-summary">
        <div class="profile-identity">
          <div class="profile-avatar">
            <userAvatar />
          </div>
          <div class="profile-meta">
            <div class="profile-kicker">个人中心</div>
            <div class="name">
              {{ profileName }}
              <span class="verify">已认证</span>
            </div>
            <div class="bio">
              {{ profileBio }}
            </div>
          </div>
        </div>
        <div class="profile-actions">
          <el-button v-if="isAdmin" type="primary" size="small" @click="goArticleManage('create')">
            写文章
          </el-button>
          <el-button plain size="small" @click="openSettings('userinfo')">编辑资料</el-button>
        </div>
      </section>

      <section class="profile-stat-grid">
        <template v-if="isAdmin">
          <button type="button" class="stat-card" @click="selectedTab = 'articles'">
            <span class="stat-label">文章总数</span>
            <strong>{{ profileStats.articleTotal }}</strong>
          </button>
          <button type="button" class="stat-card" @click="selectedTab = 'articles'">
            <span class="stat-label">已发布</span>
            <strong>{{ profileStats.publishedTotal }}</strong>
          </button>
          <button type="button" class="stat-card" @click="selectedTab = 'articles'">
            <span class="stat-label">草稿</span>
            <strong>{{ profileStats.draftTotal }}</strong>
          </button>
          <button type="button" class="stat-card" @click="selectedTab = 'notifications'">
            <span class="stat-label">未读通知</span>
            <strong>{{ unreadNotificationCount }}</strong>
          </button>
        </template>
        <template v-else>
          <button
            type="button"
            class="stat-card stat-card--wide"
            @click="selectedTab = 'notifications'"
          >
            <span class="stat-label">未读通知</span>
            <strong>{{ unreadNotificationCount }}</strong>
          </button>
        </template>
      </section>

      <div class="profile-main-grid">
        <main class="profile-workspace">
          <div class="profile-tabs">
            <button
              v-if="isAdmin"
              type="button"
              class="tab"
              :class="{ active: selectedTab === 'articles' }"
              @click="selectedTab = 'articles'"
            >
              我的文章
            </button>
            <button
              type="button"
              class="tab"
              :class="{ active: selectedTab === 'notifications' }"
              @click="selectedTab = 'notifications'"
            >
              评论通知
              <span v-if="unreadNotificationCount" class="tab-badge">
                {{ unreadNotificationCount }}
              </span>
            </button>
            <button
              type="button"
              class="tab"
              :class="{ active: selectedTab === 'settings' }"
              @click="selectedTab = 'settings'"
            >
              账号设置
            </button>
          </div>

          <section v-if="selectedTab === 'articles'" class="profile-content">
            <div class="filter-bar">
              <div class="filter-tags">
                <span class="ftag active">最近文章 {{ profileStats.articleTotal }}</span>
                <span class="ftag">已发布 {{ profileStats.publishedTotal }}</span>
                <span class="ftag">草稿 {{ profileStats.draftTotal }}</span>
              </div>
              <el-button
                v-if="isAdmin"
                type="primary"
                size="small"
                @click="goArticleManage('create')"
              >
                写新文章
              </el-button>
            </div>

            <div v-if="articlesLoading" class="empty-panel">
              <div class="empty-title">正在加载文章</div>
              <div class="empty-desc">请稍候，正在读取当前账号的文章数据。</div>
            </div>
            <table v-else-if="profileArticles.length" class="article-table">
              <thead>
                <tr>
                  <th>标题</th>
                  <th class="hide-mobile">分类</th>
                  <th>状态</th>
                  <th class="hide-mobile">发布时间</th>
                  <th class="hide-mobile">数据</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="article in profileArticles" :key="article.id || article.title">
                  <td>
                    <div class="t-title">{{ article.title }}</div>
                  </td>
                  <td class="hide-mobile">
                    <span class="tag tag-blue">{{ article.categoryName || '未分类' }}</span>
                  </td>
                  <td>
                    <span :class="['tag', isPublished(article) ? 'tag-green' : 'tag-amber']">
                      {{ isPublished(article) ? '已发布' : '草稿' }}
                    </span>
                  </td>
                  <td class="hide-mobile">{{ article.createTime || '-' }}</td>
                  <td class="hide-mobile">
                    <span class="metric">
                      阅读 {{ article.viewCount || 0 }} · 评论 {{ article.commentCount || 0 }}
                    </span>
                  </td>
                  <td>
                    <div class="actions">
                      <button
                        v-if="isAdmin"
                        type="button"
                        class="act primary"
                        @click="goArticleManage('list')"
                      >
                        管理
                      </button>
                      <button
                        v-if="article.id && isPublished(article)"
                        type="button"
                        class="act"
                        @click="goPublicArticle(article.id)"
                      >
                        查看
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-panel">
              <div class="empty-title">{{ articlesLoadError || '还没有文章' }}</div>
              <div class="empty-desc">
                {{
                  articlesLoadError
                    ? articlesLoadError.includes('权限')
                      ? '请使用管理后台的文章管理功能。'
                      : '可以稍后重试，或直接进入文章管理页面。'
                    : '可以从文章管理页面创建第一篇内容。'
                }}
              </div>
              <el-button
                v-if="isAdmin && (!articlesLoadError || !articlesLoadError.includes('权限'))"
                type="primary"
                @click="goArticleManage('create')"
              >
                去写文章
              </el-button>
              <el-button v-else-if="isAdmin" type="primary" @click="goArticleManage('list')">
                前往文章管理
              </el-button>
            </div>
          </section>

          <section v-else-if="selectedTab === 'notifications'" class="profile-content">
            <div v-if="notificationsLoading" class="empty-panel">
              <div class="empty-title">加载中...</div>
            </div>
            <template v-else>
              <div v-if="notifications.length > 0" class="notification-list">
                <div class="notify-header">
                  <span class="notify-title">站内信</span>
                  <el-button link type="primary" size="small" @click="markAllNotificationsRead">
                    全部已读
                  </el-button>
                </div>
                <div
                  v-for="item in notifications"
                  :key="item.id"
                  class="notify-item"
                  :class="{ unread: item.isRead === 0 }"
                >
                  <div class="notify-content">
                    <p class="notify-body">
                      <strong>{{ item.senderName }}</strong>
                      {{ item.type === 'reply' ? ' 回复了你的评论' : ' 评论了你的文章' }}
                      <router-link class="notify-link" :to="`/blog/article/${item.articleId}`">
                        《{{ item.articleTitle }}》
                      </router-link>
                    </p>
                    <p v-if="item.content" class="notify-preview">{{ item.content }}</p>
                    <span class="notify-time">{{ item.createTime }}</span>
                  </div>
                  <el-button
                    v-if="item.isRead === 0"
                    link
                    type="primary"
                    size="small"
                    @click="markNotificationRead(item.id)"
                  >
                    标为已读
                  </el-button>
                </div>
              </div>
              <div v-else class="empty-panel">
                <div class="empty-title">暂无评论通知</div>
                <div class="empty-desc">当有人评论你的文章或回复你的评论时，通知会出现在这里。</div>
                <el-button v-if="isAdmin" plain @click="goArticleManage('list')">
                  查看文章管理
                </el-button>
              </div>
            </template>
          </section>

          <section v-else-if="selectedTab === 'settings'" class="profile-content profile-settings">
            <div class="settings-account-info">
              <div class="detail-row">
                <svg-icon icon-class="user" />
                用户名称
                <span>{{ state.user.userName || '-' }}</span>
              </div>
              <div class="detail-row">
                <svg-icon icon-class="phone" />
                手机号码
                <span>{{ state.user.phonenumber || '-' }}</span>
              </div>
              <div class="detail-row">
                <svg-icon icon-class="email" />
                用户邮箱
                <span>{{ state.user.email || '-' }}</span>
              </div>
              <div v-if="state.user.dept" class="detail-row">
                <svg-icon icon-class="tree" />
                所属部门
                <span>{{ state.user.dept.deptName }} / {{ state.postGroup }}</span>
              </div>
              <div class="detail-row">
                <svg-icon icon-class="peoples" />
                所属角色
                <span>{{ state.roleGroup || '-' }}</span>
              </div>
              <div class="detail-row">
                <svg-icon icon-class="date" />
                创建日期
                <span>{{ state.user.createTime || '-' }}</span>
              </div>
            </div>
            <el-tabs v-model="settingsSubTab" class="settings-tabs">
              <el-tab-pane label="基本资料" name="userinfo">
                <userInfo :user="state.user" />
              </el-tab-pane>
              <el-tab-pane label="修改密码" name="resetPwd">
                <resetPwd />
              </el-tab-pane>
            </el-tabs>
          </section>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="Profile">
import userAvatar from './userAvatar.vue'
import userInfo from './userInfo.vue'
import resetPwd from './resetPwd.vue'
import { getUserProfile } from '@/api/system/user'
import { getMyArticles } from '@/api/admin/blog/article'
import { getNotificationList, markAsRead, markAllAsRead } from '@/api/blog/notification'
import type { BlogNotification } from '@/api/blog/notification'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.userType === '00')
const selectedTab = ref(isAdmin.value ? 'articles' : 'notifications')
const settingsSubTab = ref('userinfo')
const state = reactive<Record<string, any>>({
  user: {},
  roleGroup: {},
  postGroup: {}
})
const profileArticles = ref<any[]>([])
const articlesLoading = ref(false)
const articlesLoadError = ref('')
const profileStats = reactive({
  articleTotal: 0,
  publishedTotal: 0,
  draftTotal: 0
})
const profileName = computed(() => state.user.nickName || state.user.userName || '站点用户')
const profileBio = computed(() => {
  const dept = state.user.dept?.deptName
  const role = state.roleGroup
  return [dept, role, state.user.email].filter(Boolean).join(' · ') || '记录思考，分享洞见'
})

// 站内信通知
const notifications = ref<BlogNotification[]>([])
const notificationsLoading = ref(false)
const unreadNotificationCount = computed(
  () => notifications.value.filter(n => n.isRead === 0).length
)

function getNotifications() {
  notificationsLoading.value = true
  getNotificationList({ pageNum: 1, pageSize: 50 })
    .then(response => {
      notifications.value = response.rows || []
    })
    .catch(error => {
      console.error('加载通知失败:', error)
      notifications.value = []
    })
    .finally(() => {
      notificationsLoading.value = false
    })
}

function markNotificationRead(id: number) {
  markAsRead([id]).then(() => {
    const item = notifications.value.find(n => n.id === id)
    if (item) {
      item.isRead = 1
    }
  })
}

function markAllNotificationsRead() {
  markAllAsRead().then(() => {
    notifications.value.forEach(n => {
      n.isRead = 1
    })
  })
}

function getUser() {
  getUserProfile()
    .then(response => {
      state.user = response.data
      state.roleGroup = response.roleGroup
      state.postGroup = response.postGroup
      if (isAdmin.value) {
        getProfileArticles()
      }
      getNotifications()
    })
    .catch(error => {
      console.error('加载个人信息失败:', error)
    })
}

function getProfileArticles() {
  articlesLoading.value = true
  articlesLoadError.value = ''
  Promise.all([
    getMyArticles({ pageNum: 1, pageSize: 5 }),
    getMyArticles({ pageNum: 1, pageSize: 1, status: 1 }),
    getMyArticles({ pageNum: 1, pageSize: 1, status: 0 })
  ])
    .then(([recentResponse, publishedResponse, draftResponse]) => {
      profileArticles.value = recentResponse.rows || []
      profileStats.articleTotal = recentResponse.total || profileArticles.value.length
      profileStats.publishedTotal = publishedResponse.total || 0
      profileStats.draftTotal = draftResponse.total || 0
    })
    .catch(error => {
      console.error('加载个人文章失败:', error)
      profileArticles.value = []
      profileStats.articleTotal = 0
      profileStats.publishedTotal = 0
      profileStats.draftTotal = 0
      // 检查是否是权限错误
      if (error?.response?.status === 403 || error?.message?.includes('权限')) {
        articlesLoadError.value = '您没有管理文章的权限'
      } else {
        articlesLoadError.value = '文章数据加载失败'
      }
    })
    .finally(() => {
      articlesLoading.value = false
    })
}

function goArticleManage(mode: 'create' | 'list') {
  if (mode === 'create') {
    router.push('/admin/blog/article')
    return
  }
  router.push('/admin/blog/article')
}

function isPublished(article: any) {
  return Number(article.status) === 1
}

function openSettings(tab: 'userinfo' | 'resetPwd') {
  settingsSubTab.value = tab
  selectedTab.value = 'settings'
}

function goPublicArticle(articleId: number) {
  router.push(`/blog/article/${articleId}`)
}

onMounted(() => {
  const activeTab = route.params && route.params.activeTab
  if (activeTab) {
    settingsSubTab.value = Array.isArray(activeTab) ? activeTab[0] : activeTab
    selectedTab.value = 'settings'
  }
  getUser()

  // 监听外部事件切换到通知tab
  window.addEventListener('switchProfileTab', onSwitchProfileTab as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('switchProfileTab', onSwitchProfileTab as EventListener)
})

function onSwitchProfileTab(event: Event) {
  const tab = (event as CustomEvent).detail
  if (tab) {
    selectedTab.value = tab
  }
}
</script>

<style scoped>
.profile-page {
  min-height: calc(100vh - 84px);
  color: var(--mo-n700, #44403c);
  background: var(--mo-n50, #fafaf9);
}

.profile-shell {
  max-width: 1240px;
  padding: 24px 32px 32px;
  margin: 0 auto;
}

.profile-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px;
  background: var(--mo-n0, #fff);
  border: 1px solid var(--mo-n200, #e7e5e4);
  border-radius: 12px;
  box-shadow: var(--mo-shadow-sm);
}

.profile-identity {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 16px;
}

.profile-avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  overflow: hidden;
  background: var(--mo-p50, #eef2ff);
  border: 1px solid var(--mo-p100, #e0e7ff);
  border-radius: 50%;
}

.profile-avatar :deep(.user-info-head) {
  width: 72px;
  height: 72px;
}

/* 头像图片填满 72×72 容器并居中，避免 120×120 原图从左上角裁剪 */
.profile-avatar :deep(.img-circle) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.profile-meta {
  min-width: 0;
  flex: 1;
}

.profile-kicker {
  margin-bottom: 4px;
  color: var(--mo-p700, #4338ca);
  font-size: 12px;
  font-weight: 700;
}

.name {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--mo-n900, #1c1917);
  font-size: 20px;
  font-weight: 700;
}

.verify {
  padding: 2px 8px;
  color: var(--mo-p700, #4338ca);
  font-size: 11px;
  font-weight: 600;
  background: var(--mo-p50, #eef2ff);
  border-radius: 9999px;
}

.bio {
  margin-top: 4px;
  overflow: hidden;
  color: var(--mo-n500, #78716c);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.profile-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72px;
  padding: 14px 16px;
  text-align: left;
  cursor: pointer;
  background: var(--mo-n0, #fff);
  border: 1px solid var(--mo-n200, #e7e5e4);
  border-radius: 8px;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}

.stat-card:hover {
  border-color: var(--mo-p200, #c7d2fe);
  box-shadow: var(--mo-shadow-md);
}

.stat-card strong {
  min-width: 0;
  overflow: hidden;
  color: var(--mo-n900, #1c1917);
  font-size: 24px;
  line-height: 1;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-label {
  color: var(--mo-n500, #78716c);
  font-size: 12px;
  font-weight: 600;
}

.stat-card--wide {
  grid-column: 1 / -1;
}

.profile-main-grid {
  margin-top: 16px;
}

.profile-workspace {
  min-width: 0;
  background: var(--mo-n0, #fff);
  border: 1px solid var(--mo-n200, #e7e5e4);
  border-radius: 12px;
  box-shadow: var(--mo-shadow-sm);
}

.profile-tabs {
  display: flex;
  padding: 0 18px;
  border-bottom: 1px solid var(--mo-n200, #e7e5e4);
}

.profile-tabs .tab {
  padding: 14px 16px;
  color: var(--mo-n500, #78716c);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  transition: all 0.15s;
}

.profile-tabs .tab:hover {
  color: var(--mo-n800, #292524);
}

.profile-tabs .tab.active {
  color: var(--mo-p600, #4f46e5);
  border-bottom-color: var(--mo-p600, #4f46e5);
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  margin-left: 6px;
  padding: 0 5px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  background: var(--mo-p600, #4f46e5);
  border-radius: 9999px;
}

.profile-content {
  padding: 18px;
}

/* 账号设置页签：账号信息 + 子 Tab */
.profile-settings {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings-account-info {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--mo-n200, #e7e5e4);
  border-radius: 8px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ftag {
  padding: 5px 14px;
  color: var(--mo-n600, #57534e);
  font-size: 13px;
  cursor: pointer;
  background: var(--mo-n100, #f5f5f4);
  border-radius: 9999px;
  transition: all 0.15s;
}

.ftag.active {
  color: #fff;
  font-weight: 500;
  background: var(--mo-p600, #4f46e5);
}

.article-table {
  width: 100%;
  overflow: hidden;
  font-size: 13px;
  background: var(--mo-n0, #fff);
  border: 1px solid var(--mo-n200, #e7e5e4);
  border-collapse: collapse;
  border-radius: 8px;
}

.article-table th {
  padding: 12px 16px;
  color: var(--mo-n600, #57534e);
  font-size: 12px;
  font-weight: 600;
  text-align: left;
  text-transform: uppercase;
  background: var(--mo-n50, #fafaf9);
  border-bottom: 1px solid var(--mo-n200, #e7e5e4);
}

.article-table td {
  padding: 14px 16px;
  color: var(--mo-n700, #44403c);
  border-bottom: 1px solid var(--mo-n100, #f5f5f4);
}

.article-table tr:last-child td {
  border-bottom: 0;
}

.article-table tr:hover {
  background: var(--mo-n50, #fafaf9);
}

.t-title {
  color: var(--mo-n800, #292524);
  font-weight: 500;
  cursor: pointer;
}

.t-title:hover {
  color: var(--mo-p600, #4f46e5);
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
}

.tag-blue {
  color: var(--mo-p700, #4338ca);
  background: var(--mo-p50, #eef2ff);
}

.tag-green {
  color: #047857;
  background: #dcfce7;
}

.tag-pink {
  color: #db2777;
  background: #fce7f3;
}

.tag-amber {
  color: #d97706;
  background: #fef3c7;
}

.metric {
  color: var(--mo-n500, #78716c);
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 4px;
}

.act {
  padding: 4px 10px;
  color: var(--mo-n500, #78716c);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 4px;
}

.act:hover {
  color: var(--mo-n700, #44403c);
  background: var(--mo-n100, #f5f5f4);
}

.act.primary:hover {
  color: var(--mo-p600, #4f46e5);
  background: var(--mo-p50, #eef2ff);
}

.empty-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  padding: 32px;
  text-align: center;
  background: var(--mo-n0, #fff);
  border: 1px solid var(--mo-n200, #e7e5e4);
  border-radius: 8px;
}

.empty-title {
  color: var(--mo-n900, #1c1917);
  font-size: 16px;
  font-weight: 600;
}

.empty-desc {
  margin: 8px 0 18px;
  color: var(--mo-n500, #78716c);
  font-size: 13px;
}

.settings-account-info {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--mo-n200, #e7e5e4);
  border-radius: 8px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 11px 12px;
  color: var(--mo-n600, #57534e);
  font-size: 13px;
  border-bottom: 1px solid var(--mo-n100, #f5f5f4);
}

.detail-row span {
  min-width: 0;
  margin-left: auto;
  overflow: hidden;
  color: var(--mo-n800, #292524);
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-row:last-child {
  border-bottom: 0;
}

.settings-tabs {
  min-width: 0;
}

.settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

@media (max-width: 768px) {
  .profile-shell {
    padding: 16px;
  }

  .profile-summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-actions {
    width: 100%;
  }

  .profile-actions :deep(.el-button) {
    flex: 1;
    margin-left: 0;
  }

  .profile-stat-grid,
  .profile-main-grid {
    grid-template-columns: 1fr;
  }

  .article-table {
    font-size: 12px;
  }

  .article-table th,
  .article-table td {
    padding: 8px;
  }

  .hide-mobile {
    display: none;
  }
}

/* ===== 站内信通知 ===== */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.notify-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 12px;
  border-bottom: 1px solid var(--mo-n200, #e7e5e4);
  margin-bottom: 4px;
}

.notify-title {
  color: var(--mo-n900, #1c1917);
  font-size: 15px;
  font-weight: 600;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--mo-n100, #f5f5f4);
  transition: background 0.15s;
}

.notify-item:last-child {
  border-bottom: 0;
}

.notify-item:hover {
  background: var(--mo-n50, #fafaf9);
}

.notify-item.unread {
  background: var(--mo-p25, #f5f3ff);
}

.notify-item.unread::before {
  content: '';
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 6px;
  background: var(--mo-p600, #4f46e5);
  border-radius: 50%;
}

.notify-content {
  flex: 1;
  min-width: 0;
}

.notify-body {
  margin: 0;
  color: var(--mo-n700, #44403c);
  font-size: 13px;
  line-height: 1.5;
}

.notify-preview {
  margin: 4px 0 0;
  overflow: hidden;
  color: var(--mo-n400, #a8a29e);
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-time {
  display: block;
  margin-top: 4px;
  color: var(--mo-n400, #a8a29e);
  font-size: 11px;
}

.notify-link {
  color: var(--mo-p600, #4f46e5);
  text-decoration: none;
}

.notify-link:hover {
  text-decoration: underline;
}

/* ===== 深色模式 =====
   个人中心的亮色样式大量引用 --mo-n* 变量，深色下需反转色阶。
   为与各主题深色配色一致：
   - 默认主题（Tech Blue 深蓝）：将 --mo-n* 映射到 --el-* 变量（#0a192f/#112240 系）
   - Mo-Blog 主题（中性深棕）：用 mo-blog 棕色色阶反转
   两种映射都使背景变深、文字变浅，自动适配所有引用变量的子元素。 */

/* 默认主题深色：映射到 Tech Blue 深蓝系（与其他管理页面一致） */
html.dark .profile-page {
  --mo-n0: var(--el-bg-color-overlay);
  --mo-n50: var(--el-bg-color);
  --mo-n100: var(--el-bg-color-overlay);
  --mo-n200: var(--el-border-color);
  --mo-n300: var(--el-text-color-regular);
  --mo-n400: var(--el-text-color-regular);
  --mo-n500: var(--el-text-color-regular);
  --mo-n600: var(--el-text-color-primary);
  --mo-n700: var(--el-text-color-primary);
  --mo-n800: var(--el-text-color-primary);
  --mo-n900: var(--el-text-color-primary);
  --mo-p25: rgba(0, 212, 255, 0.08);
  --mo-p50: rgba(0, 212, 255, 0.12);
  --mo-p100: rgba(0, 212, 255, 0.2);
  --mo-p200: rgba(0, 212, 255, 0.3);
}

/* 状态标签硬编码浅色 → 深色暗调 */
html.dark .profile-page .tag-green {
  color: #34d399;
  background: rgba(16, 185, 129, 0.15);
}

html.dark .profile-page .tag-pink {
  color: #f472b6;
  background: rgba(236, 72, 153, 0.15);
}

html.dark .profile-page .tag-amber {
  color: #fbbf24;
  background: rgba(245, 158, 11, 0.15);
}

/* 表格行深色高亮 */
html.dark .profile-page .article-table tr:hover {
  background: rgba(99, 102, 241, 0.08);
}

/* 以下元素在 .profile-page 色阶反转后，亮色样式引用的 --mo-n* 已自动适配深色
   （背景 n0/n50/n100→深色，文字 n700/n800/n900→浅色，边框 n200→深色）。
   仅保留不依赖反转变量的特殊处理：状态标签固定暗调、hover 用半透明高亮。 */
html.dark .profile-page .tag-blue {
  color: #a5b4fc;
  background: rgba(79, 70, 229, 0.15);
}

html.dark .profile-page .notify-item.unread {
  background: rgba(79, 70, 229, 0.1);
}

html.dark .profile-page .settings-item:hover,
html.dark .profile-page .notify-item:hover,
html.dark .profile-page .act:hover {
  background: rgba(99, 102, 241, 0.08);
}

html.dark .profile-page .load-more:hover {
  background: rgba(99, 102, 241, 0.12);
}

html.dark .act.primary:hover {
  color: var(--mo-p300);
  background: rgba(79, 70, 229, 0.15);
}

html.dark .filter-bar {
  color: var(--mo-n300);
}

html.dark .notification-list {
  background: var(--mo-n800);
  border: 1px solid var(--mo-n700);
  border-radius: 8px;
}

html.dark .notify-header {
  color: var(--mo-n100);
  border-bottom-color: var(--mo-n100);
}

html.dark .profile-page {
  background: var(--mo-n50);
}

html.dark .profile-summary {
  background: var(--mo-n0);
  border-color: var(--mo-n200);
}

html.dark .profile-avatar {
  /* zinc-800 不在 --el-* 令牌内，保留字面量 */
  background: #27272a;
  border-color: var(--mo-n200);
}

/* zinc-400 不在 --el-* 令牌内，保留字面量 */
html.dark .profile-kicker {
  color: #a1a1aa;
}

html.dark .bio {
  color: #a1a1aa;
}

html.dark .stat-card {
  background: var(--mo-n0);
  border-color: var(--mo-n200);
}

/* zinc-100 不在 --el-* 令牌内，保留字面量 */
html.dark .stat-card strong {
  color: #f4f4f5;
}

html.dark .stat-label {
  color: #a1a1aa;
}

html.dark .verify {
  color: #a1a1aa;
  background: #27272a;
}

html.dark .profile-workspace {
  background: var(--mo-n0);
  border: 1px solid var(--mo-n200);
  border-radius: 8px;
}

html.dark .profile-tabs {
  border-color: var(--mo-n200);
}

html.dark .tab {
  color: #a1a1aa;
}

html.dark .tab.active {
  color: #f4f4f5;
  background: #27272a;
}

html.dark .filter-bar {
  background: var(--mo-n0);
  border-color: var(--mo-n200);
}

html.dark .article-table {
  background: var(--mo-n0);
}

html.dark .article-table th {
  background: #27272a;
  color: #f4f4f5;
  border-color: var(--mo-n200);
}

html.dark .article-table td {
  border-color: #27272a;
}

html.dark .empty-panel {
  background: var(--mo-n0);
}

html.dark .settings-account-info {
  background: #27272a;
  border-color: var(--mo-n200);
}

html.dark .detail-row {
  color: #a1a1aa;
  border-bottom-color: var(--mo-n200);
}

/* zinc-200 不在 --el-* 令牌内，保留字面量 */
html.dark .detail-row span {
  color: #e4e4e7;
}

html.dark .settings-tabs :deep(.el-tabs__header) {
  background: transparent;
  border-bottom-color: var(--mo-n200);
}

html.dark .settings-tabs :deep(.el-tabs__item) {
  color: #a1a1aa;
}

html.dark .settings-tabs :deep(.el-tabs__item.is-active) {
  color: #f4f4f5;
}

html.dark .settings-tabs :deep(.el-tabs__active-bar) {
  /* indigo-500，不在 --mo-* 深色重映射内，保留字面量 */
  background: #6366f1;
}

html.dark .settings-tabs :deep(.el-tabs__content) {
  background: var(--mo-n0);
}

html.dark .settings-tabs :deep(.el-form-item__label) {
  color: #e4e4e7;
}

html.dark .settings-tabs :deep(.el-input__wrapper) {
  background: #27272a;
  border-color: var(--mo-n200);
}

html.dark .settings-tabs :deep(.el-input__inner) {
  color: #e4e4e7;
  background: transparent;
}

html.dark .settings-tabs :deep(.el-radio__label) {
  color: #e4e4e7;
}

html.dark .settings-tabs :deep(.el-radio) {
  color: #e4e4e7;
}

/* ===== Mo-Blog 主题深色模式表单控件适配 ===== */
html.dark.theme-mo-blog .settings-tabs :deep(.el-tabs__item) {
  color: var(--mo-n500) !important;
  background: transparent !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-tabs__item.is-active) {
  color: var(--mo-n700) !important;
  background: transparent !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-tabs__active-bar) {
  background: var(--mo-p600) !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-tabs__header) {
  background: transparent !important;
  border-bottom-color: var(--mo-n200) !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-form-item__label) {
  color: var(--mo-n600) !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-input__wrapper) {
  background: var(--mo-n100) !important;
  border-color: var(--mo-n200) !important;
  box-shadow: 0 0 0 1px var(--mo-n200) inset !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-input__wrapper.is-focus) {
  border-color: var(--mo-p600) !important;
  box-shadow: 0 0 0 1px var(--mo-p600) inset !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-input__inner) {
  color: var(--mo-n700) !important;
  background: transparent !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-input__inner::placeholder) {
  color: var(--mo-n400) !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-textarea__inner) {
  background: var(--mo-n100) !important;
  border-color: var(--mo-n200) !important;
  color: var(--mo-n700) !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-textarea__inner:focus) {
  border-color: var(--mo-p600);
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-textarea__inner::placeholder) {
  color: var(--mo-n400);
}

/* 单选框深色样式 */
html.dark.theme-mo-blog .settings-tabs :deep(.el-radio) {
  color: var(--mo-n600);
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-radio__input.is-checked .el-radio__inner) {
  background: var(--mo-p600);
  border-color: var(--mo-p600);
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-radio__inner) {
  background: var(--mo-n100) !important;
  border-color: var(--mo-n200) !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-radio__inner:hover) {
  border-color: var(--mo-n300);
}

/* 按钮深色样式 */
html.dark.theme-mo-blog .settings-tabs :deep(.el-button--primary) {
  background: var(--mo-p600);
  border-color: var(--mo-p600);
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-button--primary:hover) {
  background: var(--mo-p700);
  border-color: var(--mo-p700);
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-button--danger) {
  background: var(--mo-danger, #ef4444);
  border-color: var(--mo-danger, #ef4444);
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-button--danger:hover) {
  background: #dc2626;
  border-color: #dc2626;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-button.is-plain) {
  background: var(--mo-n100);
  border-color: var(--mo-n200);
  color: var(--mo-n600);
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-button.is-plain:hover) {
  background: var(--mo-n50);
  border-color: var(--mo-n300);
  color: var(--mo-n700);
}

/* Tabs 内容区域深色背景 */
html.dark.theme-mo-blog .settings-tabs :deep(.el-tabs__content) {
  background: transparent !important;
}

html.dark.theme-mo-blog .settings-tabs :deep(.el-tab-pane) {
  background: transparent !important;
}

html.dark.theme-mo-blog .settings-account-info {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .detail-row {
  color: var(--mo-n500);
  border-bottom-color: var(--mo-n200);
}

html.dark.theme-mo-blog .detail-row span {
  color: var(--mo-n700);
}

html.dark.theme-mo-blog .profile-workspace {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

/* Profile 页面整体背景 */
html.dark.theme-mo-blog .profile-page {
  background: var(--mo-n0);
}

html.dark.theme-mo-blog .profile-summary {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .profile-avatar {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .profile-kicker,
html.dark.theme-mo-blog .bio {
  color: var(--mo-n400);
}

html.dark.theme-mo-blog .stat-card {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .stat-card strong {
  color: var(--mo-n700);
}

html.dark.theme-mo-blog .stat-label {
  color: var(--mo-n500);
}

html.dark.theme-mo-blog .verify {
  color: var(--mo-n500);
  background: var(--mo-n100);
}

html.dark.theme-mo-blog .empty-panel {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .empty-title {
  color: var(--mo-n700);
}

html.dark.theme-mo-blog .empty-desc {
  color: var(--mo-n500);
}

html.dark.theme-mo-blog .profile-tabs {
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .tab {
  color: var(--mo-n500);
}

html.dark.theme-mo-blog .tab.active {
  color: var(--mo-n700);
  background: var(--mo-n50);
}

html.dark.theme-mo-blog .filter-bar {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
  color: var(--mo-n600);
}

html.dark.theme-mo-blog .article-table {
  background: var(--mo-n50);
}

html.dark.theme-mo-blog .article-table th {
  background: var(--mo-n100);
  color: var(--mo-n700);
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .article-table td {
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .notification-list {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

html.dark.theme-mo-blog .notify-header {
  color: var(--mo-n700);
  border-bottom-color: var(--mo-n200);
}

html.dark.theme-mo-blog .notify-body,
html.dark.theme-mo-blog .notify-time,
html.dark.theme-mo-blog .notify-preview {
  color: var(--mo-n500);
}

html.dark.theme-mo-blog .notify-link {
  color: var(--mo-p400);
}

html.dark.theme-mo-blog .metric {
  color: var(--mo-n500);
}

html.dark.theme-mo-blog .act {
  color: var(--mo-n500);
}

html.dark.theme-mo-blog .act:hover {
  color: var(--mo-n600);
  background: var(--mo-n100);
}

html.dark.theme-mo-blog .act.primary:hover {
  color: var(--mo-p400);
  background: var(--mo-p25);
}

html.dark.theme-mo-blog .tag {
  color: var(--mo-n600);
}

html.dark.theme-mo-blog .load-more {
  color: var(--mo-n500);
}

html.dark.theme-mo-blog .load-more:hover {
  background: var(--mo-n100);
}
</style>
