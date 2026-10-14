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
import com.zhi.system.domain.Page;
import com.zhi.system.mapper.PageMapper;
import com.zhi.system.service.IPageService;

/**
 * 自定义页面Service业务层处理
 * 
 * @author nevell
 * @date 2026-09-10
 */
@Service
public class PageServiceImpl implements IPageService
{
    /** 草稿 */
    private static final String STATUS_DRAFT = "0";

    /** 已发布 */
    private static final String STATUS_PUBLISHED = "1";

    /** 别名允许的字符：字母、数字、下划线、连字符 */
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 查询页面列表
     * 
     * @param page 页面
     * @return 页面集合
     */
    @Override
    public List<Page> selectPageList(Page page)
    {
        return pageMapper.selectPageList(page);
    }

    /**
     * 查询已发布页面列表（前台用）
     * 
     * @return 页面集合
     */
    @Override
    public List<Page> selectPublishedPageList()
    {
        return pageMapper.selectPublishedPageList();
    }

    /**
     * 通过ID查询单条数据
     * 
     * @param id 页面ID
     * @return 实例对象
     */
    @Override
    public Page selectPageById(Long id)
    {
        return pageMapper.selectPageById(id);
    }

    /**
     * 通过别名查询已发布页面
     * 
     * @param slug 页面别名
     * @return 实例对象
     */
    @Override
    public Page selectPageBySlug(String slug)
    {
        if (StringUtils.isEmpty(slug))
        {
            return null;
        }
        return pageMapper.selectPageBySlug(slug.trim());
    }

    /**
     * 新增页面
     * 
     * @param page 页面
     * @return 结果
     */
    @Override
    public int insertPage(Page page)
    {
        validatePage(page);
        if (!checkSlugUnique(page))
        {
            throw new ServiceException("页面别名已存在：" + page.getSlug());
        }
        if (StringUtils.isEmpty(page.getStatus()))
        {
            page.setStatus(STATUS_DRAFT);
        }
        if (StringUtils.isEmpty(page.getShowInNav()))
        {
            page.setShowInNav("1");
        }
        if (page.getSort() == null)
        {
            page.setSort(0);
        }
        page.setViewCount(0L);
        page.setDelFlag("0");
        page.setCreateBy(SecurityUtils.getUsername());
        page.setCreateTime(ModernDateUtils.now());
        return pageMapper.insertPage(page);
    }

    /**
     * 修改页面
     * 
     * @param page 页面
     * @return 结果
     */
    @Override
    public int updatePage(Page page)
    {
        if (page.getId() == null)
        {
            throw new ServiceException("页面ID不能为空");
        }
        if (StringUtils.isNotEmpty(page.getSlug()))
        {
            validateSlug(page.getSlug());
            if (!checkSlugUnique(page))
            {
                throw new ServiceException("页面别名已存在：" + page.getSlug());
            }
        }
        page.setUpdateBy(SecurityUtils.getUsername());
        page.setUpdateTime(ModernDateUtils.now());
        return pageMapper.updatePage(page);
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
        Page page = new Page();
        page.setId(id);
        page.setStatus(status);
        page.setUpdateBy(SecurityUtils.getUsername());
        page.setUpdateTime(ModernDateUtils.now());
        return pageMapper.updatePage(page);
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
        return pageMapper.increaseViewCount(id);
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
            return pageMapper.increaseViewCount(id);
        }
        String dedupeKey = "zhi:page:viewed:" + id + ":" + viewerKey;
        if (Boolean.TRUE.equals(redisCache.hasKey(dedupeKey)))
        {
            // 24 小时内已计过，跳过累加
            return 0;
        }
        redisCache.setCacheObject(dedupeKey, "1", 24, TimeUnit.HOURS);
        return pageMapper.increaseViewCount(id);
    }

    /**
     * 通过主键删除数据
     * 
     * @param id 页面ID
     * @return 影响行数
     */
    @Override
    public int deletePageById(Long id)
    {
        return pageMapper.deletePageById(id);
    }

    /**
     * 批量删除页面
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    @Override
    public int deletePageByIds(Long[] ids)
    {
        return pageMapper.deletePageByIds(ids);
    }

    /**
     * 新增时的字段校验
     */
    private void validatePage(Page page)
    {
        if (StringUtils.isEmpty(page.getTitle()))
        {
            throw new ServiceException("页面标题不能为空");
        }
        if (page.getTitle().length() > 100)
        {
            throw new ServiceException("页面标题长度不能超过100个字符");
        }
        validateSlug(page.getSlug());
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
    private boolean checkSlugUnique(Page page)
    {
        Long id = page.getId() == null ? -1L : page.getId();
        Page exist = pageMapper.checkSlugUnique(page.getSlug().trim());
        return exist == null || exist.getId().equals(id);
    }
}
