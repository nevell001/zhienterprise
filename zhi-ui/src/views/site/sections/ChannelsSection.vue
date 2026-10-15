<template>
  <section class="pad">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">分渠道咨询</div>
        <h2>{{ cfg.title || '按需求选择对接渠道' }}</h2>
        <p>{{ cfg.subtitle || '以下渠道均可通过页脚电话与邮箱联系，备注对应渠道即可精准触达。' }}</p>
      </div>
      <div class="ct-grid ct-four rv d1">
        <div v-for="ch in items" :key="ch.name" class="ct-card">
          <div class="ct-type">{{ ch.name }}</div>
          <p class="ct-desc">{{ ch.desc }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteChannels } from '@/utils/siteContent'

const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const cfg = computed(() => props.config ?? {})

const items = computed(() => {
  const fromConfig = props.config?.items
  return Array.isArray(fromConfig) && fromConfig.length > 0
    ? fromConfig
    : getSiteChannels(settings.value?.site_channels)
})
</script>
