package com.sion.sionpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.sion.sionpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class SionPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SionPictureBackendApplication.class, args);
    }

}
