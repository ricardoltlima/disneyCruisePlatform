package com.disney.app.cruisesearchservice.dto;

import com.disney.app.cruisesearchservice.model.CruiseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CruiseResponse(

        String id,
        String shipName,
        String departurePort,
        String destination,
        LocalDateTime departureDate,
        LocalDateTime returnDate,
        Integer availableCabins,
        BigDecimal basePrice,
        CruiseStatus status
) {
}
