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
import com.zhi.system.domain.Message;
import com.zhi.system.service.IMessageService;

/**
 * 留言板Controller（后台管理）
 * 
 * @author nevell
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/system/message")
public class MessageController extends BaseController
{
    @Autowired
    private IMessageService messageService;

    /**
     * 查询留言列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:list')")
    @GetMapping("/list")
    public TableDataInfo list(Message message)
    {
        startPage();
        List<Message> list = messageService.selectMessageList(message);
        return getDataTable(list);
    }

    /**
     * 导出留言列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:export')")
    @Log(title = "留言管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Message message)
    {
        List<Message> list = messageService.selectMessageList(message);
        ExcelUtil<Message> util = new ExcelUtil<Message>(Message.class);
        util.exportExcel(response, list, "留言数据");
    }

    /**
     * 获取留言详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(messageService.selectMessageById(id));
    }

    /**
     * 修改留言（仅内容与状态，后台一般不修改游客留言正文）
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:edit')")
    @Log(title = "留言管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Message message)
    {
        return toAjax(messageService.updateMessage(message));
    }

    /**
     * 审核留言（status：0待审核 1已发布 2已拒绝）
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:edit')")
    @Log(title = "留言管理", businessType = BusinessType.UPDATE)
    @PutMapping("/audit/{id}/{status}")
    public AjaxResult audit(@PathVariable("id") Long id, @PathVariable("status") String status)
    {
        return toAjax(messageService.auditMessage(id, status));
    }

    /**
     * 回复留言
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:reply')")
    @Log(title = "留言管理", businessType = BusinessType.UPDATE)
    @PutMapping("/reply/{id}")
    public AjaxResult reply(@PathVariable("id") Long id, @RequestBody Message message)
    {
        String replyContent = message == null ? null : message.getReplyContent();
        return toAjax(messageService.replyMessage(id, replyContent));
    }

    /**
     * 删除留言
     */
    @PreAuthorize("@ss.hasPermi('zhi:message:remove')")
    @Log(title = "留言管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(messageService.deleteMessageByIds(ids));
    }
}
