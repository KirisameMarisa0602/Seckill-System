package com.kirisamemarisa.seckillsystem.redis;

/**
 * 用户会话 Redis Key。
 *
 * <p>完整前缀形如 {@code UserKey:token:}，再拼接登录 Token。
 */
public class UserKey extends BasePrefix {
    private UserKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    /**
     * 用户登录 Token。
     * <p>前缀：{@code UserKey:token:}；TTL：7 天（{@code 3600 * 24 * 7} 秒）。
     * <p>用途：存放已脱敏的用户对象，供参数解析与限流拦截器识别登录态。
     */
    public static UserKey token = new UserKey(3600 * 24 * 7,"token");
}
