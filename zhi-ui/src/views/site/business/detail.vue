<template>
  <SiteLayout>
    <div ref="rootEl" class="tpl-site business-detail mo-home-page">
      <!-- 面包屑（方案文档 §3.2 详情页；组件同出 BreadcrumbList JSON-LD） -->
      <SiteBreadcrumb class="bd-crumb" :items="breadcrumbItems" />

      <main class="bd-main eh-wrap">
        <template v-if="article">
          <div class="bd-head rv">
            <h1 class="bd-title">{{ article.title }}</h1>
            <div class="bd-meta">
              <span v-if="article.createTime">发布于 {{ formatDate(article.createTime) }}</span>
              <span v-if="isViewCountEnabled">{{ article.viewCount ?? 0 }} 次浏览</span>
            </div>
          </div>
          <img
            v-if="article.coverUrl"
            :src="article.coverUrl"
            class="bd-cover rv"
            :alt="article.title"
          />
          <!-- 正文契约与文章详情一致：HTML 经消毒后渲染，不做 Markdown 渲染 -->
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div class="bd-content rv" v-html="sanitizedContent"></div>

          <!-- 相关推荐：同类型最新（排除自身） -->
          <section v-if="relatedItems.length" class="bd-related">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">相关推荐</div>
              <h2>继续了解</h2>
            </div>
            <div class="bd-rel-grid">
              <article
                v-for="(item, ri) in relatedItems"
                :key="item.id"
                class="bd-rel-card rv"
                :class="`d${ri % 3}`"
                @click="goItem(item.id)"
              >
                <h3>{{ item.title }}</h3>
                <p>{{ item.summary }}</p>
              </article>
            </div>
          </section>

          <!-- 底部转化条（方案文档 §3.2：滚动至页尾的固定转化区块） -->
          <section class="bd-cta rv">
            <h2>对该{{ businessLabel }}感兴趣？</h2>
            <p>联系我们获取方案与报价。</p>
            <router-link class="bd-cta-btn" to="/contact">
              咨询该{{ businessLabel }}
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path
                  d="M3 8h10M9 4l4 4-4 4"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </router-link>
          </section>
        </template>
        <el-empty v-else-if="!loading" description="内容不存在或未发布" />

        <div class="bd-back-wrap">
          <button type="button" class="bd-back" @click="goBack">← 返回{{ businessLabel }}</button>
        </div>
      </main>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SiteLayout from '@/components/SiteLayout.vue'
import SiteBreadcrumb from '@/components/SiteBreadcrumb.vue'
import { getArticleDetail, getArticleList, updateArticleViewCount } from '@/api/site/article'
import type { Article } from '@/types'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { removeJsonLd, setJsonLd } from '@/utils/jsonld'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { parseTime } from '@/utils/zhi'
import { logger } from '@/utils/logger'
import { useScrollReveal } from '@/composables/useScrollReveal'

const router = useRouter()
const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const businessLabel = computed(
  () => String(settings.value.business_label || '').trim() || '核心业务'
)
const isViewCountEnabled = computed(() => siteSettingsStore.isFeatureEnabled('view_count_enabled'))

const article = ref<Article | null>(null)
const relatedItems = ref<Article[]>([])
const loading = ref(true)

const sanitizedContent = computed(() => sanitizeArticleContent(article.value?.content || ''))

const formatDate = (date?: string) => (date ? parseTime(date, '{y}-{m}-{d}') : '')

const goBack = () => router.push('/business')
const goItem = (id?: number) => id && router.push(`/business/${id}`)

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

// 面包屑链路（≤3 级）：首页 → 业务栏目 → 当前条目
const breadcrumbItems = computed(() => [
  { label: '首页', to: '/' },
  { label: businessLabel.value, to: '/business' },
  { label: article.value?.title || '详情' }
])

onMounted(async () => {
  // 路由参数兼容数字 ID（历史链接）与 slug 伪静态链接，由后端统一解析
  const key = String(router.currentRoute.value.params.id || '')
  if (!key) {
    loading.value = false
    return
  }
  try {
    const response = await getArticleDetail(key)
    if (response && response.code === 200 && response.data?.article) {
      article.value = response.data.article
      const siteName = settings.value.blog_name || '知企建站系统'
      applySeo({
        title:
          article.value.seoTitle ||
          (article.value.title ? `${article.value.title} - ${siteName}` : siteName),
        description: article.value.seoDescription || article.value.summary || '',
        keywords: article.value.seoKeywords || '',
        canonical: canonicalUrl()
      })
      // JSON-LD 结构化数据（方案文档 §6.2「产品/服务详情」：Service + 名称/描述/图片/所属企业）
      setJsonLd('zh-jsonld-service', {
        '@context': 'https://schema.org',
        '@type': 'Service',
        name: article.value.title,
        description: article.value.seoDescription || article.value.summary || undefined,
        image: article.value.coverUrl || undefined,
        provider: {
          '@type': 'Organization',
          name: siteName,
          url: `${window.location.origin}/`
        }
      })
      // 浏览计数与文章详情同一开关、同一接口；slug 入口时用响应里的真实 ID
      const articleId = article.value.id
      if (isViewCountEnabled.value && articleId) {
        updateArticleViewCount(articleId).catch(() => {
          // 计数失败不打扰用户
        })
      }
      // 相关推荐：同类型最新（排除自身）
      try {
        const relRes = await getArticleList({ pageNum: 1, pageSize: 4, articleType: 'business' })
        if (relRes?.code === 200) {
          relatedItems.value = ((relRes.rows || []) as Article[])
            .filter(item => item.id !== articleId)
            .slice(0, 3)
        }
      } catch {
        relatedItems.value = []
      }
    }
  } catch (error) {
    logger.error('加载业务详情失败:', error)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  removeJsonLd('zh-jsonld-service')
})
</script>

<style scoped>
@import '../about-shared.css';

/* ---------- 面包屑（样式在 SiteBreadcrumb，组件根继承页面作用域） ---------- */
.bd-crumb {
  padding: calc(64px + var(--mo-sp-5)) var(--mo-sp-7) 0;
  max-width: 820px;
  margin: 0 auto;
}

@media (max-width: 639px) {
  .bd-crumb {
    padding-left: var(--mo-sp-5);
    padding-right: var(--mo-sp-5);
  }
}

/* ---------- 正文区 ---------- */
.bd-main {
  padding: var(--mo-sp-7) var(--mo-sp-7) var(--mo-sp-11);
  max-width: 820px;
}

.bd-title {
  margin: 0 0 var(--mo-sp-3);
  font-size: var(--mo-fs-5xl);
  line-height: 1.35;
  color: var(--tpl-ink-900);
  font-family: var(--tpl-serif);
}

.bd-meta {
  display: flex;
  gap: var(--mo-sp-4);
  color: var(--tpl-ink-400);
  font-size: var(--mo-fs-sm);
  margin-bottom: var(--mo-sp-6);
}

.bd-cover {
  width: 100%;
  border-radius: var(--tpl-r-lg);
  margin-bottom: var(--mo-sp-7);
}

.bd-content {
  color: var(--tpl-ink-700);
  font-size: var(--mo-fs-md);
  line-height: 1.85;
  word-break: break-word;
}

.bd-content :deep(h1),
.bd-content :deep(h2),
.bd-content :deep(h3) {
  color: var(--tpl-ink-900);
  margin: var(--mo-sp-7) 0 var(--mo-sp-3);
}

.bd-content :deep(img) {
  max-width: 100%;
  border-radius: var(--tpl-r-md);
}

.bd-content :deep(a) {
  color: var(--tpl-p-text);
}

/* ---------- 相关推荐 ---------- */
.bd-related {
  margin-top: var(--mo-sp-12);
}

.bd-rel-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-6);
}

.bd-rel-card {
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  padding: var(--mo-sp-6);
  background: var(--tpl-bg);
  cursor: pointer;
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s,
    border-color 0.3s;
}

.bd-rel-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
  border-color: var(--tpl-p);
}

.bd-rel-card h3 {
  font-size: var(--mo-fs-md);
  margin: 0 0 var(--mo-sp-2);
  color: var(--tpl-ink-900);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bd-rel-card p {
  margin: 0;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ---------- 底部转化条 ---------- */
.bd-cta {
  margin-top: var(--mo-sp-11);
  padding: var(--mo-sp-9) var(--mo-sp-7);
  text-align: center;
  border-radius: var(--tpl-r-xl);
  background:
    radial-gradient(circle at 80% 18%, rgba(255, 255, 255, 0.14), transparent 40%), var(--tpl-dark);
}

.bd-cta h2 {
  margin: 0 0 var(--mo-sp-3);
  color: var(--tpl-white);
  font-size: var(--mo-fs-3xl);
}

.bd-cta p {
  margin: 0 0 var(--mo-sp-6);
  color: var(--tpl-dark-dim);
  font-size: var(--mo-fs-md);
}

.bd-cta-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-2);
  padding: var(--mo-sp-4) var(--mo-sp-6);
  border-radius: var(--tpl-r-md);
  background: var(--tpl-p);
  color: var(--tpl-white);
  font-weight: 600;
  text-decoration: none;
  transition:
    transform 0.2s cubic-bezier(0.16, 1, 0.3, 1),
    background 0.2s,
    box-shadow 0.2s;
}

.bd-cta-btn:hover {
  background: var(--tpl-p-h);
  transform: translateY(-2px);
  box-shadow: var(--tpl-sh-md);
}

/* ---------- 返回 ---------- */
.bd-back-wrap {
  margin-top: var(--mo-sp-9);
  text-align: center;
}

.bd-back {
  border: none;
  background: transparent;
  color: var(--tpl-p-text);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
  cursor: pointer;
  padding: var(--mo-sp-2);
}

.bd-back:hover {
  color: var(--tpl-p);
}

@media (max-width: 991px) {
  .bd-rel-grid {
    grid-template-columns: 1fr;
  }

  .eh-wrap {
    padding: 0 var(--mo-sp-5);
  }
}
</style>
