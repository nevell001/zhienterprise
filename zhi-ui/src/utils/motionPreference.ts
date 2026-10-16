/**
 * 动效偏好（企业官网模板体系，方案文档 §4.7 硬性要求）。
 *
 * 三层判定，优先级从高到低：
 * 1. 用户手动开关（页脚「减弱动效」，html.motion-off 类 + localStorage 持久化）
 *    ——优先级高于系统设置；
 * 2. prefers-reduced-motion: reduce（系统设置）；
 * 3. 性能降级（按序触发）：saveData → 纯终态；设备内存 ≤4GB 或核数 ≤4 → 纯淡入。
 *
 * shouldSkipMotion() = 完全跳过动效直接呈现终态；
 * motionFadeOnly()   = 纯淡入无位移（渲染容器需挂 .rv-fade 类）。
 */

export const MOTION_OFF_STORAGE_KEY = 'motion-off'
const MOTION_OFF_CLASS = 'motion-off'

export function isMotionOff(): boolean {
  return (
    typeof document !== 'undefined' && document.documentElement.classList.contains(MOTION_OFF_CLASS)
  )
}

export function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
}

export function shouldSkipMotion(): boolean {
  return isMotionOff() || prefersReducedMotion()
}

/** 性能降级（§4.7）：低内存/少核/saveData 设备关闭位移，仅保留淡入 */
export function motionFadeOnly(): boolean {
  if (typeof navigator === 'undefined') return false
  const conn = (navigator as { connection?: { saveData?: boolean } }).connection
  if (conn?.saveData) return true
  const memory = (navigator as { deviceMemory?: number }).deviceMemory
  if (memory !== undefined && memory <= 4) return true
  const cores = navigator.hardwareConcurrency
  if (cores !== undefined && cores <= 4) return true
  return false
}

/** 启动时同步手动开关到 <html> 类（页脚开关/初始化调用） */
export function applyMotionOff(on: boolean): void {
  if (typeof document === 'undefined') return
  document.documentElement.classList.toggle(MOTION_OFF_CLASS, on)
  try {
    localStorage.setItem(MOTION_OFF_STORAGE_KEY, on ? '1' : '0')
  } catch {
    // Storage 不可用（隐私模式）时仅类生效
  }
}

/** 读取持久化的手动开关（默认关） */
export function getMotionOffStored(): boolean {
  try {
    return localStorage.getItem(MOTION_OFF_STORAGE_KEY) === '1'
  } catch {
    return false
  }
}
