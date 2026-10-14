package com.zhi.system.mapper;

import com.zhi.system.domain.Comment;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 前台评论查询（selectFrontCommentList）的真实 SQL 测试。
 *
 * <p>前台按「顶级评论分页 + 回复挂在楼层下」取数，顶级判定依赖 parent_id 的建表默认值 0
 * （生产插入顶级评论时省略该列）。mock 掉 Service 的控制器测试发现不了这类 SQL 语义问题，
 * 所以这里在 H2 上真实执行 mapper 语句并断言结果。</p>
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@MapperScan("com.zhi.system.mapper")
@TestPropertySource(properties = {
    "mybatis.mapperLocations=classpath:mapper/system/CommentMapper.xml",
    "mybatis.typeAliasesPackage=com.zhi.system.domain",
    "spring.sql.init.mode=always"
})
// 方法级 @Sql 会取代类级 @Sql（未声明 @SqlMergeMode(MERGE)），带 statements 的方法级注解必须自带建表脚本
@Sql(scripts = "/schema-zhi-comment.sql")
class CommentMapperFrontQueryTest {

    @Autowired
    private CommentMapper commentMapper;

    /**
     * 走生产插入路径（parentId 为 null，mapper 省略该列）新增的顶级评论必须能被前台查出来：
     * 一旦顶级条件写成「仅 is null」，前台评论列表会永远为空。
     * 同时验证头像 left join / COALESCE 的行为。
     */
    @Test
    @Sql(scripts = "/schema-zhi-comment.sql", statements = {
        "DELETE FROM zhi_comment",
        "DELETE FROM sys_user",
        "INSERT INTO sys_user (user_id, avatar) VALUES (1, '/profile/avatar/1.png')",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (1, '匿名人', '顶级-匿名', 1, '2026-10-01 09:00:00')"
    })
    void topLevelCommentInsertedWithoutParentIdMustBeVisibleToFrontQuery() {
        Comment top = new Comment();
        top.setArticleId(1L);
        top.setUserId(1L);
        top.setNickname("注册用户");
        top.setContent("顶级-注册");
        top.setStatus("1");
        commentMapper.insertComment(top);

        Comment reply = new Comment();
        reply.setArticleId(1L);
        reply.setNickname("回复者");
        reply.setContent("一条回复");
        reply.setParentId(top.getId());
        reply.setStatus("1");
        commentMapper.insertComment(reply);

        List<Comment> rows = commentMapper.selectFrontCommentList(frontQuery("newest", true));

        assertEquals(List.of("顶级-注册", "顶级-匿名"), contents(rows),
                "省略 parent_id 插入的顶级评论（落库默认 0）必须出现在顶级查询里，回复不出现");

        Comment registered = byContent(rows, "顶级-注册");
        assertEquals("/profile/avatar/1.png", registered.getAvatar(), "登录用户头像应来自 sys_user 的 left join");
        assertNotNull(registered.getParentId(), "顶级评论落库应为建表默认值而不是 null");
        assertEquals(0L, registered.getParentId().longValue(), "H2 建表默认 0 必须与生产 DDL 一致");
        assertEquals("", byContent(rows, "顶级-匿名").getAvatar(), "匿名评论（无 user_id）头像应 COALESCE 成空串");
    }

    /**
     * 默认按创建时间倒序；sort=oldest 时按正序（证明 OGNL 参数分支真实生效）。
     */
    @Test
    @Sql(scripts = "/schema-zhi-comment.sql", statements = {
        "DELETE FROM zhi_comment",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (1, '甲', '顶级-早', 1, '2026-10-01 10:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (1, '乙', '顶级-中', 1, '2026-10-02 10:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (1, '丙', '顶级-晚', 1, '2026-10-03 10:00:00')"
    })
    void topLevelDefaultSortNewestFirstAndOldestOptionAscending() {
        assertEquals(List.of("顶级-晚", "顶级-中", "顶级-早"),
                contents(commentMapper.selectFrontCommentList(frontQuery("newest", true))),
                "默认应按创建时间倒序");
        assertEquals(List.of("顶级-早", "顶级-中", "顶级-晚"),
                contents(commentMapper.selectFrontCommentList(frontQuery("oldest", true))),
                "sort=oldest 应按创建时间正序");
    }

    /**
     * create_time 完全相同时以 id 兜底，保证分页每次执行顺序一致（跨页不重不漏）。
     */
    @Test
    @Sql(scripts = "/schema-zhi-comment.sql", statements = {
        "DELETE FROM zhi_comment",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (1, '甲', '并列-先插入', 1, '2026-10-04 12:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (1, '乙', '并列-后插入', 1, '2026-10-04 12:00:00')"
    })
    void sameCreateTimeMustFallBackToIdOrder() {
        assertEquals(List.of("并列-后插入", "并列-先插入"),
                contents(commentMapper.selectFrontCommentList(frontQuery("newest", true))),
                "倒序时 create_time 并列应按 id 倒序兜底");
        assertEquals(List.of("并列-先插入", "并列-后插入"),
                contents(commentMapper.selectFrontCommentList(frontQuery("oldest", true))),
                "正序时 create_time 并列应按 id 正序兜底");
    }

    /**
     * 回复查询（楼中楼也算回复）固定按时间正序，恢复对话先后；且不含顶级评论。
     */
    @Test
    @Sql(scripts = "/schema-zhi-comment.sql", statements = {
        "DELETE FROM zhi_comment",
        "INSERT INTO zhi_comment (article_id, nickname, content, parent_id, status, create_time) VALUES (1, '楼主', '顶级', 0, 1, '2026-10-01 09:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, parent_id, status, create_time) VALUES (1, '回复乙', '第二条回复', 10, 1, '2026-10-01 11:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, parent_id, status, create_time) VALUES (1, '回复甲', '第一条回复', 10, 1, '2026-10-01 10:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, parent_id, status, create_time) VALUES (1, '楼中楼', '回复的回复', 11, 1, '2026-10-01 12:00:00')"
    })
    void repliesOnlyMustReturnParentedRepliesAscending() {
        List<Comment> rows = commentMapper.selectFrontCommentList(frontQuery("oldest", false));

        assertEquals(List.of("第一条回复", "第二条回复", "回复的回复"), contents(rows),
                "回复查询应只含 parent_id > 0 的回复（含楼中楼）并按时间正序");
    }

    /**
     * 状态与文章过滤：前台只展示本文章的已发布评论；计数口径为「本文章已发布评论总数（含回复）」。
     */
    @Test
    @Sql(scripts = "/schema-zhi-comment.sql", statements = {
        "DELETE FROM zhi_comment",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (1, '甲', '已发布', 1, '2026-10-01 10:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, parent_id, status, create_time) VALUES (1, '乙', '已发布的回复', 1, 1, '2026-10-01 11:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (1, '丙', '待审核', 0, '2026-10-01 12:00:00')",
        "INSERT INTO zhi_comment (article_id, nickname, content, status, create_time) VALUES (2, '丁', '其他文章的评论', 1, '2026-10-01 13:00:00')"
    })
    void statusAndArticleFilterMustExcludeOtherStatesAndArticles() {
        List<Comment> rows = commentMapper.selectFrontCommentList(frontQuery("newest", true));

        assertEquals(List.of("已发布"), contents(rows),
                "顶级查询应排除待审核、其他文章的评论以及所有回复");

        Comment countQuery = new Comment();
        countQuery.setArticleId(1L);
        countQuery.setStatus("1");
        assertEquals(2L, commentMapper.selectCommentCount(countQuery),
                "文章计数应为本文章已发布评论总数（含回复），与前台标题口径一致");
    }

    private static Comment frontQuery(String sort, boolean topLevelOnly) {
        Comment query = new Comment();
        query.setArticleId(1L);
        query.setStatus("1");
        query.getParams().put(topLevelOnly ? "topLevelOnly" : "repliesOnly", Boolean.TRUE);
        query.getParams().put("sort", sort);
        return query;
    }

    private static List<String> contents(List<Comment> rows) {
        return rows.stream().map(Comment::getContent).toList();
    }

    private static Comment byContent(List<Comment> rows, String content) {
        return rows.stream().filter(row -> content.equals(row.getContent())).findFirst()
                .orElseThrow(() -> new AssertionError("结果中缺少评论: " + content));
    }
}
