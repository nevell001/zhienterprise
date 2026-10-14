package com.zhi.system.service;

import java.util.List;
import com.zhi.system.domain.ArticleTag;

/**
 * 文章标签关联Service接口
 * 
 * @author nevell
 * @date 2025-09-08
 */
public interface IArticleTagService 
{
    /**
     * 查询文章标签关联列表
     * 
     * @param articleTag 文章标签关联
     * @return 文章标签关联集合
     */
    public List<ArticleTag> selectArticleTagList(ArticleTag articleTag);

    /**
     * 通过文章ID查询标签ID列表
     * 
     * @param articleId 文章ID
     * @return 标签ID列表
     */
    public List<Long> selectTagIdsByArticleId(Long articleId);

    /**
     * 通过标签ID查询文章ID列表
     * 
     * @param tagId 标签ID
     * @return 文章ID列表
     */
    public List<Long> selectArticleIdsByTagId(Long tagId);

    /**
     * 新增文章标签关联
     * 
     * @param articleTag 文章标签关联
     * @return 结果
     */
    public int insertArticleTag(ArticleTag articleTag);

    /**
     * 批量新增文章标签关联
     * 
     * @param articleTagList 文章标签关联列表
     * @return 结果
     */
    public int batchInsertArticleTag(List<ArticleTag> articleTagList);

    /**
     * 修改文章标签关联
     * 
     * @param articleTag 文章标签关联
     * @return 结果
     */
    public int updateArticleTag(ArticleTag articleTag);

    /**
     * 通过主键删除数据
     * 
     * @param id 主键ID
     * @return 影响行数
     */
    public int deleteArticleTagById(Long id);

    /**
     * 批量删除文章标签关联
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    public int deleteArticleTagByIds(Long[] ids);

    /**
     * 通过文章ID删除文章和标签关联
     * 
     * @param articleId 文章ID
     * @return 影响行数
     */
    public int deleteByArticleId(Long articleId);

    /**
     * 通过ID查询文章标签关联
     * 
     * @param id 主键ID
     * @return 文章标签关联
     */
    public ArticleTag selectArticleTagById(Long id);
}