package com.zhi.system.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.zhi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.zhi.common.cache.annotation.BlogCacheable;
import com.zhi.common.cache.annotation.BlogCacheEvict;
import org.springframework.stereotype.Service;
import com.zhi.system.mapper.BlogCategoryMapper;
import com.zhi.system.mapper.BlogArticleMapper;
import com.zhi.system.domain.BlogCategory;
import com.zhi.system.service.IBlogCategoryService;
import com.zhi.common.exception.ServiceException;

/**
 * 文章分类Service业务层处理
 * 
 * @author nevell
 * @date 2025-07-18
 */
@Service
public class BlogCategoryServiceImpl implements IBlogCategoryService 
{
    @Autowired
    private BlogCategoryMapper blogCategoryMapper;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    /**
     * 查询文章分类
     *
     * @param id 文章分类主键
     * @return 文章分类
     */
    @Override
    // 暂时禁用缓存，待 SpEL 解析修复后重新启用
    // @BlogCacheable(key = "blog:category:#p0", ttl = 60, timeUnit = TimeUnit.MINUTES)
    public BlogCategory selectBlogCategoryById(Long id)
    {
        return blogCategoryMapper.selectBlogCategoryById(id);
    }

    /**
     * 按别名查询分类（仅命中启用且未删除的分类）
     */
    public BlogCategory selectBlogCategoryByAlias(String alias)
    {
        return blogCategoryMapper.selectBlogCategoryByAlias(alias);
    }

    /**
     * 查询文章分类列表
     * 
     * @param blogCategory 文章分类
     * @return 文章分类
     */
    @Override
    @BlogCacheable(key = "blog:category:list:#blogCategory.hashCode()", ttl = 30, timeUnit = TimeUnit.MINUTES)
    public List<BlogCategory> selectBlogCategoryList(BlogCategory blogCategory)
    {
        if (blogCategory.getDelFlag() == null) {
            blogCategory.setDelFlag("0");
        }
        return blogCategoryMapper.selectBlogCategoryList(blogCategory);
    }

    /**
     * 新增文章分类
     * 
     * @param blogCategory 文章分类
     * @return 结果
     */
    @Override
    @BlogCacheEvict(value = {"blog:category:*", "blog:category:list:*"}, keyPattern = "blog:category:*")
    public int insertBlogCategory(BlogCategory blogCategory)
    {
        // 重名校验（仅未删除）
        if (blogCategory.getName() != null && blogCategoryMapper.countByName(blogCategory) > 0) {
            throw new ServiceException("分类名称已存在");
        }
        blogCategory.setCreateTime(DateUtils.getNowDate());
        return blogCategoryMapper.insertBlogCategory(blogCategory);
    }

    /**
     * 修改文章分类
     * 
     * @param blogCategory 文章分类
     * @return 结果
     */
    @Override
    @BlogCacheEvict(value = {"blog:category:*", "blog:category:list:*"}, keyPattern = "blog:category:*")
    public int updateBlogCategory(BlogCategory blogCategory)
    {
        // 重名校验（更新需要排除自身）
        if (blogCategory.getName() != null && blogCategoryMapper.countByName(blogCategory) > 0) {
            throw new ServiceException("分类名称已存在");
        }
        blogCategory.setUpdateTime(DateUtils.getNowDate());
        return blogCategoryMapper.updateBlogCategory(blogCategory);
    }

    /**
     * 批量删除文章分类
     *
     * @param ids 需要删除的文章分类主键
     * @return 结果
     */
    @Override
    @BlogCacheEvict(value = {"blog:category:*", "blog:category:list:*"}, keyPattern = "blog:category:*")
    public int deleteBlogCategoryByIds(Long[] ids)
    {
        // 被引用校验
        int refCount = blogArticleMapper.countByCategoryIds(ids);
        if (refCount > 0) {
            throw new ServiceException("存在文章引用该分类，无法删除，请先迁移或删除相关文章");
        }
        return blogCategoryMapper.deleteBlogCategoryByIds(ids);
    }

    /**
     * 删除文章分类信息
     *
     * @param id 文章分类主键
     * @return 结果
     */
    @Override
    @BlogCacheEvict(value = {"blog:category:*", "blog:category:list:*"}, keyPattern = "blog:category:*")
    public int deleteBlogCategoryById(Long id)
    {
        int refCount = blogArticleMapper.countByCategoryIds(new Long[]{ id });
        if (refCount > 0) {
            throw new ServiceException("存在文章引用该分类，无法删除，请先迁移或删除相关文章");
        }
        return blogCategoryMapper.deleteBlogCategoryById(id);
    }

    /**
     * 查询前台分类列表（包含文章数量）
     *
     * @param blogCategory 分类实体
     * @return 包含文章数量的分类集合
     */
    @Override
    public List<BlogCategory> selectCategoryListForFront(BlogCategory blogCategory)
    {
        return blogCategoryMapper.selectCategoryListForFront(blogCategory);
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
        return blogCategoryMapper.updateArticleCount(id);
    }

    /**
     * 批量更新所有分类的文章数量
     *
     * @return 结果
     */
    @Override
    public int updateAllArticleCount()
    {
        return blogCategoryMapper.updateAllArticleCount();
    }

    /**
     * 查询分类总数
     *
     * @param blogCategory 分类查询条件
     * @return 分类数量
     */
    @Override
    public long selectBlogCategoryCount(BlogCategory blogCategory)
    {
        return blogCategoryMapper.selectBlogCategoryCount(blogCategory);
    }
}
