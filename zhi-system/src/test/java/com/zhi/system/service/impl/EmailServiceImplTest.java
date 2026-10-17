package com.zhi.system.service.impl;

import com.zhi.common.config.EmailCodeConfig;
import com.zhi.common.utils.ModernDateUtils;
import com.zhi.system.domain.EmailCode;
import com.zhi.system.mapper.EmailCodeMapper;
import com.zhi.system.service.IEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 博客邮件验证码服务单元测试
 * 覆盖 verifyCode 校验逻辑与防爆破锁定机制
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceImplTest {

    @Mock
    private EmailCodeMapper emailCodeMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private EmailCodeConfig emailCodeConfig;

    @InjectMocks
    private EmailServiceImpl emailService;

    private EmailCode validCode;

    @BeforeEach
    void setUp() {
        validCode = new EmailCode();
        validCode.setId(1L);
        validCode.setEmail("test@example.com");
        validCode.setCode("123456");
        validCode.setCodeType(IEmailService.CODE_TYPE_REGISTER);
        validCode.setExpireTime(new Date(System.currentTimeMillis() + 60000));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(emailCodeConfig.getMaxVerifyAttempts()).thenReturn(5);
        when(emailCodeConfig.getVerifyLockMinutes()).thenReturn(5);
        when(emailCodeConfig.getExpireMinutes()).thenReturn(5);
    }

    @Test
    void verifyCodeShouldSucceedWhenCodeMatches() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(emailCodeMapper.selectLatestUnusedCode("test@example.com", IEmailService.CODE_TYPE_REGISTER))
            .thenReturn(validCode);

        boolean result = emailService.verifyCode("test@example.com", "123456", IEmailService.CODE_TYPE_REGISTER);

        assertTrue(result);
        verify(emailCodeMapper).markCodeAsUsed(eq(1L), any(Date.class));
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void verifyCodeShouldFailWhenCodeWrongAndCountFails() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(emailCodeMapper.selectLatestUnusedCode("test@example.com", IEmailService.CODE_TYPE_REGISTER))
            .thenReturn(validCode);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean result = emailService.verifyCode("test@example.com", "999999", IEmailService.CODE_TYPE_REGISTER);

        assertFalse(result);
        verify(valueOperations).increment(anyString());
        verify(emailCodeMapper, never()).markCodeAsUsed(eq(1L), any(Date.class));
    }

    /**
     * 软锁定守卫：达到失败阈值后，**正确的验证码仍必须通过**。
     * 原实现把锁检查放在比对之前直接拒绝，攻击者只要知道受害者邮箱、连发 5 次错误码，
     * 就能让受害者本人的正确验证码也被拒（自助注册/重置流程 DoS）。
     */
    @Test
    void verifyCodeShouldStillAcceptCorrectCodeWhenLocked() {
        when(valueOperations.get(anyString())).thenReturn(5L);
        EmailCode valid = new EmailCode();
        valid.setId(1L);
        valid.setCode("123456");
        valid.setExpireTime(new java.util.Date(ModernDateUtils.now().getTime() + 5 * 60_000L));
        when(emailCodeMapper.selectLatestUnusedCode("test@example.com", IEmailService.CODE_TYPE_REGISTER))
            .thenReturn(valid);

        boolean result = emailService.verifyCode("test@example.com", "123456", IEmailService.CODE_TYPE_REGISTER);

        assertTrue(result, "锁定期间正确验证码必须放行，否则可被用来锁死他人账号");
        verify(emailCodeMapper).markCodeAsUsed(eq(1L), any(Date.class));
    }

    /**
     * 锁定期间错误验证码仍须拒绝，且不得继续累加计数（否则过期时间被无限续期，锁永不释放）。
     */
    @Test
    void verifyCodeShouldRejectWrongCodeWhenLockedWithoutExtendingLock() {
        when(valueOperations.get(anyString())).thenReturn(5L);
        EmailCode valid = new EmailCode();
        valid.setId(1L);
        valid.setCode("123456");
        valid.setExpireTime(new java.util.Date(ModernDateUtils.now().getTime() + 5 * 60_000L));
        when(emailCodeMapper.selectLatestUnusedCode("test@example.com", IEmailService.CODE_TYPE_REGISTER))
            .thenReturn(valid);

        boolean result = emailService.verifyCode("test@example.com", "000000", IEmailService.CODE_TYPE_REGISTER);

        assertFalse(result);
        verify(valueOperations, never()).increment(anyString());
        verify(emailCodeMapper, never()).markCodeAsUsed(anyLong(), any(Date.class));
    }

    @Test
    void verifyCodeShouldRejectWhenNoCodeFound() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(emailCodeMapper.selectLatestUnusedCode("test@example.com", IEmailService.CODE_TYPE_REGISTER))
            .thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean result = emailService.verifyCode("test@example.com", "123456", IEmailService.CODE_TYPE_REGISTER);

        assertFalse(result);
        verify(valueOperations).increment(anyString());
    }

    @Test
    void devPrintCodeShouldBeForcedOffInProdProfile() {
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "activeProfile", "prod");
        when(emailCodeConfig.isDevPrintCode()).thenReturn(true); // 即使配置被误开

        assertFalse(emailService.isDevPrintCodeEnabled(),
            "生产 profile 下必须强制关闭打印验证码，否则不发邮件且会跳过 IP 频率限制");
    }

    @Test
    void devPrintCodeShouldFollowConfigOutsideProd() {
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "activeProfile", "dev");
        when(emailCodeConfig.isDevPrintCode()).thenReturn(true);
        assertTrue(emailService.isDevPrintCodeEnabled());

        when(emailCodeConfig.isDevPrintCode()).thenReturn(false);
        assertFalse(emailService.isDevPrintCodeEnabled());
    }
}
