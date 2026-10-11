package com.zhi.web.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.zhi.system.service.IBlogArticleService;

/**
 * 文章定时发布任务
 *
 * <p>每分钟扫描一次，将已到发布时间的定时文章（status=2）自动置为已发布（status=1）。</p>
 */
@Component("scheduledArticlePublishTask")
public class ScheduledArticlePublishTask
{
    private static final Logger log = LoggerFactory.getLogger(ScheduledArticlePublishTask.class);

    @Autowired
    private IBlogArticleService blogArticleService;

    /**
     * 每分钟执行一次定时发布检查
     */
    @Scheduled(cron = "0 * * * * ?")
    public void publishDueArticles()
    {
        try
        {
            int published = blogArticleService.publishScheduledArticles();
            if (published > 0)
            {
                log.info("定时发布完成：共发布 {} 篇文章", published);
            }
        }
        catch (Exception e)
        {
            log.error("定时发布任务执行失败", e);
        }
    }
}
