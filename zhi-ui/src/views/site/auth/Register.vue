<template>
  <SiteLayout>
    <div class="mo-auth-page">
      <section class="auth-card">
        <router-link to="/" class="auth-brand">
          <span class="brand-mark">知</span>
          <span>知企建站系统</span>
        </router-link>

        <div class="auth-head">
          <h2>注册</h2>
          <p class="sub">创建账号，开始发布与评论</p>
        </div>

        <div class="auth-switch">
          <router-link to="/login" class="switch-item">登录</router-link>
          <span class="switch-item active">注册</span>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
        >
          <el-form-item prop="username" class="form-group">
            <label>
              用户名
              <span class="required">*</span>
            </label>
            <el-input v-model="registerForm.username" placeholder="请输入用户名" clearable>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="nickname" class="form-group">
            <label>昵称</label>
            <el-input
              v-model="registerForm.nickname"
              placeholder="选填，留空则使用用户名"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="email" class="form-group">
            <label>邮箱</label>
            <el-input v-model="registerForm.email" placeholder="name@example.com" clearable>
              <template #prefix>
                <el-icon><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="emailCode" class="form-group">
            <label>邮箱验证码</label>
            <div class="email-code-row">
              <el-input v-model="registerForm.emailCode" placeholder="邮箱验证码" clearable>
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
              <el-button
                class="code-button"
                :disabled="codeCountdown > 0"
                :loading="codeSending"
                @click="sendEmailCode"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}秒` : '发送' }}
              </el-button>
            </div>
          </el-form-item>

          <div class="password-grid">
            <el-form-item prop="password" class="form-group">
              <label>密码</label>
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="5-20 位"
                show-password
                clearable
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="confirmPassword" class="form-group">
              <label>确认密码</label>
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="再次输入"
                show-password
                clearable
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <el-form-item v-if="captchaEnabled" prop="code" class="form-group">
            <label>验证码</label>
            <div class="captcha-row">
              <el-input v-model="registerForm.code" placeholder="计算结果" clearable>
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
              <img :src="captchaUrl" class="captcha-img" alt="验证码" @click="refreshCaptcha" />
            </div>
          </el-form-item>

          <div class="form-options">
            <label class="remember">
              <input v-model="agreeToTerms" type="checkbox" />
              <span>
                我已阅读并同意
                <router-link to="/agreement" target="_blank">《用户协议》</router-link>
                和
                <router-link to="/privacy" target="_blank">《隐私政策》</router-link>
              </span>
            </label>
          </div>

          <el-button type="primary" :loading="loading" class="auth-submit" @click="handleRegister">
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form>

        <p class="auth-footer">
          已有账号？
          <router-link to="/login">返回登录</router-link>
        </p>
      </section>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/plugins/element-plus-service'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Key, Message } from '@element-plus/icons-vue'
import { useSiteUserStore } from '@/stores/siteUser'
import SiteLayout from '@/components/SiteLayout.vue'
import { getCodeImg } from '@/api/site/auth'
import { logger } from '@/utils/logger'

const router = useRouter()
const siteUserStore = useSiteUserStore()

const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let codeTimer: ReturnType<typeof setInterval> | null = null
const captchaEnabled = ref(true)
const captchaUrl = ref('')
const agreeToTerms = ref(false)

const registerForm = reactive({
  username: '',
  nickname: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
  code: '',
  uuid: ''
})

const validateEmail = (rule: any, value: any, callback: any) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!value) {
    callback(new Error('请输入邮箱地址'))
  } else if (!emailRegex.test(value)) {
    callback(new Error('请输入正确的邮箱格式'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在2到20个字符', trigger: 'blur' }
  ],
  email: [{ required: true, validator: validateEmail, trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度在5到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    const res = await getCodeImg()
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      captchaUrl.value = 'data:image/jpeg;base64,' + res.img
      registerForm.uuid = res.uuid
      registerForm.code = ''
    } else {
      captchaUrl.value = ''
      registerForm.uuid = ''
      registerForm.code = ''
    }
  } catch (error) {
    logger.error('获取验证码失败:', error)
    captchaEnabled.value = false
  }
}

// 发送邮箱验证码
const sendEmailCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  try {
    codeSending.value = true
    await siteUserStore.sendRegisterCode(registerForm.email)
    ElMessage.success('验证码已发送，请查收邮箱')

    // 开始倒计时
    codeCountdown.value = 60
    if (codeTimer) clearInterval(codeTimer)
    codeTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0 && codeTimer) {
        clearInterval(codeTimer)
        codeTimer = null
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
  } finally {
    codeSending.value = false
  }
}

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return

  try {
    const valid = await registerFormRef.value.validate()
    if (!valid) return

    if (!agreeToTerms.value) {
      ElMessage.warning('请先同意用户协议和隐私政策')
      return
    }

    loading.value = true

    await siteUserStore.register({
      username: registerForm.username,
      nickname: registerForm.nickname,
      email: registerForm.email,
      emailCode: registerForm.emailCode,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      code: registerForm.code,
      uuid: registerForm.uuid
    })

    // 注册成功后跳转到统一登录页面
    router.push('/login')
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
    // 刷新验证码
    if (captchaEnabled.value) {
      refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 如果已登录，跳转到首页
  if (siteUserStore.isLogin) {
    router.push('/')
    return
  }
  // 获取验证码
  refreshCaptcha()
})

onUnmounted(() => {
  if (codeTimer) {
    clearInterval(codeTimer)
    codeTimer = null
  }
})
</script>

<style scoped>
.mo-auth-page {
  box-sizing: border-box;
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  /* 顶部避开 SiteLayout 的 fixed 导航（60px） */
  padding: calc(60px + 24px) var(--mo-sp-6) var(--mo-sp-6);
  background: radial-gradient(circle at top left, var(--mo-p50), transparent 32%), var(--mo-n50);
}

.auth-card {
  width: min(100%, 420px);
  max-width: 420px;
  padding: 26px 28px;
  background: var(--mo-bg-card, #fff);
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-md);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  margin-bottom: 18px;
  color: var(--mo-n900);
  font-size: var(--mo-fs-sm);
  font-weight: 700;
  text-decoration: none;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #fff;
  background: var(--mo-p600);
  border-radius: var(--mo-r-md);
}

.auth-head h2 {
  margin: 0 0 6px;
  color: var(--mo-n900);
  font-size: var(--mo-fs-2xl);
  font-weight: 700;
}

.sub {
  margin: 0 0 var(--mo-sp-4);
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
}

.auth-switch {
  display: flex;
  gap: var(--mo-sp-1);
  padding: var(--mo-sp-1);
  margin-bottom: var(--mo-sp-4);
  background: var(--mo-n100);
  border-radius: var(--mo-r-md);
}

.switch-item {
  flex: 1;
  padding: 7px 10px;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  text-align: center;
  text-decoration: none;
  border-radius: var(--mo-r-sm);
  transition: all 0.2s;
}

.switch-item:hover {
  color: var(--mo-n700);
}

.switch-item.active {
  color: var(--mo-p600);
  background: var(--mo-bg-card, #fff);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.register-form {
  margin-top: 0;
}

.form-group {
  display: block;
  margin-bottom: var(--mo-sp-3);
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: var(--mo-n700);
  font-size: 13px;
  font-weight: 500;
}

.form-group label .required {
  color: var(--mo-error, #f56c6c);
  margin-left: 2px;
}

.form-group :deep(.el-form-item__content) {
  display: block;
}

:deep(.el-input__wrapper) {
  border-radius: var(--mo-r-md);
  box-shadow: 0 0 0 1px var(--mo-n300) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--mo-p400) inset,
    0 0 0 3px var(--mo-p50);
}

.email-code-row,
.captcha-row {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-2);
}

.email-code-row :deep(.el-input),
.captcha-row :deep(.el-input) {
  flex: 1;
}

.code-button {
  width: 82px;
  border-radius: var(--mo-r-md);
}

.captcha-img {
  width: 106px;
  height: 32px;
  object-fit: cover;
  cursor: pointer;
  border: 1px solid var(--mo-n300);
  border-radius: var(--mo-r-md);
  transition: border-color 0.2s;
}

.captcha-img:hover {
  border-color: var(--mo-p400);
}

.form-options {
  margin: 2px 0 16px;
  font-size: 13px;
}

.remember {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  color: var(--mo-n600);
}

.remember input {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  accent-color: var(--mo-p600);
}

.remember a {
  color: var(--mo-p600);
  font-weight: 600;
  text-decoration: none;
}

.remember a:hover {
  text-decoration: underline;
}

.auth-submit {
  width: 100%;
  height: 38px;
  border-radius: var(--mo-r-md);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
}

.auth-footer {
  margin: var(--mo-sp-4) 0 0;
  color: var(--mo-n500);
  font-size: 13px;
  text-align: center;
}

.auth-footer a {
  color: var(--mo-p600);
  font-weight: 600;
  text-decoration: none;
}

.password-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

@media (max-width: 768px) {
  .mo-auth-page {
    padding: calc(60px + 16px) var(--mo-sp-4) var(--mo-sp-4);
  }

  .auth-card {
    padding: 22px;
  }

  .password-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}

:global(html.dark.theme-mo-blog .auth-card),
:global(html.dark .auth-card) {
  background: var(--mo-bg-card) !important;
  border-color: var(--mo-border, #44403c) !important;
}

/* 深色下 --mo-n900 被重映射成背景色，直接用作文字色会与卡片同色而隐形，这里显式改回前景色 */
:global(html.dark.theme-mo-blog .auth-head h2),
:global(html.dark .auth-head h2),
:global(html.dark.theme-mo-blog .auth-brand),
:global(html.dark .auth-brand) {
  color: var(--mo-n100, #f5f5f4) !important;
}

:global(html.dark.theme-mo-blog .auth-switch),
:global(html.dark .auth-switch) {
  background: var(--mo-n700) !important;
}

:global(html.dark.theme-mo-blog .switch-item),
:global(html.dark .switch-item) {
  color: var(--mo-n300, #57534e) !important;
}

:global(html.dark.theme-mo-blog .switch-item:hover),
:global(html.dark .switch-item:hover) {
  color: var(--mo-n100, #f5f5f4) !important;
}

:global(html.dark.theme-mo-blog .switch-item.active),
:global(html.dark .switch-item.active) {
  background: var(--mo-n700) !important;
  color: var(--mo-n100, #f5f5f4) !important;
  box-shadow: none !important;
  border-color: transparent !important;
}

:global(html.dark.theme-mo-blog .auth-submit),
:global(html.dark .auth-submit) {
  background: var(--mo-p600) !important;
  color: var(--mo-n100, #f5f5f4) !important;
  border-color: transparent !important;
}

:global(html.dark.theme-mo-blog .auth-footer a),
:global(html.dark .auth-footer a) {
  color: var(--mo-p300, #a5b4fc) !important;
}

/* 深色下 .remember 内的协议链接沿用浅色 p600 会太暗（色阶被重映射），与 auth-footer 链接同口径 */
:global(html.dark.theme-mo-blog .remember a),
:global(html.dark .remember a) {
  color: var(--mo-p300, #a5b4fc);
}

/* 默认深色下标签用次要文字色；不写死 #78716c（深色底上只有 3.4:1）。 */
:global(html.dark .form-group label) {
  color: var(--mo-n400) !important;
}

/* 默认深色下 .el-input__wrapper 的描边：浅色规则用的 --mo-n300 在深色作用域被重映射成
   次要文字色（#cfd3dc），会画出一条近乎白色的边框，这里换成边框令牌 --mo-n700。
   Mo-Blog 深色由 theme-dark.css / mo-blog.scss 的同名规则覆盖，值也是 #44403c。 */
html.dark .mo-auth-page :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--mo-n700) inset;
}

/* 上一条选择器权重高于浅色区的 .is-focus 规则，聚焦态必须一起补写，否则深色下聚焦没有反馈。 */
html.dark .mo-auth-page :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--mo-p400) inset,
    0 0 0 3px var(--mo-p50);
}

/* 输入框 / 占位符的深色样式由 theme-dark.css 与 mo-blog.scss 的
   「Auth 表单」块负责。这里原先还有一批 :global(html.dark :deep(...)) 规则，
   :deep() 嵌在 :global() 里不会被编译器展开，浏览器把整条选择器当作非法
   规则丢弃（实测 CSSOM 里查不到），删掉不改变任何渲染结果。 */
</style>
