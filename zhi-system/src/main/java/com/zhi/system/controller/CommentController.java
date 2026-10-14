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
import com.zhi.system.domain.Comment;
import com.zhi.system.service.ICommentService;
import com.zhi.common.utils.poi.ExcelUtil;
import com.zhi.common.core.page.TableDataInfo;

/**
 * 博客评论Controller
 * 
 * @author nevell
 */
@RestController
@RequestMapping("/system/comment")
public class CommentController extends BaseController
{
    @Autowired
    private ICommentService commentService;

    /**
     * 查询博客评论列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:comment:list')")
    @GetMapping("/list")
    public TableDataInfo list(Comment comment)
    {
        startPage();
        List<Comment> list = commentService.selectCommentList(comment);
        return getDataTable(list);
    }

    /**
     * 导出博客评论列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:comment:export')")
    @Log(title = "评论", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Comment comment)
    {
        List<Comment> list = commentService.selectCommentList(comment);
        ExcelUtil<Comment> util = new ExcelUtil<Comment>(Comment.class);
        util.exportExcel(response, list, "评论数据");
    }

    /**
     * 获取博客评论详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:comment:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(commentService.selectCommentById(id));
    }

    /**
     * 新增博客评论
     */
    @PreAuthorize("@ss.hasPermi('zhi:comment:add')")
    @Log(title = "评论", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Comment comment)
    {
        return toAjax(commentService.insertComment(comment));
    }

    /**
     * 修改博客评论
     */
    @PreAuthorize("@ss.hasPermi('zhi:comment:edit')")
    @Log(title = "评论", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Comment comment)
    {
        return toAjax(commentService.updateComment(comment));
    }

    /**
     * 删除博客评论
     */
    @PreAuthorize("@ss.hasPermi('zhi:comment:remove')")
    @Log(title = "评论", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(commentService.deleteCommentByIds(ids));
    }
    
    /**
     * 审核通过博客评论
     */
    @PreAuthorize("@ss.hasPermi('zhi:comment:audit')")
    @Log(title = "评论", businessType = BusinessType.UPDATE)
    @PutMapping("/audit/{ids}")
    public AjaxResult audit(@PathVariable("ids") Long[] ids)
    {
        return toAjax(commentService.auditCommentByIds(ids));
    }
    
    /**
     * 审核拒绝博客评论
     */
    @PreAuthorize("@ss.hasPermi('zhi:comment:reject')")
    @Log(title = "评论", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{ids}")
    public AjaxResult reject(@PathVariable("ids") Long[] ids)
    {
        return toAjax(commentService.rejectCommentByIds(ids));
    }
}