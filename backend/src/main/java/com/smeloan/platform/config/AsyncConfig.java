package com.smeloan.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for background task execution.
 *
 * <p>Provides a dedicated thread pool ({@code externalCheckExecutor}) for
 * external check tasks so they do not compete with the default Spring async
 * executor.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool dedicated to external check executions (JCIC, AML, ETC).
     * Core pool size 4, max 10 — sized for typical concurrent check volume.
     */
    @Bean(name = "externalCheckExecutor")
    public Executor externalCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ext-check-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Default async executor for general background tasks (notifications, events).
     */
    @Bean(name = "defaultTaskExecutor")
    public Executor defaultTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
