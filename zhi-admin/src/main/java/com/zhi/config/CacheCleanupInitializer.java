package com.zhi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.zhi.common.cache.UnifiedCacheManager;

/**
 * 应用启动时自动清除所有站点相关缓存
 * 解决重启后显示缓存数据的问题
 *
 * @author nevell
 */
@Component
@Order(1) // 确保在其他初始化逻辑之后执行
public class CacheCleanupInitializer implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(CacheCleanupInitializer.class);

    @Autowired
    private UnifiedCacheManager unifiedCacheManager;

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        try
        {
            log.info("========================================");
            log.info("开始清除应用启动时的旧缓存...");

            int clearedCount = 0;

            // 清除所有站点相关缓存
            try {
                unifiedCacheManager.deleteByPattern("blog:*");
                clearedCount++;
                log.info("✓ 已清除所有站点缓存 (blog:*)");
            } catch (Exception e) {
                log.warn("清除站点缓存失败: {}", e.getMessage());
            }

            // 清除系统配置缓存
            try {
                unifiedCacheManager.deleteByPattern("sys_config:*");
                clearedCount++;
                log.info("✓ 已清除系统配置缓存 (sys_config:*)");
            } catch (Exception e) {
                log.warn("清除系统配置缓存失败: {}", e.getMessage());
            }

            // 清除字典缓存
            try {
                unifiedCacheManager.deleteByPattern("sys_dict:*");
                clearedCount++;
                log.info("✓ 已清除字典缓存 (sys_dict:*)");
            } catch (Exception e) {
                log.warn("清除字典缓存失败: {}", e.getMessage());
            }

            log.info("缓存清除完成，共清除 {} 类缓存", clearedCount);
            log.info("========================================");

        }
        catch (Exception e)
        {
            log.error("启动时清除缓存失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }
}
