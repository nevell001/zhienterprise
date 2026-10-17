package com.zhi.system.service.impl;

import com.zhi.system.domain.Article;
import com.zhi.system.domain.Tag;
import com.zhi.system.mapper.ArticleMapper;
import com.zhi.system.mapper.ArticleTagMapper;
import com.zhi.system.mapper.TagMapper;
import com.zhi.system.service.IArticleTagService;
import com.zhi.common.core.redis.RedisCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.exception.DuplicateArticleTitleException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 博客文章服务层单元测试
 * 
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ArticleServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleTagMapper articleTagMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private IArticleTagService articleTagService;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private ArticleServiceImpl articleService;

    private Article testArticle;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("测试文章标题");
        testArticle.setContent("测试文章内容");
        testArticle.setStatus(0L);
    }

    /**
     * 测试更新文章状态 - 标题非空校验
     */
    @Test
    void testUpdateArticleStatus_TitleNotNull() {
        // 模拟数据库操作
        when(articleMapper.selectArticleById(1L)).thenReturn(testArticle);
        when(articleMapper.updateArticle(any(Article.class))).thenReturn(1);

        // 执行测试方法
        Integer result = articleService.updateArticleStatus(Collections.singletonList(1L), 1);

        // 验证结果
        assertNotNull(result);
        assertTrue(result > 0);
        verify(articleMapper).updateArticle(any(Article.class));
    }

    /**
     * 测试更新文章状态 - 标题为空时抛出异常
     */
    @Test
    void testUpdateArticleStatus_TitleNull() {
        // 设置标题为空
        testArticle.setTitle(null);
        when(articleMapper.selectArticleById(1L)).thenReturn(testArticle);

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> articleService.updateArticleStatus(Collections.singletonList(1L), 1));
        
        assertEquals("文章标题不能为空", exception.getMessage());
        verify(articleMapper, never()).updateArticle(any(Article.class));
    }

    /**
     * 测试新增文章 - 标题唯一性校验
     */
    @Test
    void testInsertArticle_TitleDuplicate() {
        // 模拟数据库中已存在相同标题的文章
        when(articleMapper.selectArticleByTitle("测试文章标题")).thenReturn(testArticle);

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> articleService.insertArticle(testArticle));
        
        assertEquals("文章标题已存在", exception.getMessage());
        verify(articleMapper, never()).insertArticle(any(Article.class));
    }

    /**
     * 测试新增文章 - 内容非空校验
     */
    @Test
    void testInsertArticle_ContentNull() {
        // 设置内容为空
        testArticle.setContent(null);

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> articleService.insertArticle(testArticle));
        
        assertEquals("文章内容不能为空", exception.getMessage());
    }

    /**
     * 测试新增文章 - 内容为空字符串
     */
    @Test
    void testInsertArticle_ContentEmpty() {
        // 设置内容为空字符串
        testArticle.setContent("");

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> articleService.insertArticle(testArticle));
        
        assertEquals("文章内容不能为空", exception.getMessage());
    }

    /**
     * 测试新增文章 - 标题为空
     */
    @Test
    void testInsertArticle_TitleNull() {
        // 设置标题为空
        testArticle.setTitle(null);

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> articleService.insertArticle(testArticle));
        
        assertEquals("文章标题不能为空", exception.getMessage());
    }

    /**
     * 测试新增文章 - 标题为空字符串
     */
    @Test
    void testInsertArticle_TitleEmpty() {
        // 设置标题为空字符串
        testArticle.setTitle("");

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> articleService.insertArticle(testArticle));
        
        assertEquals("文章标题不能为空", exception.getMessage());
    }

    /**
     * 测试新增文章 - 标题前后有空格
     */
    @Test
    void testInsertArticle_TitleWithSpaces() {
        // 设置标题前后有空格
        testArticle.setTitle("  测试文章标题  ");

        // 模拟SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            SysUser user = new SysUser();
            user.setUserId(1L);
            LoginUser loginUser = new LoginUser();
            loginUser.setUser(user);
            mockedSecurityUtils.when(SecurityUtils::getLoginUser).thenReturn(loginUser);

            when(articleMapper.selectArticleByTitle("测试文章标题")).thenReturn(null);
            when(articleMapper.insertArticle(any(Article.class))).thenReturn(1);

            // 执行测试
            int result = articleService.insertArticle(testArticle);

            // 验证结果
            assertEquals(1, result);
            verify(articleMapper).selectArticleByTitle("测试文章标题");
            verify(articleMapper).insertArticle(any(Article.class));
        }
    }

    /**
     * 回归守卫：写方法必须真正带 @Transactional。
     * 2026-10-14 的 slug 伪静态提交在 insertArticle 上方插入了一段 javadoc，把原有的
     * @Transactional 与 insertArticle 隔开，注解因此漂移到了紧随其后的只读方法
     * selectArticleBySlug 上 —— 文章写入与标签关联失去同一事务：processTagAssociations
     * 抛异常时文章已落库却不回滚，接口返回失败、库里留下幽灵文章，重试又被标题唯一校验拒绝。
     * 用反射断言注解确实绑在方法上（而非文件里出现过），避免同类漂移再次发生。
     */
    @Test
    void writeMethodsMustBeTransactional() {
        // 只覆盖「一次调用内写多张表、必须原子」的方法（文章 + 标签关联）。
        // 单语句的 updateArticleTopStatus/likeArticle 等由 DB 单语句原子性保证，不在此列。
        String[] writeMethods = { "insertArticle", "updateArticle", "deleteArticleByIds",
            "deleteArticleById", "insertArticleTagRelations", "updateArticleTagRelations",
            "deleteArticleTagRelations", "updateArticleStatus" };
        for (String name : writeMethods) {
            boolean found = java.util.Arrays.stream(ArticleServiceImpl.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .anyMatch(m -> m.isAnnotationPresent(
                    org.springframework.transaction.annotation.Transactional.class));
            assertTrue(found, name + " 必须带 @Transactional（写路径要与标签关联保持原子）");
        }
    }

    /**
     * 回归守卫：只读查询不得带 @Transactional —— 防止注解再次漂移到 selectArticleBySlug。
     */
    @Test
    void selectArticleBySlugMustNotBeTransactional() {
        boolean found = java.util.Arrays.stream(ArticleServiceImpl.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("selectArticleBySlug"))
            .anyMatch(m -> m.isAnnotationPresent(
                org.springframework.transaction.annotation.Transactional.class));
        assertFalse(found, "selectArticleBySlug 是只读查询，不应带 @Transactional（注解漂移的典型症状）");
    }

    /**
     * 测试更新文章 - 标题非空校验
     */
    @Test
    void testUpdateArticle_TitleNull() {
        // 设置标题为空
        testArticle.setTitle(null);

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> articleService.updateArticle(testArticle));
        
        assertEquals("文章标题不能为空", exception.getMessage());
    }

    /**
     * 测试更新文章 - 标题为空字符串
     */
    @Test
    void testUpdateArticle_TitleEmpty() {
        // 设置标题为空字符串
        testArticle.setTitle("");

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> articleService.updateArticle(testArticle));
        
        assertEquals("文章标题不能为空", exception.getMessage());
    }

    /**
     * 测试批量更新文章状态 - 成功场景
     */
    @Test
    void testUpdateArticleStatusBatch_Success() {
        // 模拟数据库操作
        when(articleMapper.selectArticleById(1L)).thenReturn(testArticle);
        when(articleMapper.updateArticle(any(Article.class))).thenReturn(1);

        // 执行测试方法
        Integer result = articleService.updateArticleStatus(Arrays.asList(1L), 1);

        // 验证结果
        assertNotNull(result);
        assertTrue(result > 0);
        verify(articleMapper).updateArticle(any(Article.class));
    }

    /**
     * 测试批量更新文章状态 - 空列表
     */
    @Test
    void testUpdateArticleStatusBatch_EmptyList() {
        // 执行测试方法
        Integer result = articleService.updateArticleStatus(Collections.emptyList(), 1);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result);
        verify(articleMapper, never()).updateArticle(any(Article.class));
    }

    /**
     * 测试查询文章列表 - 空结果
     */
    @Test
    void testSelectArticleList_Empty() {
        // 模拟空结果
        when(articleMapper.selectArticleList(any(Article.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<Article> result = articleService.selectArticleList(new Article());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleMapper).selectArticleList(any(Article.class));
    }

    /**
     * 测试查询文章详情 - 文章不存在
     */
    @Test
    void testSelectArticleById_NotFound() {
        // 模拟文章不存在
        when(articleMapper.selectArticleById(999L)).thenReturn(null);

        // 执行测试
        Article result = articleService.selectArticleById(999L);

        // 验证结果
        assertNull(result);
        verify(articleMapper).selectArticleById(999L);
    }

    /**
     * 测试查询文章数量
     */
    @Test
    void testSelectArticleCount() {
        // 模拟数据
        when(articleMapper.selectArticleCount(any(Article.class))).thenReturn(10L);

        // 执行测试
        Long result = articleService.selectArticleCount(new Article());

        // 验证结果
        assertEquals(10L, result);
        verify(articleMapper).selectArticleCount(any(Article.class));
    }

    /**
     * 测试查询文章数量 - 空结果
     */
    @Test
    void testSelectArticleCount_Empty() {
        // 模拟空结果
        when(articleMapper.selectArticleCount(any(Article.class))).thenReturn(0L);

        // 执行测试
        Long result = articleService.selectArticleCount(new Article());

        // 验证结果
        assertEquals(0L, result);
        verify(articleMapper).selectArticleCount(any(Article.class));
    }

    /**
     * 测试搜索文章
     */
    @Test
    void testSearchArticles() {
        // 准备数据
        List<Article> articleList = Arrays.asList(testArticle);
        when(articleMapper.searchArticles(anyString(), any(Article.class)))
            .thenReturn(articleList);

        // 执行测试
        List<Article> result = articleService.searchArticles("测试", new Article());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(articleMapper).searchArticles(anyString(), any(Article.class));
    }

    /**
     * 测试搜索文章 - 空结果
     */
    @Test
    void testSearchArticles_Empty() {
        // 模拟空结果
        when(articleMapper.searchArticles(anyString(), any(Article.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<Article> result = articleService.searchArticles("不存在的关键词", new Article());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleMapper).searchArticles(anyString(), any(Article.class));
    }

    /**
     * 测试获取总浏览量
     */
    @Test
    void testSelectTotalViewCount() {
        // 模拟数据
        when(articleMapper.selectTotalViewCount()).thenReturn(1000L);

        // 执行测试
        Long result = articleService.selectTotalViewCount();

        // 验证结果
        assertEquals(1000L, result);
        verify(articleMapper).selectTotalViewCount();
    }

    /**
     * 测试获取平均浏览量
     */
    @Test
    void testSelectAverageViewCount() {
        // 模拟数据
        when(articleMapper.selectAverageViewCount()).thenReturn(50.5);

        // 执行测试
        Double result = articleService.selectAverageViewCount();

        // 验证结果
        assertEquals(50.5, result);
        verify(articleMapper).selectAverageViewCount();
    }

    /**
     * 测试删除文章 - 成功
     */
    @Test
    void testDeleteArticleById_Success() {
        // 模拟数据
        when(articleMapper.deleteArticleById(1L)).thenReturn(1);

        // 执行测试
        int result = articleService.deleteArticleById(1L);

        // 验证结果
        assertEquals(1, result);
        verify(articleMapper).deleteArticleById(1L);
    }

    /**
     * 测试批量删除文章 - 成功
     */
    @Test
    void testDeleteArticleByIds_Success() {
        // 模拟数据
        when(articleMapper.deleteArticleByIds(any(Long[].class))).thenReturn(2);
        when(articleTagMapper.deleteByArticleId(anyLong())).thenReturn(1);

        // 执行测试
        int result = articleService.deleteArticleByIds(new Long[]{1L, 2L});

        // 验证结果
        assertEquals(2, result);
        verify(articleMapper).deleteArticleByIds(any(Long[].class));
        verify(articleTagMapper, atLeastOnce()).deleteByArticleId(anyLong());
    }

    /**
     * 测试增加文章浏览量
     */
    @Test
    void testAddViewCount() {
        // 执行测试
        articleService.addViewCount(1L);

        // 验证结果
        verify(redisCache).incrementCacheObject("zhi:article:view:1", 1);
        // 浏览量仅缓冲在 Redis，由 ArticleViewSyncTask 定时批量同步数据库，避免双重计数
        verify(articleMapper, never()).addViewCount(1L);
    }

    /**
     * 测试获取上一篇文章
     */
    @Test
    void testGetPrevArticle() {
        // 模拟数据
        Article prevArticle = new Article();
        prevArticle.setId(0L);
        prevArticle.setTitle("上一篇文章");
        when(articleMapper.getPrevArticle(1L)).thenReturn(prevArticle);

        // 执行测试
        Article result = articleService.getPrevArticle(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("上一篇文章", result.getTitle());
        verify(articleMapper).getPrevArticle(1L);
    }

    /**
     * 测试获取上一篇文章 - 不存在
     */
    @Test
    void testGetPrevArticle_NotFound() {
        // 模拟数据
        when(articleMapper.getPrevArticle(1L)).thenReturn(null);

        // 执行测试
        Article result = articleService.getPrevArticle(1L);

        // 验证结果
        assertNull(result);
        verify(articleMapper).getPrevArticle(1L);
    }

    /**
     * 测试获取下一篇文章
     */
    @Test
    void testGetNextArticle() {
        // 模拟数据
        Article nextArticle = new Article();
        nextArticle.setId(2L);
        nextArticle.setTitle("下一篇文章");
        when(articleMapper.getNextArticle(1L)).thenReturn(nextArticle);

        // 执行测试
        Article result = articleService.getNextArticle(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("下一篇文章", result.getTitle());
        verify(articleMapper).getNextArticle(1L);
    }

    /**
     * 测试获取下一篇文章 - 不存在
     */
    @Test
    void testGetNextArticle_NotFound() {
        // 模拟数据
        when(articleMapper.getNextArticle(1L)).thenReturn(null);

        // 执行测试
        Article result = articleService.getNextArticle(1L);

        // 验证结果
        assertNull(result);
        verify(articleMapper).getNextArticle(1L);
    }

    /**
     * 测试获取文章归档
     */
    @Test
    void testGetArticleArchive() {
        // 模拟数据
        List<java.util.Map<String, Object>> archiveList = Arrays.asList(
            java.util.Map.of("archiveDate", "2025-01", "count", 10),
            java.util.Map.of("archiveDate", "2024-12", "count", 5)
        );
        when(articleMapper.getArticleArchive()).thenReturn(archiveList);

        // 执行测试
        List<java.util.Map<String, Object>> result = articleService.getArticleArchive();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(articleMapper).getArticleArchive();
    }

    /**
     * 测试根据标签ID查询文章
     */
    @Test
    void testSelectArticlesByTagId() {
        // 准备数据
        List<Article> articleList = Arrays.asList(testArticle);
        when(articleMapper.selectArticlesByTagId(1L)).thenReturn(articleList);

        // 执行测试
        List<Article> result = articleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(articleMapper).selectArticlesByTagId(1L);
    }

    /**
     * 测试根据标签ID查询文章 - 空结果
     */
    @Test
    void testSelectArticlesByTagId_Empty() {
        // 模拟空结果
        when(articleMapper.selectArticlesByTagId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        List<Article> result = articleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleMapper).selectArticlesByTagId(1L);
    }

    /**
     * 测试插入文章标签关联关系
     */
    @Test
    void testInsertArticleTagRelations() {
        // 模拟数据
        when(articleTagMapper.insertArticleTag(any(com.zhi.system.domain.ArticleTag.class))).thenReturn(1);

        // 执行测试
        int result = articleService.insertArticleTagRelations(1L, Arrays.asList(1L, 2L, 3L));

        // 验证结果
        assertEquals(3, result);
        verify(articleTagMapper, times(3)).insertArticleTag(any(com.zhi.system.domain.ArticleTag.class));
    }

    /**
     * 测试插入文章标签关联关系 - 空列表
     */
    @Test
    void testInsertArticleTagRelations_Empty() {
        // 执行测试
        int result = articleService.insertArticleTagRelations(1L, Collections.emptyList());

        // 验证结果
        assertEquals(0, result);
        verify(articleTagMapper, never()).insertArticleTag(any(com.zhi.system.domain.ArticleTag.class));
    }

    /**
     * 测试插入文章标签关联关系 - null列表
     */
    @Test
    void testInsertArticleTagRelations_Null() {
        // 执行测试
        int result = articleService.insertArticleTagRelations(1L, null);

        // 验证结果
        assertEquals(0, result);
        verify(articleTagMapper, never()).insertArticleTag(any(com.zhi.system.domain.ArticleTag.class));
    }

    /**
     * 测试更新文章标签关联关系
     */
    @Test
    void testUpdateArticleTagRelations() {
        // 模拟数据
        when(articleTagMapper.deleteByArticleId(1L)).thenReturn(2);
        when(articleTagMapper.insertArticleTag(any(com.zhi.system.domain.ArticleTag.class))).thenReturn(1);

        // 执行测试
        int result = articleService.updateArticleTagRelations(1L, Arrays.asList(1L, 2L, 3L));

        // 验证结果
        assertEquals(3, result);
        verify(articleTagMapper).deleteByArticleId(1L);
        verify(articleTagMapper, times(3)).insertArticleTag(any(com.zhi.system.domain.ArticleTag.class));
    }

    /**
     * 测试删除文章标签关联关系
     */
    @Test
    void testDeleteArticleTagRelations() {
        // 模拟数据
        when(articleTagMapper.deleteByArticleId(1L)).thenReturn(2);

        // 执行测试
        int result = articleService.deleteArticleTagRelations(1L);

        // 验证结果
        assertEquals(2, result);
        verify(articleTagMapper).deleteByArticleId(1L);
    }

    /**
     * 测试获取文章发布趋势
     */
    @Test
    void testSelectArticleTrend() {
        // 模拟数据
        List<java.util.Map<String, Object>> trendList = Arrays.asList(
            java.util.Map.of("month", "2025-01", "count", 10),
            java.util.Map.of("month", "2024-12", "count", 5)
        );
        when(articleMapper.selectArticleTrend()).thenReturn(trendList);

        // 执行测试
        List<java.util.Map<String, Object>> result = articleService.selectArticleTrend();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(articleMapper).selectArticleTrend();
    }

    /**
     * 测试查询文章列表（带缓存）
     */
    @Test
    void testSelectArticleListWithCache() {
        // 准备数据
        List<Article> articleList = Arrays.asList(testArticle);
        when(articleMapper.selectArticleList(any(Article.class))).thenReturn(articleList);

        // 执行测试
        List<Article> result = articleService.selectArticleListWithCache(new Article());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(articleMapper).selectArticleList(any(Article.class));
    }

    /**
     * 测试获取热门文章
     */
    @Test
    void testSelectHotArticles() {
        // 准备数据
        List<Article> hotArticles = Arrays.asList(testArticle);
        when(articleMapper.selectHotArticles(any(Article.class))).thenReturn(hotArticles);

        // 执行测试
        List<Article> result = articleService.selectHotArticles(new Article());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(articleMapper).selectHotArticles(any(Article.class));
    }

    /**
     * 测试获取热门文章 - 空结果
     */
    @Test
    void testSelectHotArticles_Empty() {
        // 模拟空结果
        when(articleMapper.selectHotArticles(any(Article.class))).thenReturn(Collections.emptyList());

        // 执行测试
        List<Article> result = articleService.selectHotArticles(new Article());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleMapper).selectHotArticles(any(Article.class));
    }

    /**
     * 测试根据归档月份获取文章列表
     */
    @Test
    void testSelectArticlesByArchive() {
        // 准备数据
        Article query = new Article();
        query.setArchiveDate("2025-01");
        List<Article> articleList = Arrays.asList(testArticle);
        when(articleMapper.selectArticlesByArchive(any(Article.class))).thenReturn(articleList);

        // 执行测试
        List<Article> result = articleService.selectArticlesByArchive(query);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(articleMapper).selectArticlesByArchive(any(Article.class));
    }

    /**
     * 测试根据归档月份获取文章列表 - 空结果
     */
    @Test
    void testSelectArticlesByArchive_Empty() {
        // 准备数据
        Article query = new Article();
        query.setArchiveDate("2025-01");
        when(articleMapper.selectArticlesByArchive(any(Article.class))).thenReturn(Collections.emptyList());

        // 执行测试
        List<Article> result = articleService.selectArticlesByArchive(query);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleMapper).selectArticlesByArchive(any(Article.class));
    }

    /**
     * 测试查询文章列表 - 带标签
     */
    @Test
    void testSelectArticleList_WithTags() {
        // 准备数据
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("测试标签");

        List<Article> articleList = Arrays.asList(testArticle);

        // 创建标签映射数据（模拟 XML 返回的 Map 结构）
        List<Map<String, Object>> tagMappings = new ArrayList<>();
        Map<String, Object> tagMapping = new HashMap<>();
        tagMapping.put("articleId", 1L);
        tagMapping.put("id", 1L);
        tagMapping.put("name", "测试标签");
        tagMapping.put("description", null);
        tagMapping.put("color", null);
        tagMapping.put("icon", null);
        tagMapping.put("delFlag", 0);
        tagMappings.add(tagMapping);

        when(articleMapper.selectArticleList(any(Article.class))).thenReturn(articleList);
        when(tagMapper.selectTagsByArticleIds(anyList())).thenReturn(tagMappings);

        // 执行测试
        List<Article> result = articleService.selectArticleList(new Article());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTags());
        assertEquals(1, result.get(0).getTags().size());
        assertEquals("测试标签", result.get(0).getTags().get(0).getName());
        verify(articleMapper).selectArticleList(any(Article.class));
        verify(tagMapper).selectTagsByArticleIds(anyList());
    }

    /**
     * 测试查询文章详情 - 带标签
     */
    @Test
    void testSelectArticleById_WithTags() {
        // 准备数据
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("测试标签");
        List<Tag> tagList = Arrays.asList(tag);

        when(articleMapper.selectArticleById(1L)).thenReturn(testArticle);
        when(tagMapper.selectTagsByArticleId(1L)).thenReturn(tagList);

        // 执行测试
        Article result = articleService.selectArticleById(1L);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getTags());
        assertEquals(1, result.getTags().size());
        assertEquals("测试标签", result.getTags().get(0).getName());
        verify(articleMapper).selectArticleById(1L);
        verify(tagMapper).selectTagsByArticleId(1L);
    }

    /**
     * 测试查询文章列表 - 文章 ID 为 null
     */
    @Test
    void testSelectArticleList_ArticleIdNull() {
        // 准备数据 - 文章 ID 为 null
        Article articleWithNullId = new Article();
        articleWithNullId.setTitle("测试文章");
        articleWithNullId.setId(null);

        when(articleMapper.selectArticleList(any(Article.class)))
            .thenReturn(Arrays.asList(articleWithNullId));

        // 执行测试
        List<Article> result = articleService.selectArticleList(new Article());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getId());
        verify(articleMapper).selectArticleList(any(Article.class));
        verify(tagMapper, never()).selectTagsByArticleId(anyLong());
    }

    /**
     * 测试查询文章列表 - 标签为空列表
     */
    @Test
    void testSelectArticleList_TagsNull() {
        // 准备数据
        Article article = new Article();
        article.setId(1L);
        article.setTitle("测试文章");

        List<Map<String, Object>> tagMappings = new ArrayList<>();

        when(articleMapper.selectArticleList(any(Article.class)))
            .thenReturn(Arrays.asList(article));
        when(tagMapper.selectTagsByArticleIds(anyList())).thenReturn(tagMappings);

        // 执行测试
        List<Article> result = articleService.selectArticleList(new Article());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTags());
        assertEquals(0, result.get(0).getTags().size());
        verify(articleMapper).selectArticleList(any(Article.class));
        verify(tagMapper).selectTagsByArticleIds(anyList());
    }

    /**
     * 测试查询文章列表 - 标签 ID 为 null
     */
    @Test
    void testSelectArticleList_TagIdNull() {
        // 准备数据 - 标签 ID 为 null
        Article article = new Article();
        article.setId(1L);
        article.setTitle("测试文章");

        // 创建标签映射数据（标签 ID 为 null）
        List<Map<String, Object>> tagMappings = new ArrayList<>();
        Map<String, Object> tagMapping = new HashMap<>();
        tagMapping.put("articleId", 1L);
        tagMapping.put("id", null);  // 标签 ID 为 null
        tagMapping.put("name", "测试标签");
        tagMapping.put("description", null);
        tagMapping.put("color", null);
        tagMapping.put("icon", null);
        tagMapping.put("delFlag", 0);
        tagMappings.add(tagMapping);

        when(articleMapper.selectArticleList(any(Article.class)))
            .thenReturn(Arrays.asList(article));
        when(tagMapper.selectTagsByArticleIds(anyList())).thenReturn(tagMappings);

        // 执行测试
        List<Article> result = articleService.selectArticleList(new Article());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTags());
        assertEquals(1, result.get(0).getTags().size());
        assertTrue(result.get(0).getTagIds().isEmpty());
        verify(articleMapper).selectArticleList(any(Article.class));
        verify(tagMapper).selectTagsByArticleIds(anyList());
    }

    /**
     * 测试查询文章详情 - 标签为 null
     */
    @Test
    void testSelectArticleById_TagsNull() {
        // 模拟返回文章，但标签为 null
        when(articleMapper.selectArticleById(1L)).thenReturn(testArticle);
        when(tagMapper.selectTagsByArticleId(1L)).thenReturn(null);

        // 执行测试
        Article result = articleService.selectArticleById(1L);

        // 验证结果
        assertNotNull(result);
        assertNull(result.getTags());
        verify(articleMapper).selectArticleById(1L);
        verify(tagMapper).selectTagsByArticleId(1L);
    }

    /**
     * 测试查询文章详情 - 标签为空列表
     */
    @Test
    void testSelectArticleById_TagsEmpty() {
        // 模拟返回文章，但标签为空列表
        when(articleMapper.selectArticleById(1L)).thenReturn(testArticle);
        when(tagMapper.selectTagsByArticleId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        Article result = articleService.selectArticleById(1L);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
        verify(articleMapper).selectArticleById(1L);
        verify(tagMapper).selectTagsByArticleId(1L);
    }

    /**
     * 测试更新文章 - delFlag 为 null
     */
    @Test
    void testUpdateArticle_DelFlagNull() {
        // 设置 delFlag 为 null
        testArticle.setDelFlag(null);
        when(articleMapper.updateArticle(any(Article.class))).thenReturn(1);

        // 执行测试
        int result = articleService.updateArticle(testArticle);

        // 验证结果
        assertEquals(1, result);
        verify(articleMapper).updateArticle(any(Article.class));
    }

    /**
     * 测试更新文章 - 标签ID列表为 null
     */
    @Test
    void testUpdateArticle_TagIdsNull() {
        // 设置标签ID列表为 null
        testArticle.setTagIds(null);
        when(articleMapper.updateArticle(any(Article.class))).thenReturn(1);

        // 执行测试
        int result = articleService.updateArticle(testArticle);

        // 验证结果
        assertEquals(1, result);
        verify(articleMapper).updateArticle(any(Article.class));
        // 标签处理已移到 controller 层：tagIds 为 null 时不得删除原有关联
        verify(articleTagMapper, never()).deleteByArticleId(anyLong());
        verify(articleTagMapper, never()).insertArticleTag(any());
    }

    /**
     * 测试更新文章 - 标签ID列表为空
     */
    @Test
    void testUpdateArticle_TagIdsEmpty() {
        // 设置标签ID列表为空
        testArticle.setTagIds(Collections.emptyList());
        when(articleMapper.updateArticle(any(Article.class))).thenReturn(1);

        // 执行测试
        int result = articleService.updateArticle(testArticle);

        // 验证结果
        assertEquals(1, result);
        verify(articleMapper).updateArticle(any(Article.class));
        // 标签处理已移到 controller 层：tagIds 为空时不得删除原有关联
        verify(articleTagMapper, never()).deleteByArticleId(anyLong());
        verify(articleTagMapper, never()).insertArticleTag(any());
    }

    /**
     * 测试更新文章 - 重复标题异常
     */
    @Test
    void testUpdateArticle_DuplicateTitle() {
        // 模拟重复标题异常
        when(articleMapper.updateArticle(any(Article.class)))
            .thenThrow(new RuntimeException("Duplicate entry 'title' for key 'idx_article_title'"));

        // 执行测试
        DuplicateArticleTitleException exception = assertThrows(DuplicateArticleTitleException.class,
                () -> articleService.updateArticle(testArticle));

        assertTrue(exception.getMessage().contains("文章标题已存在"));
    }

    /**
     * 测试更新文章 - 异常消息为 null
     */
    @Test
    void testUpdateArticle_ExceptionMessageNull() {
        // 模拟异常消息为 null
        when(articleMapper.updateArticle(any(Article.class)))
            .thenThrow(new NullPointerException());

        // 执行测试
        assertThrows(NullPointerException.class, 
                () -> articleService.updateArticle(testArticle));
    }

    /**
     * 测试根据标签ID查询文章 - 文章列表为空
     */
    @Test
    void testSelectArticlesByTagId_EmptyList() {
        // 模拟返回空列表
        when(articleMapper.selectArticlesByTagId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        List<Article> result = articleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleMapper).selectArticlesByTagId(1L);
    }

    /**
     * 测试根据标签ID查询文章 - 文章列表为 null
     */
    @Test
    void testSelectArticlesByTagId_NullList() {
        // 模拟返回 null
        when(articleMapper.selectArticlesByTagId(1L)).thenReturn(null);

        // 执行测试
        List<Article> result = articleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNull(result);
        verify(articleMapper).selectArticlesByTagId(1L);
    }

    /**
     * 测试根据标签ID查询文章 - 标签为空列表
     */
    @Test
    void testSelectArticlesByTagId_TagsEmpty() {
        // 模拟返回文章列表，但标签映射为空列表
        List<Map<String, Object>> tagMappings = new ArrayList<>();

        when(articleMapper.selectArticlesByTagId(1L)).thenReturn(Arrays.asList(testArticle));
        when(tagMapper.selectTagsByArticleIds(anyList())).thenReturn(tagMappings);

        // 执行测试
        List<Article> result = articleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTags());
        assertTrue(result.get(0).getTags().isEmpty());
        verify(articleMapper).selectArticlesByTagId(1L);
        verify(tagMapper).selectTagsByArticleIds(anyList());
    }

    /**
     * 测试查询文章列表 - 文章列表为空
     */
    @Test
    void testSelectArticleList_EmptyList() {
        // 模拟返回空列表
        when(articleMapper.selectArticleList(any(Article.class))).thenReturn(Collections.emptyList());

        // 执行测试
        List<Article> result = articleService.selectArticleList(new Article());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleMapper).selectArticleList(any(Article.class));
    }

    /**
     * 测试查询文章列表 - 文章列表为 null
     */
    @Test
    void testSelectArticleList_NullList() {
        // 模拟返回 null
        when(articleMapper.selectArticleList(any(Article.class))).thenReturn(null);

        // 执行测试
        List<Article> result = articleService.selectArticleList(new Article());

        // 验证结果
        assertNull(result);
        verify(articleMapper).selectArticleList(any(Article.class));
    }

    /**
     * 测试查询文章列表 - 文章为 null
     */
    @Test
    void testSelectArticleList_ArticleNull() {
        // 模拟返回包含 null 的列表
        Article nullArticle = null;
        when(articleMapper.selectArticleList(any(Article.class))).thenReturn(Arrays.asList(nullArticle));

        // 执行测试
        List<Article> result = articleService.selectArticleList(new Article());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0));
        verify(articleMapper).selectArticleList(any(Article.class));
    }

    /**
     * 测试批量更新置顶状态 - 成功
     */
    @Test
    void testUpdateArticleTopStatus_Success() {
        // 准备数据
        when(articleMapper.updateArticleTopStatus(anyList(), any())).thenReturn(2);

        // 执行测试
        int result = articleService.updateArticleTopStatus(Arrays.asList(1L, 2L), 1);

        // 验证结果
        assertEquals(2, result);
        verify(articleMapper).updateArticleTopStatus(Arrays.asList(1L, 2L), 1);
    }

    /**
     * 测试批量更新置顶状态 - 空列表
     */
    @Test
    void testUpdateArticleTopStatus_Empty() {
        // 执行测试
        int result = articleService.updateArticleTopStatus(Collections.emptyList(), 1);

        // 验证结果
        assertEquals(0, result);
        verify(articleMapper, never()).updateArticleTopStatus(anyList(), any());
    }

    /**
     * 测试批量更新置顶状态 - null 列表
     */
    @Test
    void testUpdateArticleTopStatus_Null() {
        // 执行测试
        int result = articleService.updateArticleTopStatus(null, 1);

        // 验证结果
        assertEquals(0, result);
        verify(articleMapper, never()).updateArticleTopStatus(anyList(), any());
    }

    /**
     * 测试批量更新推荐状态 - 成功
     */
    @Test
    void testUpdateArticleRecommendStatus_Success() {
        // 准备数据
        when(articleMapper.updateArticleRecommendStatus(anyList(), any())).thenReturn(2);

        // 执行测试
        int result = articleService.updateArticleRecommendStatus(Arrays.asList(1L, 2L), 1);

        // 验证结果
        assertEquals(2, result);
        verify(articleMapper).updateArticleRecommendStatus(Arrays.asList(1L, 2L), 1);
    }

    /**
     * 测试批量更新推荐状态 - 空列表
     */
    @Test
    void testUpdateArticleRecommendStatus_Empty() {
        // 执行测试
        int result = articleService.updateArticleRecommendStatus(Collections.emptyList(), 1);

        // 验证结果
        assertEquals(0, result);
        verify(articleMapper, never()).updateArticleRecommendStatus(anyList(), any());
    }

    /**
     * 测试点赞文章 - 成功
     */
    @Test
    void testLikeArticle_Success() {
        // 准备数据
        when(articleMapper.likeArticle(1L)).thenReturn(1);

        // 执行测试
        int result = articleService.likeArticle(1L);

        // 验证结果
        assertEquals(1, result);
        verify(articleMapper).likeArticle(1L);
    }

    /**
     * 测试点赞文章 - null ID
     */
    @Test
    void testLikeArticle_Null() {
        // 执行测试
        int result = articleService.likeArticle(null);

        // 验证结果
        assertEquals(0, result);
        verify(articleMapper, never()).likeArticle(anyLong());
    }

    /**
     * 测试查询相关文章 - 有同分类文章
     */
    @Test
    void testSelectRelatedArticles_Success() {
        // 准备数据
        Article related = new Article();
        related.setId(2L);
        related.setTitle("相关文章");
        when(articleMapper.selectRelatedArticles(1L, 6)).thenReturn(Collections.singletonList(related));

        // 执行测试
        List<Article> result = articleService.selectRelatedArticles(1L);

        // 验证结果
        assertEquals(1, result.size());
        verify(articleMapper).selectRelatedArticles(1L, 6);
        verify(articleMapper, never()).selectHotArticles(any(Article.class));
    }

    /**
     * 测试查询相关文章 - 无同分类文章时回退热门文章
     */
    @Test
    void testSelectRelatedArticles_FallbackToHot() {
        // 准备数据
        when(articleMapper.selectRelatedArticles(1L, 6)).thenReturn(Collections.emptyList());
        List<Article> hot = new ArrayList<>();
        for (long i = 0; i < 3; i++) {
            Article article = new Article();
            article.setId(i);
            hot.add(article);
        }
        when(articleMapper.selectHotArticles(any(Article.class))).thenReturn(hot);

        // 执行测试
        List<Article> result = articleService.selectRelatedArticles(1L);

        // 验证结果
        assertEquals(3, result.size());
        verify(articleMapper).selectHotArticles(any(Article.class));
    }

    /**
     * 测试查询相关文章 - 热门文章超过6篇时截断
     */
    @Test
    void testSelectRelatedArticles_FallbackToHot_Truncated() {
        // 准备数据
        when(articleMapper.selectRelatedArticles(1L, 6)).thenReturn(null);
        List<Article> hot = new ArrayList<>();
        for (long i = 0; i < 8; i++) {
            Article article = new Article();
            article.setId(i);
            hot.add(article);
        }
        when(articleMapper.selectHotArticles(any(Article.class))).thenReturn(hot);

        // 执行测试
        List<Article> result = articleService.selectRelatedArticles(1L);

        // 验证结果
        assertEquals(6, result.size());
        verify(articleMapper).selectHotArticles(any(Article.class));
    }
}
