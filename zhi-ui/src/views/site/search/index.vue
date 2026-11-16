<template>
  <div class="search-page">
    <SiteLayout>
      <div class="search-container tpl-legacy">
        <div class="search-head">
          <h1 class="search-title">搜索文章</h1>
          <div v-if="isSearchEnabled" class="search-form">
            <input
              v-model="keyword"
              type="text"
              placeholder="输入关键词搜索文章..."
              @keyup.enter="runSearch"
            />
            <button type="button" @click="runSearch">搜索</button>
          </div>
          <p v-else class="search-disabled-tip">搜索功能已关闭</p>
        </div>

        <div v-if="isSearchEnabled" class="search-body">
          <div v-if="loading" v-loading="loading" class="search-loading"></div>

          <template v-else-if="hasSearched">
            <el-empty v-if="searchError" :description="searchError">
              <el-button type="primary" @click="doSearch(currentPage)">重新搜索</el-button>
            </el-empty>
            <template v-else>
              <div v-if="results.length > 0" class="search-summary">
                找到 {{ total }} 篇与
                <strong>“{{ query }}”</strong>
                相关的文章
              </div>
              <el-empty v-else-if="query" description="没有找到相关文章，换个关键词试试吧">
                <el-button type="primary" @click="resetSearch">清除搜索</el-button>
              </el-empty>

              <article
                v-for="article in results"
                :key="article.id"
                class="result-card"
                @click="goArticle(article)"
              >
                <div class="result-card-title">{{ article.title }}</div>
                <p v-if="article.summary" class="result-card-excerpt">{{ article.summary }}</p>
                <div class="result-card-meta">
                  <span v-if="typeLabel(article.articleType)" class="result-type">
                    {{ typeLabel(article.articleType) }}
                  </span>
                  <span v-if="article.categoryName">{{ article.categoryName }}</span>
                  <span v-if="article.createTime">{{ formatDate(article.createTime) }}</span>
                </div>
              </article>

              <div v-if="total > pageSize" class="search-pagination">
                <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
                  上一页
                </button>
                <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
                <button :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">
                  下一页
                </button>
              </div>
            </template>
          </template>
        </div>
      </div>
    </SiteLayout>
  </div>
</template>

<script setup lang="ts" name="SiteSearch">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SiteLayout from '@/components/SiteLayout.vue'
import { searchArticles } from '@/api/site/article'
import { createRequestGuard } from '@/utils/requestGuard'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { parseTime } from '@/utils/zhi'
import { articleDetailPath } from '@/utils/articlePath'
import logger from '@/utils/logger'

const route = useRoute()
const router = useRouter()
const siteSettingsStore = useSiteSettingsStore()

const siteSettings = computed(() => siteSettingsStore.siteSettings)
const isSearchEnabled = computed(() => siteSettingsStore.isFeatureEnabled('search_enabled'))

const keyword = ref('')
const query = ref('')
const results = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const hasSearched = ref(false)
const searchError = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const formatDate = (date: string) => {
  return parseTime(date, '{y}-{m}-{d}')
}

const TYPE_LABELS: Record<string, string> = {
  news: '资讯',
  business: '核心业务',
  case: '客户案例',
  solution: '解决方案'
}

const typeLabel = (articleType?: string) => (articleType ? TYPE_LABELS[articleType] || '' : '')

const requestGuard = createRequestGuard()

const doSearch = async (page = 1) => {
  const kw = keyword.value.trim() || String(route.query.q || '').trim()
  if (!kw) {
    hasSearched.value = true
    results.value = []
    total.value = 0
    return
  }
  // 同步 URL，便于分享与刷新
  router.replace({ path: '/search', query: kw ? { q: kw } : {} })

  loading.value = true
  try {
    const params = { pageNum: page, pageSize: pageSize.value }
    const token = requestGuard.next()
    const response = await searchArticles(kw, params)
    // 连续输入/快速翻页时，慢的旧响应不能覆盖新结果
    if (!requestGuard.isLatest(token)) return
    results.value = response.rows || []
    total.value = response.total || 0
    query.value = kw
    keyword.value = kw
    currentPage.value = page
    hasSearched.value = true
    searchError.value = ''
  } catch (error) {
    // 区分"请求失败"与"无结果"：失败时给出错误提示和重试入口，而不是伪装成搜索无结果
    logger.error('搜索请求失败:', error)
    results.value = []
    total.value = 0
    query.value = kw
    keyword.value = kw
    hasSearched.value = true
    searchError.value = '搜索失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  if (page < 1) return
  doSearch(page)
}

// 供模板点击/回车调用的无参入口
const runSearch = () => {
  doSearch(1)
}

const resetSearch = () => {
  keyword.value = ''
  results.value = []
  total.value = 0
  query.value = ''
  hasSearched.value = false
  router.replace('/search')
}

const goArticle = (article: { id?: number; articleType?: string; slug?: string }) => {
  if (!article.id && !article.slug) return
  router.push(articleDetailPath(article))
}

// 从 URL 初始化或关键词变化时重新搜索
const initialKeyword = String(route.query.q || '').trim()
if (initialKeyword) {
  keyword.value = initialKeyword
  doSearch(1)
}

watch(
  () => route.query.q,
  q => {
    const next = String(q || '').trim()
    if (next && next !== query.value) {
      keyword.value = next
      doSearch(1)
    }
  }
)
</script>

<style scoped>
.search-page {
  min-height: 100vh;
}
.search-container {
  max-width: 860px;
  margin: 0 auto;
  padding: var(--mo-sp-12) var(--mo-sp-5) var(--mo-sp-9);
  /* 企业模板用 --tpl-*，经典模式回退到 --mo-* 与 --el-*，避免深色模式变白底 */
  --search-bg: var(--tpl-bg, var(--mo-n0, #fff));
  --search-border: var(--tpl-border, var(--el-border-color, #e5e7eb));
  --search-text: var(--tpl-ink-900, var(--el-text-color-primary, #111827));
  --search-muted: var(--tpl-ink-500, var(--mo-n500, #6b7280));
  --search-faint: var(--tpl-ink-400, var(--mo-n400, #6b7280));
  --search-primary: var(--tpl-p, var(--el-color-primary, #2563eb));
  --search-primary-text: var(--tpl-p-text, var(--mo-primary-text, #1d4ed8));
  --search-primary-sub: var(--tpl-p-sub, var(--el-color-primary-light-9, #eef3fe));
  --search-on-primary: var(--tpl-white, var(--mo-on-primary, #fff));
  --search-radius: var(--tpl-r-md, var(--mo-r-md, 10px));
  --search-shadow: var(--tpl-sh-md, var(--mo-shadow-md, 0 8px 24px rgba(15, 23, 42, 0.1)));
}
.search-head {
  margin-bottom: var(--mo-sp-6);
}
.search-title {
  margin: 0 0 14px;
  font-size: 26px;
  font-weight: 700;
  color: var(--search-text);
  font-family: var(--tpl-serif, var(--mo-font-serif, inherit));
}
.search-form {
  display: flex;
  gap: 10px;
  max-width: 560px;
}
.search-form input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--search-border);
  border-radius: var(--search-radius);
  font-size: var(--mo-fs-sm);
  outline: none;
  background: var(--search-bg);
  color: var(--search-text);
}
.search-form input:focus {
  border-color: var(--search-primary);
}
/* 移动端输入字号 ≥16px：iOS Safari 聚焦时否则自动放大页面（方案文档 §5.3） */
@media (max-width: 767px) {
  .search-form input {
    font-size: 16px;
  }
}
.search-form button {
  padding: 0 var(--mo-sp-5);
  border: none;
  border-radius: var(--search-radius);
  background: var(--search-primary);
  color: var(--search-on-primary);
  font-size: var(--mo-fs-sm);
  cursor: pointer;
}
.search-disabled-tip {
  color: var(--search-muted);
  font-size: var(--mo-fs-sm);
}
.search-loading {
  min-height: 160px;
}
.search-summary {
  margin-bottom: 14px;
  font-size: var(--mo-fs-sm);
  color: var(--search-muted);
}
.result-card {
  padding: 16px 18px;
  margin-bottom: var(--mo-sp-3);
  background: var(--search-bg);
  border: 1px solid var(--search-border);
  border-radius: var(--search-radius);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.result-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--search-shadow);
}
.result-card-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--search-text);
}
.result-card:hover .result-card-title {
  color: var(--search-primary-text);
}
.result-card-excerpt {
  margin: 8px 0 10px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--search-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.result-card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--mo-sp-2);
  font-size: var(--mo-fs-xs);
  color: var(--search-faint);
}
.result-type {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--search-primary-sub);
  color: var(--search-primary-text);
  font-weight: 600;
}
.search-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 22px;
}
.search-pagination button {
  padding: 6px 16px;
  border: 1px solid var(--search-border);
  border-radius: var(--search-radius);
  background: var(--search-bg);
  color: var(--search-text);
  cursor: pointer;
}
.search-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.page-info {
  font-size: 13px;
  color: var(--search-muted);
}
</style>
