package com.zhi.system.mapper;

import java.util.List;
import java.util.Map;
import com.zhi.system.domain.Bookmark;
import org.apache.ibatis.annotations.Param;

/**
 * 文章收藏Mapper接口
 *
 * @author nevell
 * @date 2026-01-18
 */
public interface BookmarkMapper {

    /**
     * 查询文章收藏
     *
     * @param id 文章收藏主键
     * @return 文章收藏
     */
    public Bookmark selectBookmarkById(Long id);

    /**
     * 查询文章收藏列表
     *
     * @param bookmark 文章收藏
     * @return 文章收藏集合
     */
    public List<Bookmark> selectBookmarkList(Bookmark bookmark);

    /**
     * 新增文章收藏
     *
     * @param bookmark 文章收藏
     * @return 结果
     */
    public int insertBookmark(Bookmark bookmark);

    /**
     * 删除文章收藏
     *
     * @param id 文章收藏主键
     * @return 结果
     */
    public int deleteBookmarkById(Long id);

    /**
     * 根据用户ID和文章ID删除收藏
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 结果
     */
    public int deleteBookmarkByUserIdAndArticleId(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /**
     * 检查收藏是否已存在
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 结果
     */
    public int existsBookmark(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /**
     * 查询用户的收藏列表
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    public List<Bookmark> selectBookmarksByUserId(Long userId);

    /**
     * 查询用户收藏的文章列表（联查已发布文章信息）
     *
     * @param userId 用户ID
     * @return 收藏文章集合（Map：bookmarkId/articleId/title/summary/coverUrl/authorName/viewCount/likeCount/commentCount/bookmarkTime）
     */
    public List<Map<String, Object>> selectBookmarkArticlesByUserId(Long userId);
}
