import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, it, expect } from 'vitest'

const sourcePath = resolve(process.cwd(), 'src/views/UnifiedLogin.vue')

describe('UnifiedLogin 组件测试', () => {
  it('应该导出 UnifiedLogin 组件', async () => {
    const module = await import('./UnifiedLogin.vue')
    expect(module.default).toBeDefined()
  })

  it('应该遵循后端验证码开关并使用 JPEG 验证码图片', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain(
      'captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled'
    )
    expect(source).toContain("'data:image/jpeg;base64,'")
    expect(source).not.toContain("'data:image/gif;base64,'")
  })

  it('登录页沿用紧凑单卡片布局，并接入企业官网设计语言', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    // 单卡片结构（不做左右分栏的 auth-visual/features 大图布局）
    expect(source).toContain('class="ent-auth-card"')
    expect(source).toContain('class="ent-auth-brand"')
    expect(source).toContain('class="ent-auth-switch"')
    expect(source).not.toContain('class="auth-visual"')
    expect(source).not.toContain('class="features"')
    expect(source).not.toContain('沉浸式阅读体验')
    expect(source).not.toContain('size="large"')
    expect(source).toContain('max-width: 380px;')

    // 与注册/找回密码一致：包在 SiteLayout 中，企业模式自动带企业导航与页脚
    expect(source).toContain('<SiteLayout>')
    // 样式走企业官网的 --tpl-* 变量层，而非硬编码色值
    expect(source).toContain('--tpl-ink-900')
    expect(source).toContain('--tpl-p')
    expect(source).toContain('tpl-site')
    // 品牌取站点设置，不写死站名
    expect(source).toContain('siteSettingsStore')
    expect(source).not.toContain('<span>知企建站系统</span>')
  })
})
