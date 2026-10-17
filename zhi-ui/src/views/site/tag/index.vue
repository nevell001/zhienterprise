<template>
  <div class="tag-container">
    <!-- 站点导航 -->
    <SiteLayout>
      <!-- 标签头部 -->
      <div class="tag-header">
        <div class="header-content">
          <div class="tag-info">
            <div class="tag-icon-large">
              <el-icon><PriceTag /></el-icon>
            </div>
            <h1 class="tag-title">
              {{ tagName || '文章标签' }}
            </h1>
            <p class="tag-description">
              {{ tagDescription || `浏览标签"${tagName}"下的所有文章` }}
            </p>
            <div class="tag-stats">
              <span class="stat-item">
                <el-icon><DocumentCopy /></el-icon>
                {{ total }} 篇文章
              </span>
            </div>
          </div>

          <!-- 返回按钮 -->
          <div class="back-button">
            <router-link to="/" class="back-link">
              <el-button type="default" plain>
                <el-icon><ArrowLeft /></el-icon>
                返回首页
              </el-button>
            </router-link>
          </div>
        </div>
      </div>

      <!-- 主要内容区域 -->
      <div class="tag-main">
        <div class="main-content">
          <!-- 加载状态 -->
          <ArticleListSkeleton v-if="loading" />

          <!-- 空状态 -->
          <ArticleEmptyState
            v-else-if="articleList.length === 0"
            :icon="PriceTag"
            description="该标签下还没有文章，敬请期待..."
          />

          <!-- 文章列表 -->
          <div v-else class="article-list">
            <ArticleCard
              v-for="(article, index) in articleList"
              :key="article.id"
              :article="article"
              :index="index"
            />
          </div>

          <!-- 加载更多 -->
          <div v-if="articleList.length < total && !loading" class="load-more-container">
            <el-button type="primary" :loading="loadingMore" round @click="loadMoreArticles">
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </el-button>
          </div>

          <!-- 分页 -->
          <SitePager
            :total="total"
            :page-size="queryParams.pageSize"
            :page-num="queryParams.pageNum"
            @page-change="handlePageChange"
          />
        </div>
      </div>

      <!-- 站点底部 -->
    </SiteLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'

import { ElMessage } from '@/plugins/element-plus-service'

import { useRoute } from 'vue-router'

import SiteLayout from '@/components/SiteLayout.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import ArticleEmptyState from '@/components/ArticleEmptyState.vue'
import ArticleListSkeleton from '@/components/ArticleListSkeleton.vue'
import SitePager from '@/components/SitePager.vue'

import { getArticlesByTag, getTagDetail } from '@/api/site/tag'

import { getTagCloud } from '@/api/site/tag'

import { getArticleList } from '@/api/site/article'

import { getSiteSettingsAnonymous } from '@/api/site/setting'

import { useSiteSettingsStore } from '@/stores/siteSettings'
import { logger } from '@/utils/logger'
import {
  ArrowLeft,
  Calendar,
  DocumentCopy,
  PriceTag,
  Share,
  StarFilled
} from '@element-plus/icons-vue'

const route = useRoute()

const siteSettingsStore = useSiteSettingsStore()

// 响应式数据

const articleList = ref([])

const tagName = ref('')

const tagDescription = ref('')

const tagCreateTime = ref('')

const total = ref(0)

const loading = ref(false)

const loadingMore = ref(false)

const currentTagId = ref(null)

const relatedTags = ref([])

const popularTags = ref([])

const recentArticles = ref([])

const siteSettings = computed(() => siteSettingsStore.siteSettings)

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tagId: null,
  status: 1 // 只显示已发布的文章
})

// 获取标签文章列表
const loadTagArticles = async (append = false, page?: number) => {
  // 加载更多时页码只有在成功后提交，失败重试不会跳页
  const requestedPage = page ?? queryParams.pageNum
  try {
    loading.value = !append
    if (append) loadingMore.value = true

    let response

    // 如果有 tagId，调用 getArticlesByTag，否则调用普通的 getArticleList
    if (queryParams.tagId) {
      response = await getArticlesByTag(queryParams.tagId, {
        ...queryParams,
        pageNum: requestedPage
      })
    } else {
      response = await getArticleList({ ...queryParams, pageNum: requestedPage })
    }

    // 处理不同的响应格式
    let newArticles = []
    let totalCount = 0

    if (response && response.rows && Array.isArray(response.rows)) {
      // 标准的 TableDataInfo 格式
      newArticles = response.rows
      totalCount = response.total || 0
    } else if (response && Array.isArray(response)) {
      // 直接返回数组格式
      newArticles = response
      totalCount = response.length
    } else if (response && response.data && Array.isArray(response.data)) {
      // 包装在 data 字段中的格式
      newArticles = response.data
      totalCount = response.total || response.data.length
    } else {
      logger.warn('未知的响应格式:', response)
      newArticles = []
      totalCount = 0
    }
    // 只有成功才提交页码：失败后加载更多不会跳过一页

    if (append) queryParams.pageNum = requestedPage

    if (append) {
      articleList.value = [...articleList.value, ...newArticles]
    } else {
      articleList.value = newArticles
    }

    // 如果是首次加载，更新总数（避免分页时覆盖标签详情中的总数）
    if (!append && totalCount > 0) {
      total.value = totalCount
    }
  } catch (error) {
    logger.error('获取标签文章失败:', error)
    ElMessage.error('获取文章列表失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 获取标签详情
const loadTagDetail = async () => {
  try {
    const response = await getTagDetail(queryParams.tagId)

    const tag = response.data || response

    tagName.value = tag.name || ''
    tagDescription.value = tag.description || ''
    tagCreateTime.value = tag.createTime || new Date().toISOString()

    // 更新页面显示的文章数量
    if (tag.articleCount !== undefined && tag.articleCount !== null) {
      total.value = tag.articleCount
    }
  } catch (error) {
    logger.error('获取标签详情失败:', error)
    ElMessage.error('获取标签详情失败')
  }
}

// 获取相关标签
const loadRelatedTags = async () => {
  try {
    const response = await getTagCloud()
    const tags = response.data || []
    relatedTags.value = tags.filter(tag => tag.id !== currentTagId.value).slice(0, 12)
  } catch (error) {
    logger.error('获取相关标签失败:', error)
  }
}

// 获取热门标签
const loadPopularTags = async () => {
  try {
    const response = await getTagCloud()
    popularTags.value = (response.data || []).slice(0, 10)
  } catch (error) {
    logger.error('获取热门标签失败:', error)
  }
}

// 获取最新文章
const loadRecentArticles = async () => {
  try {
    const response = await getArticleList({ pageNum: 1, pageSize: 8, status: 1 })
    recentArticles.value = response.rows || []
  } catch (error) {
    logger.error('获取最新文章失败:', error)
  }
}

// 加载更多文章
const loadMoreArticles = () => {
  if (loadingMore.value || articleList.value.length >= total.value) return
  loadTagArticles(true, queryParams.pageNum + 1)
}

// 分页处理
const handlePageChange = page => {
  queryParams.pageNum = page
  loadTagArticles()
}

// 日期格式化
const formatDate = (dateString, format = 'full') => {
  if (!dateString) return ''
  const date = new Date(dateString)

  if (format === 'MM-dd') {
    return date.toLocaleDateString('zh-CN', {
      month: '2-digit',
      day: '2-digit'
    })
  }

  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 根据文章数量计算标签字体大小
const getTagFontSize = count => {
  if (count >= 10) return 16
  if (count >= 5) return 14
  if (count >= 2) return 12
  return 11
}

// 根据标签数量计算缩放比例
const getTagScale = count => {
  if (count >= 10) return 1.1
  if (count >= 5) return 1.05
  if (count >= 2) return 1.0
  return 0.95
}

// 加载站点设置
const loadSiteSettings = async () => {
  try {
    const response = await getSiteSettingsAnonymous()
    const settings = response || {}
    // 更新 siteSettingsStore
    siteSettingsStore.updateSiteSettings(settings)
  } catch (error) {
    logger.error('加载站点设置失败:', error)
    // 使用默认值
  }
}

// 监听路由变化，Vue 3 会自动清理
watch(
  () => route.params.id,
  newId => {
    if (newId) {
      // 有标签ID，加载特定标签的文章
      const tagId = Array.isArray(newId) ? newId[0] : newId
      currentTagId.value = parseInt(tagId)
      queryParams.tagId = parseInt(tagId)
      queryParams.pageNum = 1
      loadTagDetail()
      loadTagArticles()
      loadRelatedTags()
    } else {
      // 没有标签ID，显示所有标签列表
      currentTagId.value = null
      queryParams.tagId = null
      queryParams.pageNum = 1
      tagName.value = '所有标签'
      tagDescription.value = '浏览所有标签下的文章'
      loadTagArticles()
      loadRelatedTags()
    }
  },
  { immediate: true }
)

// 组件挂载时加载数据
onMounted(() => {
  loadSiteSettings()
  loadPopularTags()
  loadRecentArticles()
})
</script>

<style scoped>
.tag-container {
  padding-top: var(--mo-sp-10);
  min-height: 100vh;
  background: var(--mo-n50);
}

.tag-header {
  color: var(--mo-n900);
  padding: 60px 0;
  text-align: center;
  position: relative;
  overflow: hidden;
  background: var(--mo-n50);
  border-bottom: 1px solid var(--mo-n200);
}

.tag-header::before {
  content: none;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--mo-sp-5);
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--mo-sp-8);
}

.tag-info {
  flex: 1;
  text-align: left;
}

.tag-icon-large {
  font-size: var(--mo-fs-7xl);
  margin-bottom: var(--mo-sp-5);
  opacity: 0.9;
}

.tag-title {
  font-size: var(--mo-fs-6xl);
  margin: 0 0 var(--mo-sp-5) 0;
  font-weight: 700;
  color: var(--mo-n900);
}

.tag-description {
  font-size: 1.3rem;
  color: var(--mo-n600);
  margin: 0 0 var(--mo-sp-5) 0;
  max-width: 600px;
  line-height: 1.6;
}

.tag-stats {
  display: flex;
  gap: var(--mo-sp-5);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-2);
  font-size: var(--mo-fs-md);
  color: var(--mo-n500);
}

.stat-item .el-icon {
  font-size: 1.1rem;
}

.back-button {
  flex-shrink: 0;
}

.back-link {
  text-decoration: none;
}

.tag-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--mo-sp-8) var(--mo-sp-5);
  gap: var(--mo-sp-8);
}

.main-content {
  min-width: 0;
}

.article-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 380px), 1fr));
  gap: 30px;
  margin-bottom: var(--mo-sp-5);
}

.recent-item {
  background: white;
  border-radius: var(--mo-r-md);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.recent-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 25px rgba(0, 0, 0, 0.1);
  border-color: rgba(79, 70, 229, 0.1);
}

.tag-meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(102, 102, 102, 0.05);
  padding: var(--mo-sp-1) var(--mo-sp-2);
  border-radius: var(--mo-r-sm);
  transition: background-color 0.3s ease;
}

.tag-meta-item:hover {
  background: rgba(79, 70, 229, 0.1);
  color: var(--mo-p600);
}

.tag-meta-item .el-icon {
  font-size: var(--mo-fs-md);
  opacity: 0.8;
}

.recent-title {
  margin: 0 0 15px 0;
  font-size: var(--mo-fs-2xl);
  line-height: 1.4;
  font-weight: 600;
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin-top: var(--mo-sp-8);
  padding-bottom: var(--mo-sp-5);
}

@media (max-width: 1200px) {
  .tag-main {
    max-width: 100%;
    padding: 30px 20px;
    gap: 30px;
  }
}

@media (max-width: 1024px) {
  .tag-main {
    gap: 25px;
  }
}

@media (max-width: 768px) {
  .tag-header {
    padding: var(--mo-sp-8) 0;
  }

  .header-content {
    flex-direction: column;
    gap: 30px;
    text-align: center;
  }

  .tag-info {
    text-align: center;
  }

  .tag-icon-large {
    font-size: var(--mo-fs-6xl);
  }

  .tag-title {
    font-size: 2.2rem;
  }

  .tag-description {
    font-size: 1.1rem;
    padding: 0 var(--mo-sp-5);
  }

  .tag-stats {
    justify-content: center;
  }

  .tag-main {
    grid-template-columns: 1fr;
    padding: 20px 15px;
    gap: 25px;
  }

  .main-content {
    order: 1;
  }

  .recent-item {
    margin-bottom: var(--mo-sp-5);
  }

  .recent-title {
    font-size: 1.4rem;
  }

  .related-tags {
    gap: var(--mo-sp-2);
  }

  .related-tag-item {
    padding: 6px 10px;
    font-size: 0.8rem;
    min-height: 45px;
  }

  .popular-tag-item {
    padding: 6px 0;
  }
}

@media (max-width: 480px) {
  .header-content {
    padding: 0 15px;
  }

  .tag-icon-large {
    font-size: var(--mo-fs-5xl);
  }

  .tag-title {
    font-size: 1.8rem;
    line-height: 1.2;
  }

  .tag-description {
    font-size: var(--mo-fs-md);
    line-height: 1.4;
  }

  .tag-stats {
    flex-direction: column;
    gap: var(--mo-sp-3);
    align-items: center;
  }

  .stat-item {
    font-size: 0.9rem;
  }

  .tag-main {
    padding: 15px 10px;
  }

  .recent-item {
    border-radius: var(--mo-r-sm);
    box-shadow: 0 1px 8px rgba(0, 0, 0, 0.08);
  }

  .recent-title {
    font-size: var(--mo-fs-xl);
    margin-bottom: var(--mo-sp-3);
  }

  .widget-title {
    font-size: var(--mo-fs-md);
    margin-bottom: var(--mo-sp-3);
  }

  .tag-about {
    padding: 0 10px;
  }

  .tag-icon {
    width: 50px;
    height: 50px;
    font-size: 1.2rem;
  }

  .tag-about .tag-name {
    font-size: var(--mo-fs-md);
  }

  .tag-about .tag-desc {
    font-size: 0.85rem;
  }
}

/* 深色主题适配 */
html.dark .tag-container {
  background-color: var(--mo-n900);
}

html.dark .tag-header {
  background: var(--mo-n900);
  border-bottom-color: var(--mo-n800);
}

html.dark .tag-title {
  color: var(--mo-n100);
}

html.dark .tag-description {
  color: var(--mo-n400);
}

html.dark .stat-item {
  color: var(--mo-n400);
}

html.dark .tag-icon-large {
  color: rgba(255, 255, 255, 0.9);
}

html.dark .recent-item {
  background: var(--mo-n800);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border-color: var(--mo-n800);
}

html.dark .recent-item:hover {
  box-shadow: 0 6px 25px rgba(0, 0, 0, 0.4);
  border-color: rgba(79, 70, 229, 0.2);
}

html.dark .tag-meta-item {
  background: rgba(255, 255, 255, 0.05);
  color: var(--mo-n400);
}

html.dark .tag-meta-item:hover {
  background: rgba(79, 70, 229, 0.15);
  color: var(--mo-p300);
}

html.dark .widget-title {
  color: var(--mo-n200);
  border-bottom-color: var(--mo-p600);
}

html.dark .tag-about .tag-name {
  color: var(--mo-n200);
}

html.dark .tag-about .tag-desc {
  color: var(--mo-n400);
}

html.dark .tag-meta .tag-meta-item {
  color: var(--mo-n400);
  background: rgba(255, 255, 255, 0.05);
}

html.dark .related-tag-item {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

html.dark .related-tag-item:hover,
html.dark .related-tag-item.active {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
}

html.dark .popular-tag-item {
  border-bottom-color: var(--mo-n800);
}

html.dark .tag-rank {
  color: var(--mo-p300);
}

html.dark .tag-link {
  color: var(--mo-n200);
}

html.dark .tag-article-count {
  color: var(--mo-n500);
}

html.dark .recent-articles .recent-item {
  border-bottom-color: var(--mo-n800);
}

html.dark .recent-link {
  color: var(--mo-n400);
}

html.dark .recent-link:hover {
  color: var(--mo-p300);
}

html.dark .recent-date {
  color: var(--mo-n500);
}

/* p600/p700 不参与深色重映射，深色下强调色文字统一改用 p300 */
html.dark .related-tag-item,
html.dark .tag-icon,
html.dark .tag-link {
  color: var(--mo-p300);
}

/* 动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
