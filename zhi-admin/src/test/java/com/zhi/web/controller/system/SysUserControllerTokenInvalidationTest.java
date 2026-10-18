package com.zhi.web.controller.system;

import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.framework.web.service.TokenService;
import com.zhi.system.service.ISysDeptService;
import com.zhi.system.service.ISysPostService;
import com.zhi.system.service.ISysRoleService;
import com.zhi.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户管理中的会话失效：重置密码、禁用、删除、角色变化后，旧 token 必须立即作废。
 */
class SysUserControllerTokenInvalidationTest
{
    private SysUserController controller;
    private ISysUserService userService;
    private ISysRoleService roleService;
    private TokenService tokenService;

    @BeforeEach
    void setUp()
    {
        controller = new SysUserController();
        userService = mock(ISysUserService.class);
        roleService = mock(ISysRoleService.class);
        tokenService = mock(TokenService.class);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "roleService", roleService);
        ReflectionTestUtils.setField(controller, "deptService", mock(ISysDeptService.class));
        ReflectionTestUtils.setField(controller, "postService", mock(ISysPostService.class));
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);

        SysUser admin = new SysUser();
        admin.setUserId(1L);
        admin.setUserName("admin");
        LoginUser loginUser = new LoginUser(1L, 1L, admin, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resettingPasswordInvalidatesTargetUserSessions()
    {
        SysUser user = user(2L, "0");
        when(userService.resetPwd(any(SysUser.class))).thenReturn(1);

        assertThat(controller.resetPwd(user).isSuccess()).isTrue();

        verify(tokenService).deleteLoginUserByUserId(2L);
    }

    @Test
    void failedPasswordResetKeepsSessions()
    {
        SysUser user = user(2L, "0");
        when(userService.resetPwd(any(SysUser.class))).thenReturn(0);

        assertThat(controller.resetPwd(user).isSuccess()).isFalse();

        verify(tokenService, never()).deleteLoginUserByUserId(any());
    }

    @Test
    void disablingUserInvalidatesSessions()
    {
        SysUser user = user(2L, "1");
        when(userService.updateUserStatus(any(SysUser.class))).thenReturn(1);

        assertThat(controller.changeStatus(user).isSuccess()).isTrue();

        verify(tokenService).deleteLoginUserByUserId(2L);
    }

    @Test
    void enablingUserDoesNotInvalidateSessions()
    {
        SysUser user = user(2L, "0");
        when(userService.updateUserStatus(any(SysUser.class))).thenReturn(1);

        assertThat(controller.changeStatus(user).isSuccess()).isTrue();

        verify(tokenService, never()).deleteLoginUserByUserId(any());
    }

    @Test
    void deletingUsersInvalidatesEachDeletedUser()
    {
        when(userService.deleteUserByIds(any(Long[].class))).thenReturn(2);

        assertThat(controller.remove(new Long[] { 2L, 3L }).isSuccess()).isTrue();

        verify(tokenService).deleteLoginUserByUserId(2L);
        verify(tokenService).deleteLoginUserByUserId(3L);
    }

    @Test
    void grantingRolesInvalidatesTargetUserSessions()
    {
        controller.insertAuthRole(2L, new Long[] { 5L });

        verify(tokenService).deleteLoginUserByUserId(2L);
    }

    @Test
    void changingRolesInvalidatesSessions()
    {
        when(roleService.selectRoleListByUserId(2L)).thenReturn(List.of(5L));
        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.updateUser(any(SysUser.class))).thenReturn(1);
        SysUser user = user(2L, "0");
        user.setRoleIds(new Long[] { 5L, 6L });

        assertThat(controller.edit(user).isSuccess()).isTrue();

        verify(tokenService).deleteLoginUserByUserId(2L);
    }

    @Test
    void editingWithoutSecurityRelevantChangeKeepsSessions()
    {
        when(roleService.selectRoleListByUserId(2L)).thenReturn(List.of(5L));
        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.updateUser(any(SysUser.class))).thenReturn(1);
        SysUser user = user(2L, "0");
        user.setRoleIds(new Long[] { 5L });

        assertThat(controller.edit(user).isSuccess()).isTrue();

        verify(tokenService, never()).deleteLoginUserByUserId(any());
    }

    private SysUser user(Long userId, String status)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user-" + userId);
        user.setStatus(status);
        user.setPassword("NewPass1!");
        return user;
    }
}
