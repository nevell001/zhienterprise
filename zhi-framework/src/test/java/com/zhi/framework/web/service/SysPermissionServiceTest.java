package com.zhi.framework.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.zhi.common.core.domain.entity.SysRole;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.system.service.ISysMenuService;
import com.zhi.system.service.ISysRoleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 权限聚合单元测试。
 *
 * <p>登录时这里算出的角色/菜单权限会写进 {@code LoginUser}，是 {@code @ss.hasPermi} 的唯一数据来源，
 * 也是 {@code DataScopeAspect} 判断"这个角色是否管这条权限"的依据（所以每个角色的 permissions 必须回填到角色对象上）。</p>
 */
class SysPermissionServiceTest
{
    private final SysPermissionService permissionService = new SysPermissionService();

    private ISysRoleService roleService;

    private ISysMenuService menuService;

    @BeforeEach
    void setUp()
    {
        roleService = Mockito.mock(ISysRoleService.class);
        menuService = Mockito.mock(ISysMenuService.class);
        ReflectionTestUtils.setField(permissionService, "roleService", roleService);
        ReflectionTestUtils.setField(permissionService, "menuService", menuService);
    }

    private static SysUser user(long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        return user;
    }

    private static SysRole role(long roleId, String roleKey, String status)
    {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setRoleKey(roleKey);
        role.setStatus(status);
        return role;
    }

    @Test
    void adminGetsTheWildcardsWithoutQueryingTheDatabase()
    {
        Set<String> roles = permissionService.getRolePermission(user(1L));
        Set<String> perms = permissionService.getMenuPermission(user(1L));

        assertThat(roles).containsExactly("admin");
        assertThat(perms).containsExactly("*:*:*");
        verify(roleService, never()).selectRolePermissionByUserId(Mockito.anyLong());
        verify(menuService, never()).selectMenuPermsByUserId(Mockito.anyLong());
        verify(menuService, never()).selectMenuPermsByRoleId(Mockito.anyLong());
    }

    @Test
    void normalUserRolesComeFromTheRoleService()
    {
        when(roleService.selectRolePermissionByUserId(2L)).thenReturn(new HashSet<>(List.of("common")));

        assertThat(permissionService.getRolePermission(user(2L))).containsExactly("common");
    }

    @Test
    void perRoleMenuPermissionsAreCollectedAndWrittenBackOntoEachRole()
    {
        SysRole normal = role(100L, "editor", "0");
        SysRole other = role(101L, "viewer", "0");
        SysUser user = user(2L);
        user.setRoles(new ArrayList<>(List.of(normal, other)));
        when(menuService.selectMenuPermsByRoleId(100L)).thenReturn(new HashSet<>(List.of("zhi:article:add")));
        when(menuService.selectMenuPermsByRoleId(101L)).thenReturn(new HashSet<>(List.of("zhi:article:list")));

        Set<String> perms = permissionService.getMenuPermission(user);

        assertThat(perms).containsExactlyInAnyOrder("zhi:article:add", "zhi:article:list");
        // 回填是为了让数据权限切面能按"角色里有这条权限字符"来筛角色
        assertThat(normal.getPermissions()).containsExactly("zhi:article:add");
        assertThat(other.getPermissions()).containsExactly("zhi:article:list");
        verify(menuService, never()).selectMenuPermsByUserId(Mockito.anyLong());
    }

    @Test
    void disabledAndAdminRolesContributNothing()
    {
        SysRole disabled = role(100L, "editor", "1");
        SysRole adminRole = role(1L, "admin", "0");
        SysUser user = user(2L);
        user.setRoles(new ArrayList<>(List.of(disabled, adminRole)));

        assertThat(permissionService.getMenuPermission(user)).isEmpty();
        verify(menuService, never()).selectMenuPermsByRoleId(Mockito.anyLong());
    }

    @Test
    void userWithoutRolesFallsBackToTheUserIdQuery()
    {
        SysUser user = user(2L);
        user.setRoles(new ArrayList<>());
        when(menuService.selectMenuPermsByUserId(2L)).thenReturn(new HashSet<>(List.of("zhi:article:query")));

        assertThat(permissionService.getMenuPermission(user)).containsExactly("zhi:article:query");
    }

    @Test
    void emptyRoleListIsTreatedAsNoRoles()
    {
        SysUser user = user(2L);
        user.setRoles(Collections.emptyList());
        when(menuService.selectMenuPermsByUserId(2L)).thenReturn(new HashSet<>());

        assertThat(permissionService.getMenuPermission(user)).isEmpty();
        verify(menuService).selectMenuPermsByUserId(2L);
    }
}