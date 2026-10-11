import request from '@/utils/request'
import type { BlogSetting, DataResult, PageParams, QueryResult } from '@/types'

/**
 * 查询站点设置列表
 */
export function listSetting(
  query?: PageParams
): Promise<QueryResult<BlogSetting & Record<string, any>>> {
  return request({
    url: '/system/setting/list',
    method: 'get',
    params: query
  })
}

/**
 * 查询站点设置详细
 */
export function getSetting(id: number): Promise<DataResult<BlogSetting>> {
  return request({
    url: '/system/setting/' + id,
    method: 'get'
  })
}

/**
 * 根据配置键查询配置值
 */
export function getConfigByKey(configKey: string): Promise<any> {
  return request({
    url: '/system/setting/value/' + configKey,
    method: 'get'
  })
}

/**
 * 新增站点设置
 */
export function addSetting(data: Partial<BlogSetting> | Record<string, any>): Promise<any> {
  return request({
    url: '/system/setting',
    method: 'post',
    data: data
  })
}

/**
 * 修改站点设置
 */
export function updateSetting(data: Partial<BlogSetting> | Record<string, any>): Promise<any> {
  return request({
    url: '/system/setting',
    method: 'put',
    data: data
  })
}

/**
 * 删除站点设置
 */
export function delSetting(ids: number | number[]): Promise<any> {
  return request({
    url: '/system/setting/' + ids,
    method: 'delete'
  })
}

/**
 * 刷新缓存
 */
export function refreshCache(): Promise<any> {
  return request({
    url: '/system/setting/refreshCache',
    method: 'delete'
  })
}

/**
 * 根据键更新设置值
 */
export function updateSettingValueByKey(key: string, value: string): Promise<any> {
  return request({
    url: '/system/setting/updateByKey',
    method: 'post',
    data: { settingKey: key, settingValue: value }
  })
}

/**
 * 用户注册开关的生效值。
 * 该开关的真实数据源是 sys_config（两个注册入口都读它），后端按同一口径判定后返回布尔。
 */
export function getRegistrationSwitch(): Promise<DataResult<boolean>> {
  return request({
    url: '/system/setting/registration',
    method: 'get'
  })
}

/** 邮件（SMTP）配置读取（脱敏，不含密码明文） */
export interface MailConfigView {
  host: string
  port: number
  username: string
  hasPassword: boolean
  ssl: boolean
  starttls: boolean
  enabled: boolean
  /** 后端 email-code.dev-print-code：为真时验证码只打印日志、不会真实发信 */
  devPrintCode?: boolean
}

/** 邮件（SMTP）配置保存载荷；password 留空表示保留原密码 */
export interface MailConfigPayload {
  host: string
  port: number
  username: string
  password?: string
  ssl: boolean
  starttls: boolean
  enabled: boolean
}

/**
 * 获取邮件服务配置（脱敏）
 */
export function getMailConfig(): Promise<DataResult<MailConfigView>> {
  return request({
    url: '/system/setting/mail',
    method: 'get'
  })
}

/**
 * 保存邮件服务配置（热更新，立即生效）
 */
export function saveMailConfig(data: MailConfigPayload): Promise<DataResult<MailConfigView>> {
  return request({
    url: '/system/setting/mail',
    method: 'post',
    data
  })
}

/**
 * 测试邮件服务连接（传入当前表单则测表单值，密码留空沿用已存密码）
 * SMTP 连不上时会一直挂到连接超时，故放宽本请求超时，让后端把真实失败原因返回来，
 * 而不是被默认 10s 掐断成"接口请求超时"。
 */
export function testMailConfig(data: MailConfigPayload): Promise<any> {
  return request({
    url: '/system/setting/mail/test',
    method: 'post',
    timeout: 30000,
    data
  })
}
