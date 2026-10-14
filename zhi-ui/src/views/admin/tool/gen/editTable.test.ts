import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const getGenTableMock = vi.fn()
const updateGenTableMock = vi.fn()
const columnListMock = vi.fn()
const treeselectMock = vi.fn()

vi.mock('@/api/tool/gen', () => ({
  getGenTable: (...args: any[]) => getGenTableMock(...args),
  updateGenTable: (...args: any[]) => updateGenTableMock(...args),
  columnList: (...args: any[]) => columnListMock(...args)
}))

vi.mock('@/api/system/menu', () => ({
  treeselect: (...args: any[]) => treeselectMock(...args)
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { tableId: '9' }, query: { pageNum: '2' } })
}))

import EditTable from './editTable.vue'

const formStub = {
  template: '<form><slot /></form>',
  methods: { validate: () => Promise.resolve(true) }
}
const passthroughStub = { template: '<div><slot /></div>' }
const buttonStub = { template: '<button><slot /></button>' }

function tableInfo(overrides: Record<string, any> = {}) {
  return {
    tableId: 9,
    tableName: 'blog_article',
    tableComment: '文章表',
    className: 'SiteArticle',
    tplCategory: 'crud',
    packageName: 'com.zhi.system',
    moduleName: 'system',
    businessName: 'article',
    functionName: '文章',
    functionAuthor: 'zhi',
    genType: '0',
    ...overrides
  }
}

function mountView() {
  const $modal = { msgSuccess: vi.fn(), msgError: vi.fn() }
  const $tab = { closeOpenPage: vi.fn() }
  const wrapper = mount(EditTable, {
    global: {
      config: { globalProperties: { $modal, $tab } },
      stubs: {
        'el-form': formStub,
        'el-button': buttonStub,
        'el-card': passthroughStub,
        'el-tabs': passthroughStub,
        'el-tab-pane': passthroughStub,
        'el-row': passthroughStub,
        'el-col': passthroughStub,
        'el-form-item': passthroughStub,
        'el-table': true,
        'el-table-column': true,
        'el-input': true,
        'el-select': true,
        'el-option': true,
        'el-checkbox': true,
        'el-radio-group': true,
        'el-radio': true,
        'el-tree-select': true
      }
    }
  })
  return { wrapper, $modal, $tab }
}

describe('GenEdit 生成配置编辑页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getGenTableMock.mockResolvedValue({
      data: {
        info: tableInfo(),
        rows: [{ columnId: 1, columnName: 'id', javaType: 'Long', isPk: '1' }],
        tables: [{ tableId: 10, tableName: 'blog_tag', tableComment: '标签表' }]
      }
    })
    updateGenTableMock.mockResolvedValue({ code: 200 })
    columnListMock.mockResolvedValue({ rows: [] })
    treeselectMock.mockResolvedValue({ data: [] })
  })

  it('应该导出 GenEdit 组件', () => {
    expect(EditTable).toBeDefined()
    expect(typeof EditTable).toBe('object')
  })

  it('应该按路由参数加载表信息、字段与上级菜单', async () => {
    mountView()
    await flushPromises()

    expect(getGenTableMock).toHaveBeenCalledWith(9)
    expect(treeselectMock).toHaveBeenCalled()
  })

  it('提交时应该把字段列表与树/菜单配置一起提交', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    await wrapper.findAll('button')[0].trigger('click')
    await flushPromises()

    expect(updateGenTableMock).toHaveBeenCalledTimes(1)
    const payload = updateGenTableMock.mock.calls[0][0]
    expect(payload.tableId).toBe(9)
    expect(payload.columns).toHaveLength(1)
    expect(payload.params).toMatchObject({ treeCode: undefined, parentMenuId: undefined })
  })

  it('树表缺少树编码字段时应该拦截提交', async () => {
    getGenTableMock.mockResolvedValue({
      data: { info: tableInfo({ tplCategory: 'tree', treeCode: '' }), rows: [], tables: [] }
    })
    const { wrapper, $modal } = mountView()
    await flushPromises()

    await wrapper.findAll('button')[0].trigger('click')
    await flushPromises()

    expect($modal.msgError).toHaveBeenCalledWith('树编码字段不能为空')
    expect(updateGenTableMock).not.toHaveBeenCalled()
  })

  it('提交成功后应该提示并返回代码生成列表', async () => {
    const { wrapper, $modal, $tab } = mountView()
    await flushPromises()

    await wrapper.findAll('button')[0].trigger('click')
    await flushPromises()

    expect($modal.msgSuccess).toHaveBeenCalledWith('修改成功')
    expect($tab.closeOpenPage).toHaveBeenCalledWith({
      path: '/admin/tool/gen',
      query: { pageNum: '2', t: expect.any(Number) }
    })
  })
})
