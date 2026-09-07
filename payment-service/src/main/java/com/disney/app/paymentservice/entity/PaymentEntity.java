package com.disney.app.paymentservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
public record PaymentEntity(
        @Id
        String id,

        @Indexed
        String reservationId,

        BigDecimal amount,

        @Indexed
        PaymentStatus status,

        String providerReference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
