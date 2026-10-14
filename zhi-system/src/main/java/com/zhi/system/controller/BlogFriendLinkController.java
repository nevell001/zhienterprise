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
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.annotation.RateLimiter;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.enums.BusinessType;
import com.zhi.common.enums.LimitType;
import com.zhi.common.cache.annotation.BlogCacheEvict;
import com.zhi.common.constant.CacheConstants;
import com.zhi.system.domain.BlogFriendLink;
import com.zhi.system.service.IBlogFriendLinkService;
import com.zhi.system.service.IBlogSettingService;
import com.zhi.system.service.ICaptchaService;
import com.zhi.common.utils.BlogSwitchUtils;
import com.zhi.common.utils.poi.ExcelUtil;
import com.zhi.common.core.page.TableDataInfo;

/**
 * 友情链接Controller
 * 
 * @author nevell
 * @date 2025-09-08
 */
@RestController
@RequestMapping("/system/friendLink")
public class BlogFriendLinkController extends BaseController
{
    @Autowired
    private IBlogFriendLinkService blogFriendLinkService;

    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired
    private ICaptchaService captchaService;

    /**
     * 查询友情链接列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:list')")
    @GetMapping("/list")
    public TableDataInfo list(BlogFriendLink blogFriendLink)
    {
        startPage();
        List<BlogFriendLink> list = blogFriendLinkService.selectBlogFriendLinkList(blogFriendLink);
        return getDataTable(list);
    }

    /**
     * 查询前台展示的友情链接列表
     */
    @Anonymous
    @GetMapping("/front/list")
    public AjaxResult frontList()
    {
        List<BlogFriendLink> list = blogFriendLinkService.selectFrontFriendLinkList();
        return success(list);
    }

    /**
     * 前台申请友情链接（提交后进入待审核状态 status=2）
     */
    @Anonymous
    @RateLimiter(key = "zhi:friendLink:apply:", time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody BlogFriendLink blogFriendLink)
    {
        String name = blogFriendLink.getName();
        String url = blogFriendLink.getUrl();
        if (name == null || name.trim().isEmpty())
        {
            return error("网站名称不能为空");
        }
        if (name.trim().length() > 64)
        {
            return error("网站名称不能超过64个字符");
        }
        if (url == null || url.trim().isEmpty())
        {
            return error("网站地址不能为空");
        }
        if (url.trim().length() > 255)
        {
            return error("网站地址不能超过255个字符");
        }
        if (blogFriendLink.getEmail() != null && blogFriendLink.getEmail().length() > 100)
        {
            return error("邮箱长度不能超过100个字符");
        }

        blogFriendLink.setId(null);
        blogFriendLink.setName(name.trim());
        blogFriendLink.setUrl(url.trim());
        if (blogFriendLink.getDescription() != null && blogFriendLink.getDescription().length() > 255)
        {
            blogFriendLink.setDescription(blogFriendLink.getDescription().substring(0, 255));
        }
        if (blogFriendLink.getLogo() != null && blogFriendLink.getLogo().length() > 255)
        {
            blogFriendLink.setLogo(blogFriendLink.getLogo().substring(0, 255));
        }
        // 前台是否开放友链申请（后台开关，默认开放）
        String applyEnabled = blogSettingService.selectSettingValueByKey("friend_link_apply_enabled");
        if (BlogSwitchUtils.isOff(applyEnabled))
        {
            return error("本站暂未开放友链申请");
        }

        // 图形验证码：仅在验证码启用时校验（与登录/注册同一开关）
        captchaService.validate(blogFriendLink.getCode(), blogFriendLink.getUuid());

        blogFriendLink.setStatus("2"); // 待审核
        blogFriendLink.setDelFlag("0");
        if (blogFriendLink.getSort() == null)
        {
            blogFriendLink.setSort(0);
        }
        blogFriendLink.setCreateBy("游客申请");
        return toAjax(blogFriendLinkService.insertBlogFriendLink(blogFriendLink));
    }

    /**
     * 审核友情链接申请。
     *
     * <p>status 与 blog_friend_link 全栈语义一致：<b>0=通过/正常（前台展示）</b>，
     * 1=拒绝/停用，2=待审核（提交申请时写入）。前台列表查询的是 status='0'，
     * 所以"通过"必须传 0、"拒绝"必须传 1。</p>
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:edit')")
    @Log(title = "友情链接", businessType = BusinessType.UPDATE)
    @BlogCacheEvict(value = CacheConstants.BLOG_FRIEND_LINK_LIST)
    @PutMapping("/audit/{id}/{status}")
    public AjaxResult audit(@PathVariable("id") Long id, @PathVariable("status") Integer status)
    {
        if (id == null || status == null || (status != 0 && status != 1))
        {
            return error("参数不合法");
        }
        BlogFriendLink link = new BlogFriendLink();
        link.setId(id);
        link.setStatus(status.toString());
        return toAjax(blogFriendLinkService.updateBlogFriendLink(link));
    }

    /**
     * 导出友情链接列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:export')")
    @Log(title = "友情链接", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BlogFriendLink blogFriendLink)
    {
        List<BlogFriendLink> list = blogFriendLinkService.selectBlogFriendLinkList(blogFriendLink);
        ExcelUtil<BlogFriendLink> util = new ExcelUtil<BlogFriendLink>(BlogFriendLink.class);
        util.exportExcel(response, list, "友情链接数据");
    }

    /**
     * 获取友情链接详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogFriendLinkService.selectBlogFriendLinkById(id));
    }

    /**
     * 新增友情链接
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:add')")
    @Log(title = "友情链接", businessType = BusinessType.INSERT)
    @BlogCacheEvict(value = CacheConstants.BLOG_FRIEND_LINK_LIST)
    @PostMapping
    public AjaxResult add(@RequestBody BlogFriendLink blogFriendLink)
    {
        return toAjax(blogFriendLinkService.insertBlogFriendLink(blogFriendLink));
    }

    /**
     * 修改友情链接
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:edit')")
    @Log(title = "友情链接", businessType = BusinessType.UPDATE)
    @BlogCacheEvict(value = CacheConstants.BLOG_FRIEND_LINK_LIST)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogFriendLink blogFriendLink)
    {
        return toAjax(blogFriendLinkService.updateBlogFriendLink(blogFriendLink));
    }

    /**
     * 删除友情链接
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:remove')")
    @Log(title = "友情链接", businessType = BusinessType.DELETE)
    @BlogCacheEvict(value = CacheConstants.BLOG_FRIEND_LINK_LIST)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogFriendLinkService.deleteBlogFriendLinkByIds(ids));
    }
}
