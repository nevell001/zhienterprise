package com.zhi.system.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 页面回收站/定时发布的 SQL 形态守卫（跨文件契约，无法在 H2 上覆盖 MySQL 的 UPDATE 表达式）。
 */
class PageLifecycleQueryTest
{
    private String xml() throws Exception
    {
        try (var in = getClass().getResourceAsStream("/mapper/system/PageMapper.xml"))
        {
            assertNotNull(in, "找不到 PageMapper.xml");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String block(String xml, String id)
    {
        int start = xml.indexOf("id=\"" + id + "\"");
        assertTrue(start >= 0, id + " 查询缺失");
        int end = xml.length();
        for (String close : new String[] { "</select>", "</insert>", "</update>", "</delete>" })
        {
            int i = xml.indexOf(close, start);
            if (i >= 0 && i < end)
            {
                end = i;
            }
        }
        return xml.substring(start, end);
    }

    @Test
    void deleteMustPreserveOriginalSlugBeforeRenaming() throws Exception
    {
        for (String id : new String[] { "deletePageById", "deletePageByIds" })
        {
            String sql = block(xml(), id);
            assertTrue(sql.contains("deleted_slug = slug"), id + " 必须保存原 slug");
            assertTrue(sql.contains("concat('__deleted_'"), id + " 必须把当前 slug 改为内部占位值");
            assertTrue(sql.contains("left(slug, 60)"), id + " 占位 slug 需截断，避免超过 100 字符");
        }
    }

    @Test
    void restoreMustPutOriginalSlugBack() throws Exception
    {
        String sql = block(xml(), "restorePageByIds");
        assertTrue(sql.contains("coalesce(deleted_slug, slug)"), "恢复时优先还原 deleted_slug");
        assertTrue(sql.contains("deleted_slug = null"), "恢复后清除回收站别名");
    }

    @Test
    void updateMustSetPublishTimeForScheduledAndClearForOthers() throws Exception
    {
        String sql = block(xml(), "updatePage");
        assertTrue(sql.contains("publish_time = #{publishTime}"), "定时发布必须写入 publish_time");
        // OGNL 的单引号 '2' 会被当成 char，String 比较恒不等，导致刚写入的 publish_time 又被清空
        assertTrue(sql.contains("status != \"2\""), "状态比较必须用双引号字符串");
        assertFalse(sql.contains("status != '2'"), "不得使用单引号字符比较");
    }

    @Test
    void scheduledPublishMustOnlyPublishDueRows() throws Exception
    {
        String sql = block(xml(), "publishScheduledPages");
        assertTrue(sql.contains("status = '2'"));
        assertTrue(sql.contains("publish_time &lt;= now()"));
        assertTrue(sql.contains("del_flag = '0'"));
    }
}