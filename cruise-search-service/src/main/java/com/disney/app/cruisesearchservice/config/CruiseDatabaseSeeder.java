package com.disney.app.cruisesearchservice.config;

import com.disney.app.cruisesearchservice.entity.CruiseEntity;
import com.disney.app.cruisesearchservice.model.CruiseStatus;
import com.disney.app.cruisesearchservice.repository.CruiseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class CruiseDatabaseSeeder implements ApplicationRunner {

    private final CruiseRepository repository;

    public CruiseDatabaseSeeder(CruiseRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        repository.saveAll(cruises())
                .collectList()
                .doOnSuccess(savedCruises -> log.info("Cruise database populated with {} sample cruises.", savedCruises.size()))
                .doOnError(error -> log.warn("Could not populate cruise database. error={}", error.getClass().getSimpleName()))
                .subscribe();
    }

    private List<CruiseEntity> cruises() {
        return List.of(
                cruise(
                        "sample-disney-magic-2027-01-10",
                        "Disney Magic",
                        "Fort Lauderdale (FL)",
                        "Bahamas",
                        LocalDateTime.of(2027, 1, 10, 9, 0),
                        LocalDateTime.of(2027, 1, 14, 7, 0),
                        120,
                        "1299.99"
                ),
                cruise(
                        "sample-disney-wonder-2027-01-17",
                        "Disney Wonder",
                        "San Diego (CA)",
                        "Baja Peninsula",
                        LocalDateTime.of(2027, 1, 17, 9, 0),
                        LocalDateTime.of(2027, 1, 22, 7, 0),
                        110,
                        "1399.99"
                ),
                cruise(
                        "sample-disney-dream-2027-01-24",
                        "Disney Dream",
                        "Port Canaveral (FL)",
                        "Bahamas",
                        LocalDateTime.of(2027, 1, 24, 9, 0),
                        LocalDateTime.of(2027, 1, 28, 7, 0),
                        150,
                        "1499.99"
                ),
                cruise(
                        "sample-disney-fantasy-2027-01-31",
                        "Disney Fantasy",
                        "Port Canaveral (FL)",
                        "Western Caribbean",
                        LocalDateTime.of(2027, 1, 31, 9, 0),
                        LocalDateTime.of(2027, 2, 7, 7, 0),
                        145,
                        "1599.99"
                ),
                cruise(
                        "sample-disney-wish-2027-02-07",
                        "Disney Wish",
                        "Port Canaveral (FL)",
                        "Bahamas",
                        LocalDateTime.of(2027, 2, 7, 9, 0),
                        LocalDateTime.of(2027, 2, 11, 7, 0),
                        180,
                        "1699.99"
                ),
                cruise(
                        "sample-disney-treasure-2027-02-14",
                        "Disney Treasure",
                        "Port Canaveral (FL)",
                        "Eastern Caribbean",
                        LocalDateTime.of(2027, 2, 14, 9, 0),
                        LocalDateTime.of(2027, 2, 21, 7, 0),
                        175,
                        "1799.99"
                ),
                cruise(
                        "sample-disney-destiny-2027-02-21",
                        "Disney Destiny",
                        "Fort Lauderdale (FL)",
                        "Western Caribbean",
                        LocalDateTime.of(2027, 2, 21, 9, 0),
                        LocalDateTime.of(2027, 2, 26, 7, 0),
                        165,
                        "1749.99"
                ),
                cruise(
                        "sample-disney-adventure-2027-02-28",
                        "Disney Adventure",
                        "Singapore",
                        "Southeast Asia",
                        LocalDateTime.of(2027, 2, 28, 9, 0),
                        LocalDateTime.of(2027, 3, 5, 7, 0),
                        200,
                        "1899.99"
                )
        );
    }

    private CruiseEntity cruise(
            String id,
            String shipName,
            String departurePort,
            String destination,
            LocalDateTime departureDate,
            LocalDateTime returnDate,
            Integer availableCabins,
            String basePrice
    ) {
        return new CruiseEntity(
                id,
                shipName,
                departurePort,
                destination,
                departureDate,
                returnDate,
                availableCabins,
                new BigDecimal(basePrice),
                CruiseStatus.AVAILABLE
        );
    }
}
