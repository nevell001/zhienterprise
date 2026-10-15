<template>
  <SiteLayout>
    <div ref="rootEl" class="tpl-site history-page mo-home-page">
      <!-- 页头 -->
      <header class="aph">
        <div class="eh-wrap">
          <span class="eyebrow rv">Milestones</span>
          <h1 class="aph-title rv">发展历程</h1>
          <p class="aph-sub rv d1">从初创到生态，每一步都算数。</p>
        </div>
      </header>

      <main class="pad">
        <div class="eh-wrap">
          <!-- 时间轴：阶段分组，荣誉节点用方形区分（方案文档 §3.5）。
               事件数据为设计稿附录 A 占位，接入内容模型后替换。 -->
          <div class="tl">
            <div v-for="stage in stages" :key="stage.name" class="tl-stage">
              <div class="tl-stage-head rv">
                <span class="tl-range">{{ stage.range }}</span>
                <h2>{{ stage.name }}</h2>
                <p>{{ stage.theme }}</p>
              </div>
              <div class="tl-events">
                <div
                  v-for="(event, ei) in stage.events"
                  :key="event.title"
                  class="tl-event rv"
                  :class="`d${ei % 4}`"
                >
                  <span class="tl-node" :class="{ square: event.honor }" aria-hidden="true"></span>
                  <div class="tl-body">
                    <span class="tl-year">{{ event.year }}</span>
                    <h3>
                      {{ event.title }}
                      <span v-if="event.honor" class="tag">荣誉</span>
                    </h3>
                    <p>{{ event.desc }}</p>
                  </div>
                </div>
              </div>
            </div>
            <div class="tl-end rv">
              <span class="tl-node square" aria-hidden="true"></span>
              <p>未来，正在书写。</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import SiteLayout from '@/components/SiteLayout.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { getSiteHistoryStages } from '@/utils/siteContent'
import { useScrollReveal } from '@/composables/useScrollReveal'

const siteSettingsStore = useSiteSettingsStore()

/* 历程数据：管理后台「官网内容」页签维护（缺失回退设计稿附录 A 占位） */
const stages = computed(() => getSiteHistoryStages(siteSettingsStore.siteSettings?.site_history))

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

onMounted(() => {
  const siteName = siteSettingsStore.siteSettings?.blog_name || '知企建站系统'
  applySeo({
    title: `发展历程 - ${siteName}`,
    description: siteSettingsStore.siteSettings?.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
})
</script>

<style scoped>
@import './about-shared.css';

/* ---------- 时间轴 ---------- */
.tl {
  max-width: 880px;
}

.tl-stage-head {
  display: flex;
  align-items: baseline;
  gap: var(--mo-sp-5);
  flex-wrap: wrap;
  margin: var(--mo-sp-9) 0 var(--mo-sp-6);
}

.tl-stage-head:first-child {
  margin-top: 0;
}

.tl-range {
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: 999px;
}

.tl-stage-head h2 {
  margin: 0;
  font-size: var(--mo-fs-2xl);
  color: var(--tpl-ink-900);
}

.tl-stage-head p {
  margin: 0;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
}

.tl-events {
  border-left: 2px solid var(--tpl-border);
  margin-left: 7px;
  padding-left: var(--mo-sp-7);
  display: flex;
  flex-direction: column;
  gap: var(--mo-sp-7);
}

.tl-event {
  position: relative;
  display: flex;
  gap: var(--mo-sp-5);
  align-items: flex-start;
}

/* 节点：普通圆形 / 荣誉方形 */
.tl-node {
  position: absolute;
  left: calc(-1 * var(--mo-sp-7) - 8px);
  top: 6px;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: var(--tpl-p);
  box-shadow: 0 0 0 3px var(--tpl-bg);
}

.tl-node.square {
  border-radius: 3px;
}

.tl-year {
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  color: var(--tpl-p-text);
  font-variant-numeric: tabular-nums;
}

.tl-body h3 {
  margin: var(--mo-sp-1) 0 var(--mo-sp-2);
  font-size: var(--mo-fs-lg);
  color: var(--tpl-ink-900);
  display: flex;
  align-items: center;
  gap: var(--mo-sp-3);
}

.tl-body p {
  margin: 0;
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-500);
  line-height: 1.75;
  max-width: 620px;
}

.tag {
  display: inline-block;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: 2px var(--mo-sp-3);
  border-radius: 999px;
}

.tl-end {
  margin-top: var(--mo-sp-9);
  padding-left: var(--mo-sp-7);
  border-left: 2px solid var(--tpl-border);
  position: relative;
}

.tl-end .tl-node {
  left: -8px;
  top: 4px;
}

.tl-end p {
  margin: 0;
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-400);
}

@media (max-width: 639px) {
  .tl-events {
    padding-left: var(--mo-sp-5);
  }

  .tl-node {
    left: calc(-1 * var(--mo-sp-5) - 8px);
  }
}
</style>
