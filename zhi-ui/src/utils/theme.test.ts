import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  APP_THEME_STORAGE_KEY,
  applyAppTheme,
  getDarkColor,
  getLightColor,
  handleThemeStyle,
  hexToRgb,
  normalizeAppTheme,
  readableOnPrimary,
  rgbToHex
} from './theme'

// Mock document
const mockStyle: any = {
  setProperty: vi.fn()
}

function createRootStub(): HTMLElement {
  const classes = new Set<string>()
  return {
    classList: {
      add: (className: string) => classes.add(className),
      remove: (className: string) => classes.delete(className),
      contains: (className: string) => classes.has(className)
    }
  } as HTMLElement
}

vi.stubGlobal('document', {
  documentElement: {
    style: mockStyle
  }
})

describe('theme 工具测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('hexToRgb', () => {
    it('应该将 hex 颜色转换为 rgb 数组', () => {
      expect(hexToRgb('#ffffff')).toEqual([255, 255, 255])
      expect(hexToRgb('#000000')).toEqual([0, 0, 0])
      expect(hexToRgb('#409eff')).toEqual([64, 158, 255])
    })

    it('应该处理没有 # 前缀的颜色', () => {
      expect(hexToRgb('ffffff')).toEqual([255, 255, 255])
      expect(hexToRgb('409eff')).toEqual([64, 158, 255])
    })

    it('应该处理无效颜色并返回默认值', () => {
      // hexToRgb 函数对无效输入返回 [0, 0, 0]
      expect(hexToRgb('')).toEqual([0, 0, 0])
      expect(hexToRgb('invalid')).toEqual([0, 0, 0]) // 匹配3个字符，转换为有效的hex
      expect(hexToRgb('#')).toEqual([0, 0, 0])
    })
  })

  describe('rgbToHex', () => {
    it('应该将 rgb 颜色转换为 hex 颜色', () => {
      expect(rgbToHex(255, 255, 255)).toBe('#ffffff')
      expect(rgbToHex(0, 0, 0)).toBe('#000000')
      expect(rgbToHex(64, 158, 255)).toBe('#409eff')
    })

    it('应该正确处理单个数字的十六进制值', () => {
      expect(rgbToHex(1, 1, 1)).toBe('#010101')
      expect(rgbToHex(15, 15, 15)).toBe('#0f0f0f')
    })
  })

  describe('getLightColor', () => {
    it('应该生成变浅的颜色', () => {
      const lightColor = getLightColor('#409eff', 0.1)
      expect(lightColor).toMatch(/^#[0-9a-fA-F]{6}$/)
    })

    it('应该根据 level 生成不同深度的浅色', () => {
      const color1 = getLightColor('#409eff', 0.1)
      const color2 = getLightColor('#409eff', 0.5)
      expect(color1).not.toBe(color2)
    })
  })

  describe('getDarkColor', () => {
    it('应该生成变深的颜色', () => {
      const darkColor = getDarkColor('#409eff', 0.1)
      expect(darkColor).toMatch(/^#[0-9a-fA-F]{6}$/)
    })

    it('应该根据 level 生成不同深度的深色', () => {
      const color1 = getDarkColor('#409eff', 0.1)
      const color2 = getDarkColor('#409eff', 0.5)
      expect(color1).not.toBe(color2)
    })
  })

  describe('readableOnPrimary', () => {
    it('亮色主色（EP 默认蓝 / Tech Blue 亮青）应该选深墨色文字', () => {
      // 白字对比度只有 2.78:1 / 1.77:1，不达 AA
      expect(readableOnPrimary('#409eff')).toBe('#06272e')
      expect(readableOnPrimary('#00d4ff')).toBe('#06272e')
    })

    it('深色主色（Mo-Blog 靛蓝）应该选白色文字', () => {
      expect(readableOnPrimary('#4f46e5')).toBe('#ffffff')
      expect(readableOnPrimary('#6366f1')).toBe('#ffffff')
      expect(readableOnPrimary('#5f62e7')).toBe('#ffffff')
    })

    it('主题实际使用的主色，配对文字对比度都不低于 4.5:1', () => {
      const luminance = (rgb: number[]) => {
        const [r, g, b] = rgb
          .map(value => value / 255)
          .map(channel =>
            channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4)
          )
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
      }
      const contrast = (a: string, b: string) => {
        const [lighter, darker] = [luminance(hexToRgb(a)), luminance(hexToRgb(b))].sort(
          (x, y) => y - x
        )
        return (lighter + 0.05) / (darker + 0.05)
      }
      // EP 默认蓝、Tech Blue 亮青（默认主题深浅两态）、Mo-Blog 靛蓝（浅色 / 深色微调后）
      for (const primary of ['#409eff', '#00d4ff', '#4f46e5', '#5f62e7']) {
        expect(contrast(readableOnPrimary(primary), primary)).toBeGreaterThanOrEqual(4.5)
      }
      // 中间调主色（如靛蓝 500 / 粉色）在白与墨之间取更高对比度那个
      for (const primary of ['#6366f1', '#ec4899']) {
        const picked = readableOnPrimary(primary)
        const other = picked === '#ffffff' ? '#06272e' : '#ffffff'
        expect(contrast(picked, primary)).toBeGreaterThanOrEqual(contrast(other, primary))
      }
    })
  })

  describe('handleThemeStyle', () => {
    it('应该设置主色调', () => {
      handleThemeStyle('#409eff')
      expect(mockStyle.setProperty).toHaveBeenCalledWith('--el-color-primary', '#409eff')
    })

    it('应该设置主色填充控件的前景色', () => {
      handleThemeStyle('#409eff')
      expect(mockStyle.setProperty).toHaveBeenCalledWith('--mo-on-primary', '#06272e')

      handleThemeStyle('#4f46e5')
      expect(mockStyle.setProperty).toHaveBeenCalledWith('--mo-on-primary', '#ffffff')
    })

    it('应该设置浅色变体', () => {
      handleThemeStyle('#409eff')
      expect(mockStyle.setProperty).toHaveBeenCalledWith(
        '--el-color-primary-light-1',
        expect.any(String)
      )
    })

    it('应该设置深色变体', () => {
      handleThemeStyle('#409eff')
      expect(mockStyle.setProperty).toHaveBeenCalledWith(
        '--el-color-primary-dark-1',
        expect.any(String)
      )
    })

    it('应该处理无效的主题并使用默认值', () => {
      handleThemeStyle('')
      expect(mockStyle.setProperty).toHaveBeenCalledWith('--el-color-primary', '#409EFF')
    })

    it('应该处理 undefined 主题并使用默认值', () => {
      handleThemeStyle(undefined as any)
      expect(mockStyle.setProperty).toHaveBeenCalledWith('--el-color-primary', '#409EFF')
    })

    it('应该生成所有9种浅色变体', () => {
      handleThemeStyle('#409eff')
      for (let i = 1; i <= 9; i++) {
        expect(mockStyle.setProperty).toHaveBeenCalledWith(
          `--el-color-primary-light-${i}`,
          expect.any(String)
        )
      }
    })

    it('应该生成所有9种深色变体', () => {
      handleThemeStyle('#409eff')
      for (let i = 1; i <= 9; i++) {
        expect(mockStyle.setProperty).toHaveBeenCalledWith(
          `--el-color-primary-dark-${i}`,
          expect.any(String)
        )
      }
    })
  })

  describe('app theme helpers', () => {
    it('已移除的 mo-blog 主题应归一化为 default', () => {
      expect(normalizeAppTheme('mo-blog')).toBe('default')
      expect(normalizeAppTheme('default')).toBe('default')
      expect(normalizeAppTheme('unknown')).toBe('default')
      expect(normalizeAppTheme(undefined)).toBe('default')
    })

    it('申请 mo-blog 也保持 default 且不挂皮肤类（主题已下线）', () => {
      const root = createRootStub()
      const storage = window.localStorage
      storage.clear()

      applyAppTheme('mo-blog', root, storage)

      expect(root.classList.contains('theme-mo-blog')).toBe(false)
      expect(storage.getItem(APP_THEME_STORAGE_KEY)).toBe('default')
    })

    it('应该切回 default 并移除 mo-blog class', () => {
      const root = createRootStub()
      root.classList.add('theme-mo-blog')
      const storage = window.localStorage

      applyAppTheme('default', root, storage)

      expect(root.classList.contains('theme-mo-blog')).toBe(false)
      expect(storage.getItem(APP_THEME_STORAGE_KEY)).toBe('default')
    })
  })
})
