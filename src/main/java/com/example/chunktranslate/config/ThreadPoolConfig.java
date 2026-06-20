package com.example.chunktranslate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 翻译线程池配置。
 * <p>为翻译任务创建独立的线程池，核心参数从 application.yml 中读取：
 * core-size / max-size / queue-capacity / keep-alive-seconds。
 * 拒绝策略为 CallerRunsPolicy，防止任务丢失。</p>
 */
@EnableAsync
@Configuration
public class ThreadPoolConfig {

    @Value("${translation.thread-pool.core-size:10}")
    private int coreSize;

    @Value("${translation.thread-pool.max-size:20}")
    private int maxSize;

    @Value("${translation.thread-pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${translation.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean("translationExecutor")
    public Executor translationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("translation-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
