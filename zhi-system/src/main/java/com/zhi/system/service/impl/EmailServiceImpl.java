package com.zhi.system.service.impl;

import com.zhi.common.config.EmailCodeConfig;
import com.zhi.common.utils.ModernDateUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.system.domain.EmailCode;
import com.zhi.system.mapper.EmailCodeMapper;
import com.zhi.system.service.IEmailService;
import com.zhi.system.service.MailConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 博客邮件服务实现
 *
 * @author nevell
 * @date 2025-01-26
 */
@Service
public class EmailServiceImpl implements IEmailService
{
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    /** Redis频率限制键前缀 */
    private static final String RATE_LIMIT_EMAIL_PREFIX = "email:rate:limit:";

    private static final String RATE_LIMIT_IP_PREFIX = "ip:rate:limit:";

    /** 验证码校验失败次数Redis键前缀 */
    private static final String VERIFY_FAIL_PREFIX = "email:verify:fail:";

    @Autowired
    private EmailCodeMapper emailCodeMapper;

    @Autowired
    private EmailCodeConfig emailCodeConfig;

    /** 当前激活的 profile：生产环境必须关掉"打印验证码"的开发捷径 */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile = "dev";

    @Autowired
    private MailConfigService mailConfigService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 发送注册验证码
     *
     * @param email 邮箱地址
     * @param ipAddress 请求IP地址
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendRegisterCode(String email, String ipAddress)
    {
        return sendCode(email, CODE_TYPE_REGISTER, ipAddress, "注册账号");
    }

    /**
     * 发送密码重置验证码
     *
     * @param email 邮箱地址
     * @param ipAddress 请求IP地址
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendResetCode(String email, String ipAddress)
    {
        return sendCode(email, CODE_TYPE_RESET, ipAddress, "重置密码");
    }

    /**
     * 发送站内通知邮件（评论/回复/审核结果等）
     *
     * @param email 收件邮箱
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本）
     * @return true=发送成功
     */
    @Override
    public boolean sendNotificationMail(String email, String subject, String content)
    {
        if (StringUtils.isEmpty(email))
        {
            return false;
        }
        if (!mailConfigService.isReady())
        {
            log.debug("通知邮件未发送：未配置或未启用邮件服务 email={}, subject={}", email, subject);
            return false;
        }
        try
        {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailConfigService.getFrom());
            message.setTo(email);
            message.setSubject(subject);
            message.setText(content);
            mailConfigService.getSender().send(message);
            log.info("通知邮件发送成功：email={}, subject={}", email, subject);
            return true;
        }
        catch (Exception e)
        {
            log.error("通知邮件发送失败：email={}, subject={}, error={}", email, subject, e.getMessage());
            return false;
        }
    }

    /**
     * 发送验证码通用方法
     *
     * @param email 邮箱地址
     * @param codeType 验证码类型
     * @param ipAddress 请求IP地址
     * @param subject 邮件主题
     * @return 结果
     */
    private boolean sendCode(String email, String codeType, String ipAddress, String subject)
    {
        // 参数校验
        if (StringUtils.isEmpty(email))
        {
            log.error("发送验证码失败：邮箱地址为空");
            return false;
        }

        // 检查频率限制
        if (!checkEmailRateLimit(email, codeType))
        {
            log.warn("发送验证码失败：邮箱频率限制 email={}", email);
            return false;
        }

        if (!checkIpRateLimit(ipAddress))
        {
            log.warn("发送验证码失败：IP频率限制 ip={}", ipAddress);
            return false;
        }

        // 生成验证码
        String code = generateCode();

        // 计算过期时间
        Date expireTime = calculateExpireTime();

        // 创建验证码记录
        EmailCode emailCode = new EmailCode();
        emailCode.setEmail(email);
        emailCode.setCode(code);
        emailCode.setCodeType(codeType);
        emailCode.setExpireTime(expireTime);
        emailCode.setUsed(0);
        emailCode.setIpAddress(ipAddress);
        emailCode.setCreateTime(ModernDateUtils.now());

        try
        {
            // 保存到数据库
            emailCodeMapper.insertEmailCode(emailCode);

            // 设置Redis频率限制
            setRateLimit(email, ipAddress, codeType);

            // 开发环境：打印验证码到控制台（生产 profile 会强制关闭）
            if (isDevPrintCodeEnabled())
            {
                log.warn("========== 邮件验证码（开发环境）==========");
                log.warn("邮箱：{}", email);
                log.warn("验证码：{}", code);
                log.warn("类型：{}", codeType);
                log.warn("有效期：{} 分钟", emailCodeConfig.getExpireMinutes());
                log.warn("========================================");
                return true;
            }

            // 生产环境：发送邮件
            if (mailConfigService.isReady())
            {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailConfigService.getFrom());
                message.setTo(email);
                message.setSubject(subject + " - 验证码");
                message.setText("您的验证码是：" + code + "\n" +
                        "有效期：" + emailCodeConfig.getExpireMinutes() + " 分钟\n" +
                        "请勿将验证码告诉他人。");

                mailConfigService.getSender().send(message);
                log.info("验证码邮件发送成功：email={}, type={}", email, codeType);
                return true;
            }
            else
            {
                log.error("邮件发送失败：未配置邮件服务");
                return false;
            }
        }
        catch (Exception e)
        {
            log.error("发送验证码失败：email={}, type={}, error={}", email, codeType, e.getMessage());
            return false;
        }
    }

    /**
     * 验证验证码是否有效
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @param codeType 验证码类型
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyCode(String email, String code, String codeType)
    {
        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(code))
        {
            return false;
        }

        String failKey = VERIFY_FAIL_PREFIX + email + ":" + codeType;

        // 锁定状态（软锁定）：达到失败阈值后**仍允许正确验证码通过**，只阻断继续爆破。
        // 原实现把锁检查放在比对之前直接 return false，导致攻击者只要知道受害者邮箱，
        // 连发 5 次错误码即可让受害者的**正确验证码也被拒**（自助流程 DoS）。
        Object failCountObj = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountObj == null ? 0 : ((Number) failCountObj).intValue();
        boolean locked = failCount >= emailCodeConfig.getMaxVerifyAttempts();
        if (locked)
        {
            log.warn("验证码处于锁定状态，仅接受正确验证码：email={}, type={}, failCount={}",
                    email, codeType, failCount);
        }

        // 查询最新的未使用验证码
        EmailCode emailCode = emailCodeMapper.selectLatestUnusedCode(email, codeType);

        if (emailCode == null)
        {
            log.warn("验证码验证失败：未找到有效验证码 email={}, type={}", email, codeType);
            recordVerifyFail(failKey);
            return false;
        }

        // 检查是否过期
        if (ModernDateUtils.now().after(emailCode.getExpireTime()))
        {
            log.warn("验证码验证失败：验证码已过期 email={}, type={}", email, codeType);
            return false;
        }

        // 验证码匹配
        if (!code.equals(emailCode.getCode()))
        {
            log.warn("验证码验证失败：验证码不匹配 email={}, type={}", email, codeType);
            if (locked)
            {
                // 锁定期内不再累加计数（避免过期时间被无限续期），也无需重复告警
                return false;
            }
            recordVerifyFail(failKey);
            return false;
        }

        // 标记为已使用
        emailCodeMapper.markCodeAsUsed(emailCode.getId(), ModernDateUtils.now());
        // 清除失败记录
        redisTemplate.delete(failKey);

        log.info("验证码验证成功：email={}, type={}", email, codeType);
        return true;
    }

    /**
     * 记录验证码校验失败次数，达到阈值后按锁定时长锁定
     *
     * @param failKey 失败计数键
     */
    private void recordVerifyFail(String failKey)
    {
        Long count = redisTemplate.opsForValue().increment(failKey);
        boolean reached = count != null && count >= emailCodeConfig.getMaxVerifyAttempts();
        redisTemplate.expire(failKey, reached ? emailCodeConfig.getVerifyLockMinutes() : emailCodeConfig.getExpireMinutes(), TimeUnit.MINUTES);
        if (reached)
        {
            log.warn("验证码连续失败达到阈值，已锁定 {} 分钟：key={}", emailCodeConfig.getVerifyLockMinutes(), failKey);
        }
    }

    /**
     * 获取验证码
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @param codeType 验证码类型
     * @return 验证码对象
     */
    @Override
    public Object getCode(String email, String code, String codeType)
    {
        return emailCodeMapper.selectLatestUnusedCode(email, codeType);
    }

    /**
     * 删除过期的验证码
     *
     * @return 结果
     */
    @Override
    public int cleanExpiredCodes()
    {
        return emailCodeMapper.deleteExpiredCodes();
    }

    /**
     * 检查邮箱频率限制
     *
     * @param email 邮箱地址
     * @param codeType 验证码类型
     * @return true=允许发送, false=频率限制
     */
    @Override
    public boolean checkEmailRateLimit(String email, String codeType)
    {
        String redisKey = RATE_LIMIT_EMAIL_PREFIX + email + ":" + codeType;

        // 检查Redis中是否存在限制
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(hasKey))
        {
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            log.warn("邮箱频率限制：email={}, remainingSeconds={}", email, ttl);
            return false;
        }

        // 检查数据库中的发送记录
        Date startTime = new Date(System.currentTimeMillis() - emailCodeConfig.getRateLimitSeconds() * 1000L);
        int count = emailCodeMapper.countCodesByEmailAndTime(email, startTime);

        if (count > 0)
        {
            // 设置Redis限制
            redisTemplate.opsForValue().set(redisKey, "1", emailCodeConfig.getRateLimitSeconds(), TimeUnit.SECONDS);
            log.warn("邮箱频率限制：email={}, count={}", email, count);
            return false;
        }

        return true;
    }

    /**
     * 是否走"开发环境打印验证码"捷径。
     *
     * <p>生产 profile 下一律返回 false：否则验证码只会写进日志而不真正发邮件，
     * 并且 {@link #checkIpRateLimit} 会连带跳过 IP 频率限制。</p>
     */
    @Override
    public boolean isDevPrintCodeEnabled()
    {
        if (activeProfile != null && activeProfile.contains("prod"))
        {
            return false;
        }
        return emailCodeConfig.isDevPrintCode();
    }

    /**
     * 检查IP频率限制
     *
     * @param ipAddress IP地址
     * @return true=允许发送, false=频率限制
     */
    @Override
    public boolean checkIpRateLimit(String ipAddress)
    {
        // 开发模式下跳过IP频率限制（因为Docker环境中所有请求来自同一IP）
        if (isDevPrintCodeEnabled())
        {
            log.debug("开发模式：跳过IP频率限制 ip={}", ipAddress);
            return true;
        }

        if (StringUtils.isEmpty(ipAddress))
        {
            return true;
        }

        String redisKey = RATE_LIMIT_IP_PREFIX + ipAddress;

        // 检查Redis中是否存在限制
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(hasKey))
        {
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            log.warn("IP频率限制：ip={}, remainingSeconds={}", ipAddress, ttl);
            return false;
        }

        // 检查数据库中的发送记录（1小时内）
        Date startTime = ModernDateUtils.toDate(ModernDateUtils.getNowDateTime().minusHours(1));

        int count = emailCodeMapper.countCodesByIpAndTime(ipAddress, startTime);

        if (count >= emailCodeConfig.getIpRateLimit())
        {
            // 设置Redis限制（1小时）
            redisTemplate.opsForValue().set(redisKey, "1", 1, TimeUnit.HOURS);
            log.warn("IP频率限制：ip={}, count={}", ipAddress, count);
            return false;
        }

        return true;
    }

    /**
     * 生成随机验证码
     *
     * @return 验证码
     */
    private String generateCode()
    {
        int length = emailCodeConfig.getCodeLength();
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < length; i++)
        {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }

    /**
     * 计算过期时间
     *
     * @return 过期时间
     */
    private Date calculateExpireTime()
    {
        return ModernDateUtils.toDate(ModernDateUtils.getNowDateTime().plusMinutes(emailCodeConfig.getExpireMinutes()));
    }

    /**
     * 设置Redis频率限制
     *
     * @param email 邮箱地址
     * @param ipAddress IP地址
     * @param codeType 验证码类型
     */
    private void setRateLimit(String email, String ipAddress, String codeType)
    {
        // 设置邮箱频率限制
        String emailKey = RATE_LIMIT_EMAIL_PREFIX + email + ":" + codeType;
        redisTemplate.opsForValue().set(emailKey, "1", emailCodeConfig.getRateLimitSeconds(), TimeUnit.SECONDS);
    }
}
