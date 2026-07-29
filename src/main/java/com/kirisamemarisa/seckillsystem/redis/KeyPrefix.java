package com.kirisamemarisa.seckillsystem.redis;

public interface KeyPrefix {
    /** 有效期 (秒)。0 或负数代表永不过期 */
    int expireSeconds();
    /** 获取约定的前缀 */
    String getPrefix();
}