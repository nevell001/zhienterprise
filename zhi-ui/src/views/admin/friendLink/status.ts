/**
 * 友情链接状态语义（全栈唯一口径，与 blog_friend_link.status 一致）：
 *   0 = 正常（前台展示，前台列表查询 status='0'）
 *   1 = 停用 / 拒绝
 *   2 = 待审核（前台提交申请时写入）
 */
// 与后端 BlogFriendLink.status(String) 对齐：这里必须是**字符串**，
// 否则 el-switch 的 [activeValue, inactiveValue].includes(row.status) 严格比较为 false，
// 每个开关一渲染就误判值非法并 emit change，导致进页面逐行触发"状态更新成功"。
export const FRIEND_LINK_STATUS = {
  NORMAL: '0',
  DISABLED: '1',
  PENDING: '2'
} as const

export type FriendLinkAuditAction = 'approve' | 'reject'

/**
 * 审核动作 → 落库状态。
 *
 * 注意方向：**通过 = '0'**（前台只展示 0），拒绝 = '1'。集中在这里定义，
 * 避免模板/提示文案里各自写数字导致"点通过反而隐藏、点拒绝反而公开"。
 */
export function auditStatusFor(action: FriendLinkAuditAction): '0' | '1' {
  return action === 'approve' ? FRIEND_LINK_STATUS.NORMAL : FRIEND_LINK_STATUS.DISABLED
}
