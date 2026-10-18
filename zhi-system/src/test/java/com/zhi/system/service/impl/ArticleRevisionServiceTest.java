package com.zhi.system.service.impl;

import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.common.core.redis.RedisCache;
import com.zhi.common.exception.ServiceException;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.domain.Article;
import com.zhi.system.domain.ArticleRevision;
import com.zhi.system.mapper.ArticleMapper;
import com.zhi.system.mapper.ArticleRevisionMapper;
import com.zhi.system.mapper.ArticleTagMapper;
import com.zhi.system.mapper.TagMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 文章修订历史服务测试：保存快照、去重、回滚与冲突校验。
 */
@ExtendWith(MockitoExtension.class)
class ArticleRevisionServiceTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleRevisionMapper articleRevisionMapper;

    @Mock
    private ArticleTagMapper articleTagMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private ArticleServiceImpl articleService;

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void mockLoginUser() {
        securityUtils = mockStatic(SecurityUtils.class);
        LoginUser loginUser = new LoginUser();
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setNickName("编辑");
        loginUser.setUser(user);
        securityUtils.when(SecurityUtils::getLoginUser).thenReturn(loginUser);
    }

    @AfterEach
    void closeStaticMock() {
        securityUtils.close();
    }

    @Test
    void insertArticleCreatesInitialRevision() {
        Article input = new Article();
        input.setTitle("初始标题");
        input.setContent("初始内容");

        when(articleMapper.selectArticleByTitle("初始标题")).thenReturn(null);
        when(articleMapper.insertArticle(input)).thenAnswer(invocation -> {
            input.setId(9L);
            return 1;
        });
        Article saved = article(9L, "初始标题", "初始内容");
        when(articleMapper.selectArticleById(9L)).thenReturn(saved);
        when(articleRevisionMapper.selectLatestRevision(9L)).thenReturn(null);

        assertEquals(1, articleService.insertArticle(input));

        ArgumentCaptor<ArticleRevision> captor = ArgumentCaptor.forClass(ArticleRevision.class);
        verify(articleRevisionMapper).insertRevision(captor.capture());
        assertEquals(1, captor.getValue().getRevisionNo());
        assertEquals(9L, captor.getValue().getArticleId());
        assertEquals("初始标题", captor.getValue().getTitle());
        assertEquals(7L, captor.getValue().getEditorId());
    }

    @Test
    void updateArticleSkipsRevisionWhenContentUnchanged() {
        Article input = new Article();
        input.setId(1L);
        input.setTitle("标题");
        input.setContent("内容");

        Article current = article(1L, "标题", "内容");
        ArticleRevision latest = revision(5L, 1L, 1, "标题", "内容");

        when(articleMapper.selectArticleByTitle("标题")).thenReturn(null);
        when(articleMapper.updateArticle(input)).thenReturn(1);
        when(articleMapper.selectArticleById(1L)).thenReturn(current);
        when(articleRevisionMapper.selectLatestRevision(1L)).thenReturn(latest);

        assertEquals(1, articleService.updateArticle(input));

        verify(articleRevisionMapper, never()).insertRevision(any(ArticleRevision.class));
    }

    @Test
    void updateArticleCreatesRevisionWhenContentChanged() {
        Article input = new Article();
        input.setId(1L);
        input.setTitle("标题");
        input.setContent("新内容");

        Article current = article(1L, "标题", "新内容");
        ArticleRevision latest = revision(5L, 1L, 2, "标题", "旧内容");

        when(articleMapper.selectArticleByTitle("标题")).thenReturn(null);
        when(articleMapper.updateArticle(input)).thenReturn(1);
        when(articleMapper.selectArticleById(1L)).thenReturn(current);
        when(articleRevisionMapper.selectLatestRevision(1L)).thenReturn(latest);

        articleService.updateArticle(input);

        ArgumentCaptor<ArticleRevision> captor = ArgumentCaptor.forClass(ArticleRevision.class);
        verify(articleRevisionMapper).insertRevision(captor.capture());
        assertEquals(3, captor.getValue().getRevisionNo());
        assertEquals("新内容", captor.getValue().getContent());
    }

    @Test
    void restoreRevisionSavesCurrentContentAsNewRevision() {
        Article current = article(1L, "当前标题", "当前内容");
        ArticleRevision snapshot = revision(10L, 1L, 1, "旧标题", "旧内容");
        ArticleRevision latest = revision(11L, 1L, 2, "当前标题", "当前内容");

        when(articleRevisionMapper.selectRevisionById(10L)).thenReturn(snapshot);
        when(articleMapper.selectArticleById(1L)).thenReturn(current);
        when(articleMapper.selectArticleByTitle("旧标题")).thenReturn(null);
        when(articleRevisionMapper.selectLatestRevision(1L)).thenReturn(latest);
        when(articleMapper.restoreArticleFromRevision(snapshot)).thenReturn(1);

        assertEquals(1, articleService.restoreArticleRevision(10L));

        ArgumentCaptor<ArticleRevision> captor = ArgumentCaptor.forClass(ArticleRevision.class);
        verify(articleRevisionMapper).insertRevision(captor.capture());
        assertEquals(3, captor.getValue().getRevisionNo());
        assertEquals("当前标题", captor.getValue().getTitle(), "回滚前必须保存当前内容，便于撤销");
        verify(articleMapper).restoreArticleFromRevision(snapshot);
    }

    @Test
    void restoreRevisionRejectsTitleTakenByAnotherArticle() {
        Article current = article(1L, "当前标题", "当前内容");
        ArticleRevision snapshot = revision(10L, 1L, 1, "旧标题", "旧内容");

        when(articleRevisionMapper.selectRevisionById(10L)).thenReturn(snapshot);
        when(articleMapper.selectArticleById(1L)).thenReturn(current);
        when(articleMapper.selectArticleByTitle("旧标题"))
                .thenReturn(article(2L, "旧标题", "别人的内容"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> articleService.restoreArticleRevision(10L));

        assertTrue(exception.getMessage().contains("已被其他文章占用"));
        verify(articleRevisionMapper, never()).insertRevision(any(ArticleRevision.class));
        verify(articleMapper, never()).restoreArticleFromRevision(any(ArticleRevision.class));
    }

    @Test
    void restoreRevisionRejectsArticleInRecycleBin() {
        Article deleted = article(1L, "当前标题", "当前内容");
        deleted.setDelFlag(2L);
        ArticleRevision snapshot = revision(10L, 1L, 1, "旧标题", "旧内容");

        when(articleRevisionMapper.selectRevisionById(10L)).thenReturn(snapshot);
        when(articleMapper.selectArticleById(1L)).thenReturn(deleted);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> articleService.restoreArticleRevision(10L));

        assertTrue(exception.getMessage().contains("回收站"));
        verify(articleMapper, never()).restoreArticleFromRevision(any(ArticleRevision.class));
    }

    private Article article(Long id, String title, String content) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setContent(content);
        article.setFormat("html");
        article.setStatus(1L);
        article.setDelFlag(0L);
        return article;
    }

    private ArticleRevision revision(Long id, Long articleId, int revisionNo, String title, String content) {
        ArticleRevision revision = new ArticleRevision();
        revision.setId(id);
        revision.setArticleId(articleId);
        revision.setRevisionNo(revisionNo);
        revision.setTitle(title);
        revision.setContent(content);
        revision.setFormat("html");
        revision.setStatus(1L);
        return revision;
    }
}
