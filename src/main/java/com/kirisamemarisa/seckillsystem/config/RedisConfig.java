package com.kirisamemarisa.seckillsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * V2.0 优化：将 Lua 脚本加载为 Spring Bean
     * 避免了每次请求都在方法体内 new 对象和解析字符串，极大提升并发性能
     */
    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        // 指定 resources 目录下的 lua 脚本文件位置
        redisScript.setLocation(new ClassPathResource("scripts/seckill-stock.lua"));
        // 指定返回类型为 Long (必须与 Lua 脚本中的 return 保持一致，也就是 1 或 0)
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}