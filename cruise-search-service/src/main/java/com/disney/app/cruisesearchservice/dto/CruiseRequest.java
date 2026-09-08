package com.disney.app.cruisesearchservice.dto;

import com.disney.app.cruisesearchservice.model.CruiseStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
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
        LocalDateTime returnDate,
        @NotNull(message = "Available cabins is mandatory")
        @PositiveOrZero(message = "Available cabins must be zero or greater")
        Integer availableCabins,
        @NotNull(message = "Base price is mandatory")
        @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than zero")
        BigDecimal basePrice,
        @NotNull(message = "Status is mandatory")
        CruiseStatus status
) {
}
