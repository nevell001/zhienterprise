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
import com.zhi.common.cache.annotation.SiteCacheEvict;
import com.zhi.common.constant.CacheConstants;
import com.zhi.system.domain.FriendLink;
import com.zhi.system.service.IFriendLinkService;
import com.zhi.system.service.ISettingService;
import com.zhi.system.service.ICaptchaService;
import com.zhi.common.utils.SiteSwitchUtils;
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
public class FriendLinkController extends BaseController
{
    @Autowired
    private IFriendLinkService friendLinkService;

    @Autowired
    private ISettingService settingService;

    @Autowired
    private ICaptchaService captchaService;

    /**
     * 查询友情链接列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:list')")
    @GetMapping("/list")
    public TableDataInfo list(FriendLink friendLink)
    {
        startPage();
        List<FriendLink> list = friendLinkService.selectFriendLinkList(friendLink);
        return getDataTable(list);
    }

    /**
     * 查询前台展示的友情链接列表
     */
    @Anonymous
    @GetMapping("/front/list")
    public AjaxResult frontList()
    {
        List<FriendLink> list = friendLinkService.selectFrontFriendLinkList();
        return success(list);
    }

    /**
     * 前台申请友情链接（提交后进入待审核状态 status=2）
     */
    @Anonymous
    @RateLimiter(key = "zhi:friendLink:apply:", time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody FriendLink friendLink)
    {
        String name = friendLink.getName();
        String url = friendLink.getUrl();
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
        if (friendLink.getEmail() != null && friendLink.getEmail().length() > 100)
        {
            return error("邮箱长度不能超过100个字符");
        }

        friendLink.setId(null);
        friendLink.setName(name.trim());
        friendLink.setUrl(url.trim());
        if (friendLink.getDescription() != null && friendLink.getDescription().length() > 255)
        {
            friendLink.setDescription(friendLink.getDescription().substring(0, 255));
        }
        if (friendLink.getLogo() != null && friendLink.getLogo().length() > 255)
        {
            friendLink.setLogo(friendLink.getLogo().substring(0, 255));
        }
        // 前台是否开放友链申请（后台开关，默认开放）
        String applyEnabled = settingService.selectSettingValueByKey("friend_link_apply_enabled");
        if (SiteSwitchUtils.isOff(applyEnabled))
        {
            return error("本站暂未开放友链申请");
        }

        // 图形验证码：仅在验证码启用时校验（与登录/注册同一开关）
        captchaService.validate(friendLink.getCode(), friendLink.getUuid());

        friendLink.setStatus("2"); // 待审核
        friendLink.setDelFlag("0");
        if (friendLink.getSort() == null)
        {
            friendLink.setSort(0);
        }
        friendLink.setCreateBy("游客申请");
        return toAjax(friendLinkService.insertFriendLink(friendLink));
    }

    /**
     * 审核友情链接申请。
     *
     * <p>status 与 zhi_friend_link 全栈语义一致：<b>0=通过/正常（前台展示）</b>，
     * 1=拒绝/停用，2=待审核（提交申请时写入）。前台列表查询的是 status='0'，
     * 所以"通过"必须传 0、"拒绝"必须传 1。</p>
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:edit')")
    @Log(title = "友情链接", businessType = BusinessType.UPDATE)
    @SiteCacheEvict(value = CacheConstants.FRIEND_LINK_LIST)
    @PutMapping("/audit/{id}/{status}")
    public AjaxResult audit(@PathVariable("id") Long id, @PathVariable("status") Integer status)
    {
        if (id == null || status == null || (status != 0 && status != 1))
        {
            return error("参数不合法");
        }
        FriendLink link = new FriendLink();
        link.setId(id);
        link.setStatus(status.toString());
        return toAjax(friendLinkService.updateFriendLink(link));
    }

    /**
     * 导出友情链接列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:export')")
    @Log(title = "友情链接", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FriendLink friendLink)
    {
        List<FriendLink> list = friendLinkService.selectFriendLinkList(friendLink);
        ExcelUtil<FriendLink> util = new ExcelUtil<FriendLink>(FriendLink.class);
        util.exportExcel(response, list, "友情链接数据");
    }

    /**
     * 获取友情链接详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(friendLinkService.selectFriendLinkById(id));
    }

    /**
     * 新增友情链接
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:add')")
    @Log(title = "友情链接", businessType = BusinessType.INSERT)
    @SiteCacheEvict(value = CacheConstants.FRIEND_LINK_LIST)
    @PostMapping
    public AjaxResult add(@RequestBody FriendLink friendLink)
    {
        return toAjax(friendLinkService.insertFriendLink(friendLink));
    }

    /**
     * 修改友情链接
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:edit')")
    @Log(title = "友情链接", businessType = BusinessType.UPDATE)
    @SiteCacheEvict(value = CacheConstants.FRIEND_LINK_LIST)
    @PutMapping
    public AjaxResult edit(@RequestBody FriendLink friendLink)
    {
        return toAjax(friendLinkService.updateFriendLink(friendLink));
    }

    /**
     * 删除友情链接
     */
    @PreAuthorize("@ss.hasPermi('zhi:friendLink:remove')")
    @Log(title = "友情链接", businessType = BusinessType.DELETE)
    @SiteCacheEvict(value = CacheConstants.FRIEND_LINK_LIST)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(friendLinkService.deleteFriendLinkByIds(ids));
    }
}
