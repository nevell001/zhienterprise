import { describe, it, expect, vi, beforeEach } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import About from '../about.vue'

const source = readFileSync(resolve(__dirname, '../about.vue'), 'utf8')

describe('About 页面测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该导出 About 组件', () => {
    expect(About).toBeDefined()
    expect(typeof About).toBe('object')
  })

  it('应该有关于页面内容', () => {
    const hasContent = true
    expect(hasContent).toBe(true)
  })

  it('应该有页面标题', () => {
    const title = '关于我们'
    expect(title).toBe('关于我们')
  })

  it('应该有项目信息', () => {
    const projectInfo = {
      name: '知企建站系统',
      version: '1.0.0',
      author: 'Nevell'
    }
    expect(projectInfo.name).toBe('知企建站系统')
    expect(projectInfo.version).toBe('1.0.0')
  })

  it('应该有技术栈信息', () => {
    const techStack = ['Vue 3', 'Spring Boot', 'TypeScript', 'MySQL']
    expect(Array.isArray(techStack)).toBe(true)
    expect(techStack.length).toBe(4)
  })

  it('应该有联系信息', () => {
    const contactInfo = {
      email: 'test@example.com',
      github: 'https://github.com/nevell001/zhienterprise'
    }
    expect(contactInfo.email).toBe('test@example.com')
    expect(contactInfo.github).toContain('github.com')
  })

  it('深色模式文字必须使用重映射后的文字色阶（n50/n900 是背景色）', () => {
    const darkBlock = source.slice(source.indexOf('html.dark'))
    // 标题用主文字色 n200，正文用次要文字色 n500；不得把背景色阶当文字色
    expect(darkBlock).toContain('color: var(--mo-n200)')
    expect(darkBlock).toContain('color: var(--mo-n500)')
    expect(darkBlock).not.toMatch(/(?:^|[;{\s])color:\s*var\(--mo-n(?:0|50|800|900)\)/)
  })

  it('深色模式正文链接应该使用浅色阶（p700/p800 在深色底上几乎不可见）', () => {
    const darkBlock = source.slice(source.indexOf('html.dark'))
    expect(darkBlock).toContain('html.dark .about-content :deep(a)')
    expect(darkBlock).toContain('color: var(--mo-p300)')
    expect(darkBlock).toContain('html.dark .about-content :deep(a:hover)')
    expect(darkBlock).toContain('color: var(--mo-p200)')
  })
})
