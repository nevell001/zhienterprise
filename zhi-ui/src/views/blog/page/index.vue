<template>
  <BlogLayout>
    <CustomPagePanel :page="page" :loading="loading" :not-found="notFound" @retry="loadPage" />
  </BlogLayout>
</template>

<script setup lang="ts" name="PublicBlogPage">
import BlogLayout from '@/components/BlogLayout.vue'
import CustomPagePanel from './CustomPagePanel.vue'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { getPublishedPageBySlug, type BlogPage } from '@/api/blog/page'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { logger } from '@/utils/logger'

const route = useRoute()
const blogSettingsStore = useBlogSettingsStore()
const blogSettings = computed(() => blogSettingsStore.blogSettings)

const loading = ref(true)
const notFound = ref(false)
const page = ref<BlogPage | null>(null)

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
    const siteName = (blogSettings.value as { blog_name?: string }).blog_name || '企业官网项目'
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
