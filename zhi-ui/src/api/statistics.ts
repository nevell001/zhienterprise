import request from '@/utils/request'
import type { DataResult, OperResult, QueryResult } from '@/types'

/**
 * 获取数据概览统计（后台，需要 statistics:overview:list 权限）
 */
export function getStatisticsOverview(): Promise<any> {
  return request({
    url: '/system-stats/overview',
    method: 'get'
  })
}

/**
 * 获取前台公开展示的统计概览（匿名可访问，仅站点内容计数）
 */
export function getPublicStatisticsOverview(): Promise<any> {
  return request({
    url: '/blog/stats/overview',
    method: 'get'
  })
}

/**
 * 获取文章统计
 */
export function getArticleStatistics(): Promise<any> {
  return request({
    url: '/statistics/article',
    method: 'get'
  })
}

/**
 * 获取用户统计
 */
export function getUserStatistics(): Promise<any> {
  return request({
    url: '/statistics/user',
    method: 'get'
  })
}

/**
 * 获取文章发布趋势数据
 */
export function getArticleTrend(): Promise<any> {
  return request({
    url: '/statistics/article/trend',
    method: 'get'
  })
}

/**
 * 获取近 N 日全站 PV/UV
 */
export function getDailyPvUv(days = 30): Promise<any> {
  return request({
    url: '/statistics/daily/pvuv',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取用户活跃度数据
 */
export function getUserActivity(): Promise<any> {
  return request({
    url: '/statistics/user/activity',
    method: 'get'
  })
}

/**
 * 获取用户注册趋势数据
 */
export function getUserRegisterTrend(): Promise<any> {
  return request({
    url: '/statistics/user/register-trend',
    method: 'get'
  })
}

/**
 * 获取用户角色分布数据
 */
export function getUserRoleDistribution(): Promise<any> {
  return request({
    url: '/statistics/user/role-distribution',
    method: 'get'
  })
}

/**
 * 获取文章分类分布数据
 */
export function getArticleCategoryDistribution(): Promise<any> {
  return request({
    url: '/statistics/article/category-distribution',
    method: 'get'
  })
}

/**
 * 获取热门标签数据
 */
export function getHotTags(): Promise<any> {
  return request({
    url: '/statistics/article/hot-tags',
    method: 'get'
  })
}

/**
 * 访问明细记录
 */
export interface SiteVisitLog {
  id: number
  /** 目标类型（article 文章 / page 自定义页面） */
  targetType: 'article' | 'page'
  targetId: number
  /** 目标标题（后端 JOIN 取得，可能为空） */
  targetTitle?: string
  path?: string
  /** 登录用户ID（匿名为空） */
  userId?: number
  /** 访客标识（u<id> 或 ip:<ip>） */
  visitorKey?: string
  ip?: string
  userAgent?: string
  referer?: string
  /** 是否当日该访客首次访问（1是 0否） */
  isUnique: '0' | '1'
  visitDate?: string
  createTime?: string
}

/**
 * 访问明细查询参数
 */
export interface VisitQueryParams {
  pageNum?: number
  pageSize?: number
  targetType?: string
  targetId?: number
  ip?: string
  visitorKey?: string
  /** 只看独立访客 */
  uniqueOnly?: boolean
  /** 匹配标题或路径 */
  keyword?: string
  /** 开始日期 yyyy-MM-dd */
  beginDate?: string
  /** 结束日期 yyyy-MM-dd */
  endDate?: string
}

/**
 * 访问排行项
 */
export interface VisitTopTarget {
  targetType: 'article' | 'page'
  targetId: number
  title?: string
  pv: number
  uv: number
}

/**
 * 访问明细区间汇总
 */
export interface VisitSummary {
  beginDate: string
  endDate: string
  days: number
  pv: number
  uv: number
  todayPv: number
  todayUv: number
  topTargets: VisitTopTarget[]
}

/**
 * 查询访问明细列表（PV/UV 明细）
 */
export function getVisitLogList(query?: VisitQueryParams): Promise<QueryResult<SiteVisitLog>> {
  return request({
    url: '/statistics/visit/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取访问明细区间汇总（默认近 30 天）
 */
export function getVisitSummary(days = 30): Promise<DataResult<VisitSummary>> {
  return request({
    url: '/statistics/visit/summary',
    method: 'get',
    params: { days }
  })
}

/**
 * 清理指定天数之前的访问明细（默认保留 90 天）
 */
export function cleanVisitLog(days = 90): Promise<OperResult> {
  return request({
    url: '/statistics/visit/clean',
    method: 'delete',
    params: { days }
  })
}

/**
 * 删除访问明细（支持单个 id 或逗号分隔的多个 id）
 */
export function delVisitLog(ids: number | number[] | string): Promise<OperResult> {
  return request({
    url: '/statistics/visit/' + ids,
    method: 'delete'
  })
}
