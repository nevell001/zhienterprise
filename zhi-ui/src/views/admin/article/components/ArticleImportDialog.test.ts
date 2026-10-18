import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ArticleImportDialog from './ArticleImportDialog.vue'
import { ElMessage } from '@/plugins/element-plus-service'

const importMarkdownArticles = vi.fn()
const listCategory = vi.fn()

vi.mock('@/api/admin/article', () => ({
  importMarkdownArticles: (...args: unknown[]) => importMarkdownArticles(...args)
}))

vi.mock('@/api/admin/category', () => ({
  listCategory: (...args: unknown[]) => listCategory(...args)
}))

vi.mock('@/plugins/element-plus-service', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() }
}))

const DialogStub = {
  props: ['modelValue'],
  template: '<div v-if="modelValue" class="dialog-stub"><slot /><slot name="footer" /></div>'
}
const UploadStub = {
  name: 'ElUpload',
  props: ['fileList'],
  emits: ['update:fileList', 'change', 'exceed'],
  template: '<div class="upload-stub"><slot /><slot name="tip" /></div>'
}
const SlotStub = {
  template: '<div><slot /></div>'
}
const ButtonStub = {
  props: ['disabled'],
  template: '<button type="button" :disabled="disabled"><slot /></button>'
}

function mountDialog(props: Record<string, unknown> = {}) {
  return mount(ArticleImportDialog, {
    props: { modelValue: true, articleType: '', ...props },
    global: {
      stubs: {
        'el-dialog': DialogStub,
        'el-upload': UploadStub,
        'el-button': ButtonStub,
        'el-form': SlotStub,
        'el-form-item': SlotStub,
        'el-select': SlotStub,
        'el-option': true,
        'el-radio-group': SlotStub,
        'el-radio': SlotStub,
        'el-icon': SlotStub,
        'upload-filled': true
      },
      directives: { hasPermi: {} }
    }
  })
}

const mdFile = (name = 'a.md') =>
  ({
    uid: 1,
    name,
    size: 100,
    raw: new File(['# 标题\n\n正文'], name, { type: 'text/markdown' })
  }) as any

function submitButton(wrapper: ReturnType<typeof mountDialog>) {
  return wrapper.findAll('button').find(btn => btn.text().includes('开始导入'))!
}

beforeEach(() => {
  vi.clearAllMocks()
  listCategory.mockResolvedValue({ rows: [{ id: 7, name: '行业资讯' }] })
})

describe('ArticleImportDialog 批量导入', () => {
  it('打开时加载分类列表', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    expect(listCategory).toHaveBeenCalled()
  })

  it('选择文件后提交：FormData 带上文件与选项，成功后关闭并通知父组件', async () => {
    importMarkdownArticles.mockResolvedValue({
      code: 200,
      data: {
        total: 1,
        imported: 1,
        failed: 0,
        results: [{ fileName: 'a.md', success: true, title: '标题', articleId: 10 }]
      }
    })

    const wrapper = mountDialog()
    await flushPromises()

    wrapper.findComponent(UploadStub).vm.$emit('update:fileList', [mdFile()])
    await wrapper.vm.$nextTick()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(importMarkdownArticles).toHaveBeenCalledTimes(1)
    const formData = importMarkdownArticles.mock.calls[0][0] as FormData
    expect(formData.get('files')).toBeInstanceOf(File)
    expect(formData.get('articleType')).toBe('news')
    expect(formData.get('status')).toBe('0')
    expect(ElMessage.success).toHaveBeenCalled()
    expect(wrapper.emitted('imported')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('部分失败时列出失败原因且不关闭对话框', async () => {
    importMarkdownArticles.mockResolvedValue({
      code: 200,
      data: {
        total: 2,
        imported: 1,
        failed: 1,
        results: [
          { fileName: 'ok.md', success: true, title: '成功篇' },
          { fileName: 'dup.md', success: false, message: '文章标题已存在' }
        ]
      }
    })

    const wrapper = mountDialog()
    await flushPromises()

    wrapper
      .findComponent(UploadStub)
      .vm.$emit('update:fileList', [mdFile('ok.md'), mdFile('dup.md')])
    await wrapper.vm.$nextTick()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('失败 1 个')
    expect(wrapper.text()).toContain('dup.md')
    expect(wrapper.text()).toContain('文章标题已存在')
    expect(wrapper.emitted('imported')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('固定栏目传入类型时按该栏目提交', async () => {
    importMarkdownArticles.mockResolvedValue({
      code: 200,
      data: { total: 1, imported: 1, failed: 0, results: [{ fileName: 'a.md', success: true }] }
    })

    const wrapper = mountDialog({ articleType: 'business' })
    await flushPromises()

    wrapper.findComponent(UploadStub).vm.$emit('update:fileList', [mdFile()])
    await wrapper.vm.$nextTick()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    const formData = importMarkdownArticles.mock.calls[0][0] as FormData
    expect(formData.get('articleType')).toBe('business')
  })

  it('超过 5MB 的文件被拦截且不发起请求', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    const big = {
      uid: 9,
      name: 'big.md',
      size: 6 * 1024 * 1024,
      raw: new File(['x'], 'big.md')
    } as any
    wrapper.findComponent(UploadStub).vm.$emit('update:fileList', [big])
    await wrapper.vm.$nextTick()

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(ElMessage.warning).toHaveBeenCalled()
    expect(importMarkdownArticles).not.toHaveBeenCalled()
  })
})
