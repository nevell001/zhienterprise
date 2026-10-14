package com.zhi.system.service.impl;

import java.util.List;
import java.util.Map;
import com.zhi.system.domain.Bookmark;
import com.zhi.system.mapper.BookmarkMapper;
import com.zhi.system.service.IBookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文章收藏Service实现
 *
 * @author nevell
 * @date 2026-01-18
 */
@Service
public class BookmarkServiceImpl implements IBookmarkService {

    @Autowired
    private BookmarkMapper bookmarkMapper;

    /**
     * 查询文章收藏
     *
     * @param id 文章收藏主键
     * @return 文章收藏
     */
    @Override
    public Bookmark selectBookmarkById(Long id) {
        return bookmarkMapper.selectBookmarkById(id);
    }

    /**
     * 查询文章收藏列表
     *
     * @param bookmark 文章收藏
     * @return 文章收藏
     */
    @Override
    public List<Bookmark> selectBookmarkList(Bookmark bookmark) {
        return bookmarkMapper.selectBookmarkList(bookmark);
    }

    /**
     * 新增文章收藏
     *
     * @param bookmark 文章收藏
     * @return 结果
     */
    @Override
    public int insertBookmark(Bookmark bookmark) {
        return bookmarkMapper.insertBookmark(bookmark);
    }

    /**
     * 删除文章收藏
     *
     * @param id 文章收藏主键
     * @return 结果
     */
    @Override
    public int deleteBookmarkById(Long id) {
        return bookmarkMapper.deleteBookmarkById(id);
    }

    /**
     * 根据用户ID和文章ID删除收藏
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 结果
     */
    @Override
    public int deleteByUserIdAndArticleId(Long userId, Long articleId) {
        return bookmarkMapper.deleteBookmarkByUserIdAndArticleId(userId, articleId);
    }

    /**
     * 切换收藏状态
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 结果：1=已收藏，0=已取消
     */
    @Override
    public int toggleBookmark(Long userId, Long articleId) {
        // 检查是否已收藏
        int exists = bookmarkMapper.existsBookmark(userId, articleId);
        if (exists > 0) {
            // 已收藏，取消收藏
            bookmarkMapper.deleteBookmarkByUserIdAndArticleId(userId, articleId);
            return 0;
        } else {
            // 未收藏，添加收藏
            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(userId);
            bookmark.setArticleId(articleId);
            bookmarkMapper.insertBookmark(bookmark);
            return 1;
        }
    }

    /**
     * 检查用户是否已收藏文章
     *
     * @param userId 用户ID
     * @param articleId 文章ID
     * @return 结果
     */
    @Override
    public boolean isBookmarked(Long userId, Long articleId) {
        return bookmarkMapper.existsBookmark(userId, articleId) > 0;
    }

    /**
     * 查询用户的收藏列表
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    @Override
    public List<Bookmark> selectBookmarksByUserId(Long userId) {
        return bookmarkMapper.selectBookmarksByUserId(userId);
    }

    /**
     * 查询用户收藏的文章列表（联查已发布文章信息）
     *
     * @param userId 用户ID
     * @return 收藏文章集合
     */
    @Override
    public List<Map<String, Object>> selectBookmarkArticlesByUserId(Long userId) {
        return bookmarkMapper.selectBookmarkArticlesByUserId(userId);
    }
}
