import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'
const SiteTokenKey = 'Site-Token'

/**
 * 记住我 Cookie 过期天数
 */
const REMEMBER_ME_DAYS = 7

/**
 * 获取管理员 Token
 */
export function getToken(): string | undefined {
  return Cookies.get(TokenKey)
}

/**
 * 设置管理员 Token
 * @param token - 令牌
 * @param rememberMe - 是否记住我（持久化 Cookie，默认 session 级别）
 */
export function setToken(token: string, rememberMe?: boolean): string | undefined {
  if (rememberMe) {
    return Cookies.set(TokenKey, token, { expires: REMEMBER_ME_DAYS })
  }
  return Cookies.set(TokenKey, token)
}

/**
 * 移除管理员 Token
 */
export function removeToken(): void {
  Cookies.remove(TokenKey)
}

/**
 * 获取注册用户 Token
 */
export function getSiteToken(): string | undefined {
  return Cookies.get(SiteTokenKey)
}

/**
 * 设置注册用户 Token
 * @param token - 令牌
 * @param rememberMe - 是否记住我（持久化 Cookie，默认 session 级别）
 */
export function setSiteToken(token: string, rememberMe?: boolean): string | undefined {
  if (rememberMe) {
    return Cookies.set(SiteTokenKey, token, { expires: REMEMBER_ME_DAYS })
  }
  return Cookies.set(SiteTokenKey, token)
}

/**
 * 移除注册用户 Token
 */
export function removeSiteToken(): void {
  Cookies.remove(SiteTokenKey)
}
