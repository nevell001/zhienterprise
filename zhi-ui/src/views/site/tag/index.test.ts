import { describe, it, expect } from 'vitest'

describe('Site Tag 组件测试', () => {
  it('应该导出 Tag 组件', async () => {
    const module = await import('./index.vue')
    expect(module.default).toBeDefined()
  })
})
