package com.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@MapperScan({"com.chat.mapper"})
@SpringBootApplication(scanBasePackages = {"com.chat"})
public class ChatApplication {
    public static void main(String[] args) {
//        TimeZone timeZone = TimeZone.getTimeZone("UTC");
//        TimeZone.setDefault(timeZone);

        SpringApplication.run(ChatApplication.class, args);
    }

}
