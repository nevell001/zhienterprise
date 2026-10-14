package com.zhi.common.utils;

/**
 * 博客功能开关判定工具
 * 
 * <p>与前端 {@code useSettingsStore().isFeatureEnabled} 保持同一口径：
 * 只有 {@code false / "false" / "0"} 视为关闭，未配置或 {@code true / "true" / "1"} 视为开启，
 * 避免前后端各自判断导致「开关不生效」。</p>
 *
 * @author nevell
 * @date 2026-09-10
 */
public class SiteSwitchUtils
{
    private SiteSwitchUtils()
    {
    }

    /**
     * 开关是否开启（未配置时默认开启）
     * 
     * @param value 设置值
     * @return true 开启
     */
    public static boolean isOn(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return true;
        }
        String trimmed = value.trim();
        return !("false".equalsIgnoreCase(trimmed) || "0".equals(trimmed));
    }

    /**
     * 开关是否关闭（未配置时视为开启，即返回 false）
     * 
     * @param value 设置值
     * @return true 关闭
     */
    public static boolean isOff(String value)
    {
        return !isOn(value);
    }
}
