import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Contact from './index.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'

/**
 * 联系我们页「咨询表单」行为测试（挂载真实组件）。
 *
 * 表单复用留言提交接口（POST /api/message → zhi_message），由独立开关
 * `contact_form_enabled` 控制；结构化字段（公司/电话/咨询类型）并入正文，
 * 后台「留言管理」统一处理，因此这里重点验证：开关显隐、字段齐全、
 * 提交载荷（昵称/邮箱/正文拼装/验证码）。
 */
const addMessage = vi.fn()
const getCodeImg = vi.fn()

vi.mock('@/api/site/message', () => ({
  addMessage: (...args: unknown[]) => addMessage(...args),
  getMessageList: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getMessageCount: vi.fn().mockResolvedValue({ data: 0 })
}))

vi.mock('@/api/site/auth', () => ({
  getCodeImg: (...args: unknown[]) => getCodeImg(...args)
}))

vi.mock('@/utils/seo', () => ({ applySeo: vi.fn(), canonicalUrl: vi.fn(() => 'https://e.com') }))
vi.mock('@/utils/jsonld', () => ({ setJsonLd: vi.fn(), removeJsonLd: vi.fn() }))

const stubs = {
  SiteLayout: { template: '<div class="layout-stub"><slot /></div>' },
  'el-form': {
    template: '<form><slot /></form>',
    methods: { validate: () => Promise.resolve() }
  },
  // label 走属性而非插槽，桩要渲染出来才能断言字段名
  'el-form-item': {
    props: ['label'],
    template: '<div class="fi"><span class="fi-label">{{ label }}</span><slot /></div>'
  },
  'el-input': { props: ['modelValue'], template: '<input />' },
  'el-select': { template: '<div><slot /></div>' },
  'el-option': { template: '<div />' },
  'el-checkbox': { template: '<label><slot /></label>' },
  'el-button': { template: '<button type="button"><slot /></button>' }
}

async function mountContact(settings: Record<string, unknown>) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useSiteSettingsStore().updateSiteSettings(settings as never)
  const wrapper = mount(Contact, { global: { plugins: [pinia], stubs } })
  await flushPromises()
  return wrapper
}

describe('联系我们 · 咨询表单', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 默认验证码关闭，聚焦表单提交逻辑
    getCodeImg.mockResolvedValue({ captchaEnabled: false })
    addMessage.mockResolvedValue({ code: 200 })
  })

  it('开关开启时渲染完整表单字段', async () => {
    const wrapper = await mountContact({ contact_form_enabled: 'true' })

    const section = wrapper.find('#contact-form')
    expect(section.exists()).toBe(true)
    expect(section.text()).toContain('姓名')
    expect(section.text()).toContain('邮箱')
    expect(section.text()).toContain('咨询类型')
    expect(section.text()).toContain('需求描述')
    expect(section.text()).toContain('《隐私政策》')
    expect(section.text()).toContain('提交咨询')
  })

  it('开关关闭时不渲染表单（只保留联系方式与 CTA）', async () => {
    const wrapper = await mountContact({ contact_form_enabled: 'false' })

    expect(wrapper.find('#contact-form').exists()).toBe(false)
    expect(wrapper.find('.ct-form').exists()).toBe(false)
  })

  it('提交时拼接结构化字段并调用留言接口', async () => {
    const wrapper = await mountContact({ contact_form_enabled: 'true' })
    const vm = wrapper.vm as any

    vm.form.name = '张三'
    vm.form.email = 'zhangsan@example.com'
    vm.form.company = '示例科技'
    vm.form.phone = '13800000000'
    vm.form.consultType = '商务合作'
    vm.form.content = '想了解企业官网建设方案'
    vm.form.agree = true

    await vm.handleSubmit()
    await flushPromises()

    expect(addMessage).toHaveBeenCalledTimes(1)
    const payload = addMessage.mock.calls[0][0]
    expect(payload.nickname).toBe('张三')
    expect(payload.email).toBe('zhangsan@example.com')
    // 公司/电话/咨询类型并入正文，后台留言管理可直接看到
    expect(payload.content).toContain('【咨询表单】')
    expect(payload.content).toContain('公司：示例科技')
    expect(payload.content).toContain('电话：13800000000')
    expect(payload.content).toContain('咨询类型：商务合作')
    expect(payload.content).toContain('想了解企业官网建设方案')
    // 验证码关闭时不应携带 code/uuid
    expect(payload.code).toBeUndefined()
  })

  it('提交成功后展示成功态并可再次提交', async () => {
    const wrapper = await mountContact({ contact_form_enabled: 'true' })
    const vm = wrapper.vm as any

    vm.form.name = '李四'
    vm.form.email = 'lisi@example.com'
    vm.form.content = '需要一套官网设计方案'
    vm.form.agree = true

    await vm.handleSubmit()
    await flushPromises()

    expect(wrapper.find('.ct-form-success').exists()).toBe(true)
    expect(wrapper.find('.ct-form').exists()).toBe(false)

    vm.resetForm()
    await flushPromises()
    expect(wrapper.find('.ct-form').exists()).toBe(true)
  })
})

describe('联系我们 · 联系方式', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getCodeImg.mockResolvedValue({ captchaEnabled: false })
  })

  it('配置联系邮箱时展示 mailto 链接', async () => {
    const wrapper = await mountContact({ contact_email: 'sales@example.com' })

    const emailCard = wrapper.findAll('.ct-card').find(card => card.text().includes('联系邮箱'))

    expect(emailCard).toBeTruthy()
    expect(emailCard!.text()).toContain('sales@example.com')
    expect(emailCard!.find('a').attributes('href')).toBe('mailto:sales@example.com')
  })

  it('未配置邮箱时不渲染联系邮箱项', async () => {
    const wrapper = await mountContact({ contact_phone: '010-12345678' })

    expect(wrapper.text()).toContain('联系电话')
    expect(wrapper.text()).not.toContain('联系邮箱')
  })
})
