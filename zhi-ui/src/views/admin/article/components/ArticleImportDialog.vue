<template>
  <el-dialog
    :model-value="modelValue"
    title="批量导入 Markdown"
    width="560px"
    append-to-body
    @close="handleClose"
  >
    <p class="import-tip">
      每个
      <code>.md</code>
      文件生成一篇文章：首个「# 一级标题」作为标题（没有则用文件名），其余内容作为正文，以 Markdown
      源码入库并由服务端渲染。
    </p>

    <el-upload
      v-model:file-list="fileList"
      class="import-upload"
      drag
      multiple
      accept=".md,.markdown"
      :auto-upload="false"
      :limit="maxFiles"
      :on-exceed="handleExceed"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        拖拽 .md 文件到此处，或
        <em>点击选择</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          仅支持 .md / .markdown，单个不超过 5MB，最多 {{ maxFiles }} 个
        </div>
      </template>
    </el-upload>

    <el-form label-width="80px" class="import-form">
      <el-form-item v-if="!articleType" label="内容类型">
        <el-select v-model="importType" class="import-select">
          <el-option
            v-for="item in typeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="categoryId" clearable placeholder="不指定" class="import-select">
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="导入为">
        <el-radio-group v-model="status">
          <el-radio :label="0">草稿</el-radio>
          <el-radio :label="1">直接发布</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <div v-if="results.length" class="import-result">
      <div class="import-summary">
        共 {{ results.length }} 个文件，成功 {{ successCount }} 个，失败 {{ failedCount }} 个
      </div>
      <ul v-if="failedCount" class="import-failed">
        <li v-for="(item, index) in failedItems" :key="index">
          {{ item.fileName }}：{{ item.message }}
        </li>
      </ul>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!fileList.length" @click="submit">
        开始导入
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { listCategory } from '@/api/admin/category'
import { importMarkdownArticles } from '@/api/admin/article'
import { ElMessage } from '@/plugins/element-plus-service'
import type { ArticleImportItem } from '@/types'
import type { UploadUserFile } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  /** 固定栏目的内容类型；为空时由用户在下拉里选择 */
  articleType?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'imported'): void
}>()

/** 与后端 MAX_IMPORT_FILES 保持一致 */
const maxFiles = 20
const maxFileSize = 5 * 1024 * 1024

const typeOptions = [
  { label: '资讯动态', value: 'news' },
  { label: '核心业务', value: 'business' },
  { label: '客户案例', value: 'case' },
  { label: '解决方案', value: 'solution' }
]

const fileList = ref<UploadUserFile[]>([])
const categoryOptions = ref<{ id: number; name: string }[]>([])
const categoryId = ref<number | undefined>(undefined)
const importType = ref(props.articleType || 'news')
const status = ref(0)
const submitting = ref(false)
const results = ref<ArticleImportItem[]>([])

const successCount = computed(() => results.value.filter(item => item.success).length)
const failedCount = computed(() => results.value.length - successCount.value)
const failedItems = computed(() => results.value.filter(item => !item.success))

watch(
  () => props.articleType,
  value => {
    if (value) importType.value = value
  }
)

watch(
  () => props.modelValue,
  async open => {
    if (open) {
      // 每次打开都从空列表开始，避免上次导入的文件被再次提交
      fileList.value = []
      results.value = []
      await loadCategories()
    }
  },
  { immediate: true }
)

async function loadCategories() {
  try {
    const response = await listCategory()
    categoryOptions.value = (response.rows || []) as { id: number; name: string }[]
  } catch (error) {
    console.error('获取分类列表失败:', error)
    categoryOptions.value = []
  }
}

function handleExceed() {
  ElMessage.warning(`单次最多导入 ${maxFiles} 个文件`)
}

async function submit() {
  if (!fileList.value.length) {
    ElMessage.warning('请先选择 Markdown 文件')
    return
  }
  const oversized = fileList.value.find(file => (file.size || 0) > maxFileSize)
  if (oversized) {
    ElMessage.warning(`${oversized.name} 超过 5MB，请移除后重试`)
    return
  }
  const formData = new FormData()
  fileList.value.forEach(file => {
    if (file.raw) formData.append('files', file.raw)
  })
  if (categoryId.value !== undefined && categoryId.value !== null) {
    formData.append('categoryId', String(categoryId.value))
  }
  formData.append('articleType', importType.value)
  formData.append('status', String(status.value))

  submitting.value = true
  try {
    const res = await importMarkdownArticles(formData)
    const data = res.data
    results.value = data?.results || []
    const imported = data?.imported ?? 0
    const failed = data?.failed ?? 0
    emit('imported')
    if (failed === 0) {
      ElMessage.success(`导入完成：成功 ${imported} 篇`)
      close()
    } else {
      ElMessage.warning(`导入完成：成功 ${imported} 篇，失败 ${failed} 篇`)
    }
  } catch (error: any) {
    ElMessage.error('导入失败：' + (error?.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}

function handleClose() {
  if (submitting.value) return
  close()
}
</script>

<style scoped>
.import-tip {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.import-form {
  margin-top: 16px;
}

.import-select {
  width: 240px;
}

.import-result {
  margin-top: 8px;
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.import-failed {
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--el-color-danger);
  line-height: 1.6;
}
</style>
