<template>
  <BlogLayout>
    <div class="guestbook-page mo-guestbook-page">
      <div class="guestbook-shell">
        <header class="guestbook-panel guestbook-hero">
          <span class="section-label">Guestbook</span>
          <h1 class="guestbook-title">留言板</h1>
          <p class="guestbook-desc">
            {{
              guestbookEnabled
                ? '欢迎在这里留下你的想法、建议或问候，看到后我会尽快回复。'
                : '博主已关闭留言板，欢迎通过邮件或文章评论区与我交流。'
            }}
          </p>
          <p v-if="guestbookEnabled" class="guestbook-count">共 {{ messageCount }} 条留言</p>
        </header>

        <section v-if="!guestbookEnabled" class="guestbook-panel guestbook-closed">
          <el-empty description="本站暂未开放留言板" />
        </section>

        <section v-else class="guestbook-panel guestbook-form-panel">
          <div class="section-head">
            <span class="section-label">Message</span>
            <h2>写下留言</h2>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
            <div class="form-grid">
              <el-form-item label="昵称" prop="nickname">
                <el-input
                  v-model="form.nickname"
                  placeholder="怎么称呼你？"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="邮箱（选填）" prop="email">
                <el-input
                  v-model="form.email"
                  placeholder="用于接收回复（不会公开）"
                  maxlength="100"
                />
              </el-form-item>
              <el-form-item label="网站（选填）" prop="website" class="form-grid-full">
                <el-input
                  v-model="form.website"
                  placeholder="https://example.com"
                  maxlength="255"
                />
              </el-form-item>
            </div>

            <el-form-item label="留言内容" prop="content">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="5"
                placeholder="说点什么吧…"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>

            <el-form-item v-if="captchaEnabled" label="验证码" prop="code">
              <div class="captcha-row">
                <el-input
                  v-model="form.code"
                  placeholder="请输入验证码"
                  maxlength="10"
                  @keyup.enter="handleSubmit"
                />
                <img
                  :src="captchaUrl"
                  class="captcha-img"
                  alt="验证码"
                  title="点击刷新验证码"
                  @click="refreshCaptcha"
                />
              </div>
            </el-form-item>

            <div class="form-actions">
              <span class="form-hint">留言需间隔 {{ SUBMIT_COOLDOWN_SECONDS }} 秒</span>
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="cooldownSeconds > 0"
                @click="handleSubmit"
              >
                {{ cooldownSeconds > 0 ? `${cooldownSeconds} 秒后可再次留言` : '提交留言' }}
              </el-button>
            </div>
          </el-form>
        </section>

        <section v-if="guestbookEnabled" class="guestbook-panel guestbook-list-panel">
          <div class="section-head">
            <span class="section-label">Messages</span>
            <h2>全部留言</h2>
          </div>

          <div v-loading="loading" class="message-body">
            <template v-if="messages.length > 0">
              <ul class="message-list">
                <li v-for="item in messages" :key="item.id" class="message-item">
                  <div class="message-head">
                    <span class="message-nickname">{{ item.nickname }}</span>
                    <a
                      v-if="item.website"
                      :href="formatUrl(item.website)"
                      target="_blank"
                      rel="noopener noreferrer nofollow"
                      class="message-website"
                    >
                      访问网站
                    </a>
                    <span class="message-time">{{ formatTime(item.createTime) }}</span>
                  </div>
                  <p class="message-content">{{ item.content }}</p>
                  <div v-if="item.replyContent" class="message-reply">
                    <span class="reply-label">管理员回复</span>
                    <p class="reply-content">{{ item.replyContent }}</p>
                    <span v-if="item.replyTime" class="reply-time">
                      {{ formatTime(item.replyTime) }}
                    </span>
                  </div>
                </li>
              </ul>

              <pagination
                v-show="total > 0"
                v-model:page="pageNum"
                v-model:limit="pageSize"
                :total="total"
                class="guestbook-pagination"
                @pagination="handlePagination"
              />
            </template>

            <el-empty v-else-if="!loading" description="还没有留言，来抢沙发吧" />
          </div>
        </section>
      </div>
    </div>
  </BlogLayout>
</template>

<script setup lang="ts" name="PublicBlogGuestbook">
import BlogLayout from '@/components/BlogLayout.vue'
import { ElMessage } from '@/plugins/element-plus-service'
import type { FormInstance, FormRules } from 'element-plus'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { getBlogSettingsAnonymous } from '@/api/blog/setting'
import { getCodeImg } from '@/api/blog/auth'
import { addMessage, getMessageCount, getMessageList, type BlogMessage } from '@/api/blog/message'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { logger } from '@/utils/logger'

/** 与后端 BlogFrontMessageController.SUBMIT_INTERVAL_SECONDS 保持一致 */
const SUBMIT_COOLDOWN_SECONDS = 60

const blogSettingsStore = useBlogSettingsStore()
const blogSettings = computed(() => blogSettingsStore.blogSettings)

const formRef = ref<FormInstance>()
const submitting = ref(false)
const loading = ref(false)
const captchaEnabled = ref(true)
const captchaUrl = ref('')
const cooldownSeconds = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | undefined

const messages = ref<BlogMessage[]>([])
const messageCount = ref(0)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const form = reactive({
  nickname: '',
  email: '',
  website: '',
  content: '',
  code: '',
  uuid: ''
})

const rules = computed<FormRules>(() => ({
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 50, message: '昵称长度不能超过50个字符', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  content: [
    { required: true, message: '请输入留言内容', trigger: 'blur' },
    { max: 500, message: '留言内容长度不能超过500个字符', trigger: 'blur' }
  ],
  ...(captchaEnabled.value
    ? { code: [{ required: true, message: '请输入验证码', trigger: 'blur' }] }
    : {})
}))

// comment_review 走 store 统一判定：未配置时按后端语义视为“需要审核”
const needsReview = computed(() => blogSettingsStore.isFeatureEnabled('comment_review'))

// guestbook_enabled 关闭时页面只展示提示，不发起留言相关的请求
const guestbookEnabled = computed(() => blogSettingsStore.isFeatureEnabled('guestbook_enabled'))

const formatUrl = (url?: string | null) => {
  if (!url || typeof url !== 'string') return ''
  return /^https?:\/\//i.test(url) ? url : 'https://' + url
}

const formatTime = (time?: string | null) => {
  if (!time) return ''
  const date = new Date(String(time).replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return String(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadBlogSettings = async () => {
  try {
    const response = await getBlogSettingsAnonymous()
    const settings = response?.data || {}
    blogSettingsStore.updateBlogSettings(settings)
  } catch (error) {
    logger.error('加载博客设置失败:', error)
  }
}

const loadMessages = async () => {
  loading.value = true
  try {
    const response = await getMessageList({ pageNum: pageNum.value, pageSize: pageSize.value })
    messages.value = Array.isArray(response?.rows) ? response.rows : []
    total.value = Number(response?.total) || 0
  } catch (error) {
    logger.error('加载留言列表失败:', error)
    messages.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadMessageCount = async () => {
  try {
    const response = await getMessageCount()
    const count = Number(response?.data)
    messageCount.value = Number.isFinite(count) && count > 0 ? count : 0
  } catch (error) {
    logger.error('加载留言总数失败:', error)
    messageCount.value = 0
  }
}

const handlePagination = (payload: { page?: number; limit?: number }) => {
  pageNum.value = Number(payload?.page) || 1
  pageSize.value = Number(payload?.limit) || pageSize.value
  loadMessages()
}

// 刷新验证码（验证码是否启用与登录/注册同一开关，由后端下发）
const refreshCaptcha = async () => {
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
  } catch (error) {
    logger.error('获取验证码失败:', error)
    captchaEnabled.value = false
  }
}

const clearCooldownTimer = () => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = undefined
  }
}

// 提交冷却倒计时（服务端同样校验，此处仅用于提前禁用按钮）
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

const handleSubmit = async () => {
  if (!formRef.value || cooldownSeconds.value > 0) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await addMessage({
      nickname: form.nickname.trim(),
      content: form.content.trim(),
      email: form.email.trim() || undefined,
      website: form.website.trim() || undefined,
      ...(captchaEnabled.value ? { code: form.code.trim(), uuid: form.uuid } : {})
    })
    ElMessage.success(needsReview.value ? '留言已提交，审核通过后展示' : '留言成功')
    form.content = ''
    if (captchaEnabled.value) {
      refreshCaptcha()
    }
    startCooldown(SUBMIT_COOLDOWN_SECONDS)
    pageNum.value = 1
    await Promise.all([loadMessages(), loadMessageCount()])
  } catch (error: any) {
    // 后端 msg（验证码错误/过期、提交过快、冷却中）由响应拦截器转为 error.message
    const msg = error?.msg || error?.message || '留言提交失败，请稍后重试'
    ElMessage.error(msg)
    const waitSeconds = /请\s*(\d+)\s*秒后再试/.exec(String(msg))
    if (waitSeconds) {
      startCooldown(Number(waitSeconds[1]))
    }
    // 验证码一次性使用，失败后必须刷新
    if (captchaEnabled.value) {
      refreshCaptcha()
    }
  } finally {
    submitting.value = false
  }
}

const applyGuestbookSeo = () => {
  const siteName = (blogSettings.value as { blog_name?: string }).blog_name || '企业官网项目'
  applySeo({
    title: `留言板 - ${siteName}`,
    description: '欢迎在留言板留下你的想法、建议或问候。',
    canonical: canonicalUrl()
  })
}

onMounted(async () => {
  await loadBlogSettings()
  applyGuestbookSeo()
  if (!guestbookEnabled.value) return
  refreshCaptcha()
  loadMessages()
  loadMessageCount()
})

onUnmounted(() => {
  clearCooldownTimer()
})
</script>

<style scoped>
.mo-guestbook-page {
  min-height: 100vh;
  padding: 84px 24px 72px;
  background: var(--mo-n50);
  color: var(--mo-n800);
  font-family: var(--mo-font-sans);
}

.guestbook-shell {
  width: min(880px, 100%);
  margin: 0 auto;
  display: grid;
  gap: var(--mo-sp-5);
}

.guestbook-panel {
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: #fff;
  box-shadow: var(--mo-shadow-sm);
}

.guestbook-hero {
  padding: var(--mo-sp-7);
}

.section-label {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 4px 10px;
  border-radius: var(--mo-r-full);
  background: var(--mo-p50);
  color: var(--mo-p700);
  font-size: var(--mo-fs-xs);
  font-weight: 700;
}

.guestbook-title {
  margin: 0 0 10px;
  color: var(--mo-n900);
  font-family: var(--mo-font-serif);
  font-size: var(--mo-fs-4xl);
  font-weight: 700;
  line-height: 1.2;
}

.guestbook-desc {
  margin: 0;
  color: var(--mo-n600);
  font-size: 15px;
  line-height: 1.8;
}

.guestbook-count {
  margin: var(--mo-sp-3) 0 0;
  color: var(--mo-p700);
  font-size: 13px;
  font-weight: 600;
}

.guestbook-form-panel,
.guestbook-list-panel {
  padding: 28px 32px 32px;
}

.section-head {
  margin-bottom: var(--mo-sp-5);
}

.section-head h2 {
  margin: 0;
  color: var(--mo-n900);
  font-family: var(--mo-font-serif);
  font-size: 22px;
  font-weight: 700;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--mo-sp-4);
}

.form-grid-full {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--mo-sp-3);
}

.form-hint {
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  width: 110px;
  height: 32px;
  object-fit: cover;
  cursor: pointer;
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-sm);
  background: #fff;
}

.message-body {
  min-height: 120px;
}

.message-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 14px;
}

.message-item {
  padding: 16px 18px;
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-md);
  background: var(--mo-n50);
  transition:
    border-color 0.2s,
    background 0.2s;
}

.message-item:hover {
  border-color: var(--mo-p200);
  background: var(--mo-p50);
}

.message-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.message-nickname {
  color: var(--mo-n900);
  font-size: 15px;
  font-weight: 700;
}

.message-website {
  color: var(--mo-p700);
  font-size: var(--mo-fs-xs);
  font-weight: 600;
  text-decoration: none;
}

.message-website:hover {
  color: var(--mo-p800);
  text-decoration: underline;
}

.message-time {
  margin-left: auto;
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
}

.message-content {
  margin: 10px 0 0;
  color: var(--mo-n700);
  font-size: var(--mo-fs-sm);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-reply {
  margin-top: var(--mo-sp-3);
  padding: 12px 14px;
  border-left: 3px solid var(--mo-p400);
  border-radius: var(--mo-r-sm);
  background: var(--mo-p50);
}

.reply-label {
  display: inline-block;
  margin-bottom: 6px;
  color: var(--mo-p700);
  font-size: var(--mo-fs-xs);
  font-weight: 700;
}

.reply-content {
  margin: 0;
  color: var(--mo-n700);
  font-size: var(--mo-fs-sm);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-time {
  display: block;
  margin-top: 6px;
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
}

.guestbook-pagination {
  margin-top: var(--mo-sp-5);
  background: transparent;
}

html.dark .mo-guestbook-page {
  background: var(--mo-n900);
  color: var(--mo-n100);
}

html.dark .guestbook-panel {
  border-color: var(--mo-n700);
  background: var(--mo-n800);
}

html.dark .section-label {
  background: var(--mo-p50);
  color: var(--mo-p300);
}

/* 深色下 --mo-* 色阶被重映射：n50/n0/n800/n900 是背景色，
   文字必须用 n100~n200（主文字）或 n300~n500（次要文字） */
html.dark .guestbook-title,
html.dark .section-head h2,
html.dark .message-nickname {
  color: var(--mo-n200);
}

html.dark .guestbook-desc,
html.dark .message-content,
html.dark .reply-content {
  color: var(--mo-n500);
}

html.dark .guestbook-count,
html.dark .message-website,
html.dark .reply-label {
  color: var(--mo-p300);
}

html.dark .message-item {
  border-color: var(--mo-n700);
  background: var(--mo-n900);
}

html.dark .message-item:hover {
  border-color: var(--mo-p700);
  background: var(--mo-p50);
}

html.dark .message-reply {
  background: var(--mo-p50);
}

html.dark .message-time,
html.dark .reply-time {
  color: var(--mo-n500);
}

/* hover 态原用 p800，深色下对比度不足，改为更亮的 p200 */
html.dark .message-website:hover {
  color: var(--mo-p200);
}

html.dark .form-hint {
  color: var(--mo-n500);
}

html.dark .captcha-img {
  border-color: var(--mo-n700);
}

@media (max-width: 768px) {
  .mo-guestbook-page {
    padding: 72px 16px 48px;
  }

  .guestbook-hero {
    padding: var(--mo-sp-6) var(--mo-sp-5);
  }

  .guestbook-form-panel,
  .guestbook-list-panel {
    padding: 22px 20px 24px;
  }

  .guestbook-title {
    font-size: 26px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .message-time {
    margin-left: 0;
    width: 100%;
  }
}
</style>
