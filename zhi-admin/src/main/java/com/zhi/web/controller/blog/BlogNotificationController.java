package com.zhi.web.controller.blog;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.page.TableDataInfo;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.domain.BlogNotification;
import com.zhi.system.service.IBlogNotificationService;

/**
 * 站内信通知控制器
 *
 * @author nevell
 * @date 2026-07-26
 */
@RestController
@RequestMapping("/api/notification")
public class BlogNotificationController extends BaseController
{
    @Autowired
    private IBlogNotificationService blogNotificationService;

    /**
     * 获取当前用户的站内信通知列表
     */
    @GetMapping("/list")
    public TableDataInfo list(BlogNotification blogNotification)
    {
        Long userId = SecurityUtils.getUserId();
        blogNotification.setRecipientId(userId);
        startPage();
        List<BlogNotification> list = blogNotificationService.selectBlogNotificationList(blogNotification);
        return getDataTable(list);
    }

    /**
     * 获取当前用户的未读通知数量
     */
    @GetMapping("/unread-count")
    public AjaxResult unreadCount()
    {
        Long userId = SecurityUtils.getUserId();
        int count = blogNotificationService.selectUnreadCount(userId);
        return success(count);
    }

    /**
     * 标记通知为已读（支持批量）
     */
    @PutMapping("/read/{ids}")
    public AjaxResult markAsRead(@PathVariable Long[] ids)
    {
        Long userId = SecurityUtils.getUserId();
        blogNotificationService.markAsRead(ids, userId);
        return AjaxResult.success();
    }

    /**
     * 标记当前用户所有通知为已读
     */
    @PutMapping("/read-all")
    public AjaxResult markAllAsRead()
    {
        Long userId = SecurityUtils.getUserId();
        blogNotificationService.markAllAsRead(userId);
        // 即使 0 行更新（无可读通知）也算成功
        return AjaxResult.success();
    }
}
