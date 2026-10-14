package com.zhi.system.service;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import com.zhi.system.domain.VisitLog;

/**
 * 访问明细Service接口
 * 
 * @author nevell
 * @date 2026-09-10
 */
public interface IVisitLogService 
{
    /**
     * 记录一次内容访问（PV 明细；is_unique 表示当日该访客首次访问，即 UV 口径）
     * 
     * <p>写入失败不得影响正常访问，实现需自行吞掉异常。</p>
     * 
     * @param targetType 目标类型（article/page）
     * @param targetId 目标ID
     * @param path 访问路径
     * @param viewerKey 访客标识（u&lt;id&gt; 或 ip:&lt;ip&gt;），为空时按请求IP兜底
     * @param request 当前请求（取 IP/UA/Referer）
     */
    public void recordVisit(String targetType, Long targetId, String path, String viewerKey, HttpServletRequest request);

    /**
     * 查询访问明细列表
     * 
     * @param visitLog 查询条件
     * @return 明细集合
     */
    public List<VisitLog> selectVisitLogList(VisitLog visitLog);

    /**
     * 区间汇总
     * 
     * @param days 最近天数
     * @return pv/uv/days/todayPv/todayUv/topTargets
     */
    public Map<String, Object> selectVisitSummary(Integer days);

    /**
     * 批量删除明细
     * 
     * @param ids 明细ID数组
     * @return 结果
     */
    public int deleteVisitLogByIds(Long[] ids);

    /**
     * 清理指定天数之前的明细
     * 
     * @param days 保留天数
     * @return 清理行数
     */
    public int cleanVisitLogs(Integer days);
}
