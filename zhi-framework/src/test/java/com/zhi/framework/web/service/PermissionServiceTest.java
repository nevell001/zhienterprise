package com.zhi.framework.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.zhi.common.constant.HttpStatus;
import com.zhi.common.core.domain.entity.SysRole;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.common.exception.ServiceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 自定义权限 bean（@ss）单元测试。
 *
 * <p>后台每个写接口的 {@code @PreAuthorize("@ss.hasPermi(...)")} 都走这里，
 * 所以"空权限串拒绝""超管通配放行""角色缺失拒绝"必须是可回归的行为断言。</p>
 */
class PermissionServiceTest
{
    private final PermissionService permissionService = new PermissionService();

    @BeforeEach
    void bindRequest()
    {
        // hasPermi 会把权限串写进请求作用域，供审计日志回读
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearContext()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void login(Set<String> permissions, SysRole... roles)
    {
        SysUser user = new SysUser();
        user.setUserName("zhangsan");
        ArrayList<SysRole> roleList = new ArrayList<>();
        Collections.addAll(roleList, roles);
        user.setRoles(roleList);
        LoginUser loginUser = new LoginUser(1L, 2L, user, permissions);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, "n/a"));
    }

    private static SysRole role(String key)
    {
        SysRole sysRole = new SysRole();
        sysRole.setRoleKey(key);
        return sysRole;
    }

    @Test
    void blankPermissionIsRejectedWithoutConsultingTheUser()
    {
        assertThat(permissionService.hasPermi("")).isFalse();
        assertThat(permissionService.hasPermi(null)).isFalse();
        assertThat(permissionService.hasAnyPermi("")).isFalse();
        assertThat(permissionService.hasRole("")).isFalse();
        assertThat(permissionService.hasAnyRoles(null)).isFalse();
    }

    @Test
    void anonymousCallerIsRejectedWithAnUnauthorizedBusinessException()
    {
        // 没有 authentication 时 SecurityUtils.getLoginUser() 不会返回 null，而是抛 401 业务异常，
        // 由全局异常处理器转成响应；这里锁住这条真实拒绝路径，避免日后被"改成返回 false"弱化。
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThatThrownBy(() -> permissionService.hasPermi("zhi:article:edit"))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> permissionService.hasRole("admin"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void userWithoutAnyPermissionIsDenied()
    {
        login(Collections.emptySet());

        assertThat(permissionService.hasPermi("zhi:article:edit")).isFalse();
        assertThat(permissionService.lacksPermi("zhi:article:edit")).isTrue();
    }

    @Test
    void exactPermissionGrantsAndTrimsTheComparedValue()
    {
        login(new HashSet<>(Collections.singletonList("zhi:article:edit")));

        assertThat(permissionService.hasPermi("zhi:article:edit")).isTrue();
        assertThat(permissionService.hasPermi("  zhi:article:edit  ")).isTrue();
        assertThat(permissionService.hasPermi("zhi:article:remove")).isFalse();
    }

    @Test
    void superAdminWildcardGrantsEverything()
    {
        login(new HashSet<>(Collections.singletonList("*:*:*")));

        assertThat(permissionService.hasPermi("system:user:remove")).isTrue();
        assertThat(permissionService.hasAnyPermi("system:user:remove,zhi:comment:audit")).isTrue();
    }

    @Test
    void anyPermiMatchesTheSecondEntryAndDeniesWhenNoneMatch()
    {
        login(new HashSet<>(Collections.singletonList("zhi:comment:audit")));

        assertThat(permissionService.hasAnyPermi("system:user:remove,zhi:comment:audit")).isTrue();
        assertThat(permissionService.hasAnyPermi("system:user:remove,zhi:comment:delete")).isFalse();
    }

    @Test
    void adminRoleBypassesTheRoleKeyComparison()
    {
        login(Collections.emptySet(), role("admin"));

        assertThat(permissionService.hasRole("zhi:editor")).isTrue();
        assertThat(permissionService.lacksRole("zhi:editor")).isFalse();
    }

    @Test
    void ordinaryRoleMustMatchExactlyAndNeverGrantsOtherRoles()
    {
        login(Collections.emptySet(), role("blog_writer"));

        assertThat(permissionService.hasRole("blog_writer")).isTrue();
        assertThat(permissionService.hasRole("blog_writer ")).isTrue();
        assertThat(permissionService.hasRole("blog_editor")).isFalse();
        assertThat(permissionService.hasAnyRoles("blog_editor,blog_writer")).isTrue();
        assertThat(permissionService.hasAnyRoles("blog_editor,reviewer")).isFalse();
    }

    @Test
    void userWithoutRolesHasNoRole()
    {
        login(Collections.emptySet());

        assertThat(permissionService.hasRole("admin")).isFalse();
        assertThat(permissionService.lacksRole("admin")).isTrue();
    }
}
