package com.disney.app.cruisesearchservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CruiseRequest(

        @NotBlank(message = "Ship name is mandatory")
        String shipName,
        @NotBlank(message = "Departure Port name is mandatory")
        String departurePort,
        @NotBlank(message = "Destination name is mandatory")
        String destination,
        @Future
        @NotNull(message = "Departure Date name is mandatory")
        LocalDateTime departureDate,
        @NotNull
        @Future(message = "Return Date name is mandatory")
        LocalDateTime returnDate
) {
}
