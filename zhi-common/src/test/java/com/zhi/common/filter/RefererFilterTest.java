package com.zhi.common.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 防盗链判定测试。
 *
 * <p>重点是"不能子串匹配"：白名单 {@code example.com} 必须挡住
 * {@code evil-example.com} / {@code example.com.evil.io} 这类伪造成员。</p>
 */
class RefererFilterTest
{
    private static final List<String> DOMAINS = Arrays.asList("example.com", "127.0.0.1");

    @Test
    @DisplayName("空 Referer 放行（爬虫/直接访问/隐私工具会剥离，热链请求必带）")
    void emptyRefererIsAllowed()
    {
        assertTrue(RefererFilter.isAllowed(null, "blog.example.com", DOMAINS));
        assertTrue(RefererFilter.isAllowed("   ", "blog.example.com", DOMAINS));
    }

    @Test
    @DisplayName("同源访问永远放行")
    void sameOriginIsAllowed()
    {
        assertTrue(RefererFilter.isAllowed("https://blog.example.com/blog/article/1", "blog.example.com", Collections.emptyList()));
        assertTrue(RefererFilter.isAllowed("http://127.0.0.1:3000/api", "127.0.0.1", Collections.emptyList()));
    }

    @Test
    @DisplayName("白名单精确命中与子域名放行")
    void allowlistMatch()
    {
        assertTrue(RefererFilter.isAllowed("https://example.com/a.png", "api.example.com", DOMAINS));
        assertTrue(RefererFilter.isAllowed("https://img.example.com/a.png", "api.example.com", DOMAINS),
            "白名单域名应覆盖其子域名");
        assertTrue(RefererFilter.isAllowed("https://EXAMPLE.com/a.png", "api.example.com", DOMAINS), "主机名大小写不敏感");
        assertTrue(RefererFilter.isAllowed("https://example.com:8443/a.png", "api.example.com", DOMAINS), "端口不参与判断");
        assertTrue(RefererFilter.isAllowed("http://127.0.0.1:3000/a.png", "api.example.com", DOMAINS));
        assertTrue(RefererFilter.isAllowed("//example.com/a.png", "api.example.com", DOMAINS), "协议相对写法");
        assertTrue(RefererFilter.isAllowed("example.com/a.png", "api.example.com", DOMAINS), "裸域名写法");
        assertTrue(RefererFilter.isAllowed("https://blog.example.com/a.png", "api.example.com", Arrays.asList("*.example.com")));
    }

    @Test
    @DisplayName("子串伪装的域名必须被拒绝（原实现用 contains 会误放行）")
    void spoofedDomainsAreRejected()
    {
        assertFalse(RefererFilter.isAllowed("https://evil-example.com/a.png", "api.example.com", DOMAINS),
            "evil-example.com 不能因为包含 example.com 而放行");
        assertFalse(RefererFilter.isAllowed("https://example.com.evil.io/a.png", "api.example.com", DOMAINS),
            "example.com.evil.io 不能因为前缀命中而放行");
        assertFalse(RefererFilter.isAllowed("https://evil.io/?u=example.com", "api.example.com", DOMAINS),
            "域名出现在路径/查询里不算命中");
        assertFalse(RefererFilter.isAllowed("https://notexample.com/a.png", "api.example.com", DOMAINS));
        assertFalse(RefererFilter.isAllowed("https://example.com.cn/a.png", "api.example.com", DOMAINS));
    }

    @Test
    @DisplayName("白名单为空时只放行同源与空 Referer")
    void emptyAllowlistOnlyAllowsSameOrigin()
    {
        assertFalse(RefererFilter.isAllowed("https://other.com/a.png", "api.example.com", Collections.emptyList()));
        assertFalse(RefererFilter.isAllowed("https://other.com/a.png", "api.example.com", null));
    }

    @Test
    @DisplayName("畸形 Referer 不放行")
    void malformedRefererIsRejected()
    {
        assertFalse(RefererFilter.isAllowed("javascript:alert(1)", "api.example.com", DOMAINS));
        assertFalse(RefererFilter.isAllowed("::::", "api.example.com", DOMAINS));
    }

    @Test
    @DisplayName("白名单条目可带协议/端口/空白，仍按主机比较")
    void allowlistEntriesAreNormalized()
    {
        List<String> messy = Arrays.asList("  HTTPS://Example.com:443/path ", "localhost");
        assertTrue(RefererFilter.isAllowed("https://img.example.com/a.png", "api.example.com", messy));
        assertTrue(RefererFilter.isAllowed("http://localhost:5173/a.png", "api.example.com", messy));
        assertFalse(RefererFilter.isAllowed("https://example.com.evil.io/a.png", "api.example.com", messy));
    }
}
