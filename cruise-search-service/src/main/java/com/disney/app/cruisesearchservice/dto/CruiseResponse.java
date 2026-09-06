package com.disney.app.cruisesearchservice.dto;

import java.time.LocalDateTime;

public record CruiseResponse(

        String id,
        String shipName,
        String departurePort,
        String destination,
        LocalDateTime departureDate,
        LocalDateTime returnDate
) {
}
