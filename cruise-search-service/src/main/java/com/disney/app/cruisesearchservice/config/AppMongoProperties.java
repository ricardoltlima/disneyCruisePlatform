package com.disney.app.cruisesearchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.mongodb")
public record AppMongoProperties(
        String uri,
        String database
) {
}
