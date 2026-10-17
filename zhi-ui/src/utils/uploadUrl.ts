/**
 * 上传图片地址规范化。
 *
 * 后端 `/common/upload` 返回的 `url` 是「请求 Host + 路径」的绝对地址
 * （`ServerConfig.getUrl()` 取自 request），经 Docker/Vite 代理时 Host 会变成
 * 容器名（如 `http://zhi-admin:8080/profile/...`）或内网 IP，**浏览器无法解析**，
 * 于是图片 404、`naturalWidth = 0`。实际可用的相对路径是响应里的 `fileName`
 * （`/profile/upload/...`），dev 由 Vite `/profile` 代理、prod 由 nginx 代理。
 *
 * 本函数把这类历史绝对地址统一收敛成站内相对路径，供渲染与保存共用：
 * - `data:` / `blob:` 等内联地址原样返回；
 * - 绝对 http(s) 地址取 pathname + search；
 * - 其它（已是相对路径）原样返回。
 */
export function normalizeUploadUrl(value: unknown): string {
  const raw = String(value ?? '').trim()
  if (!raw) return ''
  if (/^(data:|blob:)/i.test(raw)) return raw
  if (/^https?:\/\//i.test(raw)) {
    try {
      const url = new URL(raw)
      return `${url.pathname}${url.search}`
    } catch {
      return raw
    }
  }
  return raw
}
