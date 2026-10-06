package com.kirisamemarisa.seckillsystem.redis;

public interface KeyPrefix {

    int expireSeconds();

    String getPrefix();
}
