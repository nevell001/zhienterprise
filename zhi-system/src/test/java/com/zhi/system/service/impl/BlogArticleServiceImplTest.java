package com.zhi.system.service.impl;

import com.zhi.system.domain.BlogArticle;
import com.zhi.system.domain.BlogTag;
import com.zhi.system.mapper.BlogArticleMapper;
import com.zhi.system.mapper.BlogArticleTagMapper;
import com.zhi.system.mapper.BlogTagMapper;
import com.zhi.system.service.IBlogArticleTagService;
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
class BlogArticleServiceImplTest {

    @Mock
    private BlogArticleMapper blogArticleMapper;

    @Mock
    private BlogArticleTagMapper blogArticleTagMapper;

    @Mock
    private BlogTagMapper blogTagMapper;

    @Mock
    private IBlogArticleTagService blogArticleTagService;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private BlogArticleServiceImpl blogArticleService;

    private BlogArticle testArticle;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testArticle = new BlogArticle();
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
        when(blogArticleMapper.selectBlogArticleById(1L)).thenReturn(testArticle);
        when(blogArticleMapper.updateBlogArticle(any(BlogArticle.class))).thenReturn(1);

        // 执行测试方法
        Integer result = blogArticleService.updateArticleStatus(Collections.singletonList(1L), 1);

        // 验证结果
        assertNotNull(result);
        assertTrue(result > 0);
        verify(blogArticleMapper).updateBlogArticle(any(BlogArticle.class));
    }

    /**
     * 测试更新文章状态 - 标题为空时抛出异常
     */
    @Test
    void testUpdateArticleStatus_TitleNull() {
        // 设置标题为空
        testArticle.setTitle(null);
        when(blogArticleMapper.selectBlogArticleById(1L)).thenReturn(testArticle);

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> blogArticleService.updateArticleStatus(Collections.singletonList(1L), 1));
        
        assertEquals("文章标题不能为空", exception.getMessage());
        verify(blogArticleMapper, never()).updateBlogArticle(any(BlogArticle.class));
    }

    /**
     * 测试新增文章 - 标题唯一性校验
     */
    @Test
    void testInsertArticle_TitleDuplicate() {
        // 模拟数据库中已存在相同标题的文章
        when(blogArticleMapper.selectBlogArticleByTitle("测试文章标题")).thenReturn(testArticle);

        // 验证抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> blogArticleService.insertBlogArticle(testArticle));
        
        assertEquals("文章标题已存在", exception.getMessage());
        verify(blogArticleMapper, never()).insertBlogArticle(any(BlogArticle.class));
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
                () -> blogArticleService.insertBlogArticle(testArticle));
        
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
                () -> blogArticleService.insertBlogArticle(testArticle));
        
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
                () -> blogArticleService.insertBlogArticle(testArticle));
        
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
                () -> blogArticleService.insertBlogArticle(testArticle));
        
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

            when(blogArticleMapper.selectBlogArticleByTitle("测试文章标题")).thenReturn(null);
            when(blogArticleMapper.insertBlogArticle(any(BlogArticle.class))).thenReturn(1);

            // 执行测试
            int result = blogArticleService.insertBlogArticle(testArticle);

            // 验证结果
            assertEquals(1, result);
            verify(blogArticleMapper).selectBlogArticleByTitle("测试文章标题");
            verify(blogArticleMapper).insertBlogArticle(any(BlogArticle.class));
        }
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
                () -> blogArticleService.updateBlogArticle(testArticle));
        
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
                () -> blogArticleService.updateBlogArticle(testArticle));
        
        assertEquals("文章标题不能为空", exception.getMessage());
    }

    /**
     * 测试批量更新文章状态 - 成功场景
     */
    @Test
    void testUpdateArticleStatusBatch_Success() {
        // 模拟数据库操作
        when(blogArticleMapper.selectBlogArticleById(1L)).thenReturn(testArticle);
        when(blogArticleMapper.updateBlogArticle(any(BlogArticle.class))).thenReturn(1);

        // 执行测试方法
        Integer result = blogArticleService.updateArticleStatus(Arrays.asList(1L), 1);

        // 验证结果
        assertNotNull(result);
        assertTrue(result > 0);
        verify(blogArticleMapper).updateBlogArticle(any(BlogArticle.class));
    }

    /**
     * 测试批量更新文章状态 - 空列表
     */
    @Test
    void testUpdateArticleStatusBatch_EmptyList() {
        // 执行测试方法
        Integer result = blogArticleService.updateArticleStatus(Collections.emptyList(), 1);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result);
        verify(blogArticleMapper, never()).updateBlogArticle(any(BlogArticle.class));
    }

    /**
     * 测试查询文章列表 - 空结果
     */
    @Test
    void testSelectBlogArticleList_Empty() {
        // 模拟空结果
        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleList(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
    }

    /**
     * 测试查询文章详情 - 文章不存在
     */
    @Test
    void testSelectBlogArticleById_NotFound() {
        // 模拟文章不存在
        when(blogArticleMapper.selectBlogArticleById(999L)).thenReturn(null);

        // 执行测试
        BlogArticle result = blogArticleService.selectBlogArticleById(999L);

        // 验证结果
        assertNull(result);
        verify(blogArticleMapper).selectBlogArticleById(999L);
    }

    /**
     * 测试查询文章数量
     */
    @Test
    void testSelectBlogArticleCount() {
        // 模拟数据
        when(blogArticleMapper.selectBlogArticleCount(any(BlogArticle.class))).thenReturn(10L);

        // 执行测试
        Long result = blogArticleService.selectBlogArticleCount(new BlogArticle());

        // 验证结果
        assertEquals(10L, result);
        verify(blogArticleMapper).selectBlogArticleCount(any(BlogArticle.class));
    }

    /**
     * 测试查询文章数量 - 空结果
     */
    @Test
    void testSelectBlogArticleCount_Empty() {
        // 模拟空结果
        when(blogArticleMapper.selectBlogArticleCount(any(BlogArticle.class))).thenReturn(0L);

        // 执行测试
        Long result = blogArticleService.selectBlogArticleCount(new BlogArticle());

        // 验证结果
        assertEquals(0L, result);
        verify(blogArticleMapper).selectBlogArticleCount(any(BlogArticle.class));
    }

    /**
     * 测试搜索文章
     */
    @Test
    void testSearchArticles() {
        // 准备数据
        List<BlogArticle> articleList = Arrays.asList(testArticle);
        when(blogArticleMapper.searchArticles(anyString(), any(BlogArticle.class)))
            .thenReturn(articleList);

        // 执行测试
        List<BlogArticle> result = blogArticleService.searchArticles("测试", new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(blogArticleMapper).searchArticles(anyString(), any(BlogArticle.class));
    }

    /**
     * 测试搜索文章 - 空结果
     */
    @Test
    void testSearchArticles_Empty() {
        // 模拟空结果
        when(blogArticleMapper.searchArticles(anyString(), any(BlogArticle.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<BlogArticle> result = blogArticleService.searchArticles("不存在的关键词", new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(blogArticleMapper).searchArticles(anyString(), any(BlogArticle.class));
    }

    /**
     * 测试获取总浏览量
     */
    @Test
    void testSelectTotalViewCount() {
        // 模拟数据
        when(blogArticleMapper.selectTotalViewCount()).thenReturn(1000L);

        // 执行测试
        Long result = blogArticleService.selectTotalViewCount();

        // 验证结果
        assertEquals(1000L, result);
        verify(blogArticleMapper).selectTotalViewCount();
    }

    /**
     * 测试获取平均浏览量
     */
    @Test
    void testSelectAverageViewCount() {
        // 模拟数据
        when(blogArticleMapper.selectAverageViewCount()).thenReturn(50.5);

        // 执行测试
        Double result = blogArticleService.selectAverageViewCount();

        // 验证结果
        assertEquals(50.5, result);
        verify(blogArticleMapper).selectAverageViewCount();
    }

    /**
     * 测试删除文章 - 成功
     */
    @Test
    void testDeleteBlogArticleById_Success() {
        // 模拟数据
        when(blogArticleMapper.deleteBlogArticleById(1L)).thenReturn(1);

        // 执行测试
        int result = blogArticleService.deleteBlogArticleById(1L);

        // 验证结果
        assertEquals(1, result);
        verify(blogArticleMapper).deleteBlogArticleById(1L);
    }

    /**
     * 测试批量删除文章 - 成功
     */
    @Test
    void testDeleteBlogArticleByIds_Success() {
        // 模拟数据
        when(blogArticleMapper.deleteBlogArticleByIds(any(Long[].class))).thenReturn(2);
        when(blogArticleTagMapper.deleteByArticleId(anyLong())).thenReturn(1);

        // 执行测试
        int result = blogArticleService.deleteBlogArticleByIds(new Long[]{1L, 2L});

        // 验证结果
        assertEquals(2, result);
        verify(blogArticleMapper).deleteBlogArticleByIds(any(Long[].class));
        verify(blogArticleTagMapper, atLeastOnce()).deleteByArticleId(anyLong());
    }

    /**
     * 测试增加文章浏览量
     */
    @Test
    void testAddViewCount() {
        // 执行测试
        blogArticleService.addViewCount(1L);

        // 验证结果
        verify(redisCache).incrementCacheObject("zhi:article:view:1", 1);
        // 浏览量仅缓冲在 Redis，由 ArticleViewSyncTask 定时批量同步数据库，避免双重计数
        verify(blogArticleMapper, never()).addViewCount(1L);
    }

    /**
     * 测试获取上一篇文章
     */
    @Test
    void testGetPrevArticle() {
        // 模拟数据
        BlogArticle prevArticle = new BlogArticle();
        prevArticle.setId(0L);
        prevArticle.setTitle("上一篇文章");
        when(blogArticleMapper.getPrevArticle(1L)).thenReturn(prevArticle);

        // 执行测试
        BlogArticle result = blogArticleService.getPrevArticle(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("上一篇文章", result.getTitle());
        verify(blogArticleMapper).getPrevArticle(1L);
    }

    /**
     * 测试获取上一篇文章 - 不存在
     */
    @Test
    void testGetPrevArticle_NotFound() {
        // 模拟数据
        when(blogArticleMapper.getPrevArticle(1L)).thenReturn(null);

        // 执行测试
        BlogArticle result = blogArticleService.getPrevArticle(1L);

        // 验证结果
        assertNull(result);
        verify(blogArticleMapper).getPrevArticle(1L);
    }

    /**
     * 测试获取下一篇文章
     */
    @Test
    void testGetNextArticle() {
        // 模拟数据
        BlogArticle nextArticle = new BlogArticle();
        nextArticle.setId(2L);
        nextArticle.setTitle("下一篇文章");
        when(blogArticleMapper.getNextArticle(1L)).thenReturn(nextArticle);

        // 执行测试
        BlogArticle result = blogArticleService.getNextArticle(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("下一篇文章", result.getTitle());
        verify(blogArticleMapper).getNextArticle(1L);
    }

    /**
     * 测试获取下一篇文章 - 不存在
     */
    @Test
    void testGetNextArticle_NotFound() {
        // 模拟数据
        when(blogArticleMapper.getNextArticle(1L)).thenReturn(null);

        // 执行测试
        BlogArticle result = blogArticleService.getNextArticle(1L);

        // 验证结果
        assertNull(result);
        verify(blogArticleMapper).getNextArticle(1L);
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
        when(blogArticleMapper.getArticleArchive()).thenReturn(archiveList);

        // 执行测试
        List<java.util.Map<String, Object>> result = blogArticleService.getArticleArchive();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(blogArticleMapper).getArticleArchive();
    }

    /**
     * 测试根据标签ID查询文章
     */
    @Test
    void testSelectArticlesByTagId() {
        // 准备数据
        List<BlogArticle> articleList = Arrays.asList(testArticle);
        when(blogArticleMapper.selectArticlesByTagId(1L)).thenReturn(articleList);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(blogArticleMapper).selectArticlesByTagId(1L);
    }

    /**
     * 测试根据标签ID查询文章 - 空结果
     */
    @Test
    void testSelectArticlesByTagId_Empty() {
        // 模拟空结果
        when(blogArticleMapper.selectArticlesByTagId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(blogArticleMapper).selectArticlesByTagId(1L);
    }

    /**
     * 测试插入文章标签关联关系
     */
    @Test
    void testInsertArticleTagRelations() {
        // 模拟数据
        when(blogArticleTagMapper.insertBlogArticleTag(any(com.zhi.system.domain.BlogArticleTag.class))).thenReturn(1);

        // 执行测试
        int result = blogArticleService.insertArticleTagRelations(1L, Arrays.asList(1L, 2L, 3L));

        // 验证结果
        assertEquals(3, result);
        verify(blogArticleTagMapper, times(3)).insertBlogArticleTag(any(com.zhi.system.domain.BlogArticleTag.class));
    }

    /**
     * 测试插入文章标签关联关系 - 空列表
     */
    @Test
    void testInsertArticleTagRelations_Empty() {
        // 执行测试
        int result = blogArticleService.insertArticleTagRelations(1L, Collections.emptyList());

        // 验证结果
        assertEquals(0, result);
        verify(blogArticleTagMapper, never()).insertBlogArticleTag(any(com.zhi.system.domain.BlogArticleTag.class));
    }

    /**
     * 测试插入文章标签关联关系 - null列表
     */
    @Test
    void testInsertArticleTagRelations_Null() {
        // 执行测试
        int result = blogArticleService.insertArticleTagRelations(1L, null);

        // 验证结果
        assertEquals(0, result);
        verify(blogArticleTagMapper, never()).insertBlogArticleTag(any(com.zhi.system.domain.BlogArticleTag.class));
    }

    /**
     * 测试更新文章标签关联关系
     */
    @Test
    void testUpdateArticleTagRelations() {
        // 模拟数据
        when(blogArticleTagMapper.deleteByArticleId(1L)).thenReturn(2);
        when(blogArticleTagMapper.insertBlogArticleTag(any(com.zhi.system.domain.BlogArticleTag.class))).thenReturn(1);

        // 执行测试
        int result = blogArticleService.updateArticleTagRelations(1L, Arrays.asList(1L, 2L, 3L));

        // 验证结果
        assertEquals(3, result);
        verify(blogArticleTagMapper).deleteByArticleId(1L);
        verify(blogArticleTagMapper, times(3)).insertBlogArticleTag(any(com.zhi.system.domain.BlogArticleTag.class));
    }

    /**
     * 测试删除文章标签关联关系
     */
    @Test
    void testDeleteArticleTagRelations() {
        // 模拟数据
        when(blogArticleTagMapper.deleteByArticleId(1L)).thenReturn(2);

        // 执行测试
        int result = blogArticleService.deleteArticleTagRelations(1L);

        // 验证结果
        assertEquals(2, result);
        verify(blogArticleTagMapper).deleteByArticleId(1L);
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
        when(blogArticleMapper.selectArticleTrend()).thenReturn(trendList);

        // 执行测试
        List<java.util.Map<String, Object>> result = blogArticleService.selectArticleTrend();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(blogArticleMapper).selectArticleTrend();
    }

    /**
     * 测试查询文章列表（带缓存）
     */
    @Test
    void testSelectBlogArticleListWithCache() {
        // 准备数据
        List<BlogArticle> articleList = Arrays.asList(testArticle);
        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class))).thenReturn(articleList);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleListWithCache(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
    }

    /**
     * 测试获取热门文章
     */
    @Test
    void testSelectHotArticles() {
        // 准备数据
        List<BlogArticle> hotArticles = Arrays.asList(testArticle);
        when(blogArticleMapper.selectHotArticles(any(BlogArticle.class))).thenReturn(hotArticles);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectHotArticles(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(blogArticleMapper).selectHotArticles(any(BlogArticle.class));
    }

    /**
     * 测试获取热门文章 - 空结果
     */
    @Test
    void testSelectHotArticles_Empty() {
        // 模拟空结果
        when(blogArticleMapper.selectHotArticles(any(BlogArticle.class))).thenReturn(Collections.emptyList());

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectHotArticles(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(blogArticleMapper).selectHotArticles(any(BlogArticle.class));
    }

    /**
     * 测试根据归档月份获取文章列表
     */
    @Test
    void testSelectArticlesByArchive() {
        // 准备数据
        BlogArticle query = new BlogArticle();
        query.setArchiveDate("2025-01");
        List<BlogArticle> articleList = Arrays.asList(testArticle);
        when(blogArticleMapper.selectArticlesByArchive(any(BlogArticle.class))).thenReturn(articleList);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectArticlesByArchive(query);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(blogArticleMapper).selectArticlesByArchive(any(BlogArticle.class));
    }

    /**
     * 测试根据归档月份获取文章列表 - 空结果
     */
    @Test
    void testSelectArticlesByArchive_Empty() {
        // 准备数据
        BlogArticle query = new BlogArticle();
        query.setArchiveDate("2025-01");
        when(blogArticleMapper.selectArticlesByArchive(any(BlogArticle.class))).thenReturn(Collections.emptyList());

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectArticlesByArchive(query);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(blogArticleMapper).selectArticlesByArchive(any(BlogArticle.class));
    }

    /**
     * 测试查询文章列表 - 带标签
     */
    @Test
    void testSelectBlogArticleList_WithTags() {
        // 准备数据
        BlogTag tag = new BlogTag();
        tag.setId(1L);
        tag.setName("测试标签");

        List<BlogArticle> articleList = Arrays.asList(testArticle);

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

        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class))).thenReturn(articleList);
        when(blogTagMapper.selectTagsByArticleIds(anyList())).thenReturn(tagMappings);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleList(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTags());
        assertEquals(1, result.get(0).getTags().size());
        assertEquals("测试标签", result.get(0).getTags().get(0).getName());
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
        verify(blogTagMapper).selectTagsByArticleIds(anyList());
    }

    /**
     * 测试查询文章详情 - 带标签
     */
    @Test
    void testSelectBlogArticleById_WithTags() {
        // 准备数据
        BlogTag tag = new BlogTag();
        tag.setId(1L);
        tag.setName("测试标签");
        List<BlogTag> tagList = Arrays.asList(tag);

        when(blogArticleMapper.selectBlogArticleById(1L)).thenReturn(testArticle);
        when(blogTagMapper.selectTagsByArticleId(1L)).thenReturn(tagList);

        // 执行测试
        BlogArticle result = blogArticleService.selectBlogArticleById(1L);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getTags());
        assertEquals(1, result.getTags().size());
        assertEquals("测试标签", result.getTags().get(0).getName());
        verify(blogArticleMapper).selectBlogArticleById(1L);
        verify(blogTagMapper).selectTagsByArticleId(1L);
    }

    /**
     * 测试查询文章列表 - 文章 ID 为 null
     */
    @Test
    void testSelectBlogArticleList_ArticleIdNull() {
        // 准备数据 - 文章 ID 为 null
        BlogArticle articleWithNullId = new BlogArticle();
        articleWithNullId.setTitle("测试文章");
        articleWithNullId.setId(null);

        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class)))
            .thenReturn(Arrays.asList(articleWithNullId));

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleList(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getId());
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
        verify(blogTagMapper, never()).selectTagsByArticleId(anyLong());
    }

    /**
     * 测试查询文章列表 - 标签为空列表
     */
    @Test
    void testSelectBlogArticleList_TagsNull() {
        // 准备数据
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setTitle("测试文章");

        List<Map<String, Object>> tagMappings = new ArrayList<>();

        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class)))
            .thenReturn(Arrays.asList(article));
        when(blogTagMapper.selectTagsByArticleIds(anyList())).thenReturn(tagMappings);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleList(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTags());
        assertEquals(0, result.get(0).getTags().size());
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
        verify(blogTagMapper).selectTagsByArticleIds(anyList());
    }

    /**
     * 测试查询文章列表 - 标签 ID 为 null
     */
    @Test
    void testSelectBlogArticleList_TagIdNull() {
        // 准备数据 - 标签 ID 为 null
        BlogArticle article = new BlogArticle();
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

        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class)))
            .thenReturn(Arrays.asList(article));
        when(blogTagMapper.selectTagsByArticleIds(anyList())).thenReturn(tagMappings);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleList(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTags());
        assertEquals(1, result.get(0).getTags().size());
        assertTrue(result.get(0).getTagIds().isEmpty());
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
        verify(blogTagMapper).selectTagsByArticleIds(anyList());
    }

    /**
     * 测试查询文章详情 - 标签为 null
     */
    @Test
    void testSelectBlogArticleById_TagsNull() {
        // 模拟返回文章，但标签为 null
        when(blogArticleMapper.selectBlogArticleById(1L)).thenReturn(testArticle);
        when(blogTagMapper.selectTagsByArticleId(1L)).thenReturn(null);

        // 执行测试
        BlogArticle result = blogArticleService.selectBlogArticleById(1L);

        // 验证结果
        assertNotNull(result);
        assertNull(result.getTags());
        verify(blogArticleMapper).selectBlogArticleById(1L);
        verify(blogTagMapper).selectTagsByArticleId(1L);
    }

    /**
     * 测试查询文章详情 - 标签为空列表
     */
    @Test
    void testSelectBlogArticleById_TagsEmpty() {
        // 模拟返回文章，但标签为空列表
        when(blogArticleMapper.selectBlogArticleById(1L)).thenReturn(testArticle);
        when(blogTagMapper.selectTagsByArticleId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        BlogArticle result = blogArticleService.selectBlogArticleById(1L);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
        verify(blogArticleMapper).selectBlogArticleById(1L);
        verify(blogTagMapper).selectTagsByArticleId(1L);
    }

    /**
     * 测试更新文章 - delFlag 为 null
     */
    @Test
    void testUpdateBlogArticle_DelFlagNull() {
        // 设置 delFlag 为 null
        testArticle.setDelFlag(null);
        when(blogArticleMapper.updateBlogArticle(any(BlogArticle.class))).thenReturn(1);

        // 执行测试
        int result = blogArticleService.updateBlogArticle(testArticle);

        // 验证结果
        assertEquals(1, result);
        verify(blogArticleMapper).updateBlogArticle(any(BlogArticle.class));
    }

    /**
     * 测试更新文章 - 标签ID列表为 null
     */
    @Test
    void testUpdateBlogArticle_TagIdsNull() {
        // 设置标签ID列表为 null
        testArticle.setTagIds(null);
        when(blogArticleMapper.updateBlogArticle(any(BlogArticle.class))).thenReturn(1);

        // 执行测试
        int result = blogArticleService.updateBlogArticle(testArticle);

        // 验证结果
        assertEquals(1, result);
        verify(blogArticleMapper).updateBlogArticle(any(BlogArticle.class));
        // 标签处理已移到 controller 层：tagIds 为 null 时不得删除原有关联
        verify(blogArticleTagMapper, never()).deleteByArticleId(anyLong());
        verify(blogArticleTagMapper, never()).insertBlogArticleTag(any());
    }

    /**
     * 测试更新文章 - 标签ID列表为空
     */
    @Test
    void testUpdateBlogArticle_TagIdsEmpty() {
        // 设置标签ID列表为空
        testArticle.setTagIds(Collections.emptyList());
        when(blogArticleMapper.updateBlogArticle(any(BlogArticle.class))).thenReturn(1);

        // 执行测试
        int result = blogArticleService.updateBlogArticle(testArticle);

        // 验证结果
        assertEquals(1, result);
        verify(blogArticleMapper).updateBlogArticle(any(BlogArticle.class));
        // 标签处理已移到 controller 层：tagIds 为空时不得删除原有关联
        verify(blogArticleTagMapper, never()).deleteByArticleId(anyLong());
        verify(blogArticleTagMapper, never()).insertBlogArticleTag(any());
    }

    /**
     * 测试更新文章 - 重复标题异常
     */
    @Test
    void testUpdateBlogArticle_DuplicateTitle() {
        // 模拟重复标题异常
        when(blogArticleMapper.updateBlogArticle(any(BlogArticle.class)))
            .thenThrow(new RuntimeException("Duplicate entry 'title' for key 'idx_article_title'"));

        // 执行测试
        DuplicateArticleTitleException exception = assertThrows(DuplicateArticleTitleException.class,
                () -> blogArticleService.updateBlogArticle(testArticle));

        assertTrue(exception.getMessage().contains("文章标题已存在"));
    }

    /**
     * 测试更新文章 - 异常消息为 null
     */
    @Test
    void testUpdateBlogArticle_ExceptionMessageNull() {
        // 模拟异常消息为 null
        when(blogArticleMapper.updateBlogArticle(any(BlogArticle.class)))
            .thenThrow(new NullPointerException());

        // 执行测试
        assertThrows(NullPointerException.class, 
                () -> blogArticleService.updateBlogArticle(testArticle));
    }

    /**
     * 测试根据标签ID查询文章 - 文章列表为空
     */
    @Test
    void testSelectArticlesByTagId_EmptyList() {
        // 模拟返回空列表
        when(blogArticleMapper.selectArticlesByTagId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(blogArticleMapper).selectArticlesByTagId(1L);
    }

    /**
     * 测试根据标签ID查询文章 - 文章列表为 null
     */
    @Test
    void testSelectArticlesByTagId_NullList() {
        // 模拟返回 null
        when(blogArticleMapper.selectArticlesByTagId(1L)).thenReturn(null);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNull(result);
        verify(blogArticleMapper).selectArticlesByTagId(1L);
    }

    /**
     * 测试根据标签ID查询文章 - 标签为空列表
     */
    @Test
    void testSelectArticlesByTagId_TagsEmpty() {
        // 模拟返回文章列表，但标签映射为空列表
        List<Map<String, Object>> tagMappings = new ArrayList<>();

        when(blogArticleMapper.selectArticlesByTagId(1L)).thenReturn(Arrays.asList(testArticle));
        when(blogTagMapper.selectTagsByArticleIds(anyList())).thenReturn(tagMappings);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectArticlesByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTags());
        assertTrue(result.get(0).getTags().isEmpty());
        verify(blogArticleMapper).selectArticlesByTagId(1L);
        verify(blogTagMapper).selectTagsByArticleIds(anyList());
    }

    /**
     * 测试查询文章列表 - 文章列表为空
     */
    @Test
    void testSelectBlogArticleList_EmptyList() {
        // 模拟返回空列表
        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class))).thenReturn(Collections.emptyList());

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleList(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
    }

    /**
     * 测试查询文章列表 - 文章列表为 null
     */
    @Test
    void testSelectBlogArticleList_NullList() {
        // 模拟返回 null
        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class))).thenReturn(null);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleList(new BlogArticle());

        // 验证结果
        assertNull(result);
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
    }

    /**
     * 测试查询文章列表 - 文章为 null
     */
    @Test
    void testSelectBlogArticleList_ArticleNull() {
        // 模拟返回包含 null 的列表
        BlogArticle nullArticle = null;
        when(blogArticleMapper.selectBlogArticleList(any(BlogArticle.class))).thenReturn(Arrays.asList(nullArticle));

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectBlogArticleList(new BlogArticle());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0));
        verify(blogArticleMapper).selectBlogArticleList(any(BlogArticle.class));
    }

    /**
     * 测试批量更新置顶状态 - 成功
     */
    @Test
    void testUpdateArticleTopStatus_Success() {
        // 准备数据
        when(blogArticleMapper.updateArticleTopStatus(anyList(), any())).thenReturn(2);

        // 执行测试
        int result = blogArticleService.updateArticleTopStatus(Arrays.asList(1L, 2L), 1);

        // 验证结果
        assertEquals(2, result);
        verify(blogArticleMapper).updateArticleTopStatus(Arrays.asList(1L, 2L), 1);
    }

    /**
     * 测试批量更新置顶状态 - 空列表
     */
    @Test
    void testUpdateArticleTopStatus_Empty() {
        // 执行测试
        int result = blogArticleService.updateArticleTopStatus(Collections.emptyList(), 1);

        // 验证结果
        assertEquals(0, result);
        verify(blogArticleMapper, never()).updateArticleTopStatus(anyList(), any());
    }

    /**
     * 测试批量更新置顶状态 - null 列表
     */
    @Test
    void testUpdateArticleTopStatus_Null() {
        // 执行测试
        int result = blogArticleService.updateArticleTopStatus(null, 1);

        // 验证结果
        assertEquals(0, result);
        verify(blogArticleMapper, never()).updateArticleTopStatus(anyList(), any());
    }

    /**
     * 测试批量更新推荐状态 - 成功
     */
    @Test
    void testUpdateArticleRecommendStatus_Success() {
        // 准备数据
        when(blogArticleMapper.updateArticleRecommendStatus(anyList(), any())).thenReturn(2);

        // 执行测试
        int result = blogArticleService.updateArticleRecommendStatus(Arrays.asList(1L, 2L), 1);

        // 验证结果
        assertEquals(2, result);
        verify(blogArticleMapper).updateArticleRecommendStatus(Arrays.asList(1L, 2L), 1);
    }

    /**
     * 测试批量更新推荐状态 - 空列表
     */
    @Test
    void testUpdateArticleRecommendStatus_Empty() {
        // 执行测试
        int result = blogArticleService.updateArticleRecommendStatus(Collections.emptyList(), 1);

        // 验证结果
        assertEquals(0, result);
        verify(blogArticleMapper, never()).updateArticleRecommendStatus(anyList(), any());
    }

    /**
     * 测试点赞文章 - 成功
     */
    @Test
    void testLikeArticle_Success() {
        // 准备数据
        when(blogArticleMapper.likeArticle(1L)).thenReturn(1);

        // 执行测试
        int result = blogArticleService.likeArticle(1L);

        // 验证结果
        assertEquals(1, result);
        verify(blogArticleMapper).likeArticle(1L);
    }

    /**
     * 测试点赞文章 - null ID
     */
    @Test
    void testLikeArticle_Null() {
        // 执行测试
        int result = blogArticleService.likeArticle(null);

        // 验证结果
        assertEquals(0, result);
        verify(blogArticleMapper, never()).likeArticle(anyLong());
    }

    /**
     * 测试查询相关文章 - 有同分类文章
     */
    @Test
    void testSelectRelatedArticles_Success() {
        // 准备数据
        BlogArticle related = new BlogArticle();
        related.setId(2L);
        related.setTitle("相关文章");
        when(blogArticleMapper.selectRelatedArticles(1L, 6)).thenReturn(Collections.singletonList(related));

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectRelatedArticles(1L);

        // 验证结果
        assertEquals(1, result.size());
        verify(blogArticleMapper).selectRelatedArticles(1L, 6);
        verify(blogArticleMapper, never()).selectHotArticles(any(BlogArticle.class));
    }

    /**
     * 测试查询相关文章 - 无同分类文章时回退热门文章
     */
    @Test
    void testSelectRelatedArticles_FallbackToHot() {
        // 准备数据
        when(blogArticleMapper.selectRelatedArticles(1L, 6)).thenReturn(Collections.emptyList());
        List<BlogArticle> hot = new ArrayList<>();
        for (long i = 0; i < 3; i++) {
            BlogArticle article = new BlogArticle();
            article.setId(i);
            hot.add(article);
        }
        when(blogArticleMapper.selectHotArticles(any(BlogArticle.class))).thenReturn(hot);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectRelatedArticles(1L);

        // 验证结果
        assertEquals(3, result.size());
        verify(blogArticleMapper).selectHotArticles(any(BlogArticle.class));
    }

    /**
     * 测试查询相关文章 - 热门文章超过6篇时截断
     */
    @Test
    void testSelectRelatedArticles_FallbackToHot_Truncated() {
        // 准备数据
        when(blogArticleMapper.selectRelatedArticles(1L, 6)).thenReturn(null);
        List<BlogArticle> hot = new ArrayList<>();
        for (long i = 0; i < 8; i++) {
            BlogArticle article = new BlogArticle();
            article.setId(i);
            hot.add(article);
        }
        when(blogArticleMapper.selectHotArticles(any(BlogArticle.class))).thenReturn(hot);

        // 执行测试
        List<BlogArticle> result = blogArticleService.selectRelatedArticles(1L);

        // 验证结果
        assertEquals(6, result.size());
        verify(blogArticleMapper).selectHotArticles(any(BlogArticle.class));
    }
}
