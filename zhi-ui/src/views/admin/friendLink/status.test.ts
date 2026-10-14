import { describe, expect, it } from 'vitest'
import { FRIEND_LINK_STATUS, auditStatusFor } from './status'

describe('友链审核状态映射', () => {
  it('通过应落库为 0（前台列表只展示 status=0）', () => {
    expect(auditStatusFor('approve')).toBe('0')
    expect(auditStatusFor('approve')).toBe(FRIEND_LINK_STATUS.NORMAL)
  })

  it('拒绝应落库为 1（停用，不出现在前台）', () => {
    expect(auditStatusFor('reject')).toBe('1')
    expect(auditStatusFor('reject')).toBe(FRIEND_LINK_STATUS.DISABLED)
  })

  it('待审核语义应为 2，与前台申请写入一致', () => {
    expect(FRIEND_LINK_STATUS.PENDING).toBe('2')
  })

  // 回归护栏：后端 BlogFriendLink.status 是 String，接口返回 "0"/"1"。
  // el-switch 用严格 includes([activeValue, inactiveValue], row.status) 判断初值合法性，
  // 若这里是数字而 status 是字符串，每个开关渲染即误触发 change → 进页面刷一串"状态更新成功"。
  it('状态值必须是字符串，且能被后端返回的字符串 status 严格命中', () => {
    expect(typeof FRIEND_LINK_STATUS.NORMAL).toBe('string')
    expect(typeof FRIEND_LINK_STATUS.DISABLED).toBe('string')
    const switchValues = [FRIEND_LINK_STATUS.NORMAL, FRIEND_LINK_STATUS.DISABLED]
    expect(switchValues).toContain('0')
    expect(switchValues).toContain('1')
  })
})
