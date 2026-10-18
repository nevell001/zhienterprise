import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ArticleRevisionDrawer from './ArticleRevisionDrawer.vue'
import { ElMessageBox } from '@/plugins/element-plus-service'

const listArticleRevisions = vi.fn()
const getArticleRevision = vi.fn()
const restoreArticleRevision = vi.fn()

vi.mock('@/api/admin/article', () => ({
  listArticleRevisions: (...args: unknown[]) => listArticleRevisions(...args),
  getArticleRevision: (...args: unknown[]) => getArticleRevision(...args),
  restoreArticleRevision: (...args: unknown[]) => restoreArticleRevision(...args)
}))

vi.mock('@/plugins/element-plus-service', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve('confirm')) }
}))

const revision = (id: number, revisionNo: number, title: string) => ({
  id,
  articleId: 1,
  revisionNo,
  title,
  editorName: '编辑',
  createTime: '2026-10-18 10:00:00'
})

const DrawerStub = {
  props: ['modelValue'],
  template: '<div v-if="modelValue" class="drawer-stub"><slot /></div>'
}
const DialogStub = {
  props: ['modelValue'],
  template: '<div v-if="modelValue" class="dialog-stub"><slot /></div>'
}
const ButtonStub = {
  template: '<button type="button"><slot /></button>'
}

function mountDrawer() {
  return mount(ArticleRevisionDrawer, {
    props: { modelValue: true, articleId: 1 },
    global: {
      stubs: {
        'el-drawer': DrawerStub,
        'el-dialog': DialogStub,
        'el-button': ButtonStub,
        'el-empty': {
          props: ['description'],
          template: '<div class="empty-stub">{{ description }}</div>'
        }
      },
      directives: { hasPermi: {}, loading: {} }
    }
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  listArticleRevisions.mockResolvedValue({
    code: 200,
    data: [revision(2, 2, '第二版'), revision(1, 1, '第一版')]
  })
  getArticleRevision.mockResolvedValue({
    code: 200,
    data: { ...revision(2, 2, '第二版'), content: '<p>旧内容</p><script>alert(1)</script>' }
  })
  restoreArticleRevision.mockResolvedValue({ code: 200 })
})

describe('ArticleRevisionDrawer 修订历史', () => {
  it('打开时按文章加载版本列表', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    expect(listArticleRevisions).toHaveBeenCalledWith(1)
    expect(wrapper.findAll('.rev-item')).toHaveLength(2)
    expect(wrapper.text()).toContain('v2 · 第二版')
    expect(wrapper.text()).toContain('编辑')
  })

  it('查看版本时加载正文并清理危险标签', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.find('.rev-view').trigger('click')
    await flushPromises()

    expect(getArticleRevision).toHaveBeenCalledWith(2)
    const content = wrapper.find('.rev-detail-content')
    expect(content.text()).toContain('旧内容')
    expect(content.html()).not.toContain('<script')
  })

  it('恢复版本需确认，成功后通知父组件并刷新列表', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.find('.rev-restore').trigger('click')
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(restoreArticleRevision).toHaveBeenCalledWith(2)
    expect(wrapper.emitted('restored')).toHaveLength(1)
    expect(listArticleRevisions).toHaveBeenCalledTimes(2)
  })

  it('取消确认时不调用恢复接口', async () => {
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.find('.rev-restore').trigger('click')
    await flushPromises()

    expect(restoreArticleRevision).not.toHaveBeenCalled()
    expect(wrapper.emitted('restored')).toBeUndefined()
  })
})
