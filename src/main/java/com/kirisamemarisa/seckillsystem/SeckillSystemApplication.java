package com.kirisamemarisa.seckillsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀系统 Spring Boot 启动类，整个后端进程的入口。
 *
 * <p>在秒杀链路中不处理具体下单/支付请求，而是拉起容器：扫描 {@code controller} 暴露 HTTP、
 * 注册 {@code mapper} 访问数据库，并启用定时任务（Outbox 投递、超时未支付扫描等）。
 * 无对应前端 API。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.kirisamemarisa.seckillsystem.mapper") // 扫描 mapper 包，生成 MyBatis 代理并注册为 Spring Bean
public class SeckillSystemApplication {
    /**
     * 启动嵌入式 Web 容器与 Spring 上下文。
     *
     * @param args 命令行参数，转交给 Spring Boot
     * @return 无；副作用是进程常驻并监听配置端口
     */
    public static void main(String[] args) {
        SpringApplication.run(SeckillSystemApplication.class, args);
    }
}
