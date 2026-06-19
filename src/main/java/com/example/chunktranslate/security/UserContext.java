package com.example.chunktranslate.security;

/**
 * ThreadLocal 持有当前请求的用户 ID 和角色，供 MyBatisPlusHandler 自动填充 createdBy/updatedBy。
 * 在 JwtAuthenticationFilter 中设置，finally 块中清理。
 */
public class UserContext {

    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> roleHolder = new ThreadLocal<>();

    public static void set(Long userId, String role) {
        userIdHolder.set(userId);
        roleHolder.set(role);
    }

    public static Long getUserId() {
        return userIdHolder.get();
    }

    public static String getRole() {
        return roleHolder.get();
    }

    public static void clear() {
        userIdHolder.remove();
        roleHolder.remove();
    }
}
