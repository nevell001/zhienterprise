package com.zhi.web.controller.site;

/**
 * 前台可匿名访问的站点配置键白名单。
 *
 * 两个前台设置接口（/api/setting 与 /common/api/setting）共用同一份白名单，
 * 避免各自维护导致键集合漂移。
 *
 * @author zhi
 */
public final class FrontConfigKeys
{
    /**
     * 前台可匿名访问的站点配置键白名单
     */
    public static final String[] CONFIG_KEYS = {
        "blog_name", "blog_desc", "blog_author", "blog_email",
        "blog_avatar", "blog_copyright", "blog_beian",
        "comment_enabled", "comment_review", "like_enabled", "view_count_enabled",
        "share_enabled", "search_enabled", "sidebar_enabled", "footer_enabled", "copyright_enabled",
        "beian_enabled",
        // 企业官网模式：首页接管 + 导航裁剪 + 企业首页模板/核心业务
        "home_page_slug", "nav_category_enabled", "nav_tag_enabled", "nav_archive_enabled",
        "home_layout", "business_label", "contact_phone", "contact_address", "site_template",
        // 官网内容（JSON 数组，管理后台「官网内容」页签维护，前端解析带回退占位）
        "site_metrics", "site_outcomes", "home_solutions",
        "site_channels", "site_faq", "site_honors", "site_history",
        "friend_link_enabled", "friend_link_apply_enabled", "guestbook_enabled",
        "user_agreement_content", "privacy_policy_content",
        "author_title", "author_bio", "github_url", "weibo_url", "wechat_qr", "author_location", "personal_website",
        // SEO 相关键（前台动态 title/description/keywords 使用）
        "seo_title", "seo_description", "seo_canonical_url", "seo_robots", "seo_favicon",
        "blog_keywords"
    };

    private FrontConfigKeys()
    {
    }
}
