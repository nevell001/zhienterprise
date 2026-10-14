/**
 * JSON-LD 结构化数据注入工具。
 *
 * 同一 id 的 <script type="application/ld+json"> 全局唯一（upsert 语义）：
 * - 页面级数据（如 Article）在路由切换/组件卸载时调用 removeJsonLd 移除，避免残留旧数据；
 * - 站点级数据（如 Organization）由常驻布局注入，随设置更新覆盖。
 */

export function setJsonLd(id: string, data: unknown): void {
  if (typeof document === 'undefined') return
  let el = document.getElementById(id) as HTMLScriptElement | null
  if (!el) {
    el = document.createElement('script')
    el.id = id
    el.type = 'application/ld+json'
    document.head.appendChild(el)
  }
  el.textContent = JSON.stringify(data)
}

export function removeJsonLd(id: string): void {
  if (typeof document === 'undefined') return
  document.getElementById(id)?.remove()
}
