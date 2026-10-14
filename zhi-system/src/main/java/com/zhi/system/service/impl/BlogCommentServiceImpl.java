package com.zhi.system.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.zhi.common.utils.BlogSwitchUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.system.mapper.BlogArticleMapper;
import com.zhi.system.mapper.BlogCommentMapper;
import com.zhi.system.domain.BlogArticle;
import com.zhi.system.domain.BlogComment;
import com.zhi.system.domain.BlogNotification;
import com.zhi.system.service.IBlogArticleService;
import com.zhi.system.service.IBlogCommentService;
import com.zhi.system.service.IBlogEmailService;
import com.zhi.system.service.IBlogNotificationService;
import com.zhi.system.service.IBlogSettingService;
import com.zhi.system.service.ISysUserService;

/**
 * 博客评论Service业务层处理
 * 
 * @author nevell
 */
@Service
public class BlogCommentServiceImpl implements IBlogCommentService
{
    private static final Logger logger = LoggerFactory.getLogger(BlogCommentServiceImpl.class);

    @Autowired
    private BlogCommentMapper blogCommentMapper;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    @Autowired
    private IBlogArticleService blogArticleService;

    @Autowired
    private IBlogNotificationService blogNotificationService;

    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired
    private IBlogEmailService blogEmailService;

    @Autowired
    private ISysUserService sysUserService;

    /** 邮件通知开关配置键 */
    private static final String KEY_EMAIL_NOTIFY_ENABLED = "email_notify_enabled";

    /**
     * 查询博客评论
     * 
     * @param id 博客评论主键
     * @return 博客评论
     */
    @Override
    public BlogComment selectBlogCommentById(Long id)
    {
        return blogCommentMapper.selectBlogCommentById(id);
    }

    /**
     * 查询博客评论列表
     * 
     * @param blogComment 博客评论
     * @return 博客评论集合
     */
    @Override
    public List<BlogComment> selectBlogCommentList(BlogComment blogComment)
    {
        return blogCommentMapper.selectBlogCommentList(blogComment);
    }

    /**
     * 查询前台公开评论列表（不含 email 等 PII）
     */
    @Override
    public List<BlogComment> selectFrontCommentList(BlogComment blogComment)
    {
        return blogCommentMapper.selectFrontCommentList(blogComment);
    }

    /**
     * 新增博客评论
     * 
     * @param blogComment 博客评论
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBlogComment(BlogComment blogComment)
    {
        int result = blogCommentMapper.insertBlogComment(blogComment);
        if (result > 0)
        {
            // 已发布（无需审核）的评论同步文章评论数
            if ("1".equals(blogComment.getStatus()))
            {
                adjustArticleCommentCount(blogComment.getArticleId(), 1);
            }
            afterCommit(() -> sendCommentNotification(blogComment));
        }
        return result;
    }

    /**
     * 发送评论通知（站内信）
     */
    private void sendCommentNotification(BlogComment blogComment)
    {
        try
        {
            // 获取文章信息
            BlogArticle article = blogArticleService.selectBlogArticleById(blogComment.getArticleId());
            if (article == null)
            {
                logger.debug("评论通知跳过：文章不存在, articleId={}", blogComment.getArticleId());
                return;
            }

            // 确定接收者和通知类型
            Long recipientId = null;
            String type = "comment";
            String title = "你的文章《" + article.getTitle() + "》收到了新评论";

            if (blogComment.getParentId() != null && blogComment.getParentId() > 0)
            {
                // 回复他人的评论
                BlogComment parentComment = blogCommentMapper.selectBlogCommentById(blogComment.getParentId());
                if (parentComment != null && parentComment.getUserId() != null)
                {
                    recipientId = parentComment.getUserId();
                    type = "reply";
                    title = "你的评论收到了新回复";
                }
            }

            // 如果没有父评论或父评论无作者，通知文章作者
            if (recipientId == null)
            {
                recipientId = article.getAuthorId();
            }

            // 不给自己发通知
            Long commenterId = blogComment.getUserId();
            if (recipientId == null || (commenterId != null && recipientId.equals(commenterId)))
            {
                logger.debug("评论通知跳过：无需通知 (self or no recipient), articleId={}", blogComment.getArticleId());
                return;
            }

            // 构建通知
            String contentPreview = blogComment.getContent();
            if (contentPreview != null && contentPreview.length() > 200)
            {
                contentPreview = contentPreview.substring(0, 200) + "...";
            }

            // 发送者昵称：优先用评论提交的昵称，为空则用"匿名用户"
            String senderName = blogComment.getNickname();
            if (senderName == null || senderName.isEmpty()) {
                senderName = "匿名用户";
            }

            BlogNotification notification = new BlogNotification();
            notification.setRecipientId(recipientId);
            notification.setSenderName(senderName);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(contentPreview);
            notification.setArticleId(blogComment.getArticleId());
            notification.setArticleTitle(article.getTitle());
            notification.setCommentId(blogComment.getId());
            notification.setIsRead(0);

            blogNotificationService.createNotification(notification);
            logger.info("评论通知已发送: recipientId={}, type={}, articleId={}", recipientId, type, blogComment.getArticleId());

            // 邮件通知（受 email_notify_enabled 开关控制）
            String mailBody = type.equals("reply")
                    ? senderName + " 回复了你的评论：\n" + contentPreview
                    : senderName + " 评论了你的文章《" + article.getTitle() + "》：\n" + contentPreview;
            sendNotificationEmail(recipientId, title, mailBody);
        }
        catch (Exception e)
        {
            logger.error("评论通知发送失败: articleId={}", blogComment.getArticleId(), e);
        }
    }

    /**
     * 修改博客评论（状态变化时同步文章评论数）
     * 
     * @param blogComment 博客评论
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBlogComment(BlogComment blogComment)
    {
        BlogComment existing = blogComment.getId() == null ? null
                : blogCommentMapper.selectBlogCommentById(blogComment.getId());
        int result = blogCommentMapper.updateBlogComment(blogComment);
        if (result > 0 && existing != null)
        {
            String oldStatus = existing.getStatus();
            String newStatus = blogComment.getStatus();
            if (newStatus != null && !newStatus.equals(oldStatus))
            {
                if ("1".equals(newStatus) && !"1".equals(oldStatus))
                {
                    // 待审核/删除 → 已发布
                    adjustArticleCommentCount(existing.getArticleId(), 1);
                }
                else if ("1".equals(oldStatus) && !"1".equals(newStatus))
                {
                    // 已发布 → 待审核/删除
                    adjustArticleCommentCount(existing.getArticleId(), -1);
                }
            }
        }
        return result;
    }

    /**
     * 批量删除博客评论（物理删除，发布中的评论同步文章评论数）
     * 
     * @param ids 需要删除的博客评论主键集合
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBlogCommentByIds(Long[] ids)
    {
        if (ids == null)
        {
            return 0;
        }
        for (Long id : ids)
        {
            decreaseIfPublished(id);
        }
        return blogCommentMapper.deleteBlogCommentByIds(ids);
    }

    /**
     * 删除博客评论信息
     * 
     * @param id 博客评论主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBlogCommentById(Long id)
    {
        decreaseIfPublished(id);
        return blogCommentMapper.deleteBlogCommentById(id);
    }

    /**
     * 审核通过博客评论
     *
     * @param ids 需要审核通过的博客评论主键集合
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditBlogCommentByIds(Long[] ids)
    {
        int count = 0;
        for (Long id : ids) {
            // 仅当此前不是已发布状态时，通过后增加文章评论数（避免重复计数）
            BlogComment existing = blogCommentMapper.selectBlogCommentById(id);
            if (existing != null && !"1".equals(existing.getStatus()))
            {
                adjustArticleCommentCount(existing.getArticleId(), 1);
            }

            BlogComment blogComment = new BlogComment();
            blogComment.setId(id);
            blogComment.setStatus("1"); // 1表示已审核通过
            count += blogCommentMapper.updateBlogComment(blogComment);

            // 发送审核通过通知给评论者
            afterCommit(() -> sendAuditResultNotification(id, true));
        }
        return count;
    }

    /**
     * 审核拒绝博客评论
     *
     * @param ids 需要审核拒绝的博客评论主键集合
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rejectBlogCommentByIds(Long[] ids)
    {
        int count = 0;
        for (Long id : ids) {
            // 拒绝（置 2）：此前处于发布状态的评论从文章评论数中扣除
            BlogComment existing = blogCommentMapper.selectBlogCommentById(id);
            if (existing != null && "1".equals(existing.getStatus()))
            {
                adjustArticleCommentCount(existing.getArticleId(), -1);
            }

            BlogComment blogComment = new BlogComment();
            blogComment.setId(id);
            blogComment.setStatus("2"); // 2表示已删除（拒绝）
            count += blogCommentMapper.updateBlogComment(blogComment);

            // 发送审核拒绝通知给评论者
            afterCommit(() -> sendAuditResultNotification(id, false));
        }
        return count;
    }

    /**
     * 发送评论审核结果通知（站内信）
     *
     * @param commentId 评论ID
     * @param approved  true=通过, false=拒绝
     */
    private void sendAuditResultNotification(Long commentId, boolean approved)
    {
        try
        {
            BlogComment blogComment = blogCommentMapper.selectBlogCommentById(commentId);
            if (blogComment == null || blogComment.getUserId() == null)
            {
                logger.debug("审核结果通知跳过：评论不存在或匿名评论, commentId={}", commentId);
                return;
            }

            BlogArticle article = blogArticleService.selectBlogArticleById(blogComment.getArticleId());
            String articleTitle = article != null ? article.getTitle() : "已删除的文章";

            String type = approved ? "audit" : "reject";
            String title = approved ? "你的评论已通过审核" : "你的评论未通过审核";
            String contentPreview = blogComment.getContent();
            if (contentPreview != null && contentPreview.length() > 200)
            {
                contentPreview = contentPreview.substring(0, 200) + "...";
            }
            String suffix = approved ? "【审核通过】" : "【审核未通过】";

            BlogNotification notification = new BlogNotification();
            notification.setRecipientId(blogComment.getUserId());
            notification.setSenderName("系统通知");
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(suffix + contentPreview);
            notification.setArticleId(blogComment.getArticleId());
            notification.setArticleTitle(articleTitle);
            notification.setCommentId(blogComment.getId());
            notification.setIsRead(0);

            blogNotificationService.createNotification(notification);
            logger.info("审核结果通知已发送: recipientId={}, approved={}, commentId={}",
                    blogComment.getUserId(), approved, commentId);

            // 邮件通知审核结果（受 email_notify_enabled 开关控制）
            sendNotificationEmail(blogComment.getUserId(), title,
                    "你在《" + articleTitle + "》下的评论" + contentPreview);
        }
        catch (Exception e)
        {
            logger.error("审核结果通知发送失败: commentId={}", commentId, e);
        }
    }

    /**
     * 删除前同步：若该评论当前处于发布状态，则减少文章评论数
     */
    private void decreaseIfPublished(Long commentId)
    {
        if (commentId == null)
        {
            return;
        }
        try
        {
            BlogComment existing = blogCommentMapper.selectBlogCommentById(commentId);
            if (existing != null && "1".equals(existing.getStatus()))
            {
                adjustArticleCommentCount(existing.getArticleId(), -1);
            }
        }
        catch (Exception e)
        {
            logger.warn("删除评论前查询失败: commentId={}, error={}", commentId, e.getMessage());
        }
    }

    /**
     * 事务提交后再执行（发站内信/邮件这类慢操作不应占用事务连接与行锁）。
     *
     * <p>没有活动事务时（例如单元测试或非事务调用）立即执行，保证行为不变。</p>
     */
    private void afterCommit(Runnable action)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    action.run();
                }
            });
        }
        else
        {
            action.run();
        }
    }

    /**
     * 增减文章评论数（zhi_article.comment_count）
     */
    private void adjustArticleCommentCount(Long articleId, int delta)
    {
        if (articleId == null)
        {
            return;
        }
        // 不再吞异常：计数同步失败必须让外层事务回滚，否则会出现"评论已发布、comment_count 永久偏移"
        // 的历史脏数据（zhi_article.comment_count 是列表页展示列，与真实 count 会长期不一致）
        blogArticleMapper.changeCommentCount(articleId, delta);
    }

    /**
     * 发送站内通知邮件给注册用户（受 email_notify_enabled 开关控制，默认开启）
     *
     * @param recipientId 接收用户ID
     * @param subject 邮件主题
     * @param body 邮件正文
     */
    private void sendNotificationEmail(Long recipientId, String subject, String body)
    {
        try
        {
            String enabled = blogSettingService == null ? null
                    : blogSettingService.selectSettingValueByKey(KEY_EMAIL_NOTIFY_ENABLED);
            // 未配置视为开启，显式关闭（false/'false'/'0'）则跳过（与全站开关判定同口径）
            if (BlogSwitchUtils.isOff(enabled))
            {
                return;
            }
            SysUser user = sysUserService == null ? null : sysUserService.selectUserById(recipientId);
            if (user == null || StringUtils.isEmpty(user.getEmail()))
            {
                return;
            }
            if (blogEmailService != null)
            {
                blogEmailService.sendNotificationMail(user.getEmail(), subject, body);
            }
        }
        catch (Exception e)
        {
            logger.warn("邮件通知发送失败: recipientId={}, error={}", recipientId, e.getMessage());
        }
    }

    /**
     * 统计博客评论数量
     *
     * @param blogComment 查询条件
     * @return 评论数量
     */
    @Override
    public Long selectBlogCommentCount(BlogComment blogComment)
    {
        return blogCommentMapper.selectBlogCommentCount(blogComment);
    }
}