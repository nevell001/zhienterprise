import { describe, expect, it } from 'vitest'
import { isSwitchOn } from './featureSwitch'

/**
 * 这些用例与后端 SiteSwitchUtilsTest 一一对应：任何一侧改了口径，
 * 两边都会有一组用例失败。
 */
describe('功能开关唯一口径', () => {
  it.each([false, 0, '0', 'false', 'FALSE', 'False', ' 0 ', ' false '])('%s 视为关闭', value => {
    expect(isSwitchOn(value)).toBe(false)
  })

  it.each([true, 1, '1', 'true', 'TRUE', 'on', 'yes', '  ', '', null, undefined, {}, []])(
    '%s 视为开启（含缺失/空值）',
    value => {
      expect(isSwitchOn(value)).toBe(true)
    }
  )
})
