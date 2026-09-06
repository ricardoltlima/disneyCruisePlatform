package com.disney.app.cruisesearchservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String IDEMPOTENCY_CACHE = "idempotency";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(IDEMPOTENCY_CACHE);
    }
}
