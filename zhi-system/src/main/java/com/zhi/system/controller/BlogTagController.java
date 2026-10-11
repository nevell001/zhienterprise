package com.zhi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.zhi.system.domain.BlogTag;
import com.zhi.system.service.IBlogTagService;
import com.zhi.common.utils.poi.ExcelUtil;
import com.zhi.common.core.page.TableDataInfo;

/**
 * 博客标签Controller
 * 
 * @author nevell
 * @date 2025-09-08
 */
@RestController
@RequestMapping("/system/tag")
public class BlogTagController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(BlogTagController.class);

    @Autowired
    private IBlogTagService blogTagService;

    /**
     * 查询博客标签列表
     */
    @PreAuthorize("@ss.hasPermi('blog:tag:list')")
    @GetMapping("/list")
    public TableDataInfo list(BlogTag blogTag)
    {
        startPage();
        List<BlogTag> list = blogTagService.selectBlogTagList(blogTag);
        return getDataTable(list);
    }

    /**
     * 导出博客标签列表
     */
    @PreAuthorize("@ss.hasPermi('blog:tag:export')")
    @Log(title = "标签", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BlogTag blogTag)
    {
        List<BlogTag> list = blogTagService.selectBlogTagList(blogTag);
        ExcelUtil<BlogTag> util = new ExcelUtil<BlogTag>(BlogTag.class);
        util.exportExcel(response, list, "标签数据");
    }

    /**
     * 获取博客标签详细信息
     */
    @PreAuthorize("@ss.hasPermi('blog:tag:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        log.info("📝 Controller: 获取标签详情，参数 id = {}", id);
        BlogTag result = blogTagService.selectBlogTagById(id);
        log.info("📝 Controller: 获取标签详情完成，结果 id = {}, name = {}", 
                 result != null ? result.getId() : null, 
                 result != null ? result.getName() : null);
        return success(result);
    }

    /**
     * 新增博客标签
     */
    @PreAuthorize("@ss.hasPermi('blog:tag:add')")
    @Log(title = "标签", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogTag blogTag)
    {
        if (!blogTagService.checkTagNameUnique(blogTag))
        {
            return error("新增标签'" + blogTag.getName() + "'失败，标签名称已存在");
        }
        return toAjax(blogTagService.insertBlogTag(blogTag));
    }

    /**
     * 修改博客标签
     */
    @PreAuthorize("@ss.hasPermi('blog:tag:edit')")
    @Log(title = "标签", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogTag blogTag)
    {
        if (!blogTagService.checkTagNameUnique(blogTag))
        {
            return error("修改标签'" + blogTag.getName() + "'失败，标签名称已存在");
        }
        return toAjax(blogTagService.updateBlogTag(blogTag));
    }

    /**
     * 删除博客标签
     */
    @PreAuthorize("@ss.hasPermi('blog:tag:remove')")
    @Log(title = "标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        for (Long id : ids)
        {
            if (blogTagService.checkTagExistArticle(id))
            {
                return error("删除标签失败，标签已关联文章");
            }
        }
        return toAjax(blogTagService.deleteBlogTagByIds(ids));
    }

    /**
     * 获取所有标签（供前端选择）
     */
    @PreAuthorize("@ss.hasPermi('blog:tag:list')")
    @GetMapping("/all")
    public AjaxResult getAllTags()
    {
        List<BlogTag> tags = blogTagService.selectAllTagList();
        return success(tags);
    }
}