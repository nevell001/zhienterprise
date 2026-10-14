package com.zhi.system.mapper;

import java.util.List;
import java.util.Map;
import com.zhi.system.domain.VisitLog;

/**
 * 访问明细Mapper接口
 * 
 * @author nevell
 * @date 2026-09-10
 */
public interface VisitLogMapper 
{
    /**
     * 查询访问明细列表
     * 
     * @param visitLog 查询条件
     * @return 明细集合
     */
    public List<VisitLog> selectVisitLogList(VisitLog visitLog);

    /**
     * 通过ID查询单条明细
     * 
     * @param id 明细ID
     * @return 明细
     */
    public VisitLog selectVisitLogById(Long id);

    /**
     * 区间汇总（PV/UV/天数）
     * 
     * @param params 查询条件（beginDate/endDate）
     * @return 汇总结果
     */
    public Map<String, Object> selectVisitSummary(Map<String, Object> params);

    /**
     * 访问量最高的目标（文章/页面）
     * 
     * @param params 查询条件（beginDate/endDate/limit）
     * @return 排行集合
     */
    public List<Map<String, Object>> selectTopTargets(Map<String, Object> params);

    /**
     * 新增访问明细
     * 
     * @param visitLog 明细
     * @return 结果
     */
    public int insertVisitLog(VisitLog visitLog);

    /**
     * 批量删除明细
     * 
     * @param ids 明细ID数组
     * @return 结果
     */
    public int deleteVisitLogByIds(Long[] ids);

    /**
     * 删除指定日期之前的明细
     * 
     * @param date 日期（yyyy-MM-dd）
     * @return 结果
     */
    public int deleteVisitLogBefore(String date);

    /**
     * 清空全部明细
     * 
     * @return 结果
     */
    public int truncateVisitLog();
}
