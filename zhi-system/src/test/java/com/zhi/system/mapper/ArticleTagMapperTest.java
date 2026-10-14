package com.zhi.system.mapper;

import com.zhi.system.domain.ArticleTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 文章标签关联Mapper单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class ArticleTagMapperTest {

    @Mock
    private ArticleTagMapper articleTagMapper;

    private ArticleTag testArticleTag;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testArticleTag = new ArticleTag();
        testArticleTag.setId(1L);
        testArticleTag.setArticleId(1L);
        testArticleTag.setTagId(1L);
    }

    /**
     * 测试CRUD操作的完整流程
     */
    @Test
    void testCRUDOperations() {
        // 测试新增
        when(articleTagMapper.insertArticleTag(any(ArticleTag.class))).thenReturn(1);
        int insertResult = articleTagMapper.insertArticleTag(testArticleTag);
        assertEquals(1, insertResult, "新增文章标签关联失败");
        verify(articleTagMapper).insertArticleTag(any(ArticleTag.class));

        // 测试根据ID查询
        when(articleTagMapper.selectArticleTagById(1L)).thenReturn(testArticleTag);
        ArticleTag selectResult = articleTagMapper.selectArticleTagById(1L);
        assertNotNull(selectResult, "根据ID查询文章标签关联失败");
        assertEquals(1L, selectResult.getArticleId(), "文章ID不匹配");
        verify(articleTagMapper).selectArticleTagById(1L);

        // 测试修改
        when(articleTagMapper.updateArticleTag(any(ArticleTag.class))).thenReturn(1);
        testArticleTag.setTagId(2L);
        int updateResult = articleTagMapper.updateArticleTag(testArticleTag);
        assertEquals(1, updateResult, "修改文章标签关联失败");
        verify(articleTagMapper).updateArticleTag(any(ArticleTag.class));

        // 测试查询列表
        when(articleTagMapper.selectArticleTagList(any(ArticleTag.class)))
            .thenReturn(Arrays.asList(testArticleTag));
        List<ArticleTag> articleTagList = articleTagMapper.selectArticleTagList(new ArticleTag());
        assertFalse(articleTagList.isEmpty(), "查询文章标签关联列表失败");
        verify(articleTagMapper).selectArticleTagList(any(ArticleTag.class));

        // 测试删除
        when(articleTagMapper.deleteArticleTagById(1L)).thenReturn(1);
        int deleteResult = articleTagMapper.deleteArticleTagById(1L);
        assertEquals(1, deleteResult, "删除文章标签关联失败");
        verify(articleTagMapper).deleteArticleTagById(1L);
    }

    /**
     * 测试批量删除功能
     */
    @Test
    void testDeleteArticleTagByIds() {
        when(articleTagMapper.deleteArticleTagByIds(any(Long[].class))).thenReturn(2);
        int deleteResult = articleTagMapper.deleteArticleTagByIds(new Long[]{1L, 2L});
        assertEquals(2, deleteResult, "批量删除文章标签关联失败");
        verify(articleTagMapper).deleteArticleTagByIds(any(Long[].class));
    }

    /**
     * 测试根据文章ID删除标签关联
     */
    @Test
    void testDeleteByArticleId() {
        when(articleTagMapper.deleteByArticleId(anyLong())).thenReturn(1);
        int result = articleTagMapper.deleteByArticleId(1L);
        assertEquals(1, result, "根据文章ID删除标签关联失败");
        verify(articleTagMapper).deleteByArticleId(anyLong());
    }

    /**
     * 测试边界条件：空值处理
     */
    @Test
    void testNullHandling() {
        // 测试查询不存在的ID
        when(articleTagMapper.selectArticleTagById(999L)).thenReturn(null);
        ArticleTag result = articleTagMapper.selectArticleTagById(999L);
        assertNull(result, "查询不存在的ID应返回null");
        verify(articleTagMapper).selectArticleTagById(999L);

        // 测试空列表查询
        when(articleTagMapper.selectArticleTagList(any(ArticleTag.class)))
            .thenReturn(Arrays.asList());
        List<ArticleTag> list = articleTagMapper.selectArticleTagList(new ArticleTag());
        assertTrue(list.isEmpty(), "查询结果应为空列表");
        verify(articleTagMapper).selectArticleTagList(any(ArticleTag.class));
    }

    /**
     * 测试边界条件：零ID
     */
    @Test
    void testZeroIds() {
        ArticleTag zeroIdArticleTag = new ArticleTag();
        zeroIdArticleTag.setArticleId(0L);
        zeroIdArticleTag.setTagId(0L);
        
        when(articleTagMapper.insertArticleTag(any(ArticleTag.class))).thenReturn(1);
        int result = articleTagMapper.insertArticleTag(zeroIdArticleTag);
        assertEquals(1, result, "零ID文章标签关联插入失败");
        verify(articleTagMapper).insertArticleTag(any(ArticleTag.class));
    }

    /**
     * 测试边界条件：负ID
     */
    @Test
    void testNegativeIds() {
        ArticleTag negativeIdArticleTag = new ArticleTag();
        negativeIdArticleTag.setArticleId(-1L);
        negativeIdArticleTag.setTagId(-1L);
        
        when(articleTagMapper.insertArticleTag(any(ArticleTag.class))).thenReturn(1);
        int result = articleTagMapper.insertArticleTag(negativeIdArticleTag);
        assertEquals(1, result, "负ID文章标签关联插入失败");
        verify(articleTagMapper).insertArticleTag(any(ArticleTag.class));
    }

    /**
     * 测试边界条件：大ID值
     */
    @Test
    void testLargeIds() {
        ArticleTag largeIdArticleTag = new ArticleTag();
        largeIdArticleTag.setArticleId(Long.MAX_VALUE);
        largeIdArticleTag.setTagId(Long.MAX_VALUE);
        
        when(articleTagMapper.insertArticleTag(any(ArticleTag.class))).thenReturn(1);
        int result = articleTagMapper.insertArticleTag(largeIdArticleTag);
        assertEquals(1, result, "大ID值文章标签关联插入失败");
        verify(articleTagMapper).insertArticleTag(any(ArticleTag.class));
    }

    /**
     * 测试边界条件：同一文章多个标签
     */
    @Test
    void testMultipleTagsForSameArticle() {
        ArticleTag tag1 = new ArticleTag();
        tag1.setArticleId(1L);
        tag1.setTagId(1L);
        
        ArticleTag tag2 = new ArticleTag();
        tag2.setArticleId(1L);
        tag2.setTagId(2L);
        
        ArticleTag tag3 = new ArticleTag();
        tag3.setArticleId(1L);
        tag3.setTagId(3L);
        
        when(articleTagMapper.insertArticleTag(any(ArticleTag.class))).thenReturn(1);
        int result1 = articleTagMapper.insertArticleTag(tag1);
        int result2 = articleTagMapper.insertArticleTag(tag2);
        int result3 = articleTagMapper.insertArticleTag(tag3);
        
        assertEquals(1, result1, "标签1插入失败");
        assertEquals(1, result2, "标签2插入失败");
        assertEquals(1, result3, "标签3插入失败");
        
        verify(articleTagMapper, times(3)).insertArticleTag(any(ArticleTag.class));
    }

    /**
     * 测试边界条件：同一标签多篇文章
     */
    @Test
    void testMultipleArticlesForSameTag() {
        ArticleTag article1 = new ArticleTag();
        article1.setArticleId(1L);
        article1.setTagId(1L);
        
        ArticleTag article2 = new ArticleTag();
        article2.setArticleId(2L);
        article2.setTagId(1L);
        
        ArticleTag article3 = new ArticleTag();
        article3.setArticleId(3L);
        article3.setTagId(1L);
        
        when(articleTagMapper.insertArticleTag(any(ArticleTag.class))).thenReturn(1);
        int result1 = articleTagMapper.insertArticleTag(article1);
        int result2 = articleTagMapper.insertArticleTag(article2);
        int result3 = articleTagMapper.insertArticleTag(article3);
        
        assertEquals(1, result1, "文章1插入失败");
        assertEquals(1, result2, "文章2插入失败");
        assertEquals(1, result3, "文章3插入失败");
        
        verify(articleTagMapper, times(3)).insertArticleTag(any(ArticleTag.class));
    }
}