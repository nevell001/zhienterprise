/**
 * custom_js 在库中以带前缀的 base64 存储：XSS 过滤器会清掉 JSON 字段里的 <script>，
 * 编码后既保留原始代码，也避免被当成 HTML 清理。前台注入与后台编辑都需要先解码。
 */
const PREFIX = 'b64:'

export function encodeCustomJs(code: string): string {
  const text = code ?? ''
  if (!text.trim()) return ''
  return PREFIX + btoa(encodeURIComponent(text))
}

export function decodeCustomJs(value: string | null | undefined): string {
  const text = String(value ?? '').trim()
  if (!text.startsWith(PREFIX)) return text
  try {
    return decodeURIComponent(atob(text.slice(PREFIX.length)))
  } catch {
    return ''
  }
}
