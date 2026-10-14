<template>
  <div class="app-container">
    <!-- 汇总卡片 -->
    <el-card class="summary-card">
      <template #header>
        <div class="summary-header">
          <span>访问汇总</span>
          <el-radio-group v-model="summaryDays" size="small" @change="loadSummary">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button :value="90">近 90 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-row v-loading="summaryLoading" :gutter="20">
        <el-col :xs="12" :sm="12" :md="6">
          <StatCard
            :label="`区间 PV（${summary.beginDate || '-'} ~ ${summary.endDate || '-'}）`"
            :value="summary.pv"
          />
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <StatCard label="区间 UV" :value="summary.uv" />
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <StatCard label="今日 PV" :value="summary.todayPv" />
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <StatCard label="今日 UV" :value="summary.todayUv" />
        </el-col>
      </el-row>
    </el-card>

    <!-- 访问排行 -->
    <el-card class="rank-card" header="访问排行（点击行可按目标筛选明细）">
      <el-table
        v-loading="summaryLoading"
        :data="topTargets"
        size="small"
        stripe
        border
        empty-text="暂无访问排行数据"
        @row-click="handleRankRowClick"
      >
        <el-table-column label="标题" prop="title" min-width="200" :show-overflow-tooltip="true">
          <template #default="scope">
            <span>{{ scope.row.title || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="targetType" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.targetType === 'page' ? 'success' : 'primary'" size="small">
              {{ targetTypeLabel(scope.row.targetType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="PV" prop="pv" width="90" align="center" />
        <el-table-column label="UV" prop="uv" width="90" align="center" />
      </el-table>
    </el-card>

    <!-- 筛选区 -->
    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="80px"
      class="search-form"
    >
      <el-form-item label="访问日期" prop="dateRange">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="query-date"
        />
      </el-form-item>
      <el-form-item label="目标类型" prop="targetType">
        <el-select
          v-model="queryParams.targetType"
          placeholder="全部"
          clearable
          class="query-input"
        >
          <el-option label="文章" value="article" />
          <el-option label="自定义页面" value="page" />
        </el-select>
      </el-form-item>
      <el-form-item label="IP" prop="ip">
        <el-input
          v-model="queryParams.ip"
          placeholder="请输入访客 IP"
          clearable
          :maxlength="64"
          class="query-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="关键字" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="匹配标题或路径"
          clearable
          :maxlength="100"
          class="query-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="独立访客" prop="uniqueOnly">
        <el-switch v-model="queryParams.uniqueOnly" active-text="只看独立访客" inline-prompt />
      </el-form-item>
      <el-form-item label="">
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :xs="8" :sm="6" :md="4" :lg="3" :xl="1.5">
        <el-button
          v-hasPermi="['statistics:visit:export']"
          type="warning"
          plain
          icon="Download"
          class="btn-full-width"
          @click="handleExport"
        >
          导出
        </el-button>
      </el-col>
      <el-col :xs="8" :sm="6" :md="4" :lg="3" :xl="1.5">
        <el-button
          v-hasPermi="['statistics:visit:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          class="btn-full-width"
          @click="handleDelete()"
        >
          删除
        </el-button>
      </el-col>
      <el-col :xs="16" :sm="12" :md="10" :lg="8" :xl="5">
        <div class="clean-bar">
          <span class="clean-label">保留最近</span>
          <el-input-number
            v-model="retentionDays"
            :min="1"
            :max="3650"
            controls-position="right"
            class="clean-days"
          />
          <span class="clean-label">天</span>
          <el-button
            v-hasPermi="['statistics:visit:remove']"
            type="danger"
            plain
            icon="Delete"
            @click="handleClean"
          >
            清理
          </el-button>
        </div>
      </el-col>
      <right-toolbar
        v-model:show-search="showSearch"
        class="top-right-btn"
        @query-table="getList"
      />
    </el-row>

    <!-- 明细表格 -->
    <el-table
      v-loading="loading"
      :data="visitList"
      stripe
      border
      empty-text="暂无访问明细"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="访问时间" prop="createTime" width="160" align="center">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="targetType" width="110" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.targetType === 'page' ? 'success' : 'primary'" size="small">
            {{ targetTypeLabel(scope.row.targetType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="标题"
        prop="targetTitle"
        min-width="180"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <span>{{ scope.row.targetTitle || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="路径" prop="path" min-width="180" :show-overflow-tooltip="true">
        <template #default="scope">
          <router-link
            v-if="scope.row.path"
            :to="scope.row.path"
            target="_blank"
            class="visit-path"
          >
            {{ scope.row.path }}
          </router-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
        label="IP"
        prop="ip"
        width="140"
        align="center"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <span>{{ scope.row.ip || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="独立访客" prop="isUnique" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.isUnique === '1' ? 'success' : 'info'" size="small">
            {{ scope.row.isUnique === '1' ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" prop="referer" min-width="160" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.referer || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="UA" prop="userAgent" min-width="160" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.userAgent || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        width="90"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-tooltip content="删除" placement="top">
            <el-button
              v-hasPermi="['statistics:visit:remove']"
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
  </div>
</template>

<script setup lang="ts" name="VisitLog">
import StatCard from '@/components/StatCard.vue'
import { parseTime } from '@/utils/zhi'
import { getVisitLogList, getVisitSummary, cleanVisitLog, delVisitLog } from '@/api/statistics'
import type { SiteVisitLog, VisitQueryParams, VisitSummary, VisitTopTarget } from '@/api/statistics'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const summaryLoading = ref(false)
const showSearch = ref(true)
const visitList = ref<SiteVisitLog[]>([])
const topTargets = ref<VisitTopTarget[]>([])
const total = ref(0)
const summaryDays = ref(30)
const dateRange = ref<string[]>([])
const retentionDays = ref(90)
const ids = ref<number[]>([])
const multiple = ref(true)

const queryParams = reactive<VisitQueryParams>({
  pageNum: 1,
  pageSize: 10,
  targetType: undefined,
  targetId: undefined,
  ip: undefined,
  visitorKey: undefined,
  uniqueOnly: false,
  keyword: undefined
})

const summary = ref<VisitSummary>({
  beginDate: '',
  endDate: '',
  days: 30,
  pv: 0,
  uv: 0,
  todayPv: 0,
  todayUv: 0,
  topTargets: []
})

/** 目标类型文案 */
function targetTypeLabel(targetType?: string): string {
  if (targetType === 'article') return '文章'
  if (targetType === 'page') return '自定义页面'
  return '-'
}

/** 组装查询参数（日期范围转 beginDate/endDate） */
function buildQueryParams(): VisitQueryParams {
  const [beginDate, endDate] = dateRange.value || []
  return {
    ...queryParams,
    beginDate: beginDate || undefined,
    endDate: endDate || undefined
  }
}

/** 查询访问明细列表 */
async function getList() {
  loading.value = true
  try {
    const res = await getVisitLogList(buildQueryParams())
    visitList.value = res?.rows || []
    total.value = res?.total || 0
  } catch (error) {
    // 接口失败保持空态，不使用假数据兜底
    visitList.value = []
    total.value = 0
    proxy.$modal.msgError('获取访问明细失败')
  } finally {
    loading.value = false
  }
}

/** 查询访问汇总与排行 */
async function loadSummary() {
  summaryLoading.value = true
  try {
    const res = await getVisitSummary(summaryDays.value)
    const data = res?.data
    summary.value = {
      beginDate: data?.beginDate || '',
      endDate: data?.endDate || '',
      days: data?.days ?? summaryDays.value,
      pv: data?.pv ?? 0,
      uv: data?.uv ?? 0,
      todayPv: data?.todayPv ?? 0,
      todayUv: data?.todayUv ?? 0,
      topTargets: data?.topTargets || []
    }
    topTargets.value = data?.topTargets || []
  } catch (error) {
    // 接口失败保持空态，不使用假数据兜底
    summary.value = {
      beginDate: '',
      endDate: '',
      days: summaryDays.value,
      pv: 0,
      uv: 0,
      todayPv: 0,
      todayUv: 0,
      topTargets: []
    }
    topTargets.value = []
    proxy.$modal.msgError('获取访问汇总失败')
  } finally {
    summaryLoading.value = false
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  if (proxy && proxy.resetForm) {
    proxy.resetForm('queryRef')
  }
  dateRange.value = []
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.targetType = undefined
  queryParams.targetId = undefined
  queryParams.ip = undefined
  queryParams.visitorKey = undefined
  queryParams.uniqueOnly = false
  queryParams.keyword = undefined
  handleQuery()
}

/** 点击访问排行行，按目标筛选明细 */
function handleRankRowClick(row: VisitTopTarget) {
  if (!row) return
  queryParams.targetType = row.targetType
  queryParams.targetId = row.targetId
  queryParams.pageNum = 1
  getList()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: SiteVisitLog[]) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

/** 导出按钮操作 */
function handleExport() {
  if (proxy && proxy.download) {
    proxy.download(
      'statistics/visit/export',
      { ...buildQueryParams() },
      `visitLog_${new Date().getTime()}.xlsx`
    )
  }
}

/** 清理按钮操作（保留最近 N 天） */
function handleClean() {
  const days = retentionDays.value
  proxy.$modal
    .confirm(`是否确认清理 ${days} 天前的访问明细？`)
    .then(() => cleanVisitLog(days))
    .then(res => {
      proxy.$modal.msgSuccess(res?.msg || '清理成功')
      handleQuery()
      loadSummary()
    })
    .catch(error => {
      if (error !== 'cancel') {
        proxy.$modal.msgError('清理访问明细失败')
      }
    })
}

/** 删除按钮操作 */
function handleDelete(row?: SiteVisitLog) {
  const deleteIds = row ? row.id : ids.value.join(',')
  proxy.$modal
    .confirm('是否确认删除访问明细编号为"' + deleteIds + '"的数据项？')
    .then(() => delVisitLog(deleteIds))
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(error => {
      if (error !== 'cancel') {
        proxy.$modal.msgError('删除访问明细失败')
      }
    })
}

onMounted(() => {
  getList()
  loadSummary()
})
</script>

<style scoped>
.summary-card {
  margin-bottom: 12px;
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.rank-card {
  margin-bottom: 12px;
}

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

.query-input {
  width: 180px;
}

.query-date {
  width: 260px;
}

.clean-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.clean-label {
  font-size: 13px;
  color: var(--el-text-color-regular, #606266);
}

.clean-days {
  width: 110px;
}

.visit-path {
  color: var(--el-color-primary);
  text-decoration: none;
}

.visit-path:hover {
  text-decoration: underline;
}

.mb8 {
  margin-bottom: 12px;
}

.top-right-btn {
  margin-left: auto;
}

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

  .query-input,
  .query-date {
    width: 100%;
  }

  .btn-full-width {
    width: 100%;
    margin-bottom: 8px;
  }
}
</style>
