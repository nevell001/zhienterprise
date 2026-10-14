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
import com.zhi.system.domain.BlogMessage;
import com.zhi.system.service.IBlogMessageService;

/**
 * 留言板Controller（后台管理）
 * 
 * @author nevell
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/system/message")
public class BlogMessageController extends BaseController
{
    @Autowired
    private IBlogMessageService blogMessageService;

    /**
     * 查询留言列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:list')")
    @GetMapping("/list")
    public TableDataInfo list(BlogMessage blogMessage)
    {
        startPage();
        List<BlogMessage> list = blogMessageService.selectBlogMessageList(blogMessage);
        return getDataTable(list);
    }

    /**
     * 导出留言列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:export')")
    @Log(title = "留言管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BlogMessage blogMessage)
    {
        List<BlogMessage> list = blogMessageService.selectBlogMessageList(blogMessage);
        ExcelUtil<BlogMessage> util = new ExcelUtil<BlogMessage>(BlogMessage.class);
        util.exportExcel(response, list, "留言数据");
    }

    /**
     * 获取留言详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogMessageService.selectBlogMessageById(id));
    }

    /**
     * 修改留言（仅内容与状态，后台一般不修改游客留言正文）
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:edit')")
    @Log(title = "留言管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogMessage blogMessage)
    {
        return toAjax(blogMessageService.updateBlogMessage(blogMessage));
    }

    /**
     * 审核留言（status：0待审核 1已发布 2已拒绝）
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:edit')")
    @Log(title = "留言管理", businessType = BusinessType.UPDATE)
    @PutMapping("/audit/{id}/{status}")
    public AjaxResult audit(@PathVariable("id") Long id, @PathVariable("status") String status)
    {
        return toAjax(blogMessageService.auditBlogMessage(id, status));
    }

    /**
     * 回复留言
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:reply')")
    @Log(title = "留言管理", businessType = BusinessType.UPDATE)
    @PutMapping("/reply/{id}")
    public AjaxResult reply(@PathVariable("id") Long id, @RequestBody BlogMessage blogMessage)
    {
        String replyContent = blogMessage == null ? null : blogMessage.getReplyContent();
        return toAjax(blogMessageService.replyBlogMessage(id, replyContent));
    }

    /**
     * 删除留言
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:remove')")
    @Log(title = "留言管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogMessageService.deleteBlogMessageByIds(ids));
    }
}
