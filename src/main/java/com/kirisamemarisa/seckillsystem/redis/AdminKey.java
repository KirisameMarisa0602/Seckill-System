package com.kirisamemarisa.seckillsystem.redis;

/**
 * 管理员会话 Redis Key。
 *
 * <p>完整前缀形如 {@code AdminKey:token:}，再拼接登录 Token。
 */
public class AdminKey extends BasePrefix {
    private AdminKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    /**
     * 管理员登录 Token。
     * <p>前缀：{@code AdminKey:token:}；TTL：1800 秒（30 分钟）。
     * <p>用途：存放管理员用户名；拦截器校验后会刷新过期时间。
     */
    public static AdminKey token = new AdminKey(1800, "token");
}
