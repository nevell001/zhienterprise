package com.zhi.system.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文章列表查询的形状守卫（读 mapper XML，属"跨产物一致性"检查，不是行为断言）。
 *
 * <p>审计问题：列表类查询把 longtext 正文一起 select，还为两个用不到的标签表做了
 * LEFT JOIN 并用 GROUP BY 去重 —— PageHelper 的 count 查询会被再套一层，MySQL 需要
 * 建包含 longtext 的临时表；正文本身也只是白占内存与网络。</p>
 */
class ArticleListQueryShapeTest
{
    private static final Path XML = Path.of("src/main/resources/mapper/system/ArticleMapper.xml");

    /** 列表类查询：不许投影正文、不许 GROUP BY */
    private static final List<String> LIST_QUERIES = List.of(
        "selectArticleList", "selectArticlesByTagId", "searchArticles", "searchArticlesFullText");

    private String readXml() throws Exception
    {
        return Files.readString(XML);
    }

    private String selectBlock(String xml, String id)
    {
        int start = xml.indexOf("<select id=\"" + id + "\"");
        assertTrue(start >= 0, "缺少查询 " + id);
        return xml.substring(start, xml.indexOf("</select>", start));
    }

    @Test
    @DisplayName("列表查询不得投影 longtext 正文，也不得有 GROUP BY")
    void listQueriesMustNotCarryContent() throws Exception
    {
        String xml = readXml();
        for (String id : LIST_QUERIES)
        {
            String block = selectBlock(xml, id);
            assertFalse(block.contains("ba.summary, ba.content"),
                id + " 不该把正文放进列表投影（列表页只用摘要）");
            assertFalse(block.toLowerCase().contains("group by"),
                id + " 的 GROUP BY 只为标签 JOIN 去重，去掉 JOIN 后必须一并去掉");
        }
    }

    @Test
    @DisplayName("标签 JOIN 只允许出现在按标签过滤的查询里")
    void tagJoinOnlyWhereUsedForFiltering() throws Exception
    {
        String xml = readXml();
        for (String id : List.of("selectArticleList", "searchArticles", "searchArticlesFullText",
            "selectHotArticles", "selectArticlesByArchive"))
        {
            assertFalse(selectBlock(xml, id).contains("zhi_article_tag"),
                id + " 不使用标签表，JOIN 纯属浪费（标签由 loadTagsForArticles 单独批量查）");
            assertFalse(selectBlock(xml, id).toLowerCase().contains("group by"),
                id + " 去掉标签 JOIN 后 GROUP BY 也必须一并去掉");
        }
        assertTrue(selectBlock(xml, "selectArticlesByTagId").contains("inner join zhi_article_tag"),
            "按标签过滤的查询需要 zhi_article_tag");
    }

    @Test
    @DisplayName("热门列表不投影正文；归档页要留正文（无摘要时前端回退截断）")
    void hotDropsContentButArchiveKeepsIt() throws Exception
    {
        String xml = readXml();

        assertFalse(selectBlock(xml, "selectHotArticles").contains("ba.content"),
            "热门文章只出标题/摘要，侧栏与相关推荐都不读正文");
        assertTrue(selectBlock(xml, "selectArticlesByArchive").contains("ba.content"),
            "归档页用 summary || content 截断做预览，正文不能去掉");
    }

    @Test
    @DisplayName("单条查询不许为标签 JOIN 扇出（否则 selectOne 直接抛 TooManyResults）")
    void singleRowQueryMustNotFanOutOnTags() throws Exception
    {
        String block = selectBlock(readXml(), "selectArticleById");

        // 曾经只删掉 GROUP BY 却留着两个标签 JOIN：多标签文章返回 N 行，
        // 前台 /api/article/{id} 与后台详情一律 500
        assertFalse(block.contains("zhi_article_tag"),
            "selectArticleById 不投影标签列，JOIN 会让一行变多行");
        assertFalse(block.toLowerCase().contains("group by"),
            "去掉标签 JOIN 后 GROUP BY 也应一并去掉");
    }

    @Test
    @DisplayName("导出查询必须保留正文（Excel 有\"文章内容\"列）")
    void exportQueryKeepsContent() throws Exception
    {
        String block = selectBlock(readXml(), "selectArticleListForExport");

        assertTrue(block.contains("ba.content"), "导出需要正文列");
        assertFalse(block.toLowerCase().contains("group by"), "导出不需要 GROUP BY");
        // 只读校验：只查 SQL 语句本身，避免 `ba.update_time` 这类列名被误判
        String lower = block.toLowerCase();
        assertFalse(lower.contains("delete from") || lower.contains("drop table") || lower.contains("update zhi_article"),
            "导出查询只能是只读 SELECT");
        assertTrue(lower.contains("select ba.id"), "导出查询应当是 SELECT");
    }
}
