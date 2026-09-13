package com.disney.app.cruisesearchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record AppSecurityProperties(
        String secret
) {
}
