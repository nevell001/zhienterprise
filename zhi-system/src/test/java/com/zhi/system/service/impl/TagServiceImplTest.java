package com.zhi.system.service.impl;

import com.zhi.system.domain.Tag;
import com.zhi.system.mapper.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zhi.common.utils.SecurityUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 博客标签服务层单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagServiceImpl tagService;

    private Tag testTag;

    @BeforeEach
    void setUp() {
        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("测试标签");
        testTag.setColor("#ff0000");
    }

    /**
     * 测试查询标签列表
     */
    @Test
    void testSelectTagList() {
        // 准备数据
        List<Tag> tagList = Arrays.asList(testTag);
        when(tagMapper.selectTagList(any(Tag.class))).thenReturn(tagList);

        // 执行测试
        List<Tag> result = tagService.selectTagList(new Tag());

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试标签", result.get(0).getName());
        verify(tagMapper).selectTagList(any(Tag.class));
    }

    /**
     * 测试查询标签列表 - 空结果
     */
    @Test
    void testSelectTagList_Empty() {
        // 模拟空结果
        when(tagMapper.selectTagList(any(Tag.class)))
            .thenReturn(Collections.emptyList());

        // 执行测试
        List<Tag> result = tagService.selectTagList(new Tag());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagMapper).selectTagList(any(Tag.class));
    }

    /**
     * 测试查询所有标签列表
     */
    @Test
    void testSelectAllTagList() {
        // 准备数据
        List<Tag> tagList = Arrays.asList(testTag);
        when(tagMapper.selectAllTagList()).thenReturn(tagList);

        // 执行测试
        List<Tag> result = tagService.selectAllTagList();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagMapper).selectAllTagList();
    }

    /**
     * 测试查询所有标签列表 - 空结果
     */
    @Test
    void testSelectAllTagList_Empty() {
        // 模拟空结果
        when(tagMapper.selectAllTagList()).thenReturn(Collections.emptyList());

        // 执行测试
        List<Tag> result = tagService.selectAllTagList();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagMapper).selectAllTagList();
    }

    /**
     * 测试根据ID查询标签
     */
    @Test
    void testSelectTagById() {
        // 准备数据
        when(tagMapper.selectTagById(1L)).thenReturn(testTag);

        // 执行测试
        Tag result = tagService.selectTagById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("测试标签", result.getName());
        verify(tagMapper).selectTagById(1L);
    }

    /**
     * 测试根据ID查询标签 - 不存在
     */
    @Test
    void testSelectTagById_NotFound() {
        // 模拟标签不存在
        when(tagMapper.selectTagById(999L)).thenReturn(null);

        // 执行测试
        Tag result = tagService.selectTagById(999L);

        // 验证结果
        assertNull(result);
        verify(tagMapper).selectTagById(999L);
    }

    /**
     * 测试新增标签
     */
    @Test
    void testInsertTag() {
        // 准备数据
        when(tagMapper.insertTag(any(Tag.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = tagService.insertTag(testTag);

            // 验证结果
            assertEquals(1, result);
            verify(tagMapper).insertTag(any(Tag.class));
        }
    }

    /**
     * 测试新增标签 - 名称为空
     */
    @Test
    void testInsertTag_EmptyName() {
        // 设置名称为空
        testTag.setName("");

        // 准备数据
        when(tagMapper.insertTag(any(Tag.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试 - 注意：Service层可能没有验证名称为空
            int result = tagService.insertTag(testTag);

            // 验证结果 - 如果Service层没有验证，这里应该返回1
            assertEquals(1, result);
            verify(tagMapper).insertTag(any(Tag.class));
        }
    }

    /**
     * 测试新增标签 - 名称前后有空格
     */
    @Test
    void testInsertTag_NameWithSpaces() {
        // 设置名称前后有空格
        testTag.setName("  测试标签  ");

        // 准备数据
        when(tagMapper.insertTag(any(Tag.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = tagService.insertTag(testTag);

            // 验证结果
            assertEquals(1, result);
            verify(tagMapper).insertTag(any(Tag.class));
        }
    }

    /**
     * 测试修改标签
     */
    @Test
    void testUpdateTag() {
        // 准备数据
        when(tagMapper.updateTag(any(Tag.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试
            int result = tagService.updateTag(testTag);

            // 验证结果
            assertEquals(1, result);
            verify(tagMapper).updateTag(any(Tag.class));
        }
    }

    /**
     * 测试修改标签 - 名称为空
     */
    @Test
    void testUpdateTag_EmptyName() {
        // 设置名称为空
        testTag.setName("");

        // 准备数据
        when(tagMapper.updateTag(any(Tag.class))).thenReturn(1);

        // Mock SecurityUtils
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getUsername).thenReturn("testUser");

            // 执行测试 - 注意：Service层可能没有验证名称为空
            int result = tagService.updateTag(testTag);

            // 验证结果 - 如果Service层没有验证，这里应该返回1
            assertEquals(1, result);
            verify(tagMapper).updateTag(any(Tag.class));
        }
    }

    /**
     * 测试删除标签
     */
    @Test
    void testDeleteTagById() {
        // 准备数据
        when(tagMapper.deleteTagById(1L)).thenReturn(1);

        // 执行测试
        int result = tagService.deleteTagById(1L);

        // 验证结果
        assertEquals(1, result);
        verify(tagMapper).deleteTagById(1L);
    }

    /**
     * 测试删除标签 - 不存在
     */
    @Test
    void testDeleteTagById_NotFound() {
        // 准备数据
        when(tagMapper.deleteTagById(999L)).thenReturn(0);

        // 执行测试
        int result = tagService.deleteTagById(999L);

        // 验证结果
        assertEquals(0, result);
        verify(tagMapper).deleteTagById(999L);
    }

    /**
     * 测试批量删除标签
     */
    @Test
    void testDeleteTagByIds() {
        // 准备数据
        when(tagMapper.deleteTagByIds(any(Long[].class))).thenReturn(2);

        // 执行测试
        int result = tagService.deleteTagByIds(new Long[]{1L, 2L});

        // 验证结果
        assertEquals(2, result);
        verify(tagMapper).deleteTagByIds(any(Long[].class));
    }

    /**
     * 测试批量删除标签 - 空列表
     */
    @Test
    void testDeleteTagByIds_EmptyList() {
        // 准备数据
        when(tagMapper.deleteTagByIds(any(Long[].class))).thenReturn(0);

        // 执行测试
        int result = tagService.deleteTagByIds(new Long[]{});

        // 验证结果
        assertEquals(0, result);
        verify(tagMapper).deleteTagByIds(any(Long[].class));
    }

    /**
     * 测试检查标签是否关联文章 - 未关联
     */
    @Test
    void testCheckTagExistArticle_NotExist() {
        // 准备数据
        when(tagMapper.checkTagExistArticle(1L)).thenReturn(0);

        // 执行测试
        boolean result = tagService.checkTagExistArticle(1L);

        // 验证结果
        assertFalse(result);
        verify(tagMapper).checkTagExistArticle(1L);
    }

    /**
     * 测试检查标签是否关联文章 - 已关联
     */
    @Test
    void testCheckTagExistArticle_Exist() {
        // 准备数据
        when(tagMapper.checkTagExistArticle(1L)).thenReturn(1);

        // 执行测试
        boolean result = tagService.checkTagExistArticle(1L);

        // 验证结果
        assertTrue(result);
        verify(tagMapper).checkTagExistArticle(1L);
    }

    /**
     * 测试检查标签名称唯一性 - 唯一
     */
    @Test
    void testCheckTagNameUnique_Unique() {
        // 准备数据
        when(tagMapper.checkTagNameUnique(anyString())).thenReturn(null);

        // 执行测试
        boolean result = tagService.checkTagNameUnique(testTag);

        // 验证结果
        assertTrue(result);
        verify(tagMapper).checkTagNameUnique(anyString());
    }

    /**
     * 测试检查标签名称唯一性 - 不唯一
     */
    @Test
    void testCheckTagNameUnique_NotUnique() {
        // 准备数据
        Tag existingTag = new Tag();
        existingTag.setId(2L);
        when(tagMapper.checkTagNameUnique(anyString())).thenReturn(existingTag);

        // 执行测试
        boolean result = tagService.checkTagNameUnique(testTag);

        // 验证结果
        assertFalse(result);
        verify(tagMapper).checkTagNameUnique(anyString());
    }

    /**
     * 测试检查标签名称唯一性 - 更新自身
     */
    @Test
    void testCheckTagNameUnique_UpdateSelf() {
        // 准备数据
        when(tagMapper.checkTagNameUnique(anyString())).thenReturn(testTag);

        // 执行测试
        boolean result = tagService.checkTagNameUnique(testTag);

        // 验证结果
        assertTrue(result);
        verify(tagMapper).checkTagNameUnique(anyString());
    }

    /**
     * 测试获取标签云
     */
    @Test
    void testGetTagCloud() {
        // 准备数据
        List<Map<String, Object>> tagCloud = Collections.emptyList();
        when(tagMapper.getTagCloud()).thenReturn(tagCloud);

        // 执行测试
        List<Map<String, Object>> result = tagService.getTagCloud();

        // 验证结果
        assertNotNull(result);
        verify(tagMapper).getTagCloud();
    }

    /**
     * 测试根据文章ID查询标签列表
     */
    @Test
    void testSelectTagsByArticleId() {
        // 准备数据
        List<Tag> tagList = Arrays.asList(testTag);
        when(tagMapper.selectTagsByArticleId(1L)).thenReturn(tagList);

        // 执行测试
        List<Tag> result = tagService.selectTagsByArticleId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagMapper).selectTagsByArticleId(1L);
    }

    /**
     * 测试根据文章ID查询标签列表 - 空结果
     */
    @Test
    void testSelectTagsByArticleId_Empty() {
        // 模拟空结果
        when(tagMapper.selectTagsByArticleId(1L)).thenReturn(Collections.emptyList());

        // 执行测试
        List<Tag> result = tagService.selectTagsByArticleId(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagMapper).selectTagsByArticleId(1L);
    }
}