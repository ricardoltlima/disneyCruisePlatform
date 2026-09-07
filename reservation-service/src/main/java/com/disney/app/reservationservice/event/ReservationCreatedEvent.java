package com.disney.app.reservationservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationCreatedEvent(
        String eventId,
        String reservationId,
        String cruiseId,
        String guestId,
        BigDecimal amount,
        LocalDateTime occurredAt,
        Integer version
) {
}
