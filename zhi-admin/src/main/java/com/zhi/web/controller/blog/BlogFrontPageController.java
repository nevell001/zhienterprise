package com.zhi.web.controller.blog;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.utils.BlogSwitchUtils;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.ip.IpUtils;
import com.zhi.system.domain.BlogPage;
import com.zhi.system.service.IBlogPageService;
import com.zhi.system.service.IBlogSettingService;
import com.zhi.system.service.IBlogVisitLogService;

/**
 * 自定义页面前台接口
 * 
 * @author nevell
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/api/page")
public class BlogFrontPageController extends BaseController
{
    @Autowired
    private IBlogPageService blogPageService;

    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired
    private IBlogVisitLogService blogVisitLogService;

    /**
     * 查询已发布页面列表（前台导航/页面索引使用）
     */
    @Anonymous
    @GetMapping("/list")
    public AjaxResult list()
    {
        List<BlogPage> list = blogPageService.selectPublishedPageList();
        return success(list);
    }

    /**
     * 按别名查询已发布页面详情（并累加浏览次数）
     */
    @Anonymous
    @GetMapping("/{slug}")
    public AjaxResult getBySlug(@PathVariable("slug") String slug, HttpServletRequest request)
    {
        BlogPage page = blogPageService.selectBlogPageBySlug(slug);
        if (page == null)
        {
            return error("页面不存在或未发布");
        }

        // 浏览统计开关：未配置或开启都算启用（与全站开关判定、文章详情同口径）
        String viewCountEnabled = blogSettingService.selectSettingValueByKey("view_count_enabled");
        if (BlogSwitchUtils.isOn(viewCountEnabled))
        {
            String viewerKey = buildViewerKey(request);
            blogPageService.addViewCount(page.getId(), viewerKey);
            page.setViewCount(page.getViewCount() == null ? 1L : page.getViewCount() + 1);
            blogVisitLogService.recordVisit("page", page.getId(), "/page/" + page.getSlug(), viewerKey, request);
        }
        return success(page);
    }

    /**
     * 构造访客标识：登录用户用 userId，否则回退到 IP
     */
    private String buildViewerKey(HttpServletRequest request)
    {
        try
        {
            Long userId = SecurityUtils.getUserId();
            if (userId != null)
            {
                return "u" + userId;
            }
        }
        catch (Exception ignored)
        {
            // 未登录，回退到 IP
        }
        try
        {
            String ip = IpUtils.getIpAddr(request);
            if (ip == null || ip.isEmpty())
            {
                return null;
            }
            return "ip:" + ip;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }
}
