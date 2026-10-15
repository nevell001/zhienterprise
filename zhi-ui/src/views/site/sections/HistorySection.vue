<template>
  <section class="pad">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">发展历程</div>
        <h2>{{ cfg.title || '每一步都算数' }}</h2>
      </div>
      <div class="tl">
        <div v-for="stage in stages" :key="stage.name" class="tl-stage">
          <div class="tl-stage-head rv">
            <span class="tl-range">{{ stage.range }}</span>
            <h2 class="tl-stage-name">{{ stage.name }}</h2>
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
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
</script>
