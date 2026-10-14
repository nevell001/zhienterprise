package com.zhi.system.service;

import java.util.List;
import java.util.Map;

/**
 * 文章按日 PV/UV 统计 Service
 *
 * @author nevell
 * @date 2026-09-05
 */
public interface IDailyStatsService
{
    /**
     * 将某日 Redis 计数（yyyyMMdd）汇总写入 zhi_article_daily_stats
     *
     * @param dateKey 日期键，如 20260905
     * @return 处理的文章数
     */
    public int upsertDailyFromRedis(String dateKey);

    /**
     * 清理某日（yyyyMMdd）的 PV/UV Redis 计数键
     *
     * @param dateKey 日期键
     * @return 清理的键数
     */
    public int cleanupDailyRedisKeys(String dateKey);

    /**
     * 查询近 days 天全站每日 PV/UV 聚合
     *
     * @param days 天数（默认 30）
     * @return [{statDate,pv,uv}, ...]
     */
    public List<Map<String, Object>> queryDailyAggregate(int days);
}
