package com.zhi.web.controller.site;

import com.zhi.system.domain.Article;
import com.zhi.system.domain.Page;
import com.zhi.system.service.IArticleService;
import com.zhi.system.service.IPageService;
import com.zhi.system.service.ISettingService;
import java.util.List;
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

    private IPageService pageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        settingService = mock(ISettingService.class);
        SeoController controller = new SeoController();
        ReflectionTestUtils.setField(controller, "settingService", settingService);
        articleService = mock(IArticleService.class);
        ReflectionTestUtils.setField(controller, "articleService", articleService);
        pageService = mock(IPageService.class);
        ReflectionTestUtils.setField(controller, "pageService", pageService);
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
    @DisplayName("解决方案栏目独立收录，方案文章规范地址为 /solutions/{slug} 而非 /article/")
    void sitemapMapsSolutionArticlesToSolutionsPath() throws Exception
    {
        when(settingService.selectSettingValueByKey(anyString())).thenReturn(null);
        Article solution = new Article();
        solution.setId(7L);
        solution.setArticleType("solution");
        solution.setSlug("smart-retail");
        when(articleService.selectSitemapArticles()).thenReturn(List.of(solution));

        String xml = body("/sitemap.xml", "blog.example.com", 443);

        assertTrue(xml.contains("<loc>https://blog.example.com/solutions</loc>"), xml);
        assertTrue(xml.contains("<loc>https://blog.example.com/solutions/smart-retail</loc>"), xml);
        assertFalse(xml.contains("/article/smart-retail"), "方案文章不应收录到 /article/：\n" + xml);
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

    @Test
    @DisplayName("站点地图不收录已下线的 /archive")
    void sitemapMustNotIncludeRemovedArchive() throws Exception
    {
        String xml = body("/sitemap.xml", "blog.example.com", 443);
        assertFalse(xml.contains("/archive"), "/archive 页面与路由已删除，收录会产出死链");
    }

    @Test
    @DisplayName("作为首页渲染的页面不再重复收录 /page/{slug}")
    void sitemapSkipsPageRenderedAsHome() throws Exception
    {
        when(settingService.selectSettingValueByKey("home_page_slug")).thenReturn("landing");

        Page homeBuilder = new Page();
        homeBuilder.setSlug("home");
        Page landing = new Page();
        landing.setSlug("landing");
        Page about = new Page();
        about.setSlug("about");
        when(pageService.selectPublishedPageList()).thenReturn(List.of(homeBuilder, landing, about));

        String xml = body("/sitemap.xml", "blog.example.com", 443);
        assertFalse(xml.contains("/page/home"), "builder 首页内容与 \"/\" 重复，不应收录 /page/home");
        assertFalse(xml.contains("/page/landing"), "被 home_page_slug 接管的页面与 \"/\" 内容重复，不应收录");
        assertTrue(xml.contains("/page/about"), "普通自定义页面仍需收录");
    }


    @Test
    @DisplayName("留言板关闭时不收录 /guestbook，开启时收录")
    void sitemapIncludesGuestbookOnlyWhenEnabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("guestbook_enabled")).thenReturn("false");
        assertFalse(body("/sitemap.xml", "blog.example.com", 443).contains("/guestbook"),
            "留言板开关关闭时，页面只显示「暂未开放」，不应收录");

        when(settingService.selectSettingValueByKey("guestbook_enabled")).thenReturn("true");
        assertTrue(body("/sitemap.xml", "blog.example.com", 443).contains("/guestbook"),
            "留言板开启时应正常收录");
    }


    @Test
    @DisplayName("sitemap 收录协议与隐私页（法务公开内容）")
    void sitemapIncludesLegalPages() throws Exception
    {
        String xml = body("/sitemap.xml", "blog.example.com", 443);
        assertTrue(xml.contains("/agreement"), "用户协议页应被收录");
        assertTrue(xml.contains("/privacy"), "隐私政策页应被收录");
    }

}
