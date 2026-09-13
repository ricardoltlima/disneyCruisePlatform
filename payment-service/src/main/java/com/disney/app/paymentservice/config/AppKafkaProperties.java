package com.disney.app.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record AppKafkaProperties(
        String bootstrapServers,
        String consumerGroupId,
        String consumerAutoOffsetReset,
        String reservationCreatedTopic,
        String reservationCreatedSchema
) {
}
