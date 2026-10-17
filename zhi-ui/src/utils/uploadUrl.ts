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

/** 首页 Banner 允许的最大张数（后台多图上传与前台轮播共用） */
export const MAX_BANNER_IMAGES = 5

/**
 * 解析首页 Banner 多图配置。
 *
 * 存储沿用 `site_banner` 单键：多图以逗号分隔（`ImageUpload` 的多图模式即此格式），
 * 历史单图值（无逗号）自动视为只有一张，无需数据迁移。
 * 每项都经 `normalizeUploadUrl` 收敛（兼容容器名绝对地址），并去重、截断到上限。
 */
export function parseBannerUrls(value: unknown, max = MAX_BANNER_IMAGES): string[] {
  const raw = typeof value === 'string' ? value : ''
  // 内联地址（data:/blob:）自身可能含逗号，不能按分隔符拆开；多图以数组形式传入时走数组分支
  const items = Array.isArray(value)
    ? value
    : /^\s*(data:|blob:)/i.test(raw)
      ? [raw]
      : raw.split(',')
  const urls: string[] = []
  for (const item of items) {
    const url = normalizeUploadUrl(item)
    if (url && !urls.includes(url)) {
      urls.push(url)
    }
    if (urls.length >= Math.max(1, max)) break
  }
  return urls
}
