<template>
  <section class="pad surface">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">荣誉资质</div>
        <h2>{{ cfg.title || '客户信任的注脚' }}</h2>
      </div>
      <div v-if="categories.length > 1" class="honor-tabs rv" role="tablist">
        <button
          v-for="cat in categories"
          :key="cat"
          type="button"
          class="tag"
          :class="{ on: activeCat === cat }"
          role="tab"
          :aria-selected="activeCat === cat"
          @click="activeCat = cat"
        >
          {{ cat }}
        </button>
      </div>
      <div class="honor-grid rv d1">
        <div v-for="item in filteredItems" :key="item.name" class="honor-card">
          <div class="honor-seal" aria-hidden="true">
            <svg width="34" height="34" viewBox="0 0 34 34" fill="none">
              <circle cx="17" cy="13" r="8" stroke="currentColor" stroke-width="1.5" />
              <path
                d="M13 20l-2 10 6-3.5L23 30l-2-10"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <h3>{{ item.name }}</h3>
          <p class="honor-issuer">{{ item.issuer }}</p>
          <span class="honor-year">{{ item.year }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteHonors } from '@/utils/siteContent'

const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const cfg = computed(() => props.config ?? {})

const items = computed(() => {
  const fromConfig = props.config?.items
  return Array.isArray(fromConfig) && fromConfig.length > 0
    ? fromConfig
    : getSiteHonors(settings.value?.site_honors)
})

const categories = computed(() => {
  const cats = Array.from(new Set(items.value.map(item => item.cat).filter(Boolean)))
  return ['全部', ...cats]
})

const activeCat = ref<string>('全部')
const filteredItems = computed(() =>
  activeCat.value === '全部'
    ? items.value
    : items.value.filter(item => item.cat === activeCat.value)
)

// 设置异步加载后分类可能新增，当前选中项不存在时回退「全部」
watch(categories, cats => {
  if (!cats.includes(activeCat.value)) activeCat.value = '全部'
})
</script>
