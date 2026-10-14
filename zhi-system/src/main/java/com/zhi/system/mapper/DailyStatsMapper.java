package com.zhi.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 文章按日 PV/UV 统计 Mapper
 *
 * @author nevell
 * @date 2026-09-05
 */
public interface DailyStatsMapper
{
    /**
     * 插入或覆盖某篇文章某日的 PV/UV
     */
    public int upsertDailyStat(@Param("articleId") Long articleId, @Param("statDate") String statDate,
            @Param("pv") long pv, @Param("uv") long uv);

    /**
     * 查询某日起的全站每日 PV/UV 聚合
     */
    public List<Map<String, Object>> selectDailyAggregate(@Param("startDate") String startDate);
}
