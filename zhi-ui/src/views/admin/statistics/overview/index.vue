<template>
  <div class="app-container">
    <el-alert
      v-if="loadError"
      title="统计数据暂时无法获取，请稍后重试或检查服务状态"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <svg-icon icon-class="documentation" />
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ stats.articleCount || 0 }}
              </div>
              <div class="stat-label">文章总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <svg-icon icon-class="user" />
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ stats.userCount || 0 }}
              </div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <svg-icon icon-class="message" />
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ stats.commentCount || 0 }}
              </div>
              <div class="stat-label">评论总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">
              <svg-icon icon-class="eye" />
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ stats.viewCount || 0 }}
              </div>
              <div class="stat-label">总浏览量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card header="文章发布趋势">
          <div v-if="articleReady" id="articleChart" style="height: 300px"></div>
          <el-empty v-else-if="!loading" description="暂无趋势数据" :image-size="70" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="用户活跃度">
          <div v-if="userReady" id="userChart" style="height: 300px"></div>
          <el-empty v-else-if="!loading" description="暂无活跃数据" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import { getStatisticsOverview, getArticleTrend, getUserActivity } from '@/api/statistics'
import { loadEcharts, getChartThemeColors } from '@/utils/echarts'
import { useSettingsStore } from '@/stores/settings'
import { logger } from '@/utils/logger'

interface OverviewStats {
  articleCount?: number
  userCount?: number
  commentCount?: number
  viewCount?: number
}

const stats = ref<OverviewStats>({})
const settingsStore = useSettingsStore()
const articleChart = ref<any>(null)
const userChartRef = ref<any>(null)
let articleTrendData: any = null
let userActivityData: any = null

const loading = ref(true)
const loadError = ref(false)
const articleReady = ref(false)
const userReady = ref(false)

const loadData = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await getStatisticsOverview()
    if (res.code === 200 && res.data) {
      stats.value = res.data
      await loadChartData()
    } else {
      // 非 200 响应：不展示模拟数据
      stats.value = {}
      loadError.value = true
    }
  } catch (error) {
    logger.error('获取统计数据失败:', error)
    stats.value = {}
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const loadChartData = async () => {
  // 加载文章发布趋势
  try {
    const articleTrendRes = await getArticleTrend()
    if (articleTrendRes.code === 200) {
      articleReady.value = true
      await renderArticleChart(articleTrendRes.data || { labels: [], data: [] })
    } else {
      articleReady.value = false
    }
  } catch (error) {
    logger.warn('文章趋势数据加载失败:', error)
    articleReady.value = false
  }

  // 加载用户活跃度
  try {
    const userActivityRes = await getUserActivity()
    if (userActivityRes.code === 200) {
      userReady.value = true
      await renderUserChart(userActivityRes.data || { labels: [], data: [] })
    } else {
      userReady.value = false
    }
  } catch (error) {
    logger.warn('用户活跃度数据加载失败:', error)
    userReady.value = false
  }
}

const renderArticleChart = async data => {
  articleTrendData = data
  await nextTick()
  const chartElement = document.getElementById('articleChart')
  if (!chartElement) return

  const echarts = await loadEcharts()
  if (!articleChart.value) {
    articleChart.value = echarts.init(chartElement)
  }
  const colors = getChartThemeColors()
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: data.labels || [],
      axisLabel: { color: colors.secondaryColor },
      axisLine: { lineStyle: { color: colors.borderColor } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: colors.secondaryColor },
      splitLine: { lineStyle: { color: colors.splitLineColor } }
    },
    series: [
      {
        data: data.data || [],
        type: 'line',
        smooth: true,
        itemStyle: {
          color: colors.primary
        }
      }
    ]
  }
  articleChart.value.setOption(option)
}

const renderUserChart = async data => {
  userActivityData = data
  await nextTick()
  const chartElement = document.getElementById('userChart')
  if (!chartElement) return

  const echarts = await loadEcharts()
  if (!userChartRef.value) {
    userChartRef.value = echarts.init(chartElement)
  }
  const colors = getChartThemeColors()
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: data.labels || [],
      axisLabel: { color: colors.secondaryColor },
      axisLine: { lineStyle: { color: colors.borderColor } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: colors.secondaryColor },
      splitLine: { lineStyle: { color: colors.splitLineColor } }
    },
    series: [
      {
        data: data.data || [],
        type: 'bar',
        itemStyle: {
          color: colors.success
        }
      }
    ]
  }
  userChartRef.value.setOption(option)
}

// 深色模式切换时用最新主题色重绘图表
watch(
  () => settingsStore.isDark,
  () => {
    if (articleTrendData) renderArticleChart(articleTrendData)
    if (userActivityData) renderUserChart(userActivityData)
  }
)

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  font-size: 48px;
  color: var(--el-color-primary);
  margin-right: 20px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 5px;
}
</style>
