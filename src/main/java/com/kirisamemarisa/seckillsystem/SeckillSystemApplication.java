package com.kirisamemarisa.seckillsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kirisamemarisa.seckillsystem.mapper")
public class SeckillSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillSystemApplication.class, args);
    }

}