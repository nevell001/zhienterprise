package com.zhi.system.service.impl;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import com.zhi.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.zhi.common.cache.annotation.SiteCacheable;
import com.zhi.common.cache.annotation.SiteCacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zhi.system.mapper.ArticleMapper;
import com.zhi.system.mapper.ArticleTagMapper;
import com.zhi.system.mapper.TagMapper;
import com.zhi.system.domain.Article;
import com.zhi.system.domain.ArticleTag;
import com.zhi.system.domain.Tag;
import com.zhi.system.service.IArticleService;
import com.zhi.common.exception.DuplicateArticleTitleException;

/**
 * 博客文章Service业务层处理
 * 
 * @author nevell
 * @date 2025-07-18
 */
@Service
public class ArticleServiceImpl implements IArticleService
{
    private static final Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private com.zhi.common.core.redis.RedisCache redisCache;

    @Autowired(required = false)
    private DataSource dataSource;

    private volatile Boolean mysqlDatabase;

    /**
     * 查询博客文章
     *
     * @param id 博客文章主键
     * @return 博客文章
     */
    @Override
    public Article selectArticleById(Long id)
    {
        Article article = articleMapper.selectArticleById(id);
        if (article != null) {
            // 查询文章关联的标签
            List<Tag> tags = tagMapper.selectTagsByArticleId(id);
            article.setTags(tags);
            
            // 设置标签ID列表
            List<Long> tagIds = new ArrayList<>();
            if (tags != null) {
                for (Tag tag : tags) {
                    if (tag != null && tag.getId() != null) {
                        tagIds.add(tag.getId());
                    }
                }
            }
            article.setTagIds(tagIds);
        }
        return article;
    }

    /**
     * 查询博客文章列表
     *
     * @param article 博客文章
     * @return 博客文章
     */
    @Override
    public List<Article> selectArticleListForExport(Article article)
    {
        return articleMapper.selectArticleListForExport(article);
    }

    @Override
    public List<Article> selectSitemapArticles()
    {
        return articleMapper.selectSitemapArticles();
    }

    @Override
    // 移除缓存以确保前台始终显示最新数据
    public List<Article> selectArticleList(Article article)
    {
        List<Article> articleList = articleMapper.selectArticleList(article);
        loadTagsForArticles(articleList);
        return articleList;
    }

    /**
     * 批量加载文章的标签数据（优化为 Stream 映射）
     *
     * @param articleList 文章列表
     */
    private void loadTagsForArticles(List<Article> articleList) {
        if (articleList == null || articleList.isEmpty()) {
            return;
        }

        List<Long> articleIds = articleList.stream()
                .filter(a -> a != null && a.getId() != null)
                .map(Article::getId)
                .toList();

        if (articleIds.isEmpty()) {
            return;
        }

        List<Map<String, Object>> tagMappings = tagMapper.selectTagsByArticleIds(articleIds);

        Map<Long, List<Tag>> tagsMap = tagMappings.stream()
                .filter(m -> m.get("articleId") != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> (Long) m.get("articleId"),
                        java.util.stream.Collectors.mapping(m -> {
                            Tag tag = new Tag();
                            tag.setId((Long) m.get("id"));
                            tag.setName((String) m.get("name"));
                            tag.setDescription((String) m.get("description"));
                            tag.setColor((String) m.get("color"));
                            tag.setIcon((String) m.get("icon"));
                            tag.setDelFlag((Integer) m.get("delFlag"));
                            return tag;
                        }, java.util.stream.Collectors.toList())
                ));

        articleList.stream()
                .filter(a -> a != null && a.getId() != null)
                .forEach(article -> {
                    List<Tag> tags = tagsMap.getOrDefault(article.getId(), new ArrayList<>());
                    article.setTags(tags);
                    // 过滤掉 ID 为 null 的标签
                    article.setTagIds(tags.stream()
                            .filter(t -> t != null && t.getId() != null)
                            .map(Tag::getId)
                            .toList());
                });
    }


    /**
     * 按别名查询文章（仅命中已发布且未删除的文章）
     */
    public Article selectArticleBySlug(String slug)
    {
        return articleMapper.selectArticleBySlug(slug);
    }

    /**
     * 校验并规范化文章别名：可空；非空时仅允许字母/数字/下划线/连字符，且全站唯一（未删除范围）
     */
    private void validateSlug(Article article)
    {
        String slug = article.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            return;
        }
        slug = slug.trim();
        if (!slug.matches("[A-Za-z0-9_-]{1,100}")) {
            throw new IllegalArgumentException("文章别名只允许字母、数字、下划线与连字符（1-100 个字符）");
        }
        Article exist = articleMapper.selectArticleBySlug(slug);
        if (exist != null && !exist.getId().equals(article.getId())) {
            throw new IllegalArgumentException("文章别名已存在：" + slug);
        }
        article.setSlug(slug);
    }

    /**
     * 新增博客文章
     *
     * 必须清缓存：新增文章后前台搜索（zhi:search:*，10 分钟 TTL）与热门列表
     * （zhi:hot:*）都会返回旧结果 —— 此前该方法无任何失效注解，新发布内容最长
     * 10 分钟"搜不到"。
     *
     * 注意 keyPattern 才是按通配符清除（deleteByPattern → Redis SCAN MATCH），
     * value() 是**字面**删除、不支持通配符，因此这里只依赖 keyPattern。
     *
     * @param article 博客文章
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SiteCacheEvict(keyPattern = "zhi:*")
    public int insertArticle(Article article)
    {
        validateArticleContent(article);
        String title = validateArticleTitle(article.getTitle());
        article.setTitle(title);
        validateSlug(article);
        article.setCreateTime(DateUtils.getNowDate());
        article.setAuthorId(com.zhi.common.utils.SecurityUtils.getLoginUser().getUser().getUserId());
        article.setAuthorName(com.zhi.common.utils.SecurityUtils.getLoginUser().getUser().getNickName());
        setDefaultValues(article);
        
        try {
            int result = articleMapper.insertArticle(article);
            processTagAssociations(article);
            return result;
        } catch (Exception e) {
            handleInsertException(e);
            throw e;
        }
    }

    /**
     * 验证文章内容
     */
    private void validateArticleContent(Article article)
    {
        if (article.getContent() == null || article.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("文章内容不能为空");
        }
    }

    /**
     * 验证文章标题
     */
    private String validateArticleTitle(String title)
    {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("文章标题不能为空");
        }
        title = title.trim();
        Article exist = articleMapper.selectArticleByTitle(title);
        if (exist != null) {
            throw new IllegalArgumentException("文章标题已存在");
        }
        return title;
    }

    /**
     * 设置默认值
     */
    private void setDefaultValues(Article article)
    {
        if (article.getDelFlag() == null) {
            article.setDelFlag(0L);
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0L);
        }
        if (article.getLikeCount() == null) {
            article.setLikeCount(0L);
        }
        if (article.getCommentCount() == null) {
            article.setCommentCount(0L);
        }
        if (article.getStatus() == null) {
            article.setStatus(0L);
        }
    }

    /**
     * 处理标签关联
     */
    private void processTagAssociations(Article article)
    {
        if (article.getTagIds() == null || article.getTagIds().isEmpty()) {
            return;
        }
        
        for (Object tagIdObj : article.getTagIds()) {
            Long tagId = convertToLong(tagIdObj);
            if (tagId != null) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(article.getId());
                articleTag.setTagId(tagId);
                articleTagMapper.insertArticleTag(articleTag);
            }
        }
    }

    /**
     * 将对象转换为 Long 类型
     */
    private Long convertToLong(Object obj)
    {
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 处理插入异常
     */
    private void handleInsertException(Exception e)
    {
        if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
            throw new DuplicateArticleTitleException("文章标题已存在");
        }
    }

    /**
     * 修改博客文章
     * 
     * @param article 博客文章
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SiteCacheEvict(keyPattern = "zhi:*")
    public int updateArticle(Article article)
    {
        if (article.getId() == null) {
            throw new IllegalArgumentException("文章ID不能为空");
        }
        String title = article.getTitle();
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("文章标题不能为空");
        }
        title = title.trim();
        article.setTitle(title);
        Article exist = articleMapper.selectArticleByTitle(title);
        if (exist != null && !exist.getId().equals(article.getId())) {
            throw new IllegalArgumentException("文章标题已存在");
        }
        validateSlug(article);
        // 如果只更新状态等其他字段，不校验标题和唯一性
        article.setUpdateTime(DateUtils.getNowDate());
        // 确保delFlag有默认值
        if (article.getDelFlag() == null) {
            article.setDelFlag(0L);
        }
        try {
            // 标签关联统一由 controller 层在显式传入 tagIds 时处理（updateArticleTagRelations），
            // 此处不再无条件删除，避免 API 调用方漏传 tagIds 时清空文章标签
            return articleMapper.updateArticle(article);
        } catch (Exception e) {
            // 检查是否为数据库唯一索引冲突
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                throw new DuplicateArticleTitleException("文章标题已存在，请更换标题！");
            }
            throw e;
        }
    }

    /**
     * 批量删除博客文章
     * 
     * @param ids 需要删除的博客文章主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SiteCacheEvict(keyPattern = "zhi:*")
    public int deleteArticleByIds(Long[] ids)
    {
        // 先删除标签关联
        for (Long id : ids) {
            articleTagMapper.deleteByArticleId(id);
        }
        return articleMapper.deleteArticleByIds(ids);
    }

    /**
     * 删除博客文章信息
     *
     * 必须清缓存：删除后前台列表/搜索/热门仍会返回该文章（缓存未失效）。
     *
     * @param id 博客文章主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SiteCacheEvict(keyPattern = "zhi:*")
    public int deleteArticleById(Long id)
    {
        // 先删除标签关联
        articleTagMapper.deleteByArticleId(id);
        return articleMapper.deleteArticleById(id);
    }

    @Override
    public void addViewCount(Long id) {
        addViewCount(id, null);
    }

    @Override
    public void addViewCount(Long id, String viewerKey) {
        // 使用 Redis 缓冲浏览量增量，由 ArticleViewSyncTask 定时批量同步到数据库
        String key = "zhi:article:view:" + id;

        // 按访问者去重：同一访问者 24 小时内对同一文章只计一次文章总浏览量
        if (viewerKey != null && !viewerKey.isEmpty()) {
            String dedupeKey = "zhi:article:viewed:" + id + ":" + viewerKey;
            if (Boolean.FALSE.equals(redisCache.hasKey(dedupeKey))) {
                redisCache.incrementCacheObject(key, 1);
                redisCache.setCacheObject(dedupeKey, "1", 24, TimeUnit.HOURS);
            }
            // 每日 PV/UV 与访问明细按“每次访问”统计（PV=访问次数，UV=当日去重访客），
            // 不受上面 24 小时文章浏览量去重影响，避免跨日访问漏计 UV
            recordDailyPvUv(id, viewerKey);
            return;
        }
        redisCache.incrementCacheObject(key, 1);
    }

    /**
     * 维护当日 PV/UV 的 Redis 计数（由定时任务按日汇总入 zhi_article_daily_stats）
     */
    private void recordDailyPvUv(Long articleId, String viewerKey) {
        try {
            String date = DateUtils.parseDateToStr("yyyyMMdd", DateUtils.getNowDate());
            String pvKey = "zhi:article:pv:" + date + ":" + articleId;
            redisCache.incrementCacheObject(pvKey, 1);

            // UV：以“日+文章+访客”去重键判断是否新的独立访客
            String uvDedupeKey = "zhi:article:uview:" + date + ":" + articleId + ":" + viewerKey;
            if (Boolean.FALSE.equals(redisCache.hasKey(uvDedupeKey))) {
                redisCache.incrementCacheObject("zhi:article:uv:" + date + ":" + articleId, 1);
                // 覆盖跨日边界，避免深夜访问被连续两天重复计数
                redisCache.setCacheObject(uvDedupeKey, "1", 48, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            logger.warn("记录当日 PV/UV 失败: articleId={}, error={}", articleId, e.getMessage());
        }
    }

    @Override
    public Article getPrevArticle(Long id) {
        return articleMapper.getPrevArticle(id);
    }

    @Override
    public Article getNextArticle(Long id) {
        return articleMapper.getNextArticle(id);
    }

    @Override
    public List<Map<String, Object>> getArticleArchive() {
        return articleMapper.getArticleArchive();
    }

    @Override
    public List<Article> selectArticlesByTagId(Long tagId) {
        List<Article> articleList = articleMapper.selectArticlesByTagId(tagId);
        loadTagsForArticles(articleList);
        return articleList;
    }

    @Override
    // 键写作"#keyword:#article"即可：CacheAspect 会代入实参（长对象只入哈希），
    // 并把 PageHelper 的当前页码并进键 —— 否则第 2 页会命中第 1 页的缓存
    @SiteCacheable(key = "zhi:search:#keyword:#article", ttl = 10, timeUnit = TimeUnit.MINUTES)
    public List<Article> searchArticles(String keyword, Article article) {
        List<Article> articleList;
        boolean useFullText = keyword != null && !keyword.trim().isEmpty() && isMysqlDatabase() && !fullTextDisabled;
        if (useFullText) {
            try {
                articleList = articleMapper.searchArticlesFullText(keyword, article);
            } catch (org.springframework.jdbc.UncategorizedSQLException ex) {
                // FULLTEXT 索引缺失时降级到 LIKE 搜索（错误码 1191）
                logger.warn("FULLTEXT 搜索失败，降级到 LIKE 搜索。如需启用全文搜索，请运行 sql/00_init_database.sql 初始化索引。原因: {}",
                        ex.getMessage());
                fullTextDisabled = true;
                articleList = articleMapper.searchArticles(keyword, article);
            }
            // FULLTEXT 返回空结果时也降级到 LIKE（FULLTEXT 可能因停用词/min_token_size 漏掉结果）
            if (articleList.isEmpty()) {
                logger.info("FULLTEXT 搜索无结果，尝试 LIKE 降级搜索。关键词: {}", keyword);
                articleList = articleMapper.searchArticles(keyword, article);
            }
        } else {
            articleList = articleMapper.searchArticles(keyword, article);
        }
        loadTagsForArticles(articleList);
        return articleList;
    }

    /**
     * FULLTEXT 索引是否已禁用（运行时检测到索引缺失后自动降级）
     */
    private volatile boolean fullTextDisabled = false;

    private boolean isMysqlDatabase() {
        if (mysqlDatabase != null) {
            return mysqlDatabase;
        }
        if (dataSource == null) {
            mysqlDatabase = false;
            return false;
        }
        try (java.sql.Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            mysqlDatabase = productName != null && productName.toLowerCase().contains("mysql");
        } catch (Exception e) {
            mysqlDatabase = false;
        }
        return mysqlDatabase;
    }

    /**
     * 插入文章标签关联关系
     *
     * @param articleId 文章ID
     * @param tagIds 标签ID列表
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertArticleTagRelations(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return 0;
        }
        
        int result = 0;
        for (Long tagId : tagIds) {
            ArticleTag articleTag = new ArticleTag();
            articleTag.setArticleId(articleId);
            articleTag.setTagId(tagId);
            result += articleTagMapper.insertArticleTag(articleTag);
        }
        return result;
    }

    /**
     * 更新文章标签关联关系
     *
     * @param articleId 文章ID
     * @param tagIds 标签ID列表
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateArticleTagRelations(Long articleId, List<Long> tagIds) {
        // 先删除原有关联
        deleteArticleTagRelations(articleId);
        
        // 插入新的关联
        return insertArticleTagRelations(articleId, tagIds);
    }

    /**
     * 删除文章标签关联关系
     *
     * @param articleId 文章ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteArticleTagRelations(Long articleId) {
        return articleTagMapper.deleteByArticleId(articleId);
    }

    /**
     * 批量更新文章状态
     *
     * @param ids 文章ID列表
     * @param status 状态
     * @return 结果
     */
    /**
     * 批量修改文章状态（发布/下架）
     *
     * 必须清缓存：状态变更直接决定文章是否出现在前台列表与搜索结果，
     * 失效缺失会让"已下架的文章仍可搜到 / 新发布的文章搜不到"，直到 TTL 到期。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SiteCacheEvict(keyPattern = "zhi:*")
    public int updateArticleStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        int result = 0;
        for (Long id : ids) {
            // 先查询文章，验证标题是否为空
            Article existingArticle = articleMapper.selectArticleById(id);
            if (existingArticle != null) {
                // 当状态为发布时，校验标题是否为空
                if (status == 1 && (existingArticle.getTitle() == null || existingArticle.getTitle().trim().isEmpty())) {
                    throw new IllegalArgumentException("文章标题不能为空");
                }
                
                // 创建更新对象，仅设置必要字段
                Article article = new Article();
                article.setId(id);
                article.setStatus(status.longValue());
                article.setUpdateTime(DateUtils.getNowDate());
                result += articleMapper.updateArticle(article);
            }
        }
        return result;
    }

    /**
     * 定时发布：将已到发布时间的定时文章（status=2）置为已发布
     *
     * @return 发布的文章数量
     */
    @Override
    public int publishScheduledArticles() {
        return articleMapper.publishScheduledArticles();
    }

    /**
     * 查询博客文章数量
     * 
     * @param article 查询条件
     * @return 文章数量
     */
    @Override
    public Long selectArticleCount(Article article)
    {
        return articleMapper.selectArticleCount(article);
    }

    /**
     * 获取文章总浏览量
     * 
     * @return 总浏览量
     */
    @Override
    public Long selectTotalViewCount()
    {
        return articleMapper.selectTotalViewCount();
    }

    /**
     * 获取文章平均浏览量
     *
     * @return 平均浏览量
     */
    @Override
    public Double selectAverageViewCount()
    {
        return articleMapper.selectAverageViewCount();
    }

    /**
     * 获取文章发布趋势（按月统计）
     *
     * @return 文章发布趋势数据
     */
    @Override
    public List<Map<String, Object>> selectArticleTrend()
    {
        return articleMapper.selectArticleTrend();
    }

    /**
     * 查询博客文章列表（带缓存，用于热门文章）
     *
     * @param article 博客文章
     * @return 博客文章集合
     */
    @Override
    @SiteCacheable(key = "zhi:hot:#article.hashCode()", ttl = 60, timeUnit = TimeUnit.MINUTES)
    public List<Article> selectArticleListWithCache(Article article)
    {
        return selectArticleList(article);
    }

    /**
     * 获取热门文章（按浏览量降序排序）
     *
     * @param article 查询条件
     * @return 热门文章列表
     */
    @Override
    public List<Article> selectHotArticles(Article article)
    {
        return articleMapper.selectHotArticles(article);
    }

    /**
     * 根据归档月份获取文章列表
     *
     * @param article 查询条件（包含archiveDate）
     * @return 文章列表
     */
    @Override
    public List<Article> selectArticlesByArchive(Article article)
    {
        return articleMapper.selectArticlesByArchive(article);
    }

    /**
     * 批量更新文章置顶状态
     *
     * @param ids 文章ID列表
     * @param isTop 置顶状态（0-不置顶，1-置顶）
     * @return 结果
     */
    @Override
    @SiteCacheEvict(keyPattern = "zhi:*")
    public int updateArticleTopStatus(List<Long> ids, Integer isTop)
    {
        if (ids == null || ids.isEmpty())
        {
            return 0;
        }
        return articleMapper.updateArticleTopStatus(ids, isTop);
    }

    /**
     * 批量更新文章推荐状态
     *
     * @param ids 文章ID列表
     * @param isRecommend 推荐状态（0-不推荐，1-推荐）
     * @return 结果
     */
    @Override
    @SiteCacheEvict(keyPattern = "zhi:*")
    public int updateArticleRecommendStatus(List<Long> ids, Integer isRecommend)
    {
        if (ids == null || ids.isEmpty())
        {
            return 0;
        }
        return articleMapper.updateArticleRecommendStatus(ids, isRecommend);
    }

    /**
     * 点赞文章
     *
     * @param id 文章ID
     * @return 结果
     */
    @Override
    @SiteCacheEvict(keyPattern = "zhi:*")
    public int likeArticle(Long id)
    {
        if (id == null)
        {
            return 0;
        }
        return articleMapper.likeArticle(id);
    }

    /**
     * 获取相关文章（基于相同分类）
     *
     * @param id 当前文章ID
     * @return 相关文章列表
     */
    @Override
    public List<Article> selectRelatedArticles(Long id)
    {
        List<Article> relatedList = articleMapper.selectRelatedArticles(id, 6);
        if (relatedList == null || relatedList.isEmpty()) {
            // 如果没有同分类文章，返回热门文章作为推荐
            Article query = new Article();
            query.setStatus(1L);
            query.setDelFlag(0L);
            relatedList = articleMapper.selectHotArticles(query);
            if (relatedList != null && relatedList.size() > 6) {
                relatedList = relatedList.subList(0, 6);
            }
        }
        return relatedList;
    }
}
