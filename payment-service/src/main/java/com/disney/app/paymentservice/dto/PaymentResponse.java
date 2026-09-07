package com.disney.app.paymentservice.dto;

import com.disney.app.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String reservationId,
        BigDecimal amount,
        PaymentStatus status,
        String providerReference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
