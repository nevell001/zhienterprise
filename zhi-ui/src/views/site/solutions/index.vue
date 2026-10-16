<template>
  <div ref="rootEl" class="tpl-site solutions-board mo-home-page">
    <!-- 页头（方案文档 §3.3：Hero → 行业筛选 → 定制 CTA） -->
    <header class="aph">
      <div class="eh-wrap">
        <span class="eyebrow rv">Solutions</span>
        <h1 class="aph-title rv">解决方案</h1>
        <p class="aph-sub rv d1">按行业，看我们怎么解题。每个方案都基于真实业务场景。</p>
      </div>
    </header>

    <main class="pad">
      <div class="eh-wrap">
        <!-- 行业 Tab（§3.3 第一层：单选） -->
        <div class="sol-tabs rv" role="tablist">
          <button
            v-for="cat in industryTabs"
            :key="cat"
            type="button"
            class="tag"
            :class="{ on: activeIndustry === cat }"
            role="tab"
            :aria-selected="activeIndustry === cat"
            @click="activeIndustry = cat"
          >
            {{ cat }}
          </button>
        </div>

        <!-- 能力场景（§3.3 第二层：多选标签） -->
        <div class="sol-scenarios rv" role="group">
          <button
            v-for="sc in scenarioTabs"
            :key="sc"
            type="button"
            class="tag"
            :class="{ on: activeScenarios.includes(sc) }"
            :aria-pressed="activeScenarios.includes(sc)"
            @click="toggleScenario(sc)"
          >
            {{ sc }}
          </button>
          <button
            v-if="activeScenarios.length"
            type="button"
            class="sol-clear"
            @click="activeScenarios = []"
          >
            清除筛选
          </button>
        </div>

        <p class="sol-count rv d1">{{ countText }}</p>

        <div v-if="loading" class="sol-board-grid">
          <div v-for="n in 6" :key="n" class="sol-board-card sol-skeleton"></div>
        </div>
        <template v-else>
          <div v-if="filteredItems.length" class="sol-board-grid">
            <article
              v-for="(item, si) in filteredItems"
              :key="item.id"
              class="sol-board-card rv"
              :class="`d${si % 3}`"
              role="link"
              tabindex="0"
              @click="goItem(item.id)"
              @keydown.enter.prevent="goItem(item.id)"
            >
              <div class="sol-board-tags">
                <span v-if="item.industry" class="tag">{{ item.industry }}</span>
                <span v-for="sc in scenariosOf(item)" :key="sc" class="tag tag-plain">
                  {{ sc }}
                </span>
              </div>
              <h2>{{ item.title }}</h2>
              <p>{{ item.summary }}</p>
              <span class="lnk">
                查看方案
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
            </article>
          </div>
          <el-empty v-else description="该筛选下暂无方案">
            <el-button type="primary" plain @click="clearFilters">清除筛选</el-button>
          </el-empty>
        </template>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '@/api/site'
import type { Article } from '@/types'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { useScrollReveal } from '@/composables/useScrollReveal'

const router = useRouter()
const route = useRoute()
const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)

const allSolutions = ref<Article[]>([])
const total = ref(0)
const loading = ref(true)

/** 筛选状态写入 URL query（方案文档 §3.3）：行业单值 + 场景逗号分隔，分享与返回可复原 */
const parseScenariosQuery = (raw: unknown): string[] =>
  typeof raw === 'string'
    ? raw
        .split(',')
        .map(s => s.trim())
        .filter(Boolean)
    : []

const activeIndustry = ref(
  typeof route.query.industry === 'string' && route.query.industry ? route.query.industry : '全部'
)
const activeScenarios = ref<string[]>(parseScenariosQuery(route.query.scenario))

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

const scenariosOf = (item: Article): string[] =>
  (item.scenario || '')
    .split(/[,，]/)
    .map(s => s.trim())
    .filter(Boolean)

/** 行业 Tab：从方案数据聚合 */
const industryTabs = computed(() => {
  const cats = Array.from(new Set(allSolutions.value.map(i => i.industry).filter(Boolean)))
  return ['全部', ...cats]
})

/** 场景标签：从方案数据聚合 */
const scenarioTabs = computed(() => {
  const set = new Set<string>()
  allSolutions.value.forEach(i => scenariosOf(i).forEach(s => set.add(s)))
  return Array.from(set)
})

/** 筛选：行业精确 + 场景任一命中（多选标签为或语义） */
const filteredItems = computed(() =>
  allSolutions.value.filter(item => {
    if (activeIndustry.value !== '全部' && item.industry !== activeIndustry.value) return false
    if (activeScenarios.value.length > 0) {
      const tags = scenariosOf(item)
      if (!activeScenarios.value.some(s => tags.includes(s))) return false
    }
    return true
  })
)

/** 结果计数：页面一次最多加载 50 条，超出时标注总数（截断透明化） */
const countText = computed(() => {
  const base = `正在展示 ${filteredItems.value.length} 个方案`
  return total.value > allSolutions.value.length
    ? `${base}（共 ${total.value} 个，已加载 ${allSolutions.value.length} 个）`
    : base
})

const toggleScenario = (sc: string) => {
  activeScenarios.value = activeScenarios.value.includes(sc)
    ? activeScenarios.value.filter(s => s !== sc)
    : [...activeScenarios.value, sc]
}

// 筛选变更同步 URL（replace：筛选不污染浏览器历史），分享链接可复原筛选状态
watch([activeIndustry, activeScenarios], () => {
  const query: Record<string, string> = {}
  if (activeIndustry.value !== '全部') query.industry = activeIndustry.value
  if (activeScenarios.value.length) query.scenario = activeScenarios.value.join(',')
  router.replace({ query })
})

const clearFilters = () => {
  activeIndustry.value = '全部'
  activeScenarios.value = []
}

/** URL 携带了数据中不存在的行业/场景值时收敛为有效状态（数据加载后执行） */
const normalizeFilters = () => {
  if (activeIndustry.value !== '全部' && !industryTabs.value.includes(activeIndustry.value)) {
    activeIndustry.value = '全部'
  }
  const valid = activeScenarios.value.filter(s => scenarioTabs.value.includes(s))
  if (valid.length !== activeScenarios.value.length) activeScenarios.value = valid
}

const load = async () => {
  loading.value = true
  try {
    const res = await getArticleList({ pageNum: 1, pageSize: 50, articleType: 'solution' })
    if (res?.code === 200) {
      allSolutions.value = (res.rows || []) as Article[]
      total.value = res.total || allSolutions.value.length
      normalizeFilters()
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const siteName = settings.value.blog_name || '知企建站系统'
  applySeo({
    title: `解决方案 - ${siteName}`,
    description: settings.value.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
  load()
})

// 进入详情时携带筛选 query，详情页「返回」可复原当前筛选（方案文档 §3.2）
const goItem = (id?: number) =>
  id && router.push({ path: `/solutions/${id}`, query: { ...route.query } })
</script>

<style scoped>
.aph {
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

.aph-title {
  margin: 0 0 var(--mo-sp-4);
  font-size: var(--mo-fs-7xl);
  line-height: 1.15;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--tpl-ink-900);
  font-family: var(--tpl-serif);
}

.aph-sub {
  margin: 0;
  font-size: var(--mo-fs-lg);
  line-height: 1.75;
  color: var(--tpl-ink-500);
}

.pad {
  padding: var(--mo-sp-10) 0 var(--mo-sp-12);
}

/* 行业 Tab + 场景标签（§3.3 横向筛选条） */
.sol-tabs,
.sol-scenarios {
  display: flex;
  gap: var(--mo-sp-3);
  flex-wrap: wrap;
}

.sol-scenarios {
  margin-top: var(--mo-sp-4);
}

.sol-scenarios .tag:not(.on) {
  background: var(--tpl-surface);
  color: var(--tpl-ink-500);
}

.sol-clear {
  border: none;
  background: transparent;
  color: var(--tpl-p-text);
  cursor: pointer;
  font-size: var(--mo-fs-sm);
  font-family: inherit;
}

.sol-count {
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
  margin: var(--mo-sp-5) 0 var(--mo-sp-6);
}

/* 方案卡 */
.sol-board-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-6);
}

.sol-board-card {
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

.sol-board-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
  border-color: var(--tpl-p);
}

.sol-board-tags {
  display: flex;
  gap: var(--mo-sp-2);
  flex-wrap: wrap;
  margin-bottom: var(--mo-sp-4);
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

.tag-plain {
  background: var(--tpl-surface);
  color: var(--tpl-ink-400);
}

.sol-board-card h2 {
  margin: 0 0 var(--mo-sp-3);
  font-size: var(--mo-fs-xl);
  color: var(--tpl-ink-900);
}

.sol-board-card p {
  margin: 0 0 var(--mo-sp-5);
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-500);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lnk {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-2);
  color: var(--tpl-p-text);
  font-weight: 500;
  font-size: var(--mo-fs-sm);
}

.sol-skeleton {
  height: 220px;
  background: linear-gradient(90deg, var(--tpl-surface), var(--tpl-bg), var(--tpl-surface));
  background-size: 200% 100%;
  animation: sol-shimmer 1.4s ease infinite;
}

@keyframes sol-shimmer {
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

  .aph {
    padding: calc(var(--mo-sp-10) + 64px) 0 var(--mo-sp-9);
  }

  .aph-title {
    font-size: var(--mo-fs-5xl);
  }

  .sol-board-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 639px) {
  .sol-board-grid {
    grid-template-columns: 1fr;
  }
}
</style>
