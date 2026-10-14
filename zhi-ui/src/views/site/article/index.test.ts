import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import Article from './detail.vue'

describe('Article 视图组件测试', () => {
  it('应该导出 Article 组件', () => {
    expect(Article).toBeDefined()
    expect(typeof Article).toBe('object')
  })
})
