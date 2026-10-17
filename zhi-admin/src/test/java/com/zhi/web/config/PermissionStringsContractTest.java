package com.zhi.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限串「代码 ↔ 菜单数据」一致性守卫。
 *
 * <p>本项目的鉴权模型：Spring Security 只兜底「登录即可」，细粒度权限全部来自
 * {@code @PreAuthorize("@ss.hasPermi('x:y:z')")}，而权限串本身由 {@code sys_menu.perms}
 * 通过角色授予。两侧一旦脱节就有两类事故：</p>
 *
 * <ul>
 *   <li><b>代码要求、库里没有</b> —— 该接口对**所有人**都 403（含超管），功能直接不可用。
 *       这是最严重的方向，本测试断言它为**空集**。</li>
 *   <li><b>库里有、代码不用</b> —— 冗余菜单项/按钮权限，不构成安全问题，但会误导维护者。
 *       允许存在，但必须有显式清单（见下），新增冗余会失败并提示登记。</li>
 * </ul>
 *
 * <p>注：这里做跨产物文本契约（Java 注解 ↔ SQL 种子数据），行为验证需要真实库与登录态；
 * 与 {@code MapperColumnSchemaContractTest} 同类。审计已用运行库
 * {@code SELECT perms FROM sys_menu} 二次确认过同一结论。</p>
 */
class PermissionStringsContractTest
{
    /** {@code sql/00_init_database.sql}（仓库根目录下的 sql/） */
    private static final Path SQL_FILE = resolveSql();

    /**
     * 已知的「库里有、代码不用」权限串 —— 均为 RuoYi 框架遗留的监控/工具菜单，
     * 前端以**外链**形式打开（不经后端 @PreAuthorize），因此保留是合理的。
     *
     * <p>新增条目必须在此登记并写明理由，否则测试失败 —— 避免把"真漏了权限校验"
     * 混进冗余集合里被忽视。</p>
     */
    private static final Set<String> KNOWN_UNUSED_MENU_PERMS = Set.of(
        // 监控：RuoYi 内置，前端为外链/只读页面
        "monitor:actuator:list", "monitor:actuator:query",
        "monitor:cache:add", "monitor:cache:clean", "monitor:cache:edit",
        "monitor:cache:query", "monitor:cache:remove",
        "monitor:druid:list", "monitor:grafana:list", "monitor:grafana:query",
        "monitor:job:detail", "monitor:job:execute",
        "monitor:logininfor:query", "monitor:online:batchLogout", "monitor:online:query",
        "monitor:operlog:query", "monitor:prometheus:list", "monitor:prometheus:query",
        // 工具：代码生成/文档，前端外链
        "tool:build:list", "tool:gen:add", "tool:swagger:list",
        // 统计：查询权限挂在菜单上用于控制页面可见性，接口本身按 list 校验
        "statistics:article:export", "statistics:article:query",
        "statistics:overview:query", "statistics:user:export",
        "statistics:user:query", "statistics:visit:query"
    );

    @Test
    @DisplayName("代码要求的权限串必须都存在于菜单数据中（否则接口对所有人 403）")
    void everyCodePermissionMustExistInMenuData() throws IOException
    {
        Set<String> codePerms = extractCodePerms();
        Set<String> menuPerms = extractMenuPerms();

        assertTrue(codePerms.size() > 100,
            "解析到的权限串过少（" + codePerms.size() + "），注解格式可能已变化");

        Set<String> missing = new TreeSet<>(codePerms);
        missing.removeAll(menuPerms);

        assertTrue(missing.isEmpty(),
            "以下权限串被 @PreAuthorize 要求，但 sys_menu.perms 里没有 —— 这些接口会对包括超管在内的"
                + "所有角色 403：\n  " + String.join("\n  ", missing)
                + "\n修复：在 sql/00_init_database.sql 的 sys_menu 种子里补上对应菜单/按钮权限。");
    }

    @Test
    @DisplayName("菜单数据中的冗余权限串必须已登记在已知清单里")
    void unusedMenuPermsMustBeDeclared() throws IOException
    {
        Set<String> codePerms = extractCodePerms();
        Set<String> menuPerms = extractMenuPerms();

        Set<String> unused = new TreeSet<>(menuPerms);
        unused.removeAll(codePerms);

        Set<String> undeclared = new TreeSet<>(unused);
        undeclared.removeAll(KNOWN_UNUSED_MENU_PERMS);

        assertTrue(undeclared.isEmpty(),
            "发现未登记的冗余权限串（库里有、代码不用）：\n  " + String.join("\n  ", undeclared)
                + "\n若是框架遗留/前端外链，请加入 KNOWN_UNUSED_MENU_PERMS 并写明理由；"
                + "若本应是接口权限，则说明接口漏写了 @PreAuthorize。");
    }

    /** 提取所有 {@code hasPermi('...')} 里的权限串 */
    private static Set<String> extractCodePerms() throws IOException
    {
        Set<String> perms = new LinkedHashSet<>();
        Pattern p = Pattern.compile("hasPermi\\('([^']+)'\\)");
        for (Path src : javaSources())
        {
            String content = Files.readString(src, StandardCharsets.UTF_8);
            Matcher m = p.matcher(content);
            while (m.find())
            {
                perms.add(m.group(1));
            }
        }
        return perms;
    }

    /**
     * 从 SQL 里提取形如 {@code aaa:bbb:ccc} 的权限串字面量。
     * 故意放宽第三段（允许连字符，如 {@code statistics:article:hot-tags}）。
     *
     * <p>会剔除**历史迁移语句**里的旧值 —— 例如
     * {@code UPDATE sys_menu SET perms='zhi:comment:audit' WHERE perms='zhi:comment:approve'}：
     * 后者只是 WHERE 条件中的历史值，从来不是被授予的权限，不该算进菜单权限集合。</p>
     */
    private static Set<String> extractMenuPerms() throws IOException
    {
        String sql = Files.readString(SQL_FILE, StandardCharsets.UTF_8);

        // 去掉 UPDATE 语句里的 WHERE 旧值（保留 SET 侧的新值）
        String normalized = Pattern.compile(
                "UPDATE\\s+sys_menu\\s+SET\\s+perms\\s*=\\s*'([^']+)'\\s+WHERE[^;]*;",
                Pattern.CASE_INSENSITIVE)
            .matcher(sql)
            .replaceAll(m -> "UPDATE sys_menu SET perms = '" + m.group(1) + "';");

        Set<String> perms = new TreeSet<>();
        Matcher m = Pattern.compile("'([a-zA-Z_]+:[a-zA-Z_:-]+)'").matcher(normalized);
        while (m.find())
        {
            perms.add(m.group(1));
        }
        return perms;
    }

    /** 递归收集各模块 src/main/java 下的 Java 源文件 */
    private static java.util.List<Path> javaSources() throws IOException
    {
        try (Stream<Path> walk = Files.walk(Paths.get("..")))
        {
            return walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("src/main/java"))
                .filter(p -> !p.toString().contains("/target/"))
                .collect(Collectors.toList());
        }
    }

    private static Path resolveSql()
    {
        Path candidate = Paths.get("../sql/00_init_database.sql");
        assertTrue(Files.exists(candidate), "未找到 sql/00_init_database.sql（工作目录: "
            + Paths.get("").toAbsolutePath() + "）");
        return candidate;
    }
}