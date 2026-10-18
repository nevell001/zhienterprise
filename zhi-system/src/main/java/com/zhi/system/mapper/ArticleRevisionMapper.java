package com.zhi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.zhi.system.domain.ArticleRevision;

/**
 * 文章修订历史Mapper接口
 *
 * @author nevell
 */
public interface ArticleRevisionMapper
{
    /** 版本列表（不含正文等大字段） */
    List<ArticleRevision> selectRevisionList(@Param("articleId") Long articleId);

    /** 单个版本详情（含全部内容字段） */
    ArticleRevision selectRevisionById(Long id);

    /** 最新版本（用于判断内容是否变化，含全部内容字段） */
    ArticleRevision selectLatestRevision(@Param("articleId") Long articleId);

    int insertRevision(ArticleRevision revision);

    /** 删除指定版本号及更早的版本 */
    int deleteRevisionsUpTo(@Param("articleId") Long articleId, @Param("revisionNo") Integer revisionNo);

    /** 彻底删除文章时清理其全部修订历史 */
    int deleteRevisionsByArticleIds(Long[] articleIds);
}
