package com.disney.app.cruisesearchservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "cruises")
public record CruiseEntity(
        @Id
        String id,
        String shipName,
        String departurePort,
        String destination,
        LocalDateTime departureDate,
        LocalDateTime returnDate
) {
}
