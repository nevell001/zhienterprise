import request from '@/utils/request'
import type { SiteSetting, PageParams, QueryResult, OperResult, DataResult } from '@/types'

/**
 * 查询站点设置列表
 */
export function listSetting(query?: PageParams): Promise<QueryResult<SiteSetting>> {
  return request({
    url: '/system/setting/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询站点设置详细
 */
export function getSetting(id: number): Promise<SiteSetting> {
  return request({
    url: '/system/setting/' + id,
    method: 'get'
  })
}

/**
 * 新增站点设置
 */
export function addSetting(data: SiteSetting): Promise<OperResult> {
  return request({
    url: '/system/setting',
    method: 'post',
    data: data
  })
}

/**
 * 修改站点设置
 */
export function updateSetting(data: SiteSetting): Promise<OperResult> {
  return request({
    url: '/system/setting',
    method: 'put',
    data: data
  })
}

/**
 * 删除站点设置
 */
export function delSetting(id: number | number[]): Promise<OperResult> {
  return request({
    url: '/system/setting/' + id,
    method: 'delete'
  })
}

/**
 * 根据键更新设置值
 */
export function updateSettingValueByKey(key: string, value: string): Promise<OperResult> {
  return request({
    url: '/system/setting/updateByKey',
    method: 'post',
    data: { settingKey: key, settingValue: value }
  })
}

/**
 * 获取站点设置（前台用）
 */
export function getSiteSettings(): Promise<DataResult<Record<string, unknown>>> {
  return request({
    url: '/common/site/setting',
    method: 'get',
    headers: { isToken: false },
    params: { _t: Date.now() }
  })
}

/**
 * 匿名访问站点设置
 */
export function getSiteSettingsAnonymous(): Promise<DataResult<Record<string, unknown>>> {
  return request({
    url: '/common/site/setting',
    method: 'get',
    headers: { isToken: false },
    params: { _t: Date.now() }
  })
}

/**
 * 清除站点设置缓存
 */
export function clearSiteCache(): Promise<{ code: number; msg: string }> {
  return request({
    url: '/common/site/setting/clear-site-cache',
    method: 'get'
  })
}
