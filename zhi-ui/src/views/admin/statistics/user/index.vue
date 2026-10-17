<template>
  <div class="app-container">
    <el-card header="用户统计">
      <el-row :gutter="20">
        <el-col :span="6">
          <StatCard label="总用户数" :value="userStats.totalCount || 0" />
        </el-col>
        <el-col :span="6">
          <StatCard label="活跃用户" :value="userStats.activeCount || 0" />
        </el-col>
        <el-col :span="6">
          <StatCard label="新增用户" :value="userStats.newCount || 0" />
        </el-col>
        <el-col :span="6">
          <StatCard label="管理员数" :value="userStats.adminCount || 0" />
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 30px">
        <el-col :span="12">
          <el-card header="用户注册趋势">
            <div v-if="registerReady" id="registerChart" style="height: 300px"></div>
            <el-empty v-else description="暂无注册趋势数据" :image-size="70" />
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card header="用户角色分布">
            <div v-if="roleReady" id="roleChart" style="height: 300px"></div>
            <el-empty v-else description="暂无角色分布数据" :image-size="70" />
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import StatCard from '@/components/StatCard.vue'
import { getUserStatistics, getUserRegisterTrend, getUserRoleDistribution } from '@/api/statistics'
import { loadEcharts, getChartThemeColors } from '@/utils/echarts'
import { useSettingsStore } from '@/stores/settings'
import { logger } from '@/utils/logger'

interface UserStats {
  totalCount?: number
  activeCount?: number
  newCount?: number
  adminCount?: number
}

const userStats = ref<UserStats>({})
const settingsStore = useSettingsStore()
const registerChart = ref<any>(null)
const roleChartRef = ref<any>(null)
const registerReady = ref(false)
const roleReady = ref(false)
let registerData: any = null
let roleData: any = null

const loadData = async () => {
  try {
    const res = await getUserStatistics()
    if (res.code === 200) {
      userStats.value = res.data
      // 加载图表数据
      await loadChartData()
    }
  } catch (error) {
    logger.error('获取用户统计失败:', error)
  }
}

const loadChartData = async () => {
  try {
    // 加载用户注册趋势
    const registerRes = await getUserRegisterTrend()
    if (registerRes.code === 200) {
      // 先置 ready 再渲染：容器在 v-if 内，否则 render 时取不到 DOM，图表永远是空白
      registerReady.value = true
      await renderRegisterChart(registerRes.data || { labels: [], data: [] })
    } else {
      registerReady.value = false
      logger.error('用户注册趋势API返回错误:', registerRes)
    }

    // 加载用户角色分布
    const roleRes = await getUserRoleDistribution()
    if (roleRes.code === 200) {
      roleReady.value = true
      await renderRoleChart(roleRes.data || { labels: [], data: [] })
    } else {
      roleReady.value = false
      logger.error('用户角色分布API返回错误:', roleRes)
    }
  } catch (error) {
    logger.error('加载图表数据失败:', error)
  }
}

const renderRegisterChart = async data => {
  registerData = data
  await nextTick()
  const chartElement = document.getElementById('registerChart')
  if (!chartElement) return

  const echarts = await loadEcharts()
  if (!registerChart.value) {
    registerChart.value = echarts.init(chartElement)
  }
  const colors = getChartThemeColors()
  const option = {
    tooltip: {
      trigger: 'axis',
      textStyle: { color: colors.textColor }
    },
    xAxis: {
      type: 'category',
      data: data && data.labels ? data.labels : [],
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
        data: data && data.data ? data.data : [],
        type: 'line',
        smooth: true,
        itemStyle: {
          color: colors.warning
        }
      }
    ]
  }
  registerChart.value.setOption(option)
}

const renderRoleChart = async data => {
  roleData = data
  await nextTick()
  const chartElement = document.getElementById('roleChart')
  if (!chartElement) return

  const echarts = await loadEcharts()
  if (!roleChartRef.value) {
    roleChartRef.value = echarts.init(chartElement)
  }
  const colors = getChartThemeColors()

  // 处理数据
  let chartData = []
  if (data && data.labels && data.data) {
    chartData = data.labels.map((label, index) => ({
      value: data.data[index],
      name: label
    }))
  }

  const option = {
    tooltip: {
      trigger: 'item',
      textStyle: { color: colors.textColor }
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: colors.secondaryColor }
    },
    series: [
      {
        name: '用户角色',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: chartData
      }
    ]
  }
  roleChartRef.value.setOption(option)
}

// 深色模式切换时用最新主题色重绘图表
watch(
  () => settingsStore.isDark,
  () => {
    if (registerData) renderRegisterChart(registerData)
    if (roleData) renderRoleChart(roleData)
  }
)

onMounted(() => {
  loadData()
})
</script>
