package com.spingbootinit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.spingbootinit.mapper")
@EnableScheduling
@EnableAsync
public class SpingbootInitApplication {



    public static void main(String[] args) {

        SpringApplication.run(SpingbootInitApplication.class, args);
    }

}
