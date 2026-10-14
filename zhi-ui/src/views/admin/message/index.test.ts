import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import MessageManage from './index.vue'

const sourcePath = resolve(process.cwd(), 'src/views/admin/message/index.vue')

describe('MessageManage 视图组件测试', () => {
  it('应该导出 MessageManage 组件', () => {
    expect(MessageManage).toBeDefined()
    expect(typeof MessageManage).toBe('object')
  })

  it('应该注册 keep-alive 所需的组件名', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('name="SiteMessage"')
  })

  it('应该包含留言管理所需的权限标识', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('blog:message:edit')
    expect(source).toContain('blog:message:reply')
    expect(source).toContain('blog:message:remove')
    expect(source).toContain('blog:message:export')
  })

  it('应该提供昵称、内容、状态筛选条件', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('placeholder="请输入留言昵称"')
    expect(source).toContain('placeholder="请输入留言内容"')
    expect(source).toContain('<el-option label="待审核" value="0" />')
    expect(source).toContain('<el-option label="已发布" value="1" />')
    expect(source).toContain('<el-option label="已拒绝" value="2" />')
  })

  it('应该使用 el-tag 展示留言状态并展示回复内容', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('statusTagType(scope.row.status)')
    expect(source).toContain("if (status === '2') return 'danger'")
    expect(source).toContain('scope.row.replyContent')
  })

  it('应该提供回复弹窗且回复内容不超过500字', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('replyMessage(replyForm.value.id, replyForm.value.replyContent)')
    expect(source).toContain(':maxlength="500"')
    expect(source).toContain('replyRef')
  })
})
