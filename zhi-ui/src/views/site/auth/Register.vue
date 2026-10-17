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
          <span class="ent-auth-eyebrow">企业官网 · 注册账号</span>
          <h2>注册</h2>
          <p class="ent-auth-sub">创建账号，开始管理官网内容</p>
        </div>

        <div class="ent-auth-switch">
          <router-link to="/login" class="ent-switch-item">登录</router-link>
          <span class="ent-switch-item is-active">注册</span>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="ent-auth-form"
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

          <el-button
            type="primary"
            :loading="loading"
            class="ent-auth-submit"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form>

        <p class="ent-auth-footer">
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
import { useAuthBrand } from '@/composables/useAuthBrand'

const router = useRouter()
const siteUserStore = useSiteUserStore()
const { brandName, brandLogo, brandInitial } = useAuthBrand()

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
