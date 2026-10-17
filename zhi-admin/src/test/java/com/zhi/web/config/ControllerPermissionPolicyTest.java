package com.zhi.web.config;

import com.zhi.common.annotation.Anonymous;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理接口权限守卫。
 *
 * <p>本项目的鉴权模型是：Spring Security 只兜底「登录即可」（anyRequest().authenticated()），
 * 细粒度权限完全依赖方法上的 {@code @PreAuthorize}。因此漏写注解 = 任何已登录账号
 * （包括自助注册的零权限 site_user）都能调用 —— 一次审计就发现
 * {@code ArticleTagController} 两个查询、{@code TagController#getAllTags}、
 * {@code SysConfigController#getConfigKey} 就是这样漏的。</p>
 *
 * <p>规则：每个映射方法必须满足其一 —— ① 带 {@code @PreAuthorize}；② 带 {@code @Anonymous}
 * （公开接口）；③ 在下面的白名单里（SecurityConfig 按路径放行，或"登录即可"的自助接口）。
 * 白名单要求写明理由，新增条目必须同样说明，避免把漏注解的接口悄悄塞进来。</p>
 */
class ControllerPermissionPolicyTest
{
    /** 需要扫描的 controller 包（编译产物，zhi-system 与 zhi-admin 都在类路径上） */
    private static final List<String> CONTROLLER_PACKAGES = List.of(
        "com/zhi/system/controller",
        "com/zhi/web/controller"
    );

    /**
     * 允许「既没有 @PreAuthorize 也没有 @Anonymous」的方法。
     *
     * <p>只放两类：SecurityConfig 里按路径 permitAll 的登录/登出类接口，以及
     * 明确设计为「任何登录用户可用」的自助接口（个人信息、通知等）。</p>
     */
    private static final Set<String> AUTHENTICATED_ONLY_ALLOWLIST = new LinkedHashSet<>(List.of(
        // ── SecurityConfig 按路径 permitAll ──────────────────────────────────
        "UnifiedAuthController#login",          // /auth/login（带 @RateLimiter）
        "UnifiedAuthController#logout",         // /logout
        "UnifiedAuthController#getUserInfo",    // 当前登录用户信息（任何登录用户）
        "CaptchaController#getCode",            // /captchaImage
        "SysIndexController#index",             // 站点首页探活
        "SysRegisterController#register",       // 自助注册（受 sys.account.registerUser 开关控制）
        "SpaRouterController#spaFrontRouter",   // 前台 SPA 路由回落（短路径）
        "SpaRouterController#spaIndexRouter",
        "AdminRouterController#spaAdminRouter", // 后台 SPA 路由回落

        // ── 设计为「任何登录用户可用」的自助接口（服务端按当前 userId 过滤）──
        "SysProfileController#",                // 个人中心：资料/头像/改密
        "LikeController#",                  // 点赞状态与切换
        "NotificationController#",          // 站内通知
        "ArticleController#getMyArticles",  // 我的文章
        "FrontController#updateMyComment",  // 改自己评论：登录即可，服务端按当前 userId 校验归属
        "FrontController#deleteMyComment",  // 删自己评论：登录即可，服务端按当前 userId 校验归属

        // ── RuoYi 约定的「登录即可读」的下拉/树数据 ─────────────────────────
        "SysLoginController#getRouters",        // 当前用户可访问菜单
        "SysMenuController#treeselect",         // 菜单树（供角色配置）
        "SysMenuController#roleMenuTreeselect",
        "SysDictDataController#dictType",       // 字典数据
        "SysDictTypeController#optionselect",   // 字典类型下拉
        "SysPostController#optionselect",       // 岗位下拉

        // ── 上传/下载：登录即可，另有 @RateLimiter 限流 ─────────────────────
        "CommonController#"                     // 上传、下载、头像、封面等
    ));

    /** 白名单条目以 # 结尾表示整类放行 */
    private static boolean allowlisted(Class<?> controller, Method method)
    {
        return AUTHENTICATED_ONLY_ALLOWLIST.contains(controller.getSimpleName() + "#" + method.getName())
            || AUTHENTICATED_ONLY_ALLOWLIST.contains(controller.getSimpleName() + "#");
    }

    @Test
    @DisplayName("每个映射方法都必须带 @PreAuthorize 或 @Anonymous，或在白名单中")
    void everyMappingMethodIsGuarded() throws Exception
    {
        List<String> violations = new ArrayList<>();
        for (String pkg : CONTROLLER_PACKAGES)
        {
            for (Class<?> controller : scanControllers(pkg))
            {
                boolean classPublic = AnnotatedElementUtils.hasAnnotation(controller, Anonymous.class);
                for (Method method : controller.getDeclaredMethods())
                {
                    if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) == null)
                    {
                        continue;
                    }
                    boolean guarded = AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)
                        || AnnotatedElementUtils.hasAnnotation(method, Anonymous.class)
                        || classPublic
                        || allowlisted(controller, method);
                    if (!guarded)
                    {
                        violations.add(controller.getSimpleName() + "#" + method.getName());
                    }
                }
            }
        }

        violations.sort(String::compareTo);
        assertTrue(violations.isEmpty(),
            "以下接口既没有 @PreAuthorize 也没有 @Anonymous（任何登录用户都能调用）：\n  "
                + String.join("\n  ", violations)
                + "\n请补权限注解，或在 ControllerPermissionPolicyTest 的白名单里说明理由。");
    }

    private List<Class<?>> scanControllers(String packagePath) throws IOException, ClassNotFoundException
    {
        List<Class<?>> controllers = new ArrayList<>();
        Set<String> classNames = new LinkedHashSet<>();
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
        while (resources.hasMoreElements())
        {
            URL url = resources.nextElement();
            // 类路径上同时存在 target/classes 与 target/test-classes，后者是测试类，跳过
            if (url.getPath().contains("test-classes"))
            {
                continue;
            }
            if ("file".equals(url.getProtocol()))
            {
                Path root = Path.of(url.getPath());
                try (Stream<Path> walk = Files.walk(root))
                {
                    // 用相对路径推导全限定名，子包（如 com/zhi/web/controller/site）才不会丢包名
                    walk.filter(path -> path.toString().endsWith(".class"))
                        .map(root::relativize)
                        .forEach(relative -> classNames.add(packagePath.replace('/', '.') + "."
                            + relative.toString().replace(java.io.File.separatorChar, '.').replace(".class", "")));
                }
            }
            else if ("jar".equals(url.getProtocol()))
            {
                if (url.getPath().contains("test-classes"))
                {
                    continue;
                }
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                try (JarFile jar = connection.getJarFile())
                {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements())
                    {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(packagePath) && name.endsWith(".class"))
                        {
                            classNames.add(name.replace('/', '.').replace(".class", ""));
                        }
                    }
                }
            }
        }

        for (String className : classNames)
        {
            if (className.contains("$"))
            {
                continue;
            }
            if (!className.endsWith("Controller"))
            {
                continue;
            }
            Class<?> clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            if (clazz.isInterface() || clazz.isEnum() || clazz.isAnnotation())
            {
                continue;
            }
            if (AnnotatedElementUtils.hasAnnotation(clazz, RestController.class)
                || AnnotatedElementUtils.hasAnnotation(clazz, Controller.class))
            {
                controllers.add(clazz);
            }
        }
        return controllers;
    }

}
