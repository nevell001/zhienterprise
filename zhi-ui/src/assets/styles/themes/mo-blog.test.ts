import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const stylesheet = readFileSync(resolve(__dirname, 'mo-blog.scss'), 'utf8')

describe('mo-blog theme stylesheet', () => {
  it('应该在 root 上提供博客设计 token，避免默认主题下页面变量失效', () => {
    expect(stylesheet).toContain(':root {')
    expect(stylesheet).toContain('--mo-p50: #eef2ff')
    expect(stylesheet).toContain('--mo-n900: #1c1917')
  })

  it('应该为博客首页和布局提供高优先级覆盖以压过 scoped 样式', () => {
    expect(stylesheet).toContain('.theme-mo-blog .site-layout .site-top-nav')
    expect(stylesheet).toContain('.theme-mo-blog .site-home-v2 .tag')
    expect(stylesheet).toContain(
      'html.dark.theme-mo-blog .mo-home-page .home-hero .search-box input'
    )
  })

  it('主色控件跟随「主题颜色」（--el-color-primary），不再钉死 mo 调色板', () => {
    // 实心主按钮：底色/边框取 --el-color-primary（由 handleThemeStyle 或模板映射注入），
    // 前景用对比度推导的 --mo-on-primary（亮主色自动转深墨，深主色用白）。
    expect(stylesheet).toContain('--el-button-bg-color: var(--el-color-primary);')
    expect(stylesheet).toContain('--el-button-text-color: var(--mo-on-primary, #fff);')
    expect(stylesheet).toContain('--el-button-hover-text-color: var(--mo-on-primary, #fff);')

    // plain 主按钮：底色/边框用主题色混合，hover 变实心主题色
    expect(stylesheet).toContain('.theme-mo-blog .el-button--primary.is-plain')
    expect(stylesheet).toContain('--el-button-hover-bg-color: var(--el-color-primary);')
    expect(stylesheet).toContain('--el-button-bg-color: color-mix(')

    // Tabs / 分页 / 侧栏菜单激活也跟随主题色
    expect(stylesheet).toContain('background-color: var(--el-color-primary);')
    expect(stylesheet).toContain('.el-pagination.is-background .el-pager li.is-active')
  })

  it('应该把管理后台框架和常用控件纳入当前主题', () => {
    expect(stylesheet).toContain('.theme-mo-blog .app-wrapper')
    expect(stylesheet).toContain('.theme-mo-blog .navbar')
    expect(stylesheet).toContain('.theme-mo-blog .tags-view-container')
    expect(stylesheet).toContain('.theme-mo-blog .sidebar-logo-container')
    expect(stylesheet).toContain('.theme-mo-blog .sidebar-container .el-menu-item.is-active')
    expect(stylesheet).toContain('.theme-mo-blog .app-container')
    expect(stylesheet).toContain('.theme-mo-blog .el-table')
    expect(stylesheet).toContain('.theme-mo-blog .el-form-item__label')
    expect(stylesheet).toContain('.theme-mo-blog .el-tag')
    expect(stylesheet).toContain('background: var(--mo-n50)')
  })

  it('主色填充控件应该提供配对的前景色（亮色主色下白字不达 AA）', () => {
    expect(stylesheet).toContain('html.theme-mo-blog {')
    expect(stylesheet).toContain('--mo-on-primary: #ffffff')
    // 后台表单标签在深色下不能用浅色主题的 --mo-n300（≈2:1）
    expect(stylesheet).toContain('html.dark.theme-mo-blog .el-form-item__label')
    expect(stylesheet).toContain('color: var(--el-text-color-regular)')
  })

  it('Mo-Blog 深色的次要文字色阶必须是浅灰', () => {
    // --mo-n300/n400 误映射成 #57534e/#78716c 会让全站文字对比度掉到 1.99~3.65:1
    expect(stylesheet).not.toContain('--mo-n300: #57534e')
    expect(stylesheet).not.toContain('--mo-n400: #78716c')
  })
})
