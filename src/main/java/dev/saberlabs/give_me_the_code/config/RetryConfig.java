package dev.saberlabs.give_me_the_code.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/** Backs {@code @Retryable} on the LeetCode client: a transient 429/5xx gets retried with backoff. */
@Configuration
@EnableRetry
public class RetryConfig {
}
