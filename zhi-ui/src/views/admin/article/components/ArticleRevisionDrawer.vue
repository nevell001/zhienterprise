<template>
  <el-drawer
    :model-value="modelValue"
    title="修订历史"
    size="min(560px, 94vw)"
    append-to-body
    class="rev-drawer"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="rev-history">
      <el-empty
        v-if="!loading && revisions.length === 0"
        description="暂无历史版本，保存文章后自动记录"
      />
      <ul v-else class="rev-list">
        <li v-for="item in revisions" :key="item.id" class="rev-item">
          <div class="rev-main">
            <div class="rev-title">v{{ item.revisionNo }} · {{ item.title }}</div>
            <div class="rev-meta">
              {{ item.editorName || '未知编辑' }}
              <span v-if="item.createTime">· {{ item.createTime }}</span>
            </div>
          </div>
          <div class="rev-actions">
            <el-button class="rev-view" link type="primary" @click="openDetail(item)">
              查看
            </el-button>
            <el-button
              v-hasPermi="['zhi:article:edit']"
              class="rev-restore"
              link
              type="warning"
              :loading="restoringId === item.id"
              @click="handleRestore(item)"
            >
              恢复
            </el-button>
          </div>
        </li>
      </ul>
    </div>
  </el-drawer>

  <el-dialog v-model="detailVisible" :title="detailTitle" width="min(820px, 94vw)" append-to-body>
    <div v-if="detail" class="rev-detail">
      <p class="rev-detail-meta">
        v{{ detail.revisionNo }} · {{ detail.editorName || '未知编辑' }}
        <span v-if="detail.createTime">· {{ detail.createTime }}</span>
      </p>
      <h3 class="rev-detail-title">{{ detail.title }}</h3>
      <p v-if="detail.summary" class="rev-detail-summary">{{ detail.summary }}</p>
      <div class="rev-detail-content" v-html="detailHtml"></div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  listArticleRevisions,
  getArticleRevision,
  restoreArticleRevision
} from '@/api/admin/article'
import type { ArticleRevision } from '@/types/api'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-service'

const props = defineProps<{
  modelValue: boolean
  articleId?: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'restored'): void
}>()

const revisions = ref<ArticleRevision[]>([])
const loading = ref(false)
const restoringId = ref<number | null>(null)
const detailVisible = ref(false)
const detail = ref<ArticleRevision | null>(null)

const detailTitle = computed(() =>
  detail.value ? `历史版本 v${detail.value.revisionNo}` : '历史版本'
)
const detailHtml = computed(() => sanitizeArticleContent(detail.value?.content || ''))

async function load() {
  if (!props.articleId) {
    revisions.value = []
    return
  }
  loading.value = true
  try {
    const response = await listArticleRevisions(Number(props.articleId))
    revisions.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function openDetail(row: ArticleRevision) {
  const response = await getArticleRevision(row.id)
  detail.value = response.data || null
  detailVisible.value = true
}

async function handleRestore(row: ArticleRevision) {
  try {
    await ElMessageBox.confirm(
      `恢复到版本 v${row.revisionNo}？当前内容会先保存为新版本，可再次恢复。`,
      '恢复历史版本',
      { type: 'warning', confirmButtonText: '恢复', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  restoringId.value = row.id
  try {
    await restoreArticleRevision(row.id)
    ElMessage.success('已恢复历史版本')
    emit('restored')
    await load()
  } finally {
    restoringId.value = null
  }
}

watch(
  () => props.modelValue,
  open => {
    if (open) {
      detailVisible.value = false
      detail.value = null
      load()
    }
  },
  { immediate: true }
)

watch(
  () => props.articleId,
  () => {
    if (props.modelValue) load()
  }
)
</script>

<style scoped>
.rev-history {
  min-height: 200px;
}

.rev-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.rev-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 4px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.rev-item:last-child {
  border-bottom: none;
}

.rev-main {
  min-width: 0;
}

.rev-title {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rev-meta {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.rev-actions {
  flex-shrink: 0;
}

.rev-detail-meta {
  margin: 0 0 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.rev-detail-title {
  margin: 0 0 8px;
  color: var(--el-text-color-primary);
  font-size: 18px;
}

.rev-detail-summary {
  margin: 0 0 16px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.rev-detail-content {
  overflow-wrap: anywhere;
  color: var(--el-text-color-primary);
  line-height: 1.75;
}

.rev-detail-content :deep(img) {
  max-width: 100%;
}
</style>
