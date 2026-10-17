package com.zhi.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 博客设置控制器单元测试
 *
 * @author test
 * @date 2025-07-18
 */
@WebMvcTest(controllers = SettingController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class SettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.zhi.system.service.ISettingService settingService;

    @MockBean
    private com.zhi.system.service.ISysConfigService configService;

    @MockBean
    private com.zhi.common.cache.UnifiedCacheManager unifiedCacheManager;

    @MockBean
    private com.zhi.system.service.MailConfigService mailConfigService;

    @MockBean
    private com.zhi.system.service.IEmailService emailService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试获取设置列表接口
     */
    @Test
    void testGetSettingList() throws Exception {
        // 模拟数据
        List<com.zhi.system.domain.Setting> settingList = new ArrayList<>();
        com.zhi.system.domain.Setting setting = new com.zhi.system.domain.Setting();
        setting.setId(1L);
        setting.setSettingKey("blog_name");
        setting.setSettingValue("我的博客");
        settingList.add(setting);

        when(settingService.selectSettingList(any(com.zhi.system.domain.Setting.class)))
            .thenReturn(settingList);

        // 执行测试
        mockMvc.perform(get("/system/setting/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].settingKey").value("blog_name"));

        verify(settingService).selectSettingList(any(com.zhi.system.domain.Setting.class));
    }

    /**
     * 测试通过设置键获取设置值接口
     */
    @Test
    void testGetSettingValueByKey() throws Exception {
        // 模拟数据
        when(settingService.selectSettingValueByKey("blog_name")).thenReturn("我的博客");

        // 执行测试
        mockMvc.perform(get("/system/setting/value/blog_name")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).selectSettingValueByKey("blog_name");
    }

    /**
     * 测试获取设置详情接口
     */
    @Test
    void testGetSettingDetail() throws Exception {
        // 模拟数据
        com.zhi.system.domain.Setting setting = new com.zhi.system.domain.Setting();
        setting.setId(1L);
        setting.setSettingKey("blog_name");
        setting.setSettingValue("我的博客");

        when(settingService.selectSettingById(1L)).thenReturn(setting);

        // 执行测试
        mockMvc.perform(get("/system/setting/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.settingKey").value("blog_name"));

        verify(settingService).selectSettingById(1L);
    }

    /**
     * 测试新增设置接口
     */
    @Test
    void testAddSetting() throws Exception {
        // 模拟成功添加
        when(settingService.insertSetting(any(com.zhi.system.domain.Setting.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "test_key");
        params.put("settingValue", "test_value");

        // 执行测试
        mockMvc.perform(post("/system/setting")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).insertSetting(any(com.zhi.system.domain.Setting.class));
    }

    /**
     * 测试更新设置接口
     */
    @Test
    void testEditSetting() throws Exception {
        // 模拟成功更新
        when(settingService.updateSetting(any(com.zhi.system.domain.Setting.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1L);
        params.put("settingKey", "blog_name");
        params.put("settingValue", "修改后的博客名称");

        // 执行测试
        mockMvc.perform(put("/system/setting")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).updateSetting(any(com.zhi.system.domain.Setting.class));
    }

    /**
     * 测试通过设置键更新设置值接口 (PUT)
     */
    @Test
    void testUpdateSettingByKey_Put() throws Exception {
        // 模拟成功更新
        when(settingService.updateSettingValueByKey("blog_name", "新博客名称")).thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "blog_name");
        params.put("settingValue", "新博客名称");

        // 执行测试
        mockMvc.perform(put("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).updateSettingValueByKey("blog_name", "新博客名称");
    }

    /**
     * 测试通过设置键更新设置值接口 (POST)
     */
    @Test
    void testUpdateSettingByKey_Post() throws Exception {
        // 模拟成功更新
        when(settingService.updateSettingValueByKey("blog_name", "新博客名称")).thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "blog_name");
        params.put("settingValue", "新博客名称");

        // 执行测试
        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).updateSettingValueByKey("blog_name", "新博客名称");
    }

    /**
     * 测试删除设置接口
     */
    @Test
    void testRemoveSetting() throws Exception {
        // 模拟成功删除
        when(settingService.deleteSettingByIds(any(Long[].class))).thenReturn(1);

        // 执行测试
        mockMvc.perform(delete("/system/setting/1,2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).deleteSettingByIds(any(Long[].class));
    }

    /**
     * 测试通过设置键更新设置值接口 (PUT) - 更新现有配置
     */
    @Test
    void testUpdateSettingByKey_Put_UpdateExisting() throws Exception {
        // 模拟成功更新
        when(settingService.updateSettingValueByKey("blog_name", "新博客名称")).thenReturn(1);

        // 模拟现有配置
        List<com.zhi.system.domain.SysConfig> configList = new ArrayList<>();
        com.zhi.system.domain.SysConfig config = new com.zhi.system.domain.SysConfig();
        config.setConfigKey("blog_name");
        config.setConfigValue("旧博客名称");
        configList.add(config);

        when(configService.selectConfigList(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(configList);
        when(configService.updateConfig(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "blog_name");
        params.put("settingValue", "新博客名称");

        // 执行测试
        mockMvc.perform(put("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).updateSettingValueByKey("blog_name", "新博客名称");
        verify(configService).updateConfig(any(com.zhi.system.domain.SysConfig.class));
    }

    /**
     * 栏目名称（business_label）是短标题：保存时去首尾空白（与前台渲染 trim 口径一致）。
     */
    @Test
    void testUpdateByKey_TrimsBusinessLabel() throws Exception {
        when(settingService.updateSettingValueByKey("business_label", "产品中心")).thenReturn(1);

        List<com.zhi.system.domain.SysConfig> configList = new ArrayList<>();
        com.zhi.system.domain.SysConfig config = new com.zhi.system.domain.SysConfig();
        config.setConfigKey("business_label");
        config.setConfigValue("核心业务");
        configList.add(config);
        when(configService.selectConfigList(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(configList);
        when(configService.updateConfig(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(1);

        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "business_label");
        params.put("settingValue", "  产品中心  ");

        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).updateSettingValueByKey("business_label", "产品中心");
    }

    /**
     * 栏目名称超过 20 字符必须拒绝：前端 maxlength 可被直接调接口绕过，
     * 超长值会撑坏导航与业务页标题。
     */
    @Test
    void testUpdateByKey_RejectsTooLongBusinessLabel() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "business_label");
        params.put("settingValue", "产品".repeat(11));

        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("栏目名称长度不能超过20个字符"));

        verify(settingService, never()).updateSettingValueByKey(eq("business_label"), any());
        verify(configService, never()).selectConfigList(any(com.zhi.system.domain.SysConfig.class));
    }

    /**
     * 测试通过设置键更新设置值接口 (PUT) - 创建新配置
     */
    @Test
    void testUpdateSettingByKey_Put_CreateNew() throws Exception {
        // 模拟成功更新
        when(settingService.updateSettingValueByKey("new_key", "新值")).thenReturn(1);

        // 模拟无现有配置
        when(configService.selectConfigList(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(new ArrayList<>());
        when(configService.insertConfig(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "new_key");
        params.put("settingValue", "新值");

        // 执行测试
        mockMvc.perform(put("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).updateSettingValueByKey("new_key", "新值");
        verify(configService).insertConfig(any(com.zhi.system.domain.SysConfig.class));
    }

    /**
     * 测试通过设置键更新设置值接口 (PUT) - 更新失败
     */
    @Test
    void testUpdateSettingByKey_Put_Failure() throws Exception {
        // 模拟更新失败
        when(settingService.updateSettingValueByKey("blog_name", "新博客名称")).thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "blog_name");
        params.put("settingValue", "新博客名称");

        // 执行测试
        mockMvc.perform(put("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(settingService).updateSettingValueByKey("blog_name", "新博客名称");
        // 验证不会调用 configService
        verify(configService, never()).selectConfigList(any(com.zhi.system.domain.SysConfig.class));
    }

    /**
     * 测试通过设置键更新设置值接口 (POST) - 更新现有配置
     */
    @Test
    void testUpdateSettingByKey_Post_UpdateExisting() throws Exception {
        // 模拟成功更新
        when(settingService.updateSettingValueByKey("blog_name", "新博客名称")).thenReturn(1);

        // 模拟现有配置
        List<com.zhi.system.domain.SysConfig> configList = new ArrayList<>();
        com.zhi.system.domain.SysConfig config = new com.zhi.system.domain.SysConfig();
        config.setConfigKey("blog_name");
        config.setConfigValue("旧博客名称");
        configList.add(config);

        when(configService.selectConfigList(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(configList);
        when(configService.updateConfig(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "blog_name");
        params.put("settingValue", "新博客名称");

        // 执行测试
        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).updateSettingValueByKey("blog_name", "新博客名称");
        verify(configService).updateConfig(any(com.zhi.system.domain.SysConfig.class));
    }

    /**
     * 测试通过设置键更新设置值接口 (POST) - 创建新配置
     */
    @Test
    void testUpdateSettingByKey_Post_CreateNew() throws Exception {
        // 模拟成功更新
        when(settingService.updateSettingValueByKey("new_key", "新值")).thenReturn(1);

        // 模拟无现有配置
        when(configService.selectConfigList(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(new ArrayList<>());
        when(configService.insertConfig(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(1);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "new_key");
        params.put("settingValue", "新值");

        // 执行测试
        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(settingService).updateSettingValueByKey("new_key", "新值");
        verify(configService).insertConfig(any(com.zhi.system.domain.SysConfig.class));
    }

    /**
     * 测试通过设置键更新设置值接口 (POST) - 更新失败
     */
    @Test
    void testUpdateSettingByKey_Post_Failure() throws Exception {
        // 模拟更新失败
        when(settingService.updateSettingValueByKey("blog_name", "新博客名称")).thenReturn(0);

        // 准备请求体
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "blog_name");
        params.put("settingValue", "新博客名称");

        // 执行测试
        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(settingService).updateSettingValueByKey("blog_name", "新博客名称");
        // 验证不会调用 configService
        verify(configService, never()).selectConfigList(any(com.zhi.system.domain.SysConfig.class));
    }

    /**
     * 回归护栏：zhi_setting 写入成功但 sys_config 同步抛异常时，必须**抛出异常**让事务回滚。
     *
     * <p>前台公开设置以 sys_config 为主数据源 —— 若此处吞掉异常并返回错误对象，调用方的
     * {@code @Transactional} 会照常提交，留下"zhi_setting 已改、sys_config 未同步"的不一致状态。
     * 因此必须让异常逃逸出事务边界（由 GlobalExceptionHandler 转成面向用户的提示）。</p>
     */
    @Test
    void testUpdateSettingByKey_Post_SyncFailure_RollsBack() throws Exception {
        when(settingService.updateSettingValueByKey("blog_name", "新博客名称")).thenReturn(1);
        when(configService.selectConfigList(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(new ArrayList<>());
        when(configService.insertConfig(any(com.zhi.system.domain.SysConfig.class)))
            .thenThrow(new RuntimeException("db down"));

        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "blog_name");
        params.put("settingValue", "新博客名称");

        // 异常必须逃逸（而非被吞成响应体），否则事务不会回滚
        Exception thrown = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
            mockMvc.perform(post("/system/setting/updateByKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(params))));
        org.junit.jupiter.api.Assertions.assertTrue(
            thrown.getMessage() != null && thrown.getMessage().contains("已回滚"),
            "异常信息应说明已回滚，实际: " + thrown.getMessage());
    }

    /**
     * 清空设置必须把 sys_config 镜像同步置空：updateConfig 的 mapper 对空值直接跳过，
     * 旧值会残留在 sys_config 被前台回退读取“复活”；而镜像行 configType='Y'
     * 受内置参数删除守卫保护删不掉，所以必须走专用的无条件写值语句。
     */
    @Test
    void testUpdateSettingByKey_ClearingValueClearsSysConfigMirror() throws Exception {
        when(settingService.updateSettingValueByKey("home_page_slug", "")).thenReturn(1);

        List<com.zhi.system.domain.SysConfig> configList = new ArrayList<>();
        com.zhi.system.domain.SysConfig config = new com.zhi.system.domain.SysConfig();
        config.setConfigId(108L);
        config.setConfigKey("home_page_slug");
        config.setConfigValue("about");
        configList.add(config);

        when(configService.selectConfigList(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(configList);

        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "home_page_slug");
        params.put("settingValue", "");

        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(configService).updateConfigValueByKey("home_page_slug", "");
        verify(configService, never()).updateConfig(any(com.zhi.system.domain.SysConfig.class));
        verify(configService, never()).deleteConfigByIds(any(Long[].class));
    }

    /**
     * 清空一个从未镜像过的设置：不需要创建空的 sys_config 行，保持"无行 = 未配置"。
     */
    @Test
    void testUpdateSettingByKey_ClearingValueWithoutMirrorCreatesNoRow() throws Exception {
        when(settingService.updateSettingValueByKey("home_page_slug", "")).thenReturn(1);
        when(configService.selectConfigList(any(com.zhi.system.domain.SysConfig.class)))
            .thenReturn(new ArrayList<>());

        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "home_page_slug");
        params.put("settingValue", "");

        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(configService, never()).insertConfig(any(com.zhi.system.domain.SysConfig.class));
        verify(configService, never()).updateConfig(any(com.zhi.system.domain.SysConfig.class));
        verify(configService, never()).updateConfigValueByKey(anyString(), anyString());
    }

    /**
     * mail_* 键必须走邮件专用接口：通用 updateByKey 会把它镜像进 sys_config（含密码），
     * 因此这里要直接拒绝，且不能落到 zhi_setting。
     */
    @Test
    void testUpdateSettingByKey_RejectsMailKey() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("settingKey", "mail_password");
        params.put("settingValue", "secret");

        mockMvc.perform(post("/system/setting/updateByKey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(settingService, never()).updateSettingValueByKey(eq("mail_password"), anyString());
    }

    /** 管理端列表读取时 SMTP 密码必须脱敏 */
    @Test
    void testGetSettingList_MasksMailPassword() throws Exception {
        List<com.zhi.system.domain.Setting> settingList = new ArrayList<>();
        com.zhi.system.domain.Setting secret = new com.zhi.system.domain.Setting();
        secret.setId(9L);
        secret.setSettingKey("mail_password");
        secret.setSettingValue("super-secret");
        settingList.add(secret);

        when(settingService.selectSettingList(any(com.zhi.system.domain.Setting.class)))
            .thenReturn(settingList);

        mockMvc.perform(get("/system/setting/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].settingKey").value("mail_password"))
                .andExpect(jsonPath("$.rows[0].settingValue").value(com.zhi.system.service.MailConfigService.PASSWORD_MASK));
    }

    /** 邮件配置读取端点：返回脱敏视图（不含 password），并带上"只打印不发信"的开发模式状态 */
    @Test
    void testGetMailConfig_returnsMaskedView() throws Exception {
        Map<String, Object> view = new HashMap<>();
        view.put("host", "smtp.example.com");
        view.put("port", 465);
        view.put("username", "no-reply@example.com");
        view.put("hasPassword", true);
        when(mailConfigService.getMaskedView()).thenReturn(view);
        when(emailService.isDevPrintCodeEnabled()).thenReturn(true);

        mockMvc.perform(get("/system/setting/mail")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.host").value("smtp.example.com"))
                .andExpect(jsonPath("$.data.hasPassword").value(true))
                .andExpect(jsonPath("$.data.devPrintCode").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    /** 开发模式关闭时该字段必须是 false，前端据此不再显示"不会真实发信"的警告 */
    @Test
    void testGetMailConfig_reportsDevPrintDisabled() throws Exception {
        when(mailConfigService.getMaskedView()).thenReturn(new HashMap<>());
        when(emailService.isDevPrintCodeEnabled()).thenReturn(false);

        mockMvc.perform(get("/system/setting/mail")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.devPrintCode").value(false));
    }

    /**
     * 注册开关读的是 sys_config（两个注册入口的读取源），而不是 zhi_setting，
     * 且判定口径与注册入口一致：只有字面 "true" 算开启。
     */
    @Test
    void testGetRegistrationSwitch_readsSysConfigWithStrictTrueRule() throws Exception {
        when(configService.selectConfigByKey("sys.account.registerUser")).thenReturn("true");
        mockMvc.perform(get("/system/setting/registration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // 博客开关的宽松口径会把 "1" 当作开启，注册入口却按 "true".equals 拒绝：
        // 卡片必须如实显示关闭，否则又是"显示已开启、注册仍被拒"
        when(configService.selectConfigByKey("sys.account.registerUser")).thenReturn("1");
        mockMvc.perform(get("/system/setting/registration"))
                .andExpect(jsonPath("$.data").value(false));

        verify(configService, times(2)).selectConfigByKey("sys.account.registerUser");
        verifyNoInteractions(settingService);
    }
}