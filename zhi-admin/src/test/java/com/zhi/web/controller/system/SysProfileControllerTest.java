package com.zhi.web.controller.system;

import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.constant.CacheConstants;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.framework.web.service.TokenService;
import com.zhi.system.domain.SysConfig;
import com.zhi.system.service.ISettingService;
import com.zhi.system.service.ISysConfigService;
import com.zhi.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 个人信息控制器：头像全局同步范围与改密后的会话失效。
 */
class SysProfileControllerTest
{
    private static final String AVATAR = "/profile/avatar/2026/10/18/avatar.png";

    private SysProfileController controller;
    private ISysUserService userService;
    private TokenService tokenService;
    private ISettingService settingService;
    private ISysConfigService configService;
    private UnifiedCacheManager cacheManager;

    @BeforeEach
    void setUp()
    {
        controller = new SysProfileController();
        userService = mock(ISysUserService.class);
        tokenService = mock(TokenService.class);
        settingService = mock(ISettingService.class);
        configService = mock(ISysConfigService.class);
        cacheManager = mock(UnifiedCacheManager.class);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        ReflectionTestUtils.setField(controller, "settingService", settingService);
        ReflectionTestUtils.setField(controller, "configService", configService);
        ReflectionTestUtils.setField(controller, "unifiedCacheManager", cacheManager);
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registeredUserAvatarDoesNotOverwriteGlobalBrandAvatar()
    {
        LoginUser loginUser = loginUser(2L, "01");

        boolean synced = controller.syncGlobalAvatarIfSystemUser(loginUser, AVATAR);

        assertThat(synced).isFalse();
        verifyNoInteractions(settingService, configService, cacheManager);
    }

    @Test
    void systemUserAvatarStillSyncsGlobalBrandAvatar()
    {
        LoginUser loginUser = loginUser(1L, "00");
        when(configService.selectConfigList(any(SysConfig.class))).thenReturn(Collections.emptyList());

        boolean synced = controller.syncGlobalAvatarIfSystemUser(loginUser, AVATAR);

        assertThat(synced).isTrue();
        verify(settingService).updateSettingValueByKey("blog_avatar", AVATAR);
        ArgumentCaptor<SysConfig> configCaptor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configService).insertConfig(configCaptor.capture());
        assertThat(configCaptor.getValue().getConfigKey()).isEqualTo("blog_avatar");
        assertThat(configCaptor.getValue().getConfigValue()).isEqualTo(AVATAR);
        verify(cacheManager).delete(CacheConstants.SETTINGS_ALL);
    }

    @Test
    void changingPasswordInvalidatesEverySessionOfThatUser()
    {
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setPassword(SecurityUtils.encryptPassword("OldPass1!"));
        loginUser(user);
        when(userService.resetUserPwd(eq(7L), anyString())).thenReturn(1);

        AjaxResult result = controller.updatePwd(Map.of(
                "oldPassword", "OldPass1!",
                "newPassword", "NewPass1!"));

        assertThat(result.isSuccess()).isTrue();
        verify(tokenService).deleteLoginUserByUserId(7L);
        verify(tokenService, never()).setLoginUser(any());
    }

    @Test
    void failedPasswordChangeKeepsSessions()
    {
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setPassword(SecurityUtils.encryptPassword("OldPass1!"));
        loginUser(user);
        when(userService.resetUserPwd(eq(7L), anyString())).thenReturn(0);

        AjaxResult result = controller.updatePwd(Map.of(
                "oldPassword", "OldPass1!",
                "newPassword", "NewPass1!"));

        assertThat(result.isSuccess()).isFalse();
        verify(tokenService, never()).deleteLoginUserByUserId(any());
    }

    private LoginUser loginUser(Long userId, String userType)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user-" + userId);
        user.setUserType(userType);
        LoginUser loginUser = new LoginUser(userId, 1L, user, Collections.emptySet());
        loginUser(loginUser);
        return loginUser;
    }

    private void loginUser(SysUser user)
    {
        loginUser(new LoginUser(user.getUserId(), 1L, user, Collections.emptySet()));
    }

    private void loginUser(LoginUser loginUser)
    {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
