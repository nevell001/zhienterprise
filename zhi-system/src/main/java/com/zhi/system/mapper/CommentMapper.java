package com.zhi.system.mapper;

import java.util.List;
import com.zhi.system.domain.Comment;

/**
 * 博客评论Mapper接口
 * 
 * @author nevell
 */
public interface CommentMapper
{
    /**
     * 查询博客评论
     * 
     * @param id 博客评论主键
     * @return 博客评论
     */
    public Comment selectCommentById(Long id);

    /**
     * 查询博客评论列表
     * 
     * @param comment 博客评论
     * @return 博客评论集合
     */
    public List<Comment> selectCommentList(Comment comment);

    /**
     * 查询前台公开评论列表（不含 email 等 PII）
     *
     * @param comment 评论查询条件
     * @return 评论集合
     */
    public List<Comment> selectFrontCommentList(Comment comment);

    /**
     * 新增博客评论
     * 
     * @param comment 博客评论
     * @return 结果
     */
    public int insertComment(Comment comment);

    /**
     * 修改博客评论
     * 
     * @param comment 博客评论
     * @return 结果
     */
    public int updateComment(Comment comment);

    /**
     * 批量删除博客评论
     * 
     * @param ids 需要删除的博客评论主键集合
     * @return 结果
     */
    public int deleteCommentByIds(Long[] ids);

    /**
     * 删除博客评论信息
     * 
     * @param id 博客评论主键
     * @return 结果
     */
    public int deleteCommentById(Long id);

    /**
     * 统计博客评论数量
     * 
     * @param comment 查询条件
     * @return 评论数量
     */
    public Long selectCommentCount(Comment comment);
}