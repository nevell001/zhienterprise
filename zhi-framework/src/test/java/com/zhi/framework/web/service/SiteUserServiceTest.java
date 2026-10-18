package com.zhi.framework.web.service;

import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.model.SiteRegisterBody;
import com.zhi.common.utils.PasswordPolicy;
import com.zhi.system.mapper.SysRoleMapper;
import com.zhi.system.service.IEmailService;
import com.zhi.system.service.ISysConfigService;
import com.zhi.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 前台注册的口令策略：注册与找回密码必须共用 {@link PasswordPolicy}。
 *
 * <p>DTO 上的 {@code @Size(min = 5, max = 20)} 只约束长度，拦不住
 * {@code aaaaaa} / 含空白的弱口令；这里锁定服务层行为。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SiteUserServiceTest
{
    @Mock
    private ISysUserService userService;

    @Mock
    private ISysConfigService configService;

    @Mock
    private UnifiedCacheManager unifiedCacheManager;

    @Mock
    private IEmailService emailService;

    @Mock
    private SysRoleMapper roleMapper;

    private SiteUserService siteUserService;

    @BeforeEach
    void setUp()
    {
        siteUserService = new SiteUserService();
        ReflectionTestUtils.setField(siteUserService, "userService", userService);
        ReflectionTestUtils.setField(siteUserService, "configService", configService);
        ReflectionTestUtils.setField(siteUserService, "unifiedCacheManager", unifiedCacheManager);
        ReflectionTestUtils.setField(siteUserService, "emailService", emailService);
        ReflectionTestUtils.setField(siteUserService, "roleMapper", roleMapper);

        when(configService.selectCaptchaEnabled()).thenReturn(false);
        when(emailService.verifyCode(anyString(), anyString(), anyString())).thenReturn(true);
        when(userService.checkUserNameUnique(any())).thenReturn(true);
        when(userService.checkEmailUnique(any())).thenReturn(true);
    }

    private SiteRegisterBody registerBody(String password)
    {
        SiteRegisterBody body = new SiteRegisterBody();
        body.setUsername("zhangsan");
        body.setPassword(password);
        body.setConfirmPassword(password);
        body.setEmail("zhangsan@example.com");
        body.setEmailCode("123456");
        return body;
    }

    @Test
    void rejectsRepeatedSingleCharacterPassword()
    {
        AjaxResult result = siteUserService.registerUser(registerBody("aaaaaaaa"));

        assertThat(result.isError()).isTrue();
        assertThat(result.get(AjaxResult.MSG_TAG)).isEqualTo("密码不能是重复的单一字符");
        verify(userService, never()).registerUser(any());
    }

    @Test
    void rejectsPasswordContainingWhitespace()
    {
        AjaxResult result = siteUserService.registerUser(registerBody("abcd 1234"));

        assertThat(result.isError()).isTrue();
        assertThat(result.get(AjaxResult.MSG_TAG)).isEqualTo("密码不能包含空白字符");
        verify(userService, never()).registerUser(any());
    }

    @Test
    void rejectsPasswordOutsideLengthPolicy()
    {
        AjaxResult result = siteUserService.registerUser(registerBody("abc"));

        assertThat(result.isError()).isTrue();
        assertThat(String.valueOf(result.get(AjaxResult.MSG_TAG)))
                .contains(PasswordPolicy.MIN_LENGTH + "和" + PasswordPolicy.MAX_LENGTH);
        verify(userService, never()).registerUser(any());
    }
}
