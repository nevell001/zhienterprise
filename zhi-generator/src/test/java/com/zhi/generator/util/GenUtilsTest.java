package com.zhi.generator.util;

import com.zhi.generator.config.GenConfig;
import com.zhi.generator.domain.GenTable;
import com.zhi.generator.domain.GenTableColumn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenUtils 代码生成器工具类测试
 *
 * @author ruoyi
 */
public class GenUtilsTest {

    @Test
    public void testArraysContains() {
        // 测试数组是否包含指定值
        String[] arr = {"a", "b", "c"};
        assertTrue(GenUtils.arraysContains(arr, "a"));
        assertTrue(GenUtils.arraysContains(arr, "b"));
        assertTrue(GenUtils.arraysContains(arr, "c"));
        assertFalse(GenUtils.arraysContains(arr, "d"));
        assertFalse(GenUtils.arraysContains(arr, null));
    }

    @Test
    public void testGetModuleName() {
        // 测试获取模块名
        assertEquals("common", GenUtils.getModuleName("com.zhi.common"));
        assertEquals("system", GenUtils.getModuleName("com.zhi.system"));
        assertEquals("framework", GenUtils.getModuleName("com.zhi.framework"));
    }

    @Test
    public void testGetBusinessName() {
        // 测试获取业务名
        assertEquals("user", GenUtils.getBusinessName("sys_user"));
        assertEquals("role", GenUtils.getBusinessName("sys_role"));
        assertEquals("menu", GenUtils.getBusinessName("sys_menu"));
        assertEquals("article", GenUtils.getBusinessName("zhi_article"));
    }

    @Test
    public void testConvertClassName() {
        // 测试表名转换成Java类名
        assertEquals("SysUser", GenUtils.convertClassName("sys_user"));
        assertEquals("SysRole", GenUtils.convertClassName("sys_role"));
        assertEquals("BlogArticle", GenUtils.convertClassName("zhi_article"));
    }

    @Test
    public void testReplaceFirst() {
        // 测试批量替换前缀
        String[] searchList = {"sys_", "blog_"};
        assertEquals("user", GenUtils.replaceFirst("sys_user", searchList));
        assertEquals("article", GenUtils.replaceFirst("zhi_article", searchList));
        assertEquals("test", GenUtils.replaceFirst("test", searchList));
    }

    @Test
    public void testReplaceText() {
        // 测试关键字替换
        assertEquals("用户", GenUtils.replaceText("用户表"));
        assertEquals("用户", GenUtils.replaceText("若依用户"));
        assertEquals("用户", GenUtils.replaceText("用户"));
        assertEquals("", GenUtils.replaceText("表若依"));
    }

    @Test
    public void testGetDbType() {
        // 测试获取数据库类型字段
        assertEquals("varchar", GenUtils.getDbType("varchar(255)"));
        assertEquals("int", GenUtils.getDbType("int(11)"));
        assertEquals("bigint", GenUtils.getDbType("bigint(20)"));
        assertEquals("text", GenUtils.getDbType("text"));
        assertEquals("datetime", GenUtils.getDbType("datetime"));
    }

    @Test
    public void testGetColumnLength() {
        // 测试获取字段长度
        assertEquals(255, GenUtils.getColumnLength("varchar(255)"));
        assertEquals(11, GenUtils.getColumnLength("int(11)"));
        assertEquals(20, GenUtils.getColumnLength("bigint(20)"));
        assertEquals(0, GenUtils.getColumnLength("text"));
        assertEquals(0, GenUtils.getColumnLength("datetime"));
    }

    @Test
    public void testInitTable() {
        // 测试初始化表信息
        GenTable genTable = new GenTable();
        genTable.setTableName("sys_user");
        genTable.setTableComment("用户表");

        // 设置 GenConfig 的静态字段值
        GenConfig.packageName = "com.zhi.system";
        GenConfig.author = "ruoyi";

        GenUtils.initTable(genTable, "admin");

        assertEquals("SysUser", genTable.getClassName());
        assertEquals("user", genTable.getBusinessName());
        assertEquals("用户", genTable.getFunctionName());
        assertEquals("admin", genTable.getCreateBy());
    }

    @Test
    public void testInitColumnField() {
        // 测试初始化列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("user_name");
        column.setColumnType("varchar(50)");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("userName", column.getJavaField());
        assertEquals(table.getTableId(), column.getTableId());
        assertEquals("admin", column.getCreateBy());
    }

    @Test
    public void testInitColumnFieldWithInteger() {
        // 测试初始化整型列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("user_id");
        column.setColumnType("int(11)");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("userId", column.getJavaField());
    }

    @Test
    public void testInitColumnFieldWithLong() {
        // 测试初始化长整型列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("create_time");
        column.setColumnType("bigint(20)");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("createTime", column.getJavaField());
    }

    @Test
    public void testInitColumnFieldWithBigDecimal() {
        // 测试初始化 BigDecimal 列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("price");
        column.setColumnType("decimal(10,2)");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("price", column.getJavaField());
    }

    @Test
    public void testInitColumnFieldWithDate() {
        // 测试初始化日期列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("create_time");
        column.setColumnType("datetime");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("createTime", column.getJavaField());
    }

    @Test
    public void testInitColumnFieldWithText() {
        // 测试初始化文本列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("content");
        column.setColumnType("text");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("content", column.getJavaField());
    }

    @Test
    public void testInitColumnFieldWithStatus() {
        // 测试初始化状态列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("status");
        column.setColumnType("char(1)");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("status", column.getJavaField());
    }

    @Test
    public void testInitColumnFieldWithType() {
        // 测试初始化类型列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("user_type");
        column.setColumnType("char(1)");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("userType", column.getJavaField());
    }

    @Test
    public void testInitColumnFieldWithImage() {
        // 测试初始化图片列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("cover_image");
        column.setColumnType("varchar(255)");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("coverImage", column.getJavaField());
    }

    @Test
    public void testInitColumnFieldWithFile() {
        // 测试初始化文件列属性字段
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setCreateBy("admin");

        GenTableColumn column = new GenTableColumn();
        column.setColumnName("attachment_file");
        column.setColumnType("varchar(255)");
        column.setIsPk("0");

        GenUtils.initColumnField(column, table);

        assertEquals("attachmentFile", column.getJavaField());
    }
}