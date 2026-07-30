//redis存键值对的核心接口，要求每个人往redis里面放东西的时候必须说明有效期是多长，前缀是什么
package com.kirisamemarisa.seckillsystem.redis;

public interface KeyPrefix {
    //有效期
    int expireSeconds();
    //前缀
    String getPrefix();
}