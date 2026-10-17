<template>
  <!-- 与「注册 / 找回密码」一致：包在 SiteLayout 里，企业官网模式下自动获得
       企业导航与页脚，登录页与整站风格统一（此前是脱离布局的独立全屏页）。 -->
  <SiteLayout>
    <div class="unified-login-container auth-enterprise tpl-site">
      <div class="auth-enterprise__aurora" aria-hidden="true"></div>

      <section class="ent-auth-card">
        <router-link to="/" class="ent-auth-brand">
          <img
            v-if="brandAvatar"
            :src="brandAvatar"
            class="ent-brand-logo"
            :alt="brandName"
            loading="lazy"
          />
          <span v-else class="ent-brand-mark">{{ brandInitial }}</span>
          <span class="ent-brand-name">{{ brandName }}</span>
        </router-link>

        <div class="ent-auth-head">
          <span class="ent-auth-eyebrow">企业官网 · 管理后台</span>
          <h2>登录</h2>
          <p class="ent-auth-sub">登录后管理官网内容、页面与品牌形象</p>
        </div>

        <div class="ent-auth-switch">
          <span class="ent-switch-item is-active">登录</span>
          <router-link to="/register" class="ent-switch-item">注册</router-link>
        </div>

        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="ent-login-form">
          <el-form-item prop="username" class="form-group">
            <label>邮箱 / 用户名</label>
            <el-input
              ref="usernameInputRef"
              v-model="loginForm.username"
              placeholder="用户名或邮箱"
              clearable
              @keyup.enter="focusPassword"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password" class="form-group">
            <label>密码</label>
            <el-input
              ref="passwordInputRef"
              v-model="loginForm.password"
              type="password"
              placeholder="输入密码"
              show-password
              clearable
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="captchaEnabled" prop="code" class="form-group">
            <label>验证码</label>
            <div class="captcha-row">
              <el-input
                ref="codeInputRef"
                v-model="loginForm.code"
                placeholder="计算结果"
                clearable
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
              <img :src="captchaUrl" class="captcha-img" alt="验证码" @click="refreshCaptcha" />
            </div>
          </el-form-item>

          <div class="form-options">
            <label class="remember">
              <input v-model="rememberMe" type="checkbox" />
              记住我
            </label>
            <router-link to="/forgot-password" class="forgot">忘记密码？</router-link>
          </div>

          <el-button type="primary" :loading="loading" class="ent-auth-submit" @click="handleLogin">
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <p class="ent-auth-footer">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </p>
      </section>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-service'
import type { FormInstance, FormRules, InputInstance } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getCodeImg } from '@/api/login'
import { unifiedLogin } from '@/api/unifiedAuth'
import { setToken } from '@/utils/auth'
import SiteLayout from '@/components/SiteLayout.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const siteSettingsStore = useSiteSettingsStore()

// 品牌信息取站点设置（站名 / 站点头像），与企业官网保持一致；设置未加载时回退默认站名
const brandName = computed(() => {
  const name = String(siteSettingsStore.siteSettings?.blog_name || '').trim()
  return name || '知企建站系统'
})
const brandAvatar = computed(() => String(siteSettingsStore.siteSettings?.blog_avatar || '').trim())
const brandInitial = computed(() => brandName.value.slice(0, 1))

const loginFormRef = ref<FormInstance>()
const usernameInputRef = ref<InputInstance>()
const passwordInputRef = ref<InputInstance>()
const codeInputRef = ref<InputInstance>()
const loading = ref(false)
const captchaEnabled = ref(true)
const captchaUrl = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  code: '',
  uuid: ''
})

const rememberMe = ref(false)

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 聚焦到密码输入框
const focusPassword = () => {
  nextTick(() => {
    passwordInputRef.value?.focus()
  })
}

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    const res = await getCodeImg()
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      captchaUrl.value = 'data:image/jpeg;base64,' + res.img
      loginForm.uuid = res.uuid
      loginForm.code = ''
    } else {
      captchaUrl.value = ''
      loginForm.uuid = ''
      loginForm.code = ''
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
    captchaEnabled.value = false
  }
}

// 获取友好的错误提示
const getErrorMessage = (error: any): string => {
  const errorMessage = error.message || error.msg || error.data?.msg || ''
  const isProduction = import.meta.env?.MODE === 'production'

  // 生产环境：隐藏敏感信息，只显示通用错误
  if (isProduction) {
    // 所有认证相关的错误都统一为"用户名或密码错误"
    if (
      errorMessage.includes('用户') ||
      errorMessage.includes('密码') ||
      errorMessage.includes('账号')
    ) {
      return '用户名或密码错误，请检查后重试'
    }

    // 验证码错误可以显示
    if (errorMessage.includes('验证码')) {
      return '验证码错误，请重新输入'
    }

    // 其他错误显示通用提示
    return '登录失败，请稍后重试'
  }

  // 开发环境：显示详细错误信息以便调试
  if (errorMessage.includes('用户不存在') || errorMessage.includes('用户名或密码错误')) {
    return '用户名或密码错误，请检查后重试'
  }

  if (errorMessage.includes('密码错误')) {
    return '密码错误，请重新输入'
  }

  if (errorMessage.includes('验证码')) {
    return '验证码错误，请重新输入'
  }

  if (errorMessage.includes('账号已被锁定')) {
    return '账号已被锁定，请联系管理员'
  }

  if (errorMessage.includes('账号已被禁用')) {
    return '账号已被禁用，请联系管理员'
  }

  return errorMessage || '登录失败，请稍后重试'
}

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    const valid = await loginFormRef.value.validate()
    if (!valid) return

    loading.value = true

    const res = await unifiedLogin({
      username: loginForm.username,
      password: loginForm.password,
      code: loginForm.code,
      uuid: loginForm.uuid,
      rememberMe: rememberMe.value
    })

    // 从响应中获取token
    const loginResponse = res as any
    const token = loginResponse.token || loginResponse.data?.token

    if (!token) {
      throw new Error('登录失败：未获取到token')
    }

    // 统一使用Admin-Token存储（记住我时持久化到 Cookie）
    setToken(token, rememberMe.value)
    // 同时更新 store 中的 token 状态
    userStore.token = token

    // 立即获取用户信息，避免页面刷新后才显示登录状态
    try {
      await userStore.getInfo()
      ElMessage.success({
        message: '登录成功，欢迎回来！',
        duration: 2000
      })
    } catch (error) {
      console.error('获取用户信息失败:', error)
      ElMessage.warning({
        message: '登录成功，但获取用户信息失败，请刷新页面',
        duration: 3000
      })
    }

    // 跳转到首页，路由守卫会根据用户权限自动跳转
    const redirect = (route.query.redirect as string) || '/index'
    router.push(redirect)
  } catch (error: any) {
    const errorMessage = getErrorMessage(error)
    ElMessage.error({
      message: errorMessage,
      duration: 3000
    })
    // 刷新验证码
    if (captchaEnabled.value) {
      refreshCaptcha()
      // 清空验证码输入框
      loginForm.code = ''
      nextTick(() => {
        codeInputRef.value?.focus()
      })
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 获取验证码
  refreshCaptcha()
  // 自动聚焦到用户名输入框
  nextTick(() => {
    usernameInputRef.value?.focus()
  })
})
</script>

<style scoped>
/* ==================== 企业官网风格登录页 ====================
   设计语言对齐企业官网（site-templates.scss 的 --tpl-* 变量层）：
   - 蓝系主色（.tpl-site 基类 --tpl-p: #1a56db）、大圆角（--tpl-r-lg）、
     分层阴影（--tpl-sh-md）、浅底 + 柔和极光渐变；
   - 文字走 --tpl-ink-900/700/500；深色模式由 site-templates.scss 的
     html.dark .tpl-site 区块自动重映射，无需为每个属性再写一遍暗色；
   - 结构随注册/找回密码页：包在 SiteLayout 里，企业模式自动带企业导航与页脚。 */

.auth-enterprise {
  position: relative;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 60px);
  /* 顶部避开 SiteLayout 固定导航（60px），并留出企业站的呼吸感 */
  padding: calc(60px + var(--mo-sp-9, 48px)) var(--mo-sp-6, 24px) var(--mo-sp-9, 48px);
  overflow: hidden;
  background: var(--tpl-surface, #f7f8fa);
}

/* 企业官网模式：SiteLayout 已用 .tpl-nav-offset 为 fixed 导航留出 64px，
   页面不再重复让位；同时让渐变区铺满到导航下方 */
:global(.site-layout.tpl-nav-offset) > .auth-enterprise {
  min-height: calc(100vh - 64px);
  padding-top: var(--mo-sp-9, 48px);
}

/* 登录页与页脚紧接：企业页脚的 margin-top 是给常规内容段留呼吸感的，
   这里若保留会在渐变区与深色页脚之间露出一条页面底色 */
:global(.site-layout > .auth-enterprise + .ent-footer) {
  margin-top: 0;
}

/* 极光背景：两处低饱和主色光晕，配合企业站 Hero 的浅底气质 */
.auth-enterprise__aurora {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(
      circle at 18% 8%,
      color-mix(in srgb, var(--tpl-p, #1a56db) 14%, transparent),
      transparent 44%
    ),
    radial-gradient(
      circle at 84% 92%,
      color-mix(in srgb, var(--tpl-p, #1a56db) 10%, transparent),
      transparent 46%
    );
}

.ent-auth-card {
  position: relative;
  z-index: 1;
  box-sizing: border-box;
  width: 100%;
  max-width: 380px;
  padding: 34px 32px 28px;
  background: var(--tpl-bg, #fff);
  border: 1px solid var(--tpl-border, #e5e7eb);
  border-radius: var(--tpl-r-lg, 16px);
  box-shadow: var(--tpl-sh-md, 0 8px 24px rgba(11, 15, 25, 0.1));
}

.ent-auth-brand {
  display: flex;
  gap: 12px;
  align-items: center;
  width: fit-content;
  margin-bottom: 26px;
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-sm, 14px);
  font-weight: 700;
  text-decoration: none;
}

.ent-brand-mark {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: var(--tpl-white, #fff);
  font-size: var(--mo-fs-md, 16px);
  font-weight: 700;
  background: var(--tpl-p, #1a56db);
  border-radius: var(--tpl-r-md, 10px);
}

.ent-brand-logo {
  flex: none;
  width: 36px;
  height: 36px;
  object-fit: cover;
  border-radius: var(--tpl-r-md, 10px);
}

.ent-brand-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ent-auth-eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  padding: 4px 10px;
  color: var(--tpl-p-text, #1a56db);
  font-size: var(--mo-fs-xs, 12px);
  font-weight: 600;
  letter-spacing: 0.02em;
  background: var(--tpl-p-sub, #eef3fe);
  border-radius: 999px;
}

.ent-auth-head h2 {
  margin: 0 0 8px;
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-3xl, 28px);
  font-weight: 700;
  letter-spacing: -0.01em;
}

.ent-auth-sub {
  margin: 0 0 22px;
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-sm, 14px);
  line-height: 1.6;
}

.ent-auth-switch {
  display: flex;
  gap: 4px;
  padding: 4px;
  margin-bottom: 20px;
  background: var(--tpl-surface, #f7f8fa);
  border-radius: var(--tpl-r-md, 10px);
}

.ent-switch-item {
  flex: 1;
  padding: 8px 10px;
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-sm, 14px);
  font-weight: 500;
  text-align: center;
  text-decoration: none;
  border-radius: var(--tpl-r-sm, 6px);
  transition:
    color 0.2s,
    background 0.2s;
}

.ent-switch-item:hover {
  color: var(--tpl-ink-900, #0b0f19);
}

.ent-switch-item.is-active {
  color: var(--tpl-p-text, #1a56db);
  background: var(--tpl-bg, #fff);
  box-shadow: var(--tpl-sh-sm, 0 1px 2px rgba(11, 15, 25, 0.06));
}

.ent-login-form {
  margin-top: 0;
}

.form-group {
  display: block;
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: var(--tpl-ink-700, #1f2937);
  font-size: var(--mo-fs-xs, 12px);
  font-weight: 600;
}

.form-group :deep(.el-form-item__content) {
  display: block;
}

:deep(.el-input__wrapper) {
  background: var(--tpl-bg, #fff);
  border-radius: var(--tpl-r-md, 10px);
  box-shadow: 0 0 0 1px var(--tpl-border, #e5e7eb) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--tpl-p, #1a56db) inset,
    0 0 0 3px color-mix(in srgb, var(--tpl-p, #1a56db) 14%, transparent);
}

.captcha-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.captcha-row :deep(.el-input) {
  flex: 1;
}

.captcha-img {
  width: 106px;
  height: 34px;
  cursor: pointer;
  object-fit: cover;
  border: 1px solid var(--tpl-border, #e5e7eb);
  border-radius: var(--tpl-r-sm, 6px);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.captcha-img:hover {
  border-color: var(--tpl-p, #1a56db);
  box-shadow: var(--tpl-sh-sm, 0 1px 2px rgba(11, 15, 25, 0.06));
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 18px;
  font-size: var(--mo-fs-xs, 12px);
}

.remember {
  display: flex;
  gap: 6px;
  align-items: center;
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-xs, 12px);
}

.remember input {
  width: 14px;
  height: 14px;
  accent-color: var(--tpl-p, #1a56db);
}

.forgot,
.ent-auth-footer a {
  color: var(--tpl-p-text, #1a56db);
  font-size: var(--mo-fs-xs, 12px);
  font-weight: 600;
  text-decoration: none;
}

.forgot:hover,
.ent-auth-footer a:hover {
  text-decoration: underline;
}

.ent-auth-submit {
  width: 100%;
  height: 44px;
  color: var(--tpl-white, #fff);
  font-size: var(--mo-fs-sm, 14px);
  font-weight: 600;
  background: var(--tpl-p, #1a56db);
  border: none;
  border-radius: var(--tpl-r-md, 10px);
  transition: background 0.2s;
}

.ent-auth-submit:hover,
.ent-auth-submit:focus {
  color: var(--tpl-white, #fff);
  background: var(--tpl-p-h, #1544b0);
}

.ent-auth-footer {
  margin: 20px 0 0;
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-xs, 12px);
  text-align: center;
}

/* ---------- 深色模式 ----------
   --tpl-* 已由 site-templates.scss 的 html.dark .tpl-site 重映射；
   这里只需压过 mo-blog.scss 里按旧登录页写的全局暗色规则
   （html.dark.theme-mo-blog .auth-card / .auth-switch / .ent-auth-head h2 等）。
   选择器带上 .auth-enterprise 提升特异性，避免依赖样式加载顺序。 */
/* 暗色下保持「卡片比页面浅一档」的层次：页面用 --tpl-bg（#141414），
   卡片用 --tpl-surface（#1d1d1d），与亮色「浅灰页 + 白卡」同一关系 */
html.dark .auth-enterprise {
  background: var(--tpl-bg);
}

html.dark .auth-enterprise .ent-auth-card {
  background: var(--tpl-surface);
  border-color: var(--tpl-border);
  box-shadow: var(--tpl-sh-md);
}

html.dark .auth-enterprise .ent-auth-switch {
  background: var(--tpl-surface);
}

html.dark .auth-enterprise .ent-auth-head h2,
html.dark .auth-enterprise .ent-auth-brand {
  color: var(--tpl-ink-900);
}

html.dark .auth-enterprise .ent-auth-footer,
html.dark .auth-enterprise .ent-auth-sub,
html.dark .auth-enterprise .remember {
  color: var(--tpl-ink-500);
}

html.dark .auth-enterprise .form-group label {
  color: var(--tpl-ink-700);
}

/* mo-blog.scss 对全站输入框写了 !important 的棕色底（--mo-n800），
   这里用更高特异性 + !important 拉回企业中性色 */
html.dark .auth-enterprise :deep(.el-input__wrapper) {
  background-color: var(--tpl-surface) !important;
  box-shadow: 0 0 0 1px var(--tpl-border) inset !important;
}

html.dark .auth-enterprise :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--tpl-p) inset,
    0 0 0 3px color-mix(in srgb, var(--tpl-p) 26%, transparent) !important;
}

html.dark .auth-enterprise .ent-switch-item {
  color: var(--tpl-ink-500);
}

html.dark .auth-enterprise .ent-switch-item.is-active {
  color: var(--tpl-p-text);
  /* 暗色下卡片是 --tpl-surface，选中胶囊再用 --tpl-bg 会比卡片更暗（像凹陷）。
     混入少量前景色提亮一档，形成「浮起」的选中态。 */
  background: color-mix(in srgb, var(--tpl-ink-900) 6%, var(--tpl-surface));
}

html.dark .auth-enterprise .forgot,
html.dark .auth-enterprise .ent-auth-footer a {
  color: var(--tpl-p-text);
}

html.dark .auth-enterprise .ent-auth-submit {
  color: var(--tpl-white);
  background: var(--tpl-p);
}

html.dark .auth-enterprise .ent-auth-submit:hover,
html.dark .auth-enterprise .ent-auth-submit:focus {
  background: var(--tpl-p-h);
}

@media (max-width: 768px) {
  .auth-enterprise {
    min-height: auto;
    padding: calc(60px + var(--mo-sp-6, 24px)) var(--mo-sp-4, 16px) var(--mo-sp-7, 32px);
  }

  .ent-auth-card {
    padding: 26px 22px 22px;
  }

  .ent-auth-head h2 {
    font-size: var(--mo-fs-2xl, 24px);
  }
}
</style>
