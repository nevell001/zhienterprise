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
          <span>{{ displayLabelOf(scope.row) }}</span>
          <el-tag
            v-if="isControlledLabel(scope.row)"
            size="small"
            type="warning"
            effect="plain"
            class="page-tag"
            title="该标签取自「站点设置 → 栏目名称」，在本页修改不会生效"
          >
            栏目名称控制
          </el-tag>
          <el-tag
            v-if="pageSlugOf(scope.row)"
            size="small"
            type="success"
            effect="plain"
            class="page-tag"
          >
            页面
          </el-tag>
          <el-tag
            v-if="isCategoryLink(scope.row)"
            size="small"
            type="info"
            effect="plain"
            class="page-tag"
          >
            分类
          </el-tag>
          <el-tag
            v-if="isTagLink(scope.row)"
            size="small"
            type="info"
            effect="plain"
            class="page-tag"
          >
            标签
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
          <el-input
            v-model="form.label"
            placeholder="如：关于我们"
            :maxlength="50"
            :disabled="labelControlled"
          />
          <div v-if="labelControlled" class="slug-tip">{{ labelControlTip }}</div>
        </el-form-item>
        <el-form-item v-if="form.linkType !== 'url'" label="链接目标">
          <el-select
            v-model="selectedTarget"
            placeholder="选择页面 / 文章分类 / 标签（也可留空手填路径）"
            clearable
            filterable
            style="width: 100%"
            @change="handleTargetSelect"
          >
            <el-option-group v-for="group in targetOptions" :key="group.label" :label="group.label">
              <el-option
                v-for="opt in group.options"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-option-group>
          </el-select>
          <div class="slug-tip">{{ targetTip }}</div>
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
            :placeholder="
              form.linkType === 'url'
                ? 'https://example.com'
                : '如：/news、/category/分类别名、/tag/标签ID、/about'
            "
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
import { listCategory } from '@/api/admin/category'
import { listTag } from '@/api/admin/tag'
import { getConfigByKey } from '@/api/admin/setting'
import type { SitePage } from '@/api/admin/page'
import type { Category, Tag } from '@/types'
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

/** 链接值按类型校验：站内路径必须以 / 开头，外链必须是 http(s) */
const validateLinkValue = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const raw = (value || '').trim()
  if (!raw) {
    callback(new Error('链接值不能为空'))
    return
  }
  if (form.value.linkType === 'url') {
    if (!/^https?:\/\//i.test(raw)) {
      callback(new Error('外链需以 http:// 或 https:// 开头'))
      return
    }
  } else if (!raw.startsWith('/')) {
    callback(new Error('站内路径需以 / 开头，如 /news、/category/分类别名'))
    return
  }
  callback()
}

const rules = {
  label: [{ required: true, message: '导航标签不能为空', trigger: 'blur' }],
  linkValue: [{ required: true, validator: validateLinkValue, trigger: 'blur' }]
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

/** 文章分类 / 标签：二级菜单里最常见的链接目标 */
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])

/** 常用站内页面：不必手输路径，也避免写错（如文章列表实际是 /news） */
const FIXED_TARGETS = [
  { path: '/', label: '首页' },
  { path: '/news', label: '新闻动态' },
  { path: '/business', label: '核心业务' },
  { path: '/solutions', label: '解决方案' },
  { path: '/about', label: '关于我们' },
  { path: '/honors', label: '荣誉资质' },
  { path: '/history', label: '发展历程' },
  { path: '/contact', label: '联系我们' }
]

/** 下拉值编码：`fixed:/news`、`page:about`、`cat:12`、`tag:3` */
const selectedTarget = ref('')

const targetOptions = computed(() =>
  [
    {
      label: '常用页面',
      options: FIXED_TARGETS.map(t => ({
        label: `${t.label}（${t.path}）`,
        value: `fixed:${t.path}`
      }))
    },
    {
      label: '自定义页面',
      options: pageOptions.value.map(p => ({
        label: `${p.title || p.slug}（/page/${p.slug}）`,
        value: `page:${p.slug}`
      }))
    },
    {
      label: '文章分类',
      options: categories.value
        .filter(c => c.id)
        .map(c => ({ label: `${c.name}（/category/${c.alias || c.id}）`, value: `cat:${c.id}` }))
    },
    {
      label: '文章标签',
      options: tags.value
        .filter(t => t.id)
        .map(t => ({ label: `${t.name}（/tag/${t.id}）`, value: `tag:${t.id}` }))
    }
  ].filter(g => g.options.length)
)

const targetTip = computed(() => {
  if (!pageOptions.value.length && !categories.value.length && !tags.value.length) {
    return '暂无可选目标：请先在「页面管理」新建并发布页面，或先创建文章分类 / 标签'
  }
  return '选择后自动填入标签与路径（文章分类 → /category/别名、标签 → /tag/ID）；也可直接手填站内路径。二级菜单同样适用。'
})

/** 当前编辑的导航项是否指向自定义页面（供「页面」标记） */
const pageSlugOf = (row: SiteNavItem) =>
  row.linkType === 'url' ? '' : pageSlugFromPath(row.linkValue)

/** 分类 / 标签链接标记（后台表格一眼能看出二级菜单指向什么） */
const categoryKeyOf = (row: SiteNavItem) =>
  row.linkType === 'url' ? '' : /^\/category\/([^/?#]+)/.exec(row.linkValue || '')?.[1] || ''
const tagKeyOf = (row: SiteNavItem) =>
  row.linkType === 'url' ? '' : /^\/tag\/([^/?#]+)/.exec(row.linkValue || '')?.[1] || ''
const isCategoryLink = (row: SiteNavItem) => Boolean(categoryKeyOf(row))
const isTagLink = (row: SiteNavItem) => Boolean(tagKeyOf(row))

const findCategory = (key: string) =>
  categories.value.find(c => String(c.id) === key || (c.alias || '') === key)
const findTag = (key: string) => tags.value.find(t => String(t.id) === key)

const loadPublishedPages = async () => {
  try {
    const response = await listPage({ pageNum: 1, pageSize: 200, status: '1' })
    publishedPages.value = response?.rows || []
  } catch {
    publishedPages.value = []
  }
}

const loadLinkTargets = async () => {
  const [categoryResult, tagResult] = await Promise.allSettled([
    listCategory({ pageNum: 1, pageSize: 200 }),
    listTag({ pageNum: 1, pageSize: 200 })
  ])
  categories.value = categoryResult.status === 'fulfilled' ? categoryResult.value?.rows || [] : []
  tags.value = tagResult.status === 'fulfilled' ? tagResult.value?.rows || [] : []
}

/** 选中目标：自动填充标签（仅当标签为空）与链接值 */
const handleTargetSelect = (value: string) => {
  if (!value) return
  const sep = value.indexOf(':')
  const kind = value.slice(0, sep)
  const rest = value.slice(sep + 1)
  let label = ''
  let path = ''

  if (kind === 'fixed') {
    const target = FIXED_TARGETS.find(t => t.path === rest)
    label = target?.label || ''
    path = rest
  } else if (kind === 'page') {
    const page = pageOptions.value.find(p => p.slug === rest)
    if (!page) return
    label = page.title || page.slug || ''
    path = pagePath(rest)
  } else if (kind === 'cat') {
    const category = categories.value.find(c => String(c.id) === rest)
    if (!category) return
    label = category.name || ''
    path = `/category/${category.alias || category.id}`
  } else if (kind === 'tag') {
    const tag = tags.value.find(t => String(t.id) === rest)
    if (!tag) return
    label = tag.name || ''
    path = `/tag/${tag.id}`
  }
  if (!path) return
  if (!form.value.label?.trim()) form.value.label = label
  form.value.linkValue = path
}

/** 编辑回显：把已有链接值反解成下拉选中项；无法识别（手填路径）时留空 */
const targetOfRow = (row: SiteNavItem): string => {
  if (row.linkType === 'url') return ''
  const path = (row.linkValue || '').trim()
  const pageSlug = pageSlugFromPath(path)
  if (pageSlug) return `page:${pageSlug}`
  const category = findCategory(categoryKeyOf(row))
  if (category) return `cat:${category.id}`
  const tag = findTag(tagKeyOf(row))
  if (tag) return `tag:${tag.id}`
  const fixed = FIXED_TARGETS.find(t => t.path === path)
  return fixed ? `fixed:${fixed.path}` : ''
}

/* ---------- 「栏目名称」托管：/business 的标签由站点设置统一控制 ---------- */

/** 与前台 EnterpriseNav / EnterpriseFooter 同口径：这两处把顶层 /business 项的标签换成 business_label，兜底「核心业务」 */
const BUSINESS_LABEL_FALLBACK = '核心业务'

const isControlledLabel = (item: { linkValue?: string; parentId?: number }) =>
  (item.parentId ?? 0) === 0 && (item.linkValue || '').trim() === '/business'

const businessNavLabel = ref(BUSINESS_LABEL_FALLBACK)

const loadBusinessLabel = async () => {
  try {
    const response = await getConfigByKey('business_label')
    const value = (response as { data?: unknown })?.data
    businessNavLabel.value =
      (typeof value === 'string' ? value.trim() : '') || BUSINESS_LABEL_FALLBACK
  } catch {
    businessNavLabel.value = BUSINESS_LABEL_FALLBACK
  }
}

/** 表格展示生效标签：库里那份对 /business 项改了也不生效，摆在表里只会让人以为前台跟着变 */
const displayLabelOf = (row: SiteNavItem) =>
  isControlledLabel(row) ? businessNavLabel.value : row.label

const labelControlled = computed(() => isControlledLabel(form.value))

const labelControlTip = computed(
  () =>
    `该标签由「站点设置 → 栏目名称」统一控制（当前：${businessNavLabel.value}），在本页修改不会生效`
)

/** 链接值切到 /business（或栏目名称后到）时同步生效值，禁用输入框里得显示真相 */
watch([labelControlled, businessNavLabel], () => {
  if (labelControlled.value) form.value.label = businessNavLabel.value
})

onMounted(() => {
  loadTree()
  loadPublishedPages()
  loadLinkTargets()
  loadBusinessLabel()
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
  selectedTarget.value = ''
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
  selectedTarget.value = ''
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
  selectedTarget.value = targetOfRow(row)
  form.value = { ...row }
  // 连续编辑两个托管项时 labelControlled 不翻转，watch 不会触发，这里显式同步
  if (isControlledLabel(row)) form.value.label = businessNavLabel.value
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
      `确定删除导航项「${displayLabelOf(row)}」吗？${row.children?.length ? '其子项将一并删除。' : ''}`
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
