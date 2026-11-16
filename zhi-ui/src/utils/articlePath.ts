export interface ArticlePathSource {
  id?: number | string | null
  articleType?: string | null
  slug?: string | null
}

const TYPE_PREFIX: Record<string, string> = {
  business: '/business/',
  case: '/business/',
  solution: '/solutions/'
}

export function articleDetailPath(article: ArticlePathSource): string {
  const raw = article.slug?.trim() || article.id
  const key = raw === null || raw === undefined ? '' : String(raw).trim()
  if (!key) return '/'
  const prefix = TYPE_PREFIX[String(article.articleType || '').toLowerCase()] || '/article/'
  return prefix + encodeURIComponent(key)
}
