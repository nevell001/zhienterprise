package com.zhi.system.mapper;

import java.util.List;
import java.util.Map;
import com.zhi.system.domain.Tag;

/**
 * 博客标签Mapper接口
 * 
 * @author nevell
 * @date 2025-09-08
 */
public interface TagMapper 
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
     * @param id 标签ID
     * @return 实例对象
     */
    public Tag selectTagById(Long id);

    /**
     * 新增博客标签
     * 
     * @param tag 博客标签
     * @return 影响行数
     */
    public int insertTag(Tag tag);

    /**
     * 修改博客标签
     * 
     * @param tag 博客标签
     * @return 影响行数
     */
    public int updateTag(Tag tag);

    /**
     * 通过主键删除数据
     *
     * @param id 标签ID
     * @return 影响行数
     */
    public int deleteTagById(Long id);

    /**
     * 批量删除博客标签
     * 
     * @param tagIds 需要删除的数据ID
     * @return 影响行数
     */
    public int deleteTagByIds(Long[] tagIds);

    /**
     * 校验标签名称是否唯一
     * 
     * @param tagName 标签名称
     * @return 标签信息
     */
    public Tag checkTagNameUnique(String tagName);
    
    /**
     * 检查标签是否已被文章使用
     *
     * @param id 标签ID
     * @return 结果
     */
    int checkTagExistArticle(Long id);

    /**
     * 获取标签云（包含文章数量）
     * @return 标签云列表
     */
    List<Map<String, Object>> getTagCloud();

    /**
     * 根据文章ID查询标签列表
     * @param articleId 文章ID
     * @return 标签列表
     */
    List<Tag> selectTagsByArticleId(Long articleId);

    /**
     * 批量查询多篇文章的标签列表（解决N+1查询问题）
     * @param articleIds 文章ID列表
     * @return 标签映射列表（每行包含 articleId 和标签信息）
     */
    List<Map<String, Object>> selectTagsByArticleIds(List<Long> articleIds);

    /**
     * 查询标签总数
     * @param tag 标签查询条件
     * @return 标签数量
     */
    public long selectTagCount(Tag tag);
}