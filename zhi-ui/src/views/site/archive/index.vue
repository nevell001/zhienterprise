<template>
  <SiteLayout>
    <div class="archive-page">
      <!-- 页面头部 -->
      <header class="page-header">
        <div class="header-bg"></div>
        <div class="header-content">
          <div class="header-icon">
            <el-icon :size="48">
              <Calendar />
            </el-icon>
          </div>
          <h1 class="page-title">文章归档</h1>
          <p class="page-description">
            共
            <span class="highlight-number">{{ totalArticles }}</span>
            篇文章， 记录了
            <span class="highlight-number">{{ archiveList.length }}</span>
            个月的写作历程
          </p>
          <div class="back-button">
            <router-link to="/" class="back-link">
              <el-button type="default" plain size="large" round>
                <el-icon><ArrowLeft /></el-icon>
                返回首页
              </el-button>
            </router-link>
          </div>
        </div>
      </header>

      <!-- 归档内容 -->
      <main class="archive-main">
        <div
          v-loading="loading"
          element-loading-text="加载归档中..."
          element-loading-background="rgba(255, 255, 255, 0.9)"
          class="archive-content"
        >
          <!-- 空状态 -->
          <div v-if="archiveList.length === 0 && !loading" class="no-data">
            <div class="empty-icon">
              <el-icon :size="80">
                <Calendar />
              </el-icon>
            </div>
            <h3>暂无归档数据</h3>
            <p>还没有发布任何文章，快去创作吧！</p>
            <router-link to="/" class="go-home-link">
              <el-button type="primary" round>返回首页</el-button>
            </router-link>
          </div>

          <!-- 归档列表 -->
          <div v-else class="timeline-container">
            <div class="timeline-line"></div>

            <div
              v-for="(archive, index) in archiveList"
              :key="archive.archive_date"
              class="timeline-item"
              :style="{ animationDelay: `${index * 0.1}s` }"
            >
              <!-- 时间轴节点 -->
              <div class="timeline-dot"></div>

              <!-- 归档卡片 -->
              <div class="archive-card">
                <!-- 归档头部 -->
                <div class="archive-header" @click="toggleArchive(archive.archive_date)">
                  <div class="archive-date-wrapper">
                    <div class="archive-year">
                      {{ archive.archive_date.split('-')[0] }}
                    </div>
                    <div class="archive-month">
                      {{ formatMonth(archive.archive_date.split('-')[1]) }}
                    </div>
                  </div>

                  <div class="archive-meta">
                    <span class="archive-count">
                      <el-icon><Document /></el-icon>
                      {{ archive.article_count }} 篇文章
                    </span>
                    <el-icon
                      :size="20"
                      class="toggle-icon"
                      :class="{ expanded: expandedArchives.includes(archive.archive_date) }"
                    >
                      <ArrowDown />
                    </el-icon>
                  </div>
                </div>

                <!-- 文章列表 -->
                <transition
                  name="expand"
                  @enter="onEnter"
                  @after-enter="onAfterEnter"
                  @leave="onLeave"
                >
                  <div
                    v-show="expandedArchives.includes(archive.archive_date)"
                    class="archive-articles"
                  >
                    <!-- 加载状态 -->
                    <div v-if="loadingArticles[archive.archive_date]" class="loading-articles">
                      <el-icon :size="32" class="is-loading">
                        <Loading />
                      </el-icon>
                      <span>加载文章中...</span>
                    </div>

                    <!-- 文章列表 -->
                    <div
                      v-else-if="
                        articlesByArchive[archive.archive_date] &&
                        articlesByArchive[archive.archive_date].length > 0
                      "
                      class="article-list"
                    >
                      <div
                        v-for="(article, articleIndex) in articlesByArchive[archive.archive_date]"
                        :key="article.id"
                        class="article-item"
                        :style="{ animationDelay: `${articleIndex * 0.05}s` }"
                      >
                        <!-- 文章封面 -->
                        <div v-if="article.coverUrl || article.coverImage" class="article-cover">
                          <img
                            :src="article.coverUrl || article.coverImage"
                            :alt="article.title"
                            loading="lazy"
                          />
                          <div class="cover-overlay"></div>
                        </div>

                        <!-- 文章信息 -->
                        <div class="article-info">
                          <router-link
                            :to="{ name: 'PublicSiteArticleDetail', params: { id: article.id } }"
                            class="article-title"
                          >
                            {{ article.title }}
                          </router-link>

                          <p class="article-summary">
                            {{
                              article.summary ||
                              stripHtmlTags(article.content).substring(0, 150) + '...'
                            }}
                          </p>

                          <div class="article-meta">
                            <span class="meta-item">
                              <el-icon :size="14"><Calendar /></el-icon>
                              {{ formatDate(article.createTime) }}
                            </span>
                            <span class="meta-item">
                              <el-icon :size="14"><View /></el-icon>
                              {{ article.viewCount || 0 }}
                            </span>
                            <span class="meta-item">
                              <el-icon :size="14"><ChatDotRound /></el-icon>
                              {{ article.commentCount || 0 }}
                            </span>
                            <span v-if="article.likeCount" class="meta-item">
                              <el-icon :size="14"><Star /></el-icon>
                              {{ article.likeCount }}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 无文章状态 -->
                    <div v-else class="no-articles">
                      <el-icon :size="48">
                        <DocumentDelete />
                      </el-icon>
                      <p>该月份暂无文章</p>
                    </div>
                  </div>
                </transition>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import SiteLayout from '@/components/SiteLayout.vue'
import { getArticleArchive, getArticlesByArchive } from '@/api/site/article'
import { logger } from '@/utils/logger'
import {
  Calendar,
  ArrowDown,
  Loading,
  View,
  ChatDotRound,
  ArrowLeft,
  Document,
  Star,
  DocumentDelete
} from '@element-plus/icons-vue'

// 响应式数据
const archiveList = ref<any[]>([])
const articlesByArchive = ref<Record<string, any[]>>({})
const loadingArticles = ref<Record<string, boolean>>({})
const expandedArchives = ref<string[]>([])
const totalArticles = ref(0)
const loading = ref(false)

// 获取归档数据
const loadArchiveData = async () => {
  try {
    loading.value = true
    const response = await getArticleArchive()
    archiveList.value = response.data || []

    // 计算总文章数
    totalArticles.value = archiveList.value.reduce(
      (total, item) => total + (item.article_count || 0),
      0
    )

    // 默认展开第一个归档
    if (archiveList.value.length > 0) {
      expandedArchives.value = [archiveList.value[0].archive_date]
      await loadArticlesByArchive(archiveList.value[0].archive_date)
    }
  } catch (error) {
    logger.error('获取归档数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 根据归档月份获取文章列表
const loadArticlesByArchive = async (archiveDate: string) => {
  try {
    loadingArticles.value[archiveDate] = true
    const [year, month] = archiveDate.split('-')
    const response = await getArticlesByArchive(Number(year), Number(month), {
      pageNum: 1,
      pageSize: 100
    })

    if (response && response.code === 200) {
      articlesByArchive.value[archiveDate] = response.rows || response.data || []
    }
  } catch (error) {
    logger.error('获取归档文章失败:', error)
    articlesByArchive.value[archiveDate] = []
  } finally {
    loadingArticles.value[archiveDate] = false
  }
}

// 切换归档展开/收起
const toggleArchive = async (archiveDate: string) => {
  const index = expandedArchives.value.indexOf(archiveDate)

  if (index > -1) {
    // 收起
    expandedArchives.value.splice(index, 1)
  } else {
    // 展开
    expandedArchives.value.push(archiveDate)

    // 如果还没有加载文章，则加载
    if (!articlesByArchive.value[archiveDate]) {
      await loadArticlesByArchive(archiveDate)
    }
  }
}

// 格式化月份
const formatMonth = (monthStr: string) => {
  const monthNames = [
    '一月',
    '二月',
    '三月',
    '四月',
    '五月',
    '六月',
    '七月',
    '八月',
    '九月',
    '十月',
    '十一月',
    '十二月'
  ]
  return monthNames[parseInt(monthStr) - 1]
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 去除HTML标签
const stripHtmlTags = (html: string) => {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '')
}

// 动画钩子
const onEnter = (el: Element) => {
  const element = el as HTMLElement
  element.style.height = '0'
  element.style.opacity = '0'
}

const onAfterEnter = (el: Element) => {
  const element = el as HTMLElement
  element.style.transition = 'all 0.3s ease'
  element.style.height = element.scrollHeight + 'px'
  element.style.opacity = '1'
}

const onLeave = (el: Element) => {
  const element = el as HTMLElement
  element.style.height = element.scrollHeight + 'px'
  element.style.opacity = '1'

  // 强制重绘
  element.offsetHeight

  element.style.transition = 'all 0.3s ease'
  element.style.height = '0'
  element.style.opacity = '0'
}

// 组件挂载时加载数据
onMounted(() => {
  loadArchiveData()
})
</script>

<style scoped>
/* 页面容器 */
.archive-page {
  padding-top: var(--mo-sp-10);
  min-height: 100vh;
  background: var(--mo-n50);
  padding-bottom: 0;
}

/* 页面头部 */
.page-header {
  position: relative;
  padding: 100px 20px 80px;
  text-align: center;
  color: var(--mo-n900);
  overflow: hidden;
  background: var(--mo-n50);
  border-bottom: 1px solid var(--mo-n200);
}

/* 页面头部底部波浪过渡 */
.page-header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 40px;
  background: var(--mo-n50);
  border-radius: var(--mo-r-md) var(--mo-r-md) 0 0;
}

.header-bg {
  display: none;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

.header-content {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
}

.header-icon {
  margin-bottom: var(--mo-sp-5);
  transition: color 0.2s ease;
}

@keyframes bounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.page-title {
  font-size: var(--mo-fs-6xl);
  margin-bottom: var(--mo-sp-4);
  font-weight: 800;
  animation: slideDown 0.8s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-description {
  font-size: 1.2rem;
  color: var(--mo-n600);
  margin-bottom: 30px;
  font-weight: 300;
  animation: slideDown 0.8s ease 0.2s both;
}

.highlight-number {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--mo-p600);
  text-shadow: none;
}

.back-button {
  animation: slideDown 0.8s ease 0.4s both;
}

.back-link {
  text-decoration: none;
}

/* 主要内容区域 */
.archive-main {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px 60px;
  position: relative;
  z-index: 2;
}

.archive-content {
  background: white;
  border-radius: var(--mo-r-md);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  min-height: 400px;
}

/* 空状态 */
.no-data {
  text-align: center;
  padding: var(--mo-sp-11) var(--mo-sp-5);
  color: var(--mo-n500);
}

.empty-icon {
  margin-bottom: var(--mo-sp-5);
  color: #ddd;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
}

.no-data h3 {
  font-size: var(--mo-fs-2xl);
  color: var(--mo-n500);
  margin-bottom: 10px;
}

.no-data p {
  font-size: var(--mo-fs-md);
  color: var(--mo-n500);
  margin-bottom: 30px;
}

.go-home-link {
  text-decoration: none;
}

/* 时间轴容器 */
.timeline-container {
  position: relative;
  padding: 40px 30px;
}

.timeline-line {
  position: absolute;
  left: 50px;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--mo-p600) 0%, var(--mo-p800) 100%);
  border-radius: 2px;
}

/* 时间轴项 */
.timeline-item {
  position: relative;
  margin-bottom: 30px;
  padding-left: var(--mo-sp-11);
  opacity: 0;
  animation: fadeInRight 0.6s ease forwards;
}

@keyframes fadeInRight {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.timeline-item:last-child {
  margin-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: 35px;
  top: 25px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  border: 4px solid var(--mo-p600);
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.2);
  z-index: 1;
  transition: all 0.3s ease;
}

.timeline-item:hover .timeline-dot {
  background: var(--mo-p600);
  box-shadow: 0 0 0 8px rgba(79, 70, 229, 0.3);
  transform: scale(1.2);
}

/* 归档卡片 */
.archive-card {
  background: white;
  border-radius: var(--mo-r-md);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s ease;
}

.archive-card:hover {
  box-shadow: 0 8px 30px rgba(79, 70, 229, 0.2);
}

/* 归档头部 */
.archive-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  background: var(--mo-n50);
  cursor: pointer;
  user-select: none;
  transition: all 0.3s ease;
  border: 1px solid var(--mo-n200);
}

.archive-header:hover {
  background: var(--mo-n100);
  border-color: var(--mo-p200);
}

.archive-date-wrapper {
  display: flex;
  align-items: baseline;
  gap: var(--mo-sp-2);
}

.archive-year {
  font-size: var(--mo-fs-4xl);
  font-weight: 800;
  color: var(--mo-p600);
}

.archive-month {
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--mo-n500);
}

.archive-meta {
  display: flex;
  align-items: center;
  gap: 15px;
}

.archive-count {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: white;
  border-radius: var(--mo-r-md);
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--mo-p600);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.15);
}

.toggle-icon {
  transition: transform 0.3s ease;
  color: var(--mo-p600);
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

/* 文章列表容器 */
.archive-articles {
  overflow: hidden;
  background: var(--mo-n50);
}

/* 加载状态 */
.loading-articles {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--mo-sp-3);
  padding: 50px;
  color: var(--mo-n500);
  font-size: var(--mo-fs-md);
}

/* 文章列表 */
.article-list {
  padding: 20px 25px;
  display: flex;
  flex-direction: column;
  gap: var(--mo-sp-5);
}

/* 文章项 */
.article-item {
  display: flex;
  gap: var(--mo-sp-5);
  background: white;
  border-radius: var(--mo-r-md);
  padding: var(--mo-sp-5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
  opacity: 0;
  animation: fadeInUp 0.5s ease forwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.article-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(79, 70, 229, 0.15);
  border-color: var(--mo-p600);
}

/* 文章封面 */
.article-cover {
  width: 200px;
  /* 与封面上传裁剪比例（3:1）一致 */
  aspect-ratio: 3 / 1;
  flex-shrink: 0;
  border-radius: var(--mo-r-md);
  overflow: hidden;
  position: relative;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.article-item:hover .article-cover img {
  transform: scale(1.1);
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(28, 25, 23, 0.18);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.article-item:hover .cover-overlay {
  opacity: 1;
}

/* 文章信息 */
.article-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.article-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--mo-n800);
  text-decoration: none;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 10px;
  transition: color 0.3s ease;
}

.article-title:hover {
  color: var(--mo-p600);
}

.article-summary {
  font-size: 0.9rem;
  color: var(--mo-n500);
  line-height: 1.6;
  margin: 0 0 14px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-meta {
  display: flex;
  gap: 18px;
  font-size: 0.85rem;
  color: var(--mo-n600);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-1);
  transition: color 0.3s ease;
}

.article-item:hover .meta-item {
  color: var(--mo-p600);
}

/* 无文章状态 */
.no-articles {
  text-align: center;
  padding: var(--mo-sp-8);
  color: var(--mo-n500);
}

.no-articles .el-icon {
  color: #ddd;
  margin-bottom: var(--mo-sp-3);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .page-title {
    font-size: var(--mo-fs-5xl);
  }

  .archive-main {
    max-width: 700px;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 80px 20px 50px;
  }

  .page-title {
    font-size: var(--mo-fs-4xl);
  }

  .page-description {
    font-size: var(--mo-fs-md);
  }

  .archive-main {
    margin-top: -30px;
  }

  .archive-content {
    border-radius: var(--mo-r-md);
  }

  .timeline-container {
    padding: 30px 20px;
  }

  .timeline-line {
    left: 30px;
  }

  .timeline-item {
    padding-left: 60px;
  }

  .timeline-dot {
    left: 22px;
    width: 14px;
    height: 14px;
  }

  .archive-header {
    padding: var(--mo-sp-4) var(--mo-sp-5);
  }

  .archive-year {
    font-size: 1.6rem;
  }

  .archive-month {
    font-size: var(--mo-fs-md);
  }

  .article-item {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
  }

  .article-meta {
    gap: var(--mo-sp-3);
  }
}

@media (max-width: 480px) {
  .page-header {
    padding: 60px 15px 40px;
  }

  .page-title {
    font-size: 1.6rem;
  }

  .page-description {
    font-size: 0.9rem;
  }

  .timeline-container {
    padding: 20px 15px;
  }

  .timeline-line {
    left: 20px;
  }

  .timeline-item {
    padding-left: 50px;
  }

  .timeline-dot {
    left: 13px;
    width: 12px;
    height: 12px;
  }

  .archive-header {
    padding: 14px 16px;
  }

  .archive-year {
    font-size: 1.4rem;
  }

  .archive-month {
    font-size: 0.9rem;
  }

  .archive-count {
    font-size: 0.8rem;
    padding: var(--mo-sp-1) var(--mo-sp-3);
  }

  .article-list {
    padding: 15px 16px;
  }

  .article-item {
    padding: var(--mo-sp-4);
  }

  .article-title {
    font-size: var(--mo-fs-md);
  }

  .article-summary {
    font-size: 0.85rem;
  }

  .article-meta {
    font-size: 0.8rem;
    gap: 10px;
  }
}

/* 深色主题 */
html.dark .archive-page {
  background: var(--mo-n900);
}

html.dark .page-header {
  background: var(--mo-n900);
  border-bottom-color: var(--mo-n800);
}

html.dark .archive-page .page-title {
  color: var(--mo-n100);
}

html.dark .archive-page .page-description {
  color: var(--mo-n400);
}

html.dark .page-header::after {
  background: var(--mo-n900);
}

html.dark .archive-content {
  background: var(--mo-n800);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

html.dark .no-data {
  color: var(--mo-n400);
}

html.dark .empty-icon {
  color: var(--mo-n400);
}

html.dark .no-data h3 {
  color: var(--mo-n200);
}

/* p600/p700 不参与深色重映射，深色下强调色文字统一改用 p300 */
html.dark .highlight-number,
html.dark .archive-year,
html.dark .toggle-icon {
  color: var(--mo-p300);
}

html.dark .timeline-line {
  background: linear-gradient(180deg, var(--mo-p600) 0%, var(--mo-p800) 100%);
}

html.dark .timeline-dot {
  background: var(--mo-n900);
  border-color: var(--mo-p600);
}

html.dark .timeline-item:hover .timeline-dot {
  background: var(--mo-p600);
}

html.dark .archive-card {
  background: var(--mo-n800);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

html.dark .archive-card:hover {
  box-shadow: 0 8px 30px rgba(79, 70, 229, 0.2);
}

html.dark .archive-header {
  background: var(--mo-n800);
  border-color: var(--mo-n700);
}

html.dark .archive-header:hover {
  background: var(--mo-n900);
  border-color: var(--mo-p500);
}

html.dark .archive-month {
  color: var(--mo-n400);
}

html.dark .archive-count {
  background: var(--mo-n800);
  color: var(--mo-p300);
}

html.dark .archive-articles {
  background: var(--mo-n900);
}

html.dark .article-item {
  background: var(--mo-n800);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

html.dark .article-item:hover {
  border-color: var(--mo-p600);
}

html.dark .article-title {
  color: var(--mo-n200);
}

html.dark .article-title:hover {
  color: var(--mo-p300);
}

html.dark .article-summary {
  color: var(--mo-n400);
}

html.dark .article-meta {
  color: var(--mo-n400);
}

html.dark .article-item:hover .meta-item {
  color: var(--mo-p300);
}

html.dark .no-articles {
  color: var(--mo-n400);
}

html.dark .no-articles .el-icon {
  color: var(--mo-n700);
}

/* 展开动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  height: 0 !important;
  opacity: 0;
}
</style>
