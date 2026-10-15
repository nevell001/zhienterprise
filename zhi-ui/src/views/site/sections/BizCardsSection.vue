<template>
  <section class="pad surface">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">{{ businessLabel }}</div>
        <h2>{{ cfg.title || '两条业务线，覆盖你的完整链路' }}</h2>
        <p>{{ cfg.subtitle || '产品解决「用什么」，服务解决「怎么落地」。两者共用一套方法论，互为支撑。' }}</p>
      </div>
      <div class="biz">
        <div
          v-for="(card, ci) in bizCards"
          :key="card.title"
          class="biz-card rv"
          :class="`d${ci}`"
        >
          <div class="ico" aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
              <path :d="card.iconPath" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
              <path d="M11 2l8 4.5v9L11 20l-8-4.5v-9L11 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
            </svg>
          </div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
          <ul class="biz-list">
            <li
              v-for="item in card.items"
              :key="item.label"
              :class="{ clickable: item.id }"
              @click="item.id && goBusinessItem(item.id)"
            >
              {{ item.label }}
              <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true"><path d="M3 7.5h9M8 4l3.5 3.5L8 11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" /></svg>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '@/api/site'
import type { Article } from '@/types'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteTemplate } from '@/config/siteTemplates'
import { logger } from '@/utils/logger'

/** config.cardOne/cardTwo 可覆盖行业模板的两卡文案 */
const props = defineProps<{ config?: Record<string, any> }>()

const router = useRouter()
const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const businessLabel = computed(
  () => String(settings.value.business_label || '').trim() || '核心业务'
)
const tpl = computed(() => getSiteTemplate(settings.value?.site_template))
const cfg = computed(() => props.config ?? {})

const businessItems = ref<Article[]>([])

const bizCards = computed(() => {
  const one = { ...tpl.value.biz.one, ...(props.config?.cardOne || {}) }
  const two = { ...tpl.value.biz.two, ...(props.config?.cardTwo || {}) }
  const half = Math.ceil(businessItems.value.length / 2) || 3
  const toItems = (list: Article[], fallback: string[]) => {
    const items = list.map(a => ({ id: a.id, label: a.title || '' })).filter(i => i.label)
    return items.length > 0 ? items : fallback.map(label => ({ id: undefined, label }))
  }
  return [
    { ...one, iconPath: 'M11 6v10M6 11h10', items: toItems(businessItems.value.slice(0, half), tpl.value.biz.fallbackItems) },
    { ...two, iconPath: 'M11 6.5v9M6.5 11h9', items: toItems(businessItems.value.slice(half, half + 3), tpl.value.biz.fallbackItems) }
  ]
})

onMounted(async () => {
  try {
    const res = await getArticleList({ pageNum: 1, pageSize: 6, articleType: 'business' })
    if (res?.code === 200) businessItems.value = (res.rows || []) as Article[]
  } catch (error) {
    logger.error('加载核心业务失败:', error)
  }
})

const goBusinessItem = (id?: number) => id && router.push(`/business/${id}`)
</script>
