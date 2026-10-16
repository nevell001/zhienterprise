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
          role="link"
          tabindex="0"
          @click="goItem(item)"
          @keydown.enter.prevent="goItem(item)"
        >
          <div class="num">{{ String(si + 1).padStart(2, '0') }} / 行业</div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <router-link
            class="lnk"
            :to="item.id ? `/solutions/${item.id}` : '/solutions'"
            @click.stop
          >
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteTemplate } from '@/config/siteTemplates'
import { getHomeSolutions } from '@/utils/siteContent'
import { getArticleList } from '@/api/site'
import type { Article } from '@/types'
import { logger } from '@/utils/logger'

const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const router = useRouter()
const settings = computed(() => siteSettingsStore.siteSettings)
const tpl = computed(() => getSiteTemplate(settings.value?.site_template))
const cfg = computed(() => props.config ?? {})

/** 引用方案文章优先（solution 类型），无数据回退「官网内容」手动填写 */
const solutionArticles = ref<Article[]>([])

/** items 统一携带可选 id：来源为方案文章时点击进详情，手填/回退数据点击回列表 */
const items = computed<{ id?: number; title: string; desc: string }[]>(() => {
  const fromArticles = solutionArticles.value
    .filter(a => a.title)
    .map(a => ({ id: a.id, title: a.title || '', desc: a.summary || '' }))
  if (fromArticles.length > 0) return fromArticles

  const fromConfig = props.config?.items
  if (Array.isArray(fromConfig) && fromConfig.length > 0) {
    return fromConfig.map(item => ({
      id: typeof item?.id === 'number' ? item.id : undefined,
      title: String(item?.title || ''),
      desc: String(item?.desc || '')
    }))
  }
  return getHomeSolutions(settings.value?.home_solutions).map(item => ({
    id: undefined,
    title: String(item.title || ''),
    desc: String(item.desc || '')
  }))
})

const goItem = (item: { id?: number }) =>
  router.push(item.id ? `/solutions/${item.id}` : '/solutions')

onMounted(async () => {
  try {
    const res = await getArticleList({ pageNum: 1, pageSize: 4, articleType: 'solution' })
    if (res?.code === 200) solutionArticles.value = (res.rows || []) as Article[]
  } catch (error) {
    logger.error('加载解决方案文章失败:', error)
  }
})
</script>
