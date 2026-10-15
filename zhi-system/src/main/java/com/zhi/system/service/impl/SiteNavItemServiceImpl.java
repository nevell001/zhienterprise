package com.zhi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zhi.common.exception.ServiceException;
import com.zhi.system.domain.SiteNavItem;
import com.zhi.system.mapper.SiteNavItemMapper;
import com.zhi.system.service.ISiteNavItemService;

/**
 * 站点导航项 服务实现
 *
 * @author zhi
 */
@Service
public class SiteNavItemServiceImpl implements ISiteNavItemService
{
    private static final Logger log = LoggerFactory.getLogger(SiteNavItemServiceImpl.class);

    @Autowired
    private SiteNavItemMapper siteNavItemMapper;

    @Override
    public List<SiteNavItem> selectVisibleNavTree(String location)
    {
        try
        {
            return buildTree(siteNavItemMapper.selectVisibleByLocation(location));
        }
        catch (DataAccessException e)
        {
            // 仅当 site_nav_item 表缺失（老库未重跑 sql/00_init_database.sql 的幂等升级块）时降级：
            // 返回空树，前台回退内置默认导航；其余数据库故障如实抛出，避免掩盖真实问题。
            if (!isMissingNavTable(e))
            {
                throw e;
            }
            log.warn("site_nav_item 表不存在（请重跑 sql/00_init_database.sql），返回空导航: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean isMissingNavTable(DataAccessException e)
    {
        Throwable cause = e;
        while (cause != null)
        {
            String message = cause.getMessage();
            if (message != null && message.contains("site_nav_item") && message.contains("doesn't exist"))
            {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @Override
    public List<SiteNavItem> selectNavTree(String location)
    {
        return buildTree(siteNavItemMapper.selectAllByLocation(location));
    }

    @Override
    public SiteNavItem selectSiteNavItemById(Long id)
    {
        return siteNavItemMapper.selectSiteNavItemById(id);
    }

    @Override
    public int insertSiteNavItem(SiteNavItem item)
    {
        if (item.getParentId() == null)
        {
            item.setParentId(0L);
        }
        return siteNavItemMapper.insertSiteNavItem(item);
    }

    @Override
    public int updateSiteNavItem(SiteNavItem item)
    {
        if (item.getId() == null)
        {
            throw new ServiceException("导航项ID不能为空");
        }
        if (item.getId().equals(item.getParentId()))
        {
            throw new ServiceException("导航项不能以自身为父项");
        }
        return siteNavItemMapper.updateSiteNavItem(item);
    }

    @Override
    public void deleteSiteNavItemByIds(Long[] ids)
    {
        siteNavItemMapper.deleteSiteNavItemByIds(ids);
    }

    @Override
    @Transactional
    public void importNav(String location, List<SiteNavItem> items)
    {
        if (location == null || location.isBlank())
        {
            throw new ServiceException("导航位置不能为空");
        }
        siteNavItemMapper.deleteByLocation(location);
        int sort = 1;
        for (SiteNavItem item : items)
        {
            item.setLocation(location);
            item.setSort(item.getSort() == null ? sort : item.getSort());
            item.setVisible(item.getVisible() == null ? 1 : item.getVisible());
            List<SiteNavItem> children = item.getChildren();
            item.setChildren(null);
            siteNavItemMapper.insertSiteNavItem(item);
            if (children != null)
            {
                for (SiteNavItem child : children)
                {
                    child.setParentId(item.getId());
                    child.setLocation(location);
                    siteNavItemMapper.insertSiteNavItem(child);
                }
            }
            sort++;
        }
    }

    /** 顶级项挂子项（两级导航） */
    private List<SiteNavItem> buildTree(List<SiteNavItem> list)
    {
        List<SiteNavItem> tops = new ArrayList<>();
        for (SiteNavItem item : list)
        {
            if (item.getParentId() == null || item.getParentId() == 0L)
            {
                tops.add(item);
            }
        }
        for (SiteNavItem top : tops)
        {
            List<SiteNavItem> children = new ArrayList<>();
            for (SiteNavItem item : list)
            {
                if (item.getParentId() != null && item.getParentId().equals(top.getId()))
                {
                    children.add(item);
                }
            }
            top.setChildren(children.isEmpty() ? null : children);
        }
        return tops;
    }
}
