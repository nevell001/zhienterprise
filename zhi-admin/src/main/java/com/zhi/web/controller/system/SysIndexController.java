package com.zhi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.annotation.Anonymous;
import com.zhi.common.config.RuoYiConfig;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页
 *
 * @author ruoyi
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private RuoYiConfig ruoyiConfig;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("欢迎使用{}，当前版本：v{}，请通过前端地址访问。", ruoyiConfig.getName(), ruoyiConfig.getVersion());
    }

    /**
     * 获取系统版本号
     */
    @Anonymous
    @GetMapping("/system/version")
    public AjaxResult getVersion()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("version", ruoyiConfig.getVersion());
        data.put("name", ruoyiConfig.getName());
        return AjaxResult.success(data);
    }
}
