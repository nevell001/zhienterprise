package com.zhi.framework.web.service;

import com.zhi.common.core.domain.model.RegisterBody;
import com.zhi.common.utils.PasswordPolicy;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 旧版注册入口（/register）同样必须走 {@link PasswordPolicy}，否则前台两条注册路径策略不一致。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysRegisterServiceTest
{
    @Mock
    private ISysUserService userService;

    @Mock
    private ISysConfigService configService;

    private SysRegisterService registerService;

    @BeforeEach
    void setUp()
    {
        registerService = new SysRegisterService();
        ReflectionTestUtils.setField(registerService, "userService", userService);
        ReflectionTestUtils.setField(registerService, "configService", configService);

        when(configService.selectCaptchaEnabled()).thenReturn(false);
        when(userService.checkUserNameUnique(any())).thenReturn(true);
    }

    private RegisterBody registerBody(String password)
    {
        RegisterBody body = new RegisterBody();
        body.setUsername("zhangsan");
        body.setPassword(password);
        return body;
    }

    @Test
    void rejectsRepeatedSingleCharacterPassword()
    {
        String message = registerService.register(registerBody("aaaaaaaa"));

        assertThat(message).isEqualTo("密码不能是重复的单一字符");
        verify(userService, never()).registerUser(any());
    }

    @Test
    void rejectsPasswordContainingWhitespace()
    {
        String message = registerService.register(registerBody("abcd 1234"));

        assertThat(message).isEqualTo("密码不能包含空白字符");
        verify(userService, never()).registerUser(any());
    }
}
