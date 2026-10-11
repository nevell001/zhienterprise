package com.zhi.web.controller.auth;

import com.zhi.common.annotation.RateLimiter;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginBody;
import com.zhi.common.enums.LimitType;
import com.zhi.common.utils.StringUtils;
import com.zhi.framework.web.service.SysLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一认证控制器
 * 处理管理员和注册用户的统一登录
 *
 * @author nevell
 * @date 2025-01-26
 */
@RestController
@RequestMapping("/auth")
public class UnifiedAuthController extends BaseController
{
    @Autowired
    private SysLoginService loginService;

    /**
     * 统一登录接口
     * 支持管理员和注册用户登录
     */
    @RateLimiter(key = "auth:login:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        String username = loginBody.getUsername();
        String password = loginBody.getPassword();

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            return AjaxResult.error("用户名和密码不能为空");
        }

        try
        {
            // 调用登录服务进行验证
            String token = loginService.login(username, password, loginBody.getCode(), loginBody.getUuid(), loginBody.getRememberMe());

            logger.info("用户登录成功：username={}", username);

            // 返回token，前端根据获取的用户信息判断用户类型
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);

            return AjaxResult.success("登录成功", result);
        }
        catch (Exception e)
        {
            logger.error("登录失败：username={}, error={}", username, e.getMessage());
            return AjaxResult.error("登录失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息（统一接口）
     */
    @GetMapping("/user/info")
    public AjaxResult getUserInfo()
    {
        try
        {
            com.zhi.common.core.domain.model.LoginUser loginUser = getLoginUser();
            if (loginUser == null || loginUser.getUser() == null)
            {
                // 返回 401 让前端以匿名身份访问站点前台
                return AjaxResult.error(401, "未登录或会话已过期");
            }
            SysUser user = loginUser.getUser();
            logger.info("📋 获取用户信息 - userId: {}, userName: {}, userType: {}",
                user.getUserId(), user.getUserName(), user.getUserType());

            // 提取角色标识列表
            java.util.List<String> roles = new java.util.ArrayList<>();
            if (user.getRoles() != null && !user.getRoles().isEmpty())
            {
                for (com.zhi.common.core.domain.entity.SysRole role : user.getRoles())
                {
                    roles.add(role.getRoleKey());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("user", user);
            result.put("roles", roles);
            result.put("permissions", loginUser.getPermissions());
            result.put("userId", user.getUserId());
            result.put("userName", user.getUserName());
            result.put("nickName", user.getNickName());
            result.put("avatar", user.getAvatar());
            result.put("email", user.getEmail());
            result.put("userType", user.getUserType());

            logger.info("📊 返回用户信息 - userType: {}", result.get("userType"));
            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            logger.warn("获取用户信息失败（可能为匿名访问）：error={}", e.getMessage());
            // 返回 401 而非 500，前端拦截器会对站点页面静默处理
            return AjaxResult.error(401, "未登录或会话已过期");
        }
    }

}
