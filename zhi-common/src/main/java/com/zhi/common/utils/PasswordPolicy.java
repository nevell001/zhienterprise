package com.zhi.common.utils;

import com.zhi.common.constant.UserConstants;

/**
 * 口令策略：注册 / 密码重置 / 修改密码三处共用的唯一校验入口。
 *
 * 背景（真实缺陷）：注册路径靠 DTO 上的 {@code @Size(min = 5, max = 20)} 约束，
 * 而 {@code /api/auth/reset-password} 接收裸 {@code Map}（Bean Validation 注解在裸 Map 上
 * 不生效），只判空 + 判两次一致就把口令写库 —— 形成"注册有长度限制、重置没有"的策略不对称，
 * 可把账号口令设成单字符。
 *
 * 因此这里把校验下沉到一个纯函数，三处共同调用，避免再出现"某条路径漏校验"。
 * 长度沿用 {@link UserConstants#PASSWORD_MIN_LENGTH} / {@code MAX}（与登录侧一致）。
 */
public final class PasswordPolicy
{
    private PasswordPolicy() {}

    public static final int MIN_LENGTH = UserConstants.PASSWORD_MIN_LENGTH;
    public static final int MAX_LENGTH = UserConstants.PASSWORD_MAX_LENGTH;

    /**
     * 校验口令强度，不通过则抛出带用户可读文案的异常。
     *
     * @param rawPassword 明文口令（加密前）
     * @throws IllegalArgumentException 口令不符合策略
     */
    public static void validate(String rawPassword)
    {
        String reason = check(rawPassword);
        if (reason != null)
        {
            throw new IllegalArgumentException(reason);
        }
    }

    /**
     * 校验口令强度，返回 null 表示通过，否则返回失败原因（供不便抛异常的场景使用）。
     */
    public static String check(String rawPassword)
    {
        if (StringUtils.isEmpty(rawPassword))
        {
            return "密码不能为空";
        }
        if (rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH)
        {
            return "密码长度必须介于" + MIN_LENGTH + "和" + MAX_LENGTH + "之间";
        }
        // 全同字符（111111 / aaaaaa）与纯顺序串属最常见弱口令
        if (isSingleRepeatedChar(rawPassword))
        {
            return "密码不能是重复的单一字符";
        }
        if (containsWhitespace(rawPassword))
        {
            return "密码不能包含空白字符";
        }
        return null;
    }

    /** 是否全部为同一字符 */
    private static boolean isSingleRepeatedChar(String s)
    {
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++)
        {
            if (s.charAt(i) != first)
            {
                return false;
            }
        }
        return true;
    }

    private static boolean containsWhitespace(String s)
    {
        for (int i = 0; i < s.length(); i++)
        {
            if (Character.isWhitespace(s.charAt(i)))
            {
                return true;
            }
        }
        return false;
    }
}