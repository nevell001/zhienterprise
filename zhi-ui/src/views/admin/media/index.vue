<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams">
      <el-form-item label="类型">
        <el-select
          v-model="queryParams.uploadType"
          placeholder="全部类型"
          clearable
          style="width: 160px"
          @change="handleQuery"
        >
          <el-option
            v-for="type in uploadTypes"
            :key="type.value"
            :label="type.label"
            :value="type.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="文件名">
        <el-input
          v-model="queryParams.fileName"
          placeholder="按文件名搜索"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="文件" align="left" min-width="180">
        <template #default="scope">
          <div class="media-name">{{ scope.row.originalName || scope.row.fileName }}</div>
        </template>
      </el-table-column>
      <el-table-column label="URL" align="left" min-width="220" show-overflow-tooltip>
        <template #default="scope">
          <a
            v-if="scope.row.url"
            :href="scope.row.url"
            target="_blank"
            rel="noopener noreferrer"
            class="media-url"
          >
            {{ scope.row.url }}
          </a>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" width="120">
        <template #default="scope">
          <el-tag size="small">{{ typeText(scope.row.uploadType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="大小" align="center" width="100">
        <template #default="scope">{{ formatSize(scope.row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="上传者" align="center" prop="createBy" width="100" />
      <el-table-column label="上传时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.createTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button
            v-hasPermi="['blog:media:remove']"
            link
            type="danger"
            icon="Delete"
            size="small"
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      layout="prev, pager, next, jumper, total"
      @pagination="getList"
    />
  </div>
</template>

<script setup lang="ts" name="BlogMedia">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-service'
import { listMedia, delMedia, type MediaItem } from '@/api/admin/blog/media'

const loading = ref(false)
const list = ref<MediaItem[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  uploadType: undefined as string | undefined,
  fileName: undefined as string | undefined
})

const uploadTypes = [
  { label: '普通上传', value: 'upload' },
  { label: '智能压缩', value: 'compressed' },
  { label: '头像', value: 'avatar' },
  { label: '缩略图', value: 'thumbnail' },
  { label: '文章封面', value: 'article-cover' },
  { label: '移动端', value: 'mobile' },
  { label: '水印', value: 'watermark' },
  { label: '图片处理', value: 'image-tool' }
]

const typeText = (type?: string) => {
  if (!type) return 'upload'
  const found = uploadTypes.find(t => t.value === type)
  return found ? found.label : type
}

const formatSize = (size?: number) => {
  const bytes = Number(size) || 0
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

const getList = async () => {
  loading.value = true
  try {
    const response = await listMedia({ ...queryParams })
    list.value = response.rows || []
    total.value = response.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryParams.pageNum = 1
  queryParams.uploadType = undefined
  queryParams.fileName = undefined
  getList()
}

const handleDelete = async (row: MediaItem) => {
  try {
    await ElMessageBox.confirm('删除后将同时尝试删除物理文件，是否确认？', '删除确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await delMedia(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch {
    // 用户取消
  }
}

onMounted(getList)
</script>

<style scoped>
.media-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.media-url {
  color: var(--el-color-primary, #409eff);
  text-decoration: none;
}
.media-url:hover {
  text-decoration: underline;
}
</style>
