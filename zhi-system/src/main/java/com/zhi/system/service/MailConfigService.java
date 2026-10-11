package com.zhi.system.service;

import com.zhi.common.utils.BlogSwitchUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.system.domain.MailConfigForm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件服务配置：站点设置里配置的 SMTP 覆盖环境变量，保存后热更新到活的 {@link JavaMailSenderImpl}，无需重启。
 *
 * <p>配置只落在 {@code blog_setting} 表（{@code mail_*} 键），刻意<b>不</b>进入匿名前台设置白名单，
 * 也<b>不</b>镜像到 {@code sys_config}，因此 SMTP 密码不会被公开设置接口返回。</p>
 *
 * <p>生效值口径：某键在库里为空则回退到启动时 {@code spring.mail.*} 的基线值，从而"未配置时跟随环境变量，
 * 一旦在后台填写则以数据库为准"。</p>
 *
 * @author nevell
 */
@Service
public class MailConfigService
{
    private static final Logger log = LoggerFactory.getLogger(MailConfigService.class);

    public static final String KEY_HOST = "mail_host";
    public static final String KEY_PORT = "mail_port";
    public static final String KEY_USERNAME = "mail_username";
    public static final String KEY_PASSWORD = "mail_password";
    public static final String KEY_SSL = "mail_ssl";
    public static final String KEY_STARTTLS = "mail_starttls";
    public static final String KEY_ENABLED = "mail_enabled";

    /** 读取（含匿名/管理列表）时用于替换密码明文的占位符 */
    public static final String PASSWORD_MASK = "********";

    /** 走 service 而非 mapper：mail_* 键没有 SQL 种子，写入必须是 upsert，纯 UPDATE 会静默丢失 */
    @Autowired
    private IBlogSettingService blogSettingService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /** 启动时从 spring.mail.*（环境变量）解析出的基线值，作为库中未配置时的回退 */
    private String baselineHost = "";
    private int baselinePort = 587;
    private String baselineUsername = "";
    private String baselinePassword = "";
    private boolean baselineSsl = false;
    private boolean baselineStarttls = true;

    /** 当前生效的发件人地址（= username），供发送验证码/通知邮件使用 */
    private volatile String from = "";

    /** 邮件服务是否可用（已启用且 host + username 齐全） */
    private volatile boolean ready = false;

    @PostConstruct
    public void init()
    {
        captureBaseline();
        try
        {
            reconfigure();
        }
        catch (Exception e)
        {
            log.warn("邮件配置初始化失败，将在保存时重试：{}", e.getMessage());
        }
    }

    /** 从自动装配的 JavaMailSenderImpl 读取 spring.mail.* 基线，避免热更新把已有 SSL/端口配置抹掉 */
    private void captureBaseline()
    {
        if (!(mailSender instanceof JavaMailSenderImpl impl))
        {
            return;
        }
        if (StringUtils.isNotEmpty(impl.getHost()))
        {
            baselineHost = impl.getHost();
        }
        if (impl.getPort() > 0)
        {
            baselinePort = impl.getPort();
        }
        if (impl.getUsername() != null)
        {
            baselineUsername = impl.getUsername();
        }
        if (impl.getPassword() != null)
        {
            baselinePassword = impl.getPassword();
        }
        Properties props = impl.getJavaMailProperties();
        baselineSsl = parseBool(props.getProperty("mail.smtp.ssl.enable"), false);
        baselineStarttls = parseBool(props.getProperty("mail.smtp.starttls.enable"), true);
    }

    /**
     * 依据 blog_setting 与基线值计算生效配置，并热更新到活的 sender。
     * 保存后调用即可即时生效，无需重启。
     */
    public synchronized void reconfigure()
    {
        String host = effective(KEY_HOST, baselineHost);
        int port = effectivePort();
        String username = effective(KEY_USERNAME, baselineUsername);
        String password = effective(KEY_PASSWORD, baselinePassword);
        boolean ssl = effectiveSwitch(KEY_SSL, baselineSsl);
        boolean starttls = starttlsOf(ssl, effectiveSwitch(KEY_STARTTLS, baselineStarttls));
        boolean enabled = BlogSwitchUtils.isOn(read(KEY_ENABLED));

        from = username;
        ready = enabled && StringUtils.isNotEmpty(host) && StringUtils.isNotEmpty(username);

        if (mailSender instanceof JavaMailSenderImpl impl)
        {
            impl.setHost(host);
            impl.setPort(port);
            impl.setUsername(username);
            impl.setPassword(password);
            Properties props = impl.getJavaMailProperties();
            props.setProperty("mail.smtp.auth", "true");
            props.setProperty("mail.smtp.ssl.enable", String.valueOf(ssl));
            props.setProperty("mail.smtp.starttls.enable", String.valueOf(starttls));
            props.setProperty("mail.smtp.connectiontimeout", "10000");
            props.setProperty("mail.smtp.timeout", "10000");
            props.setProperty("mail.smtp.writetimeout", "10000");
        }

        log.info("邮件配置已应用：host={}, port={}, from={}, enabled={}, ssl={}, starttls={}",
                host, port, maskEmail(username), enabled, ssl, starttls);
    }

    /**
     * 保存后台提交的邮件配置（仅写 blog_setting，不落 sys_config）。
     *
     * @param form 表单值；{@code password} 为空表示保留原密码
     * @return 脱敏后的当前配置
     */
    public Map<String, Object> save(MailConfigForm form)
    {
        upsert(KEY_HOST, form.getHost() == null ? "" : form.getHost().trim());
        upsert(KEY_PORT, form.getPort() == null ? "" : String.valueOf(form.getPort()).trim());
        upsert(KEY_USERNAME, form.getUsername() == null ? "" : form.getUsername().trim());
        if (StringUtils.isNotEmpty(form.getPassword()))
        {
            // 仅在提交了新密码时更新，空值保留原密码
            upsert(KEY_PASSWORD, form.getPassword());
        }
        Boolean ssl = form.getSsl();
        Boolean starttls = form.getStarttls();
        if (Boolean.TRUE.equals(ssl))
        {
            // 同开时以 SSL 为准，并把这个结论落库，否则下次回填又是一对冲突值
            starttls = Boolean.FALSE;
        }
        if (ssl != null)
        {
            upsert(KEY_SSL, String.valueOf(ssl));
        }
        if (starttls != null)
        {
            upsert(KEY_STARTTLS, String.valueOf(starttls));
        }
        if (form.getEnabled() != null)
        {
            upsert(KEY_ENABLED, String.valueOf(form.getEnabled()));
        }
        reconfigure();
        return getMaskedView();
    }

    /**
     * 返回脱敏视图：host/port/username/开关为生效值，密码永不返回明文，只给出是否已配置。
     */
    public Map<String, Object> getMaskedView()
    {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("host", effective(KEY_HOST, baselineHost));
        view.put("port", effectivePort());
        view.put("username", effective(KEY_USERNAME, baselineUsername));
        view.put("hasPassword", StringUtils.isNotEmpty(effective(KEY_PASSWORD, baselinePassword)));
        boolean ssl = effectiveSwitch(KEY_SSL, baselineSsl);
        view.put("ssl", ssl);
        view.put("starttls", starttlsOf(ssl, effectiveSwitch(KEY_STARTTLS, baselineStarttls)));
        view.put("enabled", BlogSwitchUtils.isOn(read(KEY_ENABLED)));
        return view;
    }

    /**
     * 测试 SMTP 连接（不做真实发信）。表单字段非空时以表单值为准，留空则回退已保存的配置，
     * 因此可以在"尚未保存"时就测试当前正在填写的配置；密码留空表示沿用已保存的密码。
     *
     * @param form 前端当前表单，可为 null（等价于测试已保存配置）
     * @return 连接成功返回 {@code null}；失败返回可展示给管理员的原因（不含密码）
     */
    public String testConnection(MailConfigForm request)
    {
        // 先归一化再取值：实参会被提前求值，`form != null && ...` 的短路保护不住后面的 form.getX()
        MailConfigForm form = request == null ? new MailConfigForm() : request;
        String host = pick(StringUtils.isNotEmpty(form.getHost()), form.getHost(),
                effective(KEY_HOST, baselineHost));
        int port = form.getPort() != null ? form.getPort() : effectivePort();
        String username = pick(StringUtils.isNotEmpty(form.getUsername()), form.getUsername(),
                effective(KEY_USERNAME, baselineUsername));
        // 密码：表单填了就测填的，否则用已保存/环境变量里的
        String password = StringUtils.isNotEmpty(form.getPassword())
                ? form.getPassword() : effective(KEY_PASSWORD, baselinePassword);
        boolean ssl = form.getSsl() != null ? form.getSsl() : effectiveSwitch(KEY_SSL, baselineSsl);
        boolean starttls = starttlsOf(ssl, form.getStarttls() != null
                ? form.getStarttls() : effectiveSwitch(KEY_STARTTLS, baselineStarttls));

        if (StringUtils.isEmpty(host))
        {
            return "未配置 SMTP 主机";
        }

        // 独立临时实例，避免连接测试影响共享 sender 或产生副作用
        JavaMailSenderImpl probe = new JavaMailSenderImpl();
        probe.setHost(host);
        probe.setPort(port);
        probe.setUsername(username);
        probe.setPassword(password);
        Properties props = probe.getJavaMailProperties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.setProperty("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.setProperty("mail.smtp.connectiontimeout", "5000");
        props.setProperty("mail.smtp.timeout", "5000");
        try
        {
            probe.testConnection();
            return null;
        }
        catch (Exception e)
        {
            String reason = describe(e);
            log.warn("邮件连接测试失败：host={}, port={}, reason={}", host, port, reason);
            return reason;
        }
    }

    private static String pick(boolean useValue, String value, String fallback)
    {
        return useValue ? value : fallback;
    }

    /** 取异常链最底层、最有信息量的报错；连接类失败给出常见成因提示 */
    private static String describe(Throwable e)
    {
        Throwable root = e;
        String message = e.getMessage();
        while (root.getCause() != null)
        {
            root = root.getCause();
            if (StringUtils.isNotEmpty(root.getMessage()))
            {
                message = root.getMessage();
            }
        }
        if (StringUtils.isEmpty(message))
        {
            message = root.getClass().getSimpleName();
        }
        String lower = message.toLowerCase();
        if (lower.contains("authentication") || lower.contains("535") || lower.contains("login"))
        {
            return message + "（用户名或密码/授权码错误，QQ/163 等需使用授权码而非登录密码）";
        }
        if (lower.contains("timed out") || lower.contains("connection refused") || lower.contains("unknownhost")
            || lower.contains("connect failed"))
        {
            return message + "（连不上该主机/端口：检查主机、端口与网络/防火墙；"
                    + "明文去连 465 这类 SSL 端口同样会卡到超时，465 需开启 SSL）";
        }
        if (lower.contains("ssl") || lower.contains("handshake"))
        {
            return message + "（SSL/STARTTLS 与端口不匹配：465 用 SSL、587 用 STARTTLS）";
        }
        return message;
    }

    /** 发送前用于判断邮件服务是否可用 */
    public boolean isReady()
    {
        return ready;
    }

    /** 当前生效的发件人地址（= username），未配置时为空串 */
    public String getFrom()
    {
        return from;
    }

    public JavaMailSender getSender()
    {
        return mailSender;
    }

    // ---- helpers ----

    private String read(String key)
    {
        try
        {
            return blogSettingService.selectSettingValueByKey(key);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void upsert(String key, String value)
    {
        blogSettingService.updateSettingValueByKey(key, value);
    }

    private String effective(String key, String baseline)
    {
        String db = read(key);
        return StringUtils.isNotEmpty(db) ? db : baseline;
    }

    private int effectivePort()
    {
        String db = read(KEY_PORT);
        if (StringUtils.isNotEmpty(db))
        {
            try
            {
                return Integer.parseInt(db.trim());
            }
            catch (NumberFormatException ignored)
            {
                // 非法端口回退到基线
            }
        }
        return baselinePort;
    }

    /**
     * SSL(465) 与 STARTTLS(587) 语义互斥，同时开启会导致握手失败。
     * 环境变量基线可能两者皆开，因此统一以 SSL 为准收敛生效值，而不是把冲突抛给用户。
     */
    private static boolean starttlsOf(boolean ssl, boolean starttls)
    {
        return ssl ? false : starttls;
    }

    private boolean effectiveSwitch(String key, boolean baseline)
    {
        String db = read(key);
        // 库中从未写入该键时（null）跟随基线；一旦有值以数据库为准（"false"/"0" 为关）
        if (db == null)
        {
            return baseline;
        }
        return BlogSwitchUtils.isOn(db);
    }

    private static boolean parseBool(String value, boolean def)
    {
        if (StringUtils.isEmpty(value))
        {
            return def;
        }
        return BlogSwitchUtils.isOn(value);
    }

    private static String maskEmail(String email)
    {
        if (StringUtils.isEmpty(email))
        {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0)
        {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
