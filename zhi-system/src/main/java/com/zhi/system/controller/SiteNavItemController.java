package com.zhi.system.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Log;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.enums.BusinessType;
import com.zhi.system.domain.SiteNavItem;
import com.zhi.system.service.ISiteNavItemService;

/**
 * 站点导航项Controller（后台管理）
 *
 * @author zhi
 */
@RestController
@RequestMapping("/system/nav")
public class SiteNavItemController extends BaseController
{
    @Autowired
    private ISiteNavItemService navItemService;

    /**
     * 查询导航树（含隐藏项）
     */
    @PreAuthorize("@ss.hasPermi('site:nav:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(value = "location", defaultValue = "header") String location)
    {
        return success(navItemService.selectNavTree(location));
    }

    /**
     * 查询单个导航项
     */
    @PreAuthorize("@ss.hasPermi('site:nav:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(navItemService.selectSiteNavItemById(id));
    }

    /**
     * 新增导航项
     */
    @PreAuthorize("@ss.hasPermi('site:nav:add')")
    @Log(title = "站点导航项", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SiteNavItem item)
    {
        item.setCreateBy(getUsername());
        return toAjax(navItemService.insertSiteNavItem(item));
    }

    /**
     * 修改导航项
     */
    @PreAuthorize("@ss.hasPermi('site:nav:edit')")
    @Log(title = "站点导航项", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SiteNavItem item)
    {
        item.setUpdateBy(getUsername());
        return toAjax(navItemService.updateSiteNavItem(item));
    }

    /**
     * 删除导航项（级联子项）
     */
    @PreAuthorize("@ss.hasPermi('site:nav:remove')")
    @Log(title = "站点导航项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        navItemService.deleteSiteNavItemByIds(ids);
        return success();
    }

    /**
     * 导入模板包导航（清空指定位置后重建，事务内）
     */
    @PreAuthorize("@ss.hasPermi('site:nav:edit')")
    @Log(title = "站点导航项", businessType = BusinessType.UPDATE)
    @PostMapping("/import")
    public AjaxResult importNav(@RequestBody SiteNavItemImportBody body)
    {
        navItemService.importNav(body.getLocation(), body.getItems());
        return success();
    }

    /** 模板包导航导入请求体 */
    public static class SiteNavItemImportBody
    {
        private String location;
        private List<SiteNavItem> items;

        public String getLocation()
        {
            return location;
        }

        public void setLocation(String location)
        {
            this.location = location;
        }

        public List<SiteNavItem> getItems()
        {
            return items;
        }

        public void setItems(List<SiteNavItem> items)
        {
            this.items = items;
        }
    }
}
