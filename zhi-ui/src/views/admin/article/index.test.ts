import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ArticleManage from './index.vue'

const sourcePath = resolve(process.cwd(), 'src/views/admin/article/index.vue')

describe('ArticleManage 视图组件测试', () => {
  it('应该导出 ArticleManage 组件', () => {
    expect(ArticleManage).toBeDefined()
    expect(typeof ArticleManage).toBe('object')
  })

  it('文章编辑器顶部工具栏应该绑定真实编辑器命令', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('ref="richEditorRef"')
    expect(source).toContain('@click="runEditorCommand(\'bold\')"')
    expect(source).toContain('@click="runEditorCommand(\'italic\')"')
    expect(source).toContain('@click="runEditorCommand(\'strike\')"')
    expect(source).toContain('@click="setEditorHeader(1)"')
    expect(source).toContain('@click="runEditorCommand(\'blockquote\')"')
    expect(source).toContain('@click="insertEditorLink"')
    expect(source).toContain('@click="insertEditorImage"')
  })

  it('文章编辑器草稿保存和视图切换不应该是静态文案', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('{{ editorSaveStatusText }}')
    expect(source).toContain('@click="submitForm(0)"')
    expect(source).toContain('@click="submitForm(1)"')
    expect(source).toContain("editorViewMode === 'split'")
    expect(source).toContain("setEditorViewMode('edit')")
    expect(source).toContain("setEditorViewMode('preview')")
    expect(source).not.toContain('草稿已保存 · 刚刚')
    expect(source).not.toContain('@click="form.status = 0"')
  })

  it('文章发布页应该使用紧凑的编辑工作区和右侧发布面板', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('width="min(1320px, 96vw)"')
    expect(source).toContain('top="4vh"')
    expect(source).toContain('class="editor-workspace"')
    expect(source).toContain('class="editor-main"')
    expect(source).toContain('class="editor-side-panel"')
    expect(source).toContain('class="side-actions"')
    expect(source).not.toContain('class="editor-sidebar"')
  })
})
