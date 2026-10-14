<template>
  <div class="business-detail mo-home-page">
    <div class="bd-container">
      <button type="button" class="bd-back" @click="goBack">← 返回{{ businessLabel }}</button>

      <template v-if="article">
        <h1 class="bd-title">{{ article.title }}</h1>
        <div class="bd-meta">
          <span v-if="article.createTime">发布于 {{ formatDate(article.createTime) }}</span>
          <span v-if="isViewCountEnabled">{{ article.viewCount ?? 0 }} 次浏览</span>
        </div>
        <img
          v-if="article.coverUrl"
          :src="article.coverUrl"
          class="bd-cover"
          :alt="article.title"
        />
        <!-- 正文契约与文章详情一致：HTML 经消毒后渲染，不做 Markdown 渲染 -->
        <div class="bd-content" v-html="sanitizedContent"></div>
      </template>
      <el-empty v-else-if="!loading" description="内容不存在或未发布" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleDetail, updateArticleViewCount } from '@/api/blog/article'
import type { Article } from '@/types'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { removeJsonLd, setJsonLd } from '@/utils/jsonld'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { parseTime } from '@/utils/zhi'
import { logger } from '@/utils/logger'

const router = useRouter()
const blogSettingsStore = useBlogSettingsStore()
const settings = computed(() => blogSettingsStore.blogSettings)
const businessLabel = computed(() => String(settings.value.business_label || '').trim() || '核心业务')
const isViewCountEnabled = computed(() => blogSettingsStore.isFeatureEnabled('view_count_enabled'))

const article = ref<Article | null>(null)
const loading = ref(true)

const sanitizedContent = computed(() => sanitizeArticleContent(article.value?.content || ''))

const formatDate = (date?: string) => (date ? parseTime(date, '{y}-{m}-{d}') : '')

const goBack = () => router.push('/blog/business')

onBeforeUnmount(() => {
  removeJsonLd('zh-jsonld-article')
})

onMounted(async () => {
  const id = Number(router.currentRoute.value.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    loading.value = false
    return
  }
  try {
    const response = await getArticleDetail(id)
    if (response && response.code === 200 && response.data?.article) {
      article.value = response.data.article
      const siteName = settings.value.blog_name || '知企建站系统'
      applySeo({
        title: article.value.seoTitle || (article.value.title ? `${article.value.title} - ${siteName}` : siteName),
        description: article.value.seoDescription || article.value.summary || '',
        keywords: article.value.seoKeywords || '',
        canonical: canonicalUrl()
      })
      // JSON-LD 结构化数据（Article）
      setJsonLd('zh-jsonld-article', {
        '@context': 'https://schema.org',
        '@type': 'Article',
        headline: article.value.title,
        description: article.value.seoDescription || article.value.summary || undefined,
        image: article.value.coverUrl || undefined,
        datePublished: article.value.createTime || undefined,
        author: { '@type': 'Person', name: article.value.authorName || 'admin' }
      })
      // 浏览计数与文章详情同一开关、同一接口
      if (isViewCountEnabled.value) {
        updateArticleViewCount(id).catch(() => {
          // 计数失败不打扰用户
        })
      }
    }
  } catch (error) {
    logger.error('加载业务详情失败:', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.bd-container {
  max-width: 820px;
  margin: 0 auto;
  padding: var(--mo-sp-6) var(--mo-sp-5) var(--mo-sp-9);
}

.bd-back {
  border: none;
  background: transparent;
  color: var(--mo-p600);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  margin-bottom: var(--mo-sp-5);
}

.bd-back:hover {
  color: var(--mo-p500);
}

.bd-title {
  margin: 0 0 var(--mo-sp-3);
  font-size: var(--mo-fs-2xl);
  line-height: 1.4;
  color: var(--mo-n900);
}

.bd-meta {
  display: flex;
  gap: var(--mo-sp-4);
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
  margin-bottom: var(--mo-sp-5);
}

.bd-cover {
  width: 100%;
  border-radius: var(--mo-r-lg);
  margin-bottom: var(--mo-sp-6);
}

.bd-content {
  color: var(--mo-n700);
  font-size: var(--mo-fs-md);
  line-height: 1.85;
  word-break: break-word;
}

.bd-content :deep(h1),
.bd-content :deep(h2),
.bd-content :deep(h3) {
  color: var(--mo-n900);
  margin: var(--mo-sp-6) 0 var(--mo-sp-3);
}

.bd-content :deep(img) {
  max-width: 100%;
  border-radius: var(--mo-r-md);
}

.bd-content :deep(a) {
  color: var(--mo-p600);
}
</style>
