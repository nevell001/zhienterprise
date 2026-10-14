package com.zhi.web.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring bean 名冲突守卫（跨模块静态扫描）。
 *
 * <p>为什么不做行为断言：加载整上下文需要真实 MySQL/Redis，而本仓库的 Web 层测试都是
 * {@code @WebMvcTest} 切片，两个模块里的同名类根本不会同时进容器；这类冲突只在启动瞬间
 * 炸 {@code ConflictingBeanDefinitionException}。
 *
 * <p>2026-10-14 的 blog→zhi 重命名就踩了这个坑：`BlogStatisticsController` 去前缀后变成
 * `StatisticsController`，与 zhi-admin 既有的 `StatisticsController` 同名，
 * 后端容器起来即退出。规则：带 {@code @RestController/@Controller/@Service/@Component/
 * @Repository/@Configuration/@Mapper} 的类，未显式指定 bean 名时其简单名在同一容器内必须唯一
 * （显式命名如 {@code @RestController("frontArticleController")} 视为已消歧）。
 */
class BeanNameUniquenessTest {

    private static final List<String> MODULES = List.of(
            "zhi-common", "zhi-framework", "zhi-system", "zhi-quartz", "zhi-generator", "zhi-admin");

    private static final Pattern BEAN_ANNOTATION = Pattern.compile(
            "@(RestController|Controller|Service|Component|Repository|Configuration|Mapper)\\b(\\s*\\(\\s*\"([^\"]+)\"\\s*\\))?");
    private static final Pattern TYPE_DECL = Pattern.compile(
            "(?m)^\\s*(?:public\\s+)?(?:final\\s+)?(?:abstract\\s+)?(class|interface|enum)\\s+(\\w+)");

    @Test
    void beanSimpleNamesMustBeUniqueAcrossModules() throws IOException {
        Path repoRoot = Paths.get("..").toAbsolutePath().normalize();
        Map<String, List<String>> byBeanName = new HashMap<>();
        int scanned = 0;

        for (String module : MODULES) {
            Path src = repoRoot.resolve(module).resolve("src/main/java");
            if (!Files.isDirectory(src)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(src)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String text = Files.readString(file);
                    Matcher annotation = BEAN_ANNOTATION.matcher(text);
                    if (!annotation.find()) {
                        continue;
                    }
                    Matcher type = TYPE_DECL.matcher(text);
                    if (!type.find()) {
                        continue;
                    }
                    scanned++;
                    String explicit = annotation.group(3);
                    String beanName = explicit != null
                            ? explicit
                            : Character.toLowerCase(type.group(2).charAt(0)) + type.group(2).substring(1);
                    byBeanName.computeIfAbsent(beanName, k -> new ArrayList<>())
                            .add(module + ":" + repoRoot.relativize(file));
                }
            }
        }

        // 防止路径/正则失效导致「空集合也通过」
        assertThat(scanned).isGreaterThan(80);

        List<String> conflicts = byBeanName.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> "bean '" + e.getKey() + "' → " + String.join(", ", e.getValue()))
                .sorted()
                .toList();

        assertThat(conflicts)
                .as("同名 bean 会在启动时抛 ConflictingBeanDefinitionException；"
                        + "要么改名，要么用 @RestController(\"唯一名\") 显式命名")
                .isEmpty();
    }
}