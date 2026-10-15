package com.zhi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private SiteNavItemMapper siteNavItemMapper;

    @Override
    public List<SiteNavItem> selectVisibleNavTree(String location)
    {
        return buildTree(siteNavItemMapper.selectVisibleByLocation(location));
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
