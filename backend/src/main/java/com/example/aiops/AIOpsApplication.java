package com.example.aiops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.example.aiops.mapper")
@SpringBootApplication
@EnableScheduling
public class AIOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIOpsApplication.class, args);
    }
}
