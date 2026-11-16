<template>
  <SiteLayout>
    <ArticlePreviewBanner
      v-if="isPreview && page"
      :status="page.status"
      :publish-time="page.publishTime"
      admin-path="/admin/page"
    />
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
import ArticlePreviewBanner from '@/components/ArticlePreviewBanner.vue'
import CustomPagePanel from './CustomPagePanel.vue'
import { parseSections } from '@/views/site/sections/registry'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getPagePreview, getPublishedPageBySlug, type SitePage } from '@/api/site/page'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { createRequestGuard } from '@/utils/requestGuard'
import { logger } from '@/utils/logger'

const route = useRoute()
const siteSettingsStore = useSiteSettingsStore()
const siteSettings = computed(() => siteSettingsStore.siteSettings)

const loading = ref(true)
const notFound = ref(false)
const page = ref<SitePage | null>(null)
const isPreview = computed(() => route.meta.preview === true)

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

const pageId = computed(() => {
  const raw = route.params.id
  const value = Array.isArray(raw) ? raw[0] : raw
  return value ? String(value) : ''
})

// 陈旧响应守卫：详见 loadPage 内注释
const pageGuard = createRequestGuard()

const loadPage = async () => {
  const currentSlug = slug.value
  const currentId = pageId.value
  const preview = isPreview.value
  if ((!preview && !currentSlug) || (preview && !currentId)) {
    // 标识清空即作废在途请求，防止旧响应稍后覆盖 404 态
    pageGuard.invalidate()
    page.value = null
    notFound.value = true
    loading.value = false
    return
  }

  loading.value = true
  notFound.value = false
  const token = pageGuard.next()
  try {
    // 预览走受权限保护的后台接口（草稿/定时稿均可），公开页走按 slug 的匿名接口
    const response = preview
      ? await getPagePreview(currentId)
      : await getPublishedPageBySlug(currentSlug, { silent: true })
    // 陈旧响应守卫：/page/:slug 与预览路由复用组件（router-view 无 :key），
    // 快速切换目标时慢的旧响应不得覆盖新页面（内容与 SEO 同理）
    if (!pageGuard.isLatest(token)) return
    if (!response || response.code !== 200 || !response.data) {
      page.value = null
      notFound.value = true
      return
    }

    page.value = response.data
    const siteName = (siteSettings.value as { blog_name?: string }).blog_name || '知企建站系统'
    applySeo({
      title: preview
        ? `【预览】${page.value.title || '未命名页面'}`
        : page.value.seoTitle || `${page.value.title} - ${siteName}`,
      description: page.value.seoDescription || page.value.summary || '',
      keywords: page.value.seoKeywords || '',
      canonical: preview ? undefined : canonicalUrl(),
      robots: preview ? 'noindex,nofollow' : undefined
    })
  } catch (error) {
    if (!pageGuard.isLatest(token)) return
    // 后端返回「页面不存在或未发布」时会 reject，这里降级为友好的 404 提示
    logger.error('加载自定义页面失败:', error)
    page.value = null
    notFound.value = true
  } finally {
    // 仅由最新一次请求收尾 loading，避免旧响应提前关掉新请求的加载态
    if (pageGuard.isLatest(token)) loading.value = false
  }
}

watch([slug, pageId, isPreview], () => {
  loadPage()
})

onMounted(loadPage)

// 卸载后作废在途请求：慢响应不再写回状态/SEO，避免污染下一页面
onUnmounted(() => {
  pageGuard.invalidate()
})
</script>
