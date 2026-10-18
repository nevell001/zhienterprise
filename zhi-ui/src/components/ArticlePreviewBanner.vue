<template>
  <div class="preview-banner" role="status">
    <span class="preview-badge">预览模式</span>
    <span class="preview-text">{{ statusText }} · 仅登录管理员可见，未发布内容不会对外展示</span>
    <span v-if="statusText === '定时发布' && formattedPublishTime" class="preview-time">
      计划发布：{{ formattedPublishTime }}
    </span>
    <router-link class="preview-back" :to="adminPath">返回编辑</router-link>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { parseTime } from '@/utils/zhi'

const props = defineProps<{
  status?: number | string | null
  publishTime?: string | null
  adminPath: string
}>()

const statusText = computed(() => (Number(props.status) === 2 ? '定时发布' : '草稿'))
const formattedPublishTime = computed(() =>
  props.publishTime ? parseTime(props.publishTime, '{y}-{m}-{d} {h}:{i}') : ''
)
</script>

<style scoped>
.preview-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  max-width: 860px;
  margin: 16px auto 0;
  padding: 10px 16px;
  border: 1px solid var(--tpl-border, #e5e7eb);
  border-radius: var(--tpl-r-md, 8px);
  background: var(--tpl-surface, #f8fafc);
  color: var(--tpl-ink-700, #374151);
  font-size: 13px;
  line-height: 1.5;
}

.preview-badge {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--tpl-p, #2563eb);
  color: var(--tpl-white, #fff);
  font-weight: 600;
  white-space: nowrap;
}

.preview-text {
  flex: 1;
  min-width: 200px;
}

.preview-time {
  color: var(--tpl-ink-500, #6b7280);
}

.preview-back {
  color: var(--tpl-p-text, #1d4ed8);
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
}

.preview-back:hover {
  text-decoration: underline;
}

@media (max-width: 639px) {
  .preview-banner {
    margin: 12px 16px 0;
  }
}
</style>
