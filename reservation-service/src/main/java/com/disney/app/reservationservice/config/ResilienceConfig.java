package com.disney.app.reservationservice.config;

import com.disney.app.reservationservice.error.TransientCruiseSearchServiceException;
import com.mongodb.MongoException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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

        return Retry.of("reservationMongoRetry", config);
    }

    @Bean
    public Retry cruiseSearchRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(250))
                .retryExceptions(TransientCruiseSearchServiceException.class)
                .build();

        return Retry.of("cruiseSearchRetry", config);
    }

    @Bean
    public CircuitBreaker cruiseSearchCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(10)
                .build();

        return CircuitBreaker.of("cruiseSearchCircuitBreaker", config);
    }

    private boolean isRetryableDatabaseException(Throwable throwable) {
        return throwable instanceof DataAccessException || throwable instanceof MongoException;
    }
}
