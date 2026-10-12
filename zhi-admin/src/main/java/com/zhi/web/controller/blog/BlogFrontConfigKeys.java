package com.zhi.web.controller.blog;

/**
 * 前台可匿名访问的站点配置键白名单。
 *
 * 两个前台设置接口（/blog/setting 与 /common/blog/setting）共用同一份白名单，
 * 避免各自维护导致键集合漂移。
 *
 * @author zhi
 */
public final class BlogFrontConfigKeys
{
    /**
     * 前台可匿名访问的站点配置键白名单
     */
    public static final String[] BLOG_CONFIG_KEYS = {
        "blog_name", "blog_desc", "blog_author", "blog_email",
        "blog_avatar", "blog_copyright", "blog_beian",
        "comment_enabled", "comment_review", "like_enabled", "view_count_enabled",
        "share_enabled", "search_enabled", "sidebar_enabled", "footer_enabled", "copyright_enabled",
        "beian_enabled",
        // 企业官网模式：首页接管 + 导航裁剪 + 企业首页模板/核心业务
        "home_page_slug", "nav_category_enabled", "nav_tag_enabled", "nav_archive_enabled",
        "home_layout", "business_label", "contact_phone", "contact_address",
        "friend_link_enabled", "friend_link_apply_enabled", "guestbook_enabled",
        "user_agreement_content", "privacy_policy_content",
        "author_title", "author_bio", "github_url", "weibo_url", "wechat_qr", "author_location", "personal_website",
        // SEO 相关键（前台动态 title/description/keywords 使用）
        "seo_title", "seo_description", "seo_canonical_url", "seo_robots", "seo_favicon",
        "blog_keywords"
    };

    private BlogFrontConfigKeys()
    {
    }
}
