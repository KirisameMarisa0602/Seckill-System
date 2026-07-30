package com.kirisamemarisa.seckillsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//项目启动时，去com.kirisamemarisa.seckillsystem.mapper这个包下面，把所有的接口都扫描并自动生成实现类（代理对象），放到 Spring 容器里（变成 Bean）
@MapperScan("com.kirisamemarisa.seckillsystem.mapper")
public class SeckillSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillSystemApplication.class, args);
    }
}