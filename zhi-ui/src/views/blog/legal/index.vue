<template>
  <BlogLayout>
    <div class="legal-page mo-legal-page">
      <div class="legal-shell">
        <section class="legal-panel">
          <header class="legal-head">
            <span class="section-label">Legal</span>
            <h1 class="legal-title">{{ pageTitle }}</h1>
          </header>
          <div class="legal-content" v-html="contentHtml"></div>
        </section>
      </div>
    </div>
  </BlogLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import BlogLayout from '@/components/BlogLayout.vue'
import { getBlogSettingsAnonymous } from '@/api/blog/setting'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { logger } from '@/utils/logger'
import { PRIVACY_POLICY_HTML, USER_AGREEMENT_HTML } from './content'

const PAGE_MAP = {
  PublicBlogUserAgreement: {
    title: '用户协议',
    settingKey: 'user_agreement_content',
    fallback: USER_AGREEMENT_HTML
  },
  PublicBlogPrivacyPolicy: {
    title: '隐私政策',
    settingKey: 'privacy_policy_content',
    fallback: PRIVACY_POLICY_HTML
  }
} as const

const route = useRoute()
const blogSettingsStore = useBlogSettingsStore()
const blogSettings = computed(() => blogSettingsStore.blogSettings)

const pageMeta = computed(() => {
  const name = route.name as keyof typeof PAGE_MAP
  return PAGE_MAP[name] ?? PAGE_MAP.PublicBlogUserAgreement
})

const pageTitle = computed(() => pageMeta.value.title)

const contentHtml = computed(() => {
  const raw = blogSettings.value[pageMeta.value.settingKey]
  // 数据库未配置(种子未初始化或旧版后端未放行该键)时展示内置标准文本
  if (raw && typeof raw === 'string' && raw.trim()) {
    return sanitizeArticleContent(raw)
  }
  return sanitizeArticleContent(pageMeta.value.fallback)
})

const loadBlogSettings = async () => {
  try {
    const response = await getBlogSettingsAnonymous()
    const settings = response?.data || {}
    blogSettingsStore.updateBlogSettings(settings)
  } catch (error) {
    logger.error('加载站点设置失败:', error)
  }
}

onMounted(() => {
  loadBlogSettings()
})
</script>

<style scoped>
.mo-legal-page {
  min-height: 100vh;
  /* 顶部 84px 为固定导航避让（与 about/guestbook 等页面同口径），刻度上无此值，用 calc 组合表达 */
  padding: calc(var(--mo-sp-8) * 2 + var(--mo-sp-1)) var(--mo-sp-6)
    calc(var(--mo-sp-8) * 2 + var(--mo-sp-2));
  background: var(--mo-n50);
  color: var(--mo-n800);
  font-family: var(--mo-font-sans);
}

.legal-shell {
  width: min(860px, 100%);
  margin: 0 auto;
}

.legal-panel {
  padding: var(--mo-sp-7) var(--mo-sp-8) var(--mo-sp-8);
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: #fff;
  box-shadow: var(--mo-shadow-sm);
}

.section-label {
  display: inline-flex;
  margin-bottom: var(--mo-sp-3);
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: var(--mo-r-full);
  background: var(--mo-p50);
  color: var(--mo-p700);
  font-size: var(--mo-fs-xs);
  font-weight: 700;
}

.legal-title {
  margin: 0;
  color: var(--mo-n900);
  font-family: var(--mo-font-serif);
  font-size: var(--mo-fs-2xl);
  font-weight: 700;
  line-height: 1.3;
}

.legal-content {
  margin-top: var(--mo-sp-5);
  color: var(--mo-n700);
  font-family: var(--mo-font-serif);
  font-size: var(--mo-fs-md);
  line-height: 1.9;
}

.legal-content :deep(h3) {
  margin: var(--mo-sp-6) 0 var(--mo-sp-3);
  color: var(--mo-n900);
  font-family: var(--mo-font-serif);
  font-size: var(--mo-fs-lg);
  font-weight: 700;
}

.legal-content :deep(p) {
  margin: 0 0 var(--mo-sp-4);
}

.legal-content :deep(ul) {
  margin: 0 0 var(--mo-sp-4);
  padding-left: var(--mo-sp-5);
}

.legal-content :deep(li) {
  margin-bottom: var(--mo-sp-1);
}

html.dark .mo-legal-page {
  background: var(--mo-n900);
  color: var(--mo-n100);
}

html.dark .legal-panel {
  border-color: var(--mo-n700);
  background: var(--mo-n800);
}

html.dark .section-label {
  background: var(--mo-p50);
  color: var(--mo-p300);
}

html.dark .legal-title,
html.dark .legal-content :deep(h3) {
  color: var(--mo-n200);
}

html.dark .legal-content {
  color: var(--mo-n500);
}

@media (max-width: 768px) {
  .mo-legal-page {
    padding: calc(var(--mo-sp-8) * 2 + var(--mo-sp-2)) var(--mo-sp-4) var(--mo-sp-6);
  }

  .legal-panel {
    padding: var(--mo-sp-5) var(--mo-sp-5) var(--mo-sp-7);
  }
}
</style>
