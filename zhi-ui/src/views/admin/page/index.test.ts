import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import PageManage from './index.vue'

const sourcePath = resolve(process.cwd(), 'src/views/admin/page/index.vue')

describe('PageManage 视图组件测试', () => {
  it('应该导出 PageManage 组件', () => {
    expect(PageManage).toBeDefined()
    expect(typeof PageManage).toBe('object')
  })

  it('应该注册 keep-alive 所需的组件名', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('name="BlogPage"')
  })

  it('应该包含页面管理所需的权限标识', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('blog:page:add')
    expect(source).toContain('blog:page:edit')
    expect(source).toContain('blog:page:remove')
    expect(source).toContain('blog:page:export')
  })

  it('应该提供标题、别名、状态筛选条件', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('placeholder="请输入页面标题"')
    expect(source).toContain('placeholder="请输入页面别名"')
    expect(source).toContain('<el-option label="草稿" value="0" />')
    expect(source).toContain('<el-option label="已发布" value="1" />')
  })

  it('应该使用 el-tag 展示状态与导航显示', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('statusTagType(scope.row.status)')
    expect(source).toContain('scope.row.showInNav')
    expect(source).toContain('navText(scope.row.showInNav)')
    expect(source).toContain('prop="sort"')
    expect(source).toContain('prop="viewCount"')
    expect(source).toContain('parseTime(scope.row.updateTime || scope.row.createTime)')
  })

  it('应该提供别名校验提示、Markdown 文本域与 SEO 表单项', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('SLUG_PATTERN')
    expect(source).toContain('/^[A-Za-z0-9_-]{1,100}$/')
    expect(source).toContain('别名只允许字母、数字、下划线、连字符')
    expect(source).toContain('placeholder="请输入 Markdown 内容"')
    expect(source).toContain('prop="seoTitle"')
    expect(source).toContain('prop="seoKeywords"')
    expect(source).toContain('prop="seoDescription"')
    expect(source).toContain('changePageStatus(row.id, newStatus)')
  })
})
