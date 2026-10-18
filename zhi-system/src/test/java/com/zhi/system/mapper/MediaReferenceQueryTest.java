package com.zhi.system.mapper;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import com.zhi.system.domain.MediaReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 媒体库引用查询的 SQL 契约测试：每条查询都必须能命中真实表结构里的存储位置，
 * 且不把已删除的友链/用户算作引用。只有让 SQL 跑在真实列名上才能发现拼写错误。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@MapperScan("com.zhi.system.mapper")
@TestPropertySource(properties = {
    "mybatis.mapperLocations=classpath:mapper/system/ArticleMapper.xml,classpath:mapper/system/PageMapper.xml,"
        + "classpath:mapper/system/SettingMapper.xml,classpath:mapper/system/FriendLinkMapper.xml,"
        + "classpath:mapper/system/SysUserMapper.xml",
    "mybatis.typeAliasesPackage=com.zhi.system.domain,com.zhi.common.core.domain.entity",
    "mybatis.configuration.map-underscore-to-camel-case=true",
    "spring.sql.init.mode=always"
})
@Sql(scripts = { "/schema.sql", "/schema-sys-user.sql", "/schema-media-reference.sql" })
class MediaReferenceQueryTest
{
    private static final String FILE = "logo_1.png";

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private SettingMapper settingMapper;

    @Autowired
    private FriendLinkMapper friendLinkMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("正文、封面、图集、页面区块、设置、友链、用户头像都能被引用检查命中")
    void referencesAreFoundInEveryStorageLocation()
    {
        jdbcTemplate.update("insert into zhi_article (title, content) values (?, ?)",
            "正文引用", "<img src=\"/profile/upload/2026/10/18/" + FILE + "\">");
        jdbcTemplate.update("insert into zhi_article (title, cover_url) values (?, ?)",
            "封面引用", "/profile/upload/2026/10/18/" + FILE);
        jdbcTemplate.update("insert into zhi_article (title) values (?)", "无关文章");

        jdbcTemplate.update("insert into zhi_page (title, sections) values (?, ?)",
            "首页", "[{\"img\":\"/profile/upload/2026/10/18/" + FILE + "\"}]");
        jdbcTemplate.update("insert into zhi_setting (config_key, config_value) values (?, ?)",
            "site_logo", "/profile/upload/2026/10/18/" + FILE);
        jdbcTemplate.update("insert into zhi_friend_link (name, logo, del_flag) values (?, ?, '0')",
            "合作伙伴", "/profile/upload/2026/10/18/" + FILE);
        jdbcTemplate.update("insert into zhi_friend_link (name, logo, del_flag) values (?, ?, '1')",
            "已删除友链", "/profile/upload/2026/10/18/" + FILE);
        jdbcTemplate.update("insert into sys_user (user_name, avatar, del_flag) values (?, ?, '0')",
            "alice", "/profile/upload/2026/10/18/" + FILE);
        jdbcTemplate.update("insert into sys_user (user_name, avatar, del_flag) values (?, ?, '2')",
            "deleted", "/profile/upload/2026/10/18/" + FILE);

        List<MediaReference> articles = articleMapper.selectMediaReferences(FILE);
        assertEquals(2, articles.size(), "正文与封面都应算引用");
        assertTrue(articles.stream().allMatch(reference -> "article".equals(reference.getReferenceType())));
        assertTrue(articles.stream().anyMatch(reference -> "正文引用".equals(reference.getReferenceTitle())));

        List<MediaReference> pages = pageMapper.selectMediaReferences(FILE);
        assertEquals(1, pages.size());
        assertEquals("首页", pages.get(0).getReferenceTitle());

        List<MediaReference> settings = settingMapper.selectMediaReferences(FILE);
        assertEquals(1, settings.size());
        assertEquals("site_logo", settings.get(0).getReferenceTitle());

        List<MediaReference> friendLinks = friendLinkMapper.selectMediaReferences(FILE);
        assertEquals(1, friendLinks.size(), "已删除友链不应算引用");
        assertEquals("合作伙伴", friendLinks.get(0).getReferenceTitle());

        List<MediaReference> users = sysUserMapper.selectMediaReferences(FILE);
        assertEquals(1, users.size(), "已删除用户不应算引用");
        assertEquals("alice", users.get(0).getReferenceTitle());
    }

    @Test
    @DisplayName("没有引用时所有查询都返回空")
    void referencesAreEmptyWhenFileIsUnused()
    {
        jdbcTemplate.update("insert into zhi_article (title, content) values (?, ?)", "无关", "无图片");

        assertTrue(articleMapper.selectMediaReferences(FILE).isEmpty());
        assertTrue(pageMapper.selectMediaReferences(FILE).isEmpty());
        assertTrue(settingMapper.selectMediaReferences(FILE).isEmpty());
        assertTrue(friendLinkMapper.selectMediaReferences(FILE).isEmpty());
        assertTrue(sysUserMapper.selectMediaReferences(FILE).isEmpty());
    }
}
