<template>
  <SiteLayout>
    <div class="ent-auth-page tpl-site">
      <section class="ent-auth-card ent-auth-card--wide">
        <router-link to="/" class="ent-auth-brand">
          <img
            v-if="brandLogo"
            :src="brandLogo"
            class="ent-brand-logo"
            :alt="brandName"
            loading="lazy"
          />
          <span v-else class="ent-brand-mark">{{ brandInitial }}</span>
          <span class="ent-brand-name">{{ brandName }}</span>
        </router-link>

        <div class="ent-auth-head">
          <span class="ent-auth-eyebrow">企业官网 · 找回密码</span>
          <h2>找回密码</h2>
          <p class="ent-auth-sub">通过邮箱重置您的密码</p>
        </div>

        <el-steps :active="currentStep" align-center class="ent-auth-steps">
          <el-step title="验证邮箱" />
          <el-step title="重置密码" />
          <el-step title="完成" />
        </el-steps>

        <!-- 步骤1: 验证邮箱 -->
        <div v-if="currentStep === 0" class="step-content">
          <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" class="ent-auth-form">
            <el-form-item prop="email">
              <el-input v-model="emailForm.email" placeholder="请输入注册邮箱" clearable>
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item v-if="captchaEnabled" prop="code">
              <div class="captcha-row">
                <el-input v-model="emailForm.code" placeholder="图形验证码" clearable />
                <img
                  :src="captchaUrl"
                  class="captcha-img"
                  alt="验证码"
                  title="点击刷新验证码"
                  @click="refreshCaptcha"
                />
              </div>
            </el-form-item>

            <el-form-item prop="emailCode">
              <div class="email-code-row">
                <el-input v-model="emailForm.emailCode" placeholder="邮箱验证码" clearable>
                  <template #prefix>
                    <el-icon><Key /></el-icon>
                  </template>
                </el-input>
                <el-button
                  :disabled="codeCountdown > 0"
                  :loading="codeSending"
                  @click="sendEmailCode"
                >
                  {{ codeCountdown > 0 ? `${codeCountdown}秒后重试` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="ent-auth-submit"
                @click="verifyEmail"
              >
                下一步
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤2: 重置密码 -->
        <div v-if="currentStep === 1" class="step-content">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            class="ent-auth-form"
          >
            <el-form-item prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="新密码"
                show-password
                clearable
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="确认新密码"
                show-password
                clearable
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="ent-auth-submit"
                @click="resetPassword"
              >
                重置密码
              </el-button>
              <el-button @click="currentStep = 0">上一步</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤3: 完成 -->
        <div v-if="currentStep === 2" class="step-content">
          <el-result icon="success" title="密码重置成功" sub-title="您的密码已成功重置">
            <template #extra>
              <el-button type="primary" @click="goToLogin">立即登录</el-button>
              <el-button @click="goToHome">返回首页</el-button>
            </template>
          </el-result>
        </div>

        <p class="ent-auth-footer">
          <router-link to="/login">返回登录</router-link>
          ·
          <router-link to="/register">注册账号</router-link>
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
import { Lock, Key, Message } from '@element-plus/icons-vue'
import { sendResetCode, getCodeImg, resetPassword as resetPasswordApi } from '@/api/site/auth'
import SiteLayout from '@/components/SiteLayout.vue'
import { useAuthBrand } from '@/composables/useAuthBrand'

const router = useRouter()
const { brandName, brandLogo, brandInitial } = useAuthBrand()

const emailFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const loading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let codeTimer: ReturnType<typeof setInterval> | null = null
const currentStep = ref(0)
const captchaEnabled = ref(true)
const captchaUrl = ref('')

const emailForm = reactive({
  email: '',
  emailCode: '',
  code: ''
})

const passwordForm = reactive({
  newPassword: '',
  confirmPassword: ''
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
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const emailRules: FormRules = {
  email: [{ required: true, validator: validateEmail, trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }]
}

// 获取并刷新图形验证码
const refreshCaptcha = async () => {
  try {
    const res = await getCodeImg()
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      captchaUrl.value = 'data:image/jpeg;base64,' + res.img
    }
  } catch {
    captchaEnabled.value = false
    captchaUrl.value = ''
  }
}

onMounted(refreshCaptcha)

onUnmounted(() => {
  if (codeTimer) {
    clearInterval(codeTimer)
    codeTimer = null
  }
})

const passwordRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度在5到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
}

// 发送邮箱验证码
const sendEmailCode = async () => {
  if (!emailForm.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(emailForm.email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  // 图形验证码（启用时必填）
  if (captchaEnabled.value && !emailForm.code) {
    ElMessage.warning('请输入图形验证码')
    refreshCaptcha()
    return
  }

  try {
    codeSending.value = true
    await sendResetCode(emailForm.email)
    ElMessage.success('验证码已发送，请查收邮箱')
    // 发送成功后刷新图形验证码，防止复用
    if (captchaEnabled.value) {
      emailForm.code = ''
      refreshCaptcha()
    }

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

// 验证邮箱
const verifyEmail = async () => {
  if (!emailFormRef.value) return

  try {
    const valid = await emailFormRef.value.validate()
    if (!valid) return

    // 邮箱验证成功，进入下一步
    currentStep.value = 1
  } catch (error: any) {
    ElMessage.error(error.message || '验证失败')
  }
}

// 重置密码
const resetPassword = async () => {
  if (!passwordFormRef.value) return

  try {
    const valid = await passwordFormRef.value.validate()
    if (!valid) return

    loading.value = true

    await resetPasswordApi({
      email: emailForm.email,
      code: emailForm.emailCode,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })

    ElMessage.success('密码重置成功')
    currentStep.value = 2
  } catch (error: any) {
    ElMessage.error(error.message || '密码重置失败')
  } finally {
    loading.value = false
  }
}

// 跳转到统一登录页
const goToLogin = () => {
  router.push('/login')
}

// 返回首页
const goToHome = () => {
  router.push('/')
}
</script>

<style scoped>
/* 步骤内容与共享表单之间的间距（其余样式在 assets/styles/auth-enterprise.scss） */
.step-content {
  margin-top: var(--mo-sp-6, 24px);
}

.ent-auth-steps {
  margin-bottom: var(--mo-sp-7, 32px);
}

/* el-result 在窄卡片内更紧凑 */
.step-content :deep(.el-result) {
  padding: var(--mo-sp-4, 16px) 0;
}
</style>
