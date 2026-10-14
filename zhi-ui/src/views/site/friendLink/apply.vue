<template>
  <div class="friendlink-apply-page">
    <SiteLayout>
      <div class="apply-container">
        <!-- 友链申请开关关闭：展示友好提示，避免白屏 -->
        <div v-if="!applyEnabled" class="apply-card apply-closed">
          <h1 class="apply-title">本站暂未开放友链申请</h1>
          <p class="apply-sub">站长暂时关闭了友链申请入口，欢迎先浏览本站的其他内容</p>
          <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        </div>

        <div v-else class="apply-card">
          <h1 class="apply-title">申请友情链接</h1>
          <p class="apply-sub">提交后需要管理员审核，通过后会在网站底部展示</p>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="网站名称" prop="name">
              <el-input
                v-model="form.name"
                placeholder="你的网站名称"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="网站地址" prop="url">
              <el-input
                v-model="form.url"
                placeholder="https://example.com"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="联系邮箱" prop="email">
              <el-input
                v-model="form.email"
                placeholder="用于接收审核结果（选填）"
                maxlength="100"
              />
            </el-form-item>
            <el-form-item label="站点描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="一句话介绍你的网站（选填）"
                maxlength="200"
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
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">
                提交申请
              </el-button>
              <el-button @click="$router.push('/')">返回首页</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </SiteLayout>
  </div>
</template>

<script setup lang="ts" name="SiteFriendLinkApply">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-service'
import type { FormInstance, FormRules } from 'element-plus'
import SiteLayout from '@/components/SiteLayout.vue'
import { applyFriendLink, type FriendLinkApplyForm } from '@/api/site/friendLink'
import { getSiteSettingsAnonymous } from '@/api/site/setting'
import { getCodeImg } from '@/api/site/auth'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { logger } from '@/utils/logger'

const siteSettingsStore = useSiteSettingsStore()
const siteSettings = computed(() => siteSettingsStore.siteSettings)

// 友链申请开关（默认开启；'false' / '0' / false 视为关闭）
const applyEnabled = computed(() => siteSettingsStore.isFeatureEnabled('friend_link_apply_enabled'))

const formRef = ref<FormInstance>()
const submitting = ref(false)
const captchaEnabled = ref(true)
const captchaUrl = ref('')

const form = reactive<FriendLinkApplyForm>({
  name: '',
  url: '',
  email: '',
  description: '',
  code: '',
  uuid: ''
})

const validateUrl = (_rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请输入网站地址'))
  } else if (!/^https?:\/\/.+/.test(value)) {
    callback(new Error('请以 http(s):// 开头填写完整地址'))
  } else {
    callback()
  }
}

// 验证码为必填的规则仅在开启验证码时生效
const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: '请输入网站名称', trigger: 'blur' }],
  url: [{ required: true, validator: validateUrl, trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  ...(captchaEnabled.value
    ? { code: [{ required: true, message: '请输入验证码', trigger: 'blur' }] }
    : {})
}))

// 加载站点设置（判断友链申请开关）
const loadSiteSettings = async () => {
  try {
    const response = await getSiteSettingsAnonymous()
    const settings = response?.data || {}
    siteSettingsStore.updateSiteSettings(settings)
  } catch (error) {
    logger.error('加载站点设置失败:', error)
  }
}

// 刷新验证码
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

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const res: any = await applyFriendLink({
      name: form.name,
      url: form.url,
      email: form.email,
      description: form.description,
      code: form.code,
      uuid: form.uuid
    })

    // 兜底：拦截器已将非 200 转为 reject，此处防御性提示后端 msg
    if (res && typeof res.code === 'number' && res.code !== 200) {
      ElMessage.error(res.msg || '提交失败，请稍后重试')
      if (captchaEnabled.value) {
        refreshCaptcha()
      }
      return
    }

    ElMessage.success('申请已提交，请等待管理员审核')
    form.name = ''
    form.url = ''
    form.email = ''
    form.description = ''
    if (captchaEnabled.value) {
      refreshCaptcha()
    }
  } catch (error: any) {
    // 后端返回的 msg（如验证码错误/过期、本站暂未开放友链申请）由响应拦截器转为 error.message
    const msg = error?.msg || error?.message || '提交失败，请稍后重试'
    ElMessage.error(msg)
    // 提交失败后自动刷新验证码
    if (captchaEnabled.value) {
      refreshCaptcha()
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadSiteSettings()
  if (applyEnabled.value) {
    refreshCaptcha()
  }
})
</script>

<style scoped>
.friendlink-apply-page {
  min-height: 100vh;
}
.apply-container {
  max-width: 620px;
  margin: 0 auto;
  padding: var(--mo-sp-12) var(--mo-sp-5) var(--mo-sp-9);
}
.apply-card {
  padding: 28px;
  background: var(--el-bg-color-overlay, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: var(--mo-r-lg);
}
.apply-closed {
  text-align: center;
}
.apply-closed .apply-sub {
  margin-bottom: 22px;
}
.apply-title {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--el-text-color-primary, #303133);
}
.apply-sub {
  margin: 0 0 22px;
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);
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
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: var(--mo-r-sm);
}
</style>
