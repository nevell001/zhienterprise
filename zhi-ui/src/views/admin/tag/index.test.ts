import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import TagManage from './index.vue'

describe('TagManage 视图组件测试', () => {
  it('应该导出 TagManage 组件', () => {
    expect(TagManage).toBeDefined()
    expect(typeof TagManage).toBe('object')
  })
})
