package com.zhi.framework.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.zhi.framework.testsupport.SpringContextStub;
import com.zhi.system.service.ISettingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 防盗链配置提供者单元测试。
 *
 * <p>开关口径必须与全站一致（只有 false/"false"/"0" 算关闭），且"博客设置里改了立即生效"依赖
 * 5 秒缓存窗口；数据库没有配置时要回落到 application.yml 的默认值，读取抛错时按未配置处理。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefererPolicyTest
{
    @Mock
    private ISettingService settingService;

    private RefererPolicy policy;

    @BeforeEach
    void setUp()
    {
        SpringContextStub.install();
        policy = new RefererPolicy();
        ReflectionTestUtils.setField(policy, "settingService", settingService);
        ReflectionTestUtils.setField(policy, "defaultEnabled", false);
        ReflectionTestUtils.setField(policy, "defaultAllowedDomains", "");
    }

    private void settings(String enabled, String domains)
    {
        when(settingService.selectSettingValueByKey("referer_enabled")).thenReturn(enabled);
        when(settingService.selectSettingValueByKey("referer_allowed_domains")).thenReturn(domains);
    }

    @Test
    void databaseSwitchOverridesTheYamlDefault()
    {
        ReflectionTestUtils.setField(policy, "defaultEnabled", true);
        settings("false", "example.com");

        assertThat(policy.isEnabled()).isFalse();
    }

    @Test
    void missingSettingRowFallsBackToTheYamlDefault()
    {
        ReflectionTestUtils.setField(policy, "defaultEnabled", true);
        settings(null, null);

        assertThat(policy.isEnabled()).isTrue();
        assertThat(policy.getAllowedDomains()).isEmpty();
    }

    @Test
    void anyValueOtherThanFalseKeepsTheSiteWideOnSemantics()
    {
        assertThat(isOnAfterReload("1")).isTrue();
        assertThat(isOnAfterReload("true")).isTrue();
        assertThat(isOnAfterReload("0")).isFalse();
        assertThat(isOnAfterReload("false")).isFalse();
    }

    /** 绕过 5 秒缓存窗口，强制读取一次给定值 */
    private boolean isOnAfterReload(String enabled)
    {
        settings(enabled, null);
        ReflectionTestUtils.setField(policy, "loadedAtMillis", 0L);
        return policy.isEnabled();
    }

    @Test
    void domainsAcceptMixedSeparatorsAndDropDuplicatesAndBlanks()
    {
        settings("true", "a.com， b.com ;c.com\n\na.com ,,   ");

        assertThat(policy.getAllowedDomains()).containsExactly("a.com", "b.com", "c.com");
    }

    @Test
    void blankDatabaseDomainsFallBackToTheConfiguredWhitelist()
    {
        ReflectionTestUtils.setField(policy, "defaultAllowedDomains", "localhost:3000,example.com");
        settings("true", "   ");

        assertThat(policy.getAllowedDomains()).containsExactly("localhost:3000", "example.com");
    }

    @Test
    void settingsReadFailureDegradesToDefaultsInsteadOfBreakingStaticResources()
    {
        when(settingService.selectSettingValueByKey("referer_enabled"))
                .thenThrow(new RuntimeException("db down"));
        when(settingService.selectSettingValueByKey("referer_allowed_domains"))
                .thenThrow(new RuntimeException("db down"));
        ReflectionTestUtils.setField(policy, "defaultEnabled", true);
        ReflectionTestUtils.setField(policy, "defaultAllowedDomains", "example.com");

        assertThat(policy.isEnabled()).isTrue();
        assertThat(policy.getAllowedDomains()).containsExactly("example.com");
    }

    @Test
    void configurationIsCachedInsideTheTtlWindowAndRefreshedAfterItExpires()
    {
        settings("true", "a.com");
        assertThat(policy.getAllowedDomains()).containsExactly("a.com");

        // 缓存窗口内改数据库不应被感知（每个静态资源请求都查库的话开销不可接受）
        settings("true", "b.com");
        assertThat(policy.getAllowedDomains()).containsExactly("a.com");

        ReflectionTestUtils.setField(policy, "loadedAtMillis", System.currentTimeMillis() - 6000L);
        assertThat(policy.getAllowedDomains()).containsExactly("b.com");
    }
}
