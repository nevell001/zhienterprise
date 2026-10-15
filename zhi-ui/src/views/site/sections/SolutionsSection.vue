<template>
  <section class="pad">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">解决方案</div>
        <h2>{{ cfg.title || tpl.solutionsTitle }}</h2>
        <p>
          {{
            cfg.subtitle ||
            '每个方案都基于真实业务场景，包含客户挑战、方案架构、实施路径与可量化成果。'
          }}
        </p>
      </div>
      <div class="sol">
        <div
          v-for="(item, si) in items"
          :key="item.title"
          class="sol-card rv"
          :class="`d${si % 4}`"
        >
          <div class="num">{{ String(si + 1).padStart(2, '0') }} / 行业</div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <router-link class="lnk" to="/business">
            查看方案
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
          <!-- eslint-disable-next-line vue/no-v-html -->
          <svg
            class="sol-art"
            viewBox="0 0 200 200"
            fill="none"
            aria-hidden="true"
            v-html="tpl.art"
          />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteTemplate } from '@/config/siteTemplates'
import { getHomeSolutions } from '@/utils/siteContent'

const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const tpl = computed(() => getSiteTemplate(settings.value?.site_template))
const cfg = computed(() => props.config ?? {})

const items = computed(() => {
  const fromConfig = props.config?.items
  return Array.isArray(fromConfig) && fromConfig.length > 0
    ? fromConfig
    : getHomeSolutions(settings.value?.home_solutions)
})
</script>
