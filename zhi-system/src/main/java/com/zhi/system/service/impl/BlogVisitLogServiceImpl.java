package com.zhi.system.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.utils.DateUtils;
import com.zhi.common.utils.ModernDateUtils;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.ip.IpUtils;
import com.zhi.system.domain.BlogVisitLog;
import com.zhi.system.mapper.BlogVisitLogMapper;
import com.zhi.system.service.IBlogVisitLogService;

/**
 * 访问明细Service业务层处理
 * 
 * @author nevell
 * @date 2026-09-10
 */
@Service
public class BlogVisitLogServiceImpl implements IBlogVisitLogService
{
    private static final Logger log = LoggerFactory.getLogger(BlogVisitLogServiceImpl.class);

    /** 每日独立访客去重键前缀（与详情一致，48h 覆盖跨日边界） */
    private static final String DAY_UNIQUE_KEY_PREFIX = "zhi:visit:day:";

    /** UA 最大存储长度 */
    private static final int MAX_USER_AGENT_LENGTH = 255;

    /** Referer 最大存储长度 */
    private static final int MAX_REFERER_LENGTH = 500;

    /** 默认保留天数 */
    private static final int DEFAULT_RETENTION_DAYS = 90;

    /** 排行默认条数 */
    private static final int DEFAULT_TOP_LIMIT = 10;

    @Autowired
    private BlogVisitLogMapper blogVisitLogMapper;

    @Autowired
    private UnifiedCacheManager unifiedCacheManager;

    /**
     * 记录一次内容访问（PV 明细；is_unique 表示当日该访客首次访问，即 UV 口径）
     */
    @Override
    public void recordVisit(String targetType, Long targetId, String path, String viewerKey,
            HttpServletRequest request)
    {
        try
        {
            if (StringUtils.isEmpty(targetType) || targetId == null)
            {
                return;
            }

            String ip = request == null ? null : IpUtils.getIpAddr(request);
            String visitor = StringUtils.isNotEmpty(viewerKey) ? viewerKey
                    : (StringUtils.isNotEmpty(ip) ? "ip:" + ip : null);
            if (StringUtils.isEmpty(visitor))
            {
                // 既没有登录身份也拿不到 IP 时无法判定独立访客，跳过记录
                return;
            }

            String visitDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.getNowDate());
            boolean unique = markDayUnique(visitDate, targetType, targetId, visitor);

            BlogVisitLog visitLog = new BlogVisitLog();
            visitLog.setTargetType(targetType);
            visitLog.setTargetId(targetId);
            visitLog.setPath(path);
            visitLog.setVisitorKey(visitor);
            visitLog.setIp(ip);
            visitLog.setIsUnique(unique ? "1" : "0");
            visitLog.setVisitDate(visitDate);
            visitLog.setCreateTime(ModernDateUtils.now());
            if (request != null)
            {
                visitLog.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
                visitLog.setReferer(truncate(request.getHeader("Referer"), MAX_REFERER_LENGTH));
            }
            try
            {
                visitLog.setUserId(SecurityUtils.getUserId());
            }
            catch (Exception ignored)
            {
                // 匿名访客
            }

            blogVisitLogMapper.insertBlogVisitLog(visitLog);
        }
        catch (Exception e)
        {
            // 明细记录失败不能影响正常访问
            log.warn("记录访问明细失败: type={}, targetId={}, error={}", targetType, targetId, e.getMessage());
        }
    }

    /**
     * 判定并占位“当日该访客首次访问”
     * 
     * @return true 表示当日首次（计入 UV）
     */
    private boolean markDayUnique(String visitDate, String targetType, Long targetId, String visitorKey)
    {
        String key = DAY_UNIQUE_KEY_PREFIX + visitDate + ":" + targetType + ":" + targetId + ":" + visitorKey;
        if (unifiedCacheManager.exists(key))
        {
            return false;
        }
        unifiedCacheManager.set(key, "1", 48, TimeUnit.HOURS);
        return true;
    }

    /**
     * 查询访问明细列表
     */
    @Override
    public List<BlogVisitLog> selectBlogVisitLogList(BlogVisitLog blogVisitLog)
    {
        return blogVisitLogMapper.selectBlogVisitLogList(blogVisitLog);
    }

    /**
     * 区间汇总
     */
    @Override
    public Map<String, Object> selectVisitSummary(Integer days)
    {
        int range = days == null || days < 1 ? 30 : days;
        String endDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.getNowDate());
        String beginDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, offsetDays(-(range - 1)));

        Map<String, Object> params = new HashMap<>();
        params.put("beginDate", beginDate);
        params.put("endDate", endDate);

        Map<String, Object> summary = blogVisitLogMapper.selectVisitSummary(params);
        Map<String, Object> result = new HashMap<>();
        result.put("beginDate", beginDate);
        result.put("endDate", endDate);
        result.put("days", range);
        result.put("pv", longValue(summary == null ? null : summary.get("pv")));
        result.put("uv", longValue(summary == null ? null : summary.get("uv")));

        params.put("beginDate", endDate);
        Map<String, Object> todaySummary = blogVisitLogMapper.selectVisitSummary(params);
        result.put("todayPv", longValue(todaySummary == null ? null : todaySummary.get("pv")));
        result.put("todayUv", longValue(todaySummary == null ? null : todaySummary.get("uv")));

        params.put("beginDate", beginDate);
        params.put("limit", DEFAULT_TOP_LIMIT);
        List<Map<String, Object>> topTargets = blogVisitLogMapper.selectTopTargets(params);
        result.put("topTargets", topTargets == null ? java.util.Collections.emptyList() : topTargets);

        return result;
    }

    /**
     * 批量删除明细
     */
    @Override
    public int deleteBlogVisitLogByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        return blogVisitLogMapper.deleteBlogVisitLogByIds(ids);
    }

    /**
     * 清理指定天数之前的明细（保留最近 days 天，最小 1 天，避免误清空）
     */
    @Override
    public int cleanVisitLogs(Integer days)
    {
        int retention = days == null || days < 1 ? DEFAULT_RETENTION_DAYS : days;
        String cutoff = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, offsetDays(-retention));
        return blogVisitLogMapper.deleteBlogVisitLogBefore(cutoff);
    }

    /**
     * 汇总字段兜底：SQL 的 sum() 在空区间返回 NULL，统一转成 0
     */
    private long longValue(Object value)
    {
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * 以今天为基准偏移天数
     */
    private Date offsetDays(int amount)
    {
        return ModernDateUtils.toDate(ModernDateUtils.getNowDateTime().plusDays(amount));
    }

    /**
     * 字符串截断
     */
    private String truncate(String value, int maxLength)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
