package com.disney.app.reservationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record AppKafkaProperties(
        String bootstrapServers,
        String reservationCreatedTopic,
        String producerAcks,
        String producerClientId,
        String reservationCreatedSchema
) {
}
