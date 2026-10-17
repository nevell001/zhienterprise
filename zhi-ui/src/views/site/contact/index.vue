<template>
  <SiteLayout>
    <div ref="rootEl" class="tpl-site contact-page mo-home-page">
      <!-- 页头（文案可在后台「官网内容」页签调整） -->
      <header class="aph">
        <div class="eh-wrap">
          <span class="eyebrow rv">{{ heroEyebrow }}</span>
          <h1 class="aph-title rv">{{ heroTitle }}</h1>
          <p class="aph-sub rv d1">{{ heroSubtitle }}</p>
        </div>
      </header>

      <main>
        <!-- 基础联系信息（来自站点设置） -->
        <section class="pad">
          <div class="eh-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">联系方式</div>
              <h2>随时找到我们</h2>
            </div>
            <div v-if="basicItems.length" class="ct-grid rv d1">
              <div v-for="item in basicItems" :key="item.key" class="ct-card">
                <div class="ct-type">{{ item.label }}</div>
                <a v-if="item.href" :href="item.href" class="ct-value ct-link">{{ item.value }}</a>
                <div v-else class="ct-value">{{ item.value }}</div>
              </div>
            </div>
            <el-empty v-else description="暂未公开联系方式，可在后台「站点设置」中填写" />
          </div>
        </section>

        <!-- 分渠道咨询（渠道专属联系方式为占位，接入后替换） -->
        <section class="pad surface">
          <div class="eh-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">分渠道咨询</div>
              <h2>按需求选择对接渠道</h2>
              <p>以下渠道均可通过页面底部电话与邮箱联系，备注对应渠道即可精准触达。</p>
            </div>
            <div class="ct-grid ct-four rv d1">
              <div v-for="ch in channels" :key="ch.name" class="ct-card">
                <div class="ct-type">{{ ch.name }}</div>
                <p class="ct-desc">{{ ch.desc }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 咨询表单：提交写入「留言管理」（复用 /api/message，与留言板共用审核与冷却） -->
        <section v-if="contactFormEnabled" id="contact-form" class="pad">
          <div class="eh-wrap ct-form-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">在线咨询</div>
              <h2>告诉我们你的需求</h2>
              <p>填写后我们会在 1 个工作日内与你联系。</p>
            </div>

            <el-form
              v-if="!submitSuccess"
              ref="formRef"
              :model="form"
              :rules="rules"
              class="ct-form rv d1"
              label-position="top"
            >
              <div class="ct-form-grid">
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="form.name" placeholder="怎么称呼您" maxlength="50" />
                </el-form-item>
                <el-form-item label="公司">
                  <el-input
                    v-model="form.company"
                    placeholder="公司 / 组织名称（选填）"
                    maxlength="60"
                  />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="form.email" placeholder="name@example.com" maxlength="100" />
                </el-form-item>
                <el-form-item label="电话">
                  <el-input
                    v-model="form.phone"
                    placeholder="便于我们回电（选填）"
                    maxlength="30"
                  />
                </el-form-item>
                <el-form-item label="咨询类型" prop="consultType">
                  <el-select v-model="form.consultType" placeholder="请选择咨询类型">
                    <el-option v-for="t in consultTypes" :key="t" :label="t" :value="t" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="captchaEnabled" label="验证码" prop="code">
                  <div class="captcha-row">
                    <el-input v-model="form.code" placeholder="计算结果" maxlength="10" />
                    <img
                      :src="captchaUrl"
                      class="captcha-img"
                      alt="验证码"
                      title="点击刷新验证码"
                      @click="refreshCaptcha"
                    />
                  </div>
                </el-form-item>
              </div>

              <el-form-item label="需求描述" prop="content">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  :rows="4"
                  maxlength="300"
                  show-word-limit
                  placeholder="简要描述你的业务场景与诉求"
                />
              </el-form-item>

              <el-form-item prop="agree" class="ct-consent">
                <el-checkbox v-model="form.agree">
                  我已阅读并同意
                  <router-link to="/privacy" target="_blank">《隐私政策》</router-link>
                </el-checkbox>
              </el-form-item>

              <div class="ct-form-actions">
                <el-button
                  type="primary"
                  :loading="submitting"
                  :disabled="cooldownSeconds > 0"
                  @click="handleSubmit"
                >
                  {{ cooldownSeconds > 0 ? `${cooldownSeconds} 秒后可再次提交` : '提交咨询' }}
                </el-button>
                <span class="ct-form-hint">我们不会公开你的联系方式</span>
              </div>
            </el-form>

            <div v-else class="ct-form-success rv d1">
              <h3>已收到你的咨询</h3>
              <p>我们会在 1 个工作日内通过邮箱或电话与你联系。</p>
              <el-button plain @click="resetForm">再提交一条</el-button>
            </div>
          </div>
        </section>

        <!-- 地图占位（点击跳转外部地图搜索，第三方脚本延后加载，§6.1） -->
        <section v-if="siteSettings.contact_address" class="pad">
          <div class="eh-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">到访指引</div>
              <h2>我们的位置</h2>
            </div>
            <div class="ct-map rv d1">
              <svg
                class="ct-map-bg"
                viewBox="0 0 800 320"
                preserveAspectRatio="none"
                aria-hidden="true"
              >
                <g stroke="currentColor" stroke-width="1" fill="none">
                  <path d="M0 80h800M0 160h800M0 240h800M160 0v320M400 0v320M640 0v320" />
                </g>
                <circle cx="400" cy="160" r="10" fill="currentColor" />
                <circle
                  cx="400"
                  cy="160"
                  r="22"
                  stroke="currentColor"
                  stroke-width="1.5"
                  fill="none"
                />
              </svg>
              <div class="ct-map-body">
                <p>{{ siteSettings.contact_address }}</p>
                <a class="btn-p" :href="mapLink" target="_blank" rel="noopener noreferrer">
                  获取路线
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                    <path
                      d="M4 3l4 4-4 4"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </a>
              </div>
            </div>
          </div>
        </section>

        <!-- FAQ 折叠面板（方案文档 §3.6：手风琴，同时展开 1 项） -->
        <section class="pad surface">
          <div class="eh-wrap ct-faq-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">常见问题</div>
              <h2>你可能想问</h2>
            </div>
            <div class="ct-faq rv d1">
              <div v-for="(f, fi) in faqs" :key="f.q" class="ct-faq-item">
                <button
                  type="button"
                  class="ct-faq-q"
                  :aria-expanded="openFaq === fi"
                  @click="openFaq = openFaq === fi ? -1 : fi"
                >
                  {{ f.q }}
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 14 14"
                    fill="none"
                    aria-hidden="true"
                    :style="{ transform: openFaq === fi ? 'rotate(180deg)' : 'none' }"
                  >
                    <path
                      d="M3 5l4 4 4-4"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </button>
                <p v-show="openFaq === fi" class="ct-faq-a">{{ f.a }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 留言入口（询盘表单后置：使用现有留言板承接） -->
        <section class="pad">
          <div class="eh-wrap ct-cta rv">
            <h2>有任何合作意向或问题？</h2>
            <p>给我们留言，将在 1 个工作日内回复。</p>
            <router-link v-if="isGuestbookEnabled" class="btn-p" to="/guestbook">
              前往留言板
            </router-link>
          </div>
        </section>
      </main>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import SiteLayout from '@/components/SiteLayout.vue'
import { ElMessage } from '@/plugins/element-plus-service'
import type { FormInstance, FormRules } from 'element-plus'
import { addMessage } from '@/api/site/message'
import { getCodeImg } from '@/api/site/auth'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { setJsonLd } from '@/utils/jsonld'
import { getSiteChannels, getSiteFaqs, DEFAULT_PAGE_COPY, getPageCopy } from '@/utils/siteContent'
import { useScrollReveal } from '@/composables/useScrollReveal'

interface ContactItem {
  key: string
  label: string
  value: string
  href?: string
}

const siteSettingsStore = useSiteSettingsStore()
const siteSettings = computed(() => siteSettingsStore.siteSettings)
const isGuestbookEnabled = computed(() => siteSettingsStore.isFeatureEnabled('guestbook_enabled'))

/* ---------- 咨询表单 ----------
   复用留言提交接口（POST /api/message）：数据写入 zhi_message，
   后台「留言管理」统一处理；开关 contact_form_enabled 独立于留言板。 */
const contactFormEnabled = computed(() =>
  siteSettingsStore.isFeatureEnabled('contact_form_enabled')
)
const consultTypes = ['产品咨询', '服务咨询', '商务合作', '媒体采访', '其他']
const SUBMIT_COOLDOWN_SECONDS = 60

const formRef = ref<FormInstance>()
const submitting = ref(false)
const submitSuccess = ref(false)
const captchaEnabled = ref(true)
const captchaUrl = ref('')
const cooldownSeconds = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | undefined

const form = reactive({
  name: '',
  company: '',
  email: '',
  phone: '',
  consultType: '产品咨询',
  content: '',
  code: '',
  uuid: '',
  agree: false
})

const rules = computed<FormRules>(() => ({
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名长度不能超过50个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  consultType: [{ required: true, message: '请选择咨询类型', trigger: 'change' }],
  content: [
    { required: true, message: '请填写需求描述', trigger: 'blur' },
    { min: 5, message: '需求描述至少 5 个字符', trigger: 'blur' },
    { max: 300, message: '需求描述长度不能超过300个字符', trigger: 'blur' }
  ],
  agree: [
    {
      validator: (_rule, value, callback) =>
        value ? callback() : callback(new Error('请先阅读并同意隐私政策')),
      trigger: 'change'
    }
  ],
  ...(captchaEnabled.value
    ? { code: [{ required: true, message: '请输入验证码', trigger: 'blur' }] }
    : {})
}))

const refreshCaptcha = async () => {
  if (!contactFormEnabled.value) return
  try {
    const res = await getCodeImg()
    captchaEnabled.value = res?.captchaEnabled === undefined ? true : Boolean(res.captchaEnabled)
    if (captchaEnabled.value) {
      captchaUrl.value = 'data:image/jpeg;base64,' + res.img
      form.uuid = res.uuid
      form.code = ''
    } else {
      captchaUrl.value = ''
      form.uuid = ''
      form.code = ''
    }
  } catch {
    captchaEnabled.value = false
  }
}

const clearCooldownTimer = () => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = undefined
  }
}

const startCooldown = (seconds: number) => {
  clearCooldownTimer()
  cooldownSeconds.value = Math.max(0, Math.floor(seconds) || 0)
  if (cooldownSeconds.value === 0) return
  cooldownTimer = setInterval(() => {
    cooldownSeconds.value -= 1
    if (cooldownSeconds.value <= 0) {
      cooldownSeconds.value = 0
      clearCooldownTimer()
    }
  }, 1000)
}

/** 组装提交内容：把表单结构化字段并入留言正文（复用留言表，无需新增列） */
const buildContent = () => {
  const details: string[] = []
  if (form.company.trim()) details.push(`公司：${form.company.trim()}`)
  if (form.phone.trim()) details.push(`电话：${form.phone.trim()}`)
  details.push(`咨询类型：${form.consultType}`)
  return `【咨询表单】\n${details.join('\n')}\n\n${form.content.trim()}`
}

const handleSubmit = async () => {
  if (!formRef.value || submitting.value || cooldownSeconds.value > 0) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await addMessage({
      nickname: form.name.trim(),
      email: form.email.trim(),
      content: buildContent(),
      ...(captchaEnabled.value ? { code: form.code.trim(), uuid: form.uuid } : {})
    })
    submitSuccess.value = true
    ElMessage.success('咨询已提交，我们会尽快与你联系')
    startCooldown(SUBMIT_COOLDOWN_SECONDS)
  } catch (error: any) {
    const msg = error?.msg || error?.message || '提交失败，请稍后重试'
    ElMessage.error(msg)
    const wait = /请\s*(\d+)\s*秒后再试/.exec(String(msg))
    if (wait) startCooldown(Number(wait[1]))
    // 验证码一次性使用，失败后必须刷新
    if (captchaEnabled.value) refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  submitSuccess.value = false
  form.content = ''
  if (captchaEnabled.value) refreshCaptcha()
}

watch(contactFormEnabled, enabled => {
  if (enabled && !captchaUrl.value && !submitSuccess.value) refreshCaptcha()
})

/* 页头文案：「官网内容」页签的页面文案 → 内置文案 */
const pageCopy = computed(() => getPageCopy(siteSettings.value?.site_page_copy, 'contact'))
const heroEyebrow = computed(() => pageCopy.value.eyebrow || DEFAULT_PAGE_COPY.contact.eyebrow)
const heroTitle = computed(() => pageCopy.value.title || DEFAULT_PAGE_COPY.contact.title)
const heroSubtitle = computed(() => pageCopy.value.subtitle || DEFAULT_PAGE_COPY.contact.subtitle)

/* 分渠道与 FAQ：管理后台「官网内容」页签维护（缺失回退内置占位） */
const channels = computed(() => getSiteChannels(siteSettings.value?.site_channels))
const faqs = computed(() => getSiteFaqs(siteSettings.value?.site_faq))

const openFaq = ref(-1)

const basicItems = computed<ContactItem[]>(() => {
  const items: ContactItem[] = []
  if (siteSettings.value.contact_phone) {
    items.push({
      key: 'phone',
      label: '联系电话',
      value: siteSettings.value.contact_phone,
      href: `tel:${siteSettings.value.contact_phone.replace(/\s/g, '')}`
    })
  }
  if (siteSettings.value.contact_address) {
    items.push({
      key: 'address',
      label: '办公地址',
      value: siteSettings.value.contact_address
    })
  }
  return items
})

const mapLink = computed(() => {
  const addr = encodeURIComponent(siteSettings.value.contact_address || '')
  return `https://www.amap.com/search?query=${addr}`
})

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

// FAQ 结构化数据（方案文档 §6.2）：设置异步加载完成后同步刷新
const updateFaqJsonLd = () => {
  setJsonLd('zh-jsonld-faqpage', {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: faqs.value.map(f => ({
      '@type': 'Question',
      name: f.q,
      acceptedAnswer: { '@type': 'Answer', text: f.a }
    }))
  })
}

watch(faqs, updateFaqJsonLd)

const applyPageSeo = () => {
  const siteName = siteSettings.value.blog_name || '知企建站系统'
  applySeo({
    title: `${heroTitle.value} - ${siteName}`,
    description: siteSettings.value.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
}

watch(heroTitle, applyPageSeo, { immediate: true })

onMounted(() => {
  if (contactFormEnabled.value) refreshCaptcha()
  updateFaqJsonLd()
})

onUnmounted(() => {
  clearCooldownTimer()
})
</script>

<style scoped>
@import '../about-shared.css';

.ct-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-6);
}

.ct-grid.ct-four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.ct-card {
  padding: var(--mo-sp-7);
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  background: var(--tpl-bg);
}

.ct-type {
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
  margin-bottom: var(--mo-sp-3);
}

.ct-value {
  color: var(--tpl-ink-900);
  font-size: var(--mo-fs-md);
  font-weight: 600;
  word-break: break-all;
}

.ct-link:hover {
  color: var(--tpl-p-text);
}

.ct-desc {
  margin: 0;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-500);
  line-height: 1.7;
}

/* ---------- 地图占位 ---------- */
.ct-map {
  position: relative;
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  overflow: hidden;
  background: var(--tpl-surface);
  color: var(--tpl-border);
  box-shadow: var(--tpl-sh-sm);
}

.ct-map-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.ct-map-body {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mo-sp-6);
  padding: var(--mo-sp-9) var(--mo-sp-8);
  flex-wrap: wrap;
}

.ct-map-body p {
  margin: 0;
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-700);
  font-weight: 500;
}

/* ---------- 按钮 ---------- */
.btn-p {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-2);
  min-height: 48px;
  padding: 0 var(--mo-sp-6);
  border-radius: var(--tpl-r-md);
  background: var(--tpl-p);
  color: var(--tpl-white);
  font-size: var(--mo-fs-md);
  font-weight: 600;
  text-decoration: none;
  transition:
    transform 0.2s cubic-bezier(0.16, 1, 0.3, 1),
    background 0.2s,
    box-shadow 0.2s;
}

.btn-p:hover {
  background: var(--tpl-p-h);
  transform: translateY(-2px);
  box-shadow: var(--tpl-sh-md);
}

/* ---------- 咨询表单 ---------- */
.ct-form-wrap {
  max-width: 820px;
}

.ct-form {
  padding: var(--mo-sp-7);
  background: var(--tpl-bg);
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
}

.ct-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--mo-sp-5);
}

.ct-form :deep(.el-form-item__label) {
  color: var(--tpl-ink-700);
  font-weight: 600;
}

.ct-form :deep(.el-input__wrapper),
.ct-form :deep(.el-textarea__inner) {
  background: var(--tpl-bg);
  border-radius: var(--tpl-r-md);
  box-shadow: 0 0 0 1px var(--tpl-border) inset;
}

.ct-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--tpl-p) inset,
    0 0 0 3px color-mix(in srgb, var(--tpl-p) 14%, transparent);
}

.ct-form .captcha-row {
  display: flex;
  gap: var(--mo-sp-2);
  align-items: center;
  width: 100%;
}

.ct-form .captcha-row :deep(.el-input) {
  flex: 1;
}

.ct-form .captcha-img {
  width: 106px;
  height: 34px;
  cursor: pointer;
  object-fit: cover;
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-sm);
}

.ct-consent {
  margin-bottom: var(--mo-sp-4);
}

.ct-consent :deep(.el-checkbox__label) {
  color: var(--tpl-ink-500);
  font-size: var(--mo-fs-sm);
}

.ct-consent a {
  color: var(--tpl-p-text);
  text-decoration: none;
}

.ct-consent a:hover {
  text-decoration: underline;
}

.ct-form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mo-sp-4);
  align-items: center;
}

.ct-form-actions :deep(.el-button--primary) {
  min-height: 44px;
  padding: 0 var(--mo-sp-6);
  color: var(--tpl-white);
  background: var(--tpl-p);
  border: none;
  border-radius: var(--tpl-r-md);
}

.ct-form-actions :deep(.el-button--primary:hover) {
  background: var(--tpl-p-h);
}

.ct-form-hint {
  color: var(--tpl-ink-400);
  font-size: var(--mo-fs-xs);
}

.ct-form-success {
  padding: var(--mo-sp-8) var(--mo-sp-7);
  text-align: center;
  background: var(--tpl-surface);
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
}

.ct-form-success h3 {
  margin: 0 0 var(--mo-sp-2);
  color: var(--tpl-ink-900);
  font-size: var(--mo-fs-xl);
}

.ct-form-success p {
  margin: 0 0 var(--mo-sp-5);
  color: var(--tpl-ink-500);
  font-size: var(--mo-fs-sm);
}

/* ---------- FAQ ---------- */
.ct-faq-wrap {
  max-width: 760px;
}

.ct-faq {
  border-top: 1px solid var(--tpl-border);
}

.ct-faq-item {
  border-bottom: 1px solid var(--tpl-border);
}

.ct-faq-q {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mo-sp-4);
  padding: var(--mo-sp-5) 0;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font-size: var(--mo-fs-md);
  font-weight: 600;
  color: var(--tpl-ink-900);
  font-family: inherit;
}

.ct-faq-q svg {
  flex: none;
  color: var(--tpl-ink-400);
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.ct-faq-a {
  margin: 0 0 var(--mo-sp-5);
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-500);
  line-height: 1.75;
}

/* ---------- CTA ---------- */
.ct-cta {
  text-align: center;
  padding: var(--mo-sp-9) var(--mo-sp-7);
  border-radius: var(--tpl-r-xl);
  background:
    radial-gradient(circle at 80% 18%, rgba(255, 255, 255, 0.14), transparent 40%), var(--tpl-dark);
}

.ct-cta h2 {
  margin: 0 0 var(--mo-sp-3);
  color: var(--tpl-white);
  font-size: var(--mo-fs-3xl);
}

.ct-cta p {
  margin: 0 0 var(--mo-sp-6);
  color: var(--tpl-dark-dim);
  font-size: var(--mo-fs-md);
}

.ct-cta .btn-p {
  align-self: center;
}

.ct-cta {
  display: flex;
  flex-direction: column;
  align-items: center;
}

@media (max-width: 991px) {
  .ct-grid,
  .ct-grid.ct-four {
    grid-template-columns: 1fr 1fr;
  }

  .eh-wrap {
    padding: 0 var(--mo-sp-5);
  }

  .pad {
    padding: var(--mo-sp-11) 0;
  }

  .aph {
    padding: calc(var(--mo-sp-10) + 64px) 0 var(--mo-sp-9);
  }

  .aph-title {
    font-size: var(--mo-fs-5xl);
  }
}

@media (max-width: 639px) {
  .ct-grid,
  .ct-grid.ct-four {
    grid-template-columns: 1fr;
  }

  .ct-form {
    padding: var(--mo-sp-5) var(--mo-sp-4);
  }

  .ct-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
