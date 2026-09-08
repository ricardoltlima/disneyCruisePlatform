package com.disney.app.cruisesearchservice.service;

import com.disney.app.cruisesearchservice.dto.CruiseRequest;
import com.disney.app.cruisesearchservice.dto.CruiseResponse;
import com.disney.app.cruisesearchservice.entity.CruiseEntity;
import com.disney.app.cruisesearchservice.error.CruiseAlreadyExistsException;
import com.disney.app.cruisesearchservice.error.CruiseNotFoundException;
import com.disney.app.cruisesearchservice.error.CruisePersistenceException;
import com.disney.app.cruisesearchservice.mapper.CruiseMapper;
import com.disney.app.cruisesearchservice.model.CruiseStatus;
import com.disney.app.cruisesearchservice.repository.CruiseRepository;
import com.mongodb.DuplicateKeyException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcernResult;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CruiseServiceTest {

    private static final LocalDateTime DEPARTURE_DATE = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime RETURN_DATE = LocalDateTime.of(2026, 10, 5, 9, 0);

    private CruiseRepository repository;
    private CruiseService service;

    @BeforeEach
    void setUp() {
        repository = mock(CruiseRepository.class);
        service = new CruiseService(repository, new TestCruiseMapper());
    }

    @Test
    void addCruiseSavesCruiseAndReturnsResponse() {
        CruiseRequest request = cruiseRequest("Disney Wish");
        CruiseEntity savedEntity = cruiseEntity("cruise-1", request);

        when(repository.save(any(CruiseEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(service.addCruise(request))
                .expectNext(cruiseResponse(savedEntity))
                .verifyComplete();

        verify(repository).save(new CruiseEntity(
                null,
                request.shipName(),
                request.departurePort(),
                request.destination(),
                request.departureDate(),
                request.returnDate(),
                request.availableCabins(),
                request.basePrice(),
                request.status()
        ));
    }

    @Test
    void addCruiseSavesAgainWhenSameRequestIsReused() {
        CruiseRequest request = cruiseRequest("Disney Wish");
        CruiseEntity savedEntity = cruiseEntity("cruise-1", request);

        when(repository.save(any(CruiseEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(service.addCruise(request))
                .expectNext(cruiseResponse(savedEntity))
                .verifyComplete();

        StepVerifier.create(service.addCruise(request))
                .expectNext(cruiseResponse(savedEntity))
                .verifyComplete();

        verify(repository, times(2)).save(any(CruiseEntity.class));
    }

    @Test
    void addCruiseMapsDatabaseFailureToPersistenceException() {
        when(repository.save(any(CruiseEntity.class)))
                .thenReturn(Mono.error(new DataAccessResourceFailureException("Mongo unavailable")));

        StepVerifier.create(service.addCruise(cruiseRequest("Disney Wish")))
                .expectError(CruisePersistenceException.class)
                .verify();
    }

    @Test
    void addCruiseMapsDuplicateDatabaseKeyToAlreadyExistsException() {
        when(repository.save(any(CruiseEntity.class)))
                .thenReturn(Mono.error(new DuplicateKeyException(
                        new BsonDocument(),
                        new ServerAddress(),
                        WriteConcernResult.unacknowledged()
                )));

        StepVerifier.create(service.addCruise(cruiseRequest("Disney Wish")))
                .expectError(CruiseAlreadyExistsException.class)
                .verify();
    }

    @Test
    void getCruiseReturnsCruiseWhenFound() {
        CruiseEntity entity = cruiseEntity("cruise-1", cruiseRequest("Disney Wish"));

        when(repository.findById("cruise-1")).thenReturn(Mono.just(entity));

        StepVerifier.create(service.getCruise("cruise-1"))
                .expectNext(cruiseResponse(entity))
                .verifyComplete();
    }

    @Test
    void getCruiseReturnsNotFoundWhenRepositoryIsEmpty() {
        when(repository.findById("missing-cruise")).thenReturn(Mono.empty());

        StepVerifier.create(service.getCruise("missing-cruise"))
                .expectError(CruiseNotFoundException.class)
                .verify();
    }

    @Test
    void getCruisesReturnsPagedCruises() {
        Pageable pageable = PageRequest.of(0, 2);
        CruiseEntity first = cruiseEntity("cruise-1", cruiseRequest("Disney Wish"));
        CruiseEntity second = cruiseEntity("cruise-2", cruiseRequest("Disney Treasure"));

        when(repository.findAllBy(pageable)).thenReturn(Flux.just(first, second));

        StepVerifier.create(service.getCruises(pageable))
                .expectNext(cruiseResponse(first))
                .expectNext(cruiseResponse(second))
                .verifyComplete();
    }

    private static CruiseRequest cruiseRequest(String shipName) {
        return new CruiseRequest(
                shipName,
                "Port Canaveral",
                "Bahamas",
                DEPARTURE_DATE,
                RETURN_DATE,
                25,
                new BigDecimal("1499.99"),
                CruiseStatus.AVAILABLE
        );
    }

    private static CruiseEntity cruiseEntity(String id, CruiseRequest request) {
        return new CruiseEntity(
                id,
                request.shipName(),
                request.departurePort(),
                request.destination(),
                request.departureDate(),
                request.returnDate(),
                request.availableCabins(),
                request.basePrice(),
                request.status()
        );
    }

    private static CruiseResponse cruiseResponse(CruiseEntity entity) {
        return new CruiseResponse(
                entity.id(),
                entity.shipName(),
                entity.departurePort(),
                entity.destination(),
                entity.departureDate(),
                entity.returnDate(),
                entity.availableCabins(),
                entity.basePrice(),
                entity.status()
        );
    }

    private static class TestCruiseMapper implements CruiseMapper {

        @Override
        public CruiseResponse toCruiseResponse(CruiseEntity cruiseEntity) {
            return cruiseResponse(cruiseEntity);
        }

        @Override
        public CruiseEntity toCruiseEntity(CruiseRequest cruiseRequest) {
            return new CruiseEntity(
                    null,
                    cruiseRequest.shipName(),
                    cruiseRequest.departurePort(),
                    cruiseRequest.destination(),
                    cruiseRequest.departureDate(),
                    cruiseRequest.returnDate(),
                    cruiseRequest.availableCabins(),
                    cruiseRequest.basePrice(),
                    cruiseRequest.status()
            );
        }
    }
}
