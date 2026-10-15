<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="loadTree">
      <el-tab-pane label="头部导航" name="header" />
      <el-tab-pane label="页脚快速导航" name="footer" />
    </el-tabs>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['site:nav:add']"
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd()"
        >
          添加导航项
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['site:nav:add']"
          type="primary"
          plain
          icon="Plus"
          :disabled="navTree.length === 0"
          title="为当前导航项添加二级项，也可直接点击行内「子项」"
          @click="handleAddChild()"
        >
          添加二级项
        </el-button>
      </el-col>
    </el-row>

    <el-table
      v-loading="loading"
      :data="navTree"
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
    >
      <el-table-column label="标签" prop="label" min-width="180">
        <template #default="scope">
          <span>{{ scope.row.label }}</span>
          <el-tag
            v-if="pageSlugOf(scope.row)"
            size="small"
            type="success"
            effect="plain"
            class="page-tag"
          >
            页面
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="链接类型" align="center" width="110">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.linkType === 'cta' ? 'warning' : 'info'">
            {{ linkTypeLabel(scope.row.linkType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="链接值" prop="linkValue" min-width="200" show-overflow-tooltip />
      <el-table-column label="排序" prop="sort" align="center" width="70" />
      <el-table-column label="可见" align="center" width="80">
        <template #default="scope">
          <el-switch
            v-hasPermi="['site:nav:edit']"
            :model-value="scope.row.visible === 1"
            inline-prompt
            @change="(val: any) => handleVisibleChange(scope.row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="230">
        <template #default="scope">
          <el-button
            v-if="scope.row.parentId === 0"
            link
            type="primary"
            icon="Plus"
            @click="handleAddChild(scope.row)"
          >
            子项
          </el-button>
          <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
            link
            :disabled="scope.row.sort <= minSortOf(scope.row)"
            @click="handleMove(scope.row, -1)"
          >
            上移
          </el-button>
          <el-button
            link
            :disabled="scope.row.sort >= maxSortOf(scope.row)"
            @click="handleMove(scope.row, 1)"
          >
            下移
          </el-button>
          <el-button
            v-hasPermi="['site:nav:remove']"
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="open" :title="dialogTitle" width="520px" append-to-body>
      <el-form ref="navRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="导航标签" prop="label">
          <el-input v-model="form.label" placeholder="如：关于我们" :maxlength="50" />
        </el-form-item>
        <el-form-item v-if="form.linkType !== 'url'" label="链接页面">
          <el-select
            v-model="selectedPageSlug"
            placeholder="从已发布页面中选择（可留空手填路径）"
            clearable
            filterable
            style="width: 100%"
            @change="handlePageSelect"
          >
            <el-option
              v-for="p in pageOptions"
              :key="p.id"
              :label="`${p.title || p.slug}（/page/${p.slug}）`"
              :value="p.slug"
            />
          </el-select>
          <div class="slug-tip">
            {{
              pageOptions.length
                ? '选择页面后自动填入标签与路径；页面列表也能一键加入/移出导航'
                : '暂无可选页面：请先在「页面管理」新建并发布页面'
            }}
          </div>
        </el-form-item>
        <el-form-item label="链接类型" prop="linkType">
          <el-radio-group v-model="form.linkType">
            <el-radio label="path">站内路径</el-radio>
            <el-radio label="url">外链</el-radio>
            <el-radio v-if="form.location === 'header'" label="cta">咨询按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="链接值" prop="linkValue">
          <el-input
            v-model="form.linkValue"
            :placeholder="form.linkType === 'url' ? 'https://example.com' : '如：/about'"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="可见" prop="visible">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SiteNav">
import { computed, onMounted, ref } from 'vue'
import { listNav, addNav, updateNav, delNav } from '@/api/admin/nav'
import { listPage } from '@/api/admin/page'
import type { SitePage } from '@/api/admin/page'
import type { SiteNavItem } from '@/api/site/nav'
import { pagePath, pageSlugFromPath } from '@/utils/siteNav'

const { proxy } = getCurrentInstance()

const activeTab = ref<'header' | 'footer'>('header')
const loading = ref(true)
const navTree = ref<SiteNavItem[]>([])
const open = ref(false)
const dialogTitle = ref('')

const form = ref<Partial<SiteNavItem>>({})
const editingId = ref<number | null>(null)
const editMode = ref<'add' | 'edit'>('add')

const rules = {
  label: [{ required: true, message: '导航标签不能为空', trigger: 'blur' }],
  linkValue: [{ required: true, message: '链接值不能为空', trigger: 'blur' }]
}

const linkTypeLabel = (type?: string) =>
  ({ path: '站内路径', url: '外链', cta: '咨询按钮' })[type || 'path'] || '站内路径'

/* ---------- 数据加载 ---------- */

const loadTree = async () => {
  loading.value = true
  try {
    const response = await listNav(activeTab.value)
    navTree.value = response?.data || []
  } finally {
    loading.value = false
  }
}

/* ---------- 页面选择：导航项与「页面管理」打通 ---------- */

/** 已发布页面（仅它们有可访问的前台地址） */
const publishedPages = ref<SitePage[]>([])
const pageOptions = computed(() => publishedPages.value.filter(p => p.slug && p.id))

/** 当前编辑的导航项是否指向自定义页面（供下拉回显/「页面」标记） */
const pageSlugOf = (row: SiteNavItem) =>
  row.linkType === 'url' ? '' : pageSlugFromPath(row.linkValue)
const selectedPageSlug = ref('')

const loadPublishedPages = async () => {
  try {
    const response = await listPage({ pageNum: 1, pageSize: 200, status: '1' })
    publishedPages.value = response?.rows || []
  } catch {
    publishedPages.value = []
  }
}

/** 选中页面：自动填充标签与路径（标签保持可改） */
const handlePageSelect = (slug: string) => {
  if (!slug) return
  const page = publishedPages.value.find(p => p.slug === slug)
  if (!page) return
  if (!form.value.label?.trim()) form.value.label = page.title || slug
  form.value.linkValue = pagePath(slug)
}

onMounted(() => {
  loadTree()
  loadPublishedPages()
})

watch(activeTab, () => {
  loadTree()
})

/* ---------- 同层排序辅助 ---------- */

const siblingsOf = (row: SiteNavItem): SiteNavItem[] => {
  if (row.parentId && row.parentId !== 0) {
    const parent = findItem(navTree.value, row.parentId)
    return parent?.children || []
  }
  return navTree.value
}

const findItem = (list: SiteNavItem[], id: number): SiteNavItem | null => {
  for (const item of list) {
    if (item.id === id) return item
    if (item.children) {
      const found = findItem(item.children, id)
      if (found) return found
    }
  }
  return null
}

const minSortOf = (row: SiteNavItem) => Math.min(...siblingsOf(row).map(i => i.sort ?? 0))
const maxSortOf = (row: SiteNavItem) => Math.max(...siblingsOf(row).map(i => i.sort ?? 0))

/** 同层相邻交换：两行互换 sort 并分别提交 */
const handleMove = async (row: SiteNavItem, delta: number) => {
  const siblings = siblingsOf(row)
  const index = siblings.findIndex(i => i.id === row.id)
  const target = siblings[index + delta]
  if (!target) return
  const mySort = row.sort ?? 0
  const targetSort = target.sort ?? 0
  await Promise.all([
    updateNav({ id: row.id, sort: targetSort }),
    updateNav({ id: target.id, sort: mySort })
  ])
  proxy?.$modal?.msgSuccess('排序已更新')
  loadTree()
}

const handleVisibleChange = async (row: SiteNavItem, val: any) => {
  await updateNav({ id: row.id, visible: val ? 1 : 0 })
  proxy?.$modal?.msgSuccess('已更新')
  loadTree()
}

/* ---------- 新增 / 编辑 / 删除 ---------- */

function handleAdd() {
  editMode.value = 'add'
  selectedPageSlug.value = ''
  form.value = {
    location: activeTab.value,
    parentId: 0,
    label: '',
    linkType: 'path',
    linkValue: '',
    sort: (navTree.value.length || 0) + 1,
    visible: 1
  }
  open.value = true
  dialogTitle.value = '添加导航项'
}

function handleAddChild(parent?: SiteNavItem) {
  const parentRow = parent || navTree.value[0]
  if (!parentRow) return
  editMode.value = 'add'
  selectedPageSlug.value = ''
  form.value = {
    location: activeTab.value,
    parentId: parentRow.id,
    label: '',
    linkType: 'path',
    linkValue: '',
    sort: (parentRow.children?.length || 0) + 1,
    visible: 1
  }
  open.value = true
  dialogTitle.value = `添加二级项（${parentRow.label}）`
}

function handleEdit(row: SiteNavItem) {
  editMode.value = 'edit'
  editingId.value = row.id ?? null
  selectedPageSlug.value = pageSlugOf(row)
  form.value = { ...row }
  open.value = true
  dialogTitle.value = '编辑导航项'
}

function submitForm() {
  proxy?.$refs?.['navRef']?.validate(async (valid: boolean) => {
    if (!valid) return
    const payload = {
      ...form.value,
      label: (form.value.label || '').trim(),
      linkValue: (form.value.linkValue || '').trim()
    }
    if (editMode.value === 'edit' && editingId.value) {
      await updateNav({ ...payload, id: editingId.value })
    } else {
      await addNav(payload)
    }
    proxy?.$modal?.msgSuccess(editMode.value === 'edit' ? '修改成功' : '新增成功')
    open.value = false
    loadTree()
  })
}

function handleDelete(row: SiteNavItem) {
  proxy?.$modal
    ?.confirm(
      `确定删除导航项「${row.label}」吗？${row.children?.length ? '其子项将一并删除。' : ''}`
    )
    .then(async () => {
      await delNav(row.id!)
      proxy?.$modal?.msgSuccess('删除成功')
      loadTree()
    })
    .catch(() => {})
}
</script>

<style scoped>
.page-tag {
  margin-left: 6px;
}

.slug-tip {
  color: var(--el-text-color-secondary, #909399);
  font-size: 12px;
  line-height: 1.5;
}
</style>
