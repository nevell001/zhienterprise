package com.zhi.web.controller.site;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.utils.ModernDateUtils;
import com.zhi.common.utils.SiteSwitchUtils;
import com.zhi.system.domain.Article;
import com.zhi.system.domain.Category;
import com.zhi.system.domain.Page;
import com.zhi.system.service.IArticleService;
import com.zhi.system.service.ICategoryService;
import com.zhi.system.service.IPageService;
import com.zhi.system.service.ISettingService;

/**
 * 站点 SEO 控制器：/sitemap.xml 与 /robots.txt
 *
 * @author nevell
 * @date 2026-09-05
 */
@RestController
public class SeoController
{
    /** 页面地址集合（站点前台为短路径，与 zhi-ui/src/router/site.ts 一致） */
    /**
     * 静态页面清单；/archive 已随博客功能下线（页面与路由均已删除），不得再收录。
     * 协议/隐私页是公开可索引的法务内容，按产品决策一并收录。
     */
    private static final String[] STATIC_PATHS = {
        "/about", "/honors", "/history", "/category", "/tag", "/guestbook", "/agreement", "/privacy"
    };

    @Autowired
    private ISettingService settingService;

    @Autowired
    private IArticleService articleService;

    @Autowired
    private IPageService pageService;

    @Autowired
    private ICategoryService categoryService;

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
            // 留言板关闭时页面只展示「暂未开放」，收录无意义（SiteSwitchUtils 口径：缺失/其它值视为开启）
            if ("/guestbook".equals(path) && SiteSwitchUtils.isOff(readSetting("guestbook_enabled")))
            {
                continue;
            }
            writeUrl(out, baseUrl + path, null);
        }
        // 首页被自定义页面接管、或官网模板首页形态时，文章列表页 /news 与首页内容不同，收录；
        // 默认形态下 /news 与首页同为文章列表，不重复收录
        String homePageSlug = readSetting("home_page_slug");
        boolean customHome = homePageSlug != null && !homePageSlug.trim().isEmpty();
        String homeLayout = readSetting("home_layout");
        boolean tplMode = "business".equalsIgnoreCase(homeLayout == null ? "" : homeLayout.trim());
        if (customHome || tplMode)
        {
            writeUrl(out, baseUrl + "/news", null);
        }
        // 核心业务、解决方案与联系我们：两种站点形态下都是独立页面
        writeUrl(out, baseUrl + "/business", null);
        writeUrl(out, baseUrl + "/solutions", null);
        writeUrl(out, baseUrl + "/contact", null);
        // 启用中的分类（alias 伪静态优先）
        try
        {
            Category catQuery = new Category();
            catQuery.setDelFlag("0");
            catQuery.setStatus(1);
            List<Category> categories = categoryService.selectCategoryListForFront(catQuery);
            if (categories != null)
            {
                for (Category category : categories)
                {
                    String seg = category.getAlias() != null && !category.getAlias().trim().isEmpty()
                        ? category.getAlias().trim() : String.valueOf(category.getId());
                    writeUrl(out, baseUrl + "/category/" + seg, null);
                }
            }
        }
        catch (Exception e)
        {
            // 分类收录失败不影响站点地图其余部分
        }
        // 已发布文章（核心业务/客户案例的规范入口是 /business/{id}，解决方案是 /solutions/{id}）
        try
        {
            Article query = new Article();
            query.setStatus(1L);
            query.setDelFlag(0L);
            List<Article> articles = articleService.selectSitemapArticles();
            if (articles != null)
            {
                for (Article article : articles)
                {
                    String lastmod = null;
                    if (article.getUpdateTime() != null)
                    {
                        lastmod = ModernDateUtils.format(article.getUpdateTime(), ModernDateUtils.YYYY_MM_DD);
                    }
                    String type = article.getArticleType();
                    boolean isBiz = "business".equals(type) || "case".equals(type);
                    String pathPrefix = "solution".equals(type) ? "/solutions/" : (isBiz ? "/business/" : "/article/");
                    // 伪静态优先：有 slug 用 slug，否则回退数字 ID
                    String seg = article.getSlug() != null && !article.getSlug().trim().isEmpty()
                        ? article.getSlug().trim() : String.valueOf(article.getId());
                    writeUrl(out, baseUrl + pathPrefix + seg, lastmod);
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
            List<Page> pages = pageService.selectPublishedPageList();
            if (pages != null)
            {
                for (Page page : pages)
                {
                    if (page.getSlug() == null || page.getSlug().isEmpty())
                    {
                        continue;
                    }
                    // 作为首页渲染的页面不再单独收录 /page/{slug}：内容与 "/" 完全重复
                    String slug = page.getSlug().trim();
                    if ("home".equals(slug) || slug.equals(homePageSlug == null ? "" : homePageSlug.trim()))
                    {
                        continue;
                    }
                    String lastmod = null;
                    if (page.getUpdateTime() != null)
                    {
                        lastmod = ModernDateUtils.format(page.getUpdateTime(), ModernDateUtils.YYYY_MM_DD);
                    }
                    writeUrl(out, baseUrl + "/page/" + page.getSlug(), lastmod);
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

    /** 读取站点设置，异常时返回 null（按未配置处理） */
    private String readSetting(String key)
    {
        try
        {
            return settingService.selectSettingValueByKey(key);
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
        String siteUrl = readSetting("blog_url");
        if (siteUrl == null || siteUrl.trim().isEmpty())
        {
            // 未配置站点访问地址时用当前请求的域名，绝不回落到 localhost
            siteUrl = requestBaseUrl(request);
        }
        if (siteUrl == null || siteUrl.trim().isEmpty())
        {
            siteUrl = "http://localhost:3000";
        }
        siteUrl = siteUrl.trim();
        while (siteUrl.endsWith("/"))
        {
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
        }
        return siteUrl;
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
