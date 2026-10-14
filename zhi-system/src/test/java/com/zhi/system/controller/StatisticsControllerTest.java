package com.zhi.system.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 博客统计控制器单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@WebMvcTest(controllers = StatisticsController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.zhi.system.service.IArticleService articleService;

    @MockBean
    private com.zhi.system.service.ICategoryService categoryService;

    @MockBean
    private com.zhi.system.service.ITagService tagService;

    @MockBean
    private com.zhi.system.service.ICommentService commentService;

    @MockBean
    private com.zhi.system.service.ISysUserService userService;

    @MockBean
    private com.zhi.system.service.ISysLogininforService logininforService;

    @MockBean
    private com.zhi.system.service.IDailyStatsService dailyStatsService;

    @MockBean
    private com.zhi.system.service.IVisitLogService visitLogService;

    /**
     * 测试获取数据概览统计接口
     */
    @Test
    void testGetStatisticsOverview() throws Exception {
        // 模拟数据
        when(articleService.selectArticleCount(any(com.zhi.system.domain.Article.class)))
            .thenReturn(100L);
        when(userService.selectUserCount(any(com.zhi.common.core.domain.entity.SysUser.class)))
            .thenReturn(50L);
        when(commentService.selectCommentCount(any(com.zhi.system.domain.Comment.class)))
            .thenReturn(200L);
        when(articleService.selectTotalViewCount()).thenReturn(1000L);
        when(userService.selectOnlineUserCount(anyInt())).thenReturn(10L);
        when(logininforService.selectTodayLoginCount()).thenReturn(50L);

        // 执行测试
        mockMvc.perform(get("/statistics/overview")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.articleCount").value(100))
                .andExpect(jsonPath("$.data.userCount").value(50))
                .andExpect(jsonPath("$.data.commentCount").value(200))
                .andExpect(jsonPath("$.data.viewCount").value(1000))
                .andExpect(jsonPath("$.data.onlineUsers").value(10))
                .andExpect(jsonPath("$.data.todayVisits").value(50));
    }

    /**
     * 测试获取文章统计接口
     */
    @Test
    void testGetArticleStatistics() throws Exception {
        // 模拟数据
        when(articleService.selectArticleCount(any(com.zhi.system.domain.Article.class)))
            .thenReturn(80L);
        when(articleService.selectAverageViewCount()).thenReturn(15.5);

        // 执行测试
        mockMvc.perform(get("/statistics/article")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.publishedCount").value(80))
                .andExpect(jsonPath("$.data.draftCount").value(80))
                .andExpect(jsonPath("$.data.avgViews").value(15.5));
    }

    /**
     * 测试获取用户统计接口
     */
    @Test
    void testGetUserStatistics() throws Exception {
        // 模拟数据
        when(userService.selectUserCount(any(com.zhi.common.core.domain.entity.SysUser.class)))
            .thenReturn(100L);
        when(userService.selectActiveUserCount(anyInt())).thenReturn(60L);
        when(userService.selectNewUserCount(anyInt())).thenReturn(20L);
        when(userService.selectAdminUserCount()).thenReturn(5L);

        // 执行测试
        mockMvc.perform(get("/statistics/user")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(100))
                .andExpect(jsonPath("$.data.activeCount").value(60))
                .andExpect(jsonPath("$.data.newCount").value(20))
                .andExpect(jsonPath("$.data.adminCount").value(5));
    }

    /**
     * 测试获取文章发布趋势接口
     */
    @Test
    void testGetArticleTrend() throws Exception {
        // 模拟数据
        List<Map<String, Object>> trendData = new ArrayList<>();
        Map<String, Object> month1 = new HashMap<>();
        month1.put("month", "2025-01");
        month1.put("count", 10);
        trendData.add(month1);

        Map<String, Object> month2 = new HashMap<>();
        month2.put("month", "2025-02");
        month2.put("count", 15);
        trendData.add(month2);

        when(articleService.selectArticleTrend()).thenReturn(trendData);

        // 执行测试
        mockMvc.perform(get("/statistics/article/trend")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取用户活跃度接口
     */
    @Test
    void testGetUserActivity() throws Exception {
        // 模拟数据
        List<Map<String, Object>> activityData = new ArrayList<>();
        Map<String, Object> month1 = new HashMap<>();
        month1.put("month", "2025-01");
        month1.put("count", 50);
        activityData.add(month1);

        when(logininforService.selectUserActivity()).thenReturn(activityData);

        // 执行测试
        mockMvc.perform(get("/statistics/user/activity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取用户注册趋势接口
     */
    @Test
    void testGetUserRegisterTrend() throws Exception {
        // 执行测试
        mockMvc.perform(get("/statistics/user/register-trend")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取用户角色分布接口
     */
    @Test
    void testGetUserRoleDistribution() throws Exception {
        // 执行测试
        mockMvc.perform(get("/statistics/user/role-distribution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取文章分类分布接口
     */
    @Test
    void testGetArticleCategoryDistribution() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Category> categoryList = new ArrayList<>();
        com.zhi.system.domain.Category category = new com.zhi.system.domain.Category();
        category.setName("技术");
        category.setArticleCount(20);
        categoryList.add(category);

        when(categoryService.selectCategoryListForFront(any(com.zhi.system.domain.Category.class)))
            .thenReturn(categoryList);

        // 执行测试
        mockMvc.perform(get("/statistics/article/category-distribution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取热门标签接口
     */
    @Test
    void testGetHotTags() throws Exception {
        // 模拟数据
        List<Map<String, Object>> tagList = new ArrayList<>();
        Map<String, Object> tag = new HashMap<>();
        tag.put("name", "Java");
        tag.put("article_count", 15);
        tagList.add(tag);

        when(tagService.getTagCloud()).thenReturn(tagList);

        // 执行测试
        mockMvc.perform(get("/statistics/article/hot-tags")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取文章发布趋势接口 - 空数据
     */
    @Test
    void testGetArticleTrend_Empty() throws Exception {
        // 模拟空数据
        when(articleService.selectArticleTrend()).thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(get("/statistics/article/trend")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取用户活跃度接口 - 空数据
     */
    @Test
    void testGetUserActivity_Empty() throws Exception {
        // 模拟空数据
        when(logininforService.selectUserActivity()).thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(get("/statistics/user/activity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取文章分类分布接口 - 空数据
     */
    @Test
    void testGetArticleCategoryDistribution_Empty() throws Exception {
        // 模拟空数据
        when(categoryService.selectCategoryListForFront(any(com.zhi.system.domain.Category.class)))
            .thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(get("/statistics/article/category-distribution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取热门标签接口 - 空数据
     */
    @Test
    void testGetHotTags_Empty() throws Exception {
        // 模拟空数据
        when(tagService.getTagCloud()).thenReturn(new ArrayList<>());

        // 执行测试
        mockMvc.perform(get("/statistics/article/hot-tags")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.data").isArray());
    }

    /**
     * 测试获取文章统计接口 - 平均浏览量为 null
     */
    @Test
    void testGetArticleStatistics_AvgViewsNull() throws Exception {
        // 模拟平均浏览量为 null
        when(articleService.selectArticleCount(any(com.zhi.system.domain.Article.class)))
            .thenReturn(80L);
        when(articleService.selectAverageViewCount()).thenReturn(null);

        // 执行测试
        mockMvc.perform(get("/statistics/article")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.publishedCount").value(80))
                .andExpect(jsonPath("$.data.draftCount").value(80))
                .andExpect(jsonPath("$.data.avgViews").value(0.0));
    }

    /**
     * 测试获取用户统计接口 - null 值
     */
    @Test
    void testGetUserStatistics_NullValues() throws Exception {
        // 模拟 null 值
        when(userService.selectUserCount(any(com.zhi.common.core.domain.entity.SysUser.class)))
            .thenReturn(0L);
        when(userService.selectActiveUserCount(anyInt())).thenReturn(0L);
        when(userService.selectNewUserCount(anyInt())).thenReturn(0L);
        when(userService.selectAdminUserCount()).thenReturn(0L);

        // 执行测试
        mockMvc.perform(get("/statistics/user")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.activeCount").value(0))
                .andExpect(jsonPath("$.data.newCount").value(0))
                .andExpect(jsonPath("$.data.adminCount").value(0));
    }

    /**
     * 测试访问明细列表
     */
    @Test
    void testVisitList() throws Exception {
        java.util.List<com.zhi.system.domain.VisitLog> list = new java.util.ArrayList<>();
        com.zhi.system.domain.VisitLog visitLog = new com.zhi.system.domain.VisitLog();
        visitLog.setId(1L);
        visitLog.setTargetType("article");
        visitLog.setTargetId(9L);
        visitLog.setIsUnique("1");
        list.add(visitLog);

        when(visitLogService.selectVisitLogList(any(com.zhi.system.domain.VisitLog.class)))
            .thenReturn(list);

        mockMvc.perform(get("/statistics/visit/list").param("targetType", "article"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.rows[0].isUnique").value("1"));
    }

    /**
     * 测试访问明细汇总
     */
    @Test
    void testVisitSummary() throws Exception {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("pv", 100L);
        summary.put("uv", 40L);
        summary.put("todayPv", 5L);
        summary.put("todayUv", 3L);
        summary.put("topTargets", new java.util.ArrayList<>());
        when(visitLogService.selectVisitSummary(30)).thenReturn(summary);

        mockMvc.perform(get("/statistics/visit/summary").param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pv").value(100))
            .andExpect(jsonPath("$.data.uv").value(40));
    }

    /**
     * 测试清理访问明细
     */
    @Test
    void testCleanVisitLog() throws Exception {
        when(visitLogService.cleanVisitLogs(90)).thenReturn(12);

        mockMvc.perform(delete("/statistics/visit/clean").param("days", "90"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("已清理 12 条访问明细"));
    }

    /**
     * 测试批量删除访问明细
     */
    @Test
    void testRemoveVisitLog() throws Exception {
        when(visitLogService.deleteVisitLogByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/statistics/visit/1,2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(visitLogService).deleteVisitLogByIds(any(Long[].class));
    }
}