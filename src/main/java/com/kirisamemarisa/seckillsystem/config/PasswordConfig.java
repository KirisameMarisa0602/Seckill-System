package com.kirisamemarisa.seckillsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器装配。
 *
 * <p>用户/管理员登录与引导管理员创建共用此 Bean。强度 12 是安全与耗时的折中：
 * 比默认 10 更抗暴力破解，又避免登录接口在秒杀峰值被 BCrypt 拖死。
 * 无外部中间件、无 {@code @Order}。
 */
@Configuration
public class PasswordConfig {
    /**
     * 提供全局 {@link PasswordEncoder}。
     *
     * @return 强度为 12 的 BCrypt 编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
