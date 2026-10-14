package com.zhi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.system.domain.ArticleTag;
import com.zhi.system.mapper.ArticleTagMapper;
import com.zhi.system.service.IArticleTagService;

/**
 * 文章标签关联Service业务层处理
 * 
 * @author nevell
 * @date 2025-09-08
 */
@Service
public class ArticleTagServiceImpl  implements IArticleTagService
{
    @Autowired
    private ArticleTagMapper articleTagMapper;

    /**
     * 查询文章标签关联列表
     * 
     * @param articleTag 文章标签关联
     * @return 文章标签关联集合
     */
    @Override
    public List<ArticleTag> selectArticleTagList(ArticleTag articleTag)
    {
        return articleTagMapper.selectArticleTagList(articleTag);
    }

    /**
     * 通过文章ID查询标签ID列表
     * 
     * @param articleId 文章ID
     * @return 标签ID列表
     */
    @Override
    public List<Long> selectTagIdsByArticleId(Long articleId)
    {
        return articleTagMapper.selectTagIdsByArticleId(articleId);
    }

    /**
     * 通过标签ID查询文章ID列表
     * 
     * @param tagId 标签ID
     * @return 文章ID列表
     */
    @Override
    public List<Long> selectArticleIdsByTagId(Long tagId)
    {
        return articleTagMapper.selectArticleIdsByTagId(tagId);
    }

    /**
     * 新增文章标签关联
     * 
     * @param articleTag 文章标签关联
     * @return 结果
     */
    @Override
    public int insertArticleTag(ArticleTag articleTag)
    {
        return articleTagMapper.insertArticleTag(articleTag);
    }

    /**
     * 批量新增文章标签关联
     * 
     * @param articleTagList 文章标签关联列表
     * @return 结果
     */
    @Override
    public int batchInsertArticleTag(List<ArticleTag> articleTagList)
    {
        return articleTagMapper.batchInsertArticleTag(articleTagList);
    }

    /**
     * 修改文章标签关联
     * 
     * @param articleTag 文章标签关联
     * @return 结果
     */
    @Override
    public int updateArticleTag(ArticleTag articleTag)
    {
        return articleTagMapper.updateArticleTag(articleTag);
    }

    /**
     * 通过主键删除数据
     * 
     * @param id 主键ID
     * @return 影响行数
     */
    @Override
    public int deleteArticleTagById(Long id)
    {
        return articleTagMapper.deleteArticleTagById(id);
    }

    /**
     * 批量删除文章标签关联
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    @Override
    public int deleteArticleTagByIds(Long[] ids)
    {
        return articleTagMapper.deleteArticleTagByIds(ids);
    }

    /**
     * 通过文章ID删除文章和标签关联
     * 
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Override
    public int deleteByArticleId(Long articleId)
    {
        return articleTagMapper.deleteByArticleId(articleId);
    }

    /**
     * 通过ID查询文章标签关联
     * 
     * @param id 主键ID
     * @return 文章标签关联
     */
    @Override
    public ArticleTag selectArticleTagById(Long id)
    {
        return articleTagMapper.selectArticleTagById(id);
    }
}