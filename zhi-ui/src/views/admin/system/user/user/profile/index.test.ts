import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import Profile from './index.vue'

const sourcePath = resolve(process.cwd(), 'src/views/admin/system/user/user/profile/index.vue')

describe('Profile 视图组件测试', () => {
  it('应该导出 Profile 组件', () => {
    expect(Profile).toBeDefined()
    expect(typeof Profile).toBe('object')
  })

  it('个人中心不应该展示硬编码的假文章、粉丝和通知数据', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).not.toContain('<div class="num">42</div>')
    expect(source).not.toContain('<div class="num">1.2k</div>')
    expect(source).not.toContain('Vue 3.4 新特性详解')
    expect(source).not.toContain('主公')
    expect(source).not.toContain('码农阿强')
  })

  it('个人中心文章相关操作应该跳转到真实文章管理页', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain("goArticleManage('create')")
    expect(source).toContain("goArticleManage('list')")
    expect(source).toContain("router.push('/admin/article')")
  })

  it('编辑资料入口应该直接打开基本资料设置页签', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain("openSettings('userinfo')")
    expect(source).toContain('settingsSubTab.value = tab')
  })

  it('个人中心应该使用 Mo Blog 工作台式主题布局', () => {
    const source = readFileSync(sourcePath, 'utf-8')

    expect(source).toContain('class="profile-shell"')
    expect(source).toContain('class="profile-summary"')
    expect(source).toContain('class="profile-main-grid"')
    expect(source).toContain('var(--mo-p600')
    expect(source).toContain('var(--mo-n50')
    expect(source).not.toContain('class="profile-cover"')
  })
})
