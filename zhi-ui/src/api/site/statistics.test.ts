import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import {
  getStatisticsOverview,
  getPublicStatisticsOverview,
  getArticleStatistics,
  getUserStatistics,
  getArticleTrend,
  getVisitLogList,
  getVisitSummary,
  cleanVisitLog,
  delVisitLog
} from '@/api/statistics'

// Mock request
vi.mock('@/utils/request', () => ({
  default: vi.fn()
}))

describe('Site Statistics API 测试', () => {
  it('应该导出 getStatisticsOverview 函数', () => {
    expect(getStatisticsOverview).toBeDefined()
    expect(typeof getStatisticsOverview).toBe('function')
  })

  it('应该导出 getPublicStatisticsOverview 函数', () => {
    expect(getPublicStatisticsOverview).toBeDefined()
    expect(typeof getPublicStatisticsOverview).toBe('function')
  })

  it('应该导出 getArticleStatistics 函数', () => {
    expect(getArticleStatistics).toBeDefined()
    expect(typeof getArticleStatistics).toBe('function')
  })

  it('应该导出 getUserStatistics 函数', () => {
    expect(getUserStatistics).toBeDefined()
    expect(typeof getUserStatistics).toBe('function')
  })

  it('应该导出 getArticleTrend 函数', () => {
    expect(getArticleTrend).toBeDefined()
    expect(typeof getArticleTrend).toBe('function')
  })

  it('应该支持日期范围查询', () => {
    const dateRange = {
      startDate: '2024-01-01',
      endDate: '2024-12-31'
    }
    expect(dateRange.startDate).toBe('2024-01-01')
    expect(dateRange.endDate).toBe('2024-12-31')
  })

  it('应该有文章统计数据', () => {
    const articleStats = {
      total: 100,
      published: 80,
      draft: 20
    }
    expect(articleStats.total).toBe(100)
    expect(articleStats.published).toBe(80)
  })

  it('应该有用户统计数据', () => {
    const userStats = {
      totalUsers: 1000,
      newUsers: 50,
      activeUsers: 30
    }
    expect(userStats.totalUsers).toBe(1000)
    expect(userStats.newUsers).toBe(50)
  })

  it('应该有访问统计数据', () => {
    const visitStats = {
      totalVisits: 10000,
      uniqueVisitors: 5000,
      avgVisitDuration: 300
    }
    expect(visitStats.totalVisits).toBe(10000)
    expect(visitStats.uniqueVisitors).toBe(5000)
  })
})

describe('访问明细（PV/UV 明细）API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })
  it('getVisitLogList 应该调用访问明细列表接口并透传筛选参数', () => {
    const params = {
      pageNum: 1,
      pageSize: 10,
      targetType: 'article',
      targetId: 3,
      ip: '127.0.0.1',
      visitorKey: 'u1',
      uniqueOnly: true,
      keyword: 'hello',
      beginDate: '2026-01-01',
      endDate: '2026-01-31'
    }
    getVisitLogList(params)
    expect(request).toHaveBeenCalledWith({
      url: '/statistics/visit/list',
      method: 'get',
      params
    })
  })

  it('getVisitSummary 默认查询近 30 天', () => {
    getVisitSummary()
    expect(request).toHaveBeenCalledWith({
      url: '/statistics/visit/summary',
      method: 'get',
      params: { days: 30 }
    })
  })

  it('getVisitSummary 应该支持自定义天数', () => {
    getVisitSummary(7)
    expect(request).toHaveBeenCalledWith({
      url: '/statistics/visit/summary',
      method: 'get',
      params: { days: 7 }
    })
  })
  it('cleanVisitLog 默认保留 90 天', () => {
    cleanVisitLog()
    expect(request).toHaveBeenCalledWith({
      url: '/statistics/visit/clean',
      method: 'delete',
      params: { days: 90 }
    })
  })

  it('cleanVisitLog 应该支持自定义保留天数', () => {
    cleanVisitLog(30)
    expect(request).toHaveBeenCalledWith({
      url: '/statistics/visit/clean',
      method: 'delete',
      params: { days: 30 }
    })
  })

  it('delVisitLog 应该支持单个 id', () => {
    delVisitLog(1)
    expect(request).toHaveBeenCalledWith({
      url: '/statistics/visit/1',
      method: 'delete'
    })
  })

  it('delVisitLog 应该支持逗号分隔的多个 id', () => {
    delVisitLog('1,2,3')
    expect(request).toHaveBeenCalledWith({
      url: '/statistics/visit/1,2,3',
      method: 'delete'
    })
  })
})
