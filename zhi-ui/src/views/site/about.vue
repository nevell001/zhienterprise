<template>
  <SiteLayout>
    <div ref="rootEl" class="tpl-site about-page mo-home-page">
      <!-- 页头（文案可在后台「官网内容」页签调整，留空跟随站点名称/描述） -->
      <header class="aph">
        <div class="eh-wrap">
          <span class="eyebrow rv">{{ heroEyebrow }}</span>
          <h1 class="aph-title rv">{{ heroTitle }}</h1>
          <p class="aph-sub rv d1">{{ heroSubtitle }}</p>
        </div>
      </header>

      <main>
        <!-- 企业简介正文：来自页面管理里 slug=about 的已发布页面 -->
        <section class="pad">
          <div class="eh-wrap aph-body-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">企业简介</div>
              <h2>我们是谁</h2>
            </div>
            <!-- 正文经与前台统一的 renderMarkdown + 消毒管线渲染 -->
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div class="aph-content rv" v-html="aboutContentHtml"></div>
          </div>
        </section>

        <!-- 负责人信息：站点设置「个人信息」的简介 / 位置 / 个人网站（三项均留空则整块不渲染） -->
        <section v-if="hasAuthorProfile" class="pad">
          <div class="eh-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">团队</div>
              <h2>负责人</h2>
            </div>
            <div class="aph-profile rv d1">
              <p v-if="authorHeadline" class="aph-profile-head">{{ authorHeadline }}</p>
              <p v-if="authorBio" class="aph-profile-bio">{{ authorBio }}</p>
              <a
                v-if="authorWebsite"
                class="aph-profile-link"
                :href="authorWebsite"
                target="_blank"
                rel="noopener noreferrer"
              >
                个人网站 →
              </a>
            </div>
          </div>
        </section>

        <!-- 数据概览（站点真实统计） -->
        <section class="pad surface">
          <div class="eh-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">数据概览</div>
              <h2>用数字说话</h2>
            </div>
            <div class="aph-stats rv d1">
              <div v-for="s in statItems" :key="s.label" class="aph-stat">
                <b>{{ s.value }}</b>
                <span>{{ s.label }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 荣誉与历程入口 -->
        <section class="pad">
          <div class="eh-wrap aph-links rv">
            <router-link to="/honors" class="aph-link-card">
              <span class="aph-link-title">荣誉资质</span>
              <span class="aph-link-desc">资质证书 · 行业奖项 · 体系认证</span>
            </router-link>
            <router-link to="/history" class="aph-link-card">
              <span class="aph-link-title">发展历程</span>
              <span class="aph-link-desc">从初创到生态的每一步</span>
            </router-link>
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
import { getPublicStatisticsOverview } from '@/api/statistics'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { removeJsonLd, setJsonLd } from '@/utils/jsonld'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { renderMarkdown } from '@/utils/markdown'
import { DEFAULT_PAGE_COPY, getPageCopy } from '@/utils/siteContent'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { logger } from '@/utils/logger'

const siteSettingsStore = useSiteSettingsStore()
const siteSettings = computed(() => siteSettingsStore.siteSettings)

/* 页头文案：「官网内容」页签的页面文案 → 站点名称/描述 → 内置文案 */
const pageCopy = computed(() => getPageCopy(siteSettings.value?.site_page_copy, 'about'))
const heroEyebrow = computed(() => pageCopy.value.eyebrow || DEFAULT_PAGE_COPY.about.eyebrow)
const heroTitle = computed(
  () => pageCopy.value.title || siteSettings.value.blog_name || DEFAULT_PAGE_COPY.about.title
)
const heroSubtitle = computed(
  () => pageCopy.value.subtitle || siteSettings.value.blog_desc || DEFAULT_PAGE_COPY.about.subtitle
)

/* 负责人信息（站点设置「个人信息」页签）。抬头取「作者职位 · 位置信息」，
   卡片内容取个人简介与个人网站；三项内容字段全空时整块不渲染，避免留白卡片 */
const authorTitle = computed(() => String(siteSettings.value.author_title || '').trim())
const authorBio = computed(() => String(siteSettings.value.author_bio || '').trim())
const authorLocation = computed(() => String(siteSettings.value.author_location || '').trim())
const authorWebsite = computed(() => String(siteSettings.value.personal_website || '').trim())
const authorHeadline = computed(() =>
  [authorTitle.value, authorLocation.value].filter(Boolean).join(' · ')
)
const hasAuthorProfile = computed(() =>
  Boolean(authorBio.value || authorLocation.value || authorWebsite.value)
)

/* 负责人结构化数据：与负责人区块同门槛出现；Person 的 name 取站点信息「站长」，
   站长留空时不注入（无 name 的 Person 对搜索引擎无效） */
const authorProfileLd = computed(() => {
  const name = String(siteSettings.value.blog_author || '').trim()
  if (!hasAuthorProfile.value || !name) return null
  return {
    '@context': 'https://schema.org',
    '@type': 'Person',
    name,
    jobTitle: authorTitle.value || undefined,
    description: authorBio.value || undefined,
    url: authorWebsite.value || undefined,
    address: authorLocation.value
      ? { '@type': 'PostalAddress', addressLocality: authorLocation.value }
      : undefined
  }
})

watch(
  authorProfileLd,
  ld => (ld ? setJsonLd('zh-jsonld-person', ld) : removeJsonLd('zh-jsonld-person')),
  { immediate: true }
)

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

// 页面内容仅来自页面管理里 slug=about 的已发布页面；不存在时显示空态
const customAboutContent = ref('')

const aboutContentHtml = computed(() => {
  const raw = customAboutContent.value
  if (!raw || typeof raw !== 'string' || !raw.trim())
    return '企业简介内容可在后台「页面管理」中维护（别名 about）。'
  return sanitizeArticleContent(renderMarkdown(raw))
})

interface SiteStats {
  articleCount?: number
  categoryCount?: number
  tagCount?: number
  commentCount?: number
  totalViews?: number
}

const stats = ref<SiteStats>({})

const statItems = computed(() => [
  { label: '发布内容', value: formatNumber(stats.value.articleCount || 0) },
  { label: '栏目分类', value: formatNumber(stats.value.categoryCount || 0) },
  { label: '累计浏览', value: formatNumber(stats.value.totalViews || 0) },
  { label: '内容标签', value: formatNumber(stats.value.tagCount || 0) }
])

const formatNumber = (num: number) => {
  const n = Number(num) || 0
  return n >= 10000 ? `${(n / 10000).toFixed(1)}w+` : String(n)
}

const loadCustomAbout = async () => {
  try {
    // slug 可选：未建页属正常态，silent 抑制兜底 toast，落在引导文案
    const response = await getPublishedPageBySlug('about', { silent: true })
    customAboutContent.value = response?.data?.content || ''
  } catch {
    // slug=about 的页面不存在或未发布：保持空，页面显示引导文案
    customAboutContent.value = ''
  }
}

const loadStats = async () => {
  try {
    const response = await getPublicStatisticsOverview()
    const data = response?.data || {}
    stats.value = {
      articleCount: data.articleCount || 0,
      categoryCount: data.categoryCount || 0,
      tagCount: data.tagCount || 0,
      commentCount: data.commentCount || 0,
      totalViews: data.totalViews || 0
    }
  } catch (error) {
    logger.error('加载统计数据失败:', error)
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
  loadCustomAbout()
  loadStats()
})

onBeforeUnmount(() => {
  removeJsonLd('zh-jsonld-person')
})
</script>

<style scoped>
@import './about-shared.css';
</style>
