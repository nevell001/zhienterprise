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
      <el-form-item label="友链名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入友链名称"
          clearable
          :maxlength="50"
          class="query-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="URL" prop="url" class="hidden-sm-and-down">
        <el-input
          v-model="queryParams.url"
          placeholder="请输入URL"
          clearable
          :maxlength="200"
          class="query-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="">
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :xs="6" :sm="6" :md="4" :lg="3" :xl="1.5">
        <el-button
          v-hasPermi="['blog:friendLink:add']"
          type="primary"
          plain
          icon="Plus"
          class="btn-full-width"
          @click="handleAdd"
        >
          新增
        </el-button>
      </el-col>
      <el-col :xs="6" :sm="6" :md="4" :lg="3" :xl="1.5">
        <el-button
          v-hasPermi="['blog:friendLink:edit']"
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          class="btn-full-width"
          @click="handleUpdate"
        >
          修改
        </el-button>
      </el-col>
      <el-col :xs="6" :sm="6" :md="4" :lg="3" :xl="1.5">
        <el-button
          v-hasPermi="['blog:friendLink:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          class="btn-full-width"
          @click="handleDelete"
        >
          删除
        </el-button>
      </el-col>
      <el-col :xs="6" :sm="6" :md="4" :lg="3" :xl="1.5">
        <el-button
          v-hasPermi="['blog:friendLink:export']"
          type="warning"
          plain
          icon="Download"
          class="btn-full-width"
          @click="handleExport"
        >
          导出
        </el-button>
      </el-col>
      <right-toolbar
        v-model:show-search="showSearch"
        class="top-right-btn"
        @query-table="getList"
      />
    </el-row>

    <el-table
      v-loading="loading"
      :data="friendLinkList"
      :height="tableHeight"
      stripe
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="友链ID" align="center" prop="id" width="80" />
      <el-table-column label="友链名称" align="center" prop="name" min-width="120" />
      <el-table-column
        label="URL"
        align="center"
        prop="url"
        :show-overflow-tooltip="true"
        min-width="180"
        class="hidden-sm-and-down"
      />
      <el-table-column
        label="Logo"
        align="center"
        prop="logo"
        width="80"
        class="hidden-xs-and-down"
      >
        <template #default="scope">
          <el-image
            v-if="scope.row.logo"
            style="width: 40px; height: 40px"
            :src="scope.row.logo"
            :preview-src-list="[scope.row.logo]"
            fit="cover"
          />
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column
        label="描述"
        align="center"
        prop="description"
        :show-overflow-tooltip="true"
        min-width="150"
        class="hidden-md-and-down"
      />
      <el-table-column
        label="申请人邮箱"
        align="center"
        prop="email"
        width="150"
        class="hidden-md-and-down"
      >
        <template #default="scope">
          <span>{{ scope.row.email || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="排序"
        align="center"
        prop="sort"
        width="70"
        class="hidden-sm-and-down"
      />
      <el-table-column label="状态" align="center" prop="status" width="180">
        <template #default="scope">
          <template v-if="String(scope.row.status) === '2'">
            <el-tag type="warning" size="small" style="margin-right: 4px">待审核</el-tag>
            <el-button
              v-hasPermi="['blog:friendLink:edit']"
              link
              type="success"
              size="small"
              @click="auditLink(scope.row, 'approve')"
            >
              通过
            </el-button>
            <el-button
              v-hasPermi="['blog:friendLink:edit']"
              link
              type="danger"
              size="small"
              @click="auditLink(scope.row, 'reject')"
            >
              拒绝
            </el-button>
          </template>
          <el-switch
            v-else
            v-model="scope.row.status"
            v-hasPermi="['blog:friendLink:edit']"
            :active-value="FRIEND_LINK_STATUS.NORMAL"
            :inactive-value="FRIEND_LINK_STATUS.DISABLED"
            active-text="正常"
            inactive-text="停用"
            inline-prompt
            :inline-prompt-width="70"
            @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="160"
        class="hidden-sm-and-down"
      >
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        width="100"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button
              v-hasPermi="['blog:friendLink:edit']"
              link
              type="primary"
              icon="Edit"
              size="small"
              @click="handleUpdate(scope.row)"
            />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button
              v-hasPermi="['blog:friendLink:remove']"
              link
              type="primary"
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

    <!-- 添加或修改友链对话框 -->
    <el-dialog v-model="open" :title="title" :width="dialogWidth" append-to-body>
      <el-form ref="friendLinkRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="友链名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入友链名称"
            :maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="URL" prop="url">
          <el-input v-model="form.url" placeholder="请输入URL" :maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="Logo" prop="logo">
          <el-input
            v-model="form.logo"
            placeholder="请输入Logo URL"
            :maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入描述"
            :maxlength="200"
            show-word-limit
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            :max="9999"
            placeholder="请输入排序号"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BlogFriendLink">
import { ref, reactive, toRefs, getCurrentInstance, onMounted, computed, onUnmounted } from 'vue'
import {
  listFriendLink,
  getFriendLink,
  delFriendLink,
  addFriendLink,
  updateFriendLink,
  auditFriendLink
} from '@/api/admin/blog/friendLink'
import { FRIEND_LINK_STATUS, auditStatusFor, type FriendLinkAuditAction } from './status'
import { parseTime } from '@/utils/zhi' // 导入时间解析工具

const { proxy } = getCurrentInstance()

const friendLinkList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const screenWidth = ref(window.innerWidth)

// 根据屏幕宽度计算表格高度
const tableHeight = computed(() => {
  return screenWidth.value < 768 ? '300px' : 'calc(100vh - 280px)'
})

// 根据屏幕宽度计算对话框宽度
const dialogWidth = computed(() => {
  return screenWidth.value < 768 ? '90%' : '500px'
})

const data = reactive<Record<string, any>>({
  form: {} as Record<string, any>,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    url: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: '友链名称不能为空', trigger: 'blur' }],
    url: [
      { required: true, message: 'URL不能为空', trigger: 'blur' },
      { type: 'url', message: '请输入有效的URL地址', trigger: 'blur' }
    ],
    sort: [{ type: 'number', message: '排序必须为数字', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 监听窗口大小变化
function handleResize() {
  screenWidth.value = window.innerWidth
}

/** 查询友链列表 */
function getList() {
  loading.value = true
  listFriendLink(queryParams.value)
    .then(response => {
      // 后端 status 是 String；这里再显式规范一次，确保 el-switch 的
      // [activeValue, inactiveValue].includes(status) 严格比较命中，不会在渲染时误触发 change。
      friendLinkList.value = (response.rows || []).map(row => ({
        ...row,
        status: row.status === null || row.status === undefined ? row.status : String(row.status)
      }))
      total.value = response.total
    })
    .finally(() => {
      loading.value = false
    })
    .catch(() => {
      loading.value = false
      proxy.$modal.msgError('获取友链列表失败')
    })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    name: undefined,
    url: undefined,
    logo: undefined,
    description: undefined,
    sort: 0,
    status: '0'
  }
  if (proxy && proxy.resetForm) {
    proxy.resetForm('friendLinkRef')
  }
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
    name: undefined,
    url: undefined,
    status: undefined
  }
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加友链'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getFriendLink(id)
    .then(response => {
      form.value = response.data
      open.value = true
      title.value = '修改友链'
    })
    .catch(() => {
      proxy.$modal.msgError('获取友链详情失败')
    })
}

/** 提交按钮 */
function submitForm() {
  if (!proxy || !proxy.$refs || !proxy.$refs['friendLinkRef']) {
    proxy.$modal.msgError('表单引用失败')
    return
  }

  proxy.$refs['friendLinkRef'].validate(valid => {
    if (valid) {
      const promise =
        form.value.id !== undefined ? updateFriendLink(form.value) : addFriendLink(form.value)

      promise
        .then(response => {
          proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
          open.value = false
          getList()
        })
        .catch(() => {
          proxy.$modal.msgError(form.value.id ? '修改失败' : '新增失败')
        })
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const deleteIds = row.id || ids.value
  proxy.$modal
    .confirm('是否确认删除友链编号为"' + deleteIds + '"的数据项？')
    .then(function () {
      return delFriendLink(deleteIds)
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
  if (proxy && proxy.download) {
    proxy.download(
      'system/friendLink/export',
      {
        ...queryParams.value
      },
      `friendLink_${new Date().getTime()}.xlsx`
    )
  }
}

/** 状态变更处理 */
function handleStatusChange(row) {
  const statusChange = { ...row }
  updateFriendLink(statusChange)
    .then(() => {
      proxy.$modal.msgSuccess('状态更新成功')
    })
    .catch(() => {
      // 状态更新失败时，恢复原状态
      row.status = row.status === '0' ? '1' : '0'
      proxy.$modal.msgError('状态更新失败')
    })
}

/** 审核友链申请：模板只传语义化动作，落库状态由 auditStatusFor 统一映射（通过=0，拒绝=1） */
async function auditLink(row, action: FriendLinkAuditAction) {
  const status = auditStatusFor(action)
  try {
    await auditFriendLink(row.id, status)
    proxy.$modal.msgSuccess(action === 'approve' ? '已通过该申请' : '已拒绝该申请')
    getList()
  } catch (error: any) {
    proxy.$modal.msgError('审核失败：' + (error.message || '未知错误'))
  }
}

// 监听窗口大小变化
onMounted(() => {
  getList()
  window.addEventListener('resize', handleResize)
})

// 移除窗口大小变化监听
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

.search-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--el-text-color-regular, #606266);
}

/* 查询输入框样式 */
.query-input {
  width: 200px;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .app-container {
    padding: 8px;
  }

  .search-form {
    padding: 12px;
    margin-bottom: 8px;
  }

  .search-form :deep(.el-form-item) {
    margin-bottom: 10px;
    display: block;
  }

  .search-form :deep(.el-form-item__label) {
    display: block;
    margin-bottom: 4px;
    text-align: left;
    width: 100% !important;
  }

  .search-form :deep(.el-form-item__content) {
    width: 100%;
  }

  .query-input {
    width: 100%;
  }

  .btn-full-width {
    width: 100%;
    margin-bottom: 8px;
  }
}

@media (max-width: 480px) {
  .search-form {
    padding: 10px;
  }

  .query-input {
    width: 100%;
  }
}

/* 隐藏特定屏幕尺寸的元素 */
.hidden-xs-and-down {
  display: none;
}

.hidden-sm-and-down {
  display: none;
}

.hidden-md-and-down {
  display: none;
}

@media (min-width: 480px) {
  .hidden-xs-and-down {
    display: table-cell;
  }
}

@media (min-width: 768px) {
  .hidden-sm-and-down {
    display: table-cell;
  }

  .search-form :deep(.el-form-item) {
    display: inline-flex;
  }
}

@media (min-width: 992px) {
  .hidden-md-and-down {
    display: table-cell;
  }
}

/* 表格样式优化 */
:deep(.el-table__header th),
:deep(.el-table__body td) {
  padding: 10px 4px;
  font-size: 14px;
}

@media (max-width: 768px) {
  :deep(.el-table__header th),
  :deep(.el-table__body td) {
    padding: 8px 2px;
    font-size: 12px;
  }
}

/* 按钮区域样式 */
.mb8 {
  margin-bottom: 12px;
}

/* 右侧工具栏样式 */
.top-right-btn {
  margin-left: auto;
}
</style>
