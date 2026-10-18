package com.zhi.system.service;

import java.util.List;
import java.util.Map;
import com.zhi.system.domain.Article;

/**
 * 博客文章Service接口
 * 
 * @author nevell
 * @date 2025-07-18
 */
public interface IArticleService 
{
    /**
     * 查询博客文章
     * 
     * @param id 博客文章主键
     * @return 博客文章
     */
    public Article selectArticleById(Long id);

    /**
     * 查询博客文章列表
     * 
     * @param article 博客文章
     * @return 博客文章集合
     */
    public List<Article> selectArticleList(Article article);

    /**
     * 站点地图专用列表（只含 id 与更新时间）
     *
     * @return 文章集合
     */
    public List<Article> selectSitemapArticles();

    /**
     * 按别名查询文章（仅命中已发布且未删除的文章，slug 伪静态入口用）
     *
     * @param slug 文章别名
     * @return 博客文章
     */
    public Article selectArticleBySlug(String slug);

    /**
     * 导出专用列表（含正文）
     *
     * @param article 查询条件
     * @return 文章集合
     */
    public List<Article> selectArticleListForExport(Article article);

    /**
     * 查询博客文章列表（带缓存）
     * 
     * @param article 博客文章
     * @return 博客文章集合
     */
    public List<Article> selectArticleListWithCache(Article article);

    /**
     * 新增博客文章
     * 
     * @param article 博客文章
     * @return 结果
     */
    public int insertArticle(Article article);

    /**
     * 修改博客文章
     * 
     * @param article 博客文章
     * @return 结果
     */
    public int updateArticle(Article article);

    /**
     * 批量删除博客文章
     * 
     * @param ids 需要删除的博客文章主键
     * @return 结果
     */
    public int deleteArticleByIds(Long[] ids);

    /**
     * 删除博客文章信息
     * 
     * @param id 博客文章主键
     * @return 结果
     */
    public int deleteArticleById(Long id);

    /**
     * 从回收站恢复文章
     *
     * @param ids 需要恢复的文章主键
     * @return 结果
     */
    public int restoreArticleByIds(Long[] ids);

    /**
     * 彻底删除回收站中的文章（不可恢复）
     *
     * @param ids 需要彻底删除的文章主键
     * @return 结果
     */
    public int purgeArticleByIds(Long[] ids);

    /**
     * 增加文章浏览量（无去重，兼容旧调用）
     * 
     * @param id 文章ID
     */
    public void addViewCount(Long id);

    /**
     * 增加文章浏览量（按访问者去重）
     *
     * @param id 文章ID
     * @param viewerKey 访问者标识（用户ID 或 IP），同一访问者 24 小时内只计一次；
     *                  传 null 时不进行去重
     */
    public void addViewCount(Long id, String viewerKey);

    /**
     * 根据标签ID查询文章列表
     * 
     * @param tagId 标签ID
     * @return 文章列表
     */
    public List<Article> selectArticlesByTagId(Long tagId);

    /**
     * 获取上一篇文章
     * 
     * @param id 当前文章ID
     * @return 上一篇文章
     */
    public Article getPrevArticle(Long id);

    /**
     * 获取下一篇文章
     * 
     * @param id 当前文章ID
     * @return 下一篇文章
     */
    public Article getNextArticle(Long id);

    /**
     * 获取文章归档（按年月分组）
     * 
     * @return 归档列表
     */
    public List<Map<String, Object>> getArticleArchive();

    /**
     * 搜索文章（根据标题和内容搜索）
     * 
     * @param keyword 搜索关键词
     * @param article 查询条件
     * @return 文章列表
     */
    public List<Article> searchArticles(String keyword, Article article);

    /**
     * 插入文章标签关联关系
     *
     * @param articleId 文章ID
     * @param tagIds 标签ID列表
     * @return 结果
     */
    public int insertArticleTagRelations(Long articleId, List<Long> tagIds);

    /**
     * 更新文章标签关联关系
     *
     * @param articleId 文章ID
     * @param tagIds 标签ID列表
     * @return 结果
     */
    public int updateArticleTagRelations(Long articleId, List<Long> tagIds);

    /**
     * 删除文章标签关联关系
     *
     * @param articleId 文章ID
     * @return 结果
     */
    public int deleteArticleTagRelations(Long articleId);

    /**
     * 批量更新文章状态
     *
     * @param ids 文章ID列表
     * @param status 状态
     * @return 结果
     */
    public int updateArticleStatus(List<Long> ids, Integer status);

    /**
     * 查询博客文章数量
     * 
     * @param article 查询条件
     * @return 文章数量
     */
    public Long selectArticleCount(Article article);

    /**
     * 获取文章总浏览量
     * 
     * @return 总浏览量
     */
    public Long selectTotalViewCount();

    /**
     * 获取文章平均浏览量
     *
     * @return 平均浏览量
     */
    public Double selectAverageViewCount();

    /**
     * 获取文章发布趋势（按月统计）
     *
     * @return 文章发布趋势数据
     */
    public List<Map<String, Object>> selectArticleTrend();

    /**
     * 获取热门文章（按浏览量降序排序）
     *
     * @param article 查询条件
     * @return 热门文章列表
     */
    public List<Article> selectHotArticles(Article article);

    /**
     * 根据归档月份获取文章列表
     *
     * @param article 查询条件（包含archiveDate）
     * @return 文章列表
     */
    public List<Article> selectArticlesByArchive(Article article);

    /**
     * 批量更新文章置顶状态
     *
     * @param ids 文章ID列表
     * @param isTop 置顶状态（0-不置顶，1-置顶）
     * @return 结果
     */
    public int updateArticleTopStatus(List<Long> ids, Integer isTop);

    /**
     * 批量更新文章推荐状态
     *
     * @param ids 文章ID列表
     * @param isRecommend 推荐状态（0-不推荐，1-推荐）
     * @return 结果
     */
    public int updateArticleRecommendStatus(List<Long> ids, Integer isRecommend);

    /**
     * 点赞文章
     *
     * @param id 文章ID
     * @return 结果
     */
    public int likeArticle(Long id);

    /**
     * 获取相关文章（基于相同分类或标签）
     *
     * @param id 当前文章ID
     * @return 相关文章列表
     */
    public List<Article> selectRelatedArticles(Long id);

    /**
     * 定时发布：将已到发布时间的定时文章（status=2）置为已发布
     *
     * @return 发布的文章数量
     */
    public int publishScheduledArticles();
}