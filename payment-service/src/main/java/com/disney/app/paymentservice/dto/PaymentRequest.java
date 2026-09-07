package com.disney.app.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank(message = "Reservation id is mandatory")
        String reservationId,

        @NotNull(message = "Amount is mandatory")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Payment method token is mandatory")
        String paymentMethodToken
) {
}
