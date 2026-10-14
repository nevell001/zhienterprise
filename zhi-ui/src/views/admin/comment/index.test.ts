import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import CommentManage from './index.vue'

describe('CommentManage 视图组件测试', () => {
  it('应该导出 CommentManage 组件', () => {
    expect(CommentManage).toBeDefined()
    expect(typeof CommentManage).toBe('object')
  })
})
