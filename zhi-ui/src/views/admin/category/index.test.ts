import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import CategoryManage from './index.vue'

describe('CategoryManage 视图组件测试', () => {
  it('应该导出 CategoryManage 组件', () => {
    expect(CategoryManage).toBeDefined()
    expect(typeof CategoryManage).toBe('object')
  })
})
