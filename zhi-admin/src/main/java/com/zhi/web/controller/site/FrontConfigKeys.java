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
        "blog_name", "blog_desc", "blog_email",
        "blog_avatar", "blog_copyright", "blog_beian",
        // 自定义品牌图：站点 Logo 与首页 Banner（均为 /profile 下的上传地址）
        "site_logo", "site_banner",
        "comment_enabled", "comment_review", "view_count_enabled",
        "search_enabled", "footer_enabled", "copyright_enabled",
        "beian_enabled",
        // 企业官网模式：首页接管 + 导航裁剪 + 企业首页模板/核心业务
        "home_page_slug", "nav_category_enabled", "nav_tag_enabled",
        "home_layout", "business_label", "contact_phone", "contact_address", "site_template",
        // 官网内容（JSON 数组，管理后台「官网内容」页签维护，前端解析带回退占位）
        "site_metrics", "site_outcomes", "home_solutions",
        "site_channels", "site_faq", "site_honors", "site_history",
        // 模板页面页头文案（JSON 对象：页面 → eyebrow/title/subtitle）
        "site_page_copy",
        "friend_link_enabled", "friend_link_apply_enabled", "guestbook_enabled",
        "user_agreement_content", "privacy_policy_content",
        // SEO 相关键（前台动态 title/description/keywords 使用）
        "seo_title", "seo_description", "seo_canonical_url", "seo_robots", "seo_favicon",
        "blog_keywords"
    };

    private FrontConfigKeys()
    {
    }
}
