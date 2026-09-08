package com.disney.app.cruisesearchservice.entity;

import com.disney.app.cruisesearchservice.model.CruiseStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "cruises")
@CompoundIndex(
        name = "unique_cruise_sailing",
        def = "{'shipName': 1, 'departurePort': 1, 'destination': 1, 'departureDate': 1, 'returnDate': 1}",
        unique = true
)
public record CruiseEntity(
        @Id
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
