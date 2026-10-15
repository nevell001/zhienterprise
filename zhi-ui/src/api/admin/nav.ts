import request from '@/utils/request'
import type { SiteNavItem } from '@/api/site/nav'

/** 导航树（含隐藏项，管理侧） */
export function listNav(location: 'header' | 'footer'): Promise<{ code: number; data: SiteNavItem[] }> {
  return request({
    url: '/system/nav/list',
    method: 'get',
    params: { location }
  })
}

export function getNav(id: number): Promise<{ code: number; data: SiteNavItem }> {
  return request({
    url: '/system/nav/' + id,
    method: 'get'
  })
}

export function addNav(data: Partial<SiteNavItem>): Promise<{ code: number }> {
  return request({
    url: '/system/nav',
    method: 'post',
    data
  })
}

export function updateNav(data: Partial<SiteNavItem>): Promise<{ code: number }> {
  return request({
    url: '/system/nav',
    method: 'put',
    data
  })
}

export function delNav(ids: number | number[]): Promise<{ code: number }> {
  return request({
    url: '/system/nav/' + (Array.isArray(ids) ? ids.join(',') : ids),
    method: 'delete'
  })
}

/** 模板包导航导入：清空指定位置后按包数据重建（事务内） */
export function importNav(
  location: 'header' | 'footer',
  items: Partial<SiteNavItem>[]
): Promise<{ code: number }> {
  return request({
    url: '/system/nav/import',
    method: 'post',
    data: { location, items }
  })
}
