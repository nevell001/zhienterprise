package com.zhi.system.mapper;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文章「上一篇/下一篇」查询契约（SQL ↔ 控制器前置条件的跨产物校验）。
 *
 * <p>真实缺陷：`FrontController#getArticleDetail` 要求
 * `prevArticle.getStatus() != null && getStatus().equals(1L)` 才把导航项放进
 * `extraInfo`，但 `SimpleArticleResult` 与两条导航 SQL 只取了 `id/title/del_flag`，
 * 实体 `status` 恒为 null —— 结果「上一篇/下一篇」永远不返回，详情页导航静默消失，
 * 前端拿不到任何报错。这里锁死 SQL 必须带上 status。</p>
 */
class ArticleNavigationQueryTest
{
    private static String mapperXml() throws Exception
    {
        try (InputStream in = ArticleNavigationQueryTest.class
            .getResourceAsStream("/mapper/system/ArticleMapper.xml"))
        {
            assertTrue(in != null, "找不到 ArticleMapper.xml");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void navigationResultMapMustMapStatus() throws Exception
    {
        String xml = mapperXml();
        int start = xml.indexOf("id=\"SimpleArticleResult\"");
        assertTrue(start > 0, "SimpleArticleResult 结果映射缺失");

        String block = xml.substring(start, xml.indexOf("</resultMap>", start));
        assertTrue(block.contains("property=\"status\""),
            "SimpleArticleResult 必须映射 status：控制器靠它判断是否已发布");
    }

    @Test
    void navigationQueriesMustSelectStatus() throws Exception
    {
        String xml = mapperXml();

        for (String id : new String[] { "getPrevArticle", "getNextArticle" })
        {
            int start = xml.indexOf("id=\"" + id + "\"");
            assertTrue(start > 0, id + " 查询缺失");

            String block = xml.substring(start, xml.indexOf("</select>", start));
            assertTrue(block.contains("status"),
                id + " 的 SELECT 必须包含 status 列，否则控制器判定未发布、导航项被丢弃");
        }
    }
}
