package com.zhi.system.service;

import java.util.List;
import com.zhi.system.domain.BlogCategory;

/**
 * 文章分类Service接口
 * 
 * @author nevell
 * @date 2025-07-18
 */
public interface IBlogCategoryService 
{
    /**
     * 查询文章分类
     * 
     * @param id 文章分类主键
     * @return 文章分类
     */
    public BlogCategory selectBlogCategoryById(Long id);

    /**
     * 按别名查询分类（仅命中启用且未删除的分类，alias 伪静态入口用）
     *
     * @param alias 分类别名
     * @return 文章分类
     */
    public BlogCategory selectBlogCategoryByAlias(String alias);

    /**
     * 查询文章分类列表
     * 
     * @param blogCategory 文章分类
     * @return 文章分类集合
     */
    public List<BlogCategory> selectBlogCategoryList(BlogCategory blogCategory);

    /**
     * 新增文章分类
     * 
     * @param blogCategory 文章分类
     * @return 结果
     */
    public int insertBlogCategory(BlogCategory blogCategory);

    /**
     * 修改文章分类
     * 
     * @param blogCategory 文章分类
     * @return 结果
     */
    public int updateBlogCategory(BlogCategory blogCategory);

    /**
     * 批量删除文章分类
     * 
     * @param ids 需要删除的文章分类主键集合
     * @return 结果
     */
    public int deleteBlogCategoryByIds(Long[] ids);

    /**
     * 删除文章分类信息
     *
     * @param id 文章分类主键
     * @return 结果
     */
    public int deleteBlogCategoryById(Long id);

    /**
     * 查询前台分类列表（包含文章数量）
     *
     * @param blogCategory 分类实体
     * @return 包含文章数量的分类集合
     */
    public List<BlogCategory> selectCategoryListForFront(BlogCategory blogCategory);

    /**
     * 更新指定分类的文章数量
     *
     * @param id 分类ID
     * @return 结果
     */
    public int updateArticleCount(Long id);

    /**
     * 批量更新所有分类的文章数量
     *
     * @return 结果
     */
    public int updateAllArticleCount();

    /**
     * 查询分类总数
     *
     * @param blogCategory 分类查询条件
     * @return 分类数量
     */
    public long selectBlogCategoryCount(BlogCategory blogCategory);
}
