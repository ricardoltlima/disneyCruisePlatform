package com.disney.app.paymentservice.event;

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
