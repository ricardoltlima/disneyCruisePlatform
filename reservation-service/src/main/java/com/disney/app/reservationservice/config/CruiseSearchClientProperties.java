package com.disney.app.reservationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.clients.cruise-search")
public record CruiseSearchClientProperties(
        String baseUrl,
        String bearerToken,
        Duration timeout
) {
}
