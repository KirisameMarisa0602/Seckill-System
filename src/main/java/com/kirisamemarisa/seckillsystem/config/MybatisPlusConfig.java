package com.kirisamemarisa.seckillsystem.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件装配。
 *
 * <p>启用 MySQL 分页拦截器，并把单次分页上限钉在 100 行，防止管理端漏传 {@code pageSize}
 * 时一次拉全表。依赖中间件：MySQL。无 {@code @Order}。
 */
@Configuration
public class MybatisPlusConfig {
    /**
     * 注册分页插件。
     *
     * @return 内含 {@link PaginationInnerInterceptor} 的拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
