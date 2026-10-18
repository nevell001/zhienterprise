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
        <el-upload
          class="media-upload"
          action="#"
          :show-file-list="false"
          :http-request="handleUpload"
          :before-upload="beforeUpload"
          :disabled="uploading"
          accept=".bmp,.gif,.jpg,.jpeg,.png,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.rar,.zip,.gz,.bz2,.mp4,.avi,.rmvb,.pdf"
        >
          <el-button type="success" icon="Upload" :loading="uploading">上传文件</el-button>
        </el-upload>
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
            v-hasPermi="['zhi:media:remove']"
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

<script setup lang="ts" name="SiteMedia">
import { h, onMounted, reactive, ref } from 'vue'
import type { UploadRequestOptions } from 'element-plus'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-service'
import {
  listMedia,
  delMedia,
  getMediaReferences,
  uploadMedia,
  type MediaItem,
  type MediaReference
} from '@/api/admin/media'

const loading = ref(false)
const uploading = ref(false)
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

/** 与后端 MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION 保持一致 */
const allowedExtensions = [
  'bmp',
  'gif',
  'jpg',
  'jpeg',
  'png',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'ppt',
  'pptx',
  'txt',
  'rar',
  'zip',
  'gz',
  'bz2',
  'mp4',
  'avi',
  'rmvb',
  'pdf'
]
const maxFileSize = 5 * 1024 * 1024

const beforeUpload = (file: File) => {
  const dot = file.name.lastIndexOf('.')
  const extension = dot > -1 ? file.name.slice(dot + 1).toLowerCase() : ''
  if (!allowedExtensions.includes(extension)) {
    ElMessage.error('文件格式不支持，请上传图片、Office 文档、压缩包、视频或 PDF')
    return false
  }
  if (file.size > maxFileSize) {
    ElMessage.error('单个文件大小不能超过 5 MB')
    return false
  }
  return true
}

const handleUpload = async (options: UploadRequestOptions) => {
  uploading.value = true
  try {
    await uploadMedia(options.file)
    ElMessage.success('上传成功')
    queryParams.pageNum = 1
    await getList()
  } catch {
    // 错误提示由 request 拦截器统一处理
  } finally {
    uploading.value = false
  }
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

const referenceTypeText = (type: string) => {
  const labels: Record<string, string> = {
    article: '内容',
    page: '自定义页面',
    setting: '站点设置',
    friend_link: '友链',
    user: '用户头像'
  }
  return labels[type] || type
}

const buildReferenceMessage = (references: MediaReference[]) => {
  const shown = references.slice(0, 10)
  const rest =
    references.length > shown.length
      ? [
          h(
            'p',
            { style: 'margin-top: 8px' },
            `仅显示前 ${shown.length} 条，共 ${references.length} 条`
          )
        ]
      : []
  return h('div', [
    h('p', `该文件正在被以下 ${references.length} 处内容引用，删除后会导致图片或附件失效：`),
    h(
      'ul',
      { style: 'margin: 8px 0 0; padding-left: 20px; max-height: 240px; overflow: auto' },
      shown.map(reference =>
        h('li', `${referenceTypeText(reference.referenceType)}：${reference.referenceTitle}`)
      )
    ),
    ...rest
  ])
}

const handleDelete = async (row: MediaItem) => {
  try {
    const response = await getMediaReferences(row.id)
    const references = response.data || []
    if (references.length) {
      await ElMessageBox.alert(buildReferenceMessage(references), '无法删除', {
        type: 'warning',
        confirmButtonText: '知道了'
      })
      return
    }
    await ElMessageBox.confirm('删除后将同时删除物理文件且不可恢复，是否确认？', '删除确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await delMedia(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch {
    // 用户取消或接口错误（错误提示由 request 拦截器处理）
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
.media-upload {
  display: inline-block;
  margin-left: 12px;
}
</style>
