package com.zhi.web.controller.blog;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.utils.BlogSwitchUtils;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.ip.IpUtils;
import com.zhi.common.annotation.RateLimiter;
import com.zhi.common.enums.LimitType;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.page.TableDataInfo;
import com.zhi.system.domain.BlogArticle;
import com.zhi.system.domain.BlogCategory;
import com.zhi.system.domain.BlogComment;
import com.zhi.web.controller.blog.vo.FrontCommentVo;
import com.zhi.system.domain.BlogSetting;
import com.zhi.system.domain.BlogTag;
import com.zhi.system.service.IBlogArticleService;
import com.zhi.system.service.IBlogCategoryService;
import com.zhi.system.service.IBlogCommentService;
import com.zhi.system.service.IBlogSettingService;
import com.zhi.system.service.IBlogTagService;
import com.zhi.system.service.IBlogVisitLogService;
import com.zhi.system.service.ISysConfigService;
import com.zhi.system.domain.SysConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.zhi.common.core.domain.model.LoginUser;
import com.github.pagehelper.PageInfo;

/**
 * 上下篇文章信息DTO
 */
class ArticleNavigationDTO {
    private Long id;
    private String title;
    
    public ArticleNavigationDTO() {}
    
    public ArticleNavigationDTO(Long id, String title) {
        this.id = id;
        this.title = title;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}

/**
 * 站点前台控制器
 * 
 * @author nevell
 * @date 2025-10-11
 */
@RestController
@RequestMapping("/blog")
public class BlogFrontController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(BlogFrontController.class);

    public BlogFrontController() {
        log.debug("BlogFrontController initialized");
    }

    @Autowired
    private IBlogArticleService blogArticleService;

    @Autowired
    private IBlogCategoryService blogCategoryService;

    @Autowired
    private IBlogCommentService blogCommentService;

    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired
    private IBlogVisitLogService blogVisitLogService;

    @Autowired
    private IBlogTagService blogTagService;

    @Autowired
    private ISysConfigService sysConfigService;

    /**
     * 获取站点设置（前台用）
     */
    @Anonymous
    @GetMapping("/setting")
    public AjaxResult getBlogSettings()
    {
        // 创建一个Map来存储所有设置项，键为configKey，值为configValue
        Map<String, Object> settingsMap = new HashMap<>();

        try {
            // 首先尝试从sys_config表获取（后台管理页面配置的数据）
            List<SysConfig> configList = null;
            try {
                configList = sysConfigService.selectConfigList(new SysConfig());
            } catch (Exception e) {
                log.debug("sys_config表查询失败: {}", e.getMessage());
            }

            // 如果sys_config表为空，则回退到blog_setting表
            if (configList == null || configList.isEmpty()) {
                log.debug("sys_config表为空，回退到blog_setting表");
                BlogSetting blogSetting = new BlogSetting();
                blogSetting.setDelFlag("0"); // 只查询未删除的设置
                List<BlogSetting> blogSettingList = blogSettingService.selectBlogSettingList(blogSetting);

                if (blogSettingList != null && !blogSettingList.isEmpty()) {
                    for (BlogSetting setting : blogSettingList) {
                        if (isBlogConfigKey(setting.getSettingKey())) {
                            putConvertedSetting(settingsMap, setting.getSettingKey(), setting.getSettingValue());
                        }
                    }
                }
            } else {
                // 使用sys_config表的数据（仅白名单内的键）
                for (SysConfig config : configList) {
                    if (isBlogConfigKey(config.getConfigKey())) {
                        putConvertedSetting(settingsMap, config.getConfigKey(), config.getConfigValue());
                    }
                }
            }

            // 确保必要的设置项都有默认值
            applyDefaultSettings(settingsMap);

        } catch (Exception e) {
            log.error("获取站点设置出错: {}", e.getMessage(), e);

            // 如果出错，返回默认设置
            applyDefaultSettings(settingsMap);
        }

        return success(settingsMap);
    }

    /**
     * 获取前台公开展示的统计概览（匿名可访问）
     *
     * <p>只返回站点内容维度的公开计数（已发布文章/分类/标签/已通过评论/总浏览量），
     * 不包含注册用户数、系统运行状态等后台敏感信息。
     * 后台完整统计请使用 {@code /system-stats/overview}（需要 statistics:overview:list 权限）。</p>
     */
    @Anonymous
    @GetMapping("/stats/overview")
    public AjaxResult getPublicStatsOverview()
    {
        Map<String, Object> data = new HashMap<>();

        BlogArticle articleQuery = new BlogArticle();
        articleQuery.setStatus(1L); // 只统计已发布文章
        long articleCount = blogArticleService.selectBlogArticleCount(articleQuery);

        BlogComment commentQuery = new BlogComment();
        commentQuery.setStatus("1"); // 只统计已通过审核的评论
        long commentCount = blogCommentService.selectBlogCommentCount(commentQuery);

        long categoryCount = blogCategoryService.selectBlogCategoryCount(new BlogCategory());
        long tagCount = blogTagService.selectBlogTagCount(new BlogTag());
        long totalViews = blogArticleService.selectTotalViewCount();

        data.put("articleCount", articleCount);
        data.put("categoryCount", categoryCount);
        data.put("tagCount", tagCount);
        data.put("commentCount", commentCount);
        data.put("totalViews", totalViews);
        // 兼容后台概览的字段名
        data.put("viewCount", totalViews);

        return success(data);
    }

    /**
     * 将设置项做类型转换后放入Map（布尔值、头像URL特殊处理）
     */
    private void putConvertedSetting(Map<String, Object> settingsMap, String key, String value) {
        if (key == null) {
            return;
        }

        // 处理特殊类型转换
        Object convertedValue = value;

        // 布尔值转换
        if ("true".equals(value)) {
            convertedValue = true;
        } else if ("false".equals(value)) {
            convertedValue = false;
        }

        // 头像URL特殊处理
        if ("blog_avatar".equals(key) && value != null && !value.isEmpty()) {
            // 如果是完整的HTTP URL，转换为相对路径
            if (value.startsWith("http")) {
                try {
                    java.net.URL url = new java.net.URL(value);
                    String path = url.getPath();
                    if (path.startsWith("/profile/")) {
                        convertedValue = path; // 保留/profile/开头的相对路径
                    } else {
                        convertedValue = value; // 其他情况保持原样
                    }
                } catch (Exception e) {
                    convertedValue = value; // 转换失败保持原样
                }
            } else if (!value.startsWith("data:") && !value.startsWith("/")) {
                convertedValue = "/" + value; // 添加前导斜杠
            }
        }

        settingsMap.put(key, convertedValue);
    }

    /**
     * 确保必要的设置项都有默认值
     */
    private void applyDefaultSettings(Map<String, Object> settingsMap) {
        setDefaultSetting(settingsMap, "blog_avatar", "");
        setDefaultSetting(settingsMap, "github_url", "");
        setDefaultSetting(settingsMap, "weibo_url", "");
        setDefaultSetting(settingsMap, "wechat_qr", "");
        setDefaultSetting(settingsMap, "blog_author", "nevell");
        setDefaultSetting(settingsMap, "blog_name", "知企建站系统");
        setDefaultSetting(settingsMap, "blog_desc", "欢迎来到我的站点");
        setDefaultSetting(settingsMap, "author_title", "全栈开发工程师");
        setDefaultSetting(settingsMap, "blog_email", "");
    }

    /**
     * 判断键是否在白名单内
     */
    private boolean isBlogConfigKey(String key) {
        if (key == null) {
            return false;
        }
        for (String configKey : BlogFrontConfigKeys.BLOG_CONFIG_KEYS) {
            if (configKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置默认值
     */
    private void setDefaultSetting(Map<String, Object> settingsMap, String key, String defaultValue) {
        if (!settingsMap.containsKey(key) || settingsMap.get(key) == null || "".equals(settingsMap.get(key))) {
            settingsMap.put(key, defaultValue);
        }
    }

    /**
     * 获取文章详情（前台用）
     */
    @Anonymous
    /**
     * 按数字 ID 或 slug 解析文章；slug 只命中已发布且未删除的文章，数字 ID 由后续状态校验兜底
     */
    private BlogArticle resolvePublishedArticle(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (key.chars().allMatch(Character::isDigit)) {
            try {
                return blogArticleService.selectBlogArticleById(Long.parseLong(key));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return blogArticleService.selectBlogArticleBySlug(key);
    }

    @GetMapping("/article/{key:[A-Za-z0-9_-]+}")
    public AjaxResult getArticleDetail(@PathVariable("key") String key, HttpServletRequest request)
    {
        logger.info("请求文章详情，key: {}", key);

        // 兼容两种入口：数字 ID（历史链接）与 slug 伪静态链接，先解析再计数
        BlogArticle blogArticle = resolvePublishedArticle(key);
        if (blogArticle == null) {
            logger.warn("文章不存在或未发布，key: {}", key);
            return error("文章不存在");
        }
        Long id = blogArticle.getId();

        // 检查浏览统计开关，决定是否增加浏览量
        try {
            String viewCountEnabled = blogSettingService.selectSettingValueByKey("view_count_enabled");

            // 如果浏览统计开关为 true 或 "true"，或者未设置（默认启用），则增加浏览量
            boolean shouldCount = BlogSwitchUtils.isOn(viewCountEnabled);

            if (shouldCount) {
                String viewerKey = buildViewerKey(request);
                blogArticleService.addViewCount(id, viewerKey);
                // 文章访问同样写入 blog_visit_log 明细，否则管理端文章 PV/UV 明细永远为空
                blogVisitLogService.recordVisit("article", id, "/blog/article/" + id, viewerKey, request);
                logger.info("文章浏览量已增加，ID: {}", id);
            } else {
                logger.info("浏览统计已禁用，跳过增加浏览量，ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("浏览量统计失败，ID: {}, 错误: {}", id, e.getMessage());
            // 即使增加浏览量失败，也继续返回文章详情
        }

        blogArticle = blogArticleService.selectBlogArticleById(id);
        if (blogArticle == null) {
            logger.warn("文章不存在，ID: {}", id);
            return error("文章不存在");
        }

        logger.info("找到文章，ID: {}, 状态: {}, 删除标记: {}, 浏览量: {}",
            id, blogArticle.getStatus(), blogArticle.getDelFlag(), blogArticle.getViewCount());
        // slug 入口时上面已按 key 解析过一次；此处保持按 ID 重取完整字段（含 content_md/format）
        
        // 确保所有字段都有值，避免null
        if (blogArticle.getStatus() == null) {
            blogArticle.setStatus(0L); // 默认草稿状态
        }
        // 使用安全的null检查方式
        Long status = blogArticle.getStatus();
        if (status == null) {
            logger.warn("文章状态为空，ID: {}", id);
            return error("文章状态无效");
        }

        if (!Long.valueOf(1L).equals(status)) {
            logger.warn("文章未发布，ID: {}, 状态: {}", id, status);
            return error("文章未发布");
        }

        // 使用安全的null检查方式，避免直接调用getDelFlag().longValue()
        Long delFlag = blogArticle.getDelFlag();
        if (delFlag == null) {
            logger.warn("文章删除标记为空，ID: {}, 设置为默认值0", id);
            blogArticle.setDelFlag(0L); // 使用默认值
            delFlag = 0L;
        }
        // 然后安全地检查delFlag的值
        if (!Long.valueOf(0L).equals(delFlag)) {
            logger.warn("文章已删除，ID: {}, 删除标记: {}", id, delFlag);
            return error("文章已删除");
        }

        logger.info("文章状态检查通过，ID: {}", id);
        
        // 确保所有必要字段都有默认值，防止前端显示问题
        if (blogArticle.getViewCount() == null) blogArticle.setViewCount(0L);
        if (blogArticle.getLikeCount() == null) blogArticle.setLikeCount(0L);
        if (blogArticle.getCommentCount() == null) blogArticle.setCommentCount(0L);
        if (blogArticle.getIsTop() == null) blogArticle.setIsTop(0L);
        if (blogArticle.getIsRecommend() == null) blogArticle.setIsRecommend(0L);
        if (blogArticle.getTitle() == null) blogArticle.setTitle("无标题文章");
        if (blogArticle.getContent() == null) blogArticle.setContent("暂无内容");
        if (blogArticle.getSummary() == null) blogArticle.setSummary("");
        if (blogArticle.getAuthor() == null) blogArticle.setAuthor("未知作者");
        if (blogArticle.getAuthorName() == null) blogArticle.setAuthorName(blogArticle.getAuthor());
        
        // 获取上一篇和下一篇文章，使用专门的DTO对象
        Map<String, Object> extraInfo = new HashMap<>();
        
        try {
            BlogArticle prevArticle = blogArticleService.getPrevArticle(id);
            BlogArticle nextArticle = blogArticleService.getNextArticle(id);
            
            // 使用DTO对象避免序列化问题，添加更严格的null检查
            if (prevArticle != null && prevArticle.getId() != null && prevArticle.getStatus() != null && prevArticle.getStatus().equals(1L)) {
                ArticleNavigationDTO prevArticleDTO = new ArticleNavigationDTO(
                    prevArticle.getId(), 
                    prevArticle.getTitle() != null ? prevArticle.getTitle() : "上一篇"
                );
                extraInfo.put("prevArticle", prevArticleDTO);
            } else {
                extraInfo.put("prevArticle", null);
            }
            
            if (nextArticle != null && nextArticle.getId() != null && nextArticle.getStatus() != null && nextArticle.getStatus().equals(1L)) {
                ArticleNavigationDTO nextArticleDTO = new ArticleNavigationDTO(
                    nextArticle.getId(), 
                    nextArticle.getTitle() != null ? nextArticle.getTitle() : "下一篇"
                );
                extraInfo.put("nextArticle", nextArticleDTO);
            } else {
                extraInfo.put("nextArticle", null);
            }
        } catch (Exception e) {
            // 处理获取上下篇文章时的异常，确保即使出错也能返回文章详情
            extraInfo.put("prevArticle", null);
            extraInfo.put("nextArticle", null);
            log.warn("获取上下篇文章出错: {}", e.getMessage());
        }

        // 添加分类信息
        try {
            if (blogArticle.getCategoryId() != null) {
                BlogCategory category = blogCategoryService.selectBlogCategoryById(blogArticle.getCategoryId());
                if (category != null && "0".equals(category.getDelFlag())) {
                    extraInfo.put("category", category);
                }
            }
        } catch (Exception e) {
            log.warn("获取分类信息出错: {}", e.getMessage());
        }

        // 构建返回数据结构，包含文章主体和额外信息（上下篇文章、分类信息等）
        Map<String, Object> result = new HashMap<>();
        result.put("article", blogArticle);
        result.put("extraInfo", extraInfo);
        return success(result);
    }

    /**
     * 获取文章归档（按年月分组）
     */
    @Anonymous
    @GetMapping("/article-archive")
    public AjaxResult getArticleArchive()
    {
        log.debug("getArticleArchive method called");
        List<Map<String, Object>> archiveList = blogArticleService.getArticleArchive();
        log.debug("archiveList size: {}", archiveList != null ? archiveList.size() : "null");
        return success(archiveList);
    }

    /**
     * 获取文章列表（前台用，支持分页）
     */
    @Anonymous
    @GetMapping("/article/list")
    public TableDataInfo articleList(BlogArticle blogArticle)
    {
        // 设置查询条件
        blogArticle.setStatus(1L); // 只查询已发布的文章
        blogArticle.setDelFlag(0L); // 只查询未删除的文章

        startPage(); // 启用分页
        List<BlogArticle> list = blogArticleService.selectBlogArticleList(blogArticle);
        return getDataTable(list);
    }

    /**
     * 搜索文章（前台用，支持分页）
     */
    @Anonymous
    @GetMapping("/article/search")
    public TableDataInfo searchArticles(BlogArticle blogArticle, @RequestParam(value = "keyword", required = false) String keyword)
    {
        // 搜索功能开关：关闭后返回明确提示，避免只隐藏前端入口
        String searchEnabled = blogSettingService.selectSettingValueByKey("search_enabled");
        if (BlogSwitchUtils.isOff(searchEnabled))
        {
            TableDataInfo disabled = new TableDataInfo();
            disabled.setCode(500);
            disabled.setMsg("搜索功能已关闭");
            disabled.setRows(new java.util.ArrayList<>());
            disabled.setTotal(0);
            return disabled;
        }

        // 设置查询条件
        blogArticle.setStatus(1L); // 只查询已发布的文章
        blogArticle.setDelFlag(0L); // 只查询未删除的文章
        
        startPage(); // 启用分页
        
        List<BlogArticle> list;
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 如果有搜索关键词，则进行模糊搜索
            list = blogArticleService.searchArticles(keyword, blogArticle);
        } else {
            // 否则按常规条件查询
            list = blogArticleService.selectBlogArticleList(blogArticle);
        }
        
        return getDataTable(list);
    }

    /**
     * 根据分类获取文章列表（前台用，支持分页）
     */
    @Anonymous
    @GetMapping("/article/category/{categoryId}")
    public TableDataInfo getArticlesByCategory(@PathVariable("categoryId") Long categoryId, BlogArticle blogArticle)
    {
        // 设置查询条件
        blogArticle.setCategoryId(categoryId);
        blogArticle.setStatus(1L); // 只查询已发布的文章
        blogArticle.setDelFlag(0L); // 只查询未删除的文章
        
        startPage(); // 启用分页
        List<BlogArticle> list = blogArticleService.selectBlogArticleList(blogArticle);
        return getDataTable(list);
    }

    /**
     * 根据标签获取文章列表（前台用，支持分页）
     */
    @Anonymous
    @GetMapping("/article/tag/{tagId}")
    public TableDataInfo getArticlesByTag(@PathVariable("tagId") Long tagId, BlogArticle blogArticle)
    {
        startPage(); // 启用分页
        List<BlogArticle> list = blogArticleService.selectArticlesByTagId(tagId);
        return getDataTable(list);
    }

    /**
     * 获取热门文章（前台用，支持分页）
     */
    @Anonymous
    @GetMapping("/article/hot")
    public TableDataInfo getHotArticles(BlogArticle blogArticle)
    {
        // 设置查询条件
        blogArticle.setStatus(1L); // 只查询已发布的文章
        blogArticle.setDelFlag(0L); // 只查询未删除的文章

        startPage(); // 启用分页
        List<BlogArticle> list = blogArticleService.selectHotArticles(blogArticle);
        return getDataTable(list);
    }

    /**
     * 根据归档月份获取文章列表（前台用，支持分页）
     */
    @Anonymous
    @GetMapping("/article/archive-month/{year}/{month}")
    public TableDataInfo getArticlesByArchive(
            @PathVariable("year") String year,
            @PathVariable("month") String month,
            BlogArticle blogArticle)
    {
        // 设置查询条件
        String archiveDate = year + "-" + (month.length() == 1 ? "0" + month : month);
        blogArticle.setArchiveDate(archiveDate);
        blogArticle.setStatus(1L); // 只查询已发布的文章
        blogArticle.setDelFlag(0L); // 只查询未删除的文章

        startPage(); // 启用分页
        List<BlogArticle> list = blogArticleService.selectArticlesByArchive(blogArticle);
        return getDataTable(list);
    }

    /**
     * 更新文章浏览量（前台用）
     */
    @Anonymous
    @RateLimiter(key = "blog:view:", time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/article/view/{id}")
    public AjaxResult addArticleView(@PathVariable("id") Long id, HttpServletRequest request)
    {
        // 检查浏览统计开关
        String viewCountEnabled = blogSettingService.selectSettingValueByKey("view_count_enabled");

        // 浏览统计开关：未配置或开启都算启用（与全站开关判定同口径）
        boolean shouldCount = BlogSwitchUtils.isOn(viewCountEnabled);

        if (shouldCount) {
            String viewerKey = buildViewerKey(request);
            blogArticleService.addViewCount(id, viewerKey);
            blogVisitLogService.recordVisit("article", id, "/blog/article/" + id, viewerKey, request);
            logger.info("文章浏览量已增加（API调用），ID: {}", id);
        } else {
            logger.info("浏览统计已禁用，跳过增加浏览量，ID: {}", id);
        }

        return success();
    }

    /**
     * 获取相关文章（前台用）
     */
    @Anonymous
    @GetMapping("/article/related/{id}")
    public AjaxResult getRelatedArticles(@PathVariable("id") Long id)
    {
        List<BlogArticle> relatedArticles = blogArticleService.selectRelatedArticles(id);
        return success(relatedArticles);
    }

    /**
     * 获取文章评论列表（前台用）
     * 顶级评论分页，回复按所属楼层挂在各自 replies 里；两级都只取已发布（status=1）
     */
    @Anonymous
    @GetMapping("/comment/article/{articleId}")
    public TableDataInfo getArticleComments(@PathVariable("articleId") Long articleId,
                                            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort)
    {
        String normalizedSort = "oldest".equals(sort) ? "oldest" : "newest";

        BlogComment topQuery = new BlogComment();
        topQuery.setArticleId(articleId);
        topQuery.setStatus("1");
        topQuery.getParams().put("topLevelOnly", Boolean.TRUE);
        topQuery.getParams().put("sort", normalizedSort);

        startPage(); // 分页只作用于顶级评论
        // 前台专用查询 + 显式投影：既不从库里取、也不在响应里输出 email 等 PII
        List<BlogComment> topComments = blogCommentService.selectFrontCommentList(topQuery);
        long total = new PageInfo<>(topComments).getTotal();

        List<FrontCommentVo> rows = topComments.stream().map(FrontCommentVo::from).collect(Collectors.toList());
        attachReplies(articleId, rows);

        // getDataTable 会按 rows 重新推算 total，这里恢复为顶级评论总数（分页依据）
        TableDataInfo tableData = getDataTable(rows);
        tableData.setTotal(total);
        return tableData;
    }

    /**
     * 取回文章的全部回复，按所属楼层（顶级评论）挂到当前页的 rows 上。
     * 回复列表固定按时间正序，保持对话先后；不属于当前页楼层的回复直接跳过。
     */
    private void attachReplies(Long articleId, List<FrontCommentVo> rows)
    {
        if (rows.isEmpty())
        {
            return;
        }
        BlogComment replyQuery = new BlogComment();
        replyQuery.setArticleId(articleId);
        replyQuery.setStatus("1");
        replyQuery.getParams().put("repliesOnly", Boolean.TRUE);
        replyQuery.getParams().put("sort", "oldest");
        List<BlogComment> replies = blogCommentService.selectFrontCommentList(replyQuery);
        if (replies.isEmpty())
        {
            return;
        }

        Map<Long, FrontCommentVo> rowById = new HashMap<>();
        for (FrontCommentVo row : rows)
        {
            rowById.put(row.getId(), row);
        }
        Map<Long, Long> parentByReplyId = new HashMap<>();
        for (BlogComment reply : replies)
        {
            // 顶级评论 parent_id 落库是 0（默认值），回复判定统一按 > 0
            if (reply.getParentId() != null && reply.getParentId() > 0)
            {
                parentByReplyId.put(reply.getId(), reply.getParentId());
            }
        }

        for (BlogComment reply : replies)
        {
            Long rootId = resolveThreadRoot(reply.getParentId(), rowById, parentByReplyId);
            FrontCommentVo root = rootId == null ? null : rowById.get(rootId);
            if (root != null)
            {
                root.getReplies().add(FrontCommentVo.from(reply));
            }
        }
    }

    /**
     * 沿 parentId 向上找回复所属的楼层；父级不在当前页或链路异常时返回 null。
     * 深度上限用于兜底脏数据里的父子环，避免匿名接口死循环。
     */
    private static Long resolveThreadRoot(Long parentId, Map<Long, FrontCommentVo> rowById, Map<Long, Long> parentByReplyId)
    {
        Long cursor = parentId;
        for (int depth = 0; cursor != null && depth < 64; depth++)
        {
            if (rowById.containsKey(cursor))
            {
                return cursor;
            }
            if (!parentByReplyId.containsKey(cursor))
            {
                return null;
            }
            cursor = parentByReplyId.get(cursor);
        }
        return null;
    }

    /**
     * 当前文章已发布评论数（含回复），随评论增删改回传，前台计数无需猜测审核结果
     */
    private Map<String, Object> commentCountData(Long articleId)
    {
        BlogComment countQuery = new BlogComment();
        countQuery.setArticleId(articleId);
        countQuery.setStatus("1");
        Map<String, Object> data = new HashMap<>();
        data.put("commentCount", blogCommentService.selectBlogCommentCount(countQuery));
        return data;
    }

    /**
     * 添加评论（前台用）
     */
    @Anonymous
    @RateLimiter(key = "blog:comment:", time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/comment")
    public AjaxResult addComment(@RequestBody BlogComment blogComment)
    {
        // 评论功能开关：关闭后直接拒绝，避免只隐藏前端入口仍可被脚本提交
        String commentEnabled = blogSettingService.selectSettingValueByKey("comment_enabled");
        if (BlogSwitchUtils.isOff(commentEnabled))
        {
            return error("评论功能已关闭");
        }

        // 先按登录账号补全 userId 与昵称，再校验：登录态的评论表单不采集昵称，
        // 校验放在补全之前会把登录用户的评论一律拒成"昵称不能为空"
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof LoginUser loginUser) {
                if (blogComment.getUserId() == null) {
                    blogComment.setUserId(loginUser.getUserId());
                }
                if (blogComment.getNickname() == null || blogComment.getNickname().isEmpty()) {
                    blogComment.setNickname(loginUser.getUser().getNickName());
                }
            }
        } catch (Exception ignored) {
            // 匿名用户，无需填充
        }

        // 服务端校验，防止绕过前端限制
        String nickname = blogComment.getNickname();
        String content = blogComment.getContent();
        if (blogComment.getArticleId() == null)
        {
            return error("文章ID不能为空");
        }
        if (nickname == null || nickname.trim().isEmpty())
        {
            return error("昵称不能为空");
        }
        if (nickname.trim().length() > 50)
        {
            return error("昵称长度不能超过50个字符");
        }
        if (content == null || content.trim().isEmpty())
        {
            return error("评论内容不能为空");
        }
        if (content.length() > 500)
        {
            return error("评论内容长度不能超过500个字符");
        }
        if (blogComment.getEmail() != null && blogComment.getEmail().length() > 100)
        {
            return error("邮箱长度不能超过100个字符");
        }

        // 回复必须挂在同一篇文章的既有评论下，避免任意 parentId 串文章/凭空造楼层
        if (blogComment.getParentId() != null)
        {
            BlogComment parentComment = blogCommentService.selectBlogCommentById(blogComment.getParentId());
            if (parentComment == null || !blogComment.getArticleId().equals(parentComment.getArticleId()))
            {
                return error("回复的评论不存在或不属于当前文章");
            }
        }

        // 检查评论审核开关，根据站点设置决定是否需要审核
        String commentReviewSetting = blogSettingService.selectSettingValueByKey("comment_review");

        // 如果评论审核开关为 true 或 "true"，则设置为待审核（0）；否则直接发布（1）
        // 默认需要审核（安全考虑）
        boolean needsReview = BlogSwitchUtils.isOn(commentReviewSetting);

        blogComment.setStatus(needsReview ? "0" : "1");

        logger.info("新评论提交: articleId={}, status={} (审核开关={})",
                    blogComment.getArticleId(),
                    needsReview ? "待审核" : "已发布",
                    commentReviewSetting);

        int rows = blogCommentService.insertBlogComment(blogComment);
        if (rows <= 0)
        {
            return error("评论发表失败");
        }
        return success(commentCountData(blogComment.getArticleId()));
    }

    /**
     * 修改本人评论（需登录 + 归属校验）
     */
    @RateLimiter(key = "blog:comment:update:", time = 60, count = 10, limitType = LimitType.IP)
    @PutMapping("/comment/{id}")
    public AjaxResult updateMyComment(@PathVariable("id") Long id, @RequestBody BlogComment payload)
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            return error("请先登录");
        }
        BlogComment existing = blogCommentService.selectBlogCommentById(id);
        if (existing == null || !userId.equals(existing.getUserId()))
        {
            return error("评论不存在或无权修改");
        }

        String content = payload == null ? null : payload.getContent();
        if (content == null || content.trim().isEmpty())
        {
            return error("评论内容不能为空");
        }
        if (content.length() > 500)
        {
            return error("评论内容长度不能超过500个字符");
        }

        BlogComment update = new BlogComment();
        update.setId(id);
        update.setContent(content);
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(new java.util.Date());
        // 编辑后若开启评论审核则回退为待审核，避免发布后绕过审核改写内容
        if (BlogSwitchUtils.isOn(blogSettingService.selectSettingValueByKey("comment_review")))
        {
            update.setStatus("0");
        }
        int rows = blogCommentService.updateBlogComment(update);
        if (rows <= 0)
        {
            return error("评论修改失败");
        }
        return success(commentCountData(existing.getArticleId()));
    }

    /**
     * 删除本人评论（需登录 + 归属校验）
     */
    @RateLimiter(key = "blog:comment:delete:", time = 60, count = 10, limitType = LimitType.IP)
    @DeleteMapping("/comment/{id}")
    public AjaxResult deleteMyComment(@PathVariable("id") Long id)
    {
        Long userId = SecurityUtils.getUserId();
        if (userId == null)
        {
            return error("请先登录");
        }
        BlogComment existing = blogCommentService.selectBlogCommentById(id);
        if (existing == null || !userId.equals(existing.getUserId()))
        {
            return error("评论不存在或无权删除");
        }
        int rows = blogCommentService.deleteBlogCommentById(id);
        if (rows <= 0)
        {
            return error("评论删除失败");
        }
        return success(commentCountData(existing.getArticleId()));
    }

    /**
     * 获取分类列表（前台用，包含文章数量）
     */
    @Anonymous
    @GetMapping("/category/list")
    public AjaxResult categoryList()
    {
        BlogCategory blogCategory = new BlogCategory();
        blogCategory.setDelFlag("0"); // 只查询未删除的分类
        blogCategory.setStatus(1); // 只查询启用的分类（1=启用，0=禁用）
        List<BlogCategory> list = blogCategoryService.selectCategoryListForFront(blogCategory);
        return success(list);
    }

    /**
     * 获取分类详情（前台用）
     */
    @Anonymous
    @GetMapping("/category/{id}")
    public AjaxResult getCategory(@PathVariable(value = "id", required = false) String idStr)
    {
        if (idStr == null || "undefined".equals(idStr) || idStr.trim().isEmpty()) {
            return error("分类ID不能为空");
        }
        
        Long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return error("分类ID格式不正确");
        }
        
        BlogCategory category = blogCategoryService.selectBlogCategoryById(id);
        if (category == null) {
            return error("分类不存在或已删除");
        }
        // 安全检查delFlag，避免null值
        String categoryDelFlag = category.getDelFlag();
        if (categoryDelFlag == null) {
            categoryDelFlag = "0";
        }
        if (!"0".equals(categoryDelFlag)) {
            return error("分类不存在或已删除");
        }
        return success(category);
    }

    /**
     * 获取标签列表（前台用）
     */
    @Anonymous
    @GetMapping("/tag/list")
    public AjaxResult tagList()
    {
        BlogTag blogTag = new BlogTag();
        blogTag.setDelFlag(0); // 只查询未删除的标签
        List<BlogTag> list = blogTagService.selectBlogTagList(blogTag);
        return success(list);
    }

    /**
     * 获取标签详情（前台用）
     */
    @Anonymous
    @GetMapping("/tag/{id}")
    public AjaxResult getTag(@PathVariable("id") Long id)
    {
        BlogTag tag = blogTagService.selectBlogTagById(id);
        if (tag == null)
        {
            return error("标签不存在");
        }
        // 检查删除标记，避免泄露已删除的标签
        Integer delFlag = tag.getDelFlag();
        if (delFlag != null && !Integer.valueOf(0).equals(delFlag))
        {
            return error("标签不存在");
        }
        return success(tag);
    }

    /**
     * 获取标签云（包含文章数量）
     */
    @Anonymous
    @GetMapping("/tag/cloud")
    public AjaxResult getTagCloud()
    {
        log.debug("getTagCloud method called");
        List<Map<String, Object>> tagCloud = blogTagService.getTagCloud();
        log.debug("tagCloud size: {}", tagCloud != null ? tagCloud.size() : "null");
        return success(tagCloud);
    }

    /**
     * 获取RSS订阅
     */
    @Anonymous
    @GetMapping(value = "/rss", produces = "application/xml;charset=UTF-8")
    public void getRssFeed(HttpServletResponse response) throws IOException {
        response.setContentType("application/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 从配置中读取站点URL，如果配置不存在则使用默认值
        String blogUrl = blogSettingService.selectSettingValueByKey("blog_url");
        if (blogUrl == null || blogUrl.trim().isEmpty()) {
            blogUrl = "http://localhost:3000";
        }

        // 从配置中读取站点标题与描述，避免硬编码
        String blogName = blogSettingService.selectSettingValueByKey("blog_name");
        if (blogName == null || blogName.trim().isEmpty()) {
            blogName = "知企建站系统";
        }
        String blogDesc = blogSettingService.selectSettingValueByKey("blog_desc");
        if (blogDesc == null || blogDesc.trim().isEmpty()) {
            blogDesc = "欢迎来到我的站点";
        }

        DateTimeFormatter rfc1123 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<rss version=\"2.0\">");
        out.println("<channel>");
        out.println("<title><![CDATA[" + blogName + "]]></title>");
        out.println("<link>" + blogUrl + "</link>");
        out.println("<description><![CDATA[" + blogDesc + "]]></description>");
        out.println("<language>zh-CN</language>");

        // 获取最新的10篇文章
        BlogArticle blogArticle = new BlogArticle();
        blogArticle.setStatus(1L);
        blogArticle.setDelFlag(0L);

        // 只取最新 10 篇：之前是把全部已发布文章（含 longtext 正文）拉进内存再截断
        com.github.pagehelper.PageHelper.startPage(1, 10, false);
        List<BlogArticle> articles = blogArticleService.selectBlogArticleList(blogArticle);
        com.github.pagehelper.PageHelper.clearPage();
        int count = 0;
        for (BlogArticle article : articles) {
            if (count >= 10) break;

            String pubDate = "";
            if (article.getCreateTime() != null) {
                pubDate = article.getCreateTime().toInstant().atZone(ZoneId.systemDefault())
                    .format(rfc1123);
            }

            out.println("<item>");
            out.println("<title><![CDATA[" + (article.getTitle() != null ? article.getTitle() : "") + "]]></title>");
            out.println("<link>" + blogUrl + "/blog/article/" + article.getId() + "</link>");
            out.println("<description><![CDATA[" + (article.getSummary() != null ? article.getSummary() : "") + "]]></description>");
            out.println("<pubDate>" + pubDate + "</pubDate>");
            out.println("<guid>" + blogUrl + "/blog/article/" + article.getId() + "</guid>");
            out.println("</item>");

            count++;
        }

        out.println("</channel>");
        out.println("</rss>");
        out.flush();
    }

    /**
     * 生成访问者去重标识：优先登录用户ID，否则取客户端IP
     *
     * @param request 当前请求
     * @return 去重标识；获取失败返回 null（此时不去重）
     */
    private String buildViewerKey(HttpServletRequest request)
    {
        try
        {
            Long userId = SecurityUtils.getUserId();
            if (userId != null)
            {
                return "u" + userId;
            }
        }
        catch (Exception ignored)
        {
            // 未登录，回退到 IP
        }
        try
        {
            String ip = IpUtils.getIpAddr(request);
            if (ip == null || ip.isEmpty())
            {
                return null;
            }
            return "ip:" + ip;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }
}
