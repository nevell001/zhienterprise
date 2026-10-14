package com.zhi.web.controller.site.vo;

import com.zhi.system.domain.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 前台公开评论视图对象。
 *
 * <p>前台评论接口是 {@code @Anonymous}，返回体里绝不能出现评论者邮箱等 PII。
 * 即使 SQL 已经只查展示字段，实体序列化仍会把 email 输出出去（单元测试即抓到过），
 * 所以这里做一层显式投影：只有下列字段会进入 JSON。</p>
 */
public class FrontCommentVo
{
    private Long id;
    private Long articleId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private Long parentId;
    private Long replyUserId;
    private String status;
    private Long likeCount;
    private Date createTime;
    private List<FrontCommentVo> replies = new ArrayList<>();

    public static FrontCommentVo from(Comment comment)
    {
        if (comment == null)
        {
            return null;
        }
        FrontCommentVo vo = new FrontCommentVo();
        vo.id = comment.getId();
        vo.articleId = comment.getArticleId();
        vo.userId = comment.getUserId();
        vo.nickname = comment.getNickname();
        vo.avatar = comment.getAvatar();
        vo.content = comment.getContent();
        vo.parentId = comment.getParentId();
        vo.replyUserId = comment.getReplyUserId();
        vo.status = comment.getStatus();
        vo.likeCount = comment.getLikeCount();
        vo.createTime = comment.getCreateTime();
        return vo;
    }

    public Long getId() { return id; }
    public Long getArticleId() { return articleId; }
    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public String getContent() { return content; }
    public Long getParentId() { return parentId; }
    public Long getReplyUserId() { return replyUserId; }
    public String getStatus() { return status; }
    public Long getLikeCount() { return likeCount; }
    public Date getCreateTime() { return createTime; }
    public List<FrontCommentVo> getReplies() { return replies; }
}
