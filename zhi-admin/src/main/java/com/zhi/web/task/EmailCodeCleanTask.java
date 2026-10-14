package com.zhi.web.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.zhi.system.service.IEmailService;

/**
 * 邮箱验证码过期清理任务
 *
 * <p>每天凌晨清理数据库中已过期的邮箱验证码记录。</p>
 */
@Component("emailCodeCleanTask")
public class EmailCodeCleanTask
{
    private static final Logger log = LoggerFactory.getLogger(EmailCodeCleanTask.class);

    @Autowired
    private IEmailService emailService;

    /**
     * 每天凌晨 02:00 执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredCodes()
    {
        try
        {
            int count = emailService.cleanExpiredCodes();
            if (count > 0)
            {
                log.info("已清理过期邮箱验证码 {} 条", count);
            }
        }
        catch (Exception e)
        {
            log.error("清理过期邮箱验证码失败", e);
        }
    }
}
