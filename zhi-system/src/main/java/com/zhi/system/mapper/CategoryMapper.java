package com.zhi.system.mapper;

import java.util.List;
import com.zhi.system.domain.Category;

/**
 * 文章分类Mapper接口
 * 
 * @author nevell
 * @date 2025-07-18
 */
public interface CategoryMapper 
{
    /**
     * 查询文章分类
     * 
     * @param id 文章分类主键
     * @return 文章分类
     */
    public Category selectCategoryById(Long id);

    /**
     * 按别名查询分类（仅命中启用且未删除的分类，alias 伪静态入口用）
     * @param alias 分类别名
     * @return 博客分类
     */
    public Category selectCategoryByAlias(String alias);

    /**
     * 查询文章分类列表
     * 
     * @param category 文章分类
     * @return 文章分类集合
     */
    public List<Category> selectCategoryList(Category category);

    /**
     * 新增文章分类
     * 
     * @param category 文章分类
     * @return 结果
     */
    public int insertCategory(Category category);

    /**
     * 修改文章分类
     * 
     * @param category 文章分类
     * @return 结果
     */
    public int updateCategory(Category category);

    /**
     * 删除文章分类
     * 
     * @param id 文章分类主键
     * @return 结果
     */
    public int deleteCategoryById(Long id);

    /**
     * 批量删除文章分类
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCategoryByIds(Long[] ids);

    /**
     * 统计同名未删除分类数量（用于重名校验）
     * @param category 分类实体，需包含 name，可选 id（更新时排除自身）
     * @return 数量
     */
    public int countByName(Category category);

    /**
     * 查询前台分类列表（包含文章数量）
     * @param category 分类实体
     * @return 包含文章数量的分类集合
     */
    public List<Category> selectCategoryListForFront(Category category);

    /**
     * 更新指定分类的文章数量
     * @param id 分类ID
     * @return 结果
     */
    public int updateArticleCount(Long id);

    /**
     * 批量更新所有分类的文章数量
     * @return 结果
     */
    public int updateAllArticleCount();

    /**
     * 查询分类总数
     * @param category 分类查询条件
     * @return 分类数量
     */
    public long selectCategoryCount(Category category);
}
