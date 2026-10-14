package com.zhi.framework.web.service;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.common.exception.ServiceException;
import com.zhi.common.utils.MessageUtils;
import com.zhi.framework.testsupport.SpringContextStub;
import com.zhi.system.service.ISysUserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录用户加载单元测试。
 *
 * <p>这是 Spring Security 认证链的入口：账号不存在 / 已删除 / 已停用必须在查库之后、验密之前被拦下，
 * 只有全部通过才 `passwordService.validate` 验密并装配 {@code LoginUser}。</p>
 */
class UserDetailsServiceImplTest
{
    private final UserDetailsServiceImpl userDetailsService = new UserDetailsServiceImpl();

    private ISysUserService userService;

    private SysPasswordService passwordService;

    private SysPermissionService permissionService;

    @BeforeEach
    void setUp()
    {
        SpringContextStub.install();
        userService = Mockito.mock(ISysUserService.class);
        passwordService = Mockito.mock(SysPasswordService.class);
        permissionService = Mockito.mock(SysPermissionService.class);
        ReflectionTestUtils.setField(userDetailsService, "userService", userService);
        ReflectionTestUtils.setField(userDetailsService, "passwordService", passwordService);
        ReflectionTestUtils.setField(userDetailsService, "permissionService", permissionService);
    }

    private static SysUser activeUser()
    {
        SysUser user = new SysUser();
        user.setUserId(2L);
        user.setDeptId(103L);
        user.setUserName("zhangsan");
        user.setPassword("$2a$10$hashed");
        user.setDelFlag("0");
        user.setStatus("0");
        return user;
    }

    @Test
    void unknownAccountIsRejectedWithTheNotExistsMessage() throws UsernameNotFoundException
    {
        when(userService.selectUserByUserName("ghost")).thenReturn(null);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(ServiceException.class)
                .hasMessage(MessageUtils.message("user.not.exists"));
        verify(passwordService, never()).validate(Mockito.any());
    }

    @Test
    void deletedAccountIsRejectedBeforeThePasswordIsChecked()
    {
        SysUser deleted = activeUser();
        deleted.setDelFlag("2");
        when(userService.selectUserByUserName("zhangsan")).thenReturn(deleted);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("zhangsan"))
                .isInstanceOf(ServiceException.class)
                .hasMessage(MessageUtils.message("user.password.delete"));
        verify(passwordService, never()).validate(Mockito.any());
    }

    @Test
    void disabledAccountIsRejectedBeforeThePasswordIsChecked()
    {
        SysUser disabled = activeUser();
        disabled.setStatus("1");
        when(userService.selectUserByUserName("zhangsan")).thenReturn(disabled);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("zhangsan"))
                .isInstanceOf(ServiceException.class)
                .hasMessage(MessageUtils.message("user.blocked"));
        verify(passwordService, never()).validate(Mockito.any());
    }

    @Test
    void lockedAccountNeverGetsAUserDetails() throws UsernameNotFoundException
    {
        when(userService.selectUserByUserName("zhangsan")).thenReturn(activeUser());
        Mockito.doThrow(new ServiceException("密码输入错误5次，帐户锁定10分钟")).when(passwordService)
                .validate(Mockito.any());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("zhangsan"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("帐户锁定");
        verify(permissionService, never()).getMenuPermission(Mockito.any());
    }

    @Test
    void activeAccountIsVerifiedAndReturnedAsLoginUser() throws UsernameNotFoundException
    {
        SysUser user = activeUser();
        Set<String> perms = new HashSet<>(Set.of("zhi:article:list"));
        when(userService.selectUserByUserName("zhangsan")).thenReturn(user);
        when(permissionService.getMenuPermission(user)).thenReturn(perms);

        UserDetails details = userDetailsService.loadUserByUsername("zhangsan");

        verify(passwordService).validate(user);
        assertThat(details).isInstanceOf(LoginUser.class);
        LoginUser loginUser = (LoginUser) details;
        assertThat(loginUser.getUserId()).isEqualTo(2L);
        assertThat(loginUser.getDeptId()).isEqualTo(103L);
        assertThat(loginUser.getUsername()).isEqualTo("zhangsan");
        assertThat(loginUser.getPermissions()).containsExactly("zhi:article:list");
    }
}