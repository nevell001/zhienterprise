package com.zhi.system.service;

import com.zhi.system.domain.MailConfigForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 邮件配置服务单元测试
 * 覆盖三类真实回归：mail_* 无种子行时写入必须是 upsert；SSL 与 STARTTLS 冲突时的生效口径；密码不回显。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MailConfigServiceTest {

    @Mock
    private ISettingService settingService;

    private MailConfigService mailConfigService;

    private JavaMailSenderImpl sender;

    @BeforeEach
    void setUp() {
        mailConfigService = new MailConfigService();
        ReflectionTestUtils.setField(mailConfigService, "settingService", settingService);
        sender = new JavaMailSenderImpl();
        ReflectionTestUtils.setField(mailConfigService, "mailSender", sender);
    }

    private MailConfigForm form(Boolean ssl, Boolean starttls) {
        MailConfigForm form = new MailConfigForm();
        form.setHost("smtp.126.com");
        form.setPort(465);
        form.setUsername("someone@126.com");
        form.setPassword("auth-code");
        form.setSsl(ssl);
        form.setStarttls(starttls);
        form.setEnabled(true);
        return form;
    }

    @Test
    void savePersistsEveryFieldEvenWhenRowDoesNotExistYet() {
        when(settingService.selectSettingValueByKey(anyString())).thenReturn(null);
        when(settingService.updateSettingValueByKey(anyString(), anyString())).thenReturn(1);

        mailConfigService.save(form(true, false));

        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_HOST, "smtp.126.com");
        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_PORT, "465");
        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_USERNAME, "someone@126.com");
        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_PASSWORD, "auth-code");
        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_SSL, "true");
        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_STARTTLS, "false");
        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_ENABLED, "true");
    }

    @Test
    void saveKeepsStoredPasswordWhenFormSubmitsBlank() {
        MailConfigForm form = form(true, false);
        form.setPassword("");

        mailConfigService.save(form);

        verify(settingService, org.mockito.Mockito.never())
            .updateSettingValueByKey(eq(MailConfigService.KEY_PASSWORD), anyString());
    }

    @Test
    void saveResolvesConflictingSwitchesInFavorOfSsl() {
        mailConfigService.save(form(true, true));

        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_SSL, "true");
        verify(settingService).updateSettingValueByKey(MailConfigService.KEY_STARTTLS, "false");
    }

    @Test
    void appliedSenderNeverEnablesStarttlsTogetherWithSsl() {
        // 环境变量基线可能两者皆开（MAIL_SSL=true + 默认 MAIL_STARTTLS=true）
        sender.getJavaMailProperties().setProperty("mail.smtp.ssl.enable", "true");
        sender.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", "true");
        sender.setHost("smtp.126.com");
        sender.setPort(465);
        when(settingService.selectSettingValueByKey(anyString())).thenReturn(null);

        mailConfigService.init();

        assertTrue(Boolean.parseBoolean(sender.getJavaMailProperties().getProperty("mail.smtp.ssl.enable")));
        assertFalse(Boolean.parseBoolean(sender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable")));
    }

    @Test
    void maskedViewReportsResolvedStarttlsAndHidesPassword() {
        when(settingService.selectSettingValueByKey(MailConfigService.KEY_PASSWORD)).thenReturn("auth-code");
        when(settingService.selectSettingValueByKey(MailConfigService.KEY_SSL)).thenReturn("true");
        when(settingService.selectSettingValueByKey(MailConfigService.KEY_STARTTLS)).thenReturn("true");
        when(settingService.selectSettingValueByKey(MailConfigService.KEY_HOST)).thenReturn("smtp.126.com");
        when(settingService.selectSettingValueByKey(MailConfigService.KEY_USERNAME)).thenReturn("someone@126.com");

        Map<String, Object> view = mailConfigService.getMaskedView();

        assertEquals(true, view.get("ssl"));
        assertEquals(false, view.get("starttls"));
        assertEquals(true, view.get("hasPassword"));
        assertNull(view.get("password"));
        assertFalse(view.containsValue("auth-code"));
    }

    /** 让 zhi_setting 按键返回指定值；未列出的键返回 null，等价于「库里从未写入」。 */
    private void db(String... keyValuePairs) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            values.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        when(settingService.selectSettingValueByKey(anyString()))
            .thenAnswer(invocation -> values.get(invocation.getArgument(0)));
    }

    @Test
    void emptyDatabaseFallsBackToTheAutoConfiguredEnvironmentBaseline() {
        sender.setHost("smtp.env.internal");
        sender.setPort(465);
        sender.setUsername("env@126.com");
        sender.setPassword("env-secret");
        sender.getJavaMailProperties().setProperty("mail.smtp.ssl.enable", "true");
        sender.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", "false");
        db();

        mailConfigService.init();
        Map<String, Object> view = mailConfigService.getMaskedView();

        assertEquals("smtp.env.internal", view.get("host"));
        assertEquals(465, view.get("port"));
        assertEquals("env@126.com", view.get("username"));
        assertEquals(true, view.get("hasPassword"));
        assertEquals(true, view.get("ssl"));
        assertEquals(false, view.get("starttls"));
        assertTrue(mailConfigService.isReady(), "库中未配置时应跟随环境变量基线，而不是把配置抹成空");
        assertEquals("env@126.com", mailConfigService.getFrom());
    }

    @Test
    void missingSmtpPropertiesUseTheDocumentedDefaults() {
        sender.setHost("smtp.env.internal");
        sender.setPort(587);
        sender.setUsername("env@126.com");
        db();

        mailConfigService.init();
        Map<String, Object> view = mailConfigService.getMaskedView();

        assertEquals(false, view.get("ssl"), "未声明 ssl 时默认关闭");
        assertEquals(true, view.get("starttls"), "未声明 starttls 时默认开启");
    }

    @Test
    void bareSenderYieldsSafeDefaultsAndMarksMailNotReady() {
        // 未配置环境变量时 Spring 给的就是一个空 JavaMailSenderImpl：host/账号为空、端口未设
        sender.setPort(0);
        db();

        mailConfigService.init();
        Map<String, Object> view = mailConfigService.getMaskedView();

        assertEquals("", view.get("host"));
        assertEquals(587, view.get("port"), "端口非正数时保留内置默认端口");
        assertEquals("", view.get("username"));
        assertEquals(false, view.get("hasPassword"));
        assertFalse(mailConfigService.isReady(), "主机或发件人为空时不能判定邮件服务可用");
    }

    @Test
    void nonJavaMailSenderImplementationIsNotUsedForBaseline() {
        ReflectionTestUtils.setField(mailConfigService, "mailSender", org.mockito.Mockito.mock(JavaMailSender.class));
        db();

        mailConfigService.init();

        assertEquals("", mailConfigService.getMaskedView().get("host"));
        assertEquals(587, mailConfigService.getMaskedView().get("port"));
        assertFalse(mailConfigService.isReady());
    }

    @Test
    void databaseValuesOverrideTheEnvironmentBaseline() {
        sender.setHost("smtp.env.internal");
        sender.setPort(465);
        db(MailConfigService.KEY_HOST, "smtp.db.internal",
            MailConfigService.KEY_PORT, "2525",
            MailConfigService.KEY_USERNAME, "db@126.com",
            MailConfigService.KEY_PASSWORD, "db-secret",
            MailConfigService.KEY_SSL, "false",
            MailConfigService.KEY_STARTTLS, "true");

        mailConfigService.init();
        Map<String, Object> view = mailConfigService.getMaskedView();

        assertEquals("smtp.db.internal", view.get("host"));
        assertEquals(2525, view.get("port"));
        assertEquals("db@126.com", view.get("username"));
        assertEquals(false, view.get("ssl"));
        // ssl 显式关闭后，starttls 才有机会生效
        assertEquals(true, view.get("starttls"));
        assertEquals("smtp.db.internal", sender.getHost(), "生效值要热更新到活的 sender");
        assertEquals(2525, sender.getPort());
    }

    @Test
    void malformedPortFallsBackToBaselineInsteadOfFailing() {
        sender.setPort(465);
        db(MailConfigService.KEY_PORT, "not-a-number");

        mailConfigService.init();
        Map<String, Object> view = mailConfigService.getMaskedView();

        assertEquals(465, view.get("port"), "非法端口回退到基线，不能让读取设置直接抛错");
    }

    @Test
    void switchWrittenAsEmptyStringCountsAsOnPerRepositoryRule() {
        // 全库统一口径：只有 false/"false"/"0" 为关，因此空串是「开」而不是「跟随基线」
        sender.getJavaMailProperties().setProperty("mail.smtp.ssl.enable", "false");
        db(MailConfigService.KEY_SSL, "");

        assertEquals(true, mailConfigService.getMaskedView().get("ssl"));
    }

    @Test
    void settingReadFailureDegradesToBaseline() {
        sender.setHost("smtp.env.internal");
        sender.setPort(587);
        sender.setUsername("env@126.com");
        when(settingService.selectSettingValueByKey(anyString()))
            .thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(this::initAndReadView);
        Map<String, Object> view = mailConfigService.getMaskedView();

        assertEquals("smtp.env.internal", view.get("host"));
        assertTrue(mailConfigService.isReady(), "设置表暂时读不到时按环境变量基线判定，不该直接判定为不可用");
    }

    private Map<String, Object> initAndReadView() {
        mailConfigService.init();
        return mailConfigService.getMaskedView();
    }

    @Test
    void explicitFalseSwitchTurnsMailOff() {
        sender.setHost("smtp.env.internal");
        sender.setPort(587);
        sender.setUsername("env@126.com");
        db(MailConfigService.KEY_ENABLED, "false");

        mailConfigService.init();

        assertFalse(mailConfigService.isReady(), "mail_enabled=false 必须能真正关掉邮件服务");
        assertEquals(false, mailConfigService.getMaskedView().get("enabled"));
    }

    @Test
    void enabledSwitchDefaultsToOnWhenNeverWritten() {
        sender.setHost("smtp.env.internal");
        sender.setPort(587);
        sender.setUsername("env@126.com");
        db();

        mailConfigService.init();

        assertEquals(true, mailConfigService.getMaskedView().get("enabled"), "开关缺失即为开");
        assertTrue(mailConfigService.isReady());
    }

    @Test
    void readyRequiresBothHostAndUsername() {
        db(MailConfigService.KEY_HOST, "smtp.db.internal");
        mailConfigService.reconfigure();
        assertFalse(mailConfigService.isReady(), "有主机但没发件人，仍然不可用");

        db(MailConfigService.KEY_USERNAME, "db@126.com");
        mailConfigService.reconfigure();
        assertFalse(mailConfigService.isReady(), "有发件人但没主机，同样不可用");

        db(MailConfigService.KEY_HOST, "smtp.db.internal", MailConfigService.KEY_USERNAME, "db@126.com");
        mailConfigService.reconfigure();
        assertTrue(mailConfigService.isReady());
        assertEquals("db@126.com", mailConfigService.getFrom());
    }

    @Test
    void testConnectionWithoutAnyHostReportsMissingHost() {
        db();

        assertEquals("未配置 SMTP 主机", mailConfigService.testConnection(null));

        MailConfigForm blank = new MailConfigForm();
        assertEquals("未配置 SMTP 主机", mailConfigService.testConnection(blank));
    }

    @Test
    void testConnectionPrefersFormValuesOverSavedConfig() {
        // 库里配了一个根本不该被使用的地址；表单填 127.0.0.1:1，端口拒绝连接即说明用的是表单值
        db(MailConfigService.KEY_HOST, "smtp.must.not.be.used", MailConfigService.KEY_PASSWORD, "saved-secret");
        MailConfigForm form = new MailConfigForm();
        form.setHost("127.0.0.1");
        form.setPort(1);
        form.setUsername("probe@126.com");

        String reason = mailConfigService.testConnection(form);

        assertNotNull(reason, "连不上时必须返回可展示的原因，而不是 null（null 表示连接成功）");
        assertFalse(reason.contains("saved-secret"), "失败原因不得泄露已保存的密码");
        assertTrue(mailConfigService.getMaskedView().containsKey("host"), "测试连接不应改动已保存配置");
    }

    @Test
    void testConnectionFallsBackToSavedConfigWhenFormLeavesFieldsBlank() {
        db(MailConfigService.KEY_HOST, "127.0.0.1",
            MailConfigService.KEY_PORT, "1",
            MailConfigService.KEY_USERNAME, "saved@126.com");

        String reason = mailConfigService.testConnection(new MailConfigForm());

        assertNotNull(reason, "表单留空时应当沿用库里的主机/端口去试连");
        assertFalse(reason.contains("未配置"));
    }
}
