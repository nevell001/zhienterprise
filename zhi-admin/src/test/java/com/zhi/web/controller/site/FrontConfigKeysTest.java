package com.zhi.web.controller.site;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 前台设置白名单契约。
 *
 * <p>企业官网的品牌能力依赖这些键能被前台匿名读到：导航/登录页的自定义 Logo
 * （{@code site_logo}，回退 {@code blog_avatar}）与首页 Hero 的 Banner
 * （{@code site_banner}）。白名单漏键时后台能保存、前台却永远读不到 —— 静默失效，
 * 所以在这里锁死。</p>
 */
class FrontConfigKeysTest
{
    private static final List<String> KEYS = Arrays.asList(FrontConfigKeys.CONFIG_KEYS);

    @Test
    void brandImageKeysMustBeExposedToFrontend()
    {
        assertTrue(KEYS.contains("site_logo"), "前台需要 site_logo 才能显示自定义 Logo");
        assertTrue(KEYS.contains("site_banner"), "前台需要 site_banner 才能显示首页 Banner");
        assertTrue(KEYS.contains("blog_name"), "品牌名取 blog_name");
    }

    @Test
    void whitelistMustNotContainDuplicates()
    {
        Set<String> unique = new HashSet<>(KEYS);
        assertEquals(KEYS.size(), unique.size(), "白名单存在重复键：会重复读写同一设置");
    }

    @Test
    void removedBlogKeysMustStayOut()
    {
        // 点赞/分享/侧栏/归档已随博客功能下线，白名单不得重新加回
        for (String removed : List.of("like_enabled", "share_enabled", "sidebar_enabled", "nav_archive_enabled"))
        {
            assertTrue(!KEYS.contains(removed), removed + " 已下线，不应出现在前台白名单");
        }
    }
}
