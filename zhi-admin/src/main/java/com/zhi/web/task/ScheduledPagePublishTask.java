package com.zhi.web.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.zhi.system.service.IPageService;

/**
 * 页面定时发布任务
 *
 * <p>每分钟扫描一次，将已到发布时间的定时页面（status=2）自动置为已发布（status=1）。</p>
 */
@Component("scheduledPagePublishTask")
public class ScheduledPagePublishTask
{
    private static final Logger log = LoggerFactory.getLogger(ScheduledPagePublishTask.class);

    @Autowired
    private IPageService pageService;

    @Scheduled(cron = "0 * * * * ?")
    public void publishDuePages()
    {
        try
        {
            int published = pageService.publishScheduledPages();
            if (published > 0)
            {
                log.info("页面定时发布完成：共发布 {} 个页面", published);
            }
        }
        catch (Exception e)
        {
            log.error("页面定时发布任务执行失败", e);
        }
    }
}