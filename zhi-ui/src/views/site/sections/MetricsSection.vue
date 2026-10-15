<template>
  <section class="pad-s">
    <div class="eh-wrap">
      <div class="metrics rv">
        <div v-for="m in items" :key="m.label" class="metric">
          <b>{{ m.value }}</b>
          <span>{{ m.label }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteMetrics } from '@/utils/siteContent'

/** config.items 优先（模块级覆盖），否则读「官网内容」设置，最后回退内置占位 */
const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)

const items = computed(() => {
  const fromConfig = props.config?.items
  return Array.isArray(fromConfig) && fromConfig.length > 0
    ? fromConfig
    : getSiteMetrics(settings.value?.site_metrics)
})
</script>
