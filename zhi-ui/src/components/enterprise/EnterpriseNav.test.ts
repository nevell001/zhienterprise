import { describe, it, expect, vi, beforeEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import EnterpriseNav from './EnterpriseNav.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { DEFAULT_HEADER_NAV } from '@/config/siteNav'

/**
 * 企业导航的「菜单显示」契约。
 *
 * 这里锁两类真实回归：
 * 1. 一级链接必须带 `lv1` 类 —— 下划线指示器（`.ent-links a.lv1::after`）挂在它上面，
 *    导航改成 v-for 数据驱动时曾漏掉，导致悬停/当前项下划线永不显示；
 * 2. 当前路由对应的菜单项必须有 `on` 类，否则用户看不出自己在哪一页。
 */
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/solutions' }),
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('@/plugins/element-plus-service', () => ({
  ElMessageBox: { confirm: vi.fn() }
}))

const { getSiteNav } = vi.hoisted(() => ({ getSiteNav: vi.fn() }))

vi.mock('@/api/site/nav', () => ({
  getSiteNav,
  // 默认导航常量在 config/siteNav 中定义，这里保留真实导出
  default: {}
}))

beforeEach(() => {
  getSiteNav.mockResolvedValue({ code: 200, data: [] })
})

function mountNav(settings: Record<string, unknown> = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useSiteSettingsStore().updateSiteSettings({ blog_name: '知企建站系统', ...settings } as never)
  return mount(EnterpriseNav, {
    global: {
      plugins: [pinia],
      stubs: {
        // vue-router 被替换成桩后 router-link 不再是全局组件，这里补一个渲染 <a> 的桩
        RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
        'el-icon': { template: '<i><slot /></i>' },
        'el-dropdown': { template: '<div><slot /></div>' },
        'el-dropdown-menu': { template: '<div><slot /></div>' },
        'el-dropdown-item': { template: '<div><slot /></div>' }
      }
    }
  })
}

describe('EnterpriseNav 菜单显示', () => {
  it('一级菜单都带下划线挂钩类 lv1，且当前页有 on 高亮', () => {
    const wrapper = mountNav()
    const links = wrapper.findAll('.ent-links > li > a')

    // 默认导航至少有首页/核心业务/解决方案等一级项
    expect(links.length).toBe(DEFAULT_HEADER_NAV.filter(i => i.linkType !== 'cta').length)
    for (const link of links) {
      expect(link.classes()).toContain('lv1')
    }

    const active = links.filter(link => link.classes().includes('on'))
    expect(active).toHaveLength(1)
    expect(active[0].attributes('href')).toBe('/solutions')
    expect(active[0].text()).toBe('解决方案')
  })

  it('非当前页菜单不带 on 高亮', () => {
    const wrapper = mountNav()
    const links = wrapper.findAll('.ent-links > li > a')
    const home = links.find(link => link.attributes('href') === '/')
    expect(home).toBeTruthy()
    expect(home!.classes()).not.toContain('on')
  })

  it('菜单管理里 /business 项的标签跟随「栏目名称」（business_label）', async () => {
    getSiteNav.mockResolvedValue({
      code: 200,
      data: [
        {
          id: 1,
          location: 'header',
          parentId: 0,
          label: '首页',
          linkType: 'path',
          linkValue: '/',
          sort: 1,
          visible: 1
        },
        {
          id: 2,
          location: 'header',
          parentId: 0,
          label: '核心业务',
          linkType: 'path',
          linkValue: '/business',
          sort: 2,
          visible: 1
        }
      ]
    })
    const wrapper = mountNav({ business_label: '产品中心' })
    await flushPromises()

    const businessLink = wrapper
      .findAll('.ent-links > li > a')
      .find(link => link.attributes('href') === '/business')
    expect(businessLink?.text()).toBe('产品中心')
  })

  it('business_label 为空时回退「核心业务」', async () => {
    getSiteNav.mockResolvedValue({
      code: 200,
      data: [
        {
          id: 2,
          location: 'header',
          parentId: 0,
          label: '核心业务',
          linkType: 'path',
          linkValue: '/business',
          sort: 2,
          visible: 1
        }
      ]
    })
    const wrapper = mountNav({ business_label: '   ' })
    await flushPromises()

    const businessLink = wrapper
      .findAll('.ent-links > li > a')
      .find(link => link.attributes('href') === '/business')
    expect(businessLink?.text()).toBe('核心业务')
  })

  it('链接值带空白的 /business 同样被栏目名称托管（与后台同口径）', async () => {
    getSiteNav.mockResolvedValue({
      code: 200,
      data: [
        {
          id: 2,
          location: 'header',
          parentId: 0,
          label: '核心业务',
          linkType: 'path',
          linkValue: ' /business ',
          sort: 2,
          visible: 1
        }
      ]
    })
    const wrapper = mountNav({ business_label: '产品中心' })
    await flushPromises()

    const businessLink = wrapper
      .findAll('.ent-links > li > a')
      .find(link => (link.attributes('href') || '').trim() === '/business')
    expect(businessLink?.text()).toBe('产品中心')
  })

  it('二级菜单（关于我们）的下拉项可渲染', () => {
    const wrapper = mountNav()
    const drop = wrapper.find('.ent-links .drop')
    expect(drop.exists()).toBe(true)
    expect(drop.findAll('a').length).toBeGreaterThan(0)
  })
})
