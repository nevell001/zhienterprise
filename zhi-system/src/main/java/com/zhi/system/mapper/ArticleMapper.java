package com.zhi.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.zhi.system.domain.Article;
import com.zhi.system.domain.MediaReference;

/**
 * 博客文章Mapper接口
 * 
 * @author nevell
 * @date 2025-07-18
 */
public interface ArticleMapper 
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
     * 站点地图专用：只取 id 与更新时间（不带正文），并限制条数
     *
     * @return 文章集合
     */
    public List<Article> selectSitemapArticles();

    /**
     * 导出专用：带正文（列表接口用轻量投影，不含 longtext）
     *
     * @param article 查询条件
     * @return 文章集合
     */
    public List<Article> selectArticleListForExport(Article article);

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
     * 删除博客文章
     * 
     * @param id 博客文章主键
     * @return 结果
     */
    public int deleteArticleById(Long id);

    /**
     * 批量删除博客文章（逻辑删除，进入回收站）
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteArticleByIds(Long[] ids);

    /**
     * 批量恢复回收站中的文章
     *
     * @param ids 需要恢复的数据主键集合
     * @return 结果
     */
    public int restoreArticleByIds(Long[] ids);

    /**
     * 批量彻底删除回收站中的文章（物理删除）
     *
     * @param ids 需要彻底删除的数据主键集合
     * @return 结果
     */
    public int purgeArticleByIds(Long[] ids);

    /**
     * 根据标题查询博客文章
     * @param title 文章标题
     * @return 博客文章
     */
    public Article selectArticleByTitle(String title);

    /**
     * 按别名查询文章（仅命中已发布且未删除的文章，slug 伪静态入口用）
     * @param slug 文章别名
     * @return 博客文章
     */
    public Article selectArticleBySlug(String slug);

    /**
     * 文章浏览量+1
     * @param id 文章ID
     */
    void addViewCount(Long id);

    void addIncrementViewCount(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("increment") Long increment);

    /**
     * 根据标签ID查询文章列表
     * @param tagId 标签ID
     * @return 文章列表
     */
    List<Article> selectArticlesByTagId(Long tagId);

    /**
     * 搜索文章（根据标题和内容搜索）
     * @param keyword 搜索关键词
     * @param article 查询条件
     * @return 文章列表
     */
    List<Article> searchArticles(@Param("keyword") String keyword, @Param("article") Article article);

    /**
     * 使用 MySQL FULLTEXT 索引搜索文章
     * @param keyword 搜索关键词
     * @param article 查询条件
     * @return 文章列表
     */
    List<Article> searchArticlesFullText(@Param("keyword") String keyword, @Param("article") Article article);

    /**
     * 获取文章归档（按年月分组）
     * @return 归档列表
     */
    List<Map<String, Object>> getArticleArchive();

    /**
     * 获取上一篇文章
     * @param id 当前文章ID
     * @return 上一篇文章
     */
    Article getPrevArticle(Long id);

    /**
     * 获取下一篇文章
     * @param id 当前文章ID
     * @return 下一篇文章
     */
    Article getNextArticle(Long id);

    /**
     * 统计分类被文章引用数量（仅统计未删除文章）
     * @param ids 分类ID集合
     * @return 被引用的文章数量
     */
    int countByCategoryIds(Long[] ids);

    /**
     * 查询博客文章数量
     * 
     * @param article 查询条件
     * @return 文章数量
     */
    Long selectArticleCount(Article article);

    /**
     * 获取文章总浏览量
     * 
     * @return 总浏览量
     */
    Long selectTotalViewCount();

    /**
     * 获取文章平均浏览量
     *
     * @return 平均浏览量
     */
    Double selectAverageViewCount();

    /**
     * 获取文章发布趋势（按月统计）
     *
     * @return 文章发布趋势数据
     */
    List<Map<String, Object>> selectArticleTrend();

    /**
     * 获取热门文章（按浏览量降序排序）
     *
     * @param article 查询条件
     * @return 热门文章列表
     */
    List<Article> selectHotArticles(Article article);

    /**
     * 根据归档月份获取文章列表
     *
     * @param article 查询条件（包含archiveDate）
     * @return 文章列表
     */
    List<Article> selectArticlesByArchive(Article article);

    /**
     * 批量更新文章置顶状态
     *
     * @param ids 文章ID列表
     * @param isTop 置顶状态（0-不置顶，1-置顶）
     * @return 结果
     */
    int updateArticleTopStatus(@Param("ids") List<Long> ids, @Param("isTop") Integer isTop);

    /**
     * 批量更新文章推荐状态
     *
     * @param ids 文章ID列表
     * @param isRecommend 推荐状态（0-不推荐，1-推荐）
     * @return 结果
     */
    int updateArticleRecommendStatus(@Param("ids") List<Long> ids, @Param("isRecommend") Integer isRecommend);

    /**
     * 点赞文章
     *
     * @param id 文章ID
     * @return 结果
     */
    int likeArticle(@Param("id") Long id);

    /**
     * 获取相关文章（基于相同分类或标签）
     *
     * @param id 当前文章ID
     * @param limit 最大返回数量
     * @return 相关文章列表
     */
    List<Article> selectRelatedArticles(@Param("id") Long id, @Param("limit") int limit);

    /**
     * 定时发布：将已到发布时间的定时文章（status=2）置为已发布
     *
     * @return 发布的文章数量
     */
    int publishScheduledArticles();

    /**
     * 增减文章评论数字段（不进入负数）
     *
     * @param articleId 文章ID
     * @param delta 增量（+1/-1）
     * @return 结果
     */
    int changeCommentCount(@Param("articleId") Long articleId, @Param("delta") int delta);

    /**
     * 查询引用了指定文件名的文章（媒体库删除前检查，含回收站内容）
     *
     * @param keyword 文件名
     * @return 引用位置
     */
    List<MediaReference> selectMediaReferences(@Param("keyword") String keyword);
}
