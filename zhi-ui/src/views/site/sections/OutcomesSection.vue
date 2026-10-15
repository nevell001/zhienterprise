<template>
  <section class="pad dark">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">数据成果</div>
        <h2>{{ cfg.title || '可量化的改变' }}</h2>
        <p>{{ cfg.subtitle || '我们只把能被验证的数字写进这里。' }}</p>
      </div>
      <div class="numgrid rv d1">
        <div v-for="o in items" :key="o.label" class="numcell">
          <b>
            <i>{{ o.mark }}</i>
            {{ o.suffix }}
          </b>
          <span>{{ o.label }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteOutcomes } from '@/utils/siteContent'

const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const cfg = computed(() => props.config ?? {})

const items = computed(() => {
  const fromConfig = props.config?.items
  return Array.isArray(fromConfig) && fromConfig.length > 0
    ? fromConfig
    : getSiteOutcomes(settings.value?.site_outcomes)
})
</script>
