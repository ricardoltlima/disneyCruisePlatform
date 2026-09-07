package com.disney.app.reservationservice.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CruiseSearchResponse(
        String id,
        String shipName,
        String departurePort,
        String destination,
        LocalDateTime departureDate,
        LocalDateTime returnDate,
        Integer availableCabins,
        BigDecimal basePrice,
        String status
) {
}
