<template>
  <SiteLayout>
    <div ref="rootEl" class="tpl-site honors-page mo-home-page">
      <!-- 页头（文案可在后台「官网内容」页签调整） -->
      <header class="aph">
        <div class="eh-wrap">
          <span class="eyebrow rv">{{ heroEyebrow }}</span>
          <h1 class="aph-title rv">{{ heroTitle }}</h1>
          <p class="aph-sub rv d1">{{ heroSubtitle }}</p>
        </div>
      </header>

      <main>
        <!-- 可选的自定义说明：页面管理 slug=honors -->
        <section v-if="customContentHtml" class="pad-sm-wrap">
          <div class="eh-wrap">
            <!-- 内容经统一消毒管线渲染 -->
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div class="aph-content rv" v-html="customContentHtml"></div>
          </div>
        </section>

        <!-- 证书墙：分类 Tab + 网格（数据为设计稿附录 A 占位，接入内容模型后替换） -->
        <section class="pad">
          <div class="eh-wrap">
            <div class="honor-tabs rv" role="tablist">
              <button
                v-for="cat in categories"
                :key="cat"
                type="button"
                class="tag"
                :class="{ on: activeCat === cat }"
                role="tab"
                :aria-selected="activeCat === cat"
                @click="activeCat = cat"
              >
                {{ cat }}
              </button>
            </div>
            <div class="honor-grid rv">
              <div
                v-for="(item, hi) in filteredItems"
                :key="item.name"
                class="honor-card"
                :class="`d${hi % 4}`"
              >
                <div class="honor-seal" aria-hidden="true">
                  <svg width="34" height="34" viewBox="0 0 34 34" fill="none">
                    <circle cx="17" cy="13" r="8" stroke="currentColor" stroke-width="1.5" />
                    <path
                      d="M13 20l-2 10 6-3.5L23 30l-2-10"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linejoin="round"
                    />
                  </svg>
                </div>
                <h3>{{ item.name }}</h3>
                <p class="honor-issuer">{{ item.issuer }}</p>
                <span class="honor-year">{{ item.year }}</span>
              </div>
            </div>
            <p v-if="filteredItems.length === 0" class="honors-empty rv">该分类下暂无内容</p>
          </div>
        </section>
      </main>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import SiteLayout from '@/components/SiteLayout.vue'
import { getPublishedPageBySlug } from '@/api/site/page'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { removeJsonLd, setJsonLd } from '@/utils/jsonld'
import { getSiteHonors, DEFAULT_PAGE_COPY, getPageCopy } from '@/utils/siteContent'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { renderMarkdown } from '@/utils/markdown'
import { logger } from '@/utils/logger'
import { useScrollReveal } from '@/composables/useScrollReveal'

const siteSettingsStore = useSiteSettingsStore()
const siteSettings = computed(() => siteSettingsStore.siteSettings)

/* 页头文案：「官网内容」页签的页面文案 → 内置文案 */
const pageCopy = computed(() => getPageCopy(siteSettings.value?.site_page_copy, 'honors'))
const heroEyebrow = computed(() => pageCopy.value.eyebrow || DEFAULT_PAGE_COPY.honors.eyebrow)
const heroTitle = computed(() => pageCopy.value.title || DEFAULT_PAGE_COPY.honors.title)
const heroSubtitle = computed(() => pageCopy.value.subtitle || DEFAULT_PAGE_COPY.honors.subtitle)

// 可选的自定义说明（页面管理 slug=honors），证书墙数据为占位
const customContent = ref('')

const customContentHtml = computed(() => {
  const raw = customContent.value
  if (!raw || typeof raw !== 'string' || !raw.trim()) return ''
  return sanitizeArticleContent(renderMarkdown(raw))
})

/* 荣誉数据：管理后台「官网内容」页签维护（缺失回退设计稿附录 A 占位） */
const honorItems = computed(() => getSiteHonors(siteSettings.value?.site_honors))

// JSON-LD 结构化数据（方案文档 §6.2「企业与荣誉」：Organization + Award；
// 颁发方/时间并入 description，Award 无独立字段承载）
watch(
  honorItems,
  items => {
    if (!items.length) {
      removeJsonLd('zh-jsonld-awards')
      return
    }
    setJsonLd('zh-jsonld-awards', {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      name: siteSettings.value.blog_name || '知企建站系统',
      url: `${window.location.origin}/`,
      award: items.map(item => ({
        '@type': 'Award',
        name: item.name,
        description: [item.issuer, item.year].filter(Boolean).join(' · ') || undefined
      }))
    })
  },
  { immediate: true }
)
const categories = computed(() => {
  const cats = Array.from(new Set(honorItems.value.map(item => item.cat).filter(Boolean)))
  return ['全部', ...cats]
})

const activeCat = ref<string>('全部')
const filteredItems = computed(() =>
  activeCat.value === '全部'
    ? honorItems.value
    : honorItems.value.filter(item => item.cat === activeCat.value)
)

// 数据变化（设置异步加载）后分类可能新增，当前选中项不存在时回退「全部」
watch(categories, cats => {
  if (!cats.includes(activeCat.value)) activeCat.value = '全部'
})

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

const loadCustomContent = async () => {
  try {
    // slug 可选：未建页属正常态，silent 抑制兜底 toast，仅展示证书墙
    const response = await getPublishedPageBySlug('honors', { silent: true })
    customContent.value = response?.data?.content || ''
  } catch {
    // slug=honors 的页面不存在或未发布：仅展示证书墙
    customContent.value = ''
  }
}

const applyPageSeo = () => {
  const siteName = siteSettings.value.blog_name || '知企建站系统'
  applySeo({
    title: `${heroTitle.value} - ${siteName}`,
    description: siteSettings.value.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
}

watch(heroTitle, applyPageSeo, { immediate: true })

onMounted(() => {
  loadCustomContent()
})

onBeforeUnmount(() => {
  removeJsonLd('zh-jsonld-awards')
})
</script>

<style scoped>
@import './about-shared.css';

.pad-sm-wrap {
  padding: var(--mo-sp-10) 0 0;
}

/* 分类 Tab */
.honor-tabs {
  display: flex;
  gap: var(--mo-sp-3);
  flex-wrap: wrap;
  margin-bottom: var(--mo-sp-8);
}

/* 分类 Tab：芯片式过滤器（sections.css 仅在 builder 页加载，本页独立实现）
   选中态=实底主色+反白（与 .bd-cta-btn 同约定）；触控目标 ≥44px（方案文档 §5.3）用透明内边距扩展 */
.honor-tabs .tag {
  position: relative;
  display: inline-block;
  font-family: inherit;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: var(--mo-sp-2) var(--mo-sp-4);
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  transition:
    background 0.2s,
    color 0.2s,
    border-color 0.2s;
}

.honor-tabs .tag::after {
  content: '';
  position: absolute;
  inset: -7px -4px;
}

.honor-tabs .tag:hover {
  border-color: var(--tpl-p);
}

.honor-tabs .tag.on {
  background: var(--tpl-p);
  color: var(--tpl-white);
  border-color: var(--tpl-p);
}

/* 证书墙 */
.honor-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--mo-sp-6);
}

.honor-card {
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  background: var(--tpl-bg);
  padding: var(--mo-sp-7);
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s,
    border-color 0.3s;
}

.honor-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
  border-color: var(--tpl-p);
}

.honor-seal {
  width: 52px;
  height: 52px;
  border-radius: var(--tpl-r-md);
  background: var(--tpl-p-sub);
  color: var(--tpl-p-text);
  display: grid;
  place-items: center;
  margin-bottom: var(--mo-sp-5);
}

.honor-card h3 {
  font-size: var(--mo-fs-lg);
  margin: 0 0 var(--mo-sp-2);
  color: var(--tpl-ink-900);
}

.honor-issuer {
  margin: 0 0 var(--mo-sp-4);
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
}

.honor-year {
  display: inline-block;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: 999px;
}

.honors-empty {
  text-align: center;
  color: var(--tpl-ink-400);
  font-size: var(--mo-fs-sm);
}

@media (max-width: 991px) {
  .honor-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 639px) {
  .honor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
