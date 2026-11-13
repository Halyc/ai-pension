package com.easy;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = "com.easy.common.mapper")
@EnableScheduling
@EnableAsync
public class EasyAI_Pension_Application {
    public static void main(String[] args) {
        SpringApplication.run(EasyAI_Pension_Application.class, args);
    }
}