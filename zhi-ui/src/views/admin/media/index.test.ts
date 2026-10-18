import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import MediaManage from './index.vue'

/**
 * 后台「媒体管理」行为测试：上传入口、引用检查（被引用时阻止删除）、
 * 无引用时确认删除。表格行内按钮依赖 el-table 作用域插槽，桩无法还原，
 * 行渲染交由浏览器验证。
 */
const {
  listMedia,
  delMedia,
  getMediaReferences,
  uploadMedia,
  msgSuccess,
  msgError,
  alert,
  confirm
} = vi.hoisted(() => ({
  listMedia: vi.fn(),
  delMedia: vi.fn(),
  getMediaReferences: vi.fn(),
  uploadMedia: vi.fn(),
  msgSuccess: vi.fn(),
  msgError: vi.fn(),
  alert: vi.fn(),
  confirm: vi.fn()
}))

vi.mock('@/api/admin/media', () => ({
  listMedia: (...args: unknown[]) => listMedia(...args),
  delMedia: (...args: unknown[]) => delMedia(...args),
  getMediaReferences: (...args: unknown[]) => getMediaReferences(...args),
  uploadMedia: (...args: unknown[]) => uploadMedia(...args)
}))

vi.mock('@/plugins/element-plus-service', () => ({
  ElMessage: { success: msgSuccess, error: msgError },
  ElMessageBox: { alert, confirm }
}))

type MediaVm = {
  beforeUpload: (file: File) => boolean
  handleUpload: (options: { file: File }) => Promise<void>
  handleDelete: (row: { id: number }) => Promise<void>
}

function mountView() {
  return mount(MediaManage, {
    global: {
      directives: {
        hasPermi: {},
        loading: {}
      },
      stubs: {
        pagination: true,
        'el-form': { template: '<form><slot /></form>' },
        'el-form-item': { template: '<div class="fi"><slot /></div>' },
        'el-select': { template: '<div><slot /></div>' },
        'el-option': true,
        'el-input': { props: ['modelValue'], template: '<input :value="modelValue" />' },
        'el-button': { template: '<button><slot /></button>' },
        'el-upload': { template: '<div class="upload"><slot /></div>' },
        'el-table': { props: ['data'], template: '<div class="table"><slot /></div>' },
        'el-table-column': { props: ['label'], template: '<div class="col">{{ label }}</div>' },
        'el-tag': { template: '<span><slot /></span>' },
        'el-icon': true
      }
    }
  })
}

function collectText(node: any): string {
  if (!node || typeof node !== 'object') return ''
  if (typeof node.children === 'string') return node.children
  if (Array.isArray(node.children)) return node.children.map(collectText).join(' ')
  return ''
}

describe('媒体管理视图', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listMedia.mockResolvedValue({ rows: [], total: 0 })
    getMediaReferences.mockResolvedValue({ data: [] })
    confirm.mockResolvedValue('confirm')
    alert.mockResolvedValue('confirm')
    delMedia.mockResolvedValue({})
    uploadMedia.mockResolvedValue({})
  })

  it('被引用的文件拒绝删除并列出引用位置', async () => {
    getMediaReferences.mockResolvedValue({
      data: [
        { referenceType: 'article', referenceId: 10, referenceTitle: '关于我们' },
        { referenceType: 'setting', referenceId: 20, referenceTitle: 'site_logo' }
      ]
    })
    const wrapper = mountView()
    await flushPromises()

    await (wrapper.vm as unknown as MediaVm).handleDelete({ id: 7 })

    expect(alert).toHaveBeenCalledTimes(1)
    const [message, title] = alert.mock.calls[0]
    expect(title).toBe('无法删除')
    const text = collectText(message)
    expect(text).toContain('内容：关于我们')
    expect(text).toContain('站点设置：site_logo')
    expect(delMedia).not.toHaveBeenCalled()
    expect(confirm).not.toHaveBeenCalled()
  })

  it('无引用的文件确认后删除并刷新列表', async () => {
    getMediaReferences.mockResolvedValue({ data: [] })
    const wrapper = mountView()
    await flushPromises()

    await (wrapper.vm as unknown as MediaVm).handleDelete({ id: 7 })

    expect(confirm).toHaveBeenCalledTimes(1)
    expect(delMedia).toHaveBeenCalledWith(7)
    expect(msgSuccess).toHaveBeenCalledWith('删除成功')
    expect(listMedia).toHaveBeenCalledTimes(2)
  })

  it('上传成功后刷新列表', async () => {
    const wrapper = mountView()
    await flushPromises()
    const file = new File(['x'], 'logo.png', { type: 'image/png' })

    await (wrapper.vm as unknown as MediaVm).handleUpload({ file })

    expect(uploadMedia).toHaveBeenCalledWith(file)
    expect(msgSuccess).toHaveBeenCalledWith('上传成功')
    expect(listMedia).toHaveBeenCalledTimes(2)
  })

  it('上传前校验扩展名与 5MB 大小', async () => {
    const wrapper = mountView()
    await flushPromises()
    const vm = wrapper.vm as unknown as MediaVm

    expect(vm.beforeUpload(new File(['x'], 'evil.exe'))).toBe(false)
    expect(msgError).toHaveBeenCalledWith(expect.stringContaining('文件格式不支持'))

    const oversized = new File(['x'], 'big.png', { type: 'image/png' })
    Object.defineProperty(oversized, 'size', { value: 6 * 1024 * 1024 })
    expect(vm.beforeUpload(oversized)).toBe(false)
    expect(msgError).toHaveBeenCalledWith('单个文件大小不能超过 5 MB')

    expect(vm.beforeUpload(new File(['x'], 'ok.png', { type: 'image/png' }))).toBe(true)
  })
})
