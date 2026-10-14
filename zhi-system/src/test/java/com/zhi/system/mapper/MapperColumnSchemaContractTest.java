package com.zhi.system.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mapper 引用的列必须真实存在于初始化脚本建的表结构里（跨产物一致性检查，不是行为断言 ——
 * 只有把 SQL 脚本和 mapper 放在一起比对才可能发现这类缺陷，运行时反而不会提前报）。
 *
 * <p>起因：{@code zhi_message} 建表时漏了 {@code create_by} / {@code update_by}，而
 * {@code BlogMessageMapper} 从生成那天起就 select 这两列。列表、审核、回复三条路径都会抛
 * {@code Unknown column 'create_by' in 'field list'}，而前台留言提交（不写这两列）一切正常，
 * 所以缺陷只在后台「留言管理」第一次打开时暴露。</p>
 */
class MapperColumnSchemaContractTest
{
    private static final Path SCHEMA = Path.of("../sql/00_init_database.sql");

    private static final Path MAPPER_DIR = Path.of("src/main/resources/mapper");

    /** CREATE TABLE IF NOT EXISTS `t` (...) —— 取反引号列名 */
    private static final Pattern CREATE_TABLE =
        Pattern.compile("CREATE TABLE IF NOT EXISTS\\s+`?(\\w+)`?\\s*\\(([\\s\\S]*?)\\n\\)\\s*ENGINE",
            Pattern.CASE_INSENSITIVE);

    /**
     * 列定义行：反引号或裸标识符开头，后面紧跟类型关键字。
     * 用类型关键字过滤，天然跳过 PRIMARY KEY / UNIQUE KEY / KEY / CONSTRAINT / FULLTEXT 这些约束行。
     */
    private static final Pattern COLUMN_LINE = Pattern.compile(
        "^\\s*(?:`(\\w+)`|(\\w+))\\s+"
            + "(?:BIGINT|SMALLINT|MEDIUMINT|TINYINT|INT|DECIMAL|NUMERIC|FLOAT|DOUBLE|BIT"
            + "|VARBINARY|BINARY|TINYBLOB|BLOB|MEDIUMBLOB|LONGBLOB|TINYTEXT|TEXT|MEDIUMTEXT|LONGTEXT"
            + "|CHAR|VARCHAR|DATE|TIME|DATETIME|TIMESTAMP|YEAR|JSON|ENUM|SET)\\b",
        Pattern.CASE_INSENSITIVE);

    /** 幂等补齐块里的 ALTER TABLE ... ADD COLUMN（语句写在 PREPARE 的字符串里） */
    private static final Pattern TABLE_NAME_IN_ALTER =
        Pattern.compile("ALTER TABLE `(\\w+)` ADD COLUMN `(\\w+)`");

    private static final Pattern FROM_TABLE = Pattern.compile("\\bfrom\\s+`?(\\w+)`?",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern RESULT_COLUMN =
        Pattern.compile("<(?:result|id)\\b[^>]*column=\"(\\w+)\"");

    /** "x as col" / "as col" 形式产生的别名不是表上的列 */
    private static final Pattern ALIAS = Pattern.compile("\\bas\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern SELECT_LIST = Pattern.compile("(?is)select\\s+([^<]+?)\\s+from\\s");

    private static final Pattern DYNAMIC_COLUMN =
        Pattern.compile("<if test=\"[^\"]+\">\\s*(\\w+)\\s*(?:,|=)", Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("初始化脚本能解析出表结构，且留言表带 RuoYi 约定的操作人列")
    void schemaParsesAndMessageTableIsComplete() throws IOException
    {
        Map<String, Set<String>> schema = schema();
        assertTrue(schema.containsKey("zhi_message"), "初始化脚本没解析出 zhi_message");
        Set<String> columns = schema.get("zhi_message");
        // mapper 的 select 列表与审核/回复更新都引用这两列，缺一个后台就是 500
        assertTrue(columns.contains("create_by"), "zhi_message 缺少 create_by 列");
        assertTrue(columns.contains("update_by"), "zhi_message 缺少 update_by 列");
    }

    @Test
    @DisplayName("每个单表 mapper 引用的列都必须存在于表结构里")
    void mapperColumnsExistInSchema() throws IOException
    {
        Map<String, Set<String>> schema = schema();
        assertFalse(schema.isEmpty(), "初始化脚本解析失败");

        try (Stream<Path> files = Files.walk(MAPPER_DIR))
        {
            for (Path xml : files.filter(p -> p.toString().endsWith(".xml")).toList())
            {
                String text = Files.readString(xml);
                String table = singleSchemaTable(text, schema);
                if (table == null)
                {
                    // 多表 JOIN / 非本项目表（Quartz 等）不做比对，规则只保证"表自己的列"
                    continue;
                }
                Set<String> tableColumns = schema.get(table);
                Set<String> columns = referencedColumns(text);
                Set<String> unknown = new LinkedHashSet<>(columns);
                unknown.removeAll(tableColumns);
                assertTrue(unknown.isEmpty(),
                    xml + " 引用了 " + table + " 表上不存在的列 " + unknown
                        + "（要么补列到 sql/00_init_database.sql，要么从 mapper 删掉）");
            }
        }
    }

    private Map<String, Set<String>> schema() throws IOException
    {
        String sql = Files.readString(SCHEMA);
        Map<String, Set<String>> tables = new HashMap<>();
        Matcher create = CREATE_TABLE.matcher(sql);
        while (create.find())
        {
            Set<String> columns = new LinkedHashSet<>();
            for (String line : create.group(2).split("\n"))
            {
                Matcher column = COLUMN_LINE.matcher(line);
                if (column.find())
                {
                    String name = column.group(1) != null ? column.group(1) : column.group(2);
                    columns.add(name.toLowerCase(Locale.ROOT));
                }
            }
            tables.put(create.group(1).toLowerCase(Locale.ROOT), columns);
        }
        Matcher alter = TABLE_NAME_IN_ALTER.matcher(sql);
        while (alter.find())
        {
            tables.computeIfAbsent(alter.group(1).toLowerCase(Locale.ROOT), k -> new LinkedHashSet<>())
                .add(alter.group(2).toLowerCase(Locale.ROOT));
        }
        return tables;
    }

    /** 只认"整个 mapper 只查一张本项目表"的情况，避免把 JOIN 的另一侧列算进来 */
    private String singleSchemaTable(String text, Map<String, Set<String>> schema)
    {
        Set<String> tables = new LinkedHashSet<>();
        Matcher from = FROM_TABLE.matcher(text);
        while (from.find())
        {
            String candidate = from.group(1).toLowerCase(Locale.ROOT);
            if (schema.containsKey(candidate))
            {
                tables.add(candidate);
            }
        }
        return tables.size() == 1 ? tables.iterator().next() : null;
    }

    private Set<String> referencedColumns(String text)
    {
        Set<String> aliases = new LinkedHashSet<>();
        Matcher alias = ALIAS.matcher(text);
        while (alias.find())
        {
            aliases.add(alias.group(1).toLowerCase(Locale.ROOT));
        }

        Set<String> columns = new LinkedHashSet<>();
        Matcher result = RESULT_COLUMN.matcher(text);
        while (result.find())
        {
            String column = result.group(1).toLowerCase(Locale.ROOT);
            if (!aliases.contains(column))
            {
                columns.add(column);
            }
        }
        Matcher list = SELECT_LIST.matcher(text);
        while (list.find())
        {
            for (String token : list.group(1).split(","))
            {
                String column = token.trim().toLowerCase(Locale.ROOT);
                // 只收裸列名：带函数、限定名或 as 别名的表达式交给 resultMap 那条规则比对
                if (column.matches("\\w+") && !aliases.contains(column))
                {
                    columns.add(column);
                }
            }
        }
        Matcher dynamic = DYNAMIC_COLUMN.matcher(text);
        while (dynamic.find())
        {
            columns.add(dynamic.group(1).toLowerCase(Locale.ROOT));
        }
        return columns;
    }
}
