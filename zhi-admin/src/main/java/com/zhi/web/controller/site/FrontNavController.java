package com.zhi.web.controller.site;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.system.domain.SiteNavItem;
import com.zhi.system.service.ISiteNavItemService;

/**
 * 站点导航（前台公开）：企业官网头部/页脚菜单，后台「导航管理」维护
 *
 * @author zhi
 */
@RestController
public class FrontNavController extends BaseController
{
    @Autowired
    private ISiteNavItemService navItemService;

    /**
     * 导航树（仅可见项）
     */
    @Anonymous
    @GetMapping("/api/nav")
    public AjaxResult nav(@RequestParam(value = "location", defaultValue = "header") String location)
    {
        List<SiteNavItem> tree = navItemService.selectVisibleNavTree(location);
        return success(tree);
    }
}
