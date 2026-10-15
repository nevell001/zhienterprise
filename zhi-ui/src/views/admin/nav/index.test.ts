import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import SiteNav from './index.vue'

/**
 * 后台「导航管理」行为测试（挂载真实组件）。
 * 覆盖页面联动（选择器自动填充标签与路径、请求参数、空态引导）与二级项入口；
 * 表格行内按钮与列表渲染依赖 el-table 的作用域插槽，桩无法还原，交由浏览器人工验证。
 */
const { listNav, listPage, addNav, updateNav, delNav, ElMessageBox, ElMessage } = vi.hoisted(
  () => ({
    listNav: vi.fn(),
    listPage: vi.fn(),
    addNav: vi.fn(),
    updateNav: vi.fn(),
    delNav: vi.fn(),
    ElMessageBox: { confirm: vi.fn() },
    ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() }
  })
)

vi.mock('@/api/admin/nav', () => ({
  listNav: (...args: unknown[]) => listNav(...args),
  addNav: (...args: unknown[]) => addNav(...args),
  updateNav: (...args: unknown[]) => updateNav(...args),
  delNav: (...args: unknown[]) => delNav(...args)
}))

vi.mock('@/api/admin/page', () => ({
  listPage: (...args: unknown[]) => listPage(...args)
}))

vi.mock('@/plugins/element-plus-service', () => ({
  ElMessageBox,
  ElMessage
}))

const navTree = [
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
    id: 4,
    location: 'header',
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

function mountNav() {
  const wrapper = mount(SiteNav, {
    global: {
      plugins: [createPinia()],
      stubs: {
        'el-table': { props: ['data'], template: '<div class="table"></div>' },
        'el-table-column': { template: '<div class="col"></div>' },
        // 不声明 click 事件：父级 @click 作为原生监听器透传到根 <button>，触发一次即一次
        'el-button': { template: '<button><slot /></button>' },
        'el-tabs': { template: '<div><slot /></div>' },
        'el-tab-pane': { template: '<div><slot /></div>' },
        'el-row': { template: '<div><slot /></div>' },
        'el-col': { template: '<div><slot /></div>' },
        'el-tag': { template: '<span class="tag"><slot /></span>' },
        'el-switch': { template: '<span />' },
        'el-dialog': {
          props: ['modelValue', 'title'],
          template:
            '<div v-if="modelValue" class="dialog"><div class="dialog-title">{{ title }}</div><slot /><slot name="footer" /></div>'
        },
        // 表单桩直接放行校验，测试可走到真实的 submitForm 提交逻辑
        'el-form': {
          name: 'SiteNavFormStub',
          methods: {
            validate(cb: (valid: boolean) => void) {
              cb(true)
            }
          },
          template: '<form><slot /></form>'
        },
        'el-form-item': { template: '<div class="fi"><slot /></div>' },
        'el-input': { props: ['modelValue'], template: '<input :value="modelValue" />' },
        'el-radio-group': { template: '<div><slot /></div>' },
        'el-radio': { template: '<label><slot /></label>' },
        // 选择器桩：测试通过该组件的 change 事件驱动页面真实的 handlePageSelect
        'el-select': {
          name: 'SiteNavSelectStub',
          props: ['modelValue'],
          emits: ['change', 'update:modelValue'],
          template: '<div class="select"><slot /></div>'
        },
        'el-option': {
          props: ['value', 'label'],
          template: '<div class="option">{{ label }}</div>'
        },
        'el-input-number': { props: ['modelValue'], template: '<input type="number" />' }
      }
    }
  })
  return { wrapper }
}

describe('SiteNav 导航管理（页面联动）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listNav.mockResolvedValue({ code: 200, data: navTree })
    listPage.mockResolvedValue({
      code: 200,
      rows: [
        { id: 11, title: '关于我们', slug: 'about', status: '1', pageType: 'richtext' },
        { id: 12, title: '服务条款', slug: 'terms', status: '1', pageType: 'richtext' }
      ],
      total: 2
    })
    addNav.mockResolvedValue({ code: 200 })
    updateNav.mockResolvedValue({ code: 200 })
    delNav.mockResolvedValue({ code: 200 })
  })

  it('页面选择器只请求已发布页面', async () => {
    mountNav()
    await flushPromises()
    expect(listPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 200, status: '1' })
  })

  it('选择页面后自动填充标签与 /page 路径', async () => {
    const { wrapper } = mountNav()
    await flushPromises()

    await wrapper
      .findAll('button')
      .find(b => b.text().includes('添加导航项'))!
      .trigger('click')
    await flushPromises()
    expect(wrapper.find('.dialog').exists()).toBe(true)

    // 选择器 change 事件即真实的 handlePageSelect（选中「服务条款」）
    const select = wrapper.findComponent({ name: 'SiteNavSelectStub' })
    await select.vm.$emit('change', 'terms')
    await flushPromises()

    const values = wrapper.findAll('input').map(i => (i.element as HTMLInputElement).value)
    expect(values).toContain('服务条款')
    expect(values).toContain('/page/terms')
  })

  it('提交新增导航项时调用 addNav 并携带页面路径', async () => {
    const { wrapper } = mountNav()
    await flushPromises()

    await wrapper
      .findAll('button')
      .find(b => b.text().includes('添加导航项'))!
      .trigger('click')
    await flushPromises()

    const select = wrapper.findComponent({ name: 'SiteNavSelectStub' })
    await select.vm.$emit('change', 'terms')
    await flushPromises()

    const okButton = wrapper.findAll('button').find(b => b.text().includes('确 定'))!
    await okButton.trigger('click')
    await flushPromises()

    expect(addNav).toHaveBeenCalledTimes(1)
    const payload = addNav.mock.calls[0][0] as Record<string, unknown>
    expect(payload.label).toBe('服务条款')
    expect(payload.linkValue).toBe('/page/terms')
    expect(payload.location).toBe('header')
    expect(payload.parentId).toBe(0)
  })

  it('「添加二级项」入口对任意顶级项可用，弹窗标题带父项名', async () => {
    const { wrapper } = mountNav()
    await flushPromises()

    // 不再写死「关于我们」：默认取导航树首个顶级项作为父项
    const addChild = wrapper.findAll('button').find(b => b.text() === '添加二级项')!
    expect(addChild.attributes('disabled')).toBeUndefined()
    await addChild.trigger('click')
    await flushPromises()

    const dialog = wrapper.find('.dialog')
    expect(dialog.exists()).toBe(true)
    expect(dialog.text()).toContain('添加二级项（首页）')
  })

  it('已发布页面为空时给出引导提示', async () => {
    listPage.mockResolvedValue({ code: 200, rows: [], total: 0 })
    const { wrapper } = mountNav()
    await flushPromises()

    await wrapper
      .findAll('button')
      .find(b => b.text().includes('添加导航项'))!
      .trigger('click')
    await flushPromises()

    expect(wrapper.find('.dialog').text()).toContain('请先在「页面管理」新建并发布页面')
  })
})
