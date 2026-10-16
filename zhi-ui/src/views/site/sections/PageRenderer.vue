<template>
  <div ref="rootEl" class="tpl-site site-page-renderer mo-home-page" :class="tpl.className">
    <!-- builder 页：按 sections 顺序渲染模块 -->
    <template v-if="ready && sections.length > 0">
      <component
        :is="section.component"
        v-for="(section, i) in sections"
        :key="`${section.type}-${i}`"
        :config="section.config || {}"
      />
    </template>

    <!-- 回退：页面不存在 / 非 builder 页 / 未跑迁移脚本的老库 → 旧版企业首页 -->
    <EnterpriseHome v-else-if="ready" />

    <!-- 加载中：首屏骨架 -->
    <div v-else class="spr-skeleton">
      <div class="spr-skel-hero"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EnterpriseHome from '@/views/site/EnterpriseHome.vue'
import { getPublishedPageBySlug } from '@/api/site/page'
import { parseSections } from '@/views/site/sections/registry'
import { getSiteTemplate } from '@/config/siteTemplates'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { logger } from '@/utils/logger'

/** 渲染指定 slug 的 builder 页（默认首页）；失败时回退旧版企业首页组件 */
const props = withDefaults(defineProps<{ slug?: string }>(), { slug: 'home' })

const siteSettingsStore = useSiteSettingsStore()
const tpl = computed(() => getSiteTemplate(siteSettingsStore.siteSettings?.site_template))

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

const ready = ref(false)
const sections = ref<ReturnType<typeof parseSections>>([])

onMounted(async () => {
  try {
    // slug 可选内容：缺失属正常态（未跑迁移脚本的老库），silent 抑制兜底弹窗
    const response = await getPublishedPageBySlug(props.slug, { silent: true })
    const page = (response as { data?: { pageType?: string; sections?: string } })?.data
    if (page && page.pageType === 'builder') {
      sections.value = parseSections(page.sections)
    }
  } catch (error) {
    logger.warn('加载 builder 页面失败，回退旧版首页:', error)
  } finally {
    ready.value = true
  }
})
</script>

<style>
@import './sections.css';

/* 首屏标题分行上浮（§4.3）：keyframes 放全局层（sections.css 结构守卫不解析 from/to） */
@keyframes eh-line-up {
  from {
    opacity: 0;
    transform: translateY(40px);
  }

  to {
    opacity: 1;
    transform: none;
  }
}
</style>

<style scoped>
.spr-skeleton {
  padding: calc(var(--mo-sp-12) + 64px) var(--mo-sp-7) var(--mo-sp-12);
}

.spr-skel-hero {
  max-width: var(--tpl-grid);
  margin: 0 auto;
  height: 420px;
  border-radius: var(--tpl-r-xl);
  background: linear-gradient(90deg, var(--tpl-surface), var(--tpl-bg), var(--tpl-surface));
  background-size: 200% 100%;
  animation: spr-shimmer 1.4s ease infinite;
}

@keyframes spr-shimmer {
  from {
    background-position: 200% 0;
  }

  to {
    background-position: -200% 0;
  }
}
</style>
