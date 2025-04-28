package com.sion.sionpicturebackend.config;

/**
 * @Author : wick
 * @Date : 2025/4/28 19:44
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                2, // corePoolSize 核心线程数
                5, // maximumPoolSize 最大线程数
                60, // keepAliveTime 线程空闲时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(1000) // 阻塞队列，队列长度
        );
    }
}
