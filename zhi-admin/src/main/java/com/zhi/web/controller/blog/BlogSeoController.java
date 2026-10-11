package com.zhi.web.controller.blog;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.utils.ModernDateUtils;
import com.zhi.system.domain.BlogArticle;
import com.zhi.system.domain.BlogPage;
import com.zhi.system.service.IBlogArticleService;
import com.zhi.system.service.IBlogPageService;
import com.zhi.system.service.IBlogSettingService;

/**
 * 博客 SEO 控制器：/sitemap.xml 与 /robots.txt
 *
 * @author nevell
 * @date 2026-09-05
 */
@RestController
public class BlogSeoController
{
    /** 页面地址集合 */
    private static final String[] STATIC_PATHS = { "/blog", "/blog/about", "/blog/category", "/blog/tag", "/blog/archive", "/blog/guestbook" };

    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired
    private IBlogArticleService blogArticleService;

    @Autowired
    private IBlogPageService blogPageService;

    /**
     * 站点地图
     */
    @Anonymous
    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    public void sitemap(HttpServletResponse response, jakarta.servlet.http.HttpServletRequest request) throws IOException
    {
        String baseUrl = resolveBaseUrl(request);
        response.setContentType("application/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        // 首页
        writeUrl(out, baseUrl + "/", null);
        // 静态页面
        for (String path : STATIC_PATHS)
        {
            writeUrl(out, baseUrl + path, null);
        }
        // 首页被自定义页面接管时，文章列表迁到 /blog/articles（否则该地址与首页重复，不入站点地图）
        String homePageSlug = readSetting("home_page_slug");
        if (homePageSlug != null && !homePageSlug.trim().isEmpty())
        {
            writeUrl(out, baseUrl + "/blog/articles", null);
        }
        // 已发布文章
        try
        {
            BlogArticle query = new BlogArticle();
            query.setStatus(1L);
            query.setDelFlag(0L);
            List<BlogArticle> articles = blogArticleService.selectSitemapArticles();
            if (articles != null)
            {
                for (BlogArticle article : articles)
                {
                    String lastmod = null;
                    if (article.getUpdateTime() != null)
                    {
                        lastmod = ModernDateUtils.format(article.getUpdateTime(), ModernDateUtils.YYYY_MM_DD);
                    }
                    writeUrl(out, baseUrl + "/blog/article/" + article.getId(), lastmod);
                }
            }
        }
        catch (Exception e)
        {
            // 单篇文章失败不阻塞整体输出
        }
        // 已发布的自定义页面
        try
        {
            List<BlogPage> pages = blogPageService.selectPublishedPageList();
            if (pages != null)
            {
                for (BlogPage page : pages)
                {
                    if (page.getSlug() == null || page.getSlug().isEmpty())
                    {
                        continue;
                    }
                    String lastmod = null;
                    if (page.getUpdateTime() != null)
                    {
                        lastmod = ModernDateUtils.format(page.getUpdateTime(), ModernDateUtils.YYYY_MM_DD);
                    }
                    writeUrl(out, baseUrl + "/blog/page/" + page.getSlug(), lastmod);
                }
            }
        }
        catch (Exception e)
        {
            // 单个页面失败不阻塞整体输出
        }
        out.println("</urlset>");
        out.flush();
    }

    /**
     * robots.txt
     */
    @Anonymous
    @GetMapping(value = "/robots.txt", produces = "text/plain;charset=UTF-8")
    public void robots(HttpServletResponse response, jakarta.servlet.http.HttpServletRequest request) throws IOException
    {
        String baseUrl = resolveBaseUrl(request);
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("User-agent: *");
        // 后台「SEO优化 → Robots规则」里写了 noindex 时，整站对爬虫关闭（与该 meta 指令保持一致）
        String robotsRule = readSetting("seo_robots");
        if (robotsRule != null && robotsRule.toLowerCase().contains("noindex"))
        {
            out.println("Disallow: /");
        }
        else
        {
            out.println("Allow: /");
            out.println("Disallow: /admin");
        }
        out.println("Sitemap: " + baseUrl + "/sitemap.xml");
        out.flush();
    }

    /** 读取博客设置，异常时返回 null（按未配置处理） */
    private String readSetting(String key)
    {
        try
        {
            return blogSettingService.selectSettingValueByKey(key);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void writeUrl(PrintWriter out, String loc, String lastmod)
    {
        out.println("  <url>");
        out.println("    <loc>" + escapeXml(loc) + "</loc>");
        if (lastmod != null && !lastmod.isEmpty())
        {
            out.println("    <lastmod>" + escapeXml(lastmod) + "</lastmod>");
        }
        out.println("  </url>");
    }

    private String resolveBaseUrl(jakarta.servlet.http.HttpServletRequest request)
    {
        String blogUrl = readSetting("blog_url");
        if (blogUrl == null || blogUrl.trim().isEmpty())
        {
            // 未配置站点访问地址时用当前请求的域名，绝不回落到 localhost
            blogUrl = requestBaseUrl(request);
        }
        if (blogUrl == null || blogUrl.trim().isEmpty())
        {
            blogUrl = "http://localhost:3000";
        }
        blogUrl = blogUrl.trim();
        while (blogUrl.endsWith("/"))
        {
            blogUrl = blogUrl.substring(0, blogUrl.length() - 1);
        }
        return blogUrl;
    }

    /** 由当前请求推导站点地址（含非默认端口），用于未配置站点访问地址时 */
    private String requestBaseUrl(jakarta.servlet.http.HttpServletRequest request)
    {
        if (request == null)
        {
            return null;
        }
        String scheme = request.getScheme() == null ? "http" : request.getScheme();
        String host = request.getServerName();
        if (host == null || host.isEmpty())
        {
            return null;
        }
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
            || ("https".equalsIgnoreCase(scheme) && port == 443) || port <= 0;
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }

    private String escapeXml(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
