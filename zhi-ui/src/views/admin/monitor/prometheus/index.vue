<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>Prometheus 监控指标</span>
        </div>
      </template>
      <div v-loading="loading">
        <el-alert
          title="提示"
          type="info"
          description="Prometheus 指标数据以纯文本格式显示，建议直接访问 Prometheus 服务查看可视化界面"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header-item">
                  <el-icon><DataLine /></el-icon>
                  <span>指标数据（原始格式）</span>
                </div>
              </template>
              <div class="metric-content">
                <el-button type="primary" style="width: 100%" @click="viewMetrics">
                  查看指标数据
                </el-button>
                <p class="metric-url">
                  {{ metricsUrl }}
                </p>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header-item">
                  <el-icon><TrendCharts /></el-icon>
                  <span>Prometheus 服务</span>
                </div>
              </template>
              <div class="metric-content">
                <el-button type="success" style="width: 100%" @click="openPrometheus">
                  访问 Prometheus UI
                </el-button>
                <p class="metric-url">
                  {{
                    prometheusUrl ||
                    '未配置 Prometheus 地址（可在 站点设置 → 站点信息 → 监控入口 填写）'
                  }}
                </p>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <div class="card-header-item">
              <el-icon><InfoFilled /></el-icon>
              <span>常用指标查询示例</span>
            </div>
          </template>
          <div class="metric-examples">
            <el-table :data="metricExamples" style="width: 100%">
              <el-table-column prop="name" label="指标名称" width="250" />
              <el-table-column prop="query" label="PromQL 查询" />
              <el-table-column label="操作" width="150">
                <template #default="scope">
                  <el-button size="small" type="primary" @click="runQuery(scope.row.query)">
                    在 Prometheus 中运行
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </div>
    </el-card>

    <!-- 指标数据弹窗 -->
    <el-dialog
      v-model="metricsDialogVisible"
      title="Prometheus 指标数据"
      width="80%"
      top="5vh"
      destroy-on-close
    >
      <div v-loading="metricsLoading">
        <div v-if="metricsData" class="metrics-content">
          <el-button type="primary" size="small" style="margin-bottom: 10px" @click="copyMetrics">
            <el-icon><CopyDocument /></el-icon>
            复制数据
          </el-button>
          <el-button
            type="success"
            size="small"
            style="margin-bottom: 10px"
            @click="openMetricsInNewWindow"
          >
            <el-icon><View /></el-icon>
            在新窗口打开
          </el-button>
          <pre class="metrics-display">{{ metricsData }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-service'
import { loadMonitorUrl } from '@/utils/monitorConfig'
import { DataLine, TrendCharts, InfoFilled, CopyDocument, View } from '@element-plus/icons-vue'

const loading = ref(false)
const metricsBaseUrl = ref('/manage/actuator')
const metricsUrl = computed(() => `${metricsBaseUrl.value}/prometheus`)
// Prometheus UI 地址：后台设置 → 环境变量 → 按站点访问地址推导
const prometheusUrl = ref('')

onMounted(async () => {
  const [prometheus, actuator] = await Promise.all([
    loadMonitorUrl('prometheus'),
    loadMonitorUrl('actuator')
  ])
  prometheusUrl.value = prometheus
  metricsBaseUrl.value = actuator
})

// 指标数据弹窗相关
const metricsDialogVisible = ref(false)
const metricsLoading = ref(false)
const metricsData = ref('')

const metricExamples = ref([
  {
    name: 'JVM 内存使用',
    query: 'jvm_memory_used_bytes{area="heap"}'
  },
  {
    name: 'HTTP 请求总数',
    query: 'http_server_requests_seconds_count'
  },
  {
    name: 'HTTP 请求平均响应时间',
    query:
      'rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])'
  },
  {
    name: '系统 CPU 使用率',
    query: 'system_cpu_usage'
  },
  {
    name: 'Tomcat 线程池活跃线程',
    query: 'tomcat_threads_busy_threads'
  }
])

// 查看指标数据
const viewMetrics = async () => {
  metricsDialogVisible.value = true
  metricsLoading.value = true
  metricsData.value = ''

  try {
    const response = await fetch(metricsUrl.value)
    if (!response.ok) {
      throw new Error('获取指标数据失败')
    }
    const data = await response.text()
    metricsData.value = data
  } catch (error: any) {
    console.error('获取指标数据失败:', error)
    ElMessage.error('获取指标数据失败: ' + error.message)
    metricsData.value = '错误: ' + error.message
  } finally {
    metricsLoading.value = false
  }
}

// 打开指标数据（新窗口）
const openMetricsInNewWindow = () => {
  // metricsUrl 已是同源相对路径（dev 走 Vite 代理，prod 走 Nginx），直接打开即可
  window.open(metricsUrl.value, '_blank')
}

// 复制指标数据
const copyMetrics = async () => {
  try {
    await navigator.clipboard.writeText(metricsData.value)
    ElMessage.success('已复制到剪贴板')
  } catch (error: any) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 打开 Prometheus UI
const openPrometheus = () => {
  if (!prometheusUrl.value) {
    ElMessage.warning('未配置 Prometheus 地址，请到 站点设置 → 站点信息 → 监控入口 填写')
    return
  }
  window.open(prometheusUrl.value, '_blank')
}

// 在 Prometheus 中运行查询
const runQuery = query => {
  if (!prometheusUrl.value) {
    ElMessage.warning('未配置 Prometheus 地址，请到 站点设置 → 站点信息 → 监控入口 填写')
    return
  }
  const encodedQuery = encodeURIComponent(query)
  window.open(`${prometheusUrl.value}/graph?g0.expr=${encodedQuery}&g0.tab=0`, '_blank')
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.card-header-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.metric-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  padding: 20px 0;
}

.metric-url {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  word-break: break-all;
  margin: 0;
  padding: 8px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  width: 100%;
  text-align: center;
}

.metric-examples {
  padding: 10px 0;
}

.metrics-content {
  margin-top: 10px;
}

.metrics-display {
  background-color: var(--el-fill-color-light);
  padding: 15px;
  border-radius: 4px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  overflow: auto;
  max-height: 70vh;
}
</style>
