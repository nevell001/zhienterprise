package com.zhi.web.controller.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.framework.config.properties.PermitAllUrlProperties;
import com.zhi.system.domain.Article;
import com.zhi.system.domain.Category;
import com.zhi.system.domain.Comment;
import com.zhi.system.domain.Tag;
import com.zhi.system.service.IArticleService;
import com.zhi.system.service.ICategoryService;
import com.zhi.system.service.ICommentService;
import com.zhi.system.service.ISettingService;
import com.zhi.system.service.ITagService;
import com.zhi.system.service.IVisitLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 博客前台接口单元测试
 * 覆盖 /api/comment 的服务端校验与提交链路，以及 /api/stats/overview 公开统计
 */
class FrontControllerTest
{
    private IArticleService articleService;
    private ICategoryService categoryService;
    private ICommentService commentService;
    private ISettingService settingService;
    private ITagService tagService;
    private IVisitLogService visitLogService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp()
    {
        articleService = mock(IArticleService.class);
        categoryService = mock(ICategoryService.class);
        commentService = mock(ICommentService.class);
        settingService = mock(ISettingService.class);
        tagService = mock(ITagService.class);
        visitLogService = mock(IVisitLogService.class);

        FrontController controller = new FrontController();
        ReflectionTestUtils.setField(controller, "articleService", articleService);
        ReflectionTestUtils.setField(controller, "categoryService", categoryService);
        ReflectionTestUtils.setField(controller, "commentService", commentService);
        ReflectionTestUtils.setField(controller, "settingService", settingService);
        ReflectionTestUtils.setField(controller, "tagService", tagService);
        ReflectionTestUtils.setField(controller, "visitLogService", visitLogService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void addCommentShouldBeRejectedWhenCommentDisabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("comment_enabled")).thenReturn("false");

        perform(validComment())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("评论功能已关闭"));

        verify(commentService, never()).insertComment(any(Comment.class));
    }

    @Test
    void searchShouldBeRejectedWhenSearchDisabled() throws Exception
    {
        when(settingService.selectSettingValueByKey("search_enabled")).thenReturn("0");

        mockMvc.perform(get("/api/article/search").param("keyword", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("搜索功能已关闭"))
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void statsOverviewShouldExposePublicCountersOnly() throws Exception
    {
        when(articleService.selectArticleCount(any(Article.class))).thenReturn(12L);
        when(commentService.selectCommentCount(any(Comment.class))).thenReturn(34L);
        when(categoryService.selectCategoryCount(any(Category.class))).thenReturn(2L);
        when(tagService.selectTagCount(any(Tag.class))).thenReturn(1L);
        when(articleService.selectTotalViewCount()).thenReturn(567L);

        mockMvc.perform(get("/api/stats/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.articleCount").value(12))
            .andExpect(jsonPath("$.data.categoryCount").value(2))
            .andExpect(jsonPath("$.data.tagCount").value(1))
            .andExpect(jsonPath("$.data.commentCount").value(34))
            .andExpect(jsonPath("$.data.totalViews").value(567))
            .andExpect(jsonPath("$.data.viewCount").value(567))
            // 公开接口不得泄露注册用户数等后台信息
            .andExpect(jsonPath("$.data.userCount").doesNotExist());
    }

    @Test
    void addCommentShouldRejectMissingArticleId() throws Exception
    {
        Comment comment = validComment();
        comment.setArticleId(null);

        perform(comment)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));

        verify(commentService, never()).insertComment(any(Comment.class));
    }

    @Test
    void addCommentShouldRejectEmptyContent() throws Exception
    {
        Comment comment = validComment();
        comment.setContent("   ");

        perform(comment)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));

        verify(commentService, never()).insertComment(any(Comment.class));
    }

    @Test
    void addCommentShouldRejectContentTooLong() throws Exception
    {
        Comment comment = validComment();
        comment.setContent("a".repeat(501));

        perform(comment)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));

        verify(commentService, never()).insertComment(any(Comment.class));
    }

    @Test
    void addCommentShouldRejectMissingNickname() throws Exception
    {
        Comment comment = validComment();
        comment.setNickname("  ");

        perform(comment)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));

        verify(commentService, never()).insertComment(any(Comment.class));
    }

    @Test
    void addCommentShouldSubmitWithReviewStatusByDefault() throws Exception
    {
        Comment comment = validComment();
        when(commentService.insertComment(any(Comment.class))).thenReturn(1);

        perform(comment)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(commentService).insertComment(any(Comment.class));
    }

    /**
     * 登录用户（含新注册的博客用户）提交评论时不带昵称：应按账号信息补全后放行。
     * 曾经补全写在校验之后，登录态评论一律被拒成"昵称不能为空"。
     */
    @Test
    void addCommentShouldFillNicknameFromAccountWhenLoggedIn() throws Exception
    {
        Comment comment = validComment();
        comment.setNickname(null);
        when(commentService.insertComment(any(Comment.class))).thenReturn(1);

        SysUser account = new SysUser();
        account.setNickName("新注册用户");
        LoginUser loginUser = new LoginUser(100L, null, account, null);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
        try
        {
            perform(comment)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService).insertComment(captor.capture());
        assertEquals("新注册用户", captor.getValue().getNickname());
        assertEquals(100L, captor.getValue().getUserId());
    }

    private ResultActions perform(Comment comment)
        throws Exception
    {
        return mockMvc.perform(post("/api/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(comment)));
    }

    private Comment validComment()
    {
        Comment comment = new Comment();
        comment.setArticleId(1L);
        comment.setNickname("测试用户");
        comment.setContent("这是一条测试评论");
        return comment;
    }

    @Test
    void articleCommentsShouldNotExposeEmail() throws Exception
    {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setArticleId(1L);
        comment.setNickname("访客");
        comment.setContent("你好");
        comment.setEmail("secret@example.com"); // 实体即便带 email，前台接口也不允许输出
        when(commentService.selectFrontCommentList(any())).thenReturn(List.of(comment), List.of());

        mockMvc.perform(get("/api/comment/article/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].nickname").value("访客"))
            .andExpect(jsonPath("$.rows[0].content").value("你好"))
            .andExpect(jsonPath("$.rows[0].email").doesNotExist());

        // 第一次查顶级评论（分页），第二次查回复用于挂楼层
        verify(commentService, times(2)).selectFrontCommentList(any());
        verify(commentService, never()).selectCommentList(any());
    }

    @Test
    void articleCommentsShouldNestRepliesUnderTheirThreadRoot() throws Exception
    {
        Comment top = comment(10L, 1L, null, "楼主", "沙发");
        Comment reply = comment(11L, 1L, 10L, "访客", "回复");
        Comment nestedReply = comment(12L, 1L, 11L, "路人", "回复的回复"); // 回复的回复也归到同一楼
        Comment otherThreadReply = comment(13L, 1L, null, "误标", "不该出现"); // parentId 为空的"回复"不进列表
        when(commentService.selectFrontCommentList(any()))
            .thenReturn(List.of(top))
            .thenReturn(List.of(reply, nestedReply, otherThreadReply));

        mockMvc.perform(get("/api/comment/article/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows.length()").value(1))
            .andExpect(jsonPath("$.rows[0].id").value(10))
            .andExpect(jsonPath("$.rows[0].replies.length()").value(2))
            .andExpect(jsonPath("$.rows[0].replies[0].id").value(11))
            .andExpect(jsonPath("$.rows[0].replies[1].id").value(12))
            // 分页总数 = 顶级评论数（只有一页一条）
            .andExpect(jsonPath("$.total").value(1));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService, times(2)).selectFrontCommentList(captor.capture());
        Comment topQuery = captor.getAllValues().get(0);
        Comment replyQuery = captor.getAllValues().get(1);
        assertEquals(Boolean.TRUE, topQuery.getParams().get("topLevelOnly"));
        assertEquals("newest", topQuery.getParams().get("sort"));
        assertEquals(Boolean.TRUE, replyQuery.getParams().get("repliesOnly"));
        assertEquals("oldest", replyQuery.getParams().get("sort"));
    }

    @Test
    void articleCommentsShouldNormalizeUnknownSortToNewest() throws Exception
    {
        when(commentService.selectFrontCommentList(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/comment/article/1").param("sort", "id desc; drop table zhi_comment"))
            .andExpect(status().isOk());

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService).selectFrontCommentList(captor.capture());
        assertEquals("newest", captor.getValue().getParams().get("sort"));
    }

    @Test
    void articleCommentsShouldPassOldestSortToQuery() throws Exception
    {
        when(commentService.selectFrontCommentList(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/comment/article/1").param("sort", "oldest"))
            .andExpect(status().isOk());

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService).selectFrontCommentList(captor.capture());
        assertEquals("oldest", captor.getValue().getParams().get("sort"));
    }

    @Test
    void addCommentShouldRejectReplyToUnknownParent() throws Exception
    {
        Comment comment = validComment();
        comment.setParentId(999L);
        when(commentService.selectCommentById(999L)).thenReturn(null);

        perform(comment)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("回复的评论不存在或不属于当前文章"));

        verify(commentService, never()).insertComment(any(Comment.class));
    }

    @Test
    void addCommentShouldRejectReplyFromAnotherArticle() throws Exception
    {
        Comment comment = validComment();
        comment.setParentId(5L);
        when(commentService.selectCommentById(5L)).thenReturn(comment(5L, 2L, null, "别处", "另一篇的评论"));

        perform(comment)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("回复的评论不存在或不属于当前文章"));

        verify(commentService, never()).insertComment(any(Comment.class));
    }

    @Test
    void addCommentShouldReturnFreshCommentCountAfterPublish() throws Exception
    {
        Comment comment = validComment();
        when(commentService.insertComment(any(Comment.class))).thenReturn(1);
        when(commentService.selectCommentCount(any(Comment.class))).thenReturn(8L);

        perform(comment)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.commentCount").value(8));

        ArgumentCaptor<Comment> countCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService).selectCommentCount(countCaptor.capture());
        assertEquals(1L, countCaptor.getValue().getArticleId());
        assertEquals("1", countCaptor.getValue().getStatus());
    }

    @Test
    void deleteMyCommentShouldReturnFreshCommentCount() throws Exception
    {
        Comment existing = comment(5L, 1L, null, "博主", "待删除");
        existing.setUserId(100L);
        when(commentService.selectCommentById(5L)).thenReturn(existing);
        when(commentService.deleteCommentById(5L)).thenReturn(1);
        when(commentService.selectCommentCount(any(Comment.class))).thenReturn(7L);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new LoginUser(100L, null, new SysUser(), null), null, List.of()));
        try
        {
            mockMvc.perform(delete("/api/comment/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.commentCount").value(7));
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    private Comment comment(Long id, Long articleId, Long parentId, String nickname, String content)
    {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setArticleId(articleId);
        comment.setParentId(parentId);
        comment.setNickname(nickname);
        comment.setContent(content);
        return comment;
    }

    @Test
    void rssShouldLimitToTenArticles() throws Exception
    {
        when(settingService.selectSettingValueByKey(anyString())).thenReturn("博客");
        java.util.concurrent.atomic.AtomicReference<com.github.pagehelper.Page<?>> captured =
            new java.util.concurrent.atomic.AtomicReference<>();
        when(articleService.selectArticleList(any())).thenAnswer(invocation -> {
            captured.set(com.github.pagehelper.PageHelper.getLocalPage());
            return List.of();
        });

        mockMvc.perform(get("/api/rss")).andExpect(status().isOk());

        // 旧实现是把全部已发布文章（含正文）拉进内存再截前 10 条
        assertNotNull(captured.get(), "RSS 必须用 PageHelper 限制条数");
        assertEquals(1, captured.get().getPageNum());
        assertEquals(10, captured.get().getPageSize());
    }

    /**
     * 文章详情端点必须进入 @Anonymous 扫描出的匿名放行清单。
     *
     * <p>事故回归：@Anonymous 曾夹在两段 Javadoc 之间、挂到了私有方法 resolvePublishedArticle 上，
     * 真正的端点 getArticleDetail 没有注解。SecurityConfig 只放行扫描清单里的 URL，
     * 加上前端公开接口不带头（isToken:false），所有访客访问 /api/article/{id} 一律 401。
     * 本测试跑的就是生产同款 PermitAllUrlProperties 扫描，注解一旦挂错位置立即红灯。</p>
     */
    @Test
    void articleDetailShouldBeRegisteredAsAnonymousPermitUrl()
    {
        try (AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext())
        {
            ctx.refresh();
            // 手动注册的实例不经过后置处理器：本测试只关心请求映射与 @Anonymous 扫描，无需注入依赖
            ctx.getBeanFactory().registerSingleton("frontController", new FrontController());

            RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
            mapping.setApplicationContext(ctx);
            mapping.afterPropertiesSet();
            ctx.getBeanFactory().registerSingleton("requestMappingHandlerMapping", mapping);

            PermitAllUrlProperties permitAllUrl = new PermitAllUrlProperties();
            permitAllUrl.setApplicationContext(ctx);
            permitAllUrl.afterPropertiesSet();

            assertTrue(permitAllUrl.getUrls().contains("/api/article/{key:[A-Za-z0-9_-]+}"),
                "@Anonymous 缺失或挂错位置：/api/article/{key} 未进入匿名放行清单，前台访客会被拦截成 401");
        }
    }
}
