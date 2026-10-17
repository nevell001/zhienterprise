import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import type { App } from 'vue'
import { createPinia } from 'pinia'
import PageManage from './index.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'

/**
 * 后台「页面管理」行为测试（挂载真实组件）。
 * 覆盖与「菜单管理」的联动（顶部导航列的已加入/未加入、加入/移出、首页标签）、
 * 站点设置拉取与企业模式下「导航显示」字段的隐藏；
 * 表格行内渲染与按钮点击依赖 el-table 作用域插槽，桩无法还原，交由浏览器人工验证。
 */
const {
  listPage,
  getPage,
  addPage,
  updatePage,
  changePageStatus,
  delPage,
  listNav,
  addNav,
  delNav,
  getSiteSettingsAnonymous
} = vi.hoisted(() => ({
  listPage: vi.fn(),
  getPage: vi.fn(),
  addPage: vi.fn(),
  updatePage: vi.fn(),
  changePageStatus: vi.fn(),
  delPage: vi.fn(),
  listNav: vi.fn(),
  addNav: vi.fn(),
  delNav: vi.fn(),
  getSiteSettingsAnonymous: vi.fn()
}))

vi.mock('@/api/admin/page', () => ({
  listPage: (...args: unknown[]) => listPage(...args),
  getPage: (...args: unknown[]) => getPage(...args),
  addPage: (...args: unknown[]) => addPage(...args),
  updatePage: (...args: unknown[]) => updatePage(...args),
  changePageStatus: (...args: unknown[]) => changePageStatus(...args),
  delPage: (...args: unknown[]) => delPage(...args)
}))

vi.mock('@/api/admin/nav', () => ({
  listNav: (...args: unknown[]) => listNav(...args),
  addNav: (...args: unknown[]) => addNav(...args),
  delNav: (...args: unknown[]) => delNav(...args)
}))

vi.mock('@/api/site/setting', () => ({
  getSiteSettingsAnonymous: (...args: unknown[]) => getSiteSettingsAnonymous(...args)
}))

const modalMock = {
  msgSuccess: vi.fn(),
  msgError: vi.fn(),
  confirm: vi.fn<() => Promise<unknown>>()
}

/** 模拟 main.ts 中挂载的全局 $modal（组件通过 getCurrentInstance().proxy 取用） */
const globalPropertiesPlugin = {
  install(app: App) {
    app.config.globalProperties.$modal = modalMock
  }
}

const pageRows = [
  { id: 11, title: '关于我们', slug: 'about', status: '1', pageType: 'richtext' },
  { id: 12, title: '服务条款', slug: 'terms', status: '1', pageType: 'richtext' }
]

/** 头部导航：顶级「关于我们」+ 指向同一页面的二级项，用于验证重复挂载的识别 */
const headerNav = [
  { id: 1, parentId: 0, label: '首页', linkType: 'path', linkValue: '/', sort: 1, visible: 1 },
  {
    id: 4,
    parentId: 0,
    label: '关于我们',
    linkType: 'path',
    linkValue: '/page/about',
    sort: 4,
    visible: 1,
    children: [
      {
        id: 41,
        parentId: 4,
        label: '企业简介',
        linkType: 'path',
        linkValue: '/page/about',
        sort: 1,
        visible: 1
      }
    ]
  }
]

/**
 * 菜单管理里维护「关于我们」实际写的是模板短路径 /about（站点路由），
 * 同时存在早期「加入导航」留下的 /page/about 重复项 —— 联动判定必须两种都认。
 */
const shortPathNav = [
  { id: 1, parentId: 0, label: '首页', linkType: 'path', linkValue: '/', sort: 1, visible: 1 },
  {
    id: 4,
    parentId: 0,
    label: '关于我们',
    linkType: 'path',
    linkValue: '/about',
    sort: 4,
    visible: 1
  },
  {
    id: 5,
    parentId: 0,
    label: '关于我们',
    linkType: 'path',
    linkValue: '/page/about',
    sort: 5,
    visible: 1
  }
]

type PageVm = {
  navUnavailable: boolean
  isInNav: (row: { slug?: string }) => boolean
  isHomePage: (row: { slug?: string }) => boolean
  handleAddToNav: (row: { slug?: string; title?: string }) => Promise<void>
  handleRemoveFromNav: (row: { slug?: string; title?: string }) => Promise<void>
  handleAdd: () => void
  handleUpdate: (row?: { id: number }) => void
  sectionList: Array<{ type: string; config: Record<string, any> }>
  rules: { slug: { required?: boolean; pattern?: RegExp }[] }
}

function mountView(options?: { onError?: (err: unknown) => void }) {
  const pinia = createPinia()
  const wrapper = mount(PageManage, {
    global: {
      config: options?.onError ? { errorHandler: options.onError } : {},
      plugins: [pinia, globalPropertiesPlugin],
      directives: {
        hasPermi: {},
        loading: {}
      },
      stubs: {
        pagination: true,
        'right-toolbar': true,
        // el-table 作用域插槽桩无法还原行渲染，仅保证挂载通路并暴露列标题；行内按钮交由人工验证
        'el-table': { props: ['data'], template: '<div class="table"><slot /></div>' },
        'el-table-column': { props: ['label'], template: '<div class="col">{{ label }}</div>' },
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': {
          props: ['label'],
          template: '<div class="fi"><span class="fi-label">{{ label }}</span><slot /></div>'
        },
        'el-input': { props: ['modelValue'], template: '<input :value="modelValue" />' },
        'el-select': { props: ['modelValue'], template: '<div><slot /></div>' },
        'el-option': { props: ['label'], template: '<div class="option">{{ label }}</div>' },
        'el-button': { template: '<button><slot /></button>' },
        'el-row': { template: '<div><slot /></div>' },
        'el-col': { template: '<div><slot /></div>' },
        'el-tag': { template: '<span class="tag"><slot /></span>' },
        'el-switch': { props: ['modelValue'], template: '<span />' },
        'el-radio-group': { template: '<div><slot /></div>' },
        'el-radio': { template: '<label><slot /></label>' },
        'el-radio-button': { template: '<label><slot /></label>' },
        'el-input-number': { props: ['modelValue'], template: '<input type="number" />' },
        'el-tooltip': { template: '<span><slot /></span>' },
        'el-dropdown': { template: '<div><slot /></div>' },
        'el-dropdown-menu': { template: '<div><slot /></div>' },
        'el-dropdown-item': { template: '<div><slot /></div>' },
        'el-icon': { template: '<i><slot /></i>' },
        'el-checkbox': { template: '<label><slot /></label>' },
        'arrow-down': { template: '<span />' },
        'el-dialog': {
          props: ['modelValue', 'title'],
          template:
            '<div v-if="modelValue" class="dialog"><div class="dialog-title">{{ title }}</div><slot /><slot name="footer" /></div>'
        }
      }
    }
  })
  return { wrapper, pinia }
}

/** 已渲染的表格列标题（列桩把 label 渲染进 .col） */
function colLabels(wrapper: ReturnType<typeof mountView>['wrapper']): string[] {
  return wrapper.findAll('.col').map(node => node.text())
}

/** 已渲染的弹窗表单项标题 */
function formLabels(wrapper: ReturnType<typeof mountView>['wrapper']): string[] {
  return wrapper.findAll('.fi-label').map(node => node.text())
}

describe('PageManage 页面管理（导航联动）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listPage.mockResolvedValue({ code: 200, rows: pageRows, total: pageRows.length })
    listNav.mockResolvedValue({ code: 200, data: headerNav })
    addNav.mockResolvedValue({ code: 200 })
    delNav.mockResolvedValue({ code: 200 })
    // 默认经典模式：导航裁剪与「导航显示」开关才有消费点
    getSiteSettingsAnonymous.mockResolvedValue({ code: 200, data: { home_layout: 'article' } })
    modalMock.confirm.mockResolvedValue(undefined)
  })

  it('挂载后加载页面列表并拉取头部导航', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    expect(listPage).toHaveBeenCalledTimes(1)
    expect(listPage).toHaveBeenCalledWith(expect.objectContaining({ pageNum: 1, pageSize: 10 }))
    // 次要信息（顶部导航列）静默加载：失败由调用方降级，不弹兜底 toast
    expect(listNav).toHaveBeenCalledWith('header', { silent: true })
    expect((wrapper.vm as unknown as PageVm).navUnavailable).toBe(false)
  })

  it('列表加载失败时提示错误且不阻塞挂载', async () => {
    listPage.mockRejectedValue({})
    mountView()
    await flushPromises()

    expect(modalMock.msgError).toHaveBeenCalledWith('获取页面列表失败')
  })

  it('导航接口失败时降级为「导航数据不可用」，不抛异常不弹错', async () => {
    listNav.mockRejectedValue(new Error('boom'))
    const { wrapper } = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    expect(vm.navUnavailable).toBe(true)
    expect(vm.isInNav({ slug: 'about' })).toBe(false)
    // 组件自身不得把导航加载失败升级为页面级错误提示（toast 抑制在请求配置层）
    expect(modalMock.msgError).not.toHaveBeenCalled()
  })

  it('已加入判定命中顶级项与二级项，未挂载的页面为未加入', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    expect(vm.isInNav({ slug: 'about' })).toBe(true)
    expect(vm.isInNav({ slug: 'terms' })).toBe(false)
    expect(vm.isInNav({})).toBe(false)
  })

  it('「加入」以顶级项追加到导航末尾并刷新导航', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    await vm.handleAddToNav({ id: 12, slug: 'terms', title: '服务条款' })

    expect(addNav).toHaveBeenCalledTimes(1)
    expect(addNav).toHaveBeenCalledWith({
      location: 'header',
      parentId: 0,
      label: '服务条款',
      linkType: 'path',
      linkValue: '/page/terms',
      sort: 5,
      visible: 1
    })
    expect(modalMock.msgSuccess).toHaveBeenCalledWith('已加入头部导航')
    expect(listNav).toHaveBeenCalledTimes(2)
  })

  it('「移出」经确认后删除该页面的全部导航项（含二级项）', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    await vm.handleRemoveFromNav({ slug: 'about', title: '关于我们' })

    expect(modalMock.confirm).toHaveBeenCalledTimes(1)
    expect(delNav).toHaveBeenCalledWith([4, 41])
    expect(modalMock.msgSuccess).toHaveBeenCalledWith('已移出头部导航')
    expect(listNav).toHaveBeenCalledTimes(2)
  })

  it('移出确认弹窗取消时不调用删除接口', async () => {
    modalMock.confirm.mockRejectedValueOnce('cancel')
    const { wrapper } = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    await vm.handleRemoveFromNav({ slug: 'about', title: '关于我们' })

    expect(delNav).not.toHaveBeenCalled()
  })

  it('「首页」标签与站点设置 home_page_slug 联动', async () => {
    const { wrapper, pinia } = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    const store = useSiteSettingsStore(pinia)
    expect(vm.isHomePage({ slug: 'about' })).toBe(false)

    store.siteSettings.home_page_slug = 'about'
    expect(vm.isHomePage({ slug: 'about' })).toBe(true)
    expect(vm.isHomePage({ slug: 'terms' })).toBe(false)

    store.siteSettings.home_page_slug = ''
    expect(vm.isHomePage({ slug: 'about' })).toBe(false)
  })

  it('挂载时拉取站点设置：「当前首页」标记进页面即生效', async () => {
    getSiteSettingsAnonymous.mockResolvedValue({
      code: 200,
      data: { home_layout: 'article', home_page_slug: 'terms' }
    })
    const { wrapper } = mountView()
    await flushPromises()

    expect(getSiteSettingsAnonymous).toHaveBeenCalledTimes(1)
    const vm = wrapper.vm as unknown as PageVm
    expect(vm.isHomePage({ slug: 'terms' })).toBe(true)
    expect(vm.isHomePage({ slug: 'about' })).toBe(false)
  })

  it('模板页的短路径导航项同样识别为已加入，「移出」一并清掉重复项', async () => {
    // 菜单管理里「关于我们」存的是路由短路径 /about，另有一条早期一键加入留下的 /page/about
    listNav.mockResolvedValue({ code: 200, data: shortPathNav })
    const { wrapper } = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    expect(vm.isInNav({ slug: 'about' })).toBe(true)

    await vm.handleRemoveFromNav({ slug: 'about', title: '关于我们' })
    expect(delNav).toHaveBeenCalledWith([4, 5])
  })

  it('「加入」模板页写入短路径，不再新增 /page/{slug} 重复项', async () => {
    listNav.mockResolvedValue({ code: 200, data: shortPathNav })
    const { wrapper } = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    await vm.handleAddToNav({ slug: 'about', title: '关于我们' })

    expect(addNav).toHaveBeenCalledWith(
      expect.objectContaining({ linkType: 'path', linkValue: '/about' })
    )
    // 通用页面没有短路径，仍写 /page/{slug}
    await vm.handleAddToNav({ slug: 'terms', title: '服务条款' })
    expect(addNav).toHaveBeenLastCalledWith(expect.objectContaining({ linkValue: '/page/terms' }))
  })

  it('企业官网模式下隐藏「导航显示」列与编辑表单项', async () => {
    getSiteSettingsAnonymous.mockResolvedValue({ code: 200, data: { home_layout: 'business' } })
    const { wrapper } = mountView()
    await flushPromises()

    expect(colLabels(wrapper)).not.toContain('导航显示')
    const vm = wrapper.vm as unknown as PageVm
    vm.handleAdd()
    await nextTick()
    expect(formLabels(wrapper)).not.toContain('导航显示')
    // 同页其余控件不受影响
    expect(colLabels(wrapper)).toContain('状态')
    expect(formLabels(wrapper)).toContain('排序')
  })

  it('经典模式保留「导航显示」列与表单项', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    expect(colLabels(wrapper)).toContain('导航显示')
    const vm = wrapper.vm as unknown as PageVm
    vm.handleAdd()
    await nextTick()
    expect(formLabels(wrapper)).toContain('导航显示')
  })

  it('站点设置拉取失败按经典模式渲染，不凭 store 默认值隐藏控件', async () => {
    getSiteSettingsAnonymous.mockRejectedValue(new Error('boom'))
    const { wrapper } = mountView()
    await flushPromises()

    // store 里 home_layout 默认为 business，未真正加载（lastUpdate=0）时不得据此隐藏列
    expect(colLabels(wrapper)).toContain('导航显示')
  })

  it('别名校验规则只允许字母、数字、下划线、连字符', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    const { rules } = wrapper.vm as unknown as PageVm
    const slugRule = rules.slug.find(rule => rule.pattern)
    expect(slugRule).toBeDefined()
    expect(slugRule!.pattern!.test('about-us')).toBe(true)
    expect(slugRule!.pattern!.test('about_us2')).toBe(true)
    expect(slugRule!.pattern!.test('about us')).toBe(false)
    expect(slugRule!.pattern!.test('关于我们')).toBe(false)
  })

  it('builder 旧数据打开编辑器：缺键配置被补齐且预渲染全部绑定不报错', async () => {
    // 历史页面可能保存于早期模块版本：hero 缺 ctaPrimary、biz-cards 缺 cardOne/cardTwo 等
    getPage.mockResolvedValue({
      code: 200,
      data: {
        id: 21,
        title: '企业首页',
        slug: 'home',
        pageType: 'builder',
        status: '1',
        sections: JSON.stringify([
          { type: 'hero', visible: true, config: { eyebrow: '旧眉标' } },
          { type: 'biz-cards', visible: true, config: {} },
          { type: 'contact-info', visible: true, config: {} }
        ])
      }
    })
    const errors: unknown[] = []
    const { wrapper } = mountView({ onError: err => errors.push(err) })
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    vm.handleUpdate({ id: 21 })
    await flushPromises()

    // 编辑器为 v-show 预渲染：任何未补齐的对象键（如 sec.config.cardOne.title）都会在渲染期抛错
    expect(errors).toEqual([])
    expect(vm.sectionList[0].config.eyebrow).toBe('旧眉标')
    expect(vm.sectionList[0].config.ctaPrimary).toEqual({
      label: '了解我们的业务',
      link: '/business'
    })
    expect(vm.sectionList[1].config.cardOne).toEqual({ title: '', desc: '' })
    expect(vm.sectionList[1].config.cardTwo).toEqual({ title: '', desc: '' })
    expect(vm.sectionList[2].config.contact_email).toBe('')
  })

  it('builder 数据缺 cardOne 子字段时同样安全（已存在对象不整体覆盖）', async () => {
    getPage.mockResolvedValue({
      code: 200,
      data: {
        id: 22,
        title: '企业首页',
        slug: 'home',
        pageType: 'builder',
        status: '1',
        sections: JSON.stringify([
          { type: 'biz-cards', visible: true, config: { cardOne: { title: '仅标题' } } }
        ])
      }
    })
    const errors: unknown[] = []
    const { wrapper } = mountView({ onError: err => errors.push(err) })
    await flushPromises()

    const vm = wrapper.vm as unknown as PageVm
    vm.handleUpdate({ id: 22 })
    await flushPromises()

    expect(errors).toEqual([])
    // 已有的 title 保留，缺失的 desc 允许 undefined（绑定安全，保存后前台回退模板）
    expect(vm.sectionList[0].config.cardOne.title).toBe('仅标题')
  })
})
