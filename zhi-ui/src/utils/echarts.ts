import type { init as initEcharts } from 'echarts/core'

type EChartsModule = {
  init: typeof initEcharts
}
type EChartsImporter = () => Promise<EChartsModule>

export function createEchartsLoader(importEcharts: EChartsImporter) {
  let echartsPromise: Promise<EChartsModule> | null = null

  return () => {
    if (!echartsPromise) {
      echartsPromise = importEcharts()
    }
    return echartsPromise
  }
}

export const loadEcharts = createEchartsLoader(async () => {
  const { init } = await import('./echarts-runtime')
  return { init }
})

/**
 * 读取当前主题下的图表配色（跟随 html.dark 切换，浅色/深色自动适配）
 */
export function getChartThemeColors() {
  const rootStyle = getComputedStyle(document.documentElement)
  const cssVar = (name: string, fallback = '') =>
    rootStyle.getPropertyValue(name).trim() || fallback
  return {
    // 派生于「主题颜色 / 行业模板主色」：图表系列色必须跟随 --el-color-primary，
    // 不能在 option 里写死 #409EFF（改主题色后图表不换色）。
    primary: cssVar('--el-color-primary'),
    primaryLight: cssVar('--el-color-primary-light-5'),
    success: cssVar('--el-color-success', '#67c23a'),
    warning: cssVar('--el-color-warning', '#e6a23c'),
    danger: cssVar('--el-color-error', '#f56c6c'),
    textColor: cssVar('--el-text-color-primary', '#303133'),
    secondaryColor: cssVar('--el-text-color-secondary', '#909399'),
    borderColor: cssVar('--el-border-color', '#e4e7ed'),
    splitLineColor: cssVar('--el-border-color-lighter', '#ebeef5')
  }
}
