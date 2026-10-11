package com.zhi.framework.web.service;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.constant.CacheConstants;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.exception.user.UserPasswordNotMatchException;
import com.zhi.common.exception.user.UserPasswordRetryLimitExceedException;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.framework.security.context.AuthenticationContextHolder;
import com.zhi.framework.testsupport.SpringContextStub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录密码与失败次数锁定单元测试。
 *
 * <p>这是防撞库的唯一实现：连续失败必须在 {@code maxRetryCount} 次后拒绝，且锁定期内
 * 即使输入正确密码也不放行；成功登录必须清掉计数。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPasswordServiceTest
{
    private static final int MAX_RETRY = 5;

    private static final int LOCK_MINUTES = 10;

    @Mock
    private UnifiedCacheManager unifiedCacheManager;

    private SysPasswordService passwordService;

    private final SysUser user = new SysUser();

    @BeforeAll
    static void installMessageSource()
    {
        SpringContextStub.install();
    }

    @BeforeEach
    void setUp()
    {
        passwordService = new SysPasswordService();
        ReflectionTestUtils.setField(passwordService, "unifiedCacheManager", unifiedCacheManager);
        ReflectionTestUtils.setField(passwordService, "maxRetryCount", MAX_RETRY);
        ReflectionTestUtils.setField(passwordService, "lockTime", LOCK_MINUTES);
        user.setUserName("zhangsan");
        user.setPassword(SecurityUtils.encryptPassword("Zhiqi#2026"));
    }

    @AfterEach
    void clearAuthenticationContext()
    {
        AuthenticationContextHolder.clearContext();
    }

    private void attempting(String username, String rawPassword)
    {
        AuthenticationContextHolder
                .setContext(new UsernamePasswordAuthenticationToken(username, rawPassword));
    }

    private String failKey(String username)
    {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    @Test
    void correctPasswordClearsThePreviousFailureCounter()
    {
        attempting("zhangsan", "Zhiqi#2026");
        when(unifiedCacheManager.get(failKey("zhangsan"), Integer.class)).thenReturn(3);
        when(unifiedCacheManager.exists(failKey("zhangsan"))).thenReturn(true);

        assertThatCode(() -> passwordService.validate(user)).doesNotThrowAnyException();

        verify(unifiedCacheManager).delete(failKey("zhangsan"));
    }

    @Test
    void correctPasswordWithoutCounterDoesNotDeleteAnything()
    {
        attempting("zhangsan", "Zhiqi#2026");

        assertThatCode(() -> passwordService.validate(user)).doesNotThrowAnyException();

        verify(unifiedCacheManager, never()).delete(anyString());
    }

    @Test
    void firstWrongPasswordRecordsOneFailureLockedForTheConfiguredWindow()
    {
        attempting("zhangsan", "wrong-password");

        assertThatExceptionOfType(UserPasswordNotMatchException.class)
                .isThrownBy(() -> passwordService.validate(user));

        verify(unifiedCacheManager).set(failKey("zhangsan"), 1, LOCK_MINUTES, TimeUnit.MINUTES);
    }

    @Test
    void repeatedWrongPasswordAccumulatesTheCounter()
    {
        attempting("zhangsan", "wrong-password");
        when(unifiedCacheManager.get(failKey("zhangsan"), Integer.class)).thenReturn(4);

        assertThatExceptionOfType(UserPasswordNotMatchException.class)
                .isThrownBy(() -> passwordService.validate(user));

        verify(unifiedCacheManager).set(failKey("zhangsan"), 5, LOCK_MINUTES, TimeUnit.MINUTES);
    }

    @Test
    void exhaustedRetriesRejectEvenTheCorrectPasswordWithoutCheckingIt()
    {
        attempting("zhangsan", "Zhiqi#2026");
        when(unifiedCacheManager.get(failKey("zhangsan"), Integer.class)).thenReturn(MAX_RETRY);

        assertThatExceptionOfType(UserPasswordRetryLimitExceedException.class)
                .isThrownBy(() -> passwordService.validate(user));

        verify(unifiedCacheManager, never()).set(anyString(), any(), eq(LOCK_MINUTES), any());
        verify(unifiedCacheManager, never()).delete(anyString());
    }

    @Test
    void matchesComparesTheRawPasswordAgainstTheBCryptHash()
    {
        assertThat(passwordService.matches(user, "Zhiqi#2026")).isTrue();
        assertThat(passwordService.matches(user, "Zhiqi#2027")).isFalse();
        assertThat(passwordService.matches(user, "")).isFalse();
    }
}
