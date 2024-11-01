package com.kovan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class NewsServiceMain {
    public static void main(String[] args) {

        SpringApplication.run(NewsServiceMain.class, args);
    }
}