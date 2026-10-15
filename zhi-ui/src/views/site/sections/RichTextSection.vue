<template>
  <section class="pad">
    <div class="eh-wrap rich-wrap">
      <div v-if="cfg.title || cfg.subtitle" class="tpl-sec-head rv">
        <div class="sec-tag">{{ cfg.tag || '关于我们' }}</div>
        <h2>{{ cfg.title }}</h2>
        <p v-if="cfg.subtitle">{{ cfg.subtitle }}</p>
      </div>
      <!-- 内容出自后台页面/模块编辑器，渲染前经统一消毒管线 -->
      <!-- eslint-disable-next-line vue/no-v-html -->
      <div class="rich-content rv" v-html="sanitized"></div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { sanitizeArticleContent } from '@/utils/sanitize'

const props = defineProps<{ config?: Record<string, any> }>()

const cfg = computed(() => props.config ?? {})
const sanitized = computed(() => sanitizeArticleContent(cfg.value.content || ''))
</script>
