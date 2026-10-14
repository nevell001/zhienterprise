<template>
  <div class="app-container">
    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="文章标题" prop="articleTitle">
        <el-input
          v-model="queryParams.articleTitle"
          placeholder="请输入文章标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评论内容" prop="content">
        <el-input
          v-model="queryParams.content"
          placeholder="请输入评论内容"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="评论状态"
          clearable
          class="status-select"
        >
          <el-option label="待审核" value="0" />
          <el-option label="已审核" value="1" />
          <el-option label="已删除" value="2" />
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
          v-hasPermi="['blog:comment:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
        >
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['blog:comment:audit']"
          type="success"
          plain
          icon="Check"
          :disabled="multiple"
          @click="handleAudit"
        >
          审核通过
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['blog:comment:reject']"
          type="warning"
          plain
          icon="Close"
          :disabled="multiple"
          @click="handleReject"
        >
          审核拒绝
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['blog:comment:export']"
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

    <el-table v-loading="loading" :data="commentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="评论ID" align="center" prop="id" />
      <el-table-column
        label="文章标题"
        align="center"
        prop="articleTitle"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="评论内容"
        align="center"
        prop="content"
        :show-overflow-tooltip="true"
      />
      <el-table-column label="评论人" align="center" prop="authorName" />
      <el-table-column label="邮箱" align="center" prop="email" />
      <el-table-column label="IP地址" align="center" prop="ipAddress" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag
            :options="comment_status"
            :value="scope.row.status"
            class="comment-status-tag"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        width="240"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-tooltip content="详情" placement="top">
            <el-button
              v-hasPermi="['blog:comment:query']"
              link
              type="primary"
              icon="View"
              @click="handleView(scope.row)"
            />
          </el-tooltip>
          <el-tooltip v-if="scope.row.status === '0'" content="审核通过" placement="top">
            <el-button
              v-hasPermi="['blog:comment:audit']"
              link
              type="success"
              icon="Check"
              @click="handleAudit(scope.row)"
            />
          </el-tooltip>
          <el-tooltip v-if="scope.row.status === '0'" content="审核拒绝" placement="top">
            <el-button
              v-hasPermi="['blog:comment:reject']"
              link
              type="warning"
              icon="Close"
              @click="handleReject(scope.row)"
            />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button
              v-hasPermi="['blog:comment:remove']"
              link
              type="danger"
              icon="Delete"
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
      @pagination="getList"
    />

    <!-- 评论详情（只读） -->
    <el-dialog v-model="detailOpen" title="评论详情" width="600px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="评论ID">{{ detailForm.id }}</el-descriptions-item>
        <el-descriptions-item label="文章标题">
          {{ detailForm.articleTitle || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="评论人">
          {{ detailForm.authorName || detailForm.nickname || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailForm.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">
          {{ detailForm.ipAddress || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="comment_status" :value="detailForm.status" />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ parseTime(detailForm.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="评论内容">
          <div style="white-space: pre-wrap; word-break: break-all">{{ detailForm.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BlogComment">
import {
  listComment,
  getComment,
  delComment,
  auditComment,
  rejectComment
} from '@/api/admin/blog/comment'

const { proxy } = getCurrentInstance()
const { comment_status } = proxy.useDict('comment_status')

const commentList = ref([])
const open = ref(false)
const detailOpen = ref(false)
const detailForm = ref<Record<string, any>>({})
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

const data = reactive<Record<string, any>>({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    articleTitle: undefined,
    content: undefined,
    status: undefined
  }
})

const { queryParams } = toRefs(data)

/** 查询评论列表 */
function getList() {
  loading.value = true
  listComment(queryParams.value)
    .then(response => {
      commentList.value = response.rows
      total.value = response.total
    })
    .finally(() => {
      loading.value = false
    })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 查看评论详情（只读） */
function handleView(row) {
  getComment(row.id)
    .then(response => {
      detailForm.value = ((response as any)?.data ?? response) || {}
      detailOpen.value = true
    })
    .catch(error => {
      console.error('获取评论详情失败:', error)
      proxy.$modal.msgError('获取评论详情失败')
    })
}

/** 审核通过按钮操作 */
function handleAudit(row) {
  const commentIds = row.id || ids.value
  proxy.$modal
    .confirm('是否确认审核通过评论编号为"' + commentIds + '"的数据项？')
    .then(function () {
      return auditComment(commentIds)
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('审核通过成功')
    })
    .catch(error => {
      if (error !== 'cancel') console.error('操作失败:', error)
    })
}

/** 审核拒绝按钮操作 */
function handleReject(row) {
  const commentIds = row.id || ids.value
  proxy.$modal
    .confirm('是否确认审核拒绝评论编号为"' + commentIds + '"的数据项？')
    .then(function () {
      return rejectComment(commentIds)
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('审核拒绝成功')
    })
    .catch(error => {
      if (error !== 'cancel') console.error('操作失败:', error)
    })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const commentIds = row.id || ids.value
  proxy.$modal
    .confirm('是否确认删除评论编号为"' + commentIds + '"的数据项？')
    .then(function () {
      return delComment(commentIds)
    })
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(error => {
      if (error !== 'cancel') console.error('操作失败:', error)
    })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    'system/comment/export',
    {
      ...queryParams.value
    },
    `comment_${new Date().getTime()}.xlsx`
  )
}

getList()
</script>

<style scoped>
/* 评论状态标签样式优化 */
.comment-status-tag {
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

/* 状态标签深度样式控制 */
:deep(.dict-tag) {
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
  line-height: 1.4;
}

/* 状态下拉选择框样式优化 */
.status-select {
  width: 120px;
}

:deep(.status-select .el-input__wrapper) {
  height: 32px;
  font-size: 13px;
}

:deep(.status-select .el-select__placeholder) {
  font-size: 13px;
}

:deep(.status-select .el-input__inner) {
  font-size: 13px;
}

/* 下拉选项样式优化 */
:deep(.el-select-dropdown__item) {
  font-size: 13px;
  padding: 8px 12px;
  line-height: 1.4;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .status-select {
    width: 100px;
  }

  :deep(.status-select .el-input__wrapper) {
    height: 30px;
    font-size: 12px;
  }

  :deep(.status-select .el-select__placeholder) {
    font-size: 12px;
  }

  :deep(.status-select .el-input__inner) {
    font-size: 12px;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 12px;
    padding: 6px 10px;
  }

  :deep(.dict-tag) {
    font-size: 12px;
    padding: 3px 6px;
  }
}

@media (max-width: 480px) {
  .status-select {
    width: 90px;
  }

  :deep(.status-select .el-input__wrapper) {
    height: 28px;
    font-size: 11px;
  }

  :deep(.status-select .el-select__placeholder) {
    font-size: 11px;
  }

  :deep(.status-select .el-input__inner) {
    font-size: 11px;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 11px;
    padding: 5px 8px;
  }

  :deep(.dict-tag) {
    font-size: 11px;
    padding: 2px 5px;
  }
}
</style>
