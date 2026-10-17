package com.zhi.web.controller.site;

import com.zhi.common.cache.UnifiedCacheManager;
import com.zhi.system.service.ISettingService;
import com.zhi.system.service.ISysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 前台公开站点设置接口：缺行/空值时的默认值契约。
 *
 * <p>背景：`business_label`（栏目名称）在导航/页脚/首页区块/核心业务页被多处消费，
 * 老库可能根本不存这一行；服务端必须给出与前端 fallback 一致的显式默认值，
 * 否则外部 API 消费方会拿到空字符串。</p>
 */
class FrontSettingControllerTest
{
    private ISettingService settingService;
    private ISysConfigService configService;
    private UnifiedCacheManager unifiedCacheManager;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        settingService = mock(ISettingService.class);
        configService = mock(ISysConfigService.class);
        unifiedCacheManager = mock(UnifiedCacheManager.class);

        // 老库：所有设置行都不存在、sys_config 也没有镜像（走 setDefaultValue 分支）
        lenient().when(settingService.selectSettingValueByKey(anyString())).thenReturn(null);
        lenient().when(configService.selectConfigList(any())).thenReturn(Collections.emptyList());

        FrontSettingController controller = new FrontSettingController();
        ReflectionTestUtils.setField(controller, "settingService", settingService);
        ReflectionTestUtils.setField(controller, "configService", configService);
        ReflectionTestUtils.setField(controller, "unifiedCacheManager", unifiedCacheManager);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void businessLabelShouldDefaultToCoreBusinessWhenMissing() throws Exception
    {
        mockMvc.perform(get("/common/site/setting"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.business_label").value("核心业务"));
    }

    @Test
    void cachedSettingsShouldBeServedDirectly() throws Exception
    {
        when(unifiedCacheManager.get(anyString(), any()))
            .thenReturn(Map.of("business_label", "产品中心"));

        mockMvc.perform(get("/common/site/setting"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.business_label").value("产品中心"));
    }
}
