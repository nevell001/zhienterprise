import { defineStore } from 'pinia'
import { ElMessage } from '@/plugins/element-plus-service'
import { siteRegister, sendRegisterCode } from '@/api/site/auth'

const BLOG_TOKEN_KEY = 'Site-Token'

/**
 * 获取注册用户Token
 */
export function getSiteToken(): string {
  return localStorage.getItem(BLOG_TOKEN_KEY) || ''
}

interface SiteRegisterInfo {
  username: string
  nickname?: string
  password: string
  confirmPassword: string
  email: string
  emailCode: string
  code?: string
  uuid?: string
}

interface SiteUserState {
  token: string
  name: string
  avatar: string
  email: string
  userId: number
  isLoggedIn: boolean
}

export const useSiteUserStore = defineStore('siteUser', {
  state: (): SiteUserState => ({
    token: getSiteToken(),
    name: '',
    avatar: '',
    email: '',
    userId: 0,
    isLoggedIn: false
  }),

  getters: {
    /** 判断是否已登录 */
    isLogin: state => !!state.token && !!state.isLoggedIn
  },

  actions: {
    /**
     * 注册用户注册
     */
    async register(registerInfo: SiteRegisterInfo): Promise<void> {
      try {
        await siteRegister(registerInfo)
        // 注册成功后跳转到登录页面（不自动登录，因为图形验证码已被使用）
        ElMessage.success('注册成功，请登录')
        // 不需要手动跳转，由组件处理跳转
      } catch (error) {
        console.error('注册用户注册失败:', error)
        throw error
      }
    },

    /**
     * 发送注册验证码
     */
    async sendRegisterCode(email: string): Promise<void> {
      try {
        await sendRegisterCode(email)
      } catch (error) {
        console.error('发送注册验证码失败:', error)
        throw error
      }
    }
  }
})
