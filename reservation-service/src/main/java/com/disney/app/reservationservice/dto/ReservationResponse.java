package com.disney.app.reservationservice.dto;

import com.disney.app.reservationservice.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        String id,
        String cruiseId,
        String guestId,
        String guestName,
        Integer numberOfGuests,
        BigDecimal totalPrice,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
