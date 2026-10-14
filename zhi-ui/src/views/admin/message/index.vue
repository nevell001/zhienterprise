<template>
  <div class="app-container">
    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="68px"
      class="search-form"
    >
      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="queryParams.nickname"
          placeholder="请输入留言昵称"
          clearable
          :maxlength="50"
          class="query-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="留言内容" prop="content">
        <el-input
          v-model="queryParams.content"
          placeholder="请输入留言内容"
          clearable
          :maxlength="200"
          class="query-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="留言状态"
          clearable
          class="status-select"
        >
          <el-option label="待审核" value="0" />
          <el-option label="已发布" value="1" />
          <el-option label="已拒绝" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="">
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['blog:message:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete()"
        >
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['blog:message:export']"
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
        >
          导出
        </el-button>
      </el-col>
      <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
    </el-row>

    <el-table
      v-loading="loading"
      :data="messageList"
      :height="tableHeight"
      stripe
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="留言ID" align="center" prop="id" width="80" />
      <el-table-column
        label="昵称"
        align="center"
        prop="nickname"
        width="120"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <span>{{ scope.row.nickname || '匿名' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="留言内容"
        align="center"
        prop="content"
        :show-overflow-tooltip="true"
        min-width="220"
      />
      <el-table-column
        label="联系方式"
        align="center"
        prop="website"
        :show-overflow-tooltip="true"
        min-width="160"
      >
        <template #default="scope">
          <el-link
            v-if="scope.row.website"
            type="primary"
            :href="scope.row.website"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ scope.row.website }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
        label="IP"
        align="center"
        prop="ip"
        width="140"
        :show-overflow-tooltip="true"
      />
      <el-table-column label="留言时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="回复内容"
        align="center"
        prop="replyContent"
        :show-overflow-tooltip="true"
        min-width="160"
      >
        <template #default="scope">
          <span>{{ scope.row.replyContent || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        width="180"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-tooltip v-if="String(scope.row.status) !== '1'" content="审核通过" placement="top">
            <el-button
              v-hasPermi="['blog:message:edit']"
              link
              type="success"
              icon="Check"
              size="small"
              @click="handleAudit(scope.row, '1')"
            />
          </el-tooltip>
          <el-tooltip v-if="String(scope.row.status) !== '2'" content="审核拒绝" placement="top">
            <el-button
              v-hasPermi="['blog:message:edit']"
              link
              type="warning"
              icon="Close"
              size="small"
              @click="handleAudit(scope.row, '2')"
            />
          </el-tooltip>
          <el-tooltip content="回复" placement="top">
            <el-button
              v-hasPermi="['blog:message:reply']"
              link
              type="primary"
              icon="ChatDotRound"
              size="small"
              @click="handleReply(scope.row)"
            />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button
              v-hasPermi="['blog:message:remove']"
              link
              type="danger"
              icon="Delete"
              size="small"
              @click="handleDelete(scope.row)"
            />
          </el-tooltip>
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

    <!-- 回复留言对话框 -->
    <el-dialog v-model="open" :title="title" :width="dialogWidth" append-to-body>
      <el-form ref="replyRef" :model="replyForm" :rules="replyRules" label-width="80px">
        <el-form-item label="留言人">
          <span>{{ replyForm.nickname || '匿名' }}</span>
        </el-form-item>
        <el-form-item label="留言内容">
          <div class="reply-origin">{{ replyForm.content || '-' }}</div>
        </el-form-item>
        <el-form-item label="回复内容" prop="replyContent">
          <el-input
            v-model="replyForm.replyContent"
            type="textarea"
            :rows="5"
            :maxlength="500"
            show-word-limit
            placeholder="请输入回复内容（不超过500字）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitReply">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BlogMessage">
import { listMessage, auditMessage, replyMessage, delMessage } from '@/api/admin/blog/message'
import type {
  BlogMessage,
  MessageAuditStatus,
  MessageQueryParams,
  MessageStatus
} from '@/api/admin/blog/message'
import { parseTime } from '@/utils/zhi'

const { proxy } = getCurrentInstance()

const messageList = ref<BlogMessage[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const screenWidth = ref(window.innerWidth)

/** 回复表单 */
interface ReplyForm {
  id?: number
  nickname?: string
  content?: string
  replyContent: string
}

const replyForm = ref<ReplyForm>({
  id: undefined,
  nickname: '',
  content: '',
  replyContent: ''
})

const queryParams = ref<MessageQueryParams>({
  pageNum: 1,
  pageSize: 10,
  nickname: undefined,
  content: undefined,
  status: undefined
})

const replyRules = {
  replyContent: [
    { required: true, message: '回复内容不能为空', trigger: 'blur' },
    { max: 500, message: '回复内容不能超过500个字符', trigger: 'blur' }
  ]
}

// 根据屏幕宽度计算表格高度
const tableHeight = computed(() => {
  return screenWidth.value < 768 ? '300px' : 'calc(100vh - 280px)'
})

// 根据屏幕宽度计算对话框宽度
const dialogWidth = computed(() => {
  return screenWidth.value < 768 ? '90%' : '600px'
})

/** 留言状态文本 */
function statusText(status?: MessageStatus): string {
  if (status === '1') return '已发布'
  if (status === '2') return '已拒绝'
  return '待审核'
}

/** 留言状态标签类型 */
function statusTagType(status?: MessageStatus): 'success' | 'danger' | 'warning' {
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  return 'warning'
}

/** 提取后端返回的错误消息 */
function resolveErrorMessage(error: unknown, fallback: string): string {
  const err = error as { msg?: string; message?: string } | undefined
  return err?.msg || err?.message || fallback
}

/** 查询留言列表 */
function getList() {
  loading.value = true
  listMessage(queryParams.value)
    .then(response => {
      messageList.value = response.rows || []
      total.value = response.total || 0
    })
    .finally(() => {
      loading.value = false
    })
    .catch((error: unknown) => {
      loading.value = false
      proxy.$modal.msgError(resolveErrorMessage(error, '获取留言列表失败'))
    })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  if (proxy && proxy.resetForm) {
    proxy.resetForm('queryRef')
  }
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    nickname: undefined,
    content: undefined,
    status: undefined
  }
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: BlogMessage[]) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

/** 审核留言（status：1通过 2拒绝） */
function handleAudit(row: BlogMessage, status: MessageAuditStatus) {
  const actionText = status === '1' ? '通过' : '拒绝'
  proxy.$modal
    .confirm(`是否确认${actionText}留言编号为"${row.id}"的数据项？`)
    .then(() => auditMessage(row.id, status))
    .then(() => {
      proxy.$modal.msgSuccess(`${actionText}成功`)
      getList()
    })
    .catch((error: unknown) => {
      if (error !== 'cancel') {
        proxy.$modal.msgError(resolveErrorMessage(error, `${actionText}失败`))
      }
    })
}

/** 打开回复弹窗 */
function handleReply(row: BlogMessage) {
  replyForm.value = {
    id: row.id,
    nickname: row.nickname,
    content: row.content,
    replyContent: row.replyContent || ''
  }
  open.value = true
  title.value = '回复留言'
}

/** 取消按钮 */
function cancel() {
  open.value = false
  replyForm.value = {
    id: undefined,
    nickname: '',
    content: '',
    replyContent: ''
  }
}

/** 提交回复 */
function submitReply() {
  if (!proxy || !proxy.$refs || !proxy.$refs['replyRef']) {
    proxy.$modal.msgError('表单引用失败')
    return
  }

  proxy.$refs['replyRef'].validate((valid: boolean) => {
    if (!valid) return

    replyMessage(replyForm.value.id, replyForm.value.replyContent)
      .then(() => {
        proxy.$modal.msgSuccess('回复成功')
        open.value = false
        getList()
      })
      .catch((error: unknown) => {
        proxy.$modal.msgError(resolveErrorMessage(error, '回复失败'))
      })
  })
}

/** 删除按钮操作 */
function handleDelete(row?: BlogMessage) {
  const deleteIds = row?.id || ids.value
  proxy.$modal
    .confirm('是否确认删除留言编号为"' + deleteIds + '"的数据项？')
    .then(() => delMessage(deleteIds))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    })
    .catch((error: unknown) => {
      if (error !== 'cancel') {
        proxy.$modal.msgError(resolveErrorMessage(error, '删除失败'))
      }
    })
}

/** 导出按钮操作 */
function handleExport() {
  if (proxy && proxy.download) {
    proxy.download(
      'system/message/export',
      {
        ...queryParams.value
      },
      `message_${new Date().getTime()}.xlsx`
    )
  }
}

/** 监听窗口大小变化 */
function handleResize() {
  screenWidth.value = window.innerWidth
}

onMounted(() => {
  getList()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 搜索表单样式优化 */
.search-form {
  margin-bottom: 12px;
  padding: 16px 16px 4px 16px;
  background-color: var(--el-bg-color-page, #f5f7fa);
  border-radius: 4px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

/* 查询输入框与状态下拉样式 */
.query-input {
  width: 200px;
}

.status-select {
  width: 140px;
}

/* 回复弹窗中的原留言内容 */
.reply-origin {
  width: 100%;
  max-height: 120px;
  padding: 8px 12px;
  overflow-y: auto;
  color: var(--el-text-color-regular, #606266);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
  background-color: var(--el-bg-color-page, #f5f7fa);
  border-radius: 4px;
}

/* 按钮区域样式 */
.mb8 {
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .query-input,
  .status-select {
    width: 100%;
  }
}
</style>
