package com.zhi.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * SPA路由控制器
 * 处理前端SPA应用的路由请求，支持history模式
 * @author ruoyi
 */
@Controller
public class SpaRouterController {

    /**
     * 捕获站点前台页面路由（短路径，与 zhi-ui/src/router/site.ts 一致），重定向到index.html
     * 这样前端路由可以处理这些请求
     *
     * 只放行 GET/HEAD：/register 等路径同时承担业务接口（POST），不能一并吞掉
     */
    @RequestMapping(value = {
        "/login",
        "/register",
        "/forgot-password",
        "/news",
        "/article/**",
        "/business",
        "/business/**",
        "/solutions",
        "/solutions/**",
        "/contact",
        "/about",
        "/honors",
        "/history",
        "/category",
        "/category/**",
        "/tag",
        "/tag/**",
        "/guestbook",
        "/page/**",
        "/friend-links/**",
        "/agreement",
        "/privacy",
        "/search"
    }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public String spaFrontRouter() {
        return "forward:/index.html";
    }

    /**
     * 捕获其余未匹配的页面路由，重定向到index.html
     * 确保前端可以正确处理所有路由
     */
    @RequestMapping({
        "/index",
        "/404"
    })
    public String spaIndexRouter() {
        return "forward:/index.html";
    }
}
