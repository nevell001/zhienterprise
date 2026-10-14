package com.zhi.system.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.zhi.common.cache.annotation.BlogCacheable;
import com.zhi.common.cache.annotation.BlogCacheEvict;
import org.springframework.stereotype.Service;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.domain.BlogTag;
import com.zhi.system.mapper.BlogTagMapper;
import com.zhi.system.service.IBlogTagService;

/**
 * 博客标签Service业务层处理
 * 
 * @author nevell
 * @date 2025-09-08
 */
@Service
public class BlogTagServiceImpl implements IBlogTagService
{
    private static final Logger log = LoggerFactory.getLogger(BlogTagServiceImpl.class);

    @Autowired
    private BlogTagMapper blogTagMapper;

    /**
     * 查询博客标签列表
     * 
     * @param blogTag 博客标签
     * @return 博客标签
     */
    @Override
    @BlogCacheable(key = "zhi:tag:list:#blogTag.hashCode()", ttl = 30, timeUnit = TimeUnit.MINUTES)
    public List<BlogTag> selectBlogTagList(BlogTag blogTag)
    {
        return blogTagMapper.selectBlogTagList(blogTag);
    }

    /**
     * 查询所有标签列表
     * 
     * @return 标签列表
     */
    @Override
    @BlogCacheable(key = "zhi:tag:all", ttl = 60, timeUnit = TimeUnit.MINUTES)
    public List<BlogTag> selectAllTagList()
    {
        return blogTagMapper.selectAllTagList();
    }

    /**
         * 通过ID查询单条数据
         * 
         * @param id 标签ID
         * @return 实例对象
         */
        @Override
        @BlogCacheable(key = "zhi:tag:#id", ttl = 60, timeUnit = TimeUnit.MINUTES)
        public BlogTag selectBlogTagById(Long id)
        {
            log.info("📝 Service: 查询标签，参数 id = {}", id);
            BlogTag result = blogTagMapper.selectBlogTagById(id);
            log.info("📝 Service: 查询标签完成，结果 id = {}, name = {}", 
                     result != null ? result.getId() : null, 
                     result != null ? result.getName() : null);
            return result;
        }
    /**
     * 新增博客标签
     * 
     * @param blogTag 博客标签
     * @return 结果
     */
    @Override
    @BlogCacheEvict(value = {"zhi:tag:*"}, keyPattern = "zhi:tag:*")
    public int insertBlogTag(BlogTag blogTag)
    {
        blogTag.setCreateBy(SecurityUtils.getUsername());
        return blogTagMapper.insertBlogTag(blogTag);
    }

    /**
     * 修改博客标签
     * 
     * @param blogTag 博客标签
     * @return 结果
     */
    @Override
    @BlogCacheEvict(value = {"zhi:tag:*"}, keyPattern = "zhi:tag:*")
    public int updateBlogTag(BlogTag blogTag)
    {
        blogTag.setUpdateBy(SecurityUtils.getUsername());
        return blogTagMapper.updateBlogTag(blogTag);
    }

    /**
     * 通过主键删除数据
     *
     * @param id 标签ID
     * @return 影响行数
     */
    @Override
    @BlogCacheEvict(value = {"zhi:tag:*"}, keyPattern = "zhi:tag:*")
    public int deleteBlogTagById(Long id)
    {
        return blogTagMapper.deleteBlogTagById(id);
    }

    /**
     * 批量删除博客标签
     *
     * @param tagIds 需要删除的数据ID
     * @return 影响行数
     */
    @Override
    @BlogCacheEvict(value = {"zhi:tag:*"}, keyPattern = "zhi:tag:*")
    public int deleteBlogTagByIds(Long[] tagIds)
    {
        return blogTagMapper.deleteBlogTagByIds(tagIds);
    }

    @Override
    @BlogCacheable(key = "zhi:tag:cloud", ttl = 30, timeUnit = TimeUnit.MINUTES)
    public List<Map<String, Object>> getTagCloud() {
        return blogTagMapper.getTagCloud();
    }

    @Override
    public boolean checkTagNameUnique(BlogTag blogTag)
    {
        Long tagId = blogTag.getId() == null ? -1L : blogTag.getId();
        BlogTag info = blogTagMapper.checkTagNameUnique(blogTag.getName());
        if (info != null && info.getId().longValue() != tagId.longValue())
        {
            return false;
        }
        return true;
    }
    
    /**
     * 检查标签是否已被文章使用
     *
     * @param id 标签ID
     * @return 结果
     */
    @Override
    public boolean checkTagExistArticle(Long id)
    {
        int count = blogTagMapper.checkTagExistArticle(id);
        return count > 0;
    }

    /**
     * 根据文章ID查询标签列表
     *
     * @param articleId 文章ID
     * @return 标签列表
     */
    @Override
    public List<BlogTag> selectTagsByArticleId(Long articleId)
    {
        return blogTagMapper.selectTagsByArticleId(articleId);
    }

    /**
     * 查询标签总数
     *
     * @param blogTag 标签查询条件
     * @return 标签数量
     */
    @Override
    public long selectBlogTagCount(BlogTag blogTag)
    {
        return blogTagMapper.selectBlogTagCount(blogTag);
    }
}