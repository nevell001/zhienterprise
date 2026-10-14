package com.zhi.system.service;

import java.util.List;
import java.util.Map;
import com.zhi.system.domain.Tag;

/**
 * 博客标签Service接口
 * 
 * @author nevell
 * @date 2025-09-08
 */
public interface ITagService
{
    /**
     * 查询博客标签列表
     * 
     * @param tag 博客标签
     * @return 博客标签集合
     */
    public List<Tag> selectTagList(Tag tag);

    /**
     * 查询所有标签列表
     * 
     * @return 标签列表
     */
    public List<Tag> selectAllTagList();

    /**
     * 通过ID查询单条数据
     * 
     * @param tagId 标签ID
     * @return 实例对象
     */
    public Tag selectTagById(Long tagId);

    /**
     * 新增博客标签
     * 
     * @param tag 博客标签
     * @return 结果
     */
    public int insertTag(Tag tag);

    /**
     * 修改博客标签
     * 
     * @param tag 博客标签
     * @return 结果
     */
    public int updateTag(Tag tag);

    /**
     * 通过主键删除数据
     * 
     * @param tagId 标签ID
     * @return 影响行数
     */
    public int deleteTagById(Long tagId);

    /**
     * 批量删除博客标签
     * 
     * @param tagIds 需要删除的数据ID
     * @return 影响行数
     */
    public int deleteTagByIds(Long[] tagIds);

    /**
     * 获取标签云（包含文章数量）
     * 
     * @return 标签云列表
     */
    public List<Map<String, Object>> getTagCloud();

    /**
     * 校验标签名称是否唯一
     * 
     * @param tag 标签信息
     * @return 结果
     */
    public boolean checkTagNameUnique(Tag tag);
    
    /**
     * 检查标签是否已被文章使用
     * 
     * @param tagId 标签ID
     * @return 结果
     */
    public boolean checkTagExistArticle(Long tagId);

    /**
     * 根据文章ID查询标签列表
     *
     * @param articleId 文章ID
     * @return 标签列表
     */
    public List<Tag> selectTagsByArticleId(Long articleId);

    /**
     * 查询标签总数
     *
     * @param tag 标签查询条件
     * @return 标签数量
     */
    public long selectTagCount(Tag tag);
}