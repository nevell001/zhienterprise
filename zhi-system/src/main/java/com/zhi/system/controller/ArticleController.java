package com.zhi.system.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.multipart.MultipartFile;
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

    /** Markdown 批量导入：单次请求最大文件数 */
    private static final int MAX_IMPORT_FILES = 20;
    /** 文章标题长度上限（对齐 zhi_article.title varchar(200)） */
    private static final int MAX_IMPORT_TITLE_LENGTH = 200;

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
     * 查询文章的修订历史（不含正文）
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:query')")
    @GetMapping("/revisions/{articleId}")
    public AjaxResult revisions(@PathVariable("articleId") Long articleId)
    {
        return success(articleService.selectArticleRevisions(articleId));
    }

    /**
     * 获取单个修订版本详情（含正文）
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:query')")
    @GetMapping("/revision/{revisionId}")
    public AjaxResult revision(@PathVariable("revisionId") Long revisionId)
    {
        return success(articleService.selectArticleRevision(revisionId));
    }

    /**
     * 恢复到指定历史版本
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PostMapping("/revision/{revisionId}/restore")
    public AjaxResult restoreRevision(@PathVariable("revisionId") Long revisionId)
    {
        return toAjax(articleService.restoreArticleRevision(revisionId));
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
     * 删除文章（逻辑删除，进入回收站）
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:remove')")
    @Log(title = "文章管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(articleService.deleteArticleByIds(ids));
    }

    /**
     * 从回收站恢复文章
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:edit')")
    @Log(title = "文章管理", businessType = BusinessType.UPDATE)
    @PutMapping("/restore/{ids}")
    public AjaxResult restore(@PathVariable("ids") Long[] ids)
    {
        return toAjax(articleService.restoreArticleByIds(ids));
    }

    /**
     * 彻底删除回收站中的文章（物理删除，不可恢复）
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:remove')")
    @Log(title = "文章管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/purge/{ids}")
    public AjaxResult purge(@PathVariable("ids") Long[] ids)
    {
        return toAjax(articleService.purgeArticleByIds(ids));
    }

    /**
     * Markdown 批量导入：每个 .md 文件生成一篇文章（首个一级标题作为标题，其余为正文）。
     * <p>逐文件独立处理：单个文件失败不影响其他文件，结果按文件返回给前端展示。
     * 正文沿用「源码 + 服务端渲染 HTML 双写」的 Markdown 文章约定。</p>
     */
    @PreAuthorize("@ss.hasPermi('zhi:article:add')")
    @Log(title = "文章管理", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public AjaxResult importMarkdown(@RequestParam(value = "files", required = false) MultipartFile[] files,
                                     @RequestParam(value = "categoryId", required = false) Long categoryId,
                                     @RequestParam(value = "articleType", required = false) String articleType,
                                     @RequestParam(value = "status", required = false, defaultValue = "0") Integer status)
    {
        if (files == null || files.length == 0) {
            return error("请选择要导入的 Markdown 文件");
        }
        if (files.length > MAX_IMPORT_FILES) {
            return error("单次最多导入 " + MAX_IMPORT_FILES + " 个文件");
        }
        if (status == null || (status != 0 && status != 1)) {
            return error("导入仅支持「草稿」或「发布」状态");
        }
        String normalizedType = articleType == null ? null : articleType.trim();
        if (StringUtils.isNotEmpty(normalizedType) && !ARTICLE_TYPES.contains(normalizedType)) {
            log.warn("无效的 articleType 参数: {}", normalizedType);
            normalizedType = null;
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int imported = 0;
        for (MultipartFile file : files)
        {
            String fileName = file.getOriginalFilename();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fileName", fileName);
            try
            {
                String markdown = readMarkdown(file, fileName);
                String[] parsed = splitMarkdownTitle(markdown);
                String title = parsed[0] != null ? parsed[0] : fileNameWithoutExtension(fileName);
                if (title.isEmpty()) {
                    throw new IllegalArgumentException("无法确定文章标题");
                }
                if (title.length() > MAX_IMPORT_TITLE_LENGTH) {
                    throw new IllegalArgumentException("标题超过 " + MAX_IMPORT_TITLE_LENGTH + " 个字符");
                }

                Article article = new Article();
                article.setTitle(title);
                article.setFormat("markdown");
                article.setContentMd(parsed[1]);
                article.setContent(MarkdownUtil.toHtml(parsed[1]));
                article.setCategoryId(categoryId);
                article.setArticleType(normalizedType);
                article.setStatus(status.longValue());

                int rows = articleService.insertArticle(article);
                if (rows > 0) {
                    imported++;
                    item.put("success", true);
                    item.put("title", title);
                    item.put("articleId", article.getId());
                } else {
                    item.put("success", false);
                    item.put("message", "写入失败");
                }
            }
            catch (Exception e)
            {
                item.put("success", false);
                item.put("message", e.getMessage() == null ? "导入失败" : e.getMessage());
            }
            results.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", files.length);
        data.put("imported", imported);
        data.put("failed", files.length - imported);
        data.put("results", results);
        return AjaxResult.success("导入完成", data);
    }

    /**
     * 读取 Markdown 文本：校验扩展名与非空，按 UTF-8 解码并去掉 BOM
     */
    private String readMarkdown(MultipartFile file, String fileName)
    {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String name = fileName == null ? "" : fileName.toLowerCase();
        if (!name.endsWith(".md") && !name.endsWith(".markdown")) {
            throw new IllegalArgumentException("仅支持 .md / .markdown 文件");
        }
        String text;
        try
        {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        catch (java.io.IOException e)
        {
            throw new IllegalArgumentException("文件读取失败");
        }
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("文件内容为空");
        }
        return text;
    }

    /**
     * 拆分 Markdown：只看首个非空行，若为「# 标题」则作为文章标题并从正文移除。
     *
     * @return 长度为 2 的数组，[0] 为标题（正文未提供时返回 null），[1] 为正文
     */
    private static String[] splitMarkdownTitle(String markdown)
    {
        String[] lines = markdown.split("\r\n|\r|\n", -1);
        int titleLine = -1;
        String title = null;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("# ")) {
                String candidate = line.substring(2).trim();
                if (!candidate.isEmpty()) {
                    title = candidate;
                    titleLine = i;
                }
            }
            break;
        }
        if (title == null) {
            return new String[] { null, markdown };
        }
        StringBuilder body = new StringBuilder();
        for (int i = titleLine + 1; i < lines.length; i++)
        {
            if (body.length() == 0 && lines[i].trim().isEmpty()) {
                continue;
            }
            body.append(lines[i]);
            if (i < lines.length - 1) {
                body.append('\n');
            }
        }
        return new String[] { title, body.toString() };
    }

    /** 取文件名（去掉目录与扩展名）作为无标题 Markdown 的文章标题 */
    private static String fileNameWithoutExtension(String fileName)
    {
        if (fileName == null) {
            return "";
        }
        String name = fileName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.trim();
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
        // Jackson 把 JSON 数组反序列化成 List<Integer>，直接 (List<Long>) 强转会在
        // service 迭代时抛 ClassCastException；统一走 Long 转换。
        List<Long> ids = parseTagIds(params.get("ids"));
        Integer status = parseIntParam(params.get("status"));

        if (ids.isEmpty())
        {
            return error(MSG_SELECT_ARTICLES_TO_UPDATE);
        }
        if (status == null)
        {
            return error("状态不能为空");
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
        List<Long> ids = parseTagIds(params.get("ids"));
        Integer isTop = parseIntParam(params.get(PARAM_IS_TOP));

        if (ids.isEmpty())
        {
            return error(MSG_SELECT_ARTICLES_TO_UPDATE);
        }
        if (isTop == null)
        {
            return error("置顶状态不能为空");
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
        List<Long> ids = parseTagIds(params.get("ids"));
        Integer isRecommend = parseIntParam(params.get(PARAM_IS_RECOMMEND));

        if (ids.isEmpty())
        {
            return error(MSG_SELECT_ARTICLES_TO_UPDATE);
        }
        if (isRecommend == null)
        {
            return error("推荐状态不能为空");
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

        // Markdown 文章：保存源码并由服务端渲染 HTML 双写。
        // 仅在显式提交 format 时覆盖：列表「发布/转草稿」等局部更新不带 format，
        // 默认写 html 会把 Markdown 文章静默改成 HTML（下次编辑保存还会清空 content_md）。
        String formatValue = params.get("format") == null ? null : params.get("format").toString().trim();
        boolean markdown = "markdown".equalsIgnoreCase(formatValue);
        if (formatValue != null) {
            article.setFormat(markdown ? "markdown" : "html");
        }
        if (params.get("contentMd") != null) {
            article.setContentMd(params.get("contentMd").toString());
        }
        if (markdown && StringUtils.isNotEmpty(article.getContentMd())) {
            article.setContent(MarkdownUtil.toHtml(article.getContentMd()));
        }

        return article;
    }
    
    /** 解析 JSON 数字参数（Integer/Long/String 均兼容） */
    private Integer parseIntParam(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
