package com.zhi.system.controller;

import java.util.List;
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
import com.zhi.system.domain.BlogArticleTag;
import com.zhi.system.service.IBlogArticleTagService;
import com.zhi.common.core.page.TableDataInfo;

/**
 * 文章标签关联Controller
 * 
 * @author nevell
 * @date 2025-09-08
 */
@RestController
@RequestMapping("/system/articleTag")
public class BlogArticleTagController extends BaseController
{
    @Autowired
    private IBlogArticleTagService blogArticleTagService;

    /**
     * 查询文章标签关联列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:list')")
    @GetMapping("/list")
    public TableDataInfo list(BlogArticleTag blogArticleTag)
    {
        startPage();
        List<BlogArticleTag> list = blogArticleTagService.selectBlogArticleTagList(blogArticleTag);
        return getDataTable(list);
    }

    /**
     * 通过文章ID查询标签ID列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:list')")
    @GetMapping("/tags/{articleId}")
    public AjaxResult getTagIdsByArticleId(@PathVariable("articleId") Long articleId)
    {
        return success(blogArticleTagService.selectTagIdsByArticleId(articleId));
    }

    /**
     * 通过标签ID查询文章ID列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:list')")
    @GetMapping("/articles/{tagId}")
    public AjaxResult getArticleIdsByTagId(@PathVariable("tagId") Long tagId)
    {
        return success(blogArticleTagService.selectArticleIdsByTagId(tagId));
    }

    /**
     * 获取文章标签关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogArticleTagService.selectBlogArticleTagById(id));
    }

    /**
     * 新增文章标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:add')")
    @Log(title = "文章标签关联", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogArticleTag blogArticleTag)
    {
        return toAjax(blogArticleTagService.insertBlogArticleTag(blogArticleTag));
    }

    /**
     * 批量新增文章标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:add')")
    @Log(title = "文章标签关联", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody List<BlogArticleTag> articleTagList)
    {
        return toAjax(blogArticleTagService.batchInsertArticleTag(articleTagList));
    }

    /**
     * 修改文章标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:edit')")
    @Log(title = "文章标签关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogArticleTag blogArticleTag)
    {
        return toAjax(blogArticleTagService.updateBlogArticleTag(blogArticleTag));
    }

    /**
     * 删除文章标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:remove')")
    @Log(title = "文章标签关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogArticleTagService.deleteBlogArticleTagByIds(ids));
    }

    /**
     * 通过文章ID删除文章和标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:remove')")
    @Log(title = "文章标签关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/article/{articleId}")
    public AjaxResult removeByArticleId(@PathVariable("articleId") Long articleId)
    {
        return toAjax(blogArticleTagService.deleteByArticleId(articleId));
    }
}
