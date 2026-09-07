package com.disney.app.reservationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationRequest(
        @NotBlank(message = "Cruise id is mandatory")
        String cruiseId,

        @NotBlank(message = "Guest id is mandatory")
        String guestId,

        @NotBlank(message = "Guest name is mandatory")
        String guestName,

        @NotNull(message = "Number of guests is mandatory")
        @Positive(message = "Number of guests must be greater than zero")
        Integer numberOfGuests
) {
}
