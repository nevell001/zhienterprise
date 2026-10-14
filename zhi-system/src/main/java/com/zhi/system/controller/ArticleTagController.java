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
import com.zhi.system.domain.ArticleTag;
import com.zhi.system.service.IArticleTagService;
import com.zhi.common.core.page.TableDataInfo;

/**
 * 文章标签关联Controller
 * 
 * @author nevell
 * @date 2025-09-08
 */
@RestController
@RequestMapping("/system/articleTag")
public class ArticleTagController extends BaseController
{
    @Autowired
    private IArticleTagService articleTagService;

    /**
     * 查询文章标签关联列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:list')")
    @GetMapping("/list")
    public TableDataInfo list(ArticleTag articleTag)
    {
        startPage();
        List<ArticleTag> list = articleTagService.selectArticleTagList(articleTag);
        return getDataTable(list);
    }

    /**
     * 通过文章ID查询标签ID列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:list')")
    @GetMapping("/tags/{articleId}")
    public AjaxResult getTagIdsByArticleId(@PathVariable("articleId") Long articleId)
    {
        return success(articleTagService.selectTagIdsByArticleId(articleId));
    }

    /**
     * 通过标签ID查询文章ID列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:list')")
    @GetMapping("/articles/{tagId}")
    public AjaxResult getArticleIdsByTagId(@PathVariable("tagId") Long tagId)
    {
        return success(articleTagService.selectArticleIdsByTagId(tagId));
    }

    /**
     * 获取文章标签关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(articleTagService.selectArticleTagById(id));
    }

    /**
     * 新增文章标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:add')")
    @Log(title = "文章标签关联", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ArticleTag articleTag)
    {
        return toAjax(articleTagService.insertArticleTag(articleTag));
    }

    /**
     * 批量新增文章标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:add')")
    @Log(title = "文章标签关联", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody List<ArticleTag> articleTagList)
    {
        return toAjax(articleTagService.batchInsertArticleTag(articleTagList));
    }

    /**
     * 修改文章标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:edit')")
    @Log(title = "文章标签关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ArticleTag articleTag)
    {
        return toAjax(articleTagService.updateArticleTag(articleTag));
    }

    /**
     * 删除文章标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:remove')")
    @Log(title = "文章标签关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(articleTagService.deleteArticleTagByIds(ids));
    }

    /**
     * 通过文章ID删除文章和标签关联
     */
    @PreAuthorize("@ss.hasPermi('zhi:articleTag:remove')")
    @Log(title = "文章标签关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/article/{articleId}")
    public AjaxResult removeByArticleId(@PathVariable("articleId") Long articleId)
    {
        return toAjax(articleTagService.deleteByArticleId(articleId));
    }
}
