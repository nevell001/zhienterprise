<template>
  <!-- 与「注册 / 找回密码」一致：包在 SiteLayout 里，企业官网模式下自动获得
       企业导航与页脚，登录页与整站风格统一（此前是脱离布局的独立全屏页）。 -->
  <SiteLayout>
    <div class="unified-login-container ent-auth-page tpl-site">
      <section class="ent-auth-card">
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
          <span class="ent-auth-eyebrow">企业官网 · 管理后台</span>
          <h2>登录</h2>
          <p class="ent-auth-sub">登录后管理官网内容、页面与品牌形象</p>
        </div>

        <div class="ent-auth-switch">
          <span class="ent-switch-item is-active">登录</span>
          <router-link to="/register" class="ent-switch-item">注册</router-link>
        </div>

        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="ent-auth-form">
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
import { initSiteSettings } from '@/utils/siteSettings'
import SiteLayout from '@/components/SiteLayout.vue'
import { useAuthBrand } from '@/composables/useAuthBrand'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
// 品牌信息取站点设置（自定义 Logo → 站点头像 → 站名首字），三页共用
const { brandName, brandLogo, brandInitial } = useAuthBrand()

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

    // 登录后立即补齐站点个性化设置：bootstrap 的 initSiteSettings 在未登录时早退，
    // 不刷新页面直接跳转会导致「主题颜色」等设置要等下次刷新才生效。
    void initSiteSettings()

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
