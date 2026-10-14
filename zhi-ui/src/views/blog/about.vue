<template>
  <div class="tpl-site about-page mo-home-page">
    <!-- 页头 -->
    <header class="aph">
      <div class="eh-wrap">
        <span class="eyebrow rv">About Us</span>
        <h1 class="aph-title rv">{{ blogSettings.blog_name || '关于我们' }}</h1>
        <p class="aph-sub rv d1">
          {{ blogSettings.blog_desc || '以专业的产品与服务，持续为客户创造价值。' }}
        </p>
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
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getPublishedPageBySlug } from '@/api/blog/page'
import { getPublicStatisticsOverview } from '@/api/statistics'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { renderMarkdown } from '@/utils/markdown'
import { logger } from '@/utils/logger'

const blogSettingsStore = useBlogSettingsStore()
const blogSettings = computed(() => blogSettingsStore.blogSettings)

// 页面内容仅来自页面管理里 slug=about 的已发布页面；不存在时显示空态
const customAboutContent = ref('')

const aboutContentHtml = computed(() => {
  const raw = customAboutContent.value
  if (!raw || typeof raw !== 'string' || !raw.trim()) return '企业简介内容可在后台「页面管理」中维护（别名 about）。'
  return sanitizeArticleContent(renderMarkdown(raw))
})

interface BlogStats {
  articleCount?: number
  categoryCount?: number
  tagCount?: number
  commentCount?: number
  totalViews?: number
}

const stats = ref<BlogStats>({})

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
    const response = await getPublishedPageBySlug('about')
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

onMounted(() => {
  const siteName = blogSettings.value.blog_name || '知企建站系统'
  applySeo({
    title: `关于我们 - ${siteName}`,
    description: blogSettings.value.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
  loadCustomAbout()
  loadStats()
})
</script>

<style scoped>
@import './about-shared.css';
</style>
