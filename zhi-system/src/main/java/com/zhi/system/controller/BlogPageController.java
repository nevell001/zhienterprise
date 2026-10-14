package com.zhi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Log;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.enums.BusinessType;
import com.zhi.common.core.page.TableDataInfo;
import com.zhi.common.utils.poi.ExcelUtil;
import com.zhi.system.domain.BlogPage;
import com.zhi.system.service.IBlogPageService;

/**
 * 自定义页面Controller（后台管理）
 * 
 * @author nevell
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/system/page")
public class BlogPageController extends BaseController
{
    @Autowired
    private IBlogPageService blogPageService;

    /**
     * 查询页面列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:page:list')")
    @GetMapping("/list")
    public TableDataInfo list(BlogPage blogPage)
    {
        startPage();
        List<BlogPage> list = blogPageService.selectBlogPageList(blogPage);
        return getDataTable(list);
    }

    /**
     * 导出页面列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:page:export')")
    @Log(title = "页面管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BlogPage blogPage)
    {
        List<BlogPage> list = blogPageService.selectBlogPageList(blogPage);
        ExcelUtil<BlogPage> util = new ExcelUtil<BlogPage>(BlogPage.class);
        util.exportExcel(response, list, "自定义页面数据");
    }

    /**
     * 获取页面详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:page:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogPageService.selectBlogPageById(id));
    }

    /**
     * 新增页面
     */
    @PreAuthorize("@ss.hasPermi('zhi:page:add')")
    @Log(title = "页面管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogPage blogPage)
    {
        return toAjax(blogPageService.insertBlogPage(blogPage));
    }

    /**
     * 修改页面
     */
    @PreAuthorize("@ss.hasPermi('zhi:page:edit')")
    @Log(title = "页面管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogPage blogPage)
    {
        return toAjax(blogPageService.updateBlogPage(blogPage));
    }

    /**
     * 发布/下架页面（status：0草稿 1已发布）
     */
    @PreAuthorize("@ss.hasPermi('zhi:page:edit')")
    @Log(title = "页面管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus/{id}/{status}")
    public AjaxResult changeStatus(@PathVariable("id") Long id, @PathVariable("status") String status)
    {
        return toAjax(blogPageService.changePageStatus(id, status));
    }

    /**
     * 删除页面
     */
    @PreAuthorize("@ss.hasPermi('zhi:page:remove')")
    @Log(title = "页面管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogPageService.deleteBlogPageByIds(ids));
    }
}
