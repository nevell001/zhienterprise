/**
 * 站点功能开关判定 —— 全项目唯一口径，必须与后端 `SiteSwitchUtils.isOn` 完全一致：
 * 只有 `false` / `'false'`（大小写不敏感、允许首尾空白）/ `'0'` / `0` 视为**关闭**，
 * 其余（含缺失、空串、其它任意值）都视为**开启**。
 *
 * 后端实现见 `zhi-common` 的 `SiteSwitchUtils`；store 的 `isFeatureEnabled` 与后台
 * 设置页的开关显示都复用本函数，避免出现"库里是 '1' 但界面当关闭"这类不一致。
 */
export function isSwitchOn(value: unknown): boolean {
  if (value === false || value === 0) {
    return false
  }
  if (value == null) {
    return true
  }
  if (typeof value !== 'string') {
    return true
  }
  const normalized = value.trim()
  if (normalized === '') {
    return true
  }
  return !(normalized.toLowerCase() === 'false' || normalized === '0')
}
