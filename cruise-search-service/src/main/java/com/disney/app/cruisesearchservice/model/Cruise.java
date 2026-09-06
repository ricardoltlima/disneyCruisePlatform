package com.disney.app.cruisesearchservice.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Cruise {

    private String id;
    private String shipName;
    private String destination;
    private String departurePort;
    private LocalDateTime departureDate;
    private LocalDateTime returnDate;
    private Integer availableCabins;
    private BigDecimal basePrice;
    private String status;
}
