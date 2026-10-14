package com.zhi.system.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.zhi.common.cache.annotation.SiteCacheable;
import com.zhi.common.cache.annotation.SiteCacheEvict;
import org.springframework.stereotype.Service;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.domain.Tag;
import com.zhi.system.mapper.TagMapper;
import com.zhi.system.service.ITagService;

/**
 * 博客标签Service业务层处理
 * 
 * @author nevell
 * @date 2025-09-08
 */
@Service
public class TagServiceImpl implements ITagService
{
    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

    @Autowired
    private TagMapper tagMapper;

    /**
     * 查询博客标签列表
     * 
     * @param tag 博客标签
     * @return 博客标签
     */
    @Override
    @SiteCacheable(key = "zhi:tag:list:#tag.hashCode()", ttl = 30, timeUnit = TimeUnit.MINUTES)
    public List<Tag> selectTagList(Tag tag)
    {
        return tagMapper.selectTagList(tag);
    }

    /**
     * 查询所有标签列表
     * 
     * @return 标签列表
     */
    @Override
    @SiteCacheable(key = "zhi:tag:all", ttl = 60, timeUnit = TimeUnit.MINUTES)
    public List<Tag> selectAllTagList()
    {
        return tagMapper.selectAllTagList();
    }

    /**
         * 通过ID查询单条数据
         * 
         * @param id 标签ID
         * @return 实例对象
         */
        @Override
        @SiteCacheable(key = "zhi:tag:#id", ttl = 60, timeUnit = TimeUnit.MINUTES)
        public Tag selectTagById(Long id)
        {
            log.info("📝 Service: 查询标签，参数 id = {}", id);
            Tag result = tagMapper.selectTagById(id);
            log.info("📝 Service: 查询标签完成，结果 id = {}, name = {}", 
                     result != null ? result.getId() : null, 
                     result != null ? result.getName() : null);
            return result;
        }
    /**
     * 新增博客标签
     * 
     * @param tag 博客标签
     * @return 结果
     */
    @Override
    @SiteCacheEvict(value = {"zhi:tag:*"}, keyPattern = "zhi:tag:*")
    public int insertTag(Tag tag)
    {
        tag.setCreateBy(SecurityUtils.getUsername());
        return tagMapper.insertTag(tag);
    }

    /**
     * 修改博客标签
     * 
     * @param tag 博客标签
     * @return 结果
     */
    @Override
    @SiteCacheEvict(value = {"zhi:tag:*"}, keyPattern = "zhi:tag:*")
    public int updateTag(Tag tag)
    {
        tag.setUpdateBy(SecurityUtils.getUsername());
        return tagMapper.updateTag(tag);
    }

    /**
     * 通过主键删除数据
     *
     * @param id 标签ID
     * @return 影响行数
     */
    @Override
    @SiteCacheEvict(value = {"zhi:tag:*"}, keyPattern = "zhi:tag:*")
    public int deleteTagById(Long id)
    {
        return tagMapper.deleteTagById(id);
    }

    /**
     * 批量删除博客标签
     *
     * @param tagIds 需要删除的数据ID
     * @return 影响行数
     */
    @Override
    @SiteCacheEvict(value = {"zhi:tag:*"}, keyPattern = "zhi:tag:*")
    public int deleteTagByIds(Long[] tagIds)
    {
        return tagMapper.deleteTagByIds(tagIds);
    }

    @Override
    @SiteCacheable(key = "zhi:tag:cloud", ttl = 30, timeUnit = TimeUnit.MINUTES)
    public List<Map<String, Object>> getTagCloud() {
        return tagMapper.getTagCloud();
    }

    @Override
    public boolean checkTagNameUnique(Tag tag)
    {
        Long tagId = tag.getId() == null ? -1L : tag.getId();
        Tag info = tagMapper.checkTagNameUnique(tag.getName());
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
        int count = tagMapper.checkTagExistArticle(id);
        return count > 0;
    }

    /**
     * 根据文章ID查询标签列表
     *
     * @param articleId 文章ID
     * @return 标签列表
     */
    @Override
    public List<Tag> selectTagsByArticleId(Long articleId)
    {
        return tagMapper.selectTagsByArticleId(articleId);
    }

    /**
     * 查询标签总数
     *
     * @param tag 标签查询条件
     * @return 标签数量
     */
    @Override
    public long selectTagCount(Tag tag)
    {
        return tagMapper.selectTagCount(tag);
    }
}