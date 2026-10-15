import request from '@/utils/request'

export interface SiteNavItem {
  id?: number
  location?: string
  parentId?: number
  label: string
  linkType?: 'path' | 'url' | 'cta'
  linkValue?: string
  sort?: number
  visible?: number | boolean
  children?: SiteNavItem[]
}

/** 导航树（仅可见项，公开接口） */
export function getSiteNav(
  location: 'header' | 'footer'
): Promise<{ code: number; data: SiteNavItem[] }> {
  return request({
    url: '/api/nav',
    method: 'get',
    params: { location },
    headers: { isToken: false }
  })
}
