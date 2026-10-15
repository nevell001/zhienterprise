<template>
  <section class="pad">
    <div class="eh-wrap">
      <div class="tpl-sec-head eh-head-row rv">
        <div>
          <div class="sec-tag">新闻动态</div>
          <h2>{{ cfg.title || '最近在做什么' }}</h2>
        </div>
        <router-link class="lnk" to="/news">
          更多动态
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
      </div>
      <div v-if="newsItems.length" class="news">
        <article
          v-for="(item, ni) in newsItems"
          :key="item.id"
          class="n-card rv"
          :class="`d${ni % 3}`"
          @click="goNewsItem(item.id)"
        >
          <div class="n-thumb">
            <img
              v-if="item.coverUrl"
              :src="item.coverUrl"
              :alt="item.title"
              loading="lazy"
              decoding="async"
            />
            <svg
              v-else
              width="56"
              height="56"
              viewBox="0 0 56 56"
              fill="none"
              opacity=".5"
              aria-hidden="true"
            >
              <rect
                x="14"
                y="14"
                width="28"
                height="28"
                rx="8"
                stroke="currentColor"
                stroke-width="1.5"
              />
              <path d="M20 28h16" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
          </div>
          <div class="n-body">
            <h3>{{ item.title }}</h3>
            <div class="n-meta">
              <span class="tag">企业动态</span>
              <span>{{ formatDate(item.createTime) }}</span>
            </div>
          </div>
        </article>
      </div>
      <p v-else class="eh-empty rv">暂无动态，发布资讯后将在这里展示</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '@/api/site'
import type { Article } from '@/types'
import { parseTime } from '@/utils/zhi'
import { logger } from '@/utils/logger'

/** config.count 控制展示条数（默认 3） */
const props = defineProps<{ config?: Record<string, any> }>()

const router = useRouter()
const newsItems = ref<Article[]>([])
const count = computed(() => Number(props.config?.count) || 3)
const cfg = computed(() => props.config ?? {})

onMounted(async () => {
  try {
    const res = await getArticleList({ pageNum: 1, pageSize: count.value, articleType: 'news' })
    if (res?.code === 200) newsItems.value = (res.rows || []) as Article[]
  } catch (error) {
    logger.error('加载新闻动态失败:', error)
  }
})

const formatDate = (date?: string) => (date ? parseTime(date, '{y}-{m}-{d}') : '')
const goNewsItem = (id?: number) => id && router.push(`/article/${id}`)
</script>
