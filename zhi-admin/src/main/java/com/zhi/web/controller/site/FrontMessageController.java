package com.zhi.web.controller.site;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.annotation.RateLimiter;
import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.domain.model.LoginUser;
import com.zhi.common.core.page.TableDataInfo;
import com.zhi.common.enums.LimitType;
import com.zhi.common.utils.SiteSwitchUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.ip.IpUtils;
import com.zhi.system.domain.Message;
import com.zhi.system.service.IMessageService;
import com.zhi.system.service.ISettingService;
import com.zhi.system.service.ICaptchaService;

/**
 * 留言板前台接口
 * 
 * @author nevell
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/api/message")
public class FrontMessageController extends BaseController
{
    /** 昵称最大长度 */
    private static final int MAX_NICKNAME_LENGTH = 50;

    /** 留言内容最大长度 */
    private static final int MAX_CONTENT_LENGTH = 500;

    /** 网站地址最大长度 */
    private static final int MAX_WEBSITE_LENGTH = 255;

    /** 邮箱最大长度 */
    private static final int MAX_EMAIL_LENGTH = 100;

    /** UA 存储最大长度 */
    private static final int MAX_USER_AGENT_LENGTH = 255;

    /** 最短填写时间（秒）：验证码签发到提交的时间差小于该值视为机器提交 */
    private static final long MIN_FILL_SECONDS = 3;

    /** 同一访客两次留言的最小间隔（秒） */
    private static final long SUBMIT_INTERVAL_SECONDS = 60;

    /** 提交冷却缓存键前缀 */
    private static final String COOLDOWN_KEY_PREFIX = "zhi:message:cooldown:";

    /** 留言板功能开关键 */
    private static final String GUESTBOOK_ENABLED_KEY = "guestbook_enabled";

    /** 留言板关闭时的统一提示 */
    private static final String DISABLED_TIP = "本站暂未开放留言板";

    @Autowired
    private IMessageService messageService;

    @Autowired
    private ISettingService settingService;

    @Autowired
    private ICaptchaService captchaService;

    @Autowired
    private UnifiedCacheManager unifiedCacheManager;

    /**
     * 查询留言列表（前台用，仅已发布，支持分页）
     */
    @Anonymous
    @GetMapping("/list")
    public TableDataInfo list(Message message)
    {
        // 功能开关：关闭后连读取一起拒绝，避免只隐藏前端入口
        if (isGuestbookClosed())
        {
            TableDataInfo disabled = new TableDataInfo();
            disabled.setCode(500);
            disabled.setMsg(DISABLED_TIP);
            disabled.setRows(new ArrayList<>());
            disabled.setTotal(0);
            return disabled;
        }
        startPage();
        List<Message> list = messageService.selectPublishedMessageList();
        return getDataTable(list);
    }

    /**
     * 查询已发布留言总数
     */
    @Anonymous
    @GetMapping("/count")
    public AjaxResult count()
    {
        if (isGuestbookClosed())
        {
            return error(DISABLED_TIP);
        }
        return success(messageService.selectPublishedMessageCount());
    }

    /**
     * 提交留言（匿名可提交，提交后按评论审核开关决定是否需要审核）
     */
    @Anonymous
    @RateLimiter(key = "zhi:message:", time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping
    public AjaxResult add(@RequestBody Message message, HttpServletRequest request)
    {
        // 关闭时短路在最前，避免消耗一次性的验证码
        if (isGuestbookClosed())
        {
            return error(DISABLED_TIP);
        }

        // 先按登录账号补全 userId 与昵称，再校验：登录态的留言表单不采集昵称，
        // 校验放在补全之前会把登录用户的留言一律拒成"昵称不能为空"
        try
        {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof LoginUser loginUser)
            {
                if (message.getUserId() == null)
                {
                    message.setUserId(loginUser.getUserId());
                }
                if (StringUtils.isEmpty(message.getNickname()))
                {
                    message.setNickname(loginUser.getUser().getNickName());
                }
            }
        }
        catch (Exception ignored)
        {
            // 匿名用户，无需填充
        }

        String nickname = message.getNickname();
        String content = message.getContent();
        if (StringUtils.isEmpty(nickname) || nickname.trim().isEmpty())
        {
            return error("昵称不能为空");
        }
        if (nickname.trim().length() > MAX_NICKNAME_LENGTH)
        {
            return error("昵称长度不能超过" + MAX_NICKNAME_LENGTH + "个字符");
        }
        if (StringUtils.isEmpty(content) || content.trim().isEmpty())
        {
            return error("留言内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH)
        {
            return error("留言内容长度不能超过" + MAX_CONTENT_LENGTH + "个字符");
        }
        if (message.getEmail() != null && message.getEmail().length() > MAX_EMAIL_LENGTH)
        {
            return error("邮箱长度不能超过" + MAX_EMAIL_LENGTH + "个字符");
        }
        if (message.getWebsite() != null && message.getWebsite().length() > MAX_WEBSITE_LENGTH)
        {
            return error("网站地址长度不能超过" + MAX_WEBSITE_LENGTH + "个字符");
        }

        // 提交冷却：同一访客（登录用户按 userId，匿名按 IP）在间隔内只允许一条留言。
        // 语义要求（AGENTS.md）：冷却检查必须在验证码**之前**，否则拒绝时会烧掉一次性验证码。
        // 原子性：改用 SET NX EX 抢占，替代原先 exists()+set() 的 check-then-set ——
        // 原写法在"检查"与"设置"之间隔着验证码校验与一次 DB insert，并发请求都能通过检查。
        String cooldownKey = COOLDOWN_KEY_PREFIX + visitorKey(message, request);
        long cooldownRemain = remainingCooldownSeconds(cooldownKey);
        if (cooldownRemain > 0)
        {
            return error("留言太频繁啦，请 " + cooldownRemain + " 秒后再试");
        }
        boolean cooldownClaimed = unifiedCacheManager.setIfAbsent(cooldownKey,
                System.currentTimeMillis(), SUBMIT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        if (!cooldownClaimed)
        {
            // 与上面的存在性检查之间被并发抢占：按冷却中处理
            return error("留言太频繁啦，请 " + SUBMIT_INTERVAL_SECONDS + " 秒后再试");
        }

        // 图形验证码 + 填写时限：仅在验证码启用时生效（与登录/注册同一开关）
        long captchaAgeSeconds = captchaService.validateAndGetAgeSeconds(
                message.getCode(), message.getUuid());
        if (captchaAgeSeconds >= 0 && captchaAgeSeconds < MIN_FILL_SECONDS)
        {
            // 回滚冷却占位：本次并未真正提交，不能因填写过快而让访客白等 60 秒
            unifiedCacheManager.delete(cooldownKey);
            return error("提交过快，请认真填写后再提交");
        }

        // 复用评论审核开关：未配置或开启时进入待审核（与全站开关判定同口径）
        String reviewSetting = settingService.selectSettingValueByKey("comment_review");
        boolean needsReview = SiteSwitchUtils.isOn(reviewSetting);
        message.setStatus(needsReview ? "0" : "1");

        message.setId(null);
        message.setDelFlag("0");
        message.setIp(IpUtils.getIpAddr(request));
        message.setReplyContent(null);
        message.setReplyTime(null);
        message.setReplyBy(null);
        String userAgent = request.getHeader("User-Agent");
        message.setUserAgent(truncate(userAgent, MAX_USER_AGENT_LENGTH));
        message.setContent(content.trim());
        message.setNickname(nickname.trim());

        logger.info("新留言提交: nickname={}, status={} (审核开关={})",
                message.getNickname(), message.getStatus(), reviewSetting);

        int rows = messageService.insertMessage(message);
        if (rows <= 0)
        {
            // 落库失败：释放冷却占位，否则访客既没发出去、还要白等 60 秒
            unifiedCacheManager.delete(cooldownKey);
        }
        // 成功时冷却占位已在上方 setIfAbsent 时建立，无需再 set（重复 set 会重置 TTL）
        return toAjax(rows);
    }

    /**
     * 留言板是否已关闭（未配置视为开启，与全站开关同一口径）
     */
    private boolean isGuestbookClosed()
    {
        return SiteSwitchUtils.isOff(settingService.selectSettingValueByKey(GUESTBOOK_ENABLED_KEY));
    }

    /**
     * 留言访客标识：登录用户用 userId，匿名用户用 IP
     */
    private String visitorKey(Message message, HttpServletRequest request)
    {
        if (message.getUserId() != null)
        {
            return "u" + message.getUserId();
        }
        return "ip" + IpUtils.getIpAddr(request);
    }

    /**
     * 剩余冷却秒数（无冷却返回 0）
     */
    private long remainingCooldownSeconds(String cooldownKey)
    {
        if (!unifiedCacheManager.exists(cooldownKey))
        {
            return 0L;
        }
        long remain = unifiedCacheManager.getExpire(cooldownKey);
        // 键存在但拿不到 TTL 时保守按满额冷却处理
        return remain > 0 ? remain : SUBMIT_INTERVAL_SECONDS;
    }

    /**
     * 字符串截断
     */
    private String truncate(String value, int maxLength)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
