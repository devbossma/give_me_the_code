package dev.saberlabs.give_me_the_code.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Backs the daily refresh job ({@code @Scheduled}) and the on-demand cache-miss fallback
 * ({@code @Async}, used only when a request arrives before the scheduled job has run for the
 * day). A small bounded pool, not the default unbounded {@code SimpleAsyncTaskExecutor}: this
 * only ever needs to run a couple of outbound LeetCode calls at a time.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean
    public Executor leetCodeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("leetcode-");
        executor.initialize();
        return executor;
    }
}
