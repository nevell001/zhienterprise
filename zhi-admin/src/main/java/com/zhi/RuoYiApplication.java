package com.zhi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.zhi.common.config.ImageCompressConfig;

/**
 * 启动程序
 *
 * 注意：不要排除 DataSourceAutoConfiguration——Druid 的 DruidDataSourceWrapper
 * 需要容器中存在 DataSourceProperties bean（Boot 3.5 + Druid 1.2.28 起为必填依赖）。
 * 该自动配置只会创建属性对象，真正的数据源仍由 DruidConfig 的
 * master/slave/dynamicDataSource 提供（@ConditionalOnMissingBean 自动让位）。
 *
 * @author ruoyi
 */
@SpringBootApplication(exclude = { RedisAutoConfiguration.class })
@ComponentScan(basePackages = {"com.zhi", "com.zhi.system.controller"})
@EnableConfigurationProperties({ImageCompressConfig.class})
@org.springframework.scheduling.annotation.EnableScheduling
public class RuoYiApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RuoYiApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  知企建站系统启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
