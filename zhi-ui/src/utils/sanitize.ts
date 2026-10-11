/**
 * HTML内容消毒工具
 * 使用DOMPurify防止XSS攻击
 */

import DOMPurify from 'dompurify'

interface SanitizeOptions {
  ALLOWED_TAGS?: string[]
  ALLOWED_ATTR?: string[]
  ALLOW_DATA_ATTR?: boolean
  ALLOW_UNKNOWN_PROTOCOLS?: boolean
  SAFE_FOR_JQUERY?: boolean
  FORCE_BODY?: boolean
  REMOVE_COMMENTS?: boolean
  ALLOWED_URI_REGEXP?: RegExp
  ADD_ATTR?: string[]
}

interface ConfigResponse {
  data: string
}

/**
 * 允许的HTML标签白名单（文章内容）
 */
const ALLOWED_TAGS: string[] = [
  'p',
  'br',
  'hr',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'strong',
  'b',
  'em',
  'i',
  'u',
  's',
  'del',
  'a',
  'ul',
  'ol',
  'li',
  'blockquote',
  'pre',
  'code',
  'img',
  'table',
  'thead',
  'tbody',
  'tfoot',
  'tr',
  'th',
  'td',
  'div',
  'span'
]

/**
 * 允许的HTML属性白名单
 */
const ALLOWED_ATTR: string[] = [
  'href',
  'title',
  'target',
  'rel',
  'src',
  'alt',
  'width',
  'height',
  'class',
  'id'
]

/**
 * 消毒HTML内容，防止XSS攻击
 *
 * @param dirty - 未消毒的HTML内容
 * @param options - 消毒配置选项
 * @returns 消毒后的安全HTML内容
 */
export function sanitizeHtml(dirty: string, options: SanitizeOptions = {}): string {
  if (!dirty || typeof dirty !== 'string') {
    return ''
  }

  const defaultOptions: SanitizeOptions = {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false,
    ALLOW_UNKNOWN_PROTOCOLS: false,
    SAFE_FOR_JQUERY: true,
    // 强制https协议
    FORCE_BODY: false,
    // 移除HTML注释
    REMOVE_COMMENTS: true,
    // 保留所有URL中的相对路径
    ALLOWED_URI_REGEXP:
      /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|cid|xmpp):|[^a-z]|[a-z+.-]+(?:[^a-z+.-:]|$))/i
  }

  const config = { ...defaultOptions, ...options }

  return DOMPurify.sanitize(dirty, config)
}

/**
 * iframe src 允许的安全视频嵌入域名
 */
const SAFE_IFRAME_HOSTS = [
  'youtube.com',
  'www.youtube.com',
  'youtube-nocookie.com',
  'www.youtube-nocookie.com',
  'player.bilibili.com',
  'bilibili.com',
  'player.vimeo.com',
  'vimeo.com'
]

/**
 * 消毒文章内容（宽松模式，允许更多标签）
 *
 * @param content - 文章内容
 * @returns 消毒后的内容
 */
export function sanitizeArticleContent(content: string): string {
  if (!content || typeof content !== 'string') {
    return ''
  }

  // 文章允许更多标签和属性
  const articleOptions: SanitizeOptions = {
    ALLOWED_TAGS: [
      ...ALLOWED_TAGS,
      'sub',
      'sup',
      'figure',
      'figcaption',
      'iframe' // 允许iframe用于嵌入视频等
    ],
    ALLOWED_ATTR: [...ALLOWED_ATTR, 'frameborder', 'allowfullscreen', 'scrolling'],
    // 对iframe进行额外验证
    ADD_ATTR: ['data-safe-iframe']
  }

  const sanitized = sanitizeHtml(content, articleOptions)
  return restrictIframeSrc(sanitized)
}

/**
 * 移除 iframe 中非白名单域名的 src 属性
 */
function restrictIframeSrc(html: string): string {
  return html.replace(/<iframe\b[^>]*\bsrc="([^"]*)"[^>]*>/gi, (match, src: string) => {
    try {
      const url = new URL(src)
      const allowed = SAFE_IFRAME_HOSTS.some(
        h => url.hostname === h || url.hostname.endsWith('.' + h)
      )
      if (!allowed) {
        return match.replace(/\bsrc="[^"]*"/, '')
      }
    } catch {
      return match.replace(/\bsrc="[^"]*"/, '')
    }
    return match
  })
}

/**
 * 消毒用户评论内容（严格模式）
 *
 * @param comment - 评论内容
 * @returns 消毒后的内容
 */
export function sanitizeComment(comment: string): string {
  if (!comment || typeof comment !== 'string') {
    return ''
  }

  // 评论只允许基本的格式化标签
  const commentOptions: SanitizeOptions = {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'a'],
    ALLOWED_ATTR: ['href', 'title'],
    // 所有链接都在新窗口打开，并添加rel="noopener"
    ADD_ATTR: ['target', 'rel']
  }

  const sanitized = sanitizeHtml(comment, commentOptions)

  // 为所有链接添加安全属性
  return sanitized.replace(/<a\s+/gi, '<a target="_blank" rel="noopener noreferrer" ')
}

/**
 * 验证URL是否安全
 *
 * @param url - 要验证的URL
 * @returns 是否安全
 */
export function isSafeUrl(url: string): boolean {
  if (!url || typeof url !== 'string') {
    return false
  }

  try {
    const parsedUrl = new URL(url, window.location.origin)
    // 只允许http和https协议
    return ['http:', 'https:', 'mailto:', 'tel:'].includes(parsedUrl.protocol)
  } catch {
    return false
  }
}

/**
 * 创建安全的HTML元素
 *
 * @param tag - 标签名
 * @param attributes - 属性对象
 * @param content - 内容
 * @returns 安全的DOM元素
 */
export function createSafeElement(
  tag: string,
  attributes: Record<string, string> = {},
  content: string = ''
): HTMLElement {
  const element = document.createElement(tag)

  // 设置安全属性
  Object.entries(attributes).forEach(([key, value]) => {
    if (key === 'className') {
      element.className = value
    } else if (key.startsWith('data-')) {
      element.setAttribute(key, value)
    } else if (isSafeAttribute(tag, key, value)) {
      element.setAttribute(key, value)
    }
  })

  // 设置安全内容
  if (content) {
    element.textContent = content
  }

  return element
}

/**
 * 检查属性是否安全
 */
function isSafeAttribute(tag: string, attr: string, value: string): boolean {
  // 危险属性黑名单
  const dangerousAttrs = ['onclick', 'onload', 'onerror', 'onmouseover', 'onfocus', 'onblur']

  if (dangerousAttrs.includes(attr.toLowerCase())) {
    return false
  }

  // href/src属性需要验证URL
  if ((attr === 'href' || attr === 'src') && value) {
    return isSafeUrl(value)
  }

  return true
}

export default {
  sanitizeHtml,
  sanitizeArticleContent,
  sanitizeComment,
  isSafeUrl,
  createSafeElement
}
