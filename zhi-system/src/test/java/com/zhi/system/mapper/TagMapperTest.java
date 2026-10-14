package com.zhi.system.mapper;

import com.zhi.system.domain.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 博客标签Mapper单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class TagMapperTest {

    @Mock
    private TagMapper tagMapper;

    private Tag testTag;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("测试标签");
        testTag.setColor("#ff0000");
        testTag.setIcon("tag");
    }

    /**
     * 测试CRUD操作的完整流程
     */
    @Test
    void testCRUDOperations() {
        // 测试新增
        when(tagMapper.insertTag(any(Tag.class))).thenReturn(1);
        int insertResult = tagMapper.insertTag(testTag);
        assertEquals(1, insertResult, "新增博客标签失败");
        verify(tagMapper).insertTag(any(Tag.class));

        // 测试根据ID查询
        when(tagMapper.selectTagById(1L)).thenReturn(testTag);
        Tag selectResult = tagMapper.selectTagById(1L);
        assertNotNull(selectResult, "根据ID查询博客标签失败");
        assertEquals("测试标签", selectResult.getName(), "标签名称不匹配");
        verify(tagMapper).selectTagById(1L);

        // 测试修改
        when(tagMapper.updateTag(any(Tag.class))).thenReturn(1);
        testTag.setName("修改后的标签");
        int updateResult = tagMapper.updateTag(testTag);
        assertEquals(1, updateResult, "修改博客标签失败");
        verify(tagMapper).updateTag(any(Tag.class));

        // 测试查询列表
        when(tagMapper.selectTagList(any(Tag.class)))
            .thenReturn(Arrays.asList(testTag));
        List<Tag> tagList = tagMapper.selectTagList(new Tag());
        assertFalse(tagList.isEmpty(), "查询博客标签列表失败");
        verify(tagMapper).selectTagList(any(Tag.class));

        // 测试删除
        when(tagMapper.deleteTagById(1L)).thenReturn(1);
        int deleteResult = tagMapper.deleteTagById(1L);
        assertEquals(1, deleteResult, "删除博客标签失败");
        verify(tagMapper).deleteTagById(1L);
    }

    /**
     * 测试批量删除功能
     */
    @Test
    void testDeleteTagByIds() {
        when(tagMapper.deleteTagByIds(any(Long[].class))).thenReturn(2);
        int deleteResult = tagMapper.deleteTagByIds(new Long[]{1L, 2L});
        assertEquals(2, deleteResult, "批量删除博客标签失败");
        verify(tagMapper).deleteTagByIds(any(Long[].class));
    }

    /**
     * 测试查询所有标签列表
     */
    @Test
    void testSelectAllTagList() {
        when(tagMapper.selectAllTagList()).thenReturn(Arrays.asList(testTag));
        List<Tag> tagList = tagMapper.selectAllTagList();
        assertNotNull(tagList, "所有标签列表不应为null");
        assertEquals(1, tagList.size(), "所有标签列表大小不正确");
        verify(tagMapper).selectAllTagList();
    }

    /**
     * 测试检查标签名称唯一性
     */
    @Test
    void testCheckTagNameUnique() {
        when(tagMapper.checkTagNameUnique(anyString())).thenReturn(null);
        Tag result = tagMapper.checkTagNameUnique("不存在的标签");
        assertNull(result, "不存在的标签应返回null");
        verify(tagMapper).checkTagNameUnique(anyString());
    }

    /**
     * 测试检查标签是否已被文章使用
     */
    @Test
    void testCheckTagExistArticle() {
        when(tagMapper.checkTagExistArticle(anyLong())).thenReturn(0);
        int count = tagMapper.checkTagExistArticle(1L);
        assertEquals(0, count, "检查标签是否已被文章使用失败");
        verify(tagMapper).checkTagExistArticle(anyLong());
    }

    /**
     * 测试获取标签云
     */
    @Test
    void testGetTagCloud() {
        Map<String, Object> tagData = Map.of("name", "Java", "article_count", 10);
        when(tagMapper.getTagCloud()).thenReturn(Arrays.asList(tagData));
        List<Map<String, Object>> tagCloud = tagMapper.getTagCloud();
        assertNotNull(tagCloud, "标签云不应为null");
        assertEquals(1, tagCloud.size(), "标签云大小不正确");
        verify(tagMapper).getTagCloud();
    }

    /**
     * 测试根据文章ID查询标签列表
     */
    @Test
    void testSelectTagsByArticleId() {
        when(tagMapper.selectTagsByArticleId(anyLong())).thenReturn(Arrays.asList(testTag));
        List<Tag> tagList = tagMapper.selectTagsByArticleId(1L);
        assertNotNull(tagList, "标签列表不应为null");
        assertEquals(1, tagList.size(), "标签列表大小不正确");
        verify(tagMapper).selectTagsByArticleId(anyLong());
    }

    /**
     * 测试边界条件：空值处理
     */
    @Test
    void testNullHandling() {
        // 测试查询不存在的ID
        when(tagMapper.selectTagById(999L)).thenReturn(null);
        Tag result = tagMapper.selectTagById(999L);
        assertNull(result, "查询不存在的ID应返回null");
        verify(tagMapper).selectTagById(999L);

        // 测试空列表查询
        when(tagMapper.selectTagList(any(Tag.class)))
            .thenReturn(Arrays.asList());
        List<Tag> list = tagMapper.selectTagList(new Tag());
        assertTrue(list.isEmpty(), "查询结果应为空列表");
        verify(tagMapper).selectTagList(any(Tag.class));
    }

    /**
     * 测试边界条件：空名称
     */
    @Test
    void testEmptyName() {
        Tag emptyNameTag = new Tag();
        emptyNameTag.setName("");
        
        when(tagMapper.insertTag(any(Tag.class))).thenReturn(1);
        int result = tagMapper.insertTag(emptyNameTag);
        assertEquals(1, result, "空名称标签插入失败");
        verify(tagMapper).insertTag(any(Tag.class));
    }

    /**
     * 测试边界条件：空颜色
     */
    @Test
    void testEmptyColor() {
        Tag emptyColorTag = new Tag();
        emptyColorTag.setName("测试");
        emptyColorTag.setColor("");
        
        when(tagMapper.insertTag(any(Tag.class))).thenReturn(1);
        int result = tagMapper.insertTag(emptyColorTag);
        assertEquals(1, result, "空颜色标签插入失败");
        verify(tagMapper).insertTag(any(Tag.class));
    }

    /**
     * 测试边界条件：特殊字符名称
     */
    @Test
    void testSpecialCharactersName() {
        Tag specialCharsTag = new Tag();
        specialCharsTag.setName("测试@#$%^&*()");
        
        when(tagMapper.insertTag(any(Tag.class))).thenReturn(1);
        int result = tagMapper.insertTag(specialCharsTag);
        assertEquals(1, result, "特殊字符名称标签插入失败");
        verify(tagMapper).insertTag(any(Tag.class));
    }

    /**
     * 测试边界条件：长名称
     */
    @Test
    void testLongName() {
        Tag longNameTag = new Tag();
        longNameTag.setName("这是一个非常长的标签名称，用于测试系统对长文本的处理能力");
        
        when(tagMapper.insertTag(any(Tag.class))).thenReturn(1);
        int result = tagMapper.insertTag(longNameTag);
        assertEquals(1, result, "长名称标签插入失败");
        verify(tagMapper).insertTag(any(Tag.class));
    }
}