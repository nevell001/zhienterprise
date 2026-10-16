<template>
  <div ref="rootEl" class="tpl-site ent-news mo-home-page">
    <header class="enh-head">
      <div class="eh-wrap">
        <span class="eyebrow rv">News</span>
        <h1 class="enh-title rv">新闻动态</h1>
        <p class="enh-sub rv d1">企业动态与行业资讯，了解我们的最新进展。</p>
      </div>
    </header>

    <main class="eh-wrap enh-main">
      <div class="enh-body">
        <div class="enh-content">
          <div v-if="loading" class="enh-skeleton rv">
            <div class="enh-skel-hero"></div>
            <div class="enh-skel-row">
              <div class="enh-skel-card"></div>
              <div class="enh-skel-card"></div>
            </div>
          </div>

          <template v-else-if="items.length">
            <!-- 头条：16:9 大图 + 摘要（方案文档 §3.4 企业动态） -->
            <article v-if="headline" class="enh-hero-card rv" @click="goItem(headline.id)">
              <div class="enh-hero-media">
                <img
                  v-if="headline.coverUrl"
                  :src="headline.coverUrl"
                  :alt="headline.title"
                  loading="lazy"
                  decoding="async"
                />
                <svg
                  v-else
                  width="72"
                  height="72"
                  viewBox="0 0 72 72"
                  fill="none"
                  opacity=".5"
                  aria-hidden="true"
                >
                  <rect
                    x="12"
                    y="12"
                    width="48"
                    height="48"
                    rx="12"
                    stroke="currentColor"
                    stroke-width="1.5"
                  />
                  <path
                    d="M24 36h24M36 24v24"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
              </div>
              <div class="enh-hero-body">
                <div class="n-meta">
                  <span class="tag">企业动态</span>
                  <span>{{ formatDate(headline.createTime) }}</span>
                </div>
                <h2>{{ headline.title }}</h2>
                <p>{{ headline.summary }}</p>
                <span class="lnk">
                  阅读全文
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <path
                      d="M3 8h10M9 4l4 4-4 4"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </span>
              </div>
            </article>

            <!-- 次条：2 列卡片 -->
            <div v-if="secondaryItems.length" class="enh-grid">
              <article
                v-for="(item, ii) in secondaryItems"
                :key="item.id"
                class="enh-card rv"
                :class="`d${ii % 2}`"
                @click="goItem(item.id)"
              >
                <div class="enh-card-meta">
                  <span class="tag">企业动态</span>
                  <span>{{ formatDate(item.createTime) }}</span>
                </div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.summary }}</p>
              </article>
            </div>

            <div v-if="total > pageSize" class="enh-pager">
              <el-pagination
                v-model:current-page="page"
                layout="prev, pager, next"
                :total="total"
                :page-size="pageSize"
                background
                @current-change="load"
              />
            </div>
          </template>
          <p v-else class="enh-empty rv">暂无动态，发布资讯后将在这里展示</p>
        </div>

        <!-- 归档侧栏（桌面 ≥1200px，方案文档 §3.4）：年份归档入口 + 标签 -->
        <aside class="enh-side rv d1">
          <div v-if="archiveYears.length" class="enh-side-block">
            <h4>归档</h4>
            <router-link
              v-for="y in archiveYears"
              :key="y"
              class="enh-side-link"
              to="/blog/archive"
            >
              {{ y }} 年
            </router-link>
          </div>
          <div v-if="sideTags.length" class="enh-side-block">
            <h4>热门标签</h4>
            <router-link
              v-for="t in sideTags"
              :key="t.id"
              class="enh-side-link"
              :to="`/blog/tag/${t.id}`"
            >
              {{ t.name }}
            </router-link>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList, getArticleArchive } from '@/api/site'
import type { Article } from '@/types'
import { getTagCloud } from '@/api/site/tag'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { parseTime } from '@/utils/zhi'
import { logger } from '@/utils/logger'
import { useScrollReveal } from '@/composables/useScrollReveal'

const router = useRouter()
const siteSettingsStore = useSiteSettingsStore()
const items = ref<Article[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 7
const loading = ref(true)

/* ---------- 归档侧栏（桌面 ≥1200px）：年份聚合 + 热门标签 ---------- */
const archiveYears = ref<string[]>([])
const sideTags = ref<{ id: number; name: string }[]>([])

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

const headline = computed(() => items.value[0])
const secondaryItems = computed(() => items.value.slice(1))

const load = async () => {
  loading.value = true
  try {
    const response = await getArticleList({
      pageNum: page.value,
      pageSize,
      articleType: 'news'
    })
    if (response && response.code === 200) {
      items.value = (response.rows || []) as Article[]
      total.value = Number(response.total) || 0
    } else {
      items.value = []
      total.value = 0
    }
  } catch (error) {
    logger.error('加载新闻列表失败:', error)
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
    window.scrollTo({ top: 0 })
  }
}

onMounted(async () => {
  try {
    const [archiveRes, tagRes] = await Promise.allSettled([getArticleArchive(), getTagCloud()])
    if (archiveRes.status === 'fulfilled') {
      const rows = (archiveRes.value as { data?: { date: string }[] })?.data || []
      // 按年去重，倒序
      archiveYears.value = Array.from(new Set(rows.map(r => String(r.date).slice(0, 4)))).sort(
        (a, b) => b.localeCompare(a)
      )
    }
    if (tagRes.status === 'fulfilled') {
      const tags = (tagRes.value as { data?: { id: number; name: string }[] })?.data || []
      sideTags.value = (Array.isArray(tags) ? tags : [])
        .slice(0, 8)
        .map((t: any) => ({ id: t.id, name: t.name }))
        .filter((t: any) => t.id && t.name)
    }
  } catch (error) {
    logger.error('加载归档侧栏失败:', error)
  }
})

onMounted(() => {
  const siteName = siteSettingsStore.siteSettings?.blog_name || '知企建站系统'
  applySeo({
    title: `新闻动态 - ${siteName}`,
    description: '',
    keywords: '',
    canonical: canonicalUrl()
  })
  load()
})

const formatDate = (date?: string) => (date ? parseTime(date, '{y}-{m}-{d}') : '')
const goItem = (id?: number) => id && router.push(`/article/${id}`)
</script>

<style scoped>
.enh-head {
  padding: calc(var(--mo-sp-12) + 64px) 0 var(--mo-sp-10);
  background: radial-gradient(circle at 15% 20%, var(--tpl-p-sub), transparent 42%), var(--tpl-bg);
  border-bottom: 1px solid var(--tpl-border);
}

.eh-wrap {
  max-width: var(--tpl-grid);
  margin: 0 auto;
  padding: 0 var(--mo-sp-7);
}

.eyebrow {
  display: inline-flex;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: 999px;
  margin-bottom: var(--mo-sp-6);
}

.enh-title {
  margin: 0 0 var(--mo-sp-4);
  font-size: var(--mo-fs-7xl);
  line-height: 1.15;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--tpl-ink-900);
  font-family: var(--tpl-serif);
}

.enh-sub {
  margin: 0;
  font-size: var(--mo-fs-lg);
  color: var(--tpl-ink-500);
}

.enh-main {
  padding: var(--mo-sp-11) var(--mo-sp-7) var(--mo-sp-12);
}

/* ---------- 头条 ---------- */
.enh-hero-card {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  overflow: hidden;
  background: var(--tpl-bg);
  cursor: pointer;
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s;
}

.enh-hero-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
}

.enh-hero-media {
  aspect-ratio: 16 / 9;
  background: var(--tpl-surface);
  display: grid;
  place-items: center;
  color: var(--tpl-p);
  overflow: hidden;
}

.enh-hero-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.enh-hero-card:hover .enh-hero-media img {
  transform: scale(1.04);
}

.enh-hero-body {
  padding: var(--mo-sp-8);
  display: flex;
  flex-direction: column;
  gap: var(--mo-sp-4);
}

.enh-hero-body h2 {
  margin: 0;
  font-size: var(--mo-fs-3xl);
  line-height: 1.4;
  color: var(--tpl-ink-900);
}

.enh-hero-body p {
  margin: 0;
  font-size: var(--mo-fs-md);
  line-height: 1.75;
  color: var(--tpl-ink-500);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.n-meta {
  display: flex;
  gap: var(--mo-sp-3);
  align-items: center;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
}

.tag {
  display: inline-block;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: 999px;
}

.lnk {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-2);
  color: var(--tpl-p-text);
  font-weight: 500;
  font-size: var(--mo-fs-md);
  margin-top: auto;
}

/* ---------- 次条 ---------- */
.enh-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--mo-sp-6);
  margin-top: var(--mo-sp-6);
}

.enh-card {
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  padding: var(--mo-sp-7);
  background: var(--tpl-bg);
  cursor: pointer;
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s,
    border-color 0.3s;
}

.enh-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
  border-color: var(--tpl-p);
}

.enh-card-meta {
  display: flex;
  gap: var(--mo-sp-3);
  align-items: center;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
  margin-bottom: var(--mo-sp-4);
}

.enh-card h3 {
  margin: 0 0 var(--mo-sp-3);
  font-size: var(--mo-fs-lg);
  line-height: 1.5;
  color: var(--tpl-ink-900);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.enh-card p {
  margin: 0;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-500);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.enh-pager {
  display: flex;
  justify-content: center;
  margin-top: var(--mo-sp-9);
}

.enh-empty {
  text-align: center;
  color: var(--tpl-ink-400);
  font-size: var(--mo-fs-sm);
}

/* 骨架 */
.enh-skel-hero,
.enh-skel-card {
  border-radius: var(--tpl-r-lg);
  background: linear-gradient(90deg, var(--tpl-surface), var(--tpl-bg), var(--tpl-surface));
  background-size: 200% 100%;
  animation: enh-shimmer 1.4s ease infinite;
}

.enh-skel-hero {
  height: 320px;
  margin-bottom: var(--mo-sp-6);
}

.enh-skel-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--mo-sp-6);
}

.enh-skel-card {
  height: 180px;
}

@keyframes enh-shimmer {
  from {
    background-position: 200% 0;
  }

  to {
    background-position: -200% 0;
  }
}

@media (max-width: 991px) {
  .eh-wrap {
    padding: 0 var(--mo-sp-5);
  }

  .enh-head {
    padding: calc(var(--mo-sp-10) + 64px) 0 var(--mo-sp-9);
  }

  .enh-title {
    font-size: var(--mo-fs-5xl);
  }

  .enh-hero-card {
    grid-template-columns: 1fr;
  }

  .enh-grid {
    grid-template-columns: 1fr;
  }

  .enh-main {
    padding: var(--mo-sp-9) var(--mo-sp-5) var(--mo-sp-11);
  }
}
</style>
