<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>Grafana 监控</span>
        </div>
      </template>
      <div>
        <el-alert
          title="提示"
          type="info"
          description="Grafana 是一个开源的分析和可视化平台，用于查看和监控应用程序的指标数据"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header-item">
                  <el-icon><DataBoard /></el-icon>
                  <span>Grafana 仪表板</span>
                </div>
              </template>
              <div class="grafana-content">
                <el-button type="primary" style="width: 100%" @click="openGrafana">
                  打开 Grafana 界面
                </el-button>
                <p class="grafana-url">
                  {{
                    grafanaUrl || '未配置 Grafana 地址（可在 站点设置 → 站点信息 → 监控入口 填写）'
                  }}
                </p>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header-item">
                  <el-icon><User /></el-icon>
                  <span>登录信息</span>
                </div>
              </template>
              <div class="login-info">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="用户名">admin</el-descriptions-item>
                  <el-descriptions-item label="密码">
                    请查看 .env 文件中的 GF_SECURITY_ADMIN_PASSWORD 变量
                  </el-descriptions-item>
                  <el-descriptions-item label="端口">{{ grafanaPort }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <div class="card-header-item">
              <el-icon><InfoFilled /></el-icon>
              <span>使用说明</span>
            </div>
          </template>
          <div class="usage-info">
            <el-timeline>
              <el-timeline-item timestamp="第一步" placement="top">
                <el-card>
                  <h4>打开 Grafana 界面</h4>
                  <p>点击"打开 Grafana 界面"按钮，访问 Grafana 监控平台</p>
                </el-card>
              </el-timeline-item>
              <el-timeline-item timestamp="第二步" placement="top">
                <el-card>
                  <h4>登录系统</h4>
                  <p>
                    使用默认账号
                    <strong>admin / 请查看 .env 文件中的 GF_SECURITY_ADMIN_PASSWORD 变量</strong>
                    登录
                  </p>
                </el-card>
              </el-timeline-item>
              <el-timeline-item timestamp="第三步" placement="top">
                <el-card>
                  <h4>添加数据源</h4>
                  <p>在 Grafana 中添加 Prometheus 作为数据源（如果尚未配置）</p>
                  <el-alert
                    title="Prometheus 地址"
                    type="success"
                    :closable="false"
                    style="margin-top: 10px"
                  >
                    http://prometheus:9090（Grafana 容器内按 Compose
                    服务名访问，与浏览器访问地址无关）
                  </el-alert>
                </el-card>
              </el-timeline-item>
              <el-timeline-item timestamp="第四步" placement="top">
                <el-card>
                  <h4>查看仪表板</h4>
                  <p>浏览和创建各种监控仪表板，查看应用程序的实时指标</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { DataBoard, User, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from '@/plugins/element-plus-service'
import { loadMonitorUrl } from '@/utils/monitorConfig'
import { monitorUrlPort } from '@/utils/monitorUrl'

// Grafana 地址：后台设置 → 构建期环境变量 → 按站点访问地址推导
const grafanaUrl = ref('')
const grafanaPort = computed(() =>
  grafanaUrl.value ? monitorUrlPort(grafanaUrl.value, 'grafana') : '—'
)

onMounted(async () => {
  grafanaUrl.value = await loadMonitorUrl('grafana')
})

// 打开 Grafana 界面
const openGrafana = () => {
  if (!grafanaUrl.value) {
    ElMessage.warning('未配置 Grafana 地址，请到 站点设置 → 站点信息 → 监控入口 填写')
    return
  }
  window.open(grafanaUrl.value, '_blank')
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

.grafana-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  padding: 20px 0;
}

.grafana-url {
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

.login-info {
  padding: 10px 0;
}

.usage-info {
  padding: 10px 0;
}

.usage-info h4 {
  margin: 0 0 10px 0;
  color: var(--el-text-color-primary);
}

.usage-info p {
  margin: 0;
  color: var(--el-text-color-regular);
}
</style>
