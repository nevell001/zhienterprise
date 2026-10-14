package com.zhi.system.service.impl;

import com.zhi.system.domain.ArticleTag;
import com.zhi.system.mapper.ArticleTagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 文章标签关联服务层单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class ArticleTagServiceImplTest {

    @Mock
    private ArticleTagMapper articleTagMapper;

    @InjectMocks
    private ArticleTagServiceImpl articleTagService;

    private ArticleTag testArticleTag;

    @BeforeEach
    void setUp() {
        testArticleTag = new ArticleTag();
        testArticleTag.setId(1L);
        testArticleTag.setArticleId(1L);
        testArticleTag.setId(1L);
    }

    /**
     * 测试查询文章标签关联列表
     */
    @Test
    void testSelectArticleTagList() {
        // 准备数据
        List<ArticleTag> articleTagList = Arrays.asList(testArticleTag);
        when(articleTagMapper.selectArticleTagList(any(ArticleTag.class)))
            .thenReturn(articleTagList);

        // 执行测试
        List<ArticleTag> result = articleTagService.selectArticleTagList(new ArticleTag());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getArticleId());
        verify(articleTagMapper).selectArticleTagList(any(ArticleTag.class));
    }

    /**
     * 测试根据ID查询文章标签关联
     */
    @Test
    void testSelectArticleTagById() {
        // 准备数据
        when(articleTagMapper.selectArticleTagById(1L)).thenReturn(testArticleTag);

        // 执行测试
        ArticleTag result = articleTagService.selectArticleTagById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getArticleId());
        verify(articleTagMapper).selectArticleTagById(1L);
    }

    /**
     * 测试新增文章标签关联
     */
    @Test
    void testInsertArticleTag() {
        // 准备数据
        when(articleTagMapper.insertArticleTag(any(ArticleTag.class))).thenReturn(1);

        // 执行测试
        int result = articleTagService.insertArticleTag(testArticleTag);

        // 验证结果
        assertEquals(1, result);
        verify(articleTagMapper).insertArticleTag(any(ArticleTag.class));
    }

    /**
     * 测试更新文章标签关联
     */
    @Test
    void testUpdateArticleTag() {
        // 准备数据
        when(articleTagMapper.updateArticleTag(any(ArticleTag.class))).thenReturn(1);

        // 执行测试
        int result = articleTagService.updateArticleTag(testArticleTag);

        // 验证结果
        assertEquals(1, result);
        verify(articleTagMapper).updateArticleTag(any(ArticleTag.class));
    }

    /**
     * 测试删除文章标签关联
     */
    @Test
    void testDeleteArticleTagById() {
        // 准备数据
        when(articleTagMapper.deleteArticleTagById(1L)).thenReturn(1);

        // 执行测试
        int result = articleTagService.deleteArticleTagById(1L);

        // 验证结果
        assertEquals(1, result);
        verify(articleTagMapper).deleteArticleTagById(1L);
    }

    /**
     * 测试批量删除文章标签关联
     */
    @Test
    void testDeleteArticleTagByIds() {
        // 准备数据
        when(articleTagMapper.deleteArticleTagByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        int result = articleTagService.deleteArticleTagByIds(new Long[]{1L, 2L});

        // 验证结果
        assertEquals(1, result);
        verify(articleTagMapper).deleteArticleTagByIds(any(Long[].class));
    }

    /**
     * 测试通过文章ID查询标签ID列表
     */
    @Test
    void testSelectTagIdsByArticleId() {
        // 准备数据
        List<Long> tagIds = Arrays.asList(1L, 2L, 3L);
        when(articleTagMapper.selectTagIdsByArticleId(1L)).thenReturn(tagIds);

        // 执行测试
        List<Long> result = articleTagService.selectTagIdsByArticleId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
        verify(articleTagMapper).selectTagIdsByArticleId(1L);
    }

    /**
     * 测试通过文章ID查询标签ID列表 - 空结果
     */
    @Test
    void testSelectTagIdsByArticleId_Empty() {
        // 模拟空结果
        when(articleTagMapper.selectTagIdsByArticleId(1L)).thenReturn(java.util.Collections.emptyList());

        // 执行测试
        List<Long> result = articleTagService.selectTagIdsByArticleId(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleTagMapper).selectTagIdsByArticleId(1L);
    }

    /**
     * 测试通过标签ID查询文章ID列表
     */
    @Test
    void testSelectArticleIdsByTagId() {
        // 准备数据
        List<Long> articleIds = Arrays.asList(1L, 2L, 3L);
        when(articleTagMapper.selectArticleIdsByTagId(1L)).thenReturn(articleIds);

        // 执行测试
        List<Long> result = articleTagService.selectArticleIdsByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
        verify(articleTagMapper).selectArticleIdsByTagId(1L);
    }

    /**
     * 测试通过标签ID查询文章ID列表 - 空结果
     */
    @Test
    void testSelectArticleIdsByTagId_Empty() {
        // 模拟空结果
        when(articleTagMapper.selectArticleIdsByTagId(1L)).thenReturn(java.util.Collections.emptyList());

        // 执行测试
        List<Long> result = articleTagService.selectArticleIdsByTagId(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleTagMapper).selectArticleIdsByTagId(1L);
    }

    /**
     * 测试批量新增文章标签关联
     */
    @Test
    void testBatchInsertArticleTag() {
        // 准备数据
        ArticleTag articleTag1 = new ArticleTag();
        articleTag1.setArticleId(1L);
        articleTag1.setTagId(1L);

        ArticleTag articleTag2 = new ArticleTag();
        articleTag2.setArticleId(1L);
        articleTag2.setTagId(2L);

        List<ArticleTag> articleTagList = Arrays.asList(articleTag1, articleTag2);
        when(articleTagMapper.batchInsertArticleTag(any(List.class))).thenReturn(2);

        // 执行测试
        int result = articleTagService.batchInsertArticleTag(articleTagList);

        // 验证结果
        assertEquals(2, result);
        verify(articleTagMapper).batchInsertArticleTag(any(List.class));
    }

    /**
     * 测试批量新增文章标签关联 - 空列表
     */
    @Test
    void testBatchInsertArticleTag_Empty() {
        // 准备数据
        when(articleTagMapper.batchInsertArticleTag(any(List.class))).thenReturn(0);

        // 执行测试
        int result = articleTagService.batchInsertArticleTag(java.util.Collections.emptyList());

        // 验证结果
        assertEquals(0, result);
        verify(articleTagMapper).batchInsertArticleTag(any(List.class));
    }

    /**
     * 测试通过文章ID删除文章和标签关联
     */
    @Test
    void testDeleteByArticleId() {
        // 准备数据
        when(articleTagMapper.deleteByArticleId(1L)).thenReturn(2);

        // 执行测试
        int result = articleTagService.deleteByArticleId(1L);

        // 验证结果
        assertEquals(2, result);
        verify(articleTagMapper).deleteByArticleId(1L);
    }

    /**
     * 测试通过文章ID删除文章和标签关联 - 不存在
     */
    @Test
    void testDeleteByArticleId_NotFound() {
        // 准备数据
        when(articleTagMapper.deleteByArticleId(999L)).thenReturn(0);

        // 执行测试
        int result = articleTagService.deleteByArticleId(999L);

        // 验证结果
        assertEquals(0, result);
        verify(articleTagMapper).deleteByArticleId(999L);
    }

    /**
     * 测试查询文章标签关联列表 - 空结果
     */
    @Test
    void testSelectArticleTagList_Empty() {
        // 模拟空结果
        when(articleTagMapper.selectArticleTagList(any(ArticleTag.class)))
            .thenReturn(java.util.Collections.emptyList());

        // 执行测试
        List<ArticleTag> result = articleTagService.selectArticleTagList(new ArticleTag());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(articleTagMapper).selectArticleTagList(any(ArticleTag.class));
    }

    /**
     * 测试根据ID查询文章标签关联 - 不存在
     */
    @Test
    void testSelectArticleTagById_NotFound() {
        // 模拟数据不存在
        when(articleTagMapper.selectArticleTagById(999L)).thenReturn(null);

        // 执行测试
        ArticleTag result = articleTagService.selectArticleTagById(999L);

        // 验证结果
        assertNull(result);
        verify(articleTagMapper).selectArticleTagById(999L);
    }

    /**
     * 测试新增文章标签关联 - 失败
     */
    @Test
    void testInsertArticleTag_Failure() {
        // 准备数据
        when(articleTagMapper.insertArticleTag(any(ArticleTag.class))).thenReturn(0);

        // 执行测试
        int result = articleTagService.insertArticleTag(testArticleTag);

        // 验证结果
        assertEquals(0, result);
        verify(articleTagMapper).insertArticleTag(any(ArticleTag.class));
    }

    /**
     * 测试更新文章标签关联 - 失败
     */
    @Test
    void testUpdateArticleTag_Failure() {
        // 准备数据
        when(articleTagMapper.updateArticleTag(any(ArticleTag.class))).thenReturn(0);

        // 执行测试
        int result = articleTagService.updateArticleTag(testArticleTag);

        // 验证结果
        assertEquals(0, result);
        verify(articleTagMapper).updateArticleTag(any(ArticleTag.class));
    }

    /**
     * 测试删除文章标签关联 - 不存在
     */
    @Test
    void testDeleteArticleTagById_NotFound() {
        // 准备数据
        when(articleTagMapper.deleteArticleTagById(999L)).thenReturn(0);

        // 执行测试
        int result = articleTagService.deleteArticleTagById(999L);

        // 验证结果
        assertEquals(0, result);
        verify(articleTagMapper).deleteArticleTagById(999L);
    }

    /**
     * 测试批量删除文章标签关联 - 空列表
     */
    @Test
    void testDeleteArticleTagByIds_Empty() {
        // 准备数据
        when(articleTagMapper.deleteArticleTagByIds(any(Long[].class))).thenReturn(0);

        // 执行测试
        int result = articleTagService.deleteArticleTagByIds(new Long[]{});

        // 验证结果
        assertEquals(0, result);
        verify(articleTagMapper).deleteArticleTagByIds(any(Long[].class));
    }
}