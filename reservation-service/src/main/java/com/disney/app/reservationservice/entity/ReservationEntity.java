package com.disney.app.reservationservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "reservations")
public record ReservationEntity(
        @Id
        String id,

        @Indexed
        String cruiseId,

        @Indexed
        String guestId,

        String guestName,
        Integer numberOfGuests,
        BigDecimal totalPrice,

        @Indexed
        ReservationStatus status,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
