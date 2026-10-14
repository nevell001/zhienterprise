import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import FriendLinkManage from './index.vue'

describe('FriendLinkManage 视图组件测试', () => {
  it('应该导出 FriendLinkManage 组件', () => {
    expect(FriendLinkManage).toBeDefined()
    expect(typeof FriendLinkManage).toBe('object')
  })
})
