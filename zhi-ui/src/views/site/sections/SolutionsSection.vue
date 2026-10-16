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
          :style="solutionArticles.length > 0 ? 'cursor: pointer' : ''"
          @click="solutionArticles.length > 0 && goItemByTitle(item.title)"
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

const items = computed(() => {
  const fromArticles = solutionArticles.value
    .filter(a => a.title)
    .map(a => ({ title: a.title || '', desc: a.summary || '' }))
  if (fromArticles.length > 0) return fromArticles

  const fromConfig = props.config?.items
  if (Array.isArray(fromConfig) && fromConfig.length > 0) return fromConfig
  return getHomeSolutions(settings.value?.home_solutions)
})

const goItem = (id?: number) => id && router.push(`/solutions/${id}`)

/** 手填项（无 id）点击回方案列表 */
const goItemByTitle = (_title?: string) => router.push('/solutions')

onMounted(async () => {
  try {
    const res = await getArticleList({ pageNum: 1, pageSize: 4, articleType: 'solution' })
    if (res?.code === 200) solutionArticles.value = (res.rows || []) as Article[]
  } catch (error) {
    logger.error('加载解决方案文章失败:', error)
  }
})
</script>
