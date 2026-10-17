/**
 * 侧栏主题常量（原 variables.module.scss 的 :export 值）。
 *
 * 为什么不用 `import variables from '@/assets/styles/variables.module.scss'`：
 * 那会把整个变量文件当 **CSS Module** 注入一份哈希类名副本（`html:not(._dark_xxx)`），
 * 暗色模式下浅色变量会覆盖暗色变量。样式统一走 variables.scss 的 CSS 变量，
 * 这里只保留侧栏组件需要的少数字面量（全局只有一套，改动时同步 _vars.scss）。
 */
export const SIDEBAR_THEME = {
  /** 暗色侧栏背景 */
  menuBg: '#112240',
  /** 暗色侧栏文字 */
  menuText: '#bfcbd9',
  /** 浅色侧栏背景 */
  menuLightBg: '#ffffff',
  /** 浅色侧栏文字 */
  menuLightText: '#303133'
} as const
