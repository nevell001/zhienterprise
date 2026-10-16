package com.zhi.system.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Log;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.enums.BusinessType;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.ModernDateUtils;
import com.zhi.system.domain.Article;
import com.zhi.system.domain.Category;
import com.zhi.system.domain.Tag;
import com.zhi.system.service.IArticleService;
import com.zhi.common.utils.html.MarkdownUtil;
import com.zhi.system.service.ICategoryService;
import com.zhi.system.service.ITagService;
import com.zhi.common.utils.poi.ExcelUtil;
import com.zhi.common.core.page.TableDataInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文章管理Controller
 * 
 * @author nevell
 * @date 2025-11-14
 */
@RestController
@RequestMapping("/system/article")
public class ArticleController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);

    /**
     * 错误消息常量
     */
    private static final String MSG_SELECT_ARTICLES_TO_UPDATE = "请选择要更新的文章";
    private static final String PARAM_IS_TOP = "isTop";
    private static final String PARAM_IS_RECOMMEND = "isRecommend";

    /** 内容类型白名单（zhi_article.article_type）：资讯动态 / 核心业务 / 客户案例 / 解决方案 */
    private static final Set<String> ARTICLE_TYPES = Set.of("news", "business", "case", "solution");

    @Autowired
    private IArticleService articleService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private ITagService tagService;

    /**
     * 查询文章列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:list')")
    @GetMapping("/list")
    public TableDataInfo list(Article article)
    {
        startPage();
        List<Article> list = articleService.selectArticleList(article);
        return getDataTable(list);
    }

    /**
     * 导出文章列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:export')")
    @Log(title = "文章管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Article article)
    {
        // 导出需要正文列，走专用查询（列表接口的查询已不含 longtext）
        List<Article> list = articleService.selectArticleListForExport(article);
        ExcelUtil<Article> util = new ExcelUtil<Article>(Article.class);
        util.exportExcel(response, list, "文章数据");
    }

    /**
     * 获取当前登录用户的文章列表（用于个人中心）
     * 必须在 /{id} 端点之前定义，否则路径会被 /{id} 匹配
     */
    @GetMapping("/my")
    public TableDataInfo getMyArticles(Article article)
    {
        startPage();
        // 自动设置为当前登录用户的 ID
        Long currentUserId = getUserId();
        article.setAuthorId(currentUserId);
        List<Article> list = articleService.selectArticleList(article);
        return getDataTable(list);
    }

    /**
     * 获取文章详细信息
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        Article article = articleService.selectArticleById(id);
        if (article == null) {
            return error("文章不存在");
        }

        return success(article);
    }

    /**
     * 新增文章
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:add')")
    @Log(title = "文章管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> params)
    {
        // 解析文章信息
        Article article = parseArticleFromParams(params);

        // 解析标签ID列表
        List<Long> tagIds = parseTagIds(params.get("tagIds"));

        // 插入文章
        int result = articleService.insertArticle(article);
        if (result > 0) {
            // 关联标签
            if (tagIds != null && !tagIds.isEmpty()) {
                articleService.insertArticleTagRelations(article.getId(), tagIds);
            }
            return success("文章创建成功");
        } else {
            return error("文章创建失败");
        }
    }

    /**
     * 修改文章
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> params)
    {
        // 解析文章信息
        Article article = parseArticleFromParams(params);

        // 解析标签ID列表
        List<Long> tagIds = parseTagIds(params.get("tagIds"));

        // 更新文章
        int result = articleService.updateArticle(article);
        if (result > 0) {
            // 仅当显式传入 tagIds 时更新标签关联，避免漏传清空文章标签
            if (tagIds != null) {
                articleService.updateArticleTagRelations(article.getId(), tagIds);
            }
            return success("文章更新成功");
        } else {
            return error("文章更新失败");
        }
    }

    /**
     * 删除文章
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:remove')")
    @Log(title = "文章管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(articleService.deleteArticleByIds(ids));
    }

    /**
     * 获取分类和标签选项
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:query')")
    @GetMapping("/options")
    public AjaxResult getOptions()
    {
        Map<String, Object> result = new HashMap<>();

        // 获取分类列表
        Category categoryQuery = new Category();
        categoryQuery.setDelFlag("0");
        categoryQuery.setStatus(1);
        List<Category> categories = categoryService.selectCategoryList(categoryQuery);
        result.put("categories", categories);

        // 获取标签列表
        Tag tagQuery = new Tag();
        tagQuery.setDelFlag(0);
        List<Tag> tags = tagService.selectTagList(tagQuery);
        result.put("tags", tags);

        return success(result);
    }

    /**
     * 批量更新文章状态
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult updateStatus(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        Integer status = (Integer) params.get("status");

        if (ids == null || ids.isEmpty()) {
            return error(MSG_SELECT_ARTICLES_TO_UPDATE);
        }

        int result = articleService.updateArticleStatus(ids, status);
        return toAjax(result);
    }

    /**
     * 批量更新文章置顶状态
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/top")
    public AjaxResult updateTopStatus(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        Integer isTop = (Integer) params.get(PARAM_IS_TOP);

        if (ids == null || ids.isEmpty()) {
            return error(MSG_SELECT_ARTICLES_TO_UPDATE);
        }

        int result = articleService.updateArticleTopStatus(ids, isTop);
        return toAjax(result);
    }

    /**
     * 批量更新文章推荐状态
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/recommend")
    public AjaxResult updateRecommendStatus(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        Integer isRecommend = (Integer) params.get(PARAM_IS_RECOMMEND);

        if (ids == null || ids.isEmpty()) {
            return error(MSG_SELECT_ARTICLES_TO_UPDATE);
        }

        int result = articleService.updateArticleRecommendStatus(ids, isRecommend);
        return toAjax(result);
    }

    /**
     * 点赞文章
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/like/{id}")
    public AjaxResult likeArticle(@PathVariable("id") Long id)
    {
        int result = articleService.likeArticle(id);
        if (result > 0) {
            return success("点赞成功");
        }
        return error("点赞失败");
    }

    /**
     * 搜索文章
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:list')")
    @GetMapping("/search")
    public TableDataInfo search(@RequestParam("keyword") String keyword, Article article)
    {
        startPage();
        List<Article> list = articleService.searchArticles(keyword, article);
        return getDataTable(list);
    }

    /**
     * 根据分类获取文章
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:list')")
    @GetMapping("/category/{categoryId}")
    public TableDataInfo getByCategory(@PathVariable("categoryId") Long categoryId, Article article)
    {
        article.setCategoryId(categoryId);
        startPage();
        List<Article> list = articleService.selectArticleList(article);
        return getDataTable(list);
    }

    /**
     * 根据标签获取文章
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:list')")
    @GetMapping("/tag/{tagId}")
    public TableDataInfo getByTag(@PathVariable("tagId") Long tagId)
    {
        startPage();
        List<Article> list = articleService.selectArticlesByTagId(tagId);
        return getDataTable(list);
    }

    /**
     * 从参数中解析文章对象
     */
    private Article parseArticleFromParams(Map<String, Object> params) {
        Article article = new Article();

        if (params.get("id") != null) {
            article.setId(Long.valueOf(params.get("id").toString()));
        }
        if (params.get("title") != null) {
            article.setTitle(params.get("title").toString());
        }
        if (params.get("summary") != null) {
            article.setSummary(params.get("summary").toString());
        }
        if (params.get("content") != null) {
            article.setContent(params.get("content").toString());
        }
        if (params.get("coverUrl") != null) {
            article.setCoverUrl(params.get("coverUrl").toString());
        }
        if (params.get("categoryId") != null) {
            String categoryIdStr = params.get("categoryId").toString();
            try {
                // 检查是否为有效的数字字符串
                if (!categoryIdStr.isEmpty() && !categoryIdStr.equals("NaN")) {
                    article.setCategoryId(Long.valueOf(categoryIdStr));
                }
            } catch (NumberFormatException e) {
                // 忽略无效的 categoryId 参数
                log.warn("无效的 categoryId 参数: {}", categoryIdStr);
            }
        }
        if (params.get("status") != null) {
            article.setStatus(Long.valueOf(params.get("status").toString()));
        }
        if (params.get("publishTime") != null) {
            String publishTimeStr = params.get("publishTime").toString().trim();
            if (!publishTimeStr.isEmpty()) {
                java.time.LocalDateTime parsed = ModernDateUtils.parseDateTime(publishTimeStr);
                if (parsed != null) {
                    article.setPublishTime(ModernDateUtils.toDate(parsed));
                } else {
                    log.warn("无效的发布时间参数: {}", publishTimeStr);
                }
            }
        }
        if (params.get(PARAM_IS_TOP) != null) {
            article.setIsTop(Long.valueOf(params.get(PARAM_IS_TOP).toString()));
        }
        if (params.get(PARAM_IS_RECOMMEND) != null) {
            article.setIsRecommend(Long.valueOf(params.get(PARAM_IS_RECOMMEND).toString()));
        }
        if (params.get("authorId") != null) {
            article.setAuthorId(Long.valueOf(params.get("authorId").toString()));
        }
        if (params.get("authorName") != null) {
            article.setAuthorName(params.get("authorName").toString());
        }
        // 内容类型：核心业务管理复用本接口（article_type=business），漏映射会回退成 news
        if (params.get("articleType") != null) {
            String articleType = params.get("articleType").toString().trim();
            if (ARTICLE_TYPES.contains(articleType)) {
                article.setArticleType(articleType);
            } else {
                log.warn("无效的 articleType 参数: {}", articleType);
            }
        }
        if (params.get("slug") != null) {
            String slug = params.get("slug").toString().trim();
            if (!slug.isEmpty()) {
                article.setSlug(slug);
            }
        }
        if (params.get("industry") != null) {
            article.setIndustry(params.get("industry").toString());
        }
        if (params.get("scenario") != null) {
            article.setScenario(params.get("scenario").toString());
        }
        if (params.get("seoTitle") != null) {
            article.setSeoTitle(params.get("seoTitle").toString());
        }
        if (params.get("seoKeywords") != null) {
            article.setSeoKeywords(params.get("seoKeywords").toString());
        }
        if (params.get("seoDescription") != null) {
            article.setSeoDescription(params.get("seoDescription").toString());
        }
        // 业务详情结构化字段（business/case/solution）：同为逐字段白名单，漏映射会静默丢失
        if (params.get("sellingPoints") != null) {
            article.setSellingPoints(params.get("sellingPoints").toString());
        }
        if (params.get("specTable") != null) {
            article.setSpecTable(params.get("specTable").toString());
        }
        if (params.get("gallery") != null) {
            article.setGallery(params.get("gallery").toString());
        }
        if (params.get("processSteps") != null) {
            article.setProcessSteps(params.get("processSteps").toString());
        }

        // Markdown 文章：保存源码并由服务端渲染 HTML 双写
        String formatValue = params.get("format") == null ? null : params.get("format").toString();
        boolean markdown = "markdown".equalsIgnoreCase(formatValue);
        article.setFormat(markdown ? "markdown" : "html");
        if (params.get("contentMd") != null) {
            article.setContentMd(params.get("contentMd").toString());
        }
        if (markdown && StringUtils.isNotEmpty(article.getContentMd())) {
            article.setContent(MarkdownUtil.toHtml(article.getContentMd()));
        }

        return article;
    }
    
    /**
     * 解析标签ID列表
     */
    @SuppressWarnings("unchecked")
    private List<Long> parseTagIds(Object tagIdsObj) {
        List<Long> tagIds = new java.util.ArrayList<>();
        if (tagIdsObj != null) {
            if (tagIdsObj instanceof List) {
                List<Object> tagIdObjs = (List<Object>) tagIdsObj;
                for (Object tagIdObj : tagIdObjs) {
                    if (tagIdObj instanceof Long) {
                        tagIds.add((Long) tagIdObj);
                    } else if (tagIdObj instanceof Integer) {
                        tagIds.add(((Integer) tagIdObj).longValue());
                    } else if (tagIdObj instanceof String) {
                        try {
                            tagIds.add(Long.parseLong((String) tagIdObj));
                        } catch (NumberFormatException e) {
                            // 忽略无效的数字
                        }
                    }
                }
            }
        }
        return tagIds;
    }
}
