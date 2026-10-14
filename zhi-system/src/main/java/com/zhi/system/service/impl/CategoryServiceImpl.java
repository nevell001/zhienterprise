package com.zhi.system.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.zhi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.zhi.common.cache.annotation.SiteCacheable;
import com.zhi.common.cache.annotation.SiteCacheEvict;
import org.springframework.stereotype.Service;
import com.zhi.system.mapper.CategoryMapper;
import com.zhi.system.mapper.ArticleMapper;
import com.zhi.system.domain.Category;
import com.zhi.system.service.ICategoryService;
import com.zhi.common.exception.ServiceException;

/**
 * 文章分类Service业务层处理
 * 
 * @author nevell
 * @date 2025-07-18
 */
@Service
public class CategoryServiceImpl implements ICategoryService 
{
    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 查询文章分类
     *
     * @param id 文章分类主键
     * @return 文章分类
     */
    @Override
    // 暂时禁用缓存，待 SpEL 解析修复后重新启用
    // @SiteCacheable(key = "zhi:category:#p0", ttl = 60, timeUnit = TimeUnit.MINUTES)
    public Category selectCategoryById(Long id)
    {
        return categoryMapper.selectCategoryById(id);
    }

    /**
     * 按别名查询分类（仅命中启用且未删除的分类）
     */
    public Category selectCategoryByAlias(String alias)
    {
        return categoryMapper.selectCategoryByAlias(alias);
    }

    /**
     * 查询文章分类列表
     * 
     * @param category 文章分类
     * @return 文章分类
     */
    @Override
    @SiteCacheable(key = "zhi:category:list:#category.hashCode()", ttl = 30, timeUnit = TimeUnit.MINUTES)
    public List<Category> selectCategoryList(Category category)
    {
        if (category.getDelFlag() == null) {
            category.setDelFlag("0");
        }
        return categoryMapper.selectCategoryList(category);
    }

    /**
     * 新增文章分类
     * 
     * @param category 文章分类
     * @return 结果
     */
    @Override
    @SiteCacheEvict(value = {"zhi:category:*", "zhi:category:list:*"}, keyPattern = "zhi:category:*")
    public int insertCategory(Category category)
    {
        // 重名校验（仅未删除）
        if (category.getName() != null && categoryMapper.countByName(category) > 0) {
            throw new ServiceException("分类名称已存在");
        }
        category.setCreateTime(DateUtils.getNowDate());
        return categoryMapper.insertCategory(category);
    }

    /**
     * 修改文章分类
     * 
     * @param category 文章分类
     * @return 结果
     */
    @Override
    @SiteCacheEvict(value = {"zhi:category:*", "zhi:category:list:*"}, keyPattern = "zhi:category:*")
    public int updateCategory(Category category)
    {
        // 重名校验（更新需要排除自身）
        if (category.getName() != null && categoryMapper.countByName(category) > 0) {
            throw new ServiceException("分类名称已存在");
        }
        category.setUpdateTime(DateUtils.getNowDate());
        return categoryMapper.updateCategory(category);
    }

    /**
     * 批量删除文章分类
     *
     * @param ids 需要删除的文章分类主键
     * @return 结果
     */
    @Override
    @SiteCacheEvict(value = {"zhi:category:*", "zhi:category:list:*"}, keyPattern = "zhi:category:*")
    public int deleteCategoryByIds(Long[] ids)
    {
        // 被引用校验
        int refCount = articleMapper.countByCategoryIds(ids);
        if (refCount > 0) {
            throw new ServiceException("存在文章引用该分类，无法删除，请先迁移或删除相关文章");
        }
        return categoryMapper.deleteCategoryByIds(ids);
    }

    /**
     * 删除文章分类信息
     *
     * @param id 文章分类主键
     * @return 结果
     */
    @Override
    @SiteCacheEvict(value = {"zhi:category:*", "zhi:category:list:*"}, keyPattern = "zhi:category:*")
    public int deleteCategoryById(Long id)
    {
        int refCount = articleMapper.countByCategoryIds(new Long[]{ id });
        if (refCount > 0) {
            throw new ServiceException("存在文章引用该分类，无法删除，请先迁移或删除相关文章");
        }
        return categoryMapper.deleteCategoryById(id);
    }

    /**
     * 查询前台分类列表（包含文章数量）
     *
     * @param category 分类实体
     * @return 包含文章数量的分类集合
     */
    @Override
    public List<Category> selectCategoryListForFront(Category category)
    {
        return categoryMapper.selectCategoryListForFront(category);
    }

    /**
     * 更新指定分类的文章数量
     *
     * @param id 分类ID
     * @return 结果
     */
    @Override
    public int updateArticleCount(Long id)
    {
        return categoryMapper.updateArticleCount(id);
    }

    /**
     * 批量更新所有分类的文章数量
     *
     * @return 结果
     */
    @Override
    public int updateAllArticleCount()
    {
        return categoryMapper.updateAllArticleCount();
    }

    /**
     * 查询分类总数
     *
     * @param category 分类查询条件
     * @return 分类数量
     */
    @Override
    public long selectCategoryCount(Category category)
    {
        return categoryMapper.selectCategoryCount(category);
    }
}
