package com.campus.news;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.news.mapper")
public class CampusNewsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusNewsApplication.class, args);
    }
}
