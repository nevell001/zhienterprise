package com.zhi.system.mapper;

import com.zhi.system.domain.ArticleRevision;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章修订历史Mapper测试（H2）。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@MapperScan("com.zhi.system.mapper")
@TestPropertySource(properties = {
    "mybatis.mapperLocations=classpath:mapper/system/ArticleRevisionMapper.xml",
    "mybatis.typeAliasesPackage=com.zhi.system.domain",
    "spring.sql.init.mode=always"
})
@Sql(scripts = "/schema.sql")
class ArticleRevisionMapperTest {

    @Autowired
    private ArticleRevisionMapper articleRevisionMapper;

    @Test
    void revisionRoundTripAndPrune() {
        insertRevision(1L, 1, "v1", "内容1");
        insertRevision(1L, 2, "v2", "内容2");
        insertRevision(1L, 3, "v3", "内容3");
        insertRevision(2L, 1, "另一篇v1", "内容");

        List<ArticleRevision> list = articleRevisionMapper.selectRevisionList(1L);
        assertEquals(3, list.size());
        assertEquals(3, list.get(0).getRevisionNo(), "列表按版本号倒序");
        assertNull(list.get(0).getContent(), "列表不应返回正文大字段");

        ArticleRevision latest = articleRevisionMapper.selectLatestRevision(1L);
        assertNotNull(latest);
        assertEquals(3, latest.getRevisionNo());
        assertEquals("内容3", latest.getContent());

        ArticleRevision detail = articleRevisionMapper.selectRevisionById(latest.getId());
        assertNotNull(detail);
        assertEquals("v3", detail.getTitle());

        assertEquals(1, articleRevisionMapper.deleteRevisionsUpTo(1L, 1));
        assertEquals(2, articleRevisionMapper.selectRevisionList(1L).size());

        assertEquals(3, articleRevisionMapper.deleteRevisionsByArticleIds(new Long[]{1L, 2L}));
        assertEquals(0, articleRevisionMapper.selectRevisionList(1L).size());
        assertNull(articleRevisionMapper.selectLatestRevision(2L));
    }

    private void insertRevision(Long articleId, int revisionNo, String title, String content) {
        ArticleRevision revision = new ArticleRevision();
        revision.setArticleId(articleId);
        revision.setRevisionNo(revisionNo);
        revision.setTitle(title);
        revision.setContent(content);
        revision.setFormat("html");
        revision.setStatus(1L);
        revision.setIsTop(0L);
        revision.setIsRecommend(0L);
        revision.setEditorId(7L);
        revision.setEditorName("编辑");
        revision.setCreateTime(new Date());
        assertEquals(1, articleRevisionMapper.insertRevision(revision));
        assertNotNull(revision.getId());
    }
}
