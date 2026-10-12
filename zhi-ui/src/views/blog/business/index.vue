<template>
  <div class="business-page mo-home-page">
    <header class="bp-head">
      <div class="bp-container">
        <h1>{{ businessLabel }}</h1>
        <p>了解我们的主要业务与产品</p>
      </div>
    </header>

    <main class="bp-main bp-container">
      <div v-if="loading" class="bp-grid">
        <div v-for="n in 6" :key="n" class="bp-card bp-skeleton"></div>
      </div>
      <template v-else>
        <div v-if="items.length" class="bp-grid">
          <article v-for="item in items" :key="item.id" class="bp-card" @click="goItem(item.id)">
            <div class="bp-thumb" :style="thumbStyle(item.id)">
              {{ (item.title || '').slice(0, 1) }}
            </div>
            <div class="bp-body">
              <h2>{{ item.title }}</h2>
              <p>{{ item.summary }}</p>
              <div class="bp-meta">
                <span>{{ formatDate(item.createTime) }}</span>
                <span class="bp-more">了解详情 →</span>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无内容" />
      </template>

      <div v-if="!loading && total > pageSize" class="bp-pager">
        <el-pagination
          v-model:current-page="page"
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          background
          @current-change="load"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '@/api/blog'
import type { Article } from '@/types'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { parseTime } from '@/utils/zhi'
import { logger } from '@/utils/logger'

const router = useRouter()
const blogSettingsStore = useBlogSettingsStore()
const settings = computed(() => blogSettingsStore.blogSettings)
const businessLabel = computed(() => String(settings.value.business_label || '').trim() || '核心业务')

const items = ref<Article[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 9
const loading = ref(true)

const load = async () => {
  loading.value = true
  try {
    const response = await getArticleList({
      pageNum: page.value,
      pageSize,
      articleType: 'business'
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
})

const formatDate = (date?: string) => (date ? parseTime(date, '{y}-{m}-{d}') : '')

const thumbStyle = (id?: number) => ({
  background:
    Math.abs(Number(id) || 0) % 2 === 0
      ? 'linear-gradient(135deg, var(--mo-p500), var(--mo-p700))'
      : 'linear-gradient(135deg, var(--mo-p700), var(--mo-n700))'
})

const goItem = (id?: number) => id && router.push(`/blog/business/${id}`)
</script>

<style scoped>
.bp-container {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 var(--mo-sp-5);
}

.bp-head {
  padding: var(--mo-sp-8) 0;
  text-align: center;
  color: var(--mo-n50);
  background:
    radial-gradient(circle at 18% 20%, rgba(255, 255, 255, 0.14), transparent 42%),
    linear-gradient(135deg, var(--mo-p700), var(--mo-p900));
}

.bp-head h1 {
  margin: 0 0 var(--mo-sp-2);
  font-size: var(--mo-fs-2xl);
  letter-spacing: 1px;
}

.bp-head p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: var(--mo-fs-sm);
}

.bp-main {
  padding: var(--mo-sp-7) var(--mo-sp-5) var(--mo-sp-9);
}

.bp-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-5);
}

.bp-card {
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-xl);
  overflow: hidden;
  background: var(--el-bg-color);
  box-shadow: var(--mo-shadow-sm);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.bp-card:hover {
  transform: translateY(-3px);
  border-color: var(--mo-p200);
  box-shadow: var(--mo-shadow-lg);
}

.bp-thumb {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 36px;
  font-weight: 700;
}

.bp-body {
  padding: var(--mo-sp-5);
}

.bp-body h2 {
  margin: 0 0 var(--mo-sp-2);
  font-size: var(--mo-fs-lg);
  color: var(--mo-n900);
}

.bp-body p {
  margin: 0 0 var(--mo-sp-3);
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bp-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
}

.bp-more {
  color: var(--mo-p600);
  font-weight: 600;
}

.bp-skeleton {
  height: 280px;
  background: linear-gradient(90deg, var(--mo-n100), var(--mo-n50), var(--mo-n100));
  background-size: 200% 100%;
  animation: bp-shimmer 1.4s ease infinite;
}

@keyframes bp-shimmer {
  from {
    background-position: 200% 0;
  }

  to {
    background-position: -200% 0;
  }
}

.bp-pager {
  display: flex;
  justify-content: center;
  margin-top: var(--mo-sp-7);
}

@media (max-width: 900px) {
  .bp-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .bp-grid {
    grid-template-columns: 1fr;
  }
}
</style>
