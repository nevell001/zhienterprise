<template>
  <SiteLayout>
    <div ref="rootEl" class="tpl-site business-page mo-home-page">
      <!-- 页头（与关于页同构：eyebrow + 标题 + 副题；文案可在后台「官网内容」页签调整） -->
      <header class="aph">
        <div class="eh-wrap">
          <span class="eyebrow rv">{{ heroEyebrow }}</span>
          <h1 class="aph-title rv">{{ heroTitle }}</h1>
          <p class="aph-sub rv d1">{{ heroSubtitle }}</p>
        </div>
      </header>

      <main class="pad">
        <div class="eh-wrap">
          <!-- 分类筛选栏（方案文档 §3.2：分类 Tab 横向排列） -->
          <div class="biz-tabs rv" role="tablist">
            <button
              v-for="cat in categoryTabs"
              :key="cat.id"
              type="button"
              class="tag"
              :class="{ on: activeCatId === cat.id }"
              role="tab"
              :aria-selected="activeCatId === cat.id"
              @click="switchCat(cat.id)"
            >
              {{ cat.name }}
            </button>
            <div class="view-toggle">
              <button
                type="button"
                class="view-btn"
                :class="{ on: layout === 'card' }"
                aria-label="卡片视图"
                @click="layout = 'card'"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <rect
                    x="2"
                    y="2"
                    width="5"
                    height="5"
                    rx="1"
                    stroke="currentColor"
                    stroke-width="1.3"
                  />
                  <rect
                    x="9"
                    y="2"
                    width="5"
                    height="5"
                    rx="1"
                    stroke="currentColor"
                    stroke-width="1.3"
                  />
                  <rect
                    x="2"
                    y="9"
                    width="5"
                    height="5"
                    rx="1"
                    stroke="currentColor"
                    stroke-width="1.3"
                  />
                  <rect
                    x="9"
                    y="9"
                    width="5"
                    height="5"
                    rx="1"
                    stroke="currentColor"
                    stroke-width="1.3"
                  />
                </svg>
              </button>
              <button
                type="button"
                class="view-btn"
                :class="{ on: layout === 'list' }"
                aria-label="列表视图"
                @click="layout = 'list'"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M2 4h12M2 8h12M2 12h12"
                    stroke="currentColor"
                    stroke-width="1.3"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
            </div>
          </div>

          <!-- 已选条件胶囊（方案文档 §3.2：可清除） -->
          <div v-if="activeCatId !== null" class="biz-chips">
            <span class="biz-chip">
              分类：{{ activeCatName }}
              <button type="button" aria-label="清除筛选" @click="switchCat(null)">✕</button>
            </span>
          </div>

          <div v-if="loading" class="biz-grid">
            <div v-for="n in 6" :key="n" class="biz-card biz-skeleton"></div>
          </div>
          <template v-else>
            <div v-if="items.length" class="biz-grid" :class="`is-${layout}`">
              <article
                v-for="(item, bi) in items"
                :key="item.id"
                class="biz-card rv"
                :class="`d${bi % 3}`"
                @click="goItem(item.id)"
              >
                <div class="biz-thumb">
                  <img
                    v-if="item.coverUrl"
                    :src="item.coverUrl"
                    :alt="item.title"
                    loading="lazy"
                    decoding="async"
                  />
                  <span v-else class="biz-thumb-fallback" :style="thumbStyle(item.id)">
                    {{ (item.title || '').slice(0, 1) }}
                  </span>
                </div>
                <div class="biz-body">
                  <h2>{{ item.title }}</h2>
                  <p>{{ item.summary }}</p>
                  <div class="biz-meta">
                    <span>{{ formatDate(item.createTime) }}</span>
                    <span class="biz-more">
                      了解详情
                      <svg
                        width="15"
                        height="15"
                        viewBox="0 0 15 15"
                        fill="none"
                        aria-hidden="true"
                      >
                        <path
                          d="M3 7.5h9M8 4l3.5 3.5L8 11"
                          stroke="currentColor"
                          stroke-width="1.5"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        />
                      </svg>
                    </span>
                  </div>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无内容" />
          </template>

          <!-- 分页：桌面页码；移动端「加载更多」（方案文档 §3.2） -->
          <div v-if="!loading && total > pageSize" class="biz-pager">
            <el-pagination
              v-model:current-page="page"
              class="biz-pager-desktop"
              layout="prev, pager, next"
              :total="total"
              :page-size="pageSize"
              background
              @current-change="load(false)"
            />
            <el-button
              v-if="hasMore"
              class="biz-load-more"
              plain
              :loading="loading"
              @click="loadMore"
            >
              加载更多
            </el-button>
          </div>
        </div>
      </main>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SiteLayout from '@/components/SiteLayout.vue'
import { getArticleList } from '@/api/site'
import type { Article } from '@/types'
import { getCategoryList } from '@/api/site/category'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { DEFAULT_PAGE_COPY, getPageCopy } from '@/utils/siteContent'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { parseTime } from '@/utils/zhi'
import { logger } from '@/utils/logger'
import { useScrollReveal } from '@/composables/useScrollReveal'

const router = useRouter()
const route = useRoute()
const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const businessLabel = computed(
  () => String(settings.value.business_label || '').trim() || '核心业务'
)

/* 页头文案：「官网内容」页签的页面文案 → 核心业务栏目名称 → 内置文案 */
const pageCopy = computed(() => getPageCopy(settings.value?.site_page_copy, 'business'))
const heroEyebrow = computed(() => pageCopy.value.eyebrow || businessLabel.value)
const heroTitle = computed(() => pageCopy.value.title || businessLabel.value)
const heroSubtitle = computed(() => pageCopy.value.subtitle || DEFAULT_PAGE_COPY.business.subtitle)

interface CatTab {
  id: number | null
  name: string
  alias?: string
}

const items = ref<Article[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 9
const loading = ref(true)
const categoryTabs = ref<CatTab[]>([{ id: null, name: '全部', alias: '' }])
const activeCatId = ref<number | null>(null)
/** 视图切换（方案文档 §3.2）：card 卡片网格 / list 横向列表 */
const layout = ref<'card' | 'list'>('card')
const hasMore = computed(() => items.value.length < total.value)

const activeCatName = computed(
  () => categoryTabs.value.find(t => t.id === activeCatId.value)?.name || ''
)

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

const loadMore = async () => {
  page.value++
  await load(true)
}

const load = async (append = false) => {
  loading.value = true
  try {
    const response = await getArticleList({
      pageNum: page.value,
      pageSize,
      // 核心业务板块同时承载客户案例（case），否则案例内容在前台没有入口
      articleTypes: 'business,case',
      categoryId: activeCatId.value ?? undefined
    })
    if (response && response.code === 200) {
      const rows = (response.rows || []) as Article[]
      items.value = append ? [...items.value, ...rows] : rows
      total.value = Number(response.total) || 0
    } else {
      items.value = []
      total.value = 0
    }
  } catch (error) {
    logger.error('加载核心业务列表失败:', error)
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const response = await getCategoryList({ pageNum: 1, pageSize: 50 })
    const list = response?.data || response?.rows || []
    const cats = (Array.isArray(list) ? list : [])
      .filter((c: { id?: number; name?: string; alias?: string }) => c.id && c.name)
      .map((c: { id: number; name: string; alias?: string }) => ({
        id: c.id as number,
        name: c.name,
        alias: c.alias || ''
      }))
    categoryTabs.value = [{ id: null, name: '全部', alias: '' }, ...cats]
  } catch (error) {
    logger.error('加载分类失败:', error)
  }
}

const switchCat = (id: number | null) => {
  activeCatId.value = id
  page.value = 1
  syncCatToUrl(id)
  load()
}

/** 筛选状态写入 URL query（方案文档 §3.2），分享与返回可复原 */
const syncCatToUrl = (id: number | null) => {
  const tab = categoryTabs.value.find(t => t.id === id)
  const value = tab?.alias || (id !== null ? String(id) : '')
  router.replace({
    query: { ...route.query, ...(value ? { cat: value } : { cat: undefined }) }
  })
}

/** 从 URL query 恢复筛选（支持 alias 或数字 id），分类就绪后调用 */
const initCatFromRoute = () => {
  const raw = String(route.query.cat || '').trim()
  if (!raw) {
    activeCatId.value = null
    return
  }
  const tab = categoryTabs.value.find(t => t.alias === raw || String(t.id) === raw)
  activeCatId.value = tab?.id ?? null
}

const applyPageSeo = () => {
  const siteName = settings.value.blog_name || '知企建站系统'
  applySeo({
    title: `${businessLabel.value} - ${siteName}`,
    description: settings.value.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
}

watch(businessLabel, applyPageSeo, { immediate: true })

onMounted(async () => {
  applyPageSeo()
  await loadCategories()
  initCatFromRoute()
  await load()
})

const formatDate = (date?: string) => (date ? parseTime(date, '{y}-{m}-{d}') : '')

const thumbStyle = (id?: number) => ({
  background:
    Math.abs(Number(id) || 0) % 2 === 0
      ? 'linear-gradient(135deg, var(--tpl-p), var(--tpl-p-h))'
      : 'linear-gradient(135deg, var(--tpl-p-h), var(--tpl-dark))'
})

const goItem = (id?: number) => id && router.push(`/business/${id}`)
</script>

<style scoped>
@import '../about-shared.css';

/* 分类筛选栏（吸顶由页面滚动自然形成，样式横向排列） */
.biz-tabs {
  display: flex;
  gap: var(--mo-sp-3);
  flex-wrap: wrap;
  margin-bottom: var(--mo-sp-8);
}

/* 分类筛选芯片：与企业官网其他页面的 .tag 约定一致（sections.css 同款），
   显式用 --tpl-* 令牌，避免落到全局旧博客规则 `.theme-mo-blog .tag`
   （那里用的是 --mo-p50/--mo-p700，深色下 p700 不参与重映射 → 深蓝字贴深底，
   对比度仅 2:1，筛选栏几乎看不清）。 */
.biz-tabs .tag {
  position: relative;
  display: inline-block;
  padding: var(--mo-sp-1) var(--mo-sp-3);
  font-family: inherit;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--tpl-p-text);
  cursor: pointer;
  background: var(--tpl-p-sub);
  border: 1px solid transparent;
  border-radius: 999px;
  transition:
    background 0.2s,
    color 0.2s,
    border-color 0.2s;
}

/* 触控目标 ≥44px 用透明内边距扩展，视觉尺寸不变（方案文档 §5.3） */
.biz-tabs .tag::after {
  position: absolute;
  inset: -7px -4px;
  content: '';
}

.biz-tabs .tag:hover {
  border-color: var(--tpl-p);
}

.biz-tabs .tag.on {
  color: var(--tpl-white, #fff);
  background: var(--tpl-p);
  border-color: var(--tpl-p);
}

.biz-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-6);
}

.biz-card {
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  overflow: hidden;
  background: var(--tpl-bg);
  cursor: pointer;
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s,
    border-color 0.3s;
}

.biz-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
  border-color: var(--tpl-p);
}

.biz-thumb {
  aspect-ratio: 16 / 9;
  background: var(--tpl-surface);
  display: grid;
  place-items: center;
  overflow: hidden;
}

.biz-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.biz-card:hover .biz-thumb img {
  transform: scale(1.04);
}

.biz-thumb-fallback {
  color: var(--tpl-white);
  font-size: var(--mo-fs-6xl);
  font-weight: 700;
}

.biz-body {
  padding: var(--mo-sp-6);
}

.biz-body h2 {
  margin: 0 0 var(--mo-sp-2);
  font-size: var(--mo-fs-xl);
  color: var(--tpl-ink-900);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.biz-body p {
  margin: 0 0 var(--mo-sp-4);
  color: var(--tpl-ink-500);
  font-size: var(--mo-fs-sm);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.biz-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--tpl-ink-400);
  font-size: var(--mo-fs-sm);
}

.biz-more {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-1);
  color: var(--tpl-p-text);
  font-weight: 500;
}

.biz-skeleton {
  height: 300px;
  background: linear-gradient(90deg, var(--tpl-surface), var(--tpl-bg), var(--tpl-surface));
  background-size: 200% 100%;
  animation: biz-shimmer 1.4s ease infinite;
}

@keyframes biz-shimmer {
  from {
    background-position: 200% 0;
  }

  to {
    background-position: -200% 0;
  }
}

.biz-pager {
  display: flex;
  justify-content: center;
  margin-top: var(--mo-sp-9);
}

/* 视图切换 */
.view-toggle {
  display: flex;
  gap: 4px;
  margin-left: auto;
}

.view-btn {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-sm);
  background: var(--tpl-bg);
  color: var(--tpl-ink-400);
  cursor: pointer;
  transition:
    color 0.2s,
    border-color 0.2s;
}

.view-btn.on,
.view-btn:hover {
  color: var(--tpl-p-text);
  border-color: var(--tpl-p);
}

/* 已选条件胶囊 */
.biz-chips {
  display: flex;
  gap: var(--mo-sp-3);
  flex-wrap: wrap;
  margin-bottom: var(--mo-sp-5);
}

.biz-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-2);
  font-size: var(--mo-fs-sm);
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: 4px 10px;
  border-radius: 999px;
}

.biz-chip button {
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
  line-height: 1;
}

/* 列表视图：横向行卡 */
.biz-grid.is-list {
  grid-template-columns: 1fr;
}

.biz-grid.is-list .biz-card {
  display: flex;
  gap: var(--mo-sp-6);
  align-items: stretch;
}

.biz-grid.is-list .biz-thumb {
  flex: none;
  width: 240px;
  aspect-ratio: auto;
  min-height: 150px;
}

.biz-grid.is-list .biz-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

@media (max-width: 991px) {
  .biz-grid.is-list .biz-thumb {
    width: 180px;
  }
}

@media (max-width: 639px) {
  .biz-grid.is-list .biz-card {
    flex-direction: column;
  }

  .biz-grid.is-list .biz-thumb {
    width: 100%;
    aspect-ratio: 16 / 9;
  }

  /* 移动端「加载更多」（方案文档 §3.2） */
  .biz-pager .biz-pager-desktop {
    display: none;
  }

  .biz-load-more {
    display: inline-flex;
  }
}

.biz-load-more {
  display: none;
}

@media (max-width: 991px) {
  .biz-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 639px) {
  .biz-grid {
    grid-template-columns: 1fr;
  }
}
</style>
