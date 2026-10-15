<template>
  <SiteLayout>
    <!-- builder 页：模块化渲染（sections 顺序即渲染顺序） -->
    <div
      v-if="!loading && page && page.pageType === 'builder'"
      ref="builderRoot"
      class="tpl-site site-page-builder mo-home-page"
    >
      <template v-for="(section, i) in builderSections" :key="`${section.type}-${i}`">
        <component :is="section.component" :config="section.config || {}" />
      </template>
    </div>

    <!-- 富文本页（现状）：CustomPagePanel 统一渲染 -->
    <CustomPagePanel
      v-else
      :page="page"
      :loading="loading"
      :not-found="notFound"
      @retry="loadPage"
    />
  </SiteLayout>
</template>

<script setup lang="ts" name="PublicSitePage">
import { computed, ref, watch } from 'vue'
import SiteLayout from '@/components/SiteLayout.vue'
import CustomPagePanel from './CustomPagePanel.vue'
import { parseSections } from '@/views/site/sections/registry'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getPublishedPageBySlug, type SitePage } from '@/api/site/page'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { logger } from '@/utils/logger'

const route = useRoute()
const siteSettingsStore = useSiteSettingsStore()
const siteSettings = computed(() => siteSettingsStore.siteSettings)

const loading = ref(true)
const notFound = ref(false)
const page = ref<SitePage | null>(null)

const builderRoot = ref<HTMLElement | null>(null)
useScrollReveal(builderRoot)

const builderSections = computed(() => {
  if (page.value?.pageType !== 'builder') return []
  return parseSections(page.value.sections)
})

const slug = computed(() => {
  const raw = route.params.slug
  const value = Array.isArray(raw) ? raw[0] : raw
  return value ? String(value) : ''
})

const loadPage = async () => {
  const currentSlug = slug.value
  if (!currentSlug) {
    page.value = null
    notFound.value = true
    loading.value = false
    return
  }

  loading.value = true
  notFound.value = false
  try {
    const response = await getPublishedPageBySlug(currentSlug)
    if (!response || response.code !== 200 || !response.data) {
      page.value = null
      notFound.value = true
      return
    }

    page.value = response.data
    const siteName = (siteSettings.value as { blog_name?: string }).blog_name || '知企建站系统'
    applySeo({
      title: page.value.seoTitle || `${page.value.title} - ${siteName}`,
      description: page.value.seoDescription || page.value.summary || '',
      keywords: page.value.seoKeywords || '',
      canonical: canonicalUrl()
    })
  } catch (error) {
    // 后端返回「页面不存在或未发布」时会 reject，这里降级为友好的 404 提示
    logger.error('加载自定义页面失败:', error)
    page.value = null
    notFound.value = true
  } finally {
    loading.value = false
  }
}

watch(slug, () => {
  loadPage()
})

onMounted(loadPage)
</script>
