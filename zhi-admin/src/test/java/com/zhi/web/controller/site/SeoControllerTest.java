package com.zhi.web.controller.site;

import com.zhi.system.service.IArticleService;
import com.zhi.system.service.IPageService;
import com.zhi.system.service.ISettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /sitemap.xml 与 /robots.txt 测试。
 *
 * <p>审计发现两个问题：未配置「站点访问地址」时站点地图回落到 {@code http://localhost:3000}
 * （搜索引擎会收录 localhost），以及后台「Robots规则」设置根本没人读。</p>
 */
class SeoControllerTest
{
    private ISettingService settingService;

    private IArticleService articleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        settingService = mock(ISettingService.class);
        SeoController controller = new SeoController();
        ReflectionTestUtils.setField(controller, "settingService", settingService);
        articleService = mock(IArticleService.class);
        ReflectionTestUtils.setField(controller, "articleService", articleService);
        ReflectionTestUtils.setField(controller, "pageService", mock(IPageService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private String body(String path, String host, int port) throws Exception
    {
        MvcResult result = mockMvc.perform(get(path).header("Host", host).secure(true)
                .with(request -> {
                    request.setScheme("https");
                    request.setServerName(host);
                    request.setServerPort(port);
                    return request;
                }))
            .andExpect(status().isOk())
            .andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    @DisplayName("未配置站点访问地址时，站点地图用请求域名而不是 localhost")
    void sitemapFallsBackToRequestHost() throws Exception
    {
        when(settingService.selectSettingValueByKey(anyString())).thenReturn(null);

        String xml = body("/sitemap.xml", "blog.example.com", 443);

        assertFalse(xml.contains("localhost"), "站点地图绝不能出现 localhost：\n" + xml);
        assertFalse(xml.contains("/blog/"), "站点地图不应出现 /blog 页面路径：\n" + xml);
        assertTrue(xml.contains("<loc>https://blog.example.com/</loc>"), xml);
        assertTrue(xml.contains("https://blog.example.com/about"), xml);
        verify(articleService).selectSitemapArticles();
    }

    @Test
    @DisplayName("配置了站点访问地址时优先使用它")
    void sitemapPrefersConfiguredSiteUrl() throws Exception
    {
        when(settingService.selectSettingValueByKey(anyString())).thenReturn("https://myblog.example.com/");

        String xml = body("/sitemap.xml", "internal-host:8080", 8080);

        assertTrue(xml.contains("<loc>https://myblog.example.com/</loc>"), xml);
        assertFalse(xml.contains("internal-host"), xml);
    }

    @Test
    @DisplayName("首页被自定义页面接管时，站点地图收录文章列表地址 /news")
    void sitemapIncludesArticleListWhenHomeIsCustomPage() throws Exception
    {
        when(settingService.selectSettingValueByKey("blog_url")).thenReturn("https://blog.example.com");
        when(settingService.selectSettingValueByKey("home_page_slug")).thenReturn("home");

        String xml = body("/sitemap.xml", "blog.example.com", 443);

        assertTrue(xml.contains("<loc>https://blog.example.com/news</loc>"), xml);
    }

    @Test
    @DisplayName("默认博客首页时，文章列表地址 /news 不重复收录")
    void sitemapOmitsArticleListForDefaultHome() throws Exception
    {
        when(settingService.selectSettingValueByKey(anyString())).thenReturn(null);

        String xml = body("/sitemap.xml", "blog.example.com", 443);

        assertFalse(xml.contains("/news"), xml);
    }

    @Test
    @DisplayName("核心业务/联系我们为独立页面，任何形态下都收录")
    void sitemapAlwaysIncludesBusinessAndContact() throws Exception
    {
        when(settingService.selectSettingValueByKey(anyString())).thenReturn(null);

        String xml = body("/sitemap.xml", "blog.example.com", 443);

        assertTrue(xml.contains("<loc>https://blog.example.com/business</loc>"), xml);
        assertTrue(xml.contains("<loc>https://blog.example.com/contact</loc>"), xml);
    }

    @Test
    @DisplayName("Robots规则含 noindex 时整站关闭爬虫抓取")
    void robotsHonoursNoindexSetting() throws Exception
    {
        when(settingService.selectSettingValueByKey("blog_url")).thenReturn("https://blog.example.com");
        when(settingService.selectSettingValueByKey("seo_robots")).thenReturn("noindex,nofollow");

        String txt = body("/robots.txt", "blog.example.com", 443);

        assertTrue(txt.contains("Disallow: /"), txt);
        assertFalse(txt.contains("Allow: /"), "noindex 时不应再声明 Allow: /\n" + txt);
        assertTrue(txt.contains("Sitemap: https://blog.example.com/sitemap.xml"), txt);
    }

    @Test
    @DisplayName("Robots规则为常规值时保持默认抓取策略")
    void robotsDefaultsToAllow() throws Exception
    {
        when(settingService.selectSettingValueByKey("blog_url")).thenReturn("https://blog.example.com");
        when(settingService.selectSettingValueByKey("seo_robots")).thenReturn("index,follow");

        String txt = body("/robots.txt", "blog.example.com", 443);

        assertTrue(txt.contains("Allow: /"), txt);
        assertTrue(txt.contains("Disallow: /admin"), txt);
    }
}
