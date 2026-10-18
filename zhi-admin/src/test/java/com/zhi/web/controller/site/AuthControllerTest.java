package com.zhi.web.controller.site;

import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.framework.web.service.SiteUserService;
import com.zhi.framework.web.service.TokenService;
import com.zhi.system.mapper.SysUserMapper;
import com.zhi.system.service.IEmailService;
import com.zhi.system.service.ISysConfigService;
import com.zhi.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 前台找回密码：重置成功后必须让该账号的既有会话失效。
 */
class AuthControllerTest
{
    private AuthController controller;
    private IEmailService emailService;
    private ISysUserService userService;
    private SysUserMapper userMapper;
    private TokenService tokenService;

    @BeforeEach
    void setUp()
    {
        controller = new AuthController();
        emailService = mock(IEmailService.class);
        userService = mock(ISysUserService.class);
        userMapper = mock(SysUserMapper.class);
        tokenService = mock(TokenService.class);
        ReflectionTestUtils.setField(controller, "siteUserService", mock(SiteUserService.class));
        ReflectionTestUtils.setField(controller, "emailService", emailService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "configService", mock(ISysConfigService.class));
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
    }

    @Test
    void resetPasswordInvalidatesExistingSessions()
    {
        SysUser user = new SysUser();
        user.setUserId(9L);
        when(emailService.verifyCode("owner@example.com", "123456", IEmailService.CODE_TYPE_RESET))
                .thenReturn(true);
        when(userMapper.checkEmailUnique("owner@example.com")).thenReturn(user);
        when(userService.resetUserPwd(eq(9L), anyString())).thenReturn(1);

        AjaxResult result = controller.resetPassword(Map.of(
                "email", "owner@example.com",
                "code", "123456",
                "newPassword", "NewPass1!",
                "confirmPassword", "NewPass1!"));

        assertThat(result.isSuccess()).isTrue();
        verify(tokenService).deleteLoginUserByUserId(9L);
    }

    @Test
    void failedResetKeepsSessions()
    {
        SysUser user = new SysUser();
        user.setUserId(9L);
        when(emailService.verifyCode("owner@example.com", "123456", IEmailService.CODE_TYPE_RESET))
                .thenReturn(true);
        when(userMapper.checkEmailUnique("owner@example.com")).thenReturn(user);
        when(userService.resetUserPwd(eq(9L), anyString())).thenReturn(0);

        AjaxResult result = controller.resetPassword(Map.of(
                "email", "owner@example.com",
                "code", "123456",
                "newPassword", "NewPass1!",
                "confirmPassword", "NewPass1!"));

        assertThat(result.isSuccess()).isFalse();
        verify(tokenService, never()).deleteLoginUserByUserId(9L);
    }
}
