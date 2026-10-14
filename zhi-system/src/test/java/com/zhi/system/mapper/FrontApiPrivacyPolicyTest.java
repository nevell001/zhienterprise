package com.zhi.system.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 前台公开接口的字段暴露守卫。
 *
 * <p>小程序/前台接口用 @Anonymous 暴露给所有人，任何 PII（邮箱/IP）都不允许出现在
 * 返回字段里；这里直接校验 Mapper 里前台语句的字段列表。</p>
 */
class FrontApiPrivacyPolicyTest {

    private static String readMapper(String name) throws IOException {
        try (InputStream in = FrontApiPrivacyPolicyTest.class.getClassLoader()
                .getResourceAsStream("mapper/system/" + name)) {
            assertNotNull(in, "classpath 缺少 mapper/system/" + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 取某个 <select id="..."> 到下一个 </select> 之间的文本 */
    private static String statement(String xml, String id) {
        String marker = "<select id=\"" + id + "\"";
        int start = xml.indexOf(marker);
        assertTrue(start >= 0, "找不到语句 " + id);
        int end = xml.indexOf("</select>", start);
        return xml.substring(start, end);
    }

    @Test
    @DisplayName("前台评论列表不得返回评论者 email")
    void frontCommentListMustNotExposeEmail() throws IOException {
        String sql = statement(readMapper("CommentMapper.xml"), "selectFrontCommentList");
        assertFalse(sql.contains("email"),
            "selectFrontCommentList 是 @Anonymous 接口，不能带出评论者 email：\n" + sql);
        assertFalse(sql.contains("c.ip") || sql.contains("user_agent"),
            "前台评论列表不应带出 IP / User-Agent");
    }

    @Test
    @DisplayName("前台友链列表不得返回申请者 email")
    void frontFriendLinkListMustNotExposeEmail() throws IOException {
        String sql = statement(readMapper("FriendLinkMapper.xml"), "selectFrontFriendLinkList");
        assertFalse(sql.contains("email"),
            "selectFrontFriendLinkList 是 @Anonymous 接口，不能带出申请者 email：\n" + sql);
        assertTrue(sql.contains("select id, name, url"),
            "前台友链列表应使用显式字段列表而不是共享 VO（共享 VO 里含 email）");
    }
}
