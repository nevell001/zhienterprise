package com.zhi.web.controller.blog;

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
import com.zhi.common.utils.BlogSwitchUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.ip.IpUtils;
import com.zhi.system.domain.BlogMessage;
import com.zhi.system.service.IBlogMessageService;
import com.zhi.system.service.IBlogSettingService;
import com.zhi.system.service.ICaptchaService;

/**
 * 留言板前台接口
 * 
 * @author nevell
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/api/message")
public class BlogFrontMessageController extends BaseController
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
    private IBlogMessageService blogMessageService;

    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired
    private ICaptchaService captchaService;

    @Autowired
    private UnifiedCacheManager unifiedCacheManager;

    /**
     * 查询留言列表（前台用，仅已发布，支持分页）
     */
    @Anonymous
    @GetMapping("/list")
    public TableDataInfo list(BlogMessage blogMessage)
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
        List<BlogMessage> list = blogMessageService.selectPublishedMessageList();
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
        return success(blogMessageService.selectPublishedMessageCount());
    }

    /**
     * 提交留言（匿名可提交，提交后按评论审核开关决定是否需要审核）
     */
    @Anonymous
    @RateLimiter(key = "zhi:message:", time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping
    public AjaxResult add(@RequestBody BlogMessage blogMessage, HttpServletRequest request)
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
                if (blogMessage.getUserId() == null)
                {
                    blogMessage.setUserId(loginUser.getUserId());
                }
                if (StringUtils.isEmpty(blogMessage.getNickname()))
                {
                    blogMessage.setNickname(loginUser.getUser().getNickName());
                }
            }
        }
        catch (Exception ignored)
        {
            // 匿名用户，无需填充
        }

        String nickname = blogMessage.getNickname();
        String content = blogMessage.getContent();
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
        if (blogMessage.getEmail() != null && blogMessage.getEmail().length() > MAX_EMAIL_LENGTH)
        {
            return error("邮箱长度不能超过" + MAX_EMAIL_LENGTH + "个字符");
        }
        if (blogMessage.getWebsite() != null && blogMessage.getWebsite().length() > MAX_WEBSITE_LENGTH)
        {
            return error("网站地址长度不能超过" + MAX_WEBSITE_LENGTH + "个字符");
        }

        // 提交冷却：同一访客（登录用户按 userId，匿名按 IP）在间隔内只允许一条留言
        String cooldownKey = COOLDOWN_KEY_PREFIX + visitorKey(blogMessage, request);
        long cooldownRemain = remainingCooldownSeconds(cooldownKey);
        if (cooldownRemain > 0)
        {
            return error("留言太频繁啦，请 " + cooldownRemain + " 秒后再试");
        }

        // 图形验证码 + 填写时限：仅在验证码启用时生效（与登录/注册同一开关）
        long captchaAgeSeconds = captchaService.validateAndGetAgeSeconds(
                blogMessage.getCode(), blogMessage.getUuid());
        if (captchaAgeSeconds >= 0 && captchaAgeSeconds < MIN_FILL_SECONDS)
        {
            return error("提交过快，请认真填写后再提交");
        }

        // 复用评论审核开关：未配置或开启时进入待审核（与全站开关判定同口径）
        String reviewSetting = blogSettingService.selectSettingValueByKey("comment_review");
        boolean needsReview = BlogSwitchUtils.isOn(reviewSetting);
        blogMessage.setStatus(needsReview ? "0" : "1");

        blogMessage.setId(null);
        blogMessage.setDelFlag("0");
        blogMessage.setIp(IpUtils.getIpAddr(request));
        blogMessage.setReplyContent(null);
        blogMessage.setReplyTime(null);
        blogMessage.setReplyBy(null);
        String userAgent = request.getHeader("User-Agent");
        blogMessage.setUserAgent(truncate(userAgent, MAX_USER_AGENT_LENGTH));
        blogMessage.setContent(content.trim());
        blogMessage.setNickname(nickname.trim());

        logger.info("新留言提交: nickname={}, status={} (审核开关={})",
                blogMessage.getNickname(), blogMessage.getStatus(), reviewSetting);

        int rows = blogMessageService.insertBlogMessage(blogMessage);
        if (rows > 0)
        {
            unifiedCacheManager.set(cooldownKey, System.currentTimeMillis(),
                    SUBMIT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
        return toAjax(rows);
    }

    /**
     * 留言板是否已关闭（未配置视为开启，与全站开关同一口径）
     */
    private boolean isGuestbookClosed()
    {
        return BlogSwitchUtils.isOff(blogSettingService.selectSettingValueByKey(GUESTBOOK_ENABLED_KEY));
    }

    /**
     * 留言访客标识：登录用户用 userId，匿名用户用 IP
     */
    private String visitorKey(BlogMessage blogMessage, HttpServletRequest request)
    {
        if (blogMessage.getUserId() != null)
        {
            return "u" + blogMessage.getUserId();
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
