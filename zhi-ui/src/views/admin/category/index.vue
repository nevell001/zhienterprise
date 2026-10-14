<template>
  <div class="app-container">
    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="分类名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入分类名称"
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

    <el-table v-loading="loading" :data="categoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="分类ID" align="center" prop="id" />
      <el-table-column label="分类名称" align="center" prop="name" />
      <el-table-column label="排序" align="center" prop="sort" />
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

    <!-- 添加或修改分类对话框 -->
    <el-dialog v-model="open" :title="title" width="500px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择上级分类" clearable>
            <el-option label="顶级分类" :value="0" />
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" controls-position="right" :min="0" />
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

<script setup lang="ts" name="BlogCategory">
import { ref, reactive, toRefs, computed, getCurrentInstance, onMounted } from 'vue'
import {
  listCategory,
  getCategory,
  delCategory,
  addCategory,
  updateCategory
} from '@/api/admin/blog/category'

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict('sys_normal_disable')

const categoryList = ref([])
const categoryOptions = ref<any[]>([])
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
    name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 上级分类候选：排除当前正在编辑的分类自身 */
const parentOptions = computed(() =>
  categoryOptions.value.filter(item => item.id !== form.value.id)
)

/** 查询分类列表 */
function getList() {
  loading.value = true
  listCategory(queryParams.value)
    .then(response => {
      categoryList.value = response.rows
      total.value = response.total
    })
    .finally(() => {
      loading.value = false
    })
  // 拉取全部分类作为上级分类下拉选项
  listCategory({ pageNum: 1, pageSize: 1000 }).then(response => {
    categoryOptions.value = response.rows || []
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
    parentId: 0,
    sort: 0
  }
  proxy.resetForm('categoryRef')
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
  title.value = '添加分类'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  // 处理 ID：如果是 row 对象则取 row.id，否则取 ids 数组的第一个元素
  let id
  if (row && row.id) {
    // 从表格行点击修改按钮
    id = row.id
  } else if (ids.value && ids.value.length > 0) {
    // 从顶部修改按钮点击
    id = ids.value[0]
  } else {
    proxy.$modal.msgError('请先选择要修改的数据')
    return
  }

  getCategory(id)
    .then(response => {
      // 数据加载完成后重置表单并填充数据
      reset()
      Object.assign(form.value, response.data)
      open.value = true
      title.value = '修改分类'
    })
    .catch(error => {
      console.error('获取分类信息失败:', error)
      proxy.$modal.msgError('获取分类信息失败')
    })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['categoryRef'].validate(valid => {
    if (valid) {
      if (form.value.id !== undefined) {
        updateCategory(form.value).then(response => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addCategory(form.value).then(response => {
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
  const deleteIds = row.id || ids.value
  proxy.$modal
    .confirm('是否确认删除分类编号为"' + deleteIds + '"的数据项？')
    .then(function () {
      return delCategory(deleteIds)
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
    'system/category/export',
    {
      ...queryParams.value
    },
    `category_${new Date().getTime()}.xlsx`
  )
}

// 初始化页面
onMounted(() => {
  getList()
})
</script>
