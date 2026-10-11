<template>
  <BlogLayout ref="blogLayoutRef">
    <!-- 首页可能被自定义页面接管：设置未就绪时先给骨架，避免先闪文章列表再整体切换 -->
    <CustomPagePanel v-if="homeMode === 'booting'" loading :page="null" :not-found="false" />

    <CustomPagePanel
      v-else-if="homeMode === 'custom'"
      minimal
      :page="homePage"
      :loading="homePageLoading"
      :not-found="homePageNotFound"
      @retry="loadHomePage"
    />

    <div v-else class="blog-home-v2 mo-home-page">
      <header class="home-hero">
        <h1>记录与分享</h1>
        <p>
          {{
            blogSettings.blog_desc ||
            '在这里，每一篇文章都是一次思考的沉淀。无论技术随笔、生活感悟还是行业观察，都值得被认真对待。'
          }}
        </p>
        <div v-if="isSearchEnabled" class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索文章、作者、标签..."
            @keyup.enter="handleSearch"
          />
          <button type="button" @click="handleSearch">搜索</button>
        </div>
      </header>

      <main class="home-layout">
        <section>
          <article
            v-for="article in articles"
            :key="article.id"
            class="article-card"
            @click="goToArticle(article.id)"
          >
            <div
              class="thumb"
              :style="{
                background: article.coverUrl
                  ? `url(${article.coverUrl}) center/cover`
                  : fallbackGradient(article.id)
              }"
            ></div>
            <div class="body">
              <div
                v-if="Number(article.isTop) === 1 || Number(article.isRecommend) === 1"
                class="flag-row"
              >
                <span v-if="Number(article.isTop) === 1" class="flag flag-top">置顶</span>
                <span v-if="Number(article.isRecommend) === 1" class="flag flag-rec">推荐</span>
              </div>
              <div v-if="article.tags?.length" class="tags">
                <router-link
                  v-for="(tag, index) in article.tags.slice(0, 2)"
                  :key="tag.id"
                  :to="`/blog/tag/${tag.id}`"
                  class="tag"
                  :class="tagClass(Number(index))"
                  @click.stop
                >
                  {{ tag.name }}
                </router-link>
              </div>
              <h2 class="title">{{ article.title }}</h2>
              <p class="excerpt">
                {{ article.summary || '这篇文章暂时没有摘要，点击进入阅读全文。' }}
              </p>
              <div class="meta">
                <span
                  class="author"
                  :class="{ clickable: article.authorId }"
                  @click.stop="goAuthor(article.authorId)"
                >
                  <span class="avatar-sm">{{ (article.authorName || '作').charAt(0) }}</span>
                  {{ article.authorName || '作者' }}
                </span>
                <span class="dot-sep">·</span>
                <span>{{ formatDate(article.createTime) }}</span>
                <span class="dot-sep">·</span>
                <span>👁 {{ article.viewCount || 0 }}</span>
                <span class="dot-sep">·</span>
                <span>💬 {{ article.commentCount || 0 }}</span>
              </div>
            </div>
          </article>

          <div v-if="total > 0" class="pagination">
            <button
              class="pg"
              :disabled="currentPage <= 1"
              @click="handleCurrentChange(currentPage - 1)"
            >
              ‹
            </button>
            <template v-for="(item, idx) in pageItems" :key="`${item}-${idx}`">
              <span v-if="item === '…'" class="pg pg-ellipsis">…</span>
              <button
                v-else
                class="pg"
                :class="{ active: item === currentPage }"
                :disabled="item === currentPage"
                @click="handleCurrentChange(item as number)"
              >
                {{ item }}
              </button>
            </template>
            <button
              class="pg"
              :disabled="currentPage >= totalPages"
              @click="handleCurrentChange(currentPage + 1)"
            >
              ›
            </button>
          </div>
        </section>

        <aside v-if="isSidebarEnabled">
          <div class="sidebar-widget">
            <div class="widget-title">分类导航</div>
            <div class="cat-list">
              <router-link :to="listHomePath" class="cat-item active">
                全部文章
                <span class="count">{{ total }}</span>
              </router-link>
              <router-link
                v-for="category in categories"
                :key="category.id"
                :to="`/blog/category/${category.id}`"
                class="cat-item"
              >
                {{ category.name }}
                <span class="count">{{ category.articleCount || category.count || 0 }}</span>
              </router-link>
            </div>
          </div>

          <div class="sidebar-widget">
            <div class="widget-title">热门标签</div>
            <div class="tag-cloud">
              <router-link
                v-for="(tag, index) in tags"
                :key="tag.id || tag.name"
                :to="`/blog/tag/${tag.id}`"
                class="tc"
                :class="{ lg: index % 5 === 0, md: index % 3 === 0 }"
              >
                {{ tag.name }}
              </router-link>
            </div>
          </div>

          <div class="sidebar-widget">
            <div class="widget-title">热门文章</div>
            <div
              v-for="(article, index) in popularArticles.slice(0, 5)"
              :key="article.id"
              class="hot-item"
              @click="goToArticle(article.id)"
            >
              <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <div>
                <div class="htitle">{{ article.title }}</div>
                <div class="hmeta">
                  👁 {{ article.viewCount || 0 }} · 💬 {{ article.commentCount || 0 }}
                </div>
              </div>
            </div>
          </div>
        </aside>
      </main>

      <!-- 联系弹窗 -->
      <div v-if="showContact" class="contact-modal" @click.self="showContact = false">
        <div class="contact-card">
          <button class="contact-close" @click="showContact = false">&times;</button>
          <h3>联系我们</h3>
          <p>邮箱：{{ blogSettings.blog_email || 'hello@example.com' }}</p>
        </div>
      </div>
    </div>
  </BlogLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import BlogLayout from '@/components/BlogLayout.vue'
import CustomPagePanel from './page/CustomPagePanel.vue'
import { getArticleList, getBlogSettings, getHotArticles } from '@/api/blog'
import { getCategoryList } from '@/api/blog/category'
import { getTagCloud } from '@/api/blog/tag'
import { getPublishedPageBySlug, type BlogPage } from '@/api/blog/page'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { logger } from '@/utils/logger'
import { parseTime } from '@/utils/zhi'
import { buildPageItems } from '@/utils/pageItems'
import { hasCustomHomePage } from '@/utils/blogNav'
import { applySeo, canonicalUrl } from '@/utils/seo'

const router = useRouter()
const route = useRoute()
const blogSettingsStore = useBlogSettingsStore()

const articles = ref([])
const popularArticles = ref([])
const categories = ref([])
const tags = ref([])
const blogSettings = computed(() => blogSettingsStore.blogSettings)
const loading = ref(true)
const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)
const searchKeyword = ref('')
const showContact = ref(false)
const blogLayoutRef = ref<any>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const pageItems = computed<(number | '…')[]>(() =>
  buildPageItems(totalPages.value, currentPage.value)
)

// 前台功能开关统一走 store 判定（search_enabled / sidebar_enabled 默认开启）
const isSearchEnabled = computed(() => blogSettingsStore.isFeatureEnabled('search_enabled'))
const isSidebarEnabled = computed(() => blogSettingsStore.isFeatureEnabled('sidebar_enabled'))

// —— 企业官网模式：home_page_slug 非空时本页渲染自定义页面，文章列表挪到 /blog/articles ——
const settingsReady = ref(blogSettingsStore.lastUpdate > 0)
const isCustomHomePage = computed(() =>
  hasCustomHomePage(blogSettingsStore.blogSettings.home_page_slug)
)
const isArticleListRoute = computed(() => route.name === 'PublicBlogArticleList')
const homeSlug = computed(() =>
  isCustomHomePage.value ? String(blogSettingsStore.blogSettings.home_page_slug).trim() : ''
)

const homePage = ref<BlogPage | null>(null)
const homePageLoading = ref(false)
const homePageNotFound = ref(false)
const loadedHomeSlug = ref('')

const showCustomHome = computed(() => !isArticleListRoute.value && isCustomHomePage.value)

// booting 只出现在首页路由：/blog/articles 明确是列表页，不等设置
const homeMode = computed<'booting' | 'custom' | 'list'>(() => {
  if (isArticleListRoute.value) return 'list'
  if (!settingsReady.value) return 'booting'
  if (!showCustomHome.value || homePageNotFound.value) return 'list'
  return 'custom'
})

// 侧栏「全部文章」入口：首页被接管后指向列表页，否则就是首页自身
const listHomePath = computed(() => (isCustomHomePage.value ? '/blog/articles' : '/blog'))

const loadHomePage = async () => {
  const slug = homeSlug.value
  if (!slug) return
  homePageLoading.value = true
  homePageNotFound.value = false
  try {
    const response = await getPublishedPageBySlug(slug)
    if (!response || response.code !== 200 || !response.data) {
      homePage.value = null
      homePageNotFound.value = true
      return
    }

    homePage.value = response.data
    // 首页 SEO 用自定义页面的 TDK 覆盖 BlogLayout 的站点默认值；页面不存在时降级回列表
    const siteName = blogSettings.value.blog_name || '知企建站系统'
    applySeo({
      title: homePage.value.seoTitle || `${homePage.value.title} - ${siteName}`,
      description: homePage.value.seoDescription || homePage.value.summary || '',
      keywords: homePage.value.seoKeywords || '',
      canonical: canonicalUrl()
    })
  } catch (error) {
    logger.error('加载首页自定义页面失败:', error)
    homePage.value = null
    homePageNotFound.value = true
  } finally {
    homePageLoading.value = false
    loadedHomeSlug.value = slug
  }
}

// 首次进入或从 /blog/articles 返回首页时按需加载；同一 slug 只加载一次
watch(
  [showCustomHome, homeSlug],
  () => {
    if (!showCustomHome.value || !homeSlug.value) return
    if (homePageLoading.value) return
    if (homePage.value && loadedHomeSlug.value === homeSlug.value) return
    loadHomePage()
  },
  { immediate: true }
)

const formatDate = (date: string) => {
  return parseTime(date, '{y}-{m}-{d}')
}

const tagClass = (index: number) => {
  return index === 0 ? 'tag-blue' : 'tag-gray'
}

const fallbackGradient = (id: number) => {
  return Math.abs(Number(id) || 0) % 2 === 0
    ? 'linear-gradient(135deg,var(--mo-p50),var(--mo-n50))'
    : 'linear-gradient(135deg,var(--mo-n100),var(--mo-p100))'
}

const goToArticle = (id: number) => {
  if (id) router.push(`/blog/article/${id}`)
}

// 跳转到作者主页
const goAuthor = (authorId?: number) => {
  if (authorId) router.push(`/blog/author/${authorId}`)
}

const scrollToAllArticles = () => {
  document.getElementById('all-articles')?.scrollIntoView({ behavior: 'smooth' })
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadArticles()
}

// 前往独立搜索页
const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  router.push({ path: '/blog/search', query: keyword ? { q: keyword } : {} })
}

const loadArticles = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: 1
    }
    const response = await getArticleList(params)
    articles.value = response.rows || []
    total.value = response.total || 0
  } catch (error) {
    logger.error('加载文章失败:', error)
  } finally {
    loading.value = false
  }
}

const loadPopularArticles = async () => {
  try {
    const response = await getHotArticles({ pageSize: 6 })
    popularArticles.value = response.rows || []
  } catch (error) {
    logger.error('加载热门文章失败:', error)
  }
}

const loadSidebarData = async () => {
  try {
    const [categoryResponse, tagResponse] = await Promise.all([
      getCategoryList({ pageNum: 1, pageSize: 8 }),
      getTagCloud()
    ])
    categories.value = categoryResponse.rows || categoryResponse.data || []
    const tagResult = tagResponse as any
    tags.value = Array.isArray(tagResult) ? tagResult : tagResult.data || []
  } catch (error) {
    logger.error('加载侧栏数据失败:', error)
    categories.value = []
    tags.value = []
  }
}

const loadBlogSettings = async () => {
  try {
    const response = await getBlogSettings()
    const settings = response.data || {}
    blogSettingsStore.updateBlogSettings(settings)
  } catch (error) {
    logger.error('加载设置失败:', error)
  } finally {
    // 失败时按 store 默认值（博客模式）渲染，不能让骨架屏卡住
    settingsReady.value = true
  }
}

onMounted(async () => {
  await Promise.all([loadArticles(), loadPopularArticles(), loadBlogSettings(), loadSidebarData()])
})
</script>

<style scoped>
.mo-home-page {
  min-height: 100vh;
  padding-top: 60px;
  background: var(--mo-n50);
  color: var(--mo-n800);
  font-family: var(--mo-font-sans);
}

.mo-home-page .home-hero {
  padding: 56px 32px;
  background: linear-gradient(135deg, var(--mo-p600), var(--mo-p800));
  color: #fff;
  text-align: center;
}

.mo-home-page .home-hero h1 {
  margin: 0 0 10px;
  color: #fff;
  font-size: var(--mo-fs-4xl);
  font-weight: 700;
  letter-spacing: 0;
}

.mo-home-page .home-hero p {
  max-width: 520px;
  margin: 0 auto;
  opacity: 0.85;
  font-size: 15px;
  line-height: 1.7;
}

.mo-home-page .home-hero .search-box {
  display: flex;
  align-items: center;
  max-width: 440px;
  width: 100%;
  margin: var(--mo-sp-6) auto 0;
  padding: var(--mo-sp-1);
  border: none;
  border-radius: var(--mo-r-full);
  background: #fff;
}

.mo-home-page .home-hero .search-box input {
  flex: 1;
  min-width: 0;
  padding: 10px 18px;
  border: none;
  outline: none;
  background: transparent;
  color: var(--mo-n800);
  font-size: var(--mo-fs-sm);
}

.mo-home-page .home-hero .search-box button {
  padding: 9px 24px;
  border: none;
  border-radius: var(--mo-r-full);
  background: var(--el-color-primary, #409eff);
  /* 与主色配对的前景色（默认主题亮主色 → 深墨；Mo-Blog 靛蓝 → 白） */
  color: var(--mo-on-primary, #fff);
  font-size: 13px;
  font-weight: 500;
}

.mo-home-page .home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 28px;
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--mo-sp-7);
}

.mo-home-page .article-card {
  margin-bottom: 18px;
  overflow: hidden;
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.mo-home-page .article-card:hover {
  border-color: var(--mo-p300);
  box-shadow: var(--mo-shadow-md);
  transform: translateY(-1px);
}

.mo-home-page .article-card .thumb {
  /* 与封面上传裁剪比例（3:1）一致：裁什么显示什么，避免 center/cover 二次裁掉选区 */
  aspect-ratio: 3 / 1;
  background-position: center;
  background-size: cover;
}

.mo-home-page .article-card .body {
  padding: 22px;
}

.mo-home-page .article-card .tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}

.mo-home-page .article-card .title {
  margin: 0 0 var(--mo-sp-2);
  color: var(--mo-n900);
  font-size: var(--mo-fs-lg);
  font-weight: 600;
  line-height: 1.4;
}

.mo-home-page .article-card .excerpt {
  display: -webkit-box;
  margin: 0 0 14px;
  overflow: hidden;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.mo-home-page .article-card .meta {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-3);
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
}

.mo-home-page .article-card .meta .author {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--mo-n600);
  font-weight: 500;
}
.mo-home-page .article-card .meta .author.clickable {
  cursor: pointer;
}
.mo-home-page .article-card .meta .author.clickable:hover {
  color: var(--mo-p600);
}

.mo-home-page .avatar-sm {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--mo-p100);
  color: var(--mo-p600);
  font-size: 10px;
  font-weight: 600;
}

.mo-home-page .dot-sep {
  color: var(--mo-n500);
}

.mo-home-page .sidebar-widget {
  margin-bottom: 18px;
  padding: var(--mo-sp-5);
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: #fff;
}

.mo-home-page .widget-title {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-2);
  margin-bottom: 14px;
  color: var(--mo-n800);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
}

.mo-home-page .widget-title::before {
  content: '';
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: var(--mo-p500);
}

.mo-home-page .cat-list {
  display: flex;
  flex-direction: column;
  gap: var(--mo-sp-1);
}

.mo-home-page .cat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--mo-sp-2) var(--mo-sp-3);
  border-radius: var(--mo-r-md);
  color: var(--mo-n600);
  font-size: 13px;
  cursor: pointer;
  text-decoration: none;
}

.mo-home-page .cat-item:hover,
.mo-home-page .cat-item.active {
  background: var(--mo-p50);
  color: var(--mo-p700);
  font-weight: 500;
}

.mo-home-page .cat-item .count {
  padding: 2px 8px;
  border-radius: var(--mo-r-full);
  background: var(--mo-n100);
  color: var(--mo-n600);
  font-size: 11px;
}

.mo-home-page .tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mo-sp-2);
}

.mo-home-page .tc {
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: var(--mo-r-full);
  background: var(--mo-n100);
  color: var(--mo-n600);
  font-size: var(--mo-fs-xs);
  text-decoration: none;
}

.mo-home-page .tc:hover,
.mo-home-page .tc.lg {
  background: var(--mo-p50);
  color: var(--mo-p700);
}

.mo-home-page .tc.md {
  background: var(--mo-n100);
  color: var(--mo-n600);
}

.mo-home-page .hot-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: var(--mo-sp-3);
  cursor: pointer;
}

.mo-home-page .rank {
  width: 24px;
  flex-shrink: 0;
  color: var(--mo-n500);
  font-size: var(--mo-fs-lg);
  font-weight: 700;
  text-align: center;
}

.mo-home-page .rank.top {
  color: var(--mo-p600);
}

.mo-home-page .htitle {
  color: var(--mo-n800);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}

.mo-home-page .hot-item:hover .htitle {
  color: var(--mo-p600);
}

.mo-home-page .hmeta {
  margin-top: 2px;
  color: var(--mo-n500);
  font-size: 11px;
}

.mo-home-page .pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 28px 0;
}

.mo-home-page .pg {
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: var(--mo-n600);
  font-size: 13px;
  cursor: pointer;
}

.mo-home-page .pg:disabled:not(.active) {
  opacity: 0.45;
  cursor: not-allowed;
}

.mo-home-page .pg-ellipsis {
  border: none;
  background: transparent;
  cursor: default;
}

.mo-home-page .pg.active {
  border-color: var(--el-color-primary, #409eff);
  background: var(--el-color-primary, #409eff);
  /* 亮色主色（#409eff/#00d4ff）上的白字不达 AA，改用与主色配对的前景色 */
  color: var(--mo-on-primary, #fff);
}

/* 置顶/推荐标识（与上传裁剪比例、分页排序同期的后台能力前台化） */
.mo-home-page .flag-row {
  display: flex;
  gap: var(--mo-sp-1);
  margin-bottom: var(--mo-sp-2);
}

.mo-home-page .flag {
  padding: var(--mo-sp-1) var(--mo-sp-2);
  border-radius: var(--mo-r-full);
  font-size: var(--mo-fs-xs);
  font-weight: 600;
  line-height: 1.6;
}

.mo-home-page .flag-top {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
  border: 1px solid var(--el-color-danger-light-5);
}

.mo-home-page .flag-rec {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-5);
}

.mo-home-page .tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  border-radius: var(--mo-r-full);
  font-size: var(--mo-fs-xs);
  font-weight: 500;
}

.mo-home-page .tag-blue {
  background: var(--mo-p50);
  color: var(--mo-p700);
}

.mo-home-page .tag-gray {
  background: var(--mo-n100);
  color: var(--mo-n600);
}

.mo-home-page .search-result-note {
  margin-bottom: var(--mo-sp-4);
  color: var(--mo-n600);
  font-size: 13px;
}

.mo-home-page .search-result-note button {
  margin-left: var(--mo-sp-3);
  color: var(--mo-p600);
}

@media (max-width: 768px) {
  .mo-home-page .home-hero {
    padding: var(--mo-sp-7) var(--mo-sp-4);
  }

  .mo-home-page .home-hero h1 {
    font-size: var(--mo-fs-2xl);
  }

  .mo-home-page .home-layout {
    grid-template-columns: 1fr;
    gap: var(--mo-sp-4);
    padding: var(--mo-sp-4);
  }
}

.contact-modal {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--mo-sp-5);
  background: rgba(28, 25, 23, 0.42);
  backdrop-filter: blur(4px);
}

.contact-card {
  position: relative;
  width: min(400px, 100%);
  padding: var(--mo-sp-7);
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: #fff;
  box-shadow: var(--mo-shadow-md);
}

.contact-card h3 {
  margin: 0 0 14px;
  color: var(--mo-n900);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0;
}

.contact-card p {
  margin: 0;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
  line-height: 1.7;
}

.contact-close {
  position: absolute;
  top: 14px;
  right: 16px;
  border: none;
  background: transparent;
  color: var(--mo-n500);
  cursor: pointer;
  font-size: var(--mo-fs-2xl);
  line-height: 1;
}

.contact-close:hover {
  color: var(--mo-p600);
}

/* 深色模式 */
/* 默认主题深色：将深色覆盖规则引用的 --mo-n* 映射到 --el-* 变量（Tech Blue 深蓝），
   使博客首页与默认主题深色配色一致。Mo-Blog 主题深色用 :root 棕色原值（不需映射）。 */
html.dark .mo-home-page {
  --mo-n900: var(--el-bg-color);
  --mo-n800: var(--el-bg-color-overlay);
  --mo-n700: var(--el-border-color);
  --mo-n500: var(--el-text-color-regular);
  --mo-n400: var(--el-text-color-regular);
  --mo-n300: var(--el-text-color-regular);
  --mo-n200: var(--el-text-color-primary);
  --mo-n100: var(--el-text-color-primary);
  --mo-p50: rgba(0, 212, 255, 0.12);
  --mo-p100: rgba(0, 212, 255, 0.2);
  --mo-p300: var(--el-color-primary);
  --mo-p500: var(--el-color-primary);
  --mo-p600: var(--el-color-primary);
  --mo-p700: var(--el-color-primary);
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

/* Mo-Blog 主题深色：恢复棕色色阶（覆盖默认主题映射）
   n300/n400 是次要文字色，深色下必须是浅灰（原 #57534e/#78716c 比背景还深） */
html.dark.theme-mo-blog .mo-home-page {
  --mo-n900: #1c1917;
  --mo-n800: #292524;
  --mo-n700: #44403c;
  --mo-n500: #a8a29e;
  --mo-n400: #a8a29e;
  --mo-n300: #a8a29e;
  --mo-n200: #d6d3d1;
  --mo-n100: #f5f5f4;
  --mo-p50: rgba(99, 102, 241, 0.12);
  --mo-p100: rgba(99, 102, 241, 0.2);
  --mo-p300: #a5b4fc;
  --mo-p500: #6366f1;
  --mo-p600: #4f46e5;
  --mo-p700: #4338ca;
  background: var(--mo-n900);
  color: var(--mo-n200);
}

html.dark .mo-home-page .home-hero {
  background: linear-gradient(135deg, var(--mo-p800), var(--mo-p900));
}

html.dark .mo-home-page .home-hero .search-box {
  background: var(--mo-n800);
}

html.dark .mo-home-page .home-hero .search-box input {
  color: var(--mo-n100);
}

html.dark .mo-home-page .article-card {
  background: var(--mo-n800);
  border-color: var(--mo-n700);
}

html.dark .mo-home-page .article-card:hover {
  border-color: var(--mo-p600);
}

/* 无封面图时占位背景避免浅色残留 */
html.dark .mo-home-page .article-card .thumb {
  background-color: var(--mo-n700) !important;
}

html.dark .mo-home-page .article-card .title {
  color: var(--mo-n100);
}

html.dark .mo-home-page .article-card .excerpt {
  color: var(--mo-n500);
}

html.dark .mo-home-page .article-card .meta {
  color: var(--mo-n500);
}

html.dark .mo-home-page .article-card .meta .author {
  color: var(--mo-n400);
}

html.dark .mo-home-page .sidebar-widget {
  background: var(--mo-n800);
  border-color: var(--mo-n700);
}

html.dark .mo-home-page .widget-title {
  color: var(--mo-n100);
}

html.dark .mo-home-page .cat-item {
  color: var(--mo-n400);
}

html.dark .mo-home-page .cat-item:hover,
html.dark .mo-home-page .cat-item.active {
  background: var(--mo-p50);
  color: var(--mo-p300);
}

html.dark .mo-home-page .cat-item .count {
  background: var(--mo-n700);
  color: var(--mo-n200);
}

html.dark .mo-home-page .tc {
  background: var(--mo-n700);
  color: var(--mo-n200);
}

html.dark .mo-home-page .tc:hover,
html.dark .mo-home-page .tc.lg {
  background: var(--mo-p50);
  color: var(--mo-p300);
}

html.dark .mo-home-page .htitle {
  color: var(--mo-n200);
}

html.dark .mo-home-page .htitle:hover {
  color: var(--mo-p300);
}

html.dark .mo-home-page .hmeta {
  color: var(--mo-n500);
}

html.dark .mo-home-page .pg {
  background: var(--mo-n800);
  border-color: var(--mo-n700);
  color: var(--mo-n400);
}

html.dark .mo-home-page .pg.active {
  background: var(--el-color-primary, #409eff);
  border-color: var(--el-color-primary, #409eff);
  color: var(--mo-on-primary, #fff);
}

html.dark .mo-home-page .tag-blue {
  background: var(--mo-p50);
  color: var(--mo-p300);
}

html.dark .mo-home-page .tag-gray {
  background: var(--mo-n700);
  color: var(--mo-n200);
}

html.dark .mo-home-page .search-result-note {
  color: var(--mo-n300);
}

html.dark .mo-home-page .search-result-note button {
  color: var(--mo-p300);
}

html.dark .contact-card {
  background: var(--mo-n800);
  border-color: var(--mo-n700);
}

html.dark .contact-card h3 {
  color: var(--mo-n100);
}

html.dark .contact-card p {
  color: var(--mo-n400);
}

html.dark .contact-close {
  color: var(--mo-n400);
}

html.dark .contact-close:hover {
  color: var(--mo-p300);
}

/* p600 不参与深色重映射（Mo-Blog 下仍是深靛蓝），深色下强调色文字改用 p300 */
html.dark .mo-home-page .avatar-sm,
html.dark .mo-home-page .rank.top {
  color: var(--mo-p300);
}
</style>
