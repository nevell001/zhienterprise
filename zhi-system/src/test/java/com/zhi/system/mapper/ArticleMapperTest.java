package com.zhi.system.mapper;

import com.zhi.system.domain.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 博客文章Mapper单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@MapperScan("com.zhi.system.mapper")
@TestPropertySource(properties = {
    "mybatis.mapperLocations=classpath:mapper/system/ArticleMapper.xml",
    "mybatis.typeAliasesPackage=com.zhi.system.domain",
    "spring.sql.init.mode=always"
})
@Sql(scripts = "/schema.sql")
class ArticleMapperTest {

    @Autowired
    private ArticleMapper articleMapper;

    private Article testArticle;

    @Test
    void insertArticleShouldLetDatabaseGenerateTimestamps() throws Exception {
        String mapperXml = new String(
                getClass().getResourceAsStream("/mapper/system/ArticleMapper.xml").readAllBytes(),
                StandardCharsets.UTF_8);
        int insertStart = mapperXml.indexOf("<insert id=\"insertArticle\"");
        int insertEnd = mapperXml.indexOf("</insert>", insertStart);

        assertTrue(insertStart >= 0, "应存在 insertArticle 映射");
        String insertSql = mapperXml.substring(insertStart, insertEnd);

        assertFalse(insertSql.contains("#{createTime}"), "新增文章不能信任外部 createTime 参数");
        assertFalse(insertSql.contains("#{updateTime}"), "新增文章不能信任外部 updateTime 参数");
        assertTrue(insertSql.contains("NOW()"), "新增文章时间应由数据库当前时间生成");
    }

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testArticle = new Article();
        testArticle.setTitle("Mapper测试文章标题");
        testArticle.setContent("Mapper测试文章内容");
        testArticle.setStatus(0L);
        testArticle.setCategoryId(1L);
    }

    /**
     * 测试CRUD操作的完整流程
     */
    @Test
    void testCRUDOperations() {
        try {
            // 测试新增
            int insertResult = articleMapper.insertArticle(testArticle);
            assertEquals(1, insertResult, "新增文章失败");
            assertNotNull(testArticle.getId(), "新增文章后ID应该不为空");

            // 测试根据ID查询
            Article selectResult = articleMapper.selectArticleById(testArticle.getId());
            assertNotNull(selectResult, "根据ID查询文章失败");
            assertEquals("Mapper测试文章标题", selectResult.getTitle(), "文章标题不匹配");

            // 测试根据标题查询
            Article selectByTitle = articleMapper.selectArticleByTitle("Mapper测试文章标题");
            assertNotNull(selectByTitle, "根据标题查询文章失败");

            // 测试修改
            selectResult.setTitle("修改后的标题");
            int updateResult = articleMapper.updateArticle(selectResult);
            assertEquals(1, updateResult, "修改文章失败");
            Article updatedArticle = articleMapper.selectArticleById(testArticle.getId());
            assertEquals("修改后的标题", updatedArticle.getTitle(), "文章标题修改失败");

            // 测试查询列表
            Article queryParam = new Article();
            queryParam.setTitle("修改后的标题");
            List<Article> articleList = articleMapper.selectArticleList(queryParam);
            assertFalse(articleList.isEmpty(), "查询文章列表失败");

            // 测试浏览量增加
            Long oldViewCount = updatedArticle.getViewCount() != null ? updatedArticle.getViewCount() : 0L;
            articleMapper.addViewCount(testArticle.getId());
            Article viewCountArticle = articleMapper.selectArticleById(testArticle.getId());
            assertEquals(oldViewCount + 1, viewCountArticle.getViewCount(), "浏览量增加失败");

            // 测试删除（逻辑删除，进入回收站）
            int deleteResult = articleMapper.deleteArticleById(testArticle.getId());
            assertEquals(1, deleteResult, "删除文章失败");
            Article deletedArticle = articleMapper.selectArticleById(testArticle.getId());
            assertNotNull(deletedArticle, "逻辑删除后行仍应存在（回收站）");
            assertEquals(2L, deletedArticle.getDelFlag(), "删除标记应为 2（回收站）");
            assertNull(articleMapper.selectArticleByTitle("修改后的标题"),
                    "回收站中的文章不应再占用标题");
        } catch (Exception e) {
            e.printStackTrace();
            fail("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试批量删除功能
     */
    @Test
    void testDeleteArticleByIds() {
        try {
            // 新增两篇文章用于测试
            Article article1 = new Article();
            article1.setTitle("批量删除测试文章1");
            article1.setContent("内容1");
            article1.setStatus(0L);
            article1.setCategoryId(1L);
            
            Article article2 = new Article();
            article2.setTitle("批量删除测试文章2");
            article2.setContent("内容2");
            article2.setStatus(0L);
            article2.setCategoryId(1L);
            
            articleMapper.insertArticle(article1);
            articleMapper.insertArticle(article2);
            
            // 批量删除（逻辑删除）
            int deleteResult = articleMapper.deleteArticleByIds(new Long[]{article1.getId(), article2.getId()});
            assertTrue(deleteResult >= 1, "批量删除文章失败");

            // 默认列表不再返回，回收站视图（delFlag=2）能查到
            assertEquals(0, articleMapper.selectArticleList(new Article()).size(),
                    "默认列表必须排除回收站文章");
            Article recycleQuery = new Article();
            recycleQuery.setDelFlag(2L);
            assertEquals(2, articleMapper.selectArticleList(recycleQuery).size(),
                    "回收站视图应返回已删除文章");
        } catch (Exception e) {
            e.printStackTrace();
            fail("批量删除测试失败: " + e.getMessage());
        }
    }

    /**
     * 回收站往返：软删除 → 恢复 → 彻底删除。
     */
    @Test
    void recycleBinRoundTrip() {
        Article article = new Article();
        article.setTitle("回收站往返测试");
        article.setContent("内容");
        article.setStatus(1L);
        article.setCategoryId(1L);
        articleMapper.insertArticle(article);

        assertEquals(1, articleMapper.deleteArticleByIds(new Long[]{article.getId()}));
        assertEquals(2L, articleMapper.selectArticleById(article.getId()).getDelFlag());

        assertEquals(1, articleMapper.restoreArticleByIds(new Long[]{article.getId()}));
        assertEquals(0L, articleMapper.selectArticleById(article.getId()).getDelFlag(),
                "恢复后应回到未删除状态，前台列表重新可见");
        assertEquals(1, articleMapper.selectArticleList(new Article()).stream()
                .filter(a -> a.getId().equals(article.getId())).count());

        assertEquals(1, articleMapper.deleteArticleById(article.getId()));
        assertEquals(1, articleMapper.purgeArticleByIds(new Long[]{article.getId()}));
        assertNull(articleMapper.selectArticleById(article.getId()),
                "彻底删除后行应被物理移除");

        // 不在回收站的 ID 不允许被彻底删除
        Article alive = new Article();
        alive.setTitle("在线文章");
        alive.setContent("内容");
        alive.setStatus(1L);
        articleMapper.insertArticle(alive);
        assertEquals(0, articleMapper.purgeArticleByIds(new Long[]{alive.getId()}),
                "purge 只能删除回收站中的文章");
        assertNotNull(articleMapper.selectArticleById(alive.getId()));
    }

    /**
     * 核心业务列表按 articleTypes 同时取 business 与 case（客户案例入口）。
     */
    @Test
    void selectArticleListShouldFilterByArticleTypes() {
        Article business = new Article();
        business.setTitle("多类型查询-业务");
        business.setContent("内容");
        business.setStatus(1L);
        business.setCategoryId(1L);
        business.setArticleType("business");
        articleMapper.insertArticle(business);

        Article caseArticle = new Article();
        caseArticle.setTitle("多类型查询-案例");
        caseArticle.setContent("内容");
        caseArticle.setStatus(1L);
        caseArticle.setCategoryId(1L);
        caseArticle.setArticleType("case");
        articleMapper.insertArticle(caseArticle);

        Article news = new Article();
        news.setTitle("多类型查询-资讯");
        news.setContent("内容");
        news.setStatus(1L);
        news.setCategoryId(1L);
        news.setArticleType("news");
        articleMapper.insertArticle(news);

        Article query = new Article();
        query.setArticleTypes(List.of("business", "case"));
        List<String> titles = articleMapper.selectArticleList(query).stream()
                .map(Article::getTitle)
                .toList();

        assertTrue(titles.contains("多类型查询-业务"), "business 文章应命中");
        assertTrue(titles.contains("多类型查询-案例"), "case 文章应命中");
        assertFalse(titles.contains("多类型查询-资讯"), "news 文章不应命中");
    }

    /**
     * 测试搜索功能
     */
    @Test
    void testSearchArticles() {
        try {
            // 确保有测试数据 - 注意：搜索查询要求 status = 1（已发布）
            Article searchArticle = new Article();
            searchArticle.setTitle("搜索测试文章");
            searchArticle.setContent("这是一篇用于测试搜索功能的文章");
            searchArticle.setStatus(1L);  // 设置为已发布状态
            searchArticle.setCategoryId(1L);

            int insertResult = articleMapper.insertArticle(searchArticle);
            assertEquals(1, insertResult, "插入文章失败");

            // 验证 ID 已生成
            assertNotNull(searchArticle.getId(), "插入后ID应该不为空");

            // 测试搜索
            Article queryParam = new Article();
            queryParam.setStatus(1L);  // 搜索已发布的文章
            List<Article> searchResults = articleMapper.searchArticles("搜索测试", queryParam);

            // 打印调试信息
            System.out.println("插入的文章ID: " + searchArticle.getId());
            System.out.println("搜索结果数量: " + searchResults.size());
            searchResults.forEach(article -> System.out.println("搜索结果: ID=" + article.getId() + ", Title=" + article.getTitle()));

            boolean found = searchResults.stream()
                    .anyMatch(article -> article.getId().equals(searchArticle.getId()));

            assertTrue(found, "搜索功能未找到预期文章");

            // 清理数据
            if (searchArticle.getId() != null) {
                articleMapper.deleteArticleById(searchArticle.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("搜索测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试统计功能
     */
    @Test
    void testStatisticsFunctions() {
        // 测试文章数量统计
        Article queryParam = new Article();
        queryParam.setStatus(0L);
        Long articleCount = articleMapper.selectArticleCount(queryParam);
        assertNotNull(articleCount, "文章数量统计失败");
        assertTrue(articleCount >= 0, "文章数量不能为负数");

        // 测试总浏览量统计 - 注意：没有数据时可能返回 null
        Long totalViewCount = articleMapper.selectTotalViewCount();
        // 当没有文章时，SUM 返回 NULL 是正常的
        if (articleCount > 0) {
            assertNotNull(totalViewCount, "有文章时总浏览量不应为null");
            assertTrue(totalViewCount >= 0, "总浏览量不能为负数");
        }

        // 测试平均浏览量统计 - 注意：没有数据时可能返回 null
        Double averageViewCount = articleMapper.selectAverageViewCount();
        // 当没有文章时，AVG 返回 NULL 是正常的
        if (articleCount > 0) {
            assertNotNull(averageViewCount, "有文章时平均浏览量不应为null");
            assertTrue(averageViewCount >= 0, "平均浏览量不能为负数");
        }
    }

    /**
     * 归档查询按创建月份分组，只统计已发布（status=1）且未删除（del_flag=0）的文章，
     * 并按月份倒序返回。DATE_FORMAT 由 schema.sql 里的 H2 别名提供，
     * 所以这条 SQL 是真的被执行、真的在断言结果，而不是被禁用。
     */
    @Test
    @Sql(scripts = "/schema.sql", statements = {
        "DELETE FROM zhi_article",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('归档-1月A', 1, '0', '2026-01-15 10:00:00')",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('归档-1月B', 1, '0', '2026-01-28 10:00:00')",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('归档-2月A', 1, '0', '2026-02-03 10:00:00')",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('草稿', 0, '0', '2026-02-04 10:00:00')",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('已删除', 1, '1', '2026-02-05 10:00:00')"
    })
    void getArticleArchive按创建月份分组并排除草稿与已删除() {
        List<Map<String, Object>> archiveList = articleMapper.getArticleArchive();

        assertEquals(2, archiveList.size(), "应只聚出 2026-01 与 2026-02 两个分组，草稿和已删除文章不参与");
        assertEquals("2026-02", String.valueOf(firstValue(archiveList.get(0), "archive_date")),
                "倒序排列，第一个分组应为最近月份");
        assertEquals(1L, ((Number) firstValue(archiveList.get(0), "article_count")).longValue(),
                "2026-02 只有 1 篇已发布文章");
        assertEquals("2026-01", String.valueOf(firstValue(archiveList.get(1), "archive_date")));
        assertEquals(2L, ((Number) firstValue(archiveList.get(1), "article_count")).longValue(),
                "2026-01 的 2 篇已发布文章应归入同一分组");
    }

    /** H2 返回的列标签是大写，MySQL 是小写，取键时忽略大小写以免绑定数据库实现。 */
    private static Object firstValue(Map<String, Object> row, String column) {
        return row.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(column))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("归档行缺少列 " + column + "，实际键: " + row.keySet()));
    }

    /**
     * 列表排序必须可重现：create_time 完全相同的并列行以 id 倒序兜底。
     * 否则分页（LIMIT/OFFSET 分次执行、每次重新排序）会把并列行随机分布在两页，
     * 出现「一篇文章重复显示、另一篇消失」（2026-10-04 首页问题）。
     */
    @Test
    @Sql(scripts = "/schema.sql", statements = {
        "DELETE FROM zhi_article",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('并列-A', 1, '0', '2026-10-04 12:38:14')",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('并列-B', 1, '0', '2026-10-04 12:38:14')",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('并列-C', 1, '0', '2026-10-04 12:38:14')"
    })
    void selectArticleListSameCreateTimeMustSortByIdDesc() {
        Article queryParam = new Article();
        queryParam.setStatus(1L);
        List<Article> list = articleMapper.selectArticleList(queryParam);

        assertEquals(3, list.size(), "应返回全部已发布文章");
        List<Long> ids = list.stream().map(Article::getId).toList();
        List<Long> expected = ids.stream().sorted(Comparator.reverseOrder()).toList();
        assertEquals(expected, ids, "create_time 相同的文章必须按 id 倒序稳定排列");
    }

    /**
     * 标题查重只看未删除行：已删除文章不应占用标题（可复用），
     * 未删除行仍能按标题查到。
     */
    @Test
    @Sql(scripts = "/schema.sql", statements = {
        "DELETE FROM zhi_article",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('已删除的标题', 1, '1', '2026-10-04 12:00:00')",
        "INSERT INTO zhi_article (title, status, del_flag, create_time) VALUES ('在用标题', 1, '0', '2026-10-04 12:01:00')"
    })
    void selectArticleByTitleMustIgnoreDeletedRows() {
        assertNull(articleMapper.selectArticleByTitle("已删除的标题"),
                "已删除文章不应占用标题");
        assertNotNull(articleMapper.selectArticleByTitle("在用标题"),
                "未删除文章必须能按标题查到");
    }

    /**
     * 置顶文章必须浮到列表最前（is_top 优先于 create_time），
     * 其余文章仍按创建时间倒序。此前列表只按 create_time 排序，
     * 后台的置顶操作在前台完全不可见（2026-10-04）。
     */
    @Test
    @Sql(scripts = "/schema.sql", statements = {
        "DELETE FROM zhi_article",
        "INSERT INTO zhi_article (title, status, del_flag, is_top, create_time) VALUES ('普通-新', 1, '0', 0, '2026-10-04 13:00:00')",
        "INSERT INTO zhi_article (title, status, del_flag, is_top, create_time) VALUES ('置顶-旧', 1, '0', 1, '2026-10-01 10:00:00')",
        "INSERT INTO zhi_article (title, status, del_flag, is_top, create_time) VALUES ('普通-中', 1, '0', 0, '2026-10-03 10:00:00')"
    })
    void selectArticleListPinnedArticleMustFloatToTop() {
        Article queryParam = new Article();
        queryParam.setStatus(1L);
        List<Article> list = articleMapper.selectArticleList(queryParam);

        assertEquals(3, list.size());
        assertEquals("置顶-旧", list.get(0).getTitle(), "置顶文章必须排最前，即使创建时间更早");
        assertEquals("普通-新", list.get(1).getTitle(), "非置顶文章按创建时间倒序");
        assertEquals("普通-中", list.get(2).getTitle());
    }

    /**
     * 源码契约：mapper 里所有行级查询的 ORDER BY 都必须以 id 兜底，
     * 防止并列值（如相同 create_time）在分页/多次请求间随机排序。
     * 例外：GROUP BY 聚合结果（archive_date / month，天然无并列行）与已按唯一列 id 排序的查询。
     */
    @Test
    void everyOrderByClauseMustBeDeterministic() throws Exception {
        String mapperXml = new String(
                getClass().getResourceAsStream("/mapper/system/ArticleMapper.xml").readAllBytes(),
                StandardCharsets.UTF_8);
        List<String> badClauses = new java.util.ArrayList<>();
        for (String line : mapperXml.split("\n")) {
            String trimmed = line.trim().toLowerCase();
            if (!trimmed.startsWith("order by")) {
                continue;
            }
            boolean byUniqueId = trimmed.matches("order by .*\\bid (desc|asc)( limit \\d+)?");
            boolean aggregate = trimmed.matches("order by (archive_date|month) desc");
            if (!byUniqueId && !aggregate) {
                badClauses.add(trimmed);
            }
        }
        assertTrue(badClauses.isEmpty(), "以下 ORDER BY 不确定，分页/列表会出现重复或乱序: " + badClauses);
    }
}
