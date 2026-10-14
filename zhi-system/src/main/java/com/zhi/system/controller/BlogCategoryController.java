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
import com.zhi.system.domain.BlogCategory;
import com.zhi.system.service.IBlogCategoryService;
import com.zhi.common.utils.poi.ExcelUtil;
import com.zhi.common.core.page.TableDataInfo;

/**
 * 文章分类Controller
 * 
 * @author nevell
 * @date 2025-07-18
 */
@RestController
@RequestMapping("/system/category")
public class BlogCategoryController extends BaseController
{
    @Autowired
    private IBlogCategoryService blogCategoryService;

    /**
     * 查询文章分类列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(BlogCategory blogCategory)
    {
        if (blogCategory.getDelFlag() == null) {
            blogCategory.setDelFlag("0");
        }
        startPage();
        // 使用前台查询方法，获取实时统计的文章数量（与前台显示一致）
        List<BlogCategory> list = blogCategoryService.selectCategoryListForFront(blogCategory);
        return getDataTable(list);
    }

    /**
     * 导出文章分类列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:category:export')")
    @Log(title = "文章分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BlogCategory blogCategory)
    {
        if (blogCategory.getDelFlag() == null) {
            blogCategory.setDelFlag("0");
        }
        // 使用前台查询方法，获取实时统计的文章数量（与前台显示一致）
        List<BlogCategory> list = blogCategoryService.selectCategoryListForFront(blogCategory);
        ExcelUtil<BlogCategory> util = new ExcelUtil<BlogCategory>(BlogCategory.class);
        util.exportExcel(response, list, "文章分类数据");
    }

    /**
     * 获取文章分类详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:category:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogCategoryService.selectBlogCategoryById(id));
    }

    /**
     * 新增文章分类
     */
    @PreAuthorize("@ss.hasPermi('zhi:category:add')")
    @Log(title = "文章分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogCategory blogCategory)
    {
        return toAjax(blogCategoryService.insertBlogCategory(blogCategory));
    }

    /**
     * 修改文章分类
     */
    @PreAuthorize("@ss.hasPermi('zhi:category:edit')")
    @Log(title = "文章分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogCategory blogCategory)
    {
        return toAjax(blogCategoryService.updateBlogCategory(blogCategory));
    }

    /**
     * 删除文章分类
     */
    @PreAuthorize("@ss.hasPermi('zhi:category:remove')")
    @Log(title = "文章分类", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogCategoryService.deleteBlogCategoryByIds(ids));
    }
}
