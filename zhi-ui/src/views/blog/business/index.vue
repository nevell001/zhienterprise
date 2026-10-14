<template>
  <div ref="rootEl" class="tpl-site business-page mo-home-page">
    <!-- 页头（与关于页同构：eyebrow + 标题 + 副题） -->
    <header class="aph">
      <div class="eh-wrap">
        <span class="eyebrow rv">{{ businessLabel }}</span>
        <h1 class="aph-title rv">{{ businessLabel }}</h1>
        <p class="aph-sub rv d1">了解我们的主要业务与产品</p>
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
        </div>

        <div v-if="loading" class="biz-grid">
          <div v-for="n in 6" :key="n" class="biz-card biz-skeleton"></div>
        </div>
        <template v-else>
          <div v-if="items.length" class="biz-grid">
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
                    <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
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

        <div v-if="!loading && total > pageSize" class="biz-pager">
          <el-pagination
            v-model:current-page="page"
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            background
            @current-change="load"
          />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '@/api/blog'
import type { Article } from '@/types'
import { getCategoryList } from '@/api/blog/category'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { parseTime } from '@/utils/zhi'
import { logger } from '@/utils/logger'
import { useScrollReveal } from '@/composables/useScrollReveal'

const router = useRouter()
const blogSettingsStore = useBlogSettingsStore()
const settings = computed(() => blogSettingsStore.blogSettings)
const businessLabel = computed(
  () => String(settings.value.business_label || '').trim() || '核心业务'
)

interface CatTab {
  id: number | null
  name: string
}

const items = ref<Article[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 9
const loading = ref(true)
const categoryTabs = ref<CatTab[]>([{ id: null, name: '全部' }])
const activeCatId = ref<number | null>(null)

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

const load = async () => {
  loading.value = true
  try {
    const response = await getArticleList({
      pageNum: page.value,
      pageSize,
      articleType: 'business',
      categoryId: activeCatId.value ?? undefined
    })
    if (response && response.code === 200) {
      items.value = (response.rows || []) as Article[]
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
      .filter((c: { id?: number; name?: string }) => c.id && c.name)
      .map((c: { id: number; name: string }) => ({ id: c.id as number, name: c.name }))
    categoryTabs.value = [{ id: null, name: '全部' }, ...cats]
  } catch (error) {
    logger.error('加载分类失败:', error)
  }
}

const switchCat = (id: number | null) => {
  activeCatId.value = id
  page.value = 1
  load()
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

onMounted(() => {
  applyPageSeo()
  load()
  loadCategories()
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
