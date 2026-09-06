package com.disney.app.cruisesearchservice.config;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public Retry mongoRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(250))
                .retryOnException(this::isRetryableDatabaseException)
                .build();

        return Retry.of("mongoRetry", config);
    }

    private boolean isRetryableDatabaseException(Throwable throwable) {
        if (throwable instanceof DuplicateKeyException) {
            return false;
        }

        return throwable instanceof DataAccessException || throwable instanceof MongoException;
    }
}
