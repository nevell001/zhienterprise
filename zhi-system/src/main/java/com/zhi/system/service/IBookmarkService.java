package com.zhi.system.service;

import java.util.List;
import java.util.Map;
import com.zhi.system.domain.Bookmark;

/**
 * 文章收藏Service接口
 *
 * @author nevell
 * @date 2026-01-18
 */
public interface IBookmarkService {

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
    public int deleteByUserIdAndArticleId(Long userId, Long articleId);

    /**
     * 切换收藏状态
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 结果：1=已收藏，0=已取消
     */
    public int toggleBookmark(Long userId, Long articleId);

    /**
     * 检查用户是否已收藏文章
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 结果
     */
    public boolean isBookmarked(Long userId, Long articleId);

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
     * @return 收藏文章集合
     */
    public List<Map<String, Object>> selectBookmarkArticlesByUserId(Long userId);
}
