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
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入页面标题"
          clearable
          :maxlength="100"
          class="query-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="别名" prop="slug">
        <el-input
          v-model="queryParams.slug"
          placeholder="请输入页面别名"
          clearable
          :maxlength="100"
          class="query-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="页面状态"
          clearable
          class="status-select"
        >
          <el-option label="草稿" value="0" />
          <el-option label="已发布" value="1" />
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
          v-hasPermi="['blog:page:add']"
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
        >
          新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['blog:page:edit']"
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate()"
        >
          修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['blog:page:remove']"
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
          v-hasPermi="['blog:page:export']"
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
      :data="pageList"
      :height="tableHeight"
      stripe
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="页面ID" align="center" prop="id" width="70" />
      <el-table-column
        label="标题"
        align="left"
        prop="title"
        min-width="110"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="别名"
        align="center"
        prop="slug"
        min-width="100"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="摘要"
        align="center"
        prop="summary"
        min-width="120"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <span>{{ scope.row.summary || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="85">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="导航显示" align="center" prop="showInNav" width="85">
        <template #default="scope">
          <el-tag :type="String(scope.row.showInNav) === '1' ? 'success' : 'info'" size="small">
            {{ navText(scope.row.showInNav) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="70" />
      <el-table-column label="浏览数" align="center" prop="viewCount" width="80" />
      <!-- 更新时间与操作同为右侧固定列：整表列宽合计较大，窄屏出现横向溢出时
           未固定的列会被 fixed 列压住导致“显示不全”，钉住可保证时间始终完整可见 -->
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" fixed="right">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime || scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        width="170"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button
              v-hasPermi="['blog:page:edit']"
              link
              type="primary"
              icon="Edit"
              size="small"
              @click="handleUpdate(scope.row)"
            />
          </el-tooltip>
          <el-tooltip :content="String(scope.row.status) === '1' ? '下架' : '发布'" placement="top">
            <el-button
              v-hasPermi="['blog:page:edit']"
              link
              :type="String(scope.row.status) === '1' ? 'warning' : 'success'"
              :icon="String(scope.row.status) === '1' ? 'Bottom' : 'Top'"
              size="small"
              @click="handleStatusChange(scope.row)"
            />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button
              v-hasPermi="['blog:page:remove']"
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

    <!-- 添加或修改页面对话框 -->
    <el-dialog v-model="open" :title="title" :width="dialogWidth" append-to-body destroy-on-close>
      <el-form ref="pageRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="页面标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入页面标题"
            :maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="页面别名" prop="slug">
          <el-input
            v-model="form.slug"
            placeholder="请输入页面别名，例如 about-us"
            :maxlength="100"
            show-word-limit
          />
          <div v-if="!form.id" class="slug-tip">
            别名只允许字母、数字、下划线、连字符（1-100个字符），保存后用于前台访问地址
          </div>
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            :maxlength="255"
            show-word-limit
            placeholder="请输入页面摘要"
          />
        </el-form-item>
        <el-form-item label="预置区块">
          <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap">
            <el-dropdown trigger="click" @command="insertBlock">
              <el-button type="primary" plain>
                插入区块模板<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="tpl in blockTemplates"
                    :key="tpl.name"
                    :command="tpl.name"
                  >
                    {{ tpl.name }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button @click="previewVisible = true">预览</el-button>
            <span class="slug-tip">区块为 HTML 片段，插入到光标处后可直接修改文字；预览为前台实际渲染效果</span>
          </div>
        </el-form-item>
        <el-form-item label="页面内容" prop="content">
          <el-input
            ref="contentRef"
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="请输入 Markdown 内容"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">草稿</el-radio>
            <el-radio label="1">已发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="导航显示" prop="showInNav">
          <el-switch
            v-model="form.showInNav"
            active-value="1"
            inactive-value="0"
            active-text="显示"
            inactive-text="隐藏"
            inline-prompt
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            :max="9999"
            controls-position="right"
            placeholder="请输入排序号"
          />
        </el-form-item>
        <el-form-item label="SEO标题" prop="seoTitle">
          <el-input
            v-model="form.seoTitle"
            placeholder="请输入 SEO 标题"
            :maxlength="255"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="SEO关键词" prop="seoKeywords">
          <el-input
            v-model="form.seoKeywords"
            placeholder="多个关键词请用英文逗号分隔"
            :maxlength="255"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="SEO描述" prop="seoDescription">
          <el-input
            v-model="form.seoDescription"
            type="textarea"
            :rows="3"
            :maxlength="500"
            show-word-limit
            placeholder="请输入 SEO 描述"
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

    <!-- 页面内容预览（与前台同一渲染管线：renderMarkdown + 消毒） -->
    <el-dialog v-model="previewVisible" title="页面内容预览" width="720px" append-to-body>
      <div class="page-preview" v-html="previewHtml"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BlogPage">
import {
  listPage,
  getPage,
  addPage,
  updatePage,
  changePageStatus,
  delPage
} from '@/api/admin/blog/page'
import type { BlogPage, PageQueryParams, PageStatus } from '@/api/admin/blog/page'
import { parseTime } from '@/utils/zhi'
import { renderMarkdown } from '@/utils/markdown'
import { sanitizeArticleContent } from '@/utils/sanitize'

const { proxy } = getCurrentInstance()

/** 页面别名只允许字母、数字、下划线、连字符 */
const SLUG_PATTERN = /^[A-Za-z0-9_-]{1,100}$/

const pageList = ref<BlogPage[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const screenWidth = ref(window.innerWidth)

/** 生成页面表单默认值 */
function createDefaultForm(): BlogPage {
  return {
    id: undefined,
    title: '',
    slug: '',
    summary: '',
    content: '',
    status: '0',
    showInNav: '1',
    sort: 0,
    viewCount: 0,
    seoTitle: '',
    seoKeywords: '',
    seoDescription: ''
  }
}

const form = ref<BlogPage>(createDefaultForm())

const queryParams = ref<PageQueryParams>({
  pageNum: 1,
  pageSize: 10,
  title: undefined,
  slug: undefined,
  status: undefined
})

const rules = {
  title: [
    { required: true, message: '页面标题不能为空', trigger: 'blur' },
    { max: 100, message: '页面标题不能超过100个字符', trigger: 'blur' }
  ],
  slug: [
    { required: true, message: '页面别名不能为空', trigger: 'blur' },
    { pattern: SLUG_PATTERN, message: '别名只允许字母、数字、下划线、连字符', trigger: 'blur' }
  ],
  summary: [{ max: 255, message: '摘要不能超过255个字符', trigger: 'blur' }],
  seoTitle: [{ max: 255, message: 'SEO标题不能超过255个字符', trigger: 'blur' }],
  seoKeywords: [{ max: 255, message: 'SEO关键词不能超过255个字符', trigger: 'blur' }],
  seoDescription: [{ max: 500, message: 'SEO描述不能超过500个字符', trigger: 'blur' }]
}

// 根据屏幕宽度计算表格高度
const tableHeight = computed(() => {
  return screenWidth.value < 768 ? '300px' : 'calc(100vh - 280px)'
})

// 根据屏幕宽度计算对话框宽度
const dialogWidth = computed(() => {
  return screenWidth.value < 768 ? '90%' : '720px'
})

/** 页面状态文本 */
function statusText(status?: PageStatus): string {
  return status === '1' ? '已发布' : '草稿'
}

/** 页面状态标签类型 */
function statusTagType(status?: PageStatus): 'success' | 'info' {
  return status === '1' ? 'success' : 'info'
}

/** 导航显示文本 */
function navText(showInNav?: string): string {
  return String(showInNav) === '1' ? '显示' : '隐藏'
}

/** 提取后端返回的错误消息 */
function resolveErrorMessage(error: unknown, fallback: string): string {
  const err = error as { msg?: string; message?: string } | undefined
  return err?.msg || err?.message || fallback
}

/** 查询页面列表 */
function getList() {
  loading.value = true
  listPage(queryParams.value)
    .then(response => {
      pageList.value = response.rows || []
      total.value = response.total || 0
    })
    .finally(() => {
      loading.value = false
    })
    .catch((error: unknown) => {
      loading.value = false
      proxy.$modal.msgError(resolveErrorMessage(error, '获取页面列表失败'))
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
    title: undefined,
    slug: undefined,
    status: undefined
  }
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: BlogPage[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  form.value = createDefaultForm()
  open.value = true
  title.value = '新增页面'
}

/** 预置区块模板：HTML 片段，插入到页面内容光标处（Markdown 内容会透传渲染 HTML，前台统一消毒） */
const blockTemplates = [
  {
    name: 'Hero 横幅',
    html: '<section style="padding:48px 24px;text-align:center;border-radius:12px;background:linear-gradient(135deg,#4f46e5,#1c1917);color:#ffffff"><h1 style="margin:0 0 12px">主标题文案</h1><p style="margin:0;opacity:.85">在这里输入副标语，一句话说明企业定位与核心价值</p></section>'
  },
  {
    name: '三列卡片',
    html: '<section style="display:flex;gap:20px;flex-wrap:wrap;margin:24px 0"><div style="flex:1;min-width:220px;padding:20px;border:1px solid #e7e5e4;border-radius:12px"><h3 style="margin:0 0 8px">核心业务一</h3><p style="margin:0">用一句话介绍该项业务的核心优势</p></div><div style="flex:1;min-width:220px;padding:20px;border:1px solid #e7e5e4;border-radius:12px"><h3 style="margin:0 0 8px">核心业务二</h3><p style="margin:0">用一句话介绍该项业务的核心优势</p></div><div style="flex:1;min-width:220px;padding:20px;border:1px solid #e7e5e4;border-radius:12px"><h3 style="margin:0 0 8px">核心业务三</h3><p style="margin:0">用一句话介绍该项业务的核心优势</p></div></section>'
  },
  {
    name: '图文左右',
    html: '<section style="display:flex;gap:24px;align-items:center;flex-wrap:wrap;margin:24px 0"><div style="flex:1;min-width:260px"><h2 style="margin:0 0 12px">标题文案</h2><p style="margin:0">在这里输入对该产品或服务的说明，突出核心优势与客户价值，可配合图片使用。</p></div><div style="flex:1;min-width:220px;font-size:64px;text-align:center">🖼️</div></section>'
  },
  {
    name: '数据条',
    html: '<section style="display:flex;gap:20px;flex-wrap:wrap;text-align:center;margin:24px 0;padding:28px;border-radius:12px;background:#1c1917;color:#ffffff"><div style="flex:1;min-width:140px"><div style="font-size:30px;font-weight:800">100+</div><div style="opacity:.7">服务客户</div></div><div style="flex:1;min-width:140px"><div style="font-size:30px;font-weight:800">10 年</div><div style="opacity:.7">行业经验</div></div><div style="flex:1;min-width:140px"><div style="font-size:30px;font-weight:800">50+</div><div style="opacity:.7">成功案例</div></div><div style="flex:1;min-width:140px"><div style="font-size:30px;font-weight:800">24h</div><div style="opacity:.7">响应服务</div></div></section>'
  },
  {
    name: 'CTA 行动条',
    html: '<section style="padding:36px 24px;text-align:center;border-radius:12px;background:linear-gradient(135deg,#4f46e5,#6366f1);color:#ffffff;margin:24px 0"><h2 style="margin:0 0 12px">期待与您的合作</h2><p style="margin:0 0 16px;opacity:.85">欢迎联系我们获取方案与报价</p><a href="/blog/contact" style="display:inline-block;padding:10px 26px;border-radius:8px;background:#ffffff;color:#4f46e5;font-weight:600;text-decoration:none">联系我们</a></section>'
  }
]

const contentRef = ref<any>(null)
const previewVisible = ref(false)
const previewHtml = computed(() => sanitizeArticleContent(renderMarkdown(form.value.content || '')))

/** 把区块模板插入到页面内容光标处 */
function insertBlock(name: string) {
  const tpl = blockTemplates.find(b => b.name === name)
  if (!tpl) return
  const current = form.value.content || ''
  const textarea = contentRef.value?.textarea as HTMLTextAreaElement | undefined
  const pos = textarea?.selectionStart ?? current.length
  const snippet = `\n${tpl.html}\n`
  form.value.content = current.slice(0, pos) + snippet + current.slice(pos)
  nextTick(() => textarea?.focus())
}

/** 修改按钮操作 */
function handleUpdate(row?: BlogPage) {
  const id = row?.id || ids.value[0]
  if (!id) {
    proxy.$modal.msgError('请选择要修改的页面')
    return
  }
  getPage(id)
    .then(response => {
      const data = response.data || {}
      form.value = {
        ...createDefaultForm(),
        ...data,
        title: data.title || '',
        slug: data.slug || '',
        summary: data.summary || '',
        content: data.content || '',
        status: String(data.status) === '1' ? '1' : '0',
        showInNav: String(data.showInNav) === '0' ? '0' : '1',
        sort: Number(data.sort) || 0,
        seoTitle: data.seoTitle || '',
        seoKeywords: data.seoKeywords || '',
        seoDescription: data.seoDescription || ''
      }
      open.value = true
      title.value = '修改页面'
    })
    .catch((error: unknown) => {
      proxy.$modal.msgError(resolveErrorMessage(error, '获取页面详情失败'))
    })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  form.value = createDefaultForm()
}

/** 提交按钮 */
function submitForm() {
  if (!proxy || !proxy.$refs || !proxy.$refs['pageRef']) {
    proxy.$modal.msgError('表单引用失败')
    return
  }

  proxy.$refs['pageRef'].validate((valid: boolean) => {
    if (!valid) return

    const isEdit = !!form.value.id
    const payload: BlogPage = {
      ...form.value,
      title: (form.value.title || '').trim(),
      slug: (form.value.slug || '').trim(),
      summary: (form.value.summary || '').trim(),
      content: form.value.content || '',
      status: form.value.status === '1' ? '1' : '0',
      showInNav: form.value.showInNav === '0' ? '0' : '1',
      sort: Number(form.value.sort) || 0,
      seoTitle: (form.value.seoTitle || '').trim(),
      seoKeywords: (form.value.seoKeywords || '').trim(),
      seoDescription: (form.value.seoDescription || '').trim()
    }

    const action = isEdit ? updatePage(payload) : addPage(payload)
    action
      .then(() => {
        proxy.$modal.msgSuccess(isEdit ? '修改成功' : '新增成功')
        open.value = false
        getList()
      })
      .catch((error: unknown) => {
        proxy.$modal.msgError(resolveErrorMessage(error, isEdit ? '修改失败' : '新增失败'))
      })
  })
}

/** 发布/下架按钮操作 */
function handleStatusChange(row: BlogPage) {
  const newStatus: PageStatus = String(row.status) === '1' ? '0' : '1'
  const actionText = newStatus === '1' ? '发布' : '下架'
  proxy.$modal
    .confirm(`是否确认${actionText}页面《${row.title || ''}》？`)
    .then(() => changePageStatus(row.id, newStatus))
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

/** 删除按钮操作 */
function handleDelete(row?: BlogPage) {
  const deleteIds = row?.id || ids.value
  proxy.$modal
    .confirm('是否确认删除页面编号为"' + deleteIds + '"的数据项？')
    .then(() => delPage(deleteIds))
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
      'system/page/export',
      {
        ...queryParams.value
      },
      `page_${new Date().getTime()}.xlsx`
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

/* 页面别名输入提示 */
.slug-tip {
  color: var(--el-text-color-secondary, #909399);
  font-size: 12px;
  line-height: 1.5;
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
