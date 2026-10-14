<template>
  <div class="app-container">
    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="标签名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入标签名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="">
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate">
          修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">
          删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
    </el-row>

    <el-table v-loading="loading" :data="tagList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="标签ID" align="center" prop="id" />
      <el-table-column label="标签名称" align="center" prop="name" />
      <el-table-column label="文章数量" align="center" prop="articleCount" />
      <el-table-column label="描述" align="center" prop="description" />
      <el-table-column label="颜色" align="center" prop="color" />
      <el-table-column label="图标" align="center" prop="icon">
        <template #default="scope">
          <span>{{ scope.row.icon || '-' }}</span>
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
        width="150"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
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

    <!-- 添加或修改标签对话框 -->
    <el-dialog v-model="open" :title="title" width="500px" append-to-body>
      <el-form ref="tagRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input
            v-model="form.icon"
            placeholder="可输入 emoji 或图标标识，留空则不显示"
            maxlength="16"
            clearable
          />
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

<script setup lang="ts" name="BlogTag">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import { listTag, getTag, delTag, addTag, updateTag } from '@/api/admin/blog/tag'

const { proxy } = getCurrentInstance()

const tagList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive<Record<string, any>>({
  form: {} as Record<string, any>,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined
  },
  rules: {
    name: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询标签列表 */
function getList() {
  loading.value = true
  listTag(queryParams.value)
    .then(response => {
      tagList.value = response.rows
      total.value = response.total
    })
    .finally(() => {
      loading.value = false
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
    description: undefined,
    color: undefined,
    icon: undefined
  }
  proxy.resetForm('tagRef')
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

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加标签'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  // 处理 ID：如果是 row 对象则取 row.id，否则取 ids 数组的第一个元素
  let tagId
  if (row && row.id) {
    // 从表格行点击修改按钮
    tagId = row.id
  } else if (ids.value && ids.value.length > 0) {
    // 从顶部修改按钮点击
    tagId = ids.value[0]
  } else {
    proxy.$modal.msgError('请先选择要修改的数据')
    return
  }

  getTag(tagId)
    .then(response => {
      // 数据加载完成后重置表单并填充数据
      reset()
      Object.assign(form.value, response.data)
      open.value = true
      title.value = '修改标签'
    })
    .catch(error => {
      console.error('获取标签信息失败:', error)
      proxy.$modal.msgError('获取标签信息失败')
    })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['tagRef'].validate(valid => {
    if (valid) {
      if (form.value.id !== undefined) {
        updateTag(form.value).then(response => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addTag(form.value).then(response => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const tagIds = row.id || ids.value
  proxy.$modal
    .confirm('是否确认删除标签编号为"' + tagIds + '"的数据项？')
    .then(function () {
      return delTag(tagIds)
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
    'system/tag/export',
    {
      ...queryParams.value
    },
    `tag_${new Date().getTime()}.xlsx`
  )
}

// 初始化页面
onMounted(() => {
  getList()
})
</script>
