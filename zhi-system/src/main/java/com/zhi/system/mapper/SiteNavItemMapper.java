package com.zhi.system.mapper;

import java.util.List;
import com.zhi.system.domain.SiteNavItem;

/**
 * 站点导航项 数据层
 *
 * @author zhi
 */
public interface SiteNavItemMapper
{
    /** 按位置查询可见导航项（公开侧，已排序） */
    public List<SiteNavItem> selectVisibleByLocation(String location);

    /** 按位置查询全部导航项（管理侧，含隐藏，已排序） */
    public List<SiteNavItem> selectAllByLocation(String location);

    /** 按ID查询 */
    public SiteNavItem selectSiteNavItemById(Long id);

    /** 新增 */
    public int insertSiteNavItem(SiteNavItem item);

    /** 修改 */
    public int updateSiteNavItem(SiteNavItem item);

    /** 批量删除（级联删除其子项） */
    public int deleteSiteNavItemByIds(Long[] ids);

    /** 按位置清空（模板包导入用） */
    public int deleteByLocation(String location);
}
