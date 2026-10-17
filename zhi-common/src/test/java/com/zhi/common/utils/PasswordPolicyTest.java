package com.zhi.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 口令策略单测。
 *
 * 背景：/api/auth/reset-password 接收裸 Map（Bean Validation 注解不生效），
 * 此前只判空就写库，可把账号口令设成单字符；本策略是注册/重置/改密三处的共同入口。
 */
class PasswordPolicyTest
{
    @Test
    void acceptsNormalPassword()
    {
        assertNull(PasswordPolicy.check("abc12345"));
        assertNull(PasswordPolicy.check("P@ssw0rd!"));
        assertEquals(5, PasswordPolicy.MIN_LENGTH);
        assertEquals(20, PasswordPolicy.MAX_LENGTH);
    }

    @Test
    void rejectsEmpty()
    {
        assertNotNull(PasswordPolicy.check(null));
        assertNotNull(PasswordPolicy.check(""));
        assertEquals("密码不能为空", PasswordPolicy.check(""));
    }

    @Test
    void rejectsTooShortOrTooLong()
    {
        // 单字符口令正是本次修复要拦住的形态
        assertNotNull(PasswordPolicy.check("1"));
        assertNotNull(PasswordPolicy.check("1234"));
        assertNotNull(PasswordPolicy.check("123456789012345678901"));
        assertEquals("密码长度必须介于" + PasswordPolicy.MIN_LENGTH + "和"
                + PasswordPolicy.MAX_LENGTH + "之间", PasswordPolicy.check("1"));
    }

    @Test
    void acceptsBoundaryLengths()
    {
        assertNull(PasswordPolicy.check("12345"), "恰好 5 位应通过");
        assertNull(PasswordPolicy.check("1234567890123456789a"), "恰好 20 位应通过");
    }

    @Test
    void rejectsSingleRepeatedChar()
    {
        assertNotNull(PasswordPolicy.check("111111"));
        assertNotNull(PasswordPolicy.check("aaaaa"));
        assertEquals("密码不能是重复的单一字符", PasswordPolicy.check("111111"));
    }

    @Test
    void rejectsWhitespace()
    {
        assertNotNull(PasswordPolicy.check("abc 123"));
        assertNotNull(PasswordPolicy.check("abc\t123"));
    }

    @Test
    void validateThrowsWithReadableMessage()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.validate("1"));
        assertEquals("密码长度必须介于" + PasswordPolicy.MIN_LENGTH + "和"
                + PasswordPolicy.MAX_LENGTH + "之间", e.getMessage());

        // 合法口令不应抛异常
        PasswordPolicy.validate("abc12345");
    }
}