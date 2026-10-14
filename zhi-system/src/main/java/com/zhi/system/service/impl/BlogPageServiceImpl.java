package com.zhi.system.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.common.core.redis.RedisCache;
import com.zhi.common.exception.ServiceException;
import com.zhi.common.utils.ModernDateUtils;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.system.domain.BlogPage;
import com.zhi.system.mapper.BlogPageMapper;
import com.zhi.system.service.IBlogPageService;

/**
 * 自定义页面Service业务层处理
 * 
 * @author nevell
 * @date 2026-09-10
 */
@Service
public class BlogPageServiceImpl implements IBlogPageService
{
    /** 草稿 */
    private static final String STATUS_DRAFT = "0";

    /** 已发布 */
    private static final String STATUS_PUBLISHED = "1";

    /** 别名允许的字符：字母、数字、下划线、连字符 */
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");

    @Autowired
    private BlogPageMapper blogPageMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 查询页面列表
     * 
     * @param blogPage 页面
     * @return 页面集合
     */
    @Override
    public List<BlogPage> selectBlogPageList(BlogPage blogPage)
    {
        return blogPageMapper.selectBlogPageList(blogPage);
    }

    /**
     * 查询已发布页面列表（前台用）
     * 
     * @return 页面集合
     */
    @Override
    public List<BlogPage> selectPublishedPageList()
    {
        return blogPageMapper.selectPublishedPageList();
    }

    /**
     * 通过ID查询单条数据
     * 
     * @param id 页面ID
     * @return 实例对象
     */
    @Override
    public BlogPage selectBlogPageById(Long id)
    {
        return blogPageMapper.selectBlogPageById(id);
    }

    /**
     * 通过别名查询已发布页面
     * 
     * @param slug 页面别名
     * @return 实例对象
     */
    @Override
    public BlogPage selectBlogPageBySlug(String slug)
    {
        if (StringUtils.isEmpty(slug))
        {
            return null;
        }
        return blogPageMapper.selectBlogPageBySlug(slug.trim());
    }

    /**
     * 新增页面
     * 
     * @param blogPage 页面
     * @return 结果
     */
    @Override
    public int insertBlogPage(BlogPage blogPage)
    {
        validatePage(blogPage);
        if (!checkSlugUnique(blogPage))
        {
            throw new ServiceException("页面别名已存在：" + blogPage.getSlug());
        }
        if (StringUtils.isEmpty(blogPage.getStatus()))
        {
            blogPage.setStatus(STATUS_DRAFT);
        }
        if (StringUtils.isEmpty(blogPage.getShowInNav()))
        {
            blogPage.setShowInNav("1");
        }
        if (blogPage.getSort() == null)
        {
            blogPage.setSort(0);
        }
        blogPage.setViewCount(0L);
        blogPage.setDelFlag("0");
        blogPage.setCreateBy(SecurityUtils.getUsername());
        blogPage.setCreateTime(ModernDateUtils.now());
        return blogPageMapper.insertBlogPage(blogPage);
    }

    /**
     * 修改页面
     * 
     * @param blogPage 页面
     * @return 结果
     */
    @Override
    public int updateBlogPage(BlogPage blogPage)
    {
        if (blogPage.getId() == null)
        {
            throw new ServiceException("页面ID不能为空");
        }
        if (StringUtils.isNotEmpty(blogPage.getSlug()))
        {
            validateSlug(blogPage.getSlug());
            if (!checkSlugUnique(blogPage))
            {
                throw new ServiceException("页面别名已存在：" + blogPage.getSlug());
            }
        }
        blogPage.setUpdateBy(SecurityUtils.getUsername());
        blogPage.setUpdateTime(ModernDateUtils.now());
        return blogPageMapper.updateBlogPage(blogPage);
    }

    /**
     * 切换页面发布状态
     * 
     * @param id 页面ID
     * @param status 状态（0草稿 1已发布）
     * @return 结果
     */
    @Override
    public int changePageStatus(Long id, String status)
    {
        if (!STATUS_DRAFT.equals(status) && !STATUS_PUBLISHED.equals(status))
        {
            throw new ServiceException("页面状态不合法");
        }
        BlogPage page = new BlogPage();
        page.setId(id);
        page.setStatus(status);
        page.setUpdateBy(SecurityUtils.getUsername());
        page.setUpdateTime(ModernDateUtils.now());
        return blogPageMapper.updateBlogPage(page);
    }

    /**
     * 增加浏览次数
     * 
     * @param id 页面ID
     * @return 结果
     */
    @Override
    public int increaseViewCount(Long id)
    {
        if (id == null)
        {
            return 0;
        }
        return blogPageMapper.increaseViewCount(id);
    }

    /**
     * 按访客去重增加浏览次数（同一访客 24 小时内对同一页面只计一次）
     */
    @Override
    public int addViewCount(Long id, String viewerKey)
    {
        if (id == null)
        {
            return 0;
        }
        if (StringUtils.isEmpty(viewerKey))
        {
            // 无法判定访客身份时不去重，保持与旧行为一致
            return blogPageMapper.increaseViewCount(id);
        }
        String dedupeKey = "zhi:page:viewed:" + id + ":" + viewerKey;
        if (Boolean.TRUE.equals(redisCache.hasKey(dedupeKey)))
        {
            // 24 小时内已计过，跳过累加
            return 0;
        }
        redisCache.setCacheObject(dedupeKey, "1", 24, TimeUnit.HOURS);
        return blogPageMapper.increaseViewCount(id);
    }

    /**
     * 通过主键删除数据
     * 
     * @param id 页面ID
     * @return 影响行数
     */
    @Override
    public int deleteBlogPageById(Long id)
    {
        return blogPageMapper.deleteBlogPageById(id);
    }

    /**
     * 批量删除页面
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    @Override
    public int deleteBlogPageByIds(Long[] ids)
    {
        return blogPageMapper.deleteBlogPageByIds(ids);
    }

    /**
     * 新增时的字段校验
     */
    private void validatePage(BlogPage blogPage)
    {
        if (StringUtils.isEmpty(blogPage.getTitle()))
        {
            throw new ServiceException("页面标题不能为空");
        }
        if (blogPage.getTitle().length() > 100)
        {
            throw new ServiceException("页面标题长度不能超过100个字符");
        }
        validateSlug(blogPage.getSlug());
    }

    /**
     * 别名格式校验
     */
    private void validateSlug(String slug)
    {
        if (StringUtils.isEmpty(slug))
        {
            throw new ServiceException("页面别名不能为空");
        }
        if (!SLUG_PATTERN.matcher(slug.trim()).matches())
        {
            throw new ServiceException("页面别名只能包含字母、数字、下划线和连字符，且不超过100个字符");
        }
    }

    /**
     * 别名唯一性校验（修改时排除自身）
     */
    private boolean checkSlugUnique(BlogPage blogPage)
    {
        Long id = blogPage.getId() == null ? -1L : blogPage.getId();
        BlogPage exist = blogPageMapper.checkSlugUnique(blogPage.getSlug().trim());
        return exist == null || exist.getId().equals(id);
    }
}
