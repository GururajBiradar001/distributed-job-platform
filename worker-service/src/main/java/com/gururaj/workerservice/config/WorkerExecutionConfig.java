package com.gururaj.workerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class WorkerExecutionConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor jobExecutorPool() {

        int workerThreads = 4;
        int queueCapacity = 0;

        return new ThreadPoolExecutor(
                workerThreads,
                workerThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}