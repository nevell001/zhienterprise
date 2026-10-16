<template>
  <section class="pad">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">发展历程</div>
        <h2>{{ cfg.title || '每一步都算数' }}</h2>
      </div>
      <div class="tl">
        <div v-for="(stage, si) in stages" :key="stage.name" class="tl-stage">
          <div class="tl-stage-head rv">
            <span class="tl-range">{{ stage.range }}</span>
            <h2 class="tl-stage-name">{{ stage.name }}</h2>
            <p>{{ stage.theme }}</p>
            <!-- 展开全部 / 收起（方案文档 §3.5：默认显示每阶段前 2 条） -->
            <button
              v-if="stage.events.length > 2"
              type="button"
              class="tl-expand"
              @click="toggleStage(si)"
            >
              {{ expanded.has(si) ? '收起' : `展开全部 ${stage.events.length} 条` }}
            </button>
          </div>
          <div class="tl-events">
            <div
              v-for="(event, ei) in visibleEvents(stage, si)"
              :id="`tl-${uid}-${si}-${ei}`"
              :key="event.title"
              class="tl-event rv"
              :class="`d${ei % 4}`"
            >
              <span
                class="tl-node"
                :class="{ square: event.honor }"
                :title="`${event.year} ${event.title}`"
                role="button"
                tabindex="0"
                @click="scrollToEvent(si, ei)"
                @keydown.enter="scrollToEvent(si, ei)"
              ></span>
              <div class="tl-body">
                <span class="tl-year">{{ event.year }}</span>
                <h3>
                  {{ event.title }}
                  <span v-if="event.honor" class="tag">荣誉</span>
                </h3>
                <p>{{ event.desc }}</p>
              </div>
            </div>
            <button
              v-if="stage.events.length > 2 && !expanded.has(si)"
              type="button"
              class="tl-expand tl-expand-inline"
              @click="toggleStage(si)"
            >
              展开其余 {{ stage.events.length - 2 }} 条
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteHistoryStages, type SiteHistoryStage } from '@/utils/siteContent'

const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const cfg = computed(() => props.config ?? {})

const stages = computed(() => {
  const fromConfig = props.config?.stages as SiteHistoryStage[] | undefined
  return Array.isArray(fromConfig) && fromConfig.length > 0
    ? fromConfig
    : getSiteHistoryStages(settings.value?.site_history)
})

const uid = Math.random().toString(36).slice(2, 8)

/* 展开收起（方案文档 §3.5）：默认每阶段只显示前 2 条 */
const expanded = ref<Set<number>>(new Set())

const visibleEvents = (stage: SiteHistoryStage, si: number) => {
  if (expanded.value.has(si) || stage.events.length <= 2) return stage.events
  return stage.events.slice(0, 2)
}

const toggleStage = (si: number) => {
  const next = new Set(expanded.value)
  if (next.has(si)) {
    next.delete(si)
  } else {
    next.add(si)
  }
  expanded.value = next
}

/** 点击节点：对应事件滚动至视口中部（方案文档 §3.5，800ms 平滑） */
const scrollToEvent = (si: number, ei: number) => {
  document
    .getElementById(`tl-${uid}-${si}-${ei}`)
    ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}
</script>
