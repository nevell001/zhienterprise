<template>
  <div class="category-container">
    <!-- 站点导航 -->
    <SiteLayout>
      <!-- 分类头部 -->
      <div class="category-header">
        <div class="header-content">
          <div class="category-info">
            <h1 class="category-title">
              {{ categoryName || '文章分类' }}
            </h1>
            <p class="category-description">
              {{ categoryDescription || `浏览分类"${categoryName}"下的所有文章` }}
            </p>
            <div class="category-stats">
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
      <div class="category-main">
        <div class="main-content">
          <!-- 加载状态 -->
          <ArticleListSkeleton v-if="loading" />

          <!-- 空状态 -->
          <ArticleEmptyState
            v-else-if="articleList.length === 0"
            :icon="DocumentCopy"
            description="该分类下还没有文章，敬请期待..."
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

        <!-- 侧边栏 -->
        <div v-if="isSidebarEnabled" class="sidebar">
          <!-- 关于这个分类 -->
          <div class="sidebar-widget" :style="{ animationDelay: '0.1s' }">
            <h3 class="widget-title">
              <el-icon><Menu /></el-icon>
              关于这个分类
            </h3>
            <div class="category-about">
              <div class="category-icon">
                <el-icon><Menu /></el-icon>
              </div>
              <h4 class="category-name">
                {{ categoryName || '未命名分类' }}
              </h4>
              <p class="category-desc">
                {{ categoryDescription || '暂无描述' }}
              </p>
              <div class="category-meta">
                <span class="category-meta-item">
                  <el-icon><DocumentCopy /></el-icon>
                  {{ total }} 篇文章
                </span>
                <span class="category-meta-item">
                  <el-icon><Calendar /></el-icon>
                  最后更新 {{ lastUpdateTime }}
                </span>
              </div>
            </div>
          </div>

          <!-- 相关分类 -->
          <div class="sidebar-widget" :style="{ animationDelay: '0.2s' }">
            <h3 class="widget-title">
              <el-icon><Share /></el-icon>
              相关分类
            </h3>
            <ul class="related-categories">
              <li
                v-for="(category, index) in relatedCategories.slice(0, 8)"
                :key="category.id"
                class="category-item"
                :style="{ animationDelay: `${0.3 + index * 0.05}s` }"
              >
                <router-link
                  :to="`/category/${category.alias || category.id}`"
                  class="category-link"
                  :class="{ active: category.id === currentCategoryId }"
                >
                  <span class="category-name">{{ category.name }}</span>
                  <span class="category-count">({{ category.articleCount || 0 }})</span>
                </router-link>
              </li>
            </ul>
          </div>

          <!-- 热门标签 -->
          <div class="sidebar-widget" :style="{ animationDelay: '0.3s' }">
            <h3 class="widget-title">
              <el-icon><CollectionTag /></el-icon>
              热门标签
            </h3>
            <div class="tag-cloud">
              <router-link
                v-for="(tag, index) in popularTags.slice(0, 15)"
                :key="tag.id"
                :to="`/tag/${tag.id}`"
                class="tag-item"
                :style="{
                  animationDelay: `${0.4 + index * 0.03}s`,
                  fontSize: getTagFontSize(tag.article_count) + 'px',
                  transform: `scale(${getTagScale(tag.article_count)})`
                }"
                :title="`${tag.name} (${tag.article_count}篇文章)`"
              >
                {{ tag.name }}
              </router-link>
            </div>
          </div>

          <!-- 最新文章 -->
          <div class="sidebar-widget" :style="{ animationDelay: '0.4s' }">
            <h3 class="widget-title">
              <el-icon><StarFilled /></el-icon>
              最新文章
            </h3>
            <ul class="recent-articles">
              <li
                v-for="(article, index) in recentArticles.slice(0, 8)"
                :key="article.id"
                class="recent-item"
                :style="{ animationDelay: `${0.5 + index * 0.05}s` }"
              >
                <router-link
                  :to="`/article/${article.id}`"
                  class="recent-link"
                  :title="article.title"
                >
                  <span class="recent-date">{{ formatDate(article.createTime, 'MM-dd') }}</span>
                  <span class="recent-title">{{ article.title }}</span>
                </router-link>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 站点底部 -->
    </SiteLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'

import { useRoute } from 'vue-router'

import { ElMessage } from '@/plugins/element-plus-service'

import SiteLayout from '@/components/SiteLayout.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import ArticleEmptyState from '@/components/ArticleEmptyState.vue'
import ArticleListSkeleton from '@/components/ArticleListSkeleton.vue'
import SitePager from '@/components/SitePager.vue'

import { getCategoryDetail, getCategoryList } from '@/api/site/category'

import { getTagCloud } from '@/api/site/tag'

import { getArticleList } from '@/api/site/article'

import { getSiteSettingsAnonymous } from '@/api/site/setting'

import { useSiteSettingsStore } from '@/stores/siteSettings'
import { logger } from '@/utils/logger'
import {
  ArrowLeft,
  Calendar,
  CollectionTag,
  DocumentCopy,
  Menu,
  Share,
  StarFilled
} from '@element-plus/icons-vue'

const route = useRoute()

const siteSettingsStore = useSiteSettingsStore()

// 响应式数据

const articleList = ref([])

const categoryName = ref('')

const categoryDescription = ref('')

const total = ref(0)

const loading = ref(false)

const loadingMore = ref(false)

const currentCategoryId = ref(null)

// 路由参数（数字 ID 或 alias 伪静态别名）
const currentCategoryKey = ref('')

const lastUpdateTime = ref('')

const relatedCategories = ref([])

const popularTags = ref([])

const recentArticles = ref([])

const siteSettings = computed(() => siteSettingsStore.siteSettings)

// 前台功能开关统一走 store 判定（sidebar_enabled 默认开启）
const isSidebarEnabled = computed(() => siteSettingsStore.isFeatureEnabled('sidebar_enabled'))

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  categoryId: null,
  status: 1 // 只显示已发布的文章
})

// 获取分类文章列表
const loadCategoryArticles = async (append = false, page?: number) => {
  // 加载更多时页码只有在成功后提交，失败重试不会跳页
  const requestedPage = page ?? queryParams.pageNum
  try {
    loading.value = !append
    if (append) loadingMore.value = true

    const response = await getArticleList({
      ...queryParams,
      pageNum: requestedPage,
      categoryId: queryParams.categoryId
    })

    const newArticles = response.rows || []
    if (append) {
      articleList.value = [...articleList.value, ...newArticles]
    } else {
      articleList.value = newArticles
    }
    total.value = response.total || 0
    // 只有成功才提交页码：失败后"加载更多"不会跳过一页
    if (append) queryParams.pageNum = requestedPage
  } catch (error) {
    logger.error('获取分类文章失败:', error)
    ElMessage.error('获取文章列表失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
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

// 获取分类详情（按数字 ID 或 alias 解析），成功时回填数字 ID 供文章列表过滤
const loadCategoryDetail = async (): Promise<boolean> => {
  try {
    const response = await getCategoryDetail(currentCategoryKey.value)

    const category = response.data || response
    if (!category || !category.id) return false
    currentCategoryId.value = category.id
    queryParams.categoryId = category.id
    categoryName.value = category.name || ''
    categoryDescription.value = category.description || ''
    lastUpdateTime.value = formatDate(new Date().toISOString())
    return true
  } catch (error) {
    logger.error('获取分类详情失败:', error)
    return false
  }
}

// 按路由标识初始化分类页：解析成功加载该分类，失败（含 alias 未命中）回退全部文章
const initCategoryPage = async () => {
  const ok = await loadCategoryDetail()
  if (ok) {
    loadCategoryArticles()
    loadRelatedCategories()
  } else {
    logger.warn('分类不存在或未启用，回退全部文章:', currentCategoryKey.value)
    resetToAllCategories()
  }
}

// 重置为「所有分类」视图
const resetToAllCategories = () => {
  currentCategoryId.value = null
  currentCategoryKey.value = ''
  queryParams.categoryId = null
  queryParams.pageNum = 1
  categoryName.value = '所有分类'
  categoryDescription.value = '浏览所有分类下的文章'
  loadCategoryArticles()
  loadRelatedCategories()
}

// 获取相关分类
const loadRelatedCategories = async () => {
  try {
    const response = await getCategoryList({ pageNum: 1, pageSize: 10 })
    const categories = response.data || response.rows || []
    relatedCategories.value = categories.filter(cat => cat.id !== currentCategoryId.value)
  } catch (error) {
    logger.error('获取相关分类失败:', error)
  }
}

// 获取热门标签
const loadPopularTags = async () => {
  try {
    const response = await getTagCloud()
    popularTags.value = response.data || []
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
  loadCategoryArticles(true, queryParams.pageNum + 1)
}

// 分页处理
const handlePageChange = page => {
  queryParams.pageNum = page
  loadCategoryArticles()
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

// 监听路由变化，Vue 3 会自动清理
watch(
  () => route.params.id,
  newKey => {
    if (newKey) {
      // 路由参数兼容数字 ID（历史链接）与 alias 伪静态链接：先解析分类，再按解析出的数字 ID 加载文章
      currentCategoryKey.value = String(newKey)
      queryParams.pageNum = 1
      initCategoryPage()
    } else {
      // 没有分类标识，显示所有分类列表
      resetToAllCategories()
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
.category-container {
  padding-top: var(--mo-sp-10);
  min-height: 100vh;
  background: var(--mo-n50);
}

.category-header {
  background: var(--mo-n50);
  color: var(--mo-n900);
  padding: 60px 0;
  text-align: center;
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid var(--mo-n200);
}

.category-header::before {
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

.category-info {
  flex: 1;
  text-align: left;
}

.category-title {
  font-size: var(--mo-fs-6xl);
  margin: 0 0 var(--mo-sp-5) 0;
  font-weight: 700;
  color: var(--mo-n900);
}

.category-description {
  font-size: 1.3rem;
  color: var(--mo-n600);
  margin: 0 0 var(--mo-sp-5) 0;
  max-width: 600px;
  line-height: 1.6;
}

.category-stats {
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

.category-main {
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
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  border-color: rgba(79, 70, 229, 0.1);
}

.recent-title {
  margin: 0 0 15px 0;
  font-size: var(--mo-fs-2xl);
  line-height: 1.4;
  font-weight: 600;
}

.category-meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(102, 102, 102, 0.05);
  padding: var(--mo-sp-1) var(--mo-sp-2);
  border-radius: var(--mo-r-sm);
  transition: background-color 0.3s ease;
}

.category-meta-item:hover {
  background: rgba(79, 70, 229, 0.1);
  color: var(--mo-p600);
}

.category-meta-item .el-icon {
  font-size: var(--mo-fs-md);
  opacity: 0.8;
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin-top: var(--mo-sp-8);
  padding-bottom: var(--mo-sp-5);
}

.sidebar {
  min-width: 0;
}

.sidebar-widget {
  background: white;
  border-radius: var(--mo-r-md);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: var(--mo-sp-5);
  margin-bottom: var(--mo-sp-5);
}

.widget-title {
  font-size: 1.2rem;
  margin: 0 0 15px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--mo-p600);
  color: var(--mo-n800);
}

.category-about {
  text-align: center;
}

.category-icon {
  font-size: var(--mo-fs-6xl);
  color: var(--mo-p600);
  margin-bottom: 15px;
}

.category-about .category-name {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 10px 0 8px 0;
  color: var(--mo-n800);
}

.category-about .category-desc {
  color: var(--mo-n500);
  font-size: 0.9rem;
  margin-bottom: 15px;
  line-height: 1.5;
}

.category-meta {
  display: flex;
  flex-direction: column;
  gap: var(--mo-sp-2);
}

.category-meta .category-meta-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 0.85rem;
  color: var(--mo-n600);
  background: rgba(102, 102, 102, 0.05);
  padding: 6px 12px;
  border-radius: var(--mo-r-sm);
}

.related-categories {
  list-style: none;
  padding: 0;
  margin: 0;
}

.related-categories .category-item {
  margin-bottom: 10px;
}

.category-link {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  color: var(--mo-n800);
  text-decoration: none;
  transition: color 0.3s ease;
  border-radius: var(--mo-r-sm);
  padding: var(--mo-sp-2) var(--mo-sp-3);
}

.category-link:hover,
.category-link.active {
  background: rgba(79, 70, 229, 0.1);
  color: var(--mo-p600);
}

.category-count {
  color: var(--mo-n500);
  font-size: 0.9rem;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 15px 0;
}

.tag-item {
  border: 1px solid var(--mo-p100);
  background: var(--mo-p50);
  color: var(--mo-p700);
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 6px 12px;
  border-radius: var(--mo-r-md);
  font-size: 0.85rem;
  font-weight: 500;
  position: relative;
  overflow: hidden;
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.tag-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.6s ease;
}

.tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.tag-item:hover::before {
  left: 100%;
}

.recent-articles {
  list-style: none;
  padding: 0;
  margin: 0;
}

.recent-articles .recent-item {
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--mo-n100);
}

.recent-articles .recent-item:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.recent-link {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--mo-n800);
  text-decoration: none;
  font-size: 0.9rem;
  line-height: 1.4;
  transition: color 0.3s ease;
}

.recent-link:hover {
  color: var(--mo-p600);
}

.recent-date {
  font-size: 0.8rem;
  color: var(--mo-n500);
  min-width: 40px;
}

.recent-link .recent-title {
  flex: 1;
  margin: 0;
  font-size: 0.9rem;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .category-main {
    max-width: 100%;
    padding: 30px 20px;
    gap: 30px;
  }

  .sidebar {
    width: auto;
  }
}

@media (max-width: 1024px) {
  .category-main {
    gap: 25px;
  }

  .sidebar {
    width: auto;
  }
}

@media (max-width: 768px) {
  .category-header {
    padding: var(--mo-sp-8) 0;
  }

  .header-content {
    flex-direction: column;
    gap: 30px;
    text-align: center;
  }

  .category-info {
    text-align: center;
  }

  .category-title {
    font-size: 2.2rem;
  }

  .category-description {
    font-size: 1.1rem;
    padding: 0 var(--mo-sp-5);
  }

  .category-stats {
    justify-content: center;
  }

  .category-main {
    grid-template-columns: 1fr;
    padding: 20px 15px;
    gap: 25px;
  }

  .main-content {
    order: 1;
  }

  .sidebar {
    order: 2;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 15px;
  }

  .sidebar-widget {
    margin-bottom: 0;
    min-height: 200px;
  }

  .recent-item {
    margin-bottom: var(--mo-sp-5);
  }

  .recent-title {
    font-size: 1.4rem;
  }

  .tag-cloud {
    gap: var(--mo-sp-2);
  }

  .tag-item {
    padding: var(--mo-sp-1) var(--mo-sp-2);
    font-size: 0.85rem;
  }
}

@media (max-width: 480px) {
  .header-content {
    padding: 0 15px;
  }

  .category-title {
    font-size: 1.8rem;
    line-height: 1.2;
  }

  .category-description {
    font-size: var(--mo-fs-md);
    line-height: 1.4;
  }

  .category-stats {
    flex-direction: column;
    gap: var(--mo-sp-3);
    align-items: center;
  }

  .stat-item {
    font-size: 0.9rem;
  }

  .category-main {
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

  .sidebar {
    grid-template-columns: 1fr;
    gap: var(--mo-sp-3);
  }

  .sidebar-widget {
    padding: 15px;
    border-radius: var(--mo-r-sm);
  }

  .widget-title {
    font-size: var(--mo-fs-md);
    margin-bottom: var(--mo-sp-3);
  }
}

/* 深色主题适配 */
html.dark .category-container {
  background-color: var(--mo-n900);
}

html.dark .category-header {
  background: var(--mo-n900);
  border-bottom-color: var(--mo-n800);
}

html.dark .category-title {
  color: var(--mo-n100);
}

html.dark .category-description {
  color: var(--mo-n400);
}

html.dark .stat-item {
  color: var(--mo-n400);
}

html.dark .recent-item {
  background: var(--mo-n800);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  border-color: var(--mo-n800);
}

html.dark .recent-item:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
  border-color: rgba(79, 70, 229, 0.2);
}

html.dark .category-meta-item {
  background: rgba(255, 255, 255, 0.05);
  color: var(--mo-n400);
}

html.dark .category-meta-item:hover {
  background: rgba(79, 70, 229, 0.15);
  color: var(--mo-p300);
}

html.dark .sidebar-widget {
  background: var(--mo-n800);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

html.dark .widget-title {
  color: var(--mo-n200);
  border-bottom-color: var(--mo-p600);
}

html.dark .category-icon {
  color: var(--mo-p300);
}

html.dark .category-about .category-name {
  color: var(--mo-n200);
}

html.dark .category-about .category-desc {
  color: var(--mo-n400);
}

html.dark .category-meta .category-meta-item {
  color: var(--mo-n400);
  background: rgba(255, 255, 255, 0.05);
}

html.dark .category-link {
  color: var(--mo-n400);
}

html.dark .category-link:hover,
html.dark .category-link.active {
  background: rgba(79, 70, 229, 0.15);
  color: var(--mo-p300);
}

html.dark .category-count {
  color: var(--mo-n500);
}

html.dark .tag-item {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

html.dark .tag-item:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
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
html.dark .tag-item {
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

.recent-item,
.sidebar-widget,
.category-item,
.tag-item,
.recent-articles .recent-item {
  opacity: 0;
  animation: fadeInUp 0.6s ease forwards;
}
</style>
