package com.zhi.framework.web.service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.constant.CacheConstants;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 令牌服务单元测试。
 *
 * <p>JWT 的签发/解析与缓存会话（{@code login_tokens:<uuid>}）的 TTL 口径是登录链路的核心：
 * 记住我走独立时长、临期自动续期、无效令牌必须静默返回 null 而不是把请求打成 500。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenServiceTest
{
    /** HS256 要求密钥不少于 256bit，这里用与生产同长度的串 */
    private static final String SECRET = "zhiblog-unit-test-secret-key-0123456789-abcdefghij-0123456789";

    private static final String HEADER = "Authorization";

    @Mock
    private UnifiedCacheManager unifiedCacheManager;

    private TokenService tokenService;

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp()
    {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "header", HEADER);
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);
        ReflectionTestUtils.setField(tokenService, "expireTime", 30);
        ReflectionTestUtils.setField(tokenService, "rememberMeExpireTime", 10080);
        ReflectionTestUtils.setField(tokenService, "unifiedCacheManager", unifiedCacheManager);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private LoginUser loginUser(String username)
    {
        SysUser user = new SysUser();
        user.setUserName(username);
        LoginUser loginUser = new LoginUser(1L, 2L, user, Collections.emptySet());
        loginUser.setBrowser("Chrome");
        return loginUser;
    }

    @Test
    void createTokenIssuesJwtAndCachesSessionForThirtyMinutes()
    {
        LoginUser loginUser = loginUser("admin");

        String token = tokenService.createToken(loginUser);

        assertThat(token).isNotBlank();
        assertThat(tokenService.getUsernameFromToken(token)).isEqualTo("admin");
        // 令牌里带的是缓存会话的 uuid，前端拿到的 JWT 只是指针
        String uuid = loginUser.getToken();
        assertThat(uuid).isNotBlank();
        verify(unifiedCacheManager).set(eq(CacheConstants.LOGIN_TOKEN_KEY + uuid), eq(loginUser),
                eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void rememberMeUsesTheLongerConfiguredTtl()
    {
        LoginUser loginUser = loginUser("admin");

        tokenService.createToken(loginUser, true);

        verify(unifiedCacheManager).set(anyString(), any(LoginUser.class), eq(10080L), eq(TimeUnit.MINUTES));
    }

    @Test
    void createTokenRecordsClientFingerprintFromTheCurrentRequest()
    {
        request.addHeader("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36");
        request.setRemoteAddr("127.0.0.1");
        LoginUser loginUser = loginUser("admin");

        tokenService.createToken(loginUser);

        assertThat(loginUser.getIpaddr()).isEqualTo("127.0.0.1");
        // 内网地址不做地理查询，避免单测出网
        assertThat(loginUser.getLoginLocation()).isEqualTo("内网IP");
        assertThat(loginUser.getBrowser()).isEqualTo("Chrome124");
        assertThat(loginUser.getOs()).isEqualTo("macOS10");
    }

    @Test
    void createTokenSurvivesRequestsWithoutUserAgentHeader()
    {
        // 健康检查与部分 CLI 客户端不发 User-Agent，签发令牌不能因此 500
        LoginUser loginUser = loginUser("admin");

        String token = tokenService.createToken(loginUser);

        assertThat(token).isNotBlank();
        assertThat(loginUser.getBrowser()).isEmpty();
        assertThat(loginUser.getOs()).isEmpty();
    }

    @Test
    void getLoginUserReadsTheSessionCachedUnderTheTokenUuid()
    {
        LoginUser stored = loginUser("admin");
        String jwt = tokenService.createToken(stored);
        String uuid = stored.getToken();
        when(unifiedCacheManager.get(CacheConstants.LOGIN_TOKEN_KEY + uuid, LoginUser.class)).thenReturn(stored);

        LoginUser resolved = tokenService.getLoginUser(headerRequest(jwt));

        assertThat(resolved).isSameAs(stored);
    }

    @Test
    void getLoginUserReturnsNullForMissingGarbageAndForeignTokens()
    {
        // 无 header
        assertThat(tokenService.getLoginUser(new MockHttpServletRequest())).isNull();
        // 非法 JWT（不抛异常，只按未登录处理）
        assertThat(tokenService.getLoginUser(headerRequest("Bearer not-a-jwt"))).isNull();
        // 签名不属于本实例
        String otherSecretJwt = tokenService.createToken(loginUser("admin"));
        ReflectionTestUtils.setField(tokenService, "secret",
                "a-completely-different-secret-key-0123456789-abcdefghij-0123456789");
        assertThat(tokenService.getLoginUser(headerRequest("Bearer " + otherSecretJwt))).isNull();
    }

    @Test
    void getLoginUserReturnsNullWhenTheSessionAlreadyExpiredOutOfCache()
    {
        LoginUser stored = loginUser("admin");
        String jwt = tokenService.createToken(stored);
        when(unifiedCacheManager.get(anyString(), eq(LoginUser.class))).thenReturn(null);

        assertThat(tokenService.getLoginUser(headerRequest(jwt))).isNull();
    }

    @Test
    void tokenPrefixIsOptionalWhenReadingTheHeader()
    {
        LoginUser stored = loginUser("admin");
        String jwt = tokenService.createToken(stored);
        when(unifiedCacheManager.get(CacheConstants.LOGIN_TOKEN_KEY + stored.getToken(), LoginUser.class))
                .thenReturn(stored);

        // 裸 token（没有 "Bearer "）同样能解析
        assertThat(tokenService.getLoginUser(headerRequest(jwt))).isSameAs(stored);
    }

    @Test
    void verifyTokenRenewsOnlyWhenTheSessionIsCloseToExpiry()
    {
        LoginUser soon = loginUser("admin");
        soon.setToken("uuid-soon");
        soon.setExpireTime(System.currentTimeMillis() + 10 * 60 * 1000L);
        tokenService.verifyToken(soon);
        assertThat(soon.getExpireTime()).isGreaterThan(System.currentTimeMillis());

        LoginUser fresh = loginUser("admin");
        fresh.setToken("uuid-fresh");
        fresh.setExpireTime(System.currentTimeMillis() + 60 * 60 * 1000L);
        long before = fresh.getExpireTime();
        tokenService.verifyToken(fresh);
        assertThat(fresh.getExpireTime()).isEqualTo(before);
        assertThat(fresh.getLoginTime()).isNull();
    }

    @Test
    void setLoginUserRefreshesOnlyWhenTheSessionHasAToken()
    {
        tokenService.setLoginUser(null);
        LoginUser noToken = loginUser("admin");
        tokenService.setLoginUser(noToken);
        verify(unifiedCacheManager, never()).set(anyString(), any(), anyLong(), any());

        LoginUser withToken = loginUser("admin");
        withToken.setToken("uuid-x");
        tokenService.setLoginUser(withToken);
        verify(unifiedCacheManager).set(CacheConstants.LOGIN_TOKEN_KEY + "uuid-x", withToken, 30L,
                TimeUnit.MINUTES);
    }

    @Test
    void delLoginUserIgnoresBlankTokensAndDeletesTheSessionKey()
    {
        tokenService.delLoginUser("");
        tokenService.delLoginUser(null);
        verify(unifiedCacheManager, never()).delete(anyString());

        tokenService.delLoginUser("uuid-y");
        verify(unifiedCacheManager).delete(CacheConstants.LOGIN_TOKEN_KEY + "uuid-y");
    }

    @Test
    void deleteLoginUserByUserIdInvalidatesOnlyMatchingSessions()
    {
        LoginUser first = loginUser("admin");
        first.setToken("uuid-a");
        LoginUser second = loginUser("admin");
        second.setToken("uuid-b");
        SysUser otherUser = new SysUser();
        otherUser.setUserName("guest");
        LoginUser other = new LoginUser(9L, 2L, otherUser, Collections.emptySet());
        other.setToken("uuid-c");

        String keyA = CacheConstants.LOGIN_TOKEN_KEY + "uuid-a";
        String keyB = CacheConstants.LOGIN_TOKEN_KEY + "uuid-b";
        String keyC = CacheConstants.LOGIN_TOKEN_KEY + "uuid-c";
        when(unifiedCacheManager.keys(CacheConstants.LOGIN_TOKEN_KEY + "*"))
                .thenReturn(new LinkedHashSet<>(List.of(keyA, keyB, keyC)));
        when(unifiedCacheManager.get(keyA, LoginUser.class)).thenReturn(first);
        when(unifiedCacheManager.get(keyB, LoginUser.class)).thenReturn(second);
        when(unifiedCacheManager.get(keyC, LoginUser.class)).thenReturn(other);

        int removed = tokenService.deleteLoginUserByUserId(1L);

        assertThat(removed).isEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(unifiedCacheManager).delete(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(keyA, keyB);
    }

    @Test
    void deleteLoginUserByUserIdIgnoresNullAndEmptyCache()
    {
        assertThat(tokenService.deleteLoginUserByUserId(null)).isZero();
        verify(unifiedCacheManager, never()).keys(anyString());

        when(unifiedCacheManager.keys(anyString())).thenReturn(Collections.emptySet());
        assertThat(tokenService.deleteLoginUserByUserId(1L)).isZero();
        verify(unifiedCacheManager, never()).delete(anyString());
        verify(unifiedCacheManager, never()).delete(anyCollection());
    }

    @Test
    void parsingATokenSignedWithAnotherKeyFailsLoudlyOutsideTheRequestPath()
    {
        String jwt = tokenService.createToken(loginUser("admin"));
        ReflectionTestUtils.setField(tokenService, "secret",
                "a-completely-different-secret-key-0123456789-abcdefghij-0123456789");

        // getLoginUser 吞掉异常，但 getUsernameFromToken 不吞：这是验签失败的直接表现
        assertThatThrownBy(() -> tokenService.getUsernameFromToken(jwt))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    private HttpServletRequest headerRequest(String value)
    {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(HEADER, value);
        return req;
    }

    @Test
    void issuedTokenIsSignedJwtPointingAtTheCachedSessionUuid()
    {
        LoginUser loginUser = loginUser("zhangsan");
        String jwt = tokenService.createToken(loginUser);

        assertThat(jwt.split("\\.")).hasSize(3);
        verify(unifiedCacheManager).set(eq(CacheConstants.LOGIN_TOKEN_KEY + loginUser.getToken()),
                eq(loginUser), eq(30L), eq(TimeUnit.MINUTES));
    }
}
