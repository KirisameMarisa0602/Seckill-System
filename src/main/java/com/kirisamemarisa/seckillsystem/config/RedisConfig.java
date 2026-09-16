package com.kirisamemarisa.seckillsystem.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Redis 客户端、Lua 脚本与 Pub/Sub 监听容器装配。
 *
 * <p>库存、限流、会话、Outbox 都走 Redis；Lua 保证扣库存/回滚/限流的原子性。
 * 无 {@code @Order}，Bean 在业务 Runner 之前完成注入。依赖中间件：Redis。
 */
@Configuration
public class RedisConfig {
    /**
     * 对象 RedisTemplate：Key 用字符串，Value 用受限多态 JSON。
     *
     * <p>Key/HashKey 用 {@link StringRedisSerializer}，方便与 {@link StringRedisTemplate}、Lua KEYS 对齐。
     * Value 开 default typing 是为了把 {@code User} 等实体原样反序列化，而不是 {@code LinkedHashMap}。
     * {@link BasicPolymorphicTypeValidator} 只放行本包与 {@code java.math}/{@code java.time}，
     * 避免 Jackson 多态被用来打反序列化 gadget。时间字段固定 {@code yyyy-MM-dd HH:mm:ss}，与业务 VO 一致。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.kirisamemarisa.seckillsystem.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .build();
        om.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
        JavaTimeModule timeModule = new JavaTimeModule();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dtf));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dtf));
        om.registerModule(timeModule);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 纯字符串模板，供库存计数、限流 Lua、Pub/Sub 等不需要对象多态的路径使用。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * 秒杀预扣库存 Lua：原子 decr、写一人一单标记与 Outbox。
     */
    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("scripts/seckill-stock.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 接口限流 Lua：窗口内 incr，超阈值返回 0。
     */
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("scripts/rate-limit.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 预扣失败/关单回滚 Lua：还库存、删一人一单与售罄标记。
     */
    @Bean
    public DefaultRedisScript<Long> rollbackSeckillScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("scripts/rollback-seckill.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 订阅 {@code stock_replenish_channel}，把补货广播交给 {@link StockRestoreListener} 清本地售罄缓存。
     */
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, StockRestoreListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new PatternTopic("stock_replenish_channel"));
        return container;
    }
}
