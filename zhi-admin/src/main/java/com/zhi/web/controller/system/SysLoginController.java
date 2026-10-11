package com.zhi.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.entity.SysMenu;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.service.ISysMenuService;

/**
 * 登录验证
 *
 * 注意：POST /login 与 GET /getInfo 已移除，
 * 统一登录请使用 /auth/login，用户信息请使用 /auth/user/info；
 * 登出走 Spring Security 内置的 POST /logout。
 *
 * @author ruoyi
 */
@RestController
public class SysLoginController
{
    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}
