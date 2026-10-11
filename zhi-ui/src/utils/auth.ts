import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'
const BlogTokenKey = 'Blog-Token'

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
export function getBlogToken(): string | undefined {
  return Cookies.get(BlogTokenKey)
}

/**
 * 设置注册用户 Token
 * @param token - 令牌
 * @param rememberMe - 是否记住我（持久化 Cookie，默认 session 级别）
 */
export function setBlogToken(token: string, rememberMe?: boolean): string | undefined {
  if (rememberMe) {
    return Cookies.set(BlogTokenKey, token, { expires: REMEMBER_ME_DAYS })
  }
  return Cookies.set(BlogTokenKey, token)
}

/**
 * 移除注册用户 Token
 */
export function removeBlogToken(): void {
  Cookies.remove(BlogTokenKey)
}
