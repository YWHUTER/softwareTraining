package com.campus.news;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.campus.news.mapper")
@EnableAsync  // 启用异步支持，用于AI评论机器人等异步任务
public class CampusNewsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusNewsApplication.class, args);
    }
}
