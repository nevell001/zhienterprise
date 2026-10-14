import request from '@/utils/request'

/**
 * 用户注册参数
 */
export interface SiteRegisterData {
  username: string
  password: string
  confirmPassword: string
  email: string
  emailCode: string
  code?: string
  uuid?: string
}

/**
 * 用户注册
 */
export function siteRegister(data: SiteRegisterData): Promise<any> {
  return request({
    url: '/api/auth/register',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

/**
 * 发送注册验证码
 */
export function sendRegisterCode(email: string): Promise<any> {
  return request({
    url: '/api/auth/send-register-code',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: { email }
  })
}

/**
 * 发送密码重置验证码
 */
export function sendResetCode(email: string): Promise<any> {
  return request({
    url: '/api/auth/send-reset-code',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: { email }
  })
}

/**
 * 重置密码
 */
export function resetPassword(data: {
  email: string
  code: string
  newPassword: string
  confirmPassword: string
}): Promise<any> {
  return request({
    url: '/api/auth/reset-password',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

/**
 * 获取验证码图片（复用现有的）
 */
export function getCodeImg(): Promise<any> {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}
