package com.zhi.framework.security.filter;

import java.io.IOException;
import java.util.Collections;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.framework.web.service.TokenService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JWT 过滤器单元测试。
 *
 * <p>它决定"带令牌的请求以谁的身份执行"：无会话必须原样放行给后续的 permitAll/匿名规则，
 * 已有认证不能被覆盖，且放行与否都不能因为过滤器自身出错而改变链的长度。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationTokenFilterTest
{
    @Mock
    private TokenService tokenService;

    @Mock
    private FilterChain chain;

    private JwtAuthenticationTokenFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/user/list");

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp()
    {
        filter = new JwtAuthenticationTokenFilter();
        ReflectionTestUtils.setField(filter, "tokenService", tokenService);
    }

    @AfterEach
    void clearContext()
    {
        SecurityContextHolder.clearContext();
    }

    private LoginUser session(String username)
    {
        SysUser user = new SysUser();
        user.setUserName(username);
        LoginUser loginUser = new LoginUser(7L, 3L, user, Collections.singleton("zhi:article:edit"));
        loginUser.setToken("uuid-1");
        return loginUser;
    }

    private void doFilter() throws ServletException, IOException
    {
        filter.doFilter(request, response, chain);
    }

    @Test
    void requestWithoutSessionIsPassedDownTheChainUnauthenticated() throws Exception
    {
        when(tokenService.getLoginUser(any())).thenReturn(null);

        doFilter();

        verify(chain).doFilter(request, response);
        verify(tokenService, never()).verifyToken(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validSessionAuthenticatesTheRequestAndRenewsItsExpiry() throws Exception
    {
        LoginUser loginUser = session("zhangsan");
        when(tokenService.getLoginUser(any())).thenReturn(loginUser);

        doFilter();

        verify(tokenService).verifyToken(loginUser);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(loginUser);
        assertThat(authentication.isAuthenticated()).isTrue();
        // 权限由 @PreAuthorize("@ss.hasPermi(...)") 现取现比，过滤器只放身份进来
        assertThat(authentication.getAuthorities()).isNotNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void alreadyAuthenticatedRequestIsNotOverwrittenNorRefreshed() throws Exception
    {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "someone-else",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        doFilter();

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("someone-else");
        verify(tokenService, never()).verifyToken(any());
        verify(chain).doFilter(request, response);
    }
}
