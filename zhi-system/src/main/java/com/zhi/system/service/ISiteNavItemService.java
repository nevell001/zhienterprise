package com.zhi.system.service;

import java.util.List;
import com.zhi.system.domain.SiteNavItem;

/**
 * 站点导航项 服务接口
 *
 * @author zhi
 */
public interface ISiteNavItemService
{
    /** 按位置查询可见导航树（公开侧；children 两级） */
    public List<SiteNavItem> selectVisibleNavTree(String location);

    /** 按位置查询全部导航树（管理侧，含隐藏） */
    public List<SiteNavItem> selectNavTree(String location);

    /** 按ID查询 */
    public SiteNavItem selectSiteNavItemById(Long id);

    /** 新增 */
    public int insertSiteNavItem(SiteNavItem item);

    /** 修改 */
    public int updateSiteNavItem(SiteNavItem item);

    /** 批量删除（级联子项） */
    public void deleteSiteNavItemByIds(Long[] ids);
}
